package com.example.fitty.domain.model

enum class AppNotificationType {
    General,
    Workout,
    Meal,
    Reminder,
    Streak,
    Progress
}

data class AppNotification(
    val id: Long = 0L,
    val title: String = "",
    val message: String = "",
    val type: AppNotificationType = AppNotificationType.General,
    val createdAt: Long = 0L,
    val isRead: Boolean = false
)
