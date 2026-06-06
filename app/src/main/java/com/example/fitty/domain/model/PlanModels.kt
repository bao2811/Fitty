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
    val nextWorkoutDate: String = "",
    val previewTitle: String = "",
    val previewSubtitle: String = "",
    val previewDetails: List<StarterPlanPreviewDetail> = emptyList(),
    val previewExercises: List<WorkoutExercise> = emptyList()
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
    val durationSeconds: Int? = null,
    val targetWeightKg: Float? = null
)

data class Exercise(
    val id: String = "",
    val name: String = "",
    val bodyPart: String = "",
    val target: String = "",
    val muscleGroup: String = "",
    val caloriesBurned: Int = 0,
    val durationSeconds: Int = 0,
    val description: String = "",
    val difficulty: String = "",
    val primaryMuscleGroup: String = "",
    val targetMuscles: List<String> = emptyList(),
    val equipment: String = "",
    val instructions: String = "",
    val thumbnailUrl: String = "",
    val thumbnailStoragePath: String = "",
    val gifUrl: String = "",
    val videoUrl: String = "",
    val gifStoragePath: String = "",
    val localThumbnailPath: String = "",
    val localGifPath: String = "",
    val localVideoPath: String = "",
    val gifVersion: Int = 0,
    val isDownloaded: Boolean = false,
    val isFavorite: Boolean = false,
    val remoteVersion: String = "",
    val updatedAt: String = "",
    val defaultRepsText: String = "",
    val defaultDurationSeconds: Int? = null,
    val mediaUrl: String = "",
    val mediaType: String = "gif",
    val videoDurationSeconds: Int? = null,
    val videoMimeType: String = "",
    val thumbnailMimeType: String = "",
    val gifMimeType: String = "",
    val syncStatus: String = "pending",
    val mediaDownloadProgress: Float = 0f,
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
