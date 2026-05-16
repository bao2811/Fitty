package com.example.fitty.data.exercise

import com.example.fitty.data.local.exercise.ExerciseDao
import com.example.fitty.domain.model.Exercise
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

sealed interface VideoDownloadState {
    data object Idle : VideoDownloadState
    data class Running(val exerciseId: String, val progress: Float) : VideoDownloadState
    data class Completed(val exerciseId: String, val localPath: String) : VideoDownloadState
    data class Failed(val exerciseId: String, val message: String) : VideoDownloadState
}

@Singleton
class ExerciseVideoDownloadManager @Inject constructor(
    private val mediaDownloadManager: ExerciseMediaDownloadManager,
    private val exerciseDao: ExerciseDao
) {
    private val _state = MutableStateFlow<VideoDownloadState>(VideoDownloadState.Idle)
    val state: StateFlow<VideoDownloadState> = _state.asStateFlow()

    suspend fun download(exercise: Exercise): Result<String> = runCatching {
        _state.value = VideoDownloadState.Running(exercise.id, 0f)
        val video = mediaDownloadManager.downloadVideo(exercise)
        if (video.first.isBlank()) {
            throw IllegalStateException("Video URL unavailable for ${exercise.name}")
        }
        exerciseDao.updateMediaState(
            exerciseId = exercise.id,
            localThumbnailPath = exercise.localThumbnailPath,
            localGifPath = exercise.localGifPath,
            localVideoPath = video.first,
            isDownloaded = true,
            mediaDownloadProgress = 1f,
            syncStatus = "video_downloaded",
            updatedAt = Instant.now().toString()
        )
        _state.value = VideoDownloadState.Completed(exercise.id, video.first)
        video.first
    }.onFailure { error ->
        _state.value = VideoDownloadState.Failed(exercise.id, error.message ?: "Video download failed")
    }

    suspend fun deleteDownloadedVideo(exercise: Exercise) {
        if (exercise.localVideoPath.isBlank()) return
        java.io.File(exercise.localVideoPath).delete()
        exerciseDao.updateMediaState(
            exerciseId = exercise.id,
            localThumbnailPath = exercise.localThumbnailPath,
            localGifPath = exercise.localGifPath,
            localVideoPath = "",
            isDownloaded = exercise.localThumbnailPath.isNotBlank(),
            mediaDownloadProgress = 0f,
            syncStatus = "video_removed",
            updatedAt = Instant.now().toString()
        )
        _state.value = VideoDownloadState.Idle
    }
}
