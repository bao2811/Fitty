package com.example.fitty.data.local.exercise

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exercises")
data class ExerciseEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "muscleGroup")
    val muscleGroup: String,
    @ColumnInfo(name = "bodyPart")
    val bodyPart: String,
    @ColumnInfo(name = "target")
    val target: String,
    @ColumnInfo(name = "caloriesBurned")
    val caloriesBurned: Int,
    @ColumnInfo(name = "durationSeconds")
    val durationSeconds: Int,
    @ColumnInfo(name = "difficulty")
    val difficulty: String,
    @ColumnInfo(name = "equipment")
    val equipment: String,
    @ColumnInfo(name = "description")
    val description: String,
    @ColumnInfo(name = "instructions")
    val instructions: String,
    @ColumnInfo(name = "thumbnailUrl")
    val thumbnailUrl: String,
    @ColumnInfo(name = "gifUrl")
    val gifUrl: String,
    @ColumnInfo(name = "videoUrl")
    val videoUrl: String,
    @ColumnInfo(name = "localThumbnailPath")
    val localThumbnailPath: String,
    @ColumnInfo(name = "localGifPath")
    val localGifPath: String,
    @ColumnInfo(name = "localVideoPath")
    val localVideoPath: String,
    @ColumnInfo(name = "gifVersion")
    val gifVersion: Int,
    @ColumnInfo(name = "isDownloaded")
    val isDownloaded: Boolean,
    @ColumnInfo(name = "isFavorite")
    val isFavorite: Boolean,
    @ColumnInfo(name = "remoteVersion")
    val remoteVersion: String,
    @ColumnInfo(name = "updatedAt")
    val updatedAt: String,
    @ColumnInfo(name = "syncStatus")
    val syncStatus: String,
    @ColumnInfo(name = "mediaDownloadProgress")
    val mediaDownloadProgress: Float
)

@Entity(tableName = "exercise_sync_state")
data class ExerciseSyncStateEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String = EXERCISE_SYNC_STATE_ID,
    @ColumnInfo(name = "isSyncing")
    val isSyncing: Boolean = false,
    @ColumnInfo(name = "isOnline")
    val isOnline: Boolean = false,
    @ColumnInfo(name = "lastSuccessfulSyncAt")
    val lastSuccessfulSyncAt: String? = null,
    @ColumnInfo(name = "lastAttemptedSyncAt")
    val lastAttemptedSyncAt: String? = null,
    @ColumnInfo(name = "apiVersion")
    val apiVersion: String? = null,
    @ColumnInfo(name = "deltaToken")
    val deltaToken: String? = null,
    @ColumnInfo(name = "totalExercises")
    val totalExercises: Int = 0,
    @ColumnInfo(name = "downloadedImages")
    val downloadedImages: Int = 0,
    @ColumnInfo(name = "downloadedGifs")
    val downloadedGifs: Int = 0,
    @ColumnInfo(name = "downloadedVideos")
    val downloadedVideos: Int = 0,
    @ColumnInfo(name = "progress")
    val progress: Float = 0f,
    @ColumnInfo(name = "lastErrorMessage")
    val lastErrorMessage: String? = null
)

const val EXERCISE_SYNC_STATE_ID = "exercise_catalog"
