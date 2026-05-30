package com.example.fitty.data.exercise

import com.example.fitty.data.local.exercise.ExerciseDao
import com.example.fitty.domain.model.Exercise
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

sealed interface GifDownloadState {
    data object Idle : GifDownloadState
    data class Running(val exerciseId: String, val progress: Float) : GifDownloadState
    data class Completed(val exerciseId: String, val localPath: String) : GifDownloadState
    data class Failed(val exerciseId: String, val message: String) : GifDownloadState
}

@Singleton
class ExerciseGifDownloadManager @Inject constructor(
    private val mediaDownloadManager: ExerciseMediaDownloadManager,
    private val exerciseDao: ExerciseDao
) : ExerciseGifDownloader {
    private val _state = MutableStateFlow<GifDownloadState>(GifDownloadState.Idle)
    val state: StateFlow<GifDownloadState> = _state.asStateFlow()

    override suspend fun download(exercise: Exercise): Result<String> = runCatching {
        if (exercise.localGifPath.isNotBlank()) {
            _state.value = GifDownloadState.Completed(exercise.id, exercise.localGifPath)
            return@runCatching exercise.localGifPath
        }
        _state.value = GifDownloadState.Running(exercise.id, 0f)
        val gif = mediaDownloadManager.downloadGif(exercise)
        if (gif.first.isBlank()) {
            throw IllegalStateException("GIF URL unavailable for ${exercise.name}")
        }
        exerciseDao.updateMediaState(
            exerciseId = exercise.id,
            localThumbnailPath = exercise.localThumbnailPath,
            localGifPath = gif.first,
            localVideoPath = exercise.localVideoPath,
            isDownloaded = true,
            mediaDownloadProgress = 1f,
            syncStatus = "gif_downloaded",
            updatedAt = Instant.now().toString()
        )
        _state.value = GifDownloadState.Completed(exercise.id, gif.first)
        gif.first
    }.onFailure { error ->
        _state.value = GifDownloadState.Failed(exercise.id, error.message ?: "GIF download failed")
    }
}
