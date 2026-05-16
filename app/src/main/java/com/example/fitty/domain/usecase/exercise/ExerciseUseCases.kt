package com.example.fitty.domain.usecase.exercise

import com.example.fitty.domain.model.Exercise
import com.example.fitty.domain.model.ExerciseQuery
import com.example.fitty.domain.model.ExerciseSyncReport
import com.example.fitty.domain.model.ExerciseSyncState
import com.example.fitty.domain.repository.ExerciseCatalogRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveExercisesUseCase @Inject constructor(
    private val repository: ExerciseCatalogRepository
) {
    operator fun invoke(query: ExerciseQuery = ExerciseQuery()): Flow<List<Exercise>> =
        repository.observeExercises(query)
}

class ObserveExerciseSyncStateUseCase @Inject constructor(
    private val repository: ExerciseCatalogRepository
) {
    operator fun invoke(): Flow<ExerciseSyncState> = repository.observeSyncState()
}

class GetExerciseUseCase @Inject constructor(
    private val repository: ExerciseCatalogRepository
) {
    suspend operator fun invoke(exerciseId: String): Exercise? = repository.getExercise(exerciseId)
}

class SyncExercisesUseCase @Inject constructor(
    private val repository: ExerciseCatalogRepository
) {
    suspend operator fun invoke(force: Boolean = false): Result<ExerciseSyncReport> =
        repository.syncExercises(force)
}

class ToggleExerciseFavoriteUseCase @Inject constructor(
    private val repository: ExerciseCatalogRepository
) {
    suspend operator fun invoke(exerciseId: String, isFavorite: Boolean) =
        repository.updateFavorite(exerciseId, isFavorite)
}

class RecordRecentlyViewedExerciseUseCase @Inject constructor(
    private val repository: ExerciseCatalogRepository
) {
    suspend operator fun invoke(exerciseId: String) = repository.recordRecentlyViewed(exerciseId)
}

class GetRecentlyViewedExercisesUseCase @Inject constructor(
    private val repository: ExerciseCatalogRepository
) {
    suspend operator fun invoke(limit: Int = 10): List<Exercise> = repository.getRecentlyViewed(limit)
}
