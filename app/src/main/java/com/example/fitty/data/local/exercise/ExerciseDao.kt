package com.example.fitty.data.local.exercise

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {
    @Query(
        """
        SELECT * FROM exercises
        WHERE (:searchQuery = '' OR name LIKE '%' || :searchQuery || '%' OR muscleGroup LIKE '%' || :searchQuery || '%')
          AND (:muscleGroup IS NULL OR muscleGroup = :muscleGroup)
          AND (:difficulty IS NULL OR difficulty = :difficulty)
          AND (:favoritesOnly = 0 OR isFavorite = 1)
        ORDER BY isFavorite DESC, name COLLATE NOCASE ASC
        LIMIT :limit OFFSET :offset
        """
    )
    fun observeExercises(
        searchQuery: String,
        muscleGroup: String?,
        difficulty: String?,
        favoritesOnly: Boolean,
        limit: Int,
        offset: Int
    ): Flow<List<ExerciseEntity>>

    @Query(
        """
        SELECT * FROM exercises
        WHERE (:searchQuery = '' OR name LIKE '%' || :searchQuery || '%' OR muscleGroup LIKE '%' || :searchQuery || '%')
          AND (:muscleGroup IS NULL OR muscleGroup = :muscleGroup)
          AND (:difficulty IS NULL OR difficulty = :difficulty)
          AND (:favoritesOnly = 0 OR isFavorite = 1)
        ORDER BY isFavorite DESC, name COLLATE NOCASE ASC
        LIMIT :limit OFFSET :offset
        """
    )
    suspend fun getExercises(
        searchQuery: String,
        muscleGroup: String?,
        difficulty: String?,
        favoritesOnly: Boolean,
        limit: Int,
        offset: Int
    ): List<ExerciseEntity>

    @Query("SELECT * FROM exercises WHERE id = :exerciseId LIMIT 1")
    fun observeExercise(exerciseId: String): Flow<ExerciseEntity?>

    @Query("SELECT * FROM exercises WHERE id = :exerciseId LIMIT 1")
    suspend fun getExerciseById(exerciseId: String): ExerciseEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertExercises(exercises: List<ExerciseEntity>)

    @Query("UPDATE exercises SET isFavorite = :isFavorite WHERE id = :exerciseId")
    suspend fun updateFavorite(exerciseId: String, isFavorite: Boolean)

    @Query(
        """
        UPDATE exercises
        SET localThumbnailPath = :localThumbnailPath,
            localGifPath = :localGifPath,
            localVideoPath = :localVideoPath,
            isDownloaded = :isDownloaded,
            mediaDownloadProgress = :mediaDownloadProgress,
            syncStatus = :syncStatus,
            updatedAt = :updatedAt
        WHERE id = :exerciseId
        """
    )
    suspend fun updateMediaState(
        exerciseId: String,
        localThumbnailPath: String,
        localGifPath: String,
        localVideoPath: String,
        isDownloaded: Boolean,
        mediaDownloadProgress: Float,
        syncStatus: String,
        updatedAt: String
    )

    @Query("SELECT COUNT(*) FROM exercises")
    suspend fun countExercises(): Int

    @Query("SELECT DISTINCT muscleGroup FROM exercises WHERE muscleGroup != '' ORDER BY muscleGroup COLLATE NOCASE ASC")
    suspend fun getMuscleGroups(): List<String>
}

@Dao
interface ExerciseSyncStateDao {
    @Query("SELECT * FROM exercise_sync_state WHERE id = :syncId LIMIT 1")
    fun observeSyncState(syncId: String = EXERCISE_SYNC_STATE_ID): Flow<ExerciseSyncStateEntity?>

    @Query("SELECT * FROM exercise_sync_state WHERE id = :syncId LIMIT 1")
    suspend fun getSyncState(syncId: String = EXERCISE_SYNC_STATE_ID): ExerciseSyncStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSyncState(entity: ExerciseSyncStateEntity)
}

@Dao
interface ExerciseHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHistory(entity: ExerciseHistoryEntity)

    @Query("SELECT exerciseId FROM exercise_history ORDER BY lastViewedAt DESC LIMIT :limit")
    suspend fun getRecentlyViewedIds(limit: Int): List<String>
}
