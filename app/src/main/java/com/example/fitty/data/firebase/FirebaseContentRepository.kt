package com.example.fitty.data.firebase

import com.example.fitty.data.content.LocalContentFallbacks
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
import com.example.fitty.domain.repository.ContentRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseContentRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val localContentFallbacks: LocalContentFallbacks
) : ContentRepository {
    private val diagnostics = ConcurrentHashMap<String, Pair<Boolean, String?>>()

    override suspend fun getHomeContent(language: String): HomeContentConfig {
        val fallback = localContentFallbacks.home(language)
        return runCatching {
            val snapshot = firestore.collection(COLLECTION_APP_CONTENT).document(DOC_HOME).get().await()
            snapshot.toHomeContentConfig(language, fallback)
        }.onSuccess {
            recordDiagnostic(KEY_HOME_CONTENT, usedFallback = false)
        }.onFailure { error ->
            recordDiagnostic(KEY_HOME_CONTENT, usedFallback = true, detail = error.message)
        }.getOrDefault(fallback)
    }

    override suspend fun getCoachContent(language: String): CoachContentConfig {
        val fallback = localContentFallbacks.coach()
        return runCatching {
            val snapshot = firestore.collection(COLLECTION_APP_CONTENT).document(DOC_COACH).get().await()
            snapshot.toCoachContentConfig(language, fallback)
        }.onSuccess {
            recordDiagnostic(KEY_COACH_CONTENT, usedFallback = false)
        }.onFailure { error ->
            recordDiagnostic(KEY_COACH_CONTENT, usedFallback = true, detail = error.message)
        }.getOrDefault(fallback)
    }

    override suspend fun getOnboardingContent(language: String): OnboardingContentConfig {
        val fallback = localContentFallbacks.onboarding(language)
        return runCatching {
            val snapshot = firestore.collection(COLLECTION_APP_CONTENT).document(DOC_ONBOARDING).get().await()
            snapshot.toOnboardingContentConfig(language, fallback)
        }.onSuccess {
            recordDiagnostic(KEY_ONBOARDING_CONTENT, usedFallback = false)
        }.onFailure { error ->
            recordDiagnostic(KEY_ONBOARDING_CONTENT, usedFallback = true, detail = error.message)
        }.getOrDefault(fallback)
    }

    override suspend fun getHomeBehaviorConfig(): HomeBehaviorConfig {
        val fallback = localContentFallbacks.homeBehaviorConfig()
        return runCatching {
            val snapshot = firestore.collection(COLLECTION_APP_CONTENT)
                .document(DOC_BEHAVIOR)
                .collection(COLLECTION_ITEMS)
                .document(DOC_HOME_BEHAVIOR)
                .get()
                .await()
            snapshot.toHomeBehaviorConfig(fallback)
        }.onSuccess {
            recordDiagnostic(KEY_HOME_BEHAVIOR, usedFallback = false)
        }.onFailure { error ->
            recordDiagnostic(KEY_HOME_BEHAVIOR, usedFallback = true, detail = error.message)
        }.getOrDefault(fallback)
    }

    override suspend fun getTrackBehaviorConfig(): TrackBehaviorConfig {
        val fallback = localContentFallbacks.trackBehaviorConfig()
        return runCatching {
            val snapshot = firestore.collection(COLLECTION_APP_CONTENT)
                .document(DOC_BEHAVIOR)
                .collection(COLLECTION_ITEMS)
                .document(DOC_TRACK_BEHAVIOR)
                .get()
                .await()
            snapshot.toTrackBehaviorConfig(fallback)
        }.onSuccess {
            recordDiagnostic(KEY_TRACK_BEHAVIOR, usedFallback = false)
        }.onFailure { error ->
            recordDiagnostic(KEY_TRACK_BEHAVIOR, usedFallback = true, detail = error.message)
        }.getOrDefault(fallback)
    }

    override suspend fun getQuickWorkoutConfig(language: String): QuickWorkoutConfig {
        val fallback = localContentFallbacks.quickWorkoutConfig()
        return runCatching {
            val snapshot = firestore.collection(COLLECTION_APP_CONTENT)
                .document(DOC_BEHAVIOR)
                .collection(COLLECTION_ITEMS)
                .document(DOC_QUICK_WORKOUT_BEHAVIOR)
                .get()
                .await()
            snapshot.toQuickWorkoutConfig(language, fallback)
        }.onSuccess {
            recordDiagnostic(KEY_QUICK_WORKOUT_BEHAVIOR, usedFallback = false)
        }.onFailure { error ->
            recordDiagnostic(KEY_QUICK_WORKOUT_BEHAVIOR, usedFallback = true, detail = error.message)
        }.getOrDefault(fallback)
    }

    override suspend fun getPracticeCategories(language: String): List<PracticeCategoryContent> {
        val fallback = localContentFallbacks.practiceCategories()
        return runCatching {
            val documents = firestore.collection(COLLECTION_APP_CONTENT)
                .document(DOC_PRACTICE_CATEGORIES)
                .collection(COLLECTION_ITEMS)
                .get()
                .await()
                .documents
            val remote = documents.mapNotNull { document -> document.toPracticeCategory(language) }
                .sortedBy { it.order }
            if (remote.isEmpty()) {
                recordDiagnostic(KEY_PRACTICE_CATEGORIES, usedFallback = true, detail = "Remote collection empty")
                fallback
            } else {
                recordDiagnostic(KEY_PRACTICE_CATEGORIES, usedFallback = false)
                remote
            }
        }.onFailure { error ->
            recordDiagnostic(KEY_PRACTICE_CATEGORIES, usedFallback = true, detail = error.message)
        }.getOrDefault(fallback)
    }

    override suspend fun getExercisePrescriptions(language: String): List<ExercisePrescriptionContent> {
        val fallback = localContentFallbacks.exercisePrescriptions()
        return runCatching {
            val documents = firestore.collection(COLLECTION_APP_CONTENT)
                .document(DOC_EXERCISE_PRESCRIPTIONS)
                .collection(COLLECTION_ITEMS)
                .get()
                .await()
                .documents
            val remote = documents.mapNotNull { document -> document.toExercisePrescription(language) }
            if (remote.isEmpty()) {
                recordDiagnostic(KEY_EXERCISE_PRESCRIPTIONS, usedFallback = true, detail = "Remote collection empty")
                fallback
            } else {
                recordDiagnostic(KEY_EXERCISE_PRESCRIPTIONS, usedFallback = false)
                remote
            }
        }.onFailure { error ->
            recordDiagnostic(KEY_EXERCISE_PRESCRIPTIONS, usedFallback = true, detail = error.message)
        }.getOrDefault(fallback)
    }

    override suspend fun getStarterPlanTemplate(
        goal: String,
        fitnessLevel: String,
        equipment: String,
        language: String
    ): StarterPlanTemplate {
        val fallback = localContentFallbacks.starterPlanTemplate(goal, language)
        return runCatching {
            val documents = firestore.collection(COLLECTION_STARTER_PLAN_TEMPLATES)
                .get()
                .await()
                .documents
                .mapNotNull { it.toStarterPlanTemplate(language) }
            val exact = documents.firstOrNull { template ->
                template.goal.equals(goal, ignoreCase = true) &&
                    matches(template.fitnessLevels, fitnessLevel) &&
                    matches(template.equipments, equipment)
            }
            val goalOnly = documents.firstOrNull { template ->
                template.goal.equals(goal, ignoreCase = true)
            }
            val matched = exact ?: goalOnly
            if (matched == null) {
                recordDiagnostic(KEY_STARTER_PLAN_TEMPLATE, usedFallback = true, detail = "No remote template matched goal=$goal")
                fallback
            } else {
                recordDiagnostic(KEY_STARTER_PLAN_TEMPLATE, usedFallback = false)
                matched
            }
        }.onFailure { error ->
            recordDiagnostic(KEY_STARTER_PLAN_TEMPLATE, usedFallback = true, detail = error.message)
        }.getOrDefault(fallback)
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.toHomeContentConfig(
        language: String,
        fallback: HomeContentConfig
    ): HomeContentConfig {
        val translation = resolveTranslation(language)
        val emptyState = translation["emptyState"].asMap()
        return fallback.copy(
            emptyState = HomeEmptyStateContent(
                workoutTitle = emptyState.string("workoutTitle", fallback.emptyState.workoutTitle),
                workoutBody = emptyState.string("workoutBody", fallback.emptyState.workoutBody),
                insightMessage = emptyState.string("insightMessage", fallback.emptyState.insightMessage),
                achievementMessage = emptyState.string("achievementMessage", fallback.emptyState.achievementMessage)
            ),
            insightActions = translation.stringList("insightActions").ifEmpty { fallback.insightActions },
            suggestedTaskPresets = translation.templateList("suggestedTaskPresets", fallback.suggestedTaskPresets),
            defaultTaskTemplates = translation.templateList("defaultTaskTemplates", fallback.defaultTaskTemplates)
        )
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.toCoachContentConfig(
        language: String,
        fallback: CoachContentConfig
    ): CoachContentConfig {
        val translation = resolveTranslation(language)
        return CoachContentConfig(
            welcomeMessage = translation.string("welcomeMessage", fallback.welcomeMessage),
            promptChips = translation.stringList("promptChips").ifEmpty { fallback.promptChips }
        )
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.toOnboardingContentConfig(
        language: String,
        fallback: OnboardingContentConfig
    ): OnboardingContentConfig {
        val translation = resolveTranslation(language)
        return OnboardingContentConfig(
            stepTitles = translation.stringList("stepTitles").ifEmpty { fallback.stepTitles },
            goals = translation.choiceList("goals", fallback.goals),
            fitnessLevels = translation.choiceList("fitnessLevels", fallback.fitnessLevels),
            preferredTimes = translation.choiceList("preferredTimes", fallback.preferredTimes),
            durations = translation.choiceList("durations", fallback.durations),
            equipments = translation.choiceList("equipments", fallback.equipments),
            nutritionStyles = translation.choiceList("nutritionStyles", fallback.nutritionStyles),
            workoutDays = translation.choiceList("workoutDays", fallback.workoutDays),
            restrictions = translation.choiceList("restrictions", fallback.restrictions),
            reminders = translation.choiceList("reminders", fallback.reminders)
        )
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.toHomeBehaviorConfig(
        fallback: HomeBehaviorConfig
    ): HomeBehaviorConfig {
        return HomeBehaviorConfig(
            mealTargetPerDay = (getLong("mealTargetPerDay") ?: fallback.mealTargetPerDay.toLong()).toInt(),
            waterTargetMl = (getLong("waterTargetMl") ?: fallback.waterTargetMl.toLong()).toInt()
        )
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.toTrackBehaviorConfig(
        fallback: TrackBehaviorConfig
    ): TrackBehaviorConfig {
        return TrackBehaviorConfig(
            mealTargetPerDay = (getLong("mealTargetPerDay") ?: fallback.mealTargetPerDay.toLong()).toInt(),
            activeMinutesPerWorkout = (getLong("activeMinutesPerWorkout")
                ?: fallback.activeMinutesPerWorkout.toLong()).toInt()
        )
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.toQuickWorkoutConfig(
        language: String,
        fallback: QuickWorkoutConfig
    ): QuickWorkoutConfig {
        val translation = resolveTranslation(language)
        return QuickWorkoutConfig(
            targetExerciseCount = (getLong("targetExerciseCount") ?: fallback.targetExerciseCount.toLong()).toInt(),
            preferredBodyPartOrder = get("preferredBodyPartOrder").asStringList().ifEmpty { fallback.preferredBodyPartOrder },
            defaultDurationSeconds = (getLong("defaultDurationSeconds") ?: fallback.defaultDurationSeconds.toLong()).toInt(),
            defaultSets = (getLong("defaultSets") ?: fallback.defaultSets.toLong()).toInt(),
            caloriesPerMinute = translation.doubleOrNull("caloriesPerMinute")?.toFloat()
                ?: (getDouble("caloriesPerMinute")?.toFloat() ?: fallback.caloriesPerMinute)
        )
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.toPracticeCategory(language: String): PracticeCategoryContent? {
        val labels = get("labels").asMap()
        val requestedLanguage = normalizeLanguage(language)
        val resolvedLabel = if (requestedLanguage == "en") {
            labels.string("en", getString("label").orEmpty())
        } else {
            labels.string(requestedLanguage, "")
        }
            .ifBlank { return null }
        return PracticeCategoryContent(
            id = getString("id").orEmpty().ifBlank { id },
            label = resolvedLabel,
            bodyPartKeys = get("bodyPartKeys").asStringList(),
            assetImage = getString("assetImage").orEmpty(),
            cardColorHex = getString("cardColor").orEmpty(),
            order = (getLong("order") ?: 0L).toInt()
        )
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.toStarterPlanTemplate(language: String): StarterPlanTemplate? {
        val preview = get("preview").asMap()
        val planMeta = get("planMeta").asMap()
        val scheduled = get("scheduledWorkoutTemplates").asMap()
        if (preview.isEmpty() && planMeta.isEmpty() && scheduled.isEmpty()) return null
        val translations = get("translations").asMap()
        val normalizedLanguage = normalizeLanguage(language)
        val translation = translations[normalizedLanguage].asMap()
        if (translation.isEmpty() && normalizedLanguage != "en") return null
        val englishTranslation = if (normalizedLanguage == "en") translations["en"].asMap() else emptyMap<String, Any?>()
        val previewTranslation = translation["preview"].asMap().ifEmpty { englishTranslation["preview"].asMap() }
        val planTranslation = translation["planMeta"].asMap().ifEmpty { englishTranslation["planMeta"].asMap() }
        val scheduledTranslation = translation["scheduledWorkoutTemplates"].asMap().ifEmpty { englishTranslation["scheduledWorkoutTemplates"].asMap() }
        return StarterPlanTemplate(
            goal = getString("goal").orEmpty(),
            fitnessLevels = get("fitnessLevels").asStringList(),
            equipments = get("equipments").asStringList(),
            sourceProgramId = planMeta.string("sourceProgramId", "starter_template"),
            planNameTemplate = planTranslation.string("planNameTemplate", planMeta.string("planNameTemplate", "")),
            durationWeeks = (planMeta.long("durationWeeks", 4L)).toInt(),
            status = planMeta.string("status", "draft"),
            trainingStyle = planMeta.string("trainingStyle", ""),
            previewTitleTemplate = previewTranslation.string("titleTemplate", preview.string("titleTemplate", "")),
            previewSubtitle = previewTranslation.string("subtitle", preview.string("subtitle", "")),
            previewGoalTitle = previewTranslation.string("goalTitle", preview.string("goalTitle", "")),
            previewGoalBodyTemplate = previewTranslation.string("goalBodyTemplate", preview.string("goalBodyTemplate", "")),
            previewCaloriesTitle = previewTranslation.string("caloriesTitle", preview.string("caloriesTitle", "")),
            previewCaloriesBodyTemplate = previewTranslation.string("caloriesBodyTemplate", preview.string("caloriesBodyTemplate", "")),
            previewWorkoutDaysTitle = previewTranslation.string("workoutDaysTitle", preview.string("workoutDaysTitle", "")),
            previewWorkoutDaysBodyTemplate = previewTranslation.string("workoutDaysBodyTemplate", preview.string("workoutDaysBodyTemplate", "")),
            previewDurationTitle = previewTranslation.string("durationTitle", preview.string("durationTitle", "")),
            previewDurationBodyTemplate = previewTranslation.string("durationBodyTemplate", preview.string("durationBodyTemplate", "")),
            previewWhyTitle = previewTranslation.string("whyTitle", preview.string("whyTitle", "")),
            previewWhyBodyTemplate = previewTranslation.string("whyBodyTemplate", preview.string("whyBodyTemplate", "")),
            scheduledWorkoutTitles = scheduledTranslation.stringList("titles").ifEmpty { scheduled.stringList("titles") },
            explanationTemplate = scheduledTranslation.string("explanationTemplate", scheduled.string("explanationTemplate", "")),
            exercises = scheduled.exerciseTemplateList()
        )
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.toExercisePrescription(language: String): ExercisePrescriptionContent? {
        val exerciseId = getString("exerciseId").orEmpty().ifBlank { getString("id").orEmpty().ifBlank { id } }
        if (exerciseId.isBlank()) return null
        val translation = resolveTranslation(language)
        val rules = (get("rules") as? List<*>).orEmpty().mapNotNull { raw ->
            val map = raw.asMap()
            val sets = map.long("sets", 0L).toInt()
            if (sets <= 0) return@mapNotNull null
            ExercisePrescriptionRule(
                goal = map.string("goal", ""),
                fitnessLevels = map.stringList("fitnessLevels"),
                equipments = map.stringList("equipments"),
                minWeightKg = map.longOrNull("minWeightKg")?.toInt(),
                maxWeightKg = map.longOrNull("maxWeightKg")?.toInt(),
                sets = sets,
                reps = map.stringOrNull("reps"),
                durationSeconds = map.longOrNull("durationSeconds")?.toInt(),
                targetWeightMode = map.string("targetWeightMode", StarterExerciseTemplate.TargetWeightMode.None),
                fixedTargetWeightKg = map.doubleOrNull("fixedTargetWeightKg")?.toFloat(),
                bodyWeightMultiplier = map.doubleOrNull("bodyWeightMultiplier")?.toFloat(),
                minSuggestedWeightKg = map.doubleOrNull("minSuggestedWeightKg")?.toFloat(),
                maxSuggestedWeightKg = map.doubleOrNull("maxSuggestedWeightKg")?.toFloat()
            )
        }
        return ExercisePrescriptionContent(
            exerciseId = exerciseId,
            note = translation.string("note", ""),
            rules = rules
        )
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.resolveTranslation(language: String): Map<*, *> {
        val translations = get("translations").asMap()
        val normalized = normalizeLanguage(language)
        val requested = translations[normalized].asMap()
        return when {
            requested.isNotEmpty() -> requested
            normalized == "en" -> translations["en"].asMap()
            else -> emptyMap<String, Any?>()
        }
    }

    private fun Map<*, *>.templateList(
        key: String,
        fallback: List<HomeTaskTemplateContent>
    ): List<HomeTaskTemplateContent> {
        val raw = this[key] as? List<*> ?: return fallback
        val mapped = raw.mapNotNull { item ->
            val map = item.asMap()
            val category = map.string("category", "").toHomeTaskCategory() ?: return@mapNotNull null
            HomeTaskTemplateContent(
                id = map.string("id", ""),
                title = map.string("title", ""),
                description = map.string("description", ""),
                timeMinutes = map.long("timeMinutes", 0L).toInt(),
                category = category,
                reminderEnabled = map.boolean("reminderEnabled", true)
            )
        }
        return mapped.ifEmpty { fallback }
    }

    private fun Map<*, *>.choiceList(
        key: String,
        fallback: List<OnboardingChoiceContent>
    ): List<OnboardingChoiceContent> {
        val raw = this[key] as? List<*> ?: return fallback
        val mapped = raw.mapNotNull { entry ->
            val map = entry.asMap()
            val value = map.string("value", "")
            val label = map.string("label", "")
            if (value.isBlank() || label.isBlank()) return@mapNotNull null
            OnboardingChoiceContent(
                value = value,
                label = label,
                description = map.string("description", "")
            )
        }
        return mapped.ifEmpty { fallback }
    }

    private fun Map<*, *>.exerciseTemplateList(): List<StarterExerciseTemplate> {
        val exercises = this["exercises"] as? List<*> ?: return emptyList()
        return exercises.mapNotNull { raw ->
            val map = raw.asMap()
            val exerciseId = map.string("exerciseId", "")
            val name = map.string("name", "")
            val sets = map.long("sets", 0L).toInt()
            if (exerciseId.isBlank() || name.isBlank() || sets <= 0) return@mapNotNull null
            StarterExerciseTemplate(
                exerciseId = exerciseId,
                name = name,
                sets = sets,
                reps = map.stringOrNull("reps"),
                durationSeconds = map.longOrNull("durationSeconds")?.toInt(),
                targetWeightMode = map.string("targetWeightMode", StarterExerciseTemplate.TargetWeightMode.None)
            )
        }
    }

    private fun matches(allowed: List<String>, candidate: String): Boolean {
        return allowed.isEmpty() || allowed.any { it.equals(candidate, ignoreCase = true) }
    }

    override fun usedFallbackFor(key: String): Boolean = diagnostics[key]?.first ?: false

    override fun fallbackDetailFor(key: String): String? = diagnostics[key]?.second

    private fun recordDiagnostic(key: String, usedFallback: Boolean, detail: String? = null) {
        diagnostics[key] = usedFallback to detail
    }

    private fun normalizeLanguage(language: String): String {
        return language.lowercase(Locale.US).ifBlank { "en" }.substringBefore('-')
    }

    private fun Any?.asMap(): Map<*, *> = this as? Map<*, *> ?: emptyMap<String, Any?>()
    private fun Any?.asStringList(): List<String> = (this as? List<*>)?.mapNotNull { it as? String } ?: emptyList()

    private fun Map<*, *>.string(key: String, fallback: String): String {
        return (this[key] as? String)?.takeIf { it.isNotBlank() } ?: fallback
    }

    private fun Map<*, *>.stringOrNull(key: String): String? {
        return (this[key] as? String)?.takeIf { it.isNotBlank() }
    }

    private fun Map<*, *>.stringList(key: String): List<String> {
        return (this[key] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
    }

    private fun Map<*, *>.long(key: String, fallback: Long): Long {
        return (this[key] as? Number)?.toLong() ?: fallback
    }

    private fun Map<*, *>.longOrNull(key: String): Long? {
        return (this[key] as? Number)?.toLong()
    }

    private fun Map<*, *>.doubleOrNull(key: String): Double? {
        return (this[key] as? Number)?.toDouble()
    }

    private fun Map<*, *>.boolean(key: String, fallback: Boolean): Boolean {
        return this[key] as? Boolean ?: fallback
    }

    private fun String.toHomeTaskCategory(): HomeTaskCategory? = when (lowercase(Locale.US)) {
        "workout" -> HomeTaskCategory.Workout
        "meal" -> HomeTaskCategory.Meal
        "water" -> HomeTaskCategory.Water
        "custom" -> HomeTaskCategory.Custom
        else -> null
    }

    private companion object {
        const val KEY_HOME_CONTENT = "home_content"
        const val KEY_COACH_CONTENT = "coach_content"
        const val KEY_ONBOARDING_CONTENT = "onboarding_content"
        const val KEY_HOME_BEHAVIOR = "home_behavior"
        const val KEY_TRACK_BEHAVIOR = "track_behavior"
        const val KEY_QUICK_WORKOUT_BEHAVIOR = "quick_workout_behavior"
        const val KEY_PRACTICE_CATEGORIES = "practice_categories"
        const val KEY_EXERCISE_PRESCRIPTIONS = "exercise_prescriptions"
        const val KEY_STARTER_PLAN_TEMPLATE = "starter_plan_template"
        const val COLLECTION_APP_CONTENT = "app_content"
        const val DOC_HOME = "home"
        const val DOC_COACH = "coach"
        const val DOC_ONBOARDING = "onboarding"
        const val DOC_BEHAVIOR = "behavior"
        const val DOC_PRACTICE_CATEGORIES = "practice_categories"
        const val DOC_EXERCISE_PRESCRIPTIONS = "exercise_prescriptions"
        const val DOC_HOME_BEHAVIOR = "home"
        const val DOC_TRACK_BEHAVIOR = "track"
        const val DOC_QUICK_WORKOUT_BEHAVIOR = "quick_workout"
        const val COLLECTION_ITEMS = "items"
        const val COLLECTION_STARTER_PLAN_TEMPLATES = "starter_plan_templates"
    }
}
