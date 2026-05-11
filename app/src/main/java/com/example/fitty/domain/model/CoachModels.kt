package com.example.fitty.domain.model

data class CoachThread(
    val id: String = "",
    val title: String = "",
    val lastMessagePreview: String = "",
    val lastMessageAt: Long = 0L,
    val messageCount: Int = 0
)

data class CoachMessage(
    val id: String = "",
    val threadId: String = "",
    val role: String = "user",
    val text: String = "",
    val attachments: List<CoachAttachment> = emptyList(),
    val suggestions: List<CoachSuggestion> = emptyList(),
    val createdAt: Long = 0L
)

data class CoachAttachment(
    val type: String = "image",
    val url: String = "",
    val label: String = ""
)

sealed class CoachSuggestion {
    abstract val title: String
    abstract val actionLabel: String

    data class PlanAdjustment(
        override val title: String,
        override val actionLabel: String = "Apply to Plan",
        val targetPlanId: String = "",
        val moveFromDate: String = "",
        val moveToDate: String = ""
    ) : CoachSuggestion()

    data class MealIdea(
        override val title: String,
        override val actionLabel: String = "Save Meal Idea",
        val mealType: String = "",
        val description: String = "",
        val estimatedCalories: Int = 0,
        val estimatedProtein: Int = 0
    ) : CoachSuggestion()

    data class General(
        override val title: String,
        override val actionLabel: String = "Got it"
    ) : CoachSuggestion()
}

data class CoachContext(
    val userName: String = "",
    val goal: String = "",
    val fitnessLevel: String = "",
    val activePlanName: String = "",
    val todayWorkoutTitle: String = "",
    val currentStreak: Int = 0,
    val mealsLoggedToday: Int = 0,
    val latestWeight: Float? = null,
    val recentInsight: String = ""
)
