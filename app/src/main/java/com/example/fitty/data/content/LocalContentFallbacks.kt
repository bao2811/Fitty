package com.example.fitty.data.content

import android.content.Context
import com.example.fitty.R
import com.example.fitty.domain.model.CoachContentConfig
import com.example.fitty.domain.model.ExercisePrescriptionContent
import com.example.fitty.domain.model.ExercisePrescriptionRule
import com.example.fitty.domain.model.HomeContentConfig
import com.example.fitty.domain.model.HomeBehaviorConfig
import com.example.fitty.domain.model.HomeEmptyStateContent
import com.example.fitty.domain.model.HomeTaskCategory
import com.example.fitty.domain.model.HomeTaskTemplateContent
import com.example.fitty.domain.model.OnboardingChoiceContent
import com.example.fitty.domain.model.OnboardingContentConfig
import com.example.fitty.domain.model.PracticeCategoryContent
import com.example.fitty.domain.model.QuickWorkoutConfig
import com.example.fitty.domain.model.StarterExerciseTemplate
import com.example.fitty.domain.model.StarterPlanTemplate
import com.example.fitty.domain.model.TrackBehaviorConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalContentFallbacks @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun home(language: String): HomeContentConfig {
        return HomeContentConfig(
            emptyState = HomeEmptyStateContent(
                workoutTitle = context.getString(R.string.home_workout_empty_title),
                workoutBody = context.getString(R.string.home_workout_empty_body),
                insightMessage = context.getString(R.string.home_insight_empty),
                achievementMessage = context.getString(R.string.home_achievement_empty)
            ),
            insightActions = listOf(
                context.getString(R.string.home_action_apply),
                context.getString(R.string.home_action_ask_why),
                context.getString(R.string.home_action_dismiss)
            ),
            suggestedTaskPresets = listOf(
                HomeTaskTemplateContent(
                    id = "workout",
                    title = context.getString(R.string.home_task_workout_title),
                    description = context.getString(R.string.home_task_session_desc, context.getString(R.string.home_task_workout_title)),
                    timeMinutes = 18 * 60,
                    category = HomeTaskCategory.Workout,
                    reminderEnabled = true
                ),
                HomeTaskTemplateContent(
                    id = "meal_lunch",
                    title = context.getString(R.string.home_task_log_lunch_title),
                    description = context.getString(R.string.home_task_log_lunch_desc),
                    timeMinutes = 12 * 60 + 30,
                    category = HomeTaskCategory.Meal,
                    reminderEnabled = true
                ),
                HomeTaskTemplateContent(
                    id = "water_check",
                    title = if (language.isVietnamese()) "Kiểm tra nước uống" else "Hydration check",
                    description = if (language.isVietnamese()) {
                        "Uống nước và cập nhật lượng nước hôm nay."
                    } else {
                        "Drink water and update your intake."
                    },
                    timeMinutes = 15 * 60,
                    category = HomeTaskCategory.Water,
                    reminderEnabled = true
                )
            ),
            defaultTaskTemplates = listOf(
                HomeTaskTemplateContent(
                    id = "workout",
                    title = context.getString(R.string.home_task_workout_title),
                    description = "",
                    timeMinutes = 18 * 60,
                    category = HomeTaskCategory.Workout,
                    reminderEnabled = true
                ),
                HomeTaskTemplateContent(
                    id = "meal_lunch",
                    title = context.getString(R.string.home_task_log_lunch_title),
                    description = context.getString(R.string.home_task_log_lunch_desc),
                    timeMinutes = 12 * 60 + 30,
                    category = HomeTaskCategory.Meal,
                    reminderEnabled = true
                ),
                HomeTaskTemplateContent(
                    id = "water_check",
                    title = context.getString(R.string.home_task_drink_water_title),
                    description = context.getString(R.string.home_task_drink_water_desc),
                    timeMinutes = 15 * 60,
                    category = HomeTaskCategory.Water,
                    reminderEnabled = true
                )
            )
        )
    }

    fun coach(): CoachContentConfig {
        return CoachContentConfig(
            welcomeMessage = context.getString(R.string.coach_welcome_message),
            promptChips = listOf(
                context.getString(R.string.coach_prompt_post_workout_meal),
                context.getString(R.string.coach_prompt_adjust_today),
                context.getString(R.string.coach_prompt_missed_workout),
                context.getString(R.string.coach_prompt_dinner_ideas)
            )
        )
    }

    fun onboarding(language: String): OnboardingContentConfig {
        return OnboardingContentConfig(
            stepTitles = listOf(
                context.getString(R.string.onboarding_step_title_goal),
                context.getString(R.string.onboarding_step_title_body),
                context.getString(R.string.onboarding_step_title_fitness),
                context.getString(R.string.onboarding_step_title_days),
                context.getString(R.string.onboarding_step_title_time),
                context.getString(R.string.onboarding_step_title_duration),
                context.getString(R.string.onboarding_step_title_location),
                context.getString(R.string.onboarding_step_title_nutrition),
                context.getString(R.string.onboarding_step_title_reminders)
            ),
            goals = listOf(
                choice("lose_weight", R.string.onboarding_goal_lose_weight, R.string.onboarding_goal_lose_weight_desc),
                choice("gain_muscle", R.string.onboarding_goal_gain_muscle, R.string.onboarding_goal_gain_muscle_desc),
                choice("maintain_fitness", R.string.onboarding_goal_maintain_fitness, R.string.onboarding_goal_maintain_fitness_desc),
                choice("improve_endurance", R.string.onboarding_goal_improve_endurance, R.string.onboarding_goal_improve_endurance_desc),
                choice("improve_flexibility", R.string.onboarding_goal_improve_flexibility, R.string.onboarding_goal_improve_flexibility_desc),
                choice("build_habits", R.string.onboarding_goal_build_habits, R.string.onboarding_goal_build_habits_desc)
            ),
            fitnessLevels = listOf(
                choice("beginner", R.string.onboarding_fitness_beginner, R.string.onboarding_fitness_beginner_desc),
                choice("intermediate", R.string.onboarding_fitness_intermediate, R.string.onboarding_fitness_intermediate_desc),
                choice("advanced", R.string.onboarding_fitness_advanced, R.string.onboarding_fitness_advanced_desc)
            ),
            preferredTimes = listOf(
                genericChoice("morning", R.string.onboarding_time_morning),
                genericChoice("afternoon", R.string.onboarding_time_afternoon),
                genericChoice("evening", R.string.onboarding_time_evening)
            ),
            durations = listOf(
                genericChoice("20", R.string.onboarding_duration_20),
                genericChoice("30", R.string.onboarding_duration_30),
                genericChoice("45", R.string.onboarding_duration_45),
                genericChoice("60", R.string.onboarding_duration_60)
            ),
            equipments = listOf(
                genericChoice("home_none", R.string.onboarding_equipment_home_none),
                genericChoice("home_basic", R.string.onboarding_equipment_home_basic),
                genericChoice("gym", R.string.onboarding_equipment_gym),
                genericChoice("mix", R.string.onboarding_equipment_mix)
            ),
            nutritionStyles = listOf(
                genericChoice("standard", R.string.onboarding_nutrition_standard),
                genericChoice("high_protein", R.string.onboarding_nutrition_high_protein),
                genericChoice("vegetarian", R.string.onboarding_nutrition_vegetarian),
                genericChoice("vegan", R.string.onboarding_nutrition_vegan),
                genericChoice("low_carb", R.string.onboarding_nutrition_low_carb),
                genericChoice("flexible", R.string.onboarding_nutrition_flexible)
            ),
            workoutDays = listOf(
                valueChoice("mon", R.string.onboarding_day_mon),
                valueChoice("tue", R.string.onboarding_day_tue),
                valueChoice("wed", R.string.onboarding_day_wed),
                valueChoice("thu", R.string.onboarding_day_thu),
                valueChoice("fri", R.string.onboarding_day_fri),
                valueChoice("sat", R.string.onboarding_day_sat),
                valueChoice("sun", R.string.onboarding_day_sun)
            ),
            restrictions = listOf(
                valueChoice("lactose_free", R.string.onboarding_restriction_lactose_free),
                valueChoice("nut_allergy", R.string.onboarding_restriction_nut_allergy),
                valueChoice("avoid_seafood", R.string.onboarding_restriction_avoid_seafood)
            ),
            reminders = listOf(
                valueChoice("workout", R.string.onboarding_reminder_workout),
                valueChoice("meal", R.string.onboarding_reminder_meal),
                valueChoice("water", R.string.onboarding_reminder_water),
                valueChoice("sleep", R.string.onboarding_reminder_sleep)
            )
        )
    }

    fun homeBehaviorConfig(): HomeBehaviorConfig = HomeBehaviorConfig()

    fun trackBehaviorConfig(): TrackBehaviorConfig = TrackBehaviorConfig()

    fun quickWorkoutConfig(): QuickWorkoutConfig = QuickWorkoutConfig(
        preferredBodyPartOrder = listOf(
            "chest",
            "back",
            "shoulders",
            "upper arms",
            "lower arms",
            "waist",
            "upper legs",
            "lower legs",
            "cardio",
            "neck"
        )
    )

    fun practiceCategories(): List<PracticeCategoryContent> {
        return listOf(
            PracticeCategoryContent("chest", context.getString(R.string.plan_category_chest), listOf("chest"), "chest.png", "#E8DEF8", 0),
            PracticeCategoryContent("fullbody", context.getString(R.string.plan_category_full_body), listOf("chest", "back", "upper legs", "lower legs", "shoulders", "waist"), "fullbody.png", "#E8DEF8", 1),
            PracticeCategoryContent("shoulder", context.getString(R.string.plan_category_shoulder), listOf("shoulders"), "shoulder.png", "#E8DEF8", 2),
            PracticeCategoryContent("forearm", context.getString(R.string.plan_category_forearm), listOf("lower arms", "upper arms"), "forearm.png", "#E8DEF8", 3),
            PracticeCategoryContent("triceps", context.getString(R.string.plan_category_triceps), listOf("upper arms"), "triceps.png", "#E8DEF8", 4),
            PracticeCategoryContent("abdominal", context.getString(R.string.plan_category_abdominal), listOf("waist"), "abdominal.png", "#E8DEF8", 5),
            PracticeCategoryContent("leg", context.getString(R.string.plan_category_leg), listOf("upper legs", "lower legs"), "leg.png", "#E8DEF8", 6),
            PracticeCategoryContent("cardio", context.getString(R.string.plan_category_cardio), listOf("cardio"), "cardiac.png", "#E8DEF8", 7),
            PracticeCategoryContent("back", context.getString(R.string.plan_category_back), listOf("back"), "back.png", "#E8DEF8", 8),
            PracticeCategoryContent("warmup", context.getString(R.string.plan_category_warm_up), listOf("neck"), "warmup.png", "#E8DEF8", 9)
        )
    }

    fun exercisePrescriptions(): List<ExercisePrescriptionContent> {
        return listOf(
            ExercisePrescriptionContent(
                exerciseId = "push_up",
                rules = listOf(
                    ExercisePrescriptionRule(
                        goal = "gain_muscle",
                        sets = 4,
                        reps = "8-12",
                        targetWeightMode = StarterExerciseTemplate.TargetWeightMode.Bodyweight
                    ),
                    ExercisePrescriptionRule(
                        sets = 3,
                        reps = "10-12",
                        targetWeightMode = StarterExerciseTemplate.TargetWeightMode.Bodyweight
                    )
                )
            ),
            ExercisePrescriptionContent(
                exerciseId = "split_squat",
                rules = listOf(
                    ExercisePrescriptionRule(
                        sets = 3,
                        reps = "8-10/side",
                        targetWeightMode = StarterExerciseTemplate.TargetWeightMode.LowerBody
                    )
                )
            ),
            ExercisePrescriptionContent(
                exerciseId = "plank",
                rules = listOf(
                    ExercisePrescriptionRule(
                        sets = 3,
                        durationSeconds = 30
                    )
                )
            ),
            ExercisePrescriptionContent(
                exerciseId = "bodyweight_squat",
                rules = listOf(
                    ExercisePrescriptionRule(
                        sets = 3,
                        reps = "12-15",
                        targetWeightMode = StarterExerciseTemplate.TargetWeightMode.Bodyweight
                    )
                )
            ),
            ExercisePrescriptionContent(
                exerciseId = "incline_push_up",
                rules = listOf(
                    ExercisePrescriptionRule(
                        sets = 3,
                        reps = "10-12",
                        targetWeightMode = StarterExerciseTemplate.TargetWeightMode.UpperBody
                    )
                )
            ),
            ExercisePrescriptionContent(
                exerciseId = "cat_cow",
                rules = listOf(
                    ExercisePrescriptionRule(
                        sets = 2,
                        reps = "10"
                    )
                )
            ),
            ExercisePrescriptionContent(
                exerciseId = "worlds_greatest_stretch",
                rules = listOf(
                    ExercisePrescriptionRule(
                        sets = 2,
                        reps = "8/side"
                    )
                )
            ),
            ExercisePrescriptionContent(
                exerciseId = "dead_bug",
                rules = listOf(
                    ExercisePrescriptionRule(
                        sets = 3,
                        reps = "10/side"
                    )
                )
            )
        )
    }

    fun starterPlanTemplate(goal: String, language: String): StarterPlanTemplate {
        return when (goal.lowercase(Locale.US)) {
            "gain_muscle" -> gainMuscleStarter(language)
            "improve_flexibility" -> improveFlexibilityStarter(language)
            else -> defaultStarter(language, goal)
        }
    }

    private fun gainMuscleStarter(language: String): StarterPlanTemplate {
        val vi = language.isVietnamese()
        return StarterPlanTemplate(
            goal = "gain_muscle",
            planNameTemplate = if (vi) "{{goalLabel}} khởi động" else "{{goalLabel}} Starter Plan",
            trainingStyle = "strength",
            previewTitleTemplate = if (vi) "{{goalLabel}} khởi động" else "{{goalLabel}} Starter Plan",
            previewSubtitle = if (vi) {
                "Bản xem trước dựa trên hồ sơ onboarding hiện tại. Chi tiết cuối cùng được tạo sau khi kích hoạt kế hoạch."
            } else {
                "Preview based on your onboarding profile. Final workout details are generated after plan activation."
            },
            previewGoalTitle = if (vi) "Mục tiêu" else "Goal",
            previewGoalBodyTemplate = if (vi) "Mục tiêu hiện tại: {{goalLabel}}." else "Current goal: {{goalLabel}}.",
            previewCaloriesTitle = if (vi) "Mục tiêu calo" else "Calories target",
            previewCaloriesBodyTemplate = "{{caloriesTarget}}",
            previewWorkoutDaysTitle = if (vi) "Ngày tập" else "Workout days",
            previewWorkoutDaysBodyTemplate = "{{schedule}}",
            previewDurationTitle = if (vi) "Thời lượng" else "Planned duration",
            previewDurationBodyTemplate = "{{durationLabel}}",
            previewWhyTitle = if (vi) "Vì sao chọn kế hoạch này?" else "Why this preview?",
            previewWhyBodyTemplate = if (vi) {
                "Bản xem trước này phản ánh mức {{fitnessLabel}}, điều kiện tập {{equipmentLabel}} và thời điểm tập {{preferredTimeLabel}} của bạn."
            } else {
                "This preview reflects your current onboarding answers for {{fitnessLabel}}, {{equipmentLabel}}, and {{preferredTimeLabel}}."
            },
            scheduledWorkoutTitles = if (vi) {
                listOf("Đẩy thân trên", "Chân và trụ", "Sức mạnh nền tảng", "Phục hồi chủ động")
            } else {
                listOf("Upper Push Focus", "Legs + Core", "Strength Foundations", "Active Recovery")
            },
            explanationTemplate = if (vi) {
                "Được chọn theo mục tiêu {{goalLabel}}, trình độ {{fitnessLabel}} và khung giờ {{preferredTimeLabel}}."
            } else {
                "Selected for your {{goalLabel}} goal, {{fitnessLabel}} level, and {{preferredTimeLabel}} schedule."
            },
            exercises = listOf(
                StarterExerciseTemplate("push_up", "Push Up", 3, reps = "10", targetWeightMode = StarterExerciseTemplate.TargetWeightMode.Bodyweight),
                StarterExerciseTemplate("split_squat", "Split Squat", 3, reps = "10", targetWeightMode = StarterExerciseTemplate.TargetWeightMode.LowerBody),
                StarterExerciseTemplate("plank", "Plank", 3, durationSeconds = 30)
            )
        )
    }

    private fun improveFlexibilityStarter(language: String): StarterPlanTemplate {
        val vi = language.isVietnamese()
        return StarterPlanTemplate(
            goal = "improve_flexibility",
            planNameTemplate = if (vi) "{{goalLabel}} khởi động" else "{{goalLabel}} Starter Plan",
            trainingStyle = "mobility",
            previewTitleTemplate = if (vi) "{{goalLabel}} khởi động" else "{{goalLabel}} Starter Plan",
            previewSubtitle = if (vi) {
                "Bản xem trước dựa trên hồ sơ onboarding hiện tại. Chi tiết cuối cùng được tạo sau khi kích hoạt kế hoạch."
            } else {
                "Preview based on your onboarding profile. Final workout details are generated after plan activation."
            },
            previewGoalTitle = if (vi) "Mục tiêu" else "Goal",
            previewGoalBodyTemplate = if (vi) "Mục tiêu hiện tại: {{goalLabel}}." else "Current goal: {{goalLabel}}.",
            previewCaloriesTitle = if (vi) "Mục tiêu calo" else "Calories target",
            previewCaloriesBodyTemplate = "{{caloriesTarget}}",
            previewWorkoutDaysTitle = if (vi) "Ngày tập" else "Workout days",
            previewWorkoutDaysBodyTemplate = "{{schedule}}",
            previewDurationTitle = if (vi) "Thời lượng" else "Planned duration",
            previewDurationBodyTemplate = "{{durationLabel}}",
            previewWhyTitle = if (vi) "Vì sao chọn kế hoạch này?" else "Why this preview?",
            previewWhyBodyTemplate = if (vi) {
                "Bản xem trước này phản ánh mức {{fitnessLabel}}, điều kiện tập {{equipmentLabel}} và thời điểm tập {{preferredTimeLabel}} của bạn."
            } else {
                "This preview reflects your current onboarding answers for {{fitnessLabel}}, {{equipmentLabel}}, and {{preferredTimeLabel}}."
            },
            scheduledWorkoutTitles = if (vi) {
                listOf("Mở khớp cơ bản", "Giãn toàn thân", "Kiểm soát core", "Hồi phục linh hoạt")
            } else {
                listOf("Mobility Flow", "Full Body Stretch", "Core Control", "Flex Recovery")
            },
            explanationTemplate = if (vi) {
                "Được chọn theo mục tiêu {{goalLabel}}, trình độ {{fitnessLabel}} và khung giờ {{preferredTimeLabel}}."
            } else {
                "Selected for your {{goalLabel}} goal, {{fitnessLabel}} level, and {{preferredTimeLabel}} schedule."
            },
            exercises = listOf(
                StarterExerciseTemplate("cat_cow", "Cat Cow", 2, reps = "10"),
                StarterExerciseTemplate("worlds_greatest_stretch", "World's Greatest Stretch", 2, reps = "8"),
                StarterExerciseTemplate("dead_bug", "Dead Bug", 3, reps = "10")
            )
        )
    }

    private fun defaultStarter(language: String, goal: String): StarterPlanTemplate {
        val vi = language.isVietnamese()
        return StarterPlanTemplate(
            goal = goal,
            planNameTemplate = if (vi) "{{goalLabel}} khởi động" else "{{goalLabel}} Starter Plan",
            trainingStyle = "full_body",
            previewTitleTemplate = if (vi) "{{goalLabel}} khởi động" else "{{goalLabel}} Starter Plan",
            previewSubtitle = if (vi) {
                "Bản xem trước dựa trên hồ sơ onboarding hiện tại. Chi tiết cuối cùng được tạo sau khi kích hoạt kế hoạch."
            } else {
                "Preview based on your onboarding profile. Final workout details are generated after plan activation."
            },
            previewGoalTitle = if (vi) "Mục tiêu" else "Goal",
            previewGoalBodyTemplate = if (vi) "Mục tiêu hiện tại: {{goalLabel}}." else "Current goal: {{goalLabel}}.",
            previewCaloriesTitle = if (vi) "Mục tiêu calo" else "Calories target",
            previewCaloriesBodyTemplate = "{{caloriesTarget}}",
            previewWorkoutDaysTitle = if (vi) "Ngày tập" else "Workout days",
            previewWorkoutDaysBodyTemplate = "{{schedule}}",
            previewDurationTitle = if (vi) "Thời lượng" else "Planned duration",
            previewDurationBodyTemplate = "{{durationLabel}}",
            previewWhyTitle = if (vi) "Vì sao chọn kế hoạch này?" else "Why this preview?",
            previewWhyBodyTemplate = if (vi) {
                "Bản xem trước này phản ánh mức {{fitnessLabel}}, điều kiện tập {{equipmentLabel}} và thời điểm tập {{preferredTimeLabel}} của bạn."
            } else {
                "This preview reflects your current onboarding answers for {{fitnessLabel}}, {{equipmentLabel}}, and {{preferredTimeLabel}}."
            },
            scheduledWorkoutTitles = if (vi) {
                listOf("Toàn thân cơ bản", "Cardio và core", "Sức mạnh nền tảng", "Phục hồi vận động")
            } else {
                listOf("Full Body Basics", "Cardio + Core", "Strength Foundations", "Mobility Reset")
            },
            explanationTemplate = if (vi) {
                "Được chọn theo mục tiêu {{goalLabel}}, trình độ {{fitnessLabel}} và khung giờ {{preferredTimeLabel}}."
            } else {
                "Selected for your {{goalLabel}} goal, {{fitnessLabel}} level, and {{preferredTimeLabel}} schedule."
            },
            exercises = listOf(
                StarterExerciseTemplate("bodyweight_squat", "Bodyweight Squat", 3, reps = "12", targetWeightMode = StarterExerciseTemplate.TargetWeightMode.Bodyweight),
                StarterExerciseTemplate("incline_push_up", "Incline Push Up", 3, reps = "10", targetWeightMode = StarterExerciseTemplate.TargetWeightMode.UpperBody),
                StarterExerciseTemplate("marching_glute_bridge", "Marching Glute Bridge", 3, reps = "12", targetWeightMode = StarterExerciseTemplate.TargetWeightMode.Bodyweight)
            )
        )
    }

    private fun String.isVietnamese(): Boolean = lowercase(Locale.US).startsWith("vi")

    private fun choice(value: String, labelRes: Int, descriptionRes: Int): OnboardingChoiceContent {
        return OnboardingChoiceContent(
            value = value,
            label = context.getString(labelRes),
            description = context.getString(descriptionRes)
        )
    }

    private fun genericChoice(value: String, labelRes: Int): OnboardingChoiceContent {
        return OnboardingChoiceContent(
            value = value,
            label = context.getString(labelRes),
            description = context.getString(R.string.onboarding_choice_generic_desc)
        )
    }

    private fun valueChoice(value: String, labelRes: Int): OnboardingChoiceContent {
        return OnboardingChoiceContent(
            value = value,
            label = context.getString(labelRes)
        )
    }
}
