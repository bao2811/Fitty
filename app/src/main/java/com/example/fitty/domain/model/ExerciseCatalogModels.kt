package com.example.fitty.domain.model

data class ExerciseQuery(
    val searchQuery: String = "",
    val muscleGroup: String? = null,
    val difficulty: String? = null,
    val favoritesOnly: Boolean = false,
    val limit: Int = 50,
    val offset: Int = 0
)

data class ExerciseSyncState(
    val isSyncing: Boolean = false,
    val isOnline: Boolean = false,
    val lastSuccessfulSyncAt: String? = null,
    val lastAttemptedSyncAt: String? = null,
    val apiVersion: String? = null,
    val deltaToken: String? = null,
    val totalExercises: Int = 0,
    val downloadedImages: Int = 0,
    val downloadedGifs: Int = 0,
    val downloadedVideos: Int = 0,
    val progress: Float = 0f,
    val lastErrorMessage: String? = null
)

data class ExerciseSyncReport(
    val fetched: Int = 0,
    val inserted: Int = 0,
    val updated: Int = 0,
    val mediaDownloaded: Int = 0,
    val failedMediaDownloads: Int = 0,
    val nextDeltaToken: String? = null,
    val apiVersion: String? = null
)
