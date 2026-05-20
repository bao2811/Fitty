package com.example.fitty.feature_plan

import com.example.fitty.data.exercise.ExerciseGifDownloadManager
import com.example.fitty.domain.repository.ExerciseCatalogRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PracticeGifPrefetcher @Inject constructor(
    private val exerciseRepository: ExerciseCatalogRepository,
    private val gifDownloadManager: ExerciseGifDownloadManager
) {
    suspend fun ensureGifCached(exerciseId: String): Result<String> {
        val exercise = exerciseRepository.getExercise(exerciseId)
            ?: return Result.failure(IllegalStateException("Exercise $exerciseId not found"))
        if (exercise.localGifPath.isNotBlank()) {
            return Result.success(exercise.localGifPath)
        }
        return gifDownloadManager.download(exercise)
    }
}
