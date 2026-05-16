package com.example.fitty.domain.repository

import com.example.fitty.domain.model.Exercise
import com.example.fitty.domain.model.ExerciseQuery
import com.example.fitty.domain.model.ExerciseSyncReport
import com.example.fitty.domain.model.ExerciseSyncState
import kotlinx.coroutines.flow.Flow

interface ExerciseCatalogRepository {
    fun observeExercises(query: ExerciseQuery = ExerciseQuery()): Flow<List<Exercise>>
    fun observeExercise(exerciseId: String): Flow<Exercise?>
    fun observeSyncState(): Flow<ExerciseSyncState>
    suspend fun getExercise(exerciseId: String): Exercise?
    suspend fun getExercises(query: ExerciseQuery = ExerciseQuery()): List<Exercise>
    suspend fun getRecentlyViewed(limit: Int = 10): List<Exercise>
    suspend fun upsertExercises(exercises: List<Exercise>)
    suspend fun syncExercises(force: Boolean = false): Result<ExerciseSyncReport>
    suspend fun updateFavorite(exerciseId: String, isFavorite: Boolean)
    suspend fun recordRecentlyViewed(exerciseId: String)
}
