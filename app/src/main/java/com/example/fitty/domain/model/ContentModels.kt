package com.example.fitty.domain.model

data class HomeEmptyStateContent(
    val workoutTitle: String,
    val workoutBody: String,
    val insightMessage: String,
    val achievementMessage: String
)

data class HomeTaskTemplateContent(
    val id: String,
    val title: String,
    val description: String,
    val timeMinutes: Int,
    val category: HomeTaskCategory,
    val reminderEnabled: Boolean
)

data class HomeContentConfig(
    val emptyState: HomeEmptyStateContent,
    val insightActions: List<String>,
    val suggestedTaskPresets: List<HomeTaskTemplateContent>,
    val defaultTaskTemplates: List<HomeTaskTemplateContent>
)

data class CoachContentConfig(
    val welcomeMessage: String,
    val promptChips: List<String>
)

data class OnboardingChoiceContent(
    val value: String,
    val label: String,
    val description: String = ""
)

data class OnboardingContentConfig(
    val stepTitles: List<String>,
    val goals: List<OnboardingChoiceContent>,
    val fitnessLevels: List<OnboardingChoiceContent>,
    val preferredTimes: List<OnboardingChoiceContent>,
    val durations: List<OnboardingChoiceContent>,
    val equipments: List<OnboardingChoiceContent>,
    val nutritionStyles: List<OnboardingChoiceContent>,
    val workoutDays: List<OnboardingChoiceContent>,
    val restrictions: List<OnboardingChoiceContent>,
    val reminders: List<OnboardingChoiceContent>
)

data class HomeBehaviorConfig(
    val mealTargetPerDay: Int = 3,
    val waterTargetMl: Int = 2500
)

data class TrackBehaviorConfig(
    val mealTargetPerDay: Int = 3,
    val activeMinutesPerWorkout: Int = 30
)

data class QuickWorkoutConfig(
    val targetExerciseCount: Int = 8,
    val preferredBodyPartOrder: List<String> = emptyList(),
    val defaultDurationSeconds: Int = 30,
    val defaultSets: Int = 3,
    val caloriesPerMinute: Float = 5.5f
)

data class PracticeCategoryContent(
    val id: String,
    val label: String,
    val bodyPartKeys: List<String>,
    val assetImage: String,
    val cardColorHex: String = "",
    val order: Int = 0
)

data class ExercisePrescriptionRule(
    val goal: String = "",
    val fitnessLevels: List<String> = emptyList(),
    val equipments: List<String> = emptyList(),
    val minWeightKg: Int? = null,
    val maxWeightKg: Int? = null,
    val sets: Int,
    val reps: String? = null,
    val durationSeconds: Int? = null,
    val targetWeightMode: String = StarterExerciseTemplate.TargetWeightMode.None,
    val fixedTargetWeightKg: Float? = null,
    val bodyWeightMultiplier: Float? = null,
    val minSuggestedWeightKg: Float? = null,
    val maxSuggestedWeightKg: Float? = null
)

data class ExercisePrescriptionContent(
    val exerciseId: String,
    val note: String = "",
    val rules: List<ExercisePrescriptionRule> = emptyList()
)

data class ExercisePrescriptionRecommendation(
    val sets: Int,
    val reps: String? = null,
    val durationSeconds: Int? = null,
    val targetWeightKg: Float? = null,
    val targetWeightLabel: String? = null,
    val note: String = "",
    val debugSummary: String = ""
)

data class StarterExerciseTemplate(
    val exerciseId: String,
    val name: String,
    val sets: Int,
    val reps: String? = null,
    val durationSeconds: Int? = null,
    val targetWeightMode: String = TargetWeightMode.None
) {
    object TargetWeightMode {
        const val None = "none"
        const val Bodyweight = "bodyweight"
        const val UpperBody = "upper_body"
        const val LowerBody = "lower_body"
    }
}

data class StarterPlanTemplate(
    val goal: String = "",
    val fitnessLevels: List<String> = emptyList(),
    val equipments: List<String> = emptyList(),
    val sourceProgramId: String = "starter_template",
    val planNameTemplate: String = "",
    val durationWeeks: Int = 4,
    val status: String = "draft",
    val trainingStyle: String = "",
    val previewTitleTemplate: String = "",
    val previewSubtitle: String = "",
    val previewGoalTitle: String = "",
    val previewGoalBodyTemplate: String = "",
    val previewCaloriesTitle: String = "",
    val previewCaloriesBodyTemplate: String = "",
    val previewWorkoutDaysTitle: String = "",
    val previewWorkoutDaysBodyTemplate: String = "",
    val previewDurationTitle: String = "",
    val previewDurationBodyTemplate: String = "",
    val previewWhyTitle: String = "",
    val previewWhyBodyTemplate: String = "",
    val scheduledWorkoutTitles: List<String> = emptyList(),
    val explanationTemplate: String = "",
    val exercises: List<StarterExerciseTemplate> = emptyList()
)

data class StarterPlanProfile(
    val goal: String,
    val fitnessLevel: String,
    val equipment: String,
    val heightCm: Int? = null,
    val weightKg: Int? = null,
    val workoutDays: List<String> = emptyList(),
    val durationMinutes: Int? = null,
    val preferredTime: String = "",
    val language: String = "en"
)

data class StarterPlanPreviewDetail(
    val iconKey: String,
    val title: String,
    val body: String
)

data class StarterPlanPreviewContent(
    val title: String,
    val subtitle: String,
    val details: List<StarterPlanPreviewDetail>,
    val exercises: List<WorkoutExercise>
)

data class StarterPlanBuildResult(
    val plan: PlanInstance,
    val scheduledWorkouts: List<ScheduledWorkout>,
    val preview: StarterPlanPreviewContent
)
