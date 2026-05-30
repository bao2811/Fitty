package com.example.fitty.data.content

import com.example.fitty.domain.model.FittyOnboardingAnswers
import com.example.fitty.domain.model.FittyUser
import com.example.fitty.domain.model.PlanInstance
import com.example.fitty.domain.model.ScheduledWorkout
import com.example.fitty.domain.model.StarterExerciseTemplate
import com.example.fitty.domain.model.StarterPlanBuildResult
import com.example.fitty.domain.model.StarterPlanPreviewContent
import com.example.fitty.domain.model.StarterPlanPreviewDetail
import com.example.fitty.domain.model.StarterPlanProfile
import com.example.fitty.domain.model.WorkoutExercise
import com.example.fitty.domain.repository.ContentRepository
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StarterPlanBuilder @Inject constructor(
    private val contentRepository: ContentRepository,
    private val localContentFallbacks: LocalContentFallbacks,
    private val weightAdvisor: ExerciseWeightAdvisor
) {
    suspend fun buildForUser(user: FittyUser, language: String): StarterPlanBuildResult {
        return build(
            profile = StarterPlanProfile(
                goal = user.profile.primaryGoal,
                fitnessLevel = user.profile.fitnessLevel,
                equipment = user.onboarding.equipmentAccess,
                weightKg = user.profile.weightKg,
                workoutDays = user.onboarding.workoutDays,
                durationMinutes = user.onboarding.workoutDurationMinutes,
                preferredTime = user.onboarding.preferredTime,
                language = language
            )
        )
    }

    suspend fun buildForAnswers(answers: FittyOnboardingAnswers, language: String): StarterPlanBuildResult {
        return build(
            profile = StarterPlanProfile(
                goal = answers.goal.toSchemaValue(),
                fitnessLevel = answers.fitnessLevel.toSchemaValue(),
                equipment = answers.equipment.toSchemaValue(),
                weightKg = answers.weightKg,
                workoutDays = answers.workoutDays.map { it.lowercase(Locale.US).take(3) }.sorted(),
                durationMinutes = answers.durationMinutes,
                preferredTime = answers.preferredTime.toSchemaValue(),
                language = language
            )
        )
    }

    suspend fun build(profile: StarterPlanProfile): StarterPlanBuildResult {
        val normalizedLanguage = profile.language.ifBlank { "en" }
        val template = contentRepository.getStarterPlanTemplate(
            goal = profile.goal,
            fitnessLevel = profile.fitnessLevel,
            equipment = profile.equipment,
            language = normalizedLanguage
        ) ?: localContentFallbacks.starterPlanTemplate(profile.goal, normalizedLanguage)

        val placeholderValues = placeholderValues(profile)
        val planName = template.planNameTemplate.applyPlaceholders(placeholderValues)
        val nextWorkoutDate = computeNextWorkoutDate(profile.workoutDays)
        val workoutExercises = template.exercises.map { it.toWorkoutExercise(profile) }
        val durationMinutes = profile.durationMinutes ?: 30

        val plan = PlanInstance(
            id = "starter_plan",
            sourceProgramId = template.sourceProgramId,
            name = planName,
            goal = profile.goal,
            durationWeeks = template.durationWeeks,
            workoutsPerWeek = profile.workoutDays.size.coerceAtLeast(1),
            equipment = profile.equipment,
            trainingStyle = template.trainingStyle.ifBlank { defaultTrainingStyle(profile.goal) },
            status = template.status,
            explanation = template.explanationTemplate.applyPlaceholders(placeholderValues),
            currentWeek = 1,
            nextWorkoutDate = nextWorkoutDate
        )

        val scheduledWorkouts = materializeScheduledWorkouts(
            planId = plan.id,
            profile = profile,
            templateTitles = template.scheduledWorkoutTitles,
            explanation = template.explanationTemplate.applyPlaceholders(placeholderValues),
            exercises = workoutExercises,
            durationMinutes = durationMinutes
        )

        val preview = StarterPlanPreviewContent(
            title = template.previewTitleTemplate.applyPlaceholders(placeholderValues),
            subtitle = template.previewSubtitle,
            details = listOf(
                StarterPlanPreviewDetail("goal", template.previewGoalTitle, template.previewGoalBodyTemplate.applyPlaceholders(placeholderValues)),
                StarterPlanPreviewDetail("calories", template.previewCaloriesTitle, template.previewCaloriesBodyTemplate.applyPlaceholders(placeholderValues)),
                StarterPlanPreviewDetail("workout_days", template.previewWorkoutDaysTitle, template.previewWorkoutDaysBodyTemplate.applyPlaceholders(placeholderValues)),
                StarterPlanPreviewDetail("duration", template.previewDurationTitle, template.previewDurationBodyTemplate.applyPlaceholders(placeholderValues)),
                StarterPlanPreviewDetail("why", template.previewWhyTitle, template.previewWhyBodyTemplate.applyPlaceholders(placeholderValues))
            ),
            exercises = workoutExercises
        )

        return StarterPlanBuildResult(
            plan = plan,
            scheduledWorkouts = scheduledWorkouts,
            preview = preview
        )
    }

    private fun materializeScheduledWorkouts(
        planId: String,
        profile: StarterPlanProfile,
        templateTitles: List<String>,
        explanation: String,
        exercises: List<WorkoutExercise>,
        durationMinutes: Int
    ): List<ScheduledWorkout> {
        val titles = templateTitles.ifEmpty { listOf("Starter Workout") }
        val workoutDays = profile.workoutDays.ifEmpty { listOf(LocalDate.now(appZoneId()).dayOfWeek.name.lowercase(Locale.US).take(3)) }
        val today = LocalDate.now(appZoneId())
        return workoutDays.mapIndexed { index, day ->
            val date = nextDateForDay(today, day)
            ScheduledWorkout(
                id = "${date.format(DATE_KEY_FORMATTER)}_${day}",
                planId = planId,
                dateKey = date.format(DATE_KEY_FORMATTER),
                weekNumber = 1,
                orderInWeek = index + 1,
                title = titles[index % titles.size],
                durationMinutes = durationMinutes,
                estimatedCalories = estimateCalories(durationMinutes),
                difficulty = profile.fitnessLevel,
                equipment = profile.equipment,
                status = "scheduled",
                explanation = explanation,
                exercises = exercises
            )
        }
    }

    private fun placeholderValues(profile: StarterPlanProfile): Map<String, String> {
        val goalLabel = displayLabel(profile.goal, profile.language)
        val fitnessLabel = displayLabel(profile.fitnessLevel, profile.language).lowercase(Locale.US)
        val equipmentLabel = displayLabel(profile.equipment, profile.language).lowercase(Locale.US)
        val preferredTimeLabel = displayLabel(profile.preferredTime, profile.language).lowercase(Locale.US)
        val caloriesTarget = caloriesTarget(profile)
        val schedule = if (profile.workoutDays.isNotEmpty()) {
            profile.workoutDays.joinToString(", ") { displayLabel(it, profile.language) }
        } else {
            if (profile.language.startsWith("vi")) {
                "Hãy thêm ngày tập trong onboarding để cá nhân hóa mục này."
            } else {
                "Add workout days in onboarding to personalize this section."
            }
        }
        val durationLabel = profile.durationMinutes?.let {
            if (profile.language.startsWith("vi")) "$it phút/buổi" else "$it min/session"
        } ?: if (profile.language.startsWith("vi")) {
            "Chưa chọn thời lượng buổi tập."
        } else {
            "Workout duration not selected yet."
        }
        return mapOf(
            "goalLabel" to goalLabel,
            "fitnessLabel" to fitnessLabel,
            "equipmentLabel" to equipmentLabel,
            "preferredTimeLabel" to preferredTimeLabel,
            "caloriesTarget" to caloriesTarget,
            "schedule" to schedule,
            "durationLabel" to durationLabel
        )
    }

    private fun caloriesTarget(profile: StarterPlanProfile): String {
        val weight = profile.weightKg
        if (weight == null) {
            return if (profile.language.startsWith("vi")) {
                "Mục tiêu calo sẽ được hoàn thiện sau khi đồng bộ kế hoạch đầu tiên."
            } else {
                "Calorie target will be finalized after your first synced plan."
            }
        }
        val calories = when (profile.goal) {
            "gain_muscle" -> weight * 34
            "lose_weight" -> weight * 28
            else -> weight * 30
        }
        return if (profile.language.startsWith("vi")) {
            "$calories kcal tạm tính theo hồ sơ hiện tại của bạn."
        } else {
            "$calories kcal provisional target based on your current profile."
        }
    }

    private fun StarterExerciseTemplate.toWorkoutExercise(profile: StarterPlanProfile): WorkoutExercise {
        return WorkoutExercise(
            exerciseId = exerciseId,
            name = name,
            sets = sets,
            reps = reps,
            durationSeconds = durationSeconds,
            targetWeightKg = weightAdvisor.suggestTargetWeight(
                profile = profile,
                targetWeightMode = targetWeightMode,
                fixedTargetWeightKg = null,
                bodyWeightMultiplier = null,
                minSuggestedWeightKg = null,
                maxSuggestedWeightKg = null
            )
        )
    }

    private fun defaultTrainingStyle(goal: String): String = when (goal) {
        "gain_muscle" -> "strength"
        "improve_endurance" -> "cardio"
        "improve_flexibility" -> "mobility"
        else -> "full_body"
    }

    private fun estimateCalories(durationMinutes: Int): Int = (durationMinutes * 5.5).toInt()

    private fun computeNextWorkoutDate(workoutDays: List<String>): String {
        if (workoutDays.isEmpty()) {
            return LocalDate.now(appZoneId()).format(DATE_KEY_FORMATTER)
        }
        val today = LocalDate.now(appZoneId())
        val nextDate = workoutDays.map { nextDateForDay(today, it) }.minOrNull() ?: today
        return nextDate.format(DATE_KEY_FORMATTER)
    }

    private fun nextDateForDay(startDate: LocalDate, dayKey: String): LocalDate {
        val targetIndex = DAY_ORDER.indexOf(dayKey.lowercase(Locale.US).take(3)).takeIf { it >= 0 } ?: 0
        val currentIndex = startDate.dayOfWeek.value % 7
        val delta = (targetIndex - currentIndex + 7) % 7
        return startDate.plusDays(delta.toLong())
    }

    private fun displayLabel(value: String, language: String): String {
        if (value.isBlank()) {
            return if (language.startsWith("vi")) "chưa thiết lập" else "not set"
        }
        val normalized = value.lowercase(Locale.US)
        val dictionary = if (language.startsWith("vi")) {
            mapOf(
                "gain_muscle" to "Tăng cơ",
                "lose_weight" to "Giảm cân",
                "maintain_fitness" to "Duy trì thể lực",
                "improve_endurance" to "Tăng sức bền",
                "improve_flexibility" to "Cải thiện độ dẻo",
                "build_habits" to "Xây dựng thói quen lành mạnh",
                "beginner" to "mới bắt đầu",
                "intermediate" to "trung cấp",
                "advanced" to "nâng cao",
                "home_none" to "ở nhà không dụng cụ",
                "home_basic" to "ở nhà với dụng cụ cơ bản",
                "gym" to "phòng gym",
                "mix" to "kết hợp ở nhà và phòng gym",
                "morning" to "buổi sáng",
                "afternoon" to "buổi chiều",
                "evening" to "buổi tối",
                "mon" to "Thứ 2",
                "tue" to "Thứ 3",
                "wed" to "Thứ 4",
                "thu" to "Thứ 5",
                "fri" to "Thứ 6",
                "sat" to "Thứ 7",
                "sun" to "Chủ nhật"
            )
        } else {
            emptyMap()
        }
        return dictionary[normalized] ?: normalized.split('_', ' ').joinToString(" ") { part ->
            part.replaceFirstChar { char ->
                if (char.isLowerCase()) char.titlecase(Locale.US) else char.toString()
            }
        }
    }

    private fun String.applyPlaceholders(values: Map<String, String>): String {
        var result = this
        values.forEach { (key, value) ->
            result = result.replace("{{$key}}", value)
        }
        return result
    }

    private fun String.toSchemaValue(): String = lowercase(Locale.US).trim()
        .replace("&", "and")
        .replace(Regex("[^a-z0-9]+"), "_")
        .trim('_')

    private fun appZoneId(): ZoneId = ZoneId.systemDefault()

    private companion object {
        val DATE_KEY_FORMATTER: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
        val DAY_ORDER = listOf("sun", "mon", "tue", "wed", "thu", "fri", "sat")
    }
}
