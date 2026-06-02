package com.example.fitty.domain.model

data class WorkoutSession(
    val id: String = "",
    val planId: String = "",
    val scheduledWorkoutId: String = "",
    val title: String = "",
    val source: String = "plan",
    val status: String = "in_progress",
    val startedAt: Long = 0L,
    val endedAt: Long? = null,
    val durationMinutes: Int = 0,
    val caloriesBurned: Int = 0,
    val completionRate: Float = 0f,
    val perceivedEffort: Int? = null,
    val notes: String? = null,
    val plannedExercises: List<WorkoutExercise> = emptyList(),
    val exercises: List<ExerciseLog> = emptyList()
)

data class ExerciseLog(
    val id: String = "",
    val exerciseId: String = "",
    val name: String = "",
    val orderIndex: Int = 0,
    val plannedSets: Int = 0,
    val completedSets: Int = 0,
    val repsBySet: List<Int> = emptyList(),
    val weightKgBySet: List<Float> = emptyList(),
    val durationSeconds: Int? = null,
    val completed: Boolean = false
)
