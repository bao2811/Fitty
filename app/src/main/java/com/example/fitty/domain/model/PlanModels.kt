package com.example.fitty.domain.model

data class PlanInstance(
    val id: String = "",
    val sourceProgramId: String = "",
    val name: String = "",
    val goal: String = "",
    val durationWeeks: Int = 4,
    val workoutsPerWeek: Int = 3,
    val equipment: String = "",
    val trainingStyle: String = "",
    val status: String = "draft",
    val explanation: String = "",
    val currentWeek: Int = 1,
    val nextWorkoutDate: String = ""
)

data class ScheduledWorkout(
    val id: String = "",
    val planId: String = "",
    val dateKey: String = "",
    val weekNumber: Int = 1,
    val orderInWeek: Int = 1,
    val title: String = "",
    val durationMinutes: Int = 30,
    val estimatedCalories: Int = 0,
    val difficulty: String = "",
    val equipment: String = "",
    val status: String = "scheduled",
    val explanation: String = "",
    val replacedFromWorkoutId: String? = null,
    val exercises: List<WorkoutExercise> = emptyList()
)

data class WorkoutExercise(
    val exerciseId: String = "",
    val name: String = "",
    val sets: Int = 3,
    val reps: String? = null,
    val durationSeconds: Int? = null
)

data class Exercise(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val difficulty: String = "",
    val primaryMuscleGroup: String = "",
    val targetMuscles: List<String> = emptyList(),
    val equipment: String = "",
    val defaultRepsText: String = "",
    val defaultDurationSeconds: Int? = null,
    val mediaUrl: String = "",
    val mediaType: String = "gif",
    val steps: List<String> = emptyList(),
    val mistakes: List<String> = emptyList(),
    val tips: List<String> = emptyList(),
    val variations: List<String> = emptyList()
)

data class ProgramTemplate(
    val id: String = "",
    val title: String = "",
    val goal: String = "",
    val difficulty: String = "",
    val weeks: Int = 4,
    val workoutsPerWeek: Int = 3,
    val averageDurationMinutes: Int = 30,
    val equipment: String = "",
    val description: String = "",
    val thumbnailUrl: String? = null,
    val tags: List<String> = emptyList(),
    val explanationTemplate: String = ""
)
