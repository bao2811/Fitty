package com.example.fitty.domain.model

enum class HomeTaskCategory {
    Workout,
    Meal,
    Water,
    Custom
}

enum class HomeTaskStatus {
    Todo,
    InProgress,
    Completed
}

data class HomeTask(
    val id: Long = 0L,
    val title: String = "",
    val description: String = "",
    val dateKey: String = "",
    val timeMinutes: Int = 0,
    val category: HomeTaskCategory = HomeTaskCategory.Custom,
    val status: HomeTaskStatus = HomeTaskStatus.Todo,
    val reminderEnabled: Boolean = false,
    val isDefault: Boolean = false
)

data class HomeTaskDraft(
    val title: String,
    val description: String,
    val dateKey: String,
    val timeMinutes: Int,
    val category: HomeTaskCategory,
    val reminderEnabled: Boolean,
    val status: HomeTaskStatus = HomeTaskStatus.Todo,
    val isDefault: Boolean = false
)
