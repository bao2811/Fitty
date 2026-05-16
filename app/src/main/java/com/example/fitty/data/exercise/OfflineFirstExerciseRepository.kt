package com.example.fitty.data.exercise

import com.example.fitty.data.local.exercise.EXERCISE_SYNC_STATE_ID
import com.example.fitty.data.local.exercise.ExerciseDao
import com.example.fitty.data.local.exercise.ExerciseEntity
import com.example.fitty.data.local.exercise.ExerciseHistoryDao
import com.example.fitty.data.local.exercise.ExerciseHistoryEntity
import com.example.fitty.data.local.exercise.ExerciseSyncStateDao
import com.example.fitty.data.local.exercise.ExerciseSyncStateEntity
import com.example.fitty.data.remote.exercise.ExerciseApiService
import com.example.fitty.data.remote.exercise.ExerciseDto
import com.example.fitty.domain.model.Exercise
import com.example.fitty.domain.model.ExerciseQuery
import com.example.fitty.domain.model.ExerciseSyncReport
import com.example.fitty.domain.model.ExerciseSyncState
import com.example.fitty.domain.repository.ExerciseCatalogRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OfflineFirstExerciseRepository @Inject constructor(
    private val exerciseDao: ExerciseDao,
    private val syncStateDao: ExerciseSyncStateDao,
    private val historyDao: ExerciseHistoryDao,
    private val apiService: ExerciseApiService,
    private val networkMonitor: NetworkMonitor,
    private val mediaDownloadManager: ExerciseMediaDownloadManager
) : ExerciseCatalogRepository {

    override fun observeExercises(query: ExerciseQuery): Flow<List<Exercise>> {
        return exerciseDao.observeExercises(
            searchQuery = query.searchQuery.trim(),
            muscleGroup = query.muscleGroup,
            difficulty = query.difficulty,
            favoritesOnly = query.favoritesOnly,
            limit = query.limit,
            offset = query.offset
        ).map { entities -> entities.map { it.toDomain() } }
    }

    override fun observeExercise(exerciseId: String): Flow<Exercise?> {
        return exerciseDao.observeExercise(exerciseId).map { it?.toDomain() }
    }

    override fun observeSyncState(): Flow<ExerciseSyncState> {
        return syncStateDao.observeSyncState().map { entity -> entity?.toDomain() ?: ExerciseSyncState() }
    }

    override suspend fun getExercise(exerciseId: String): Exercise? {
        return exerciseDao.getExerciseById(exerciseId)?.toDomain()
    }

    override suspend fun getExercises(query: ExerciseQuery): List<Exercise> {
        return exerciseDao.getExercises(
            searchQuery = query.searchQuery.trim(),
            muscleGroup = query.muscleGroup,
            difficulty = query.difficulty,
            favoritesOnly = query.favoritesOnly,
            limit = query.limit,
            offset = query.offset
        ).map { it.toDomain() }
    }

    override suspend fun getRecentlyViewed(limit: Int): List<Exercise> {
        val ids = historyDao.getRecentlyViewedIds(limit)
        return ids.mapNotNull { id -> exerciseDao.getExerciseById(id)?.toDomain() }
    }

    override suspend fun upsertExercises(exercises: List<Exercise>) {
        exerciseDao.upsertExercises(exercises.map { it.toEntity() })
    }

    override suspend fun syncExercises(force: Boolean): Result<ExerciseSyncReport> = runCatching {
        val online = networkMonitor.isOnline()
        val previousState = syncStateDao.getSyncState() ?: ExerciseSyncStateEntity()
        val startedAt = Instant.now().toString()
        syncStateDao.upsertSyncState(
            previousState.copy(
                isSyncing = true,
                isOnline = online,
                lastAttemptedSyncAt = startedAt,
                lastErrorMessage = null,
                progress = 0f
            )
        )

        if (!online) {
            throw IllegalStateException("No internet connection. Loading cached metadata from Room.")
        }

        var cursor: String? = null
        var fetched = 0
        var inserted = 0
        var updated = 0
        var mediaDownloaded = 0
        var failedMediaDownloads = 0
        var latestDeltaToken = previousState.deltaToken
        var apiVersion = previousState.apiVersion
        val updatedAfter = if (force) null else previousState.lastSuccessfulSyncAt
        val existingById = getAllExistingById()

        do {
            val page = apiService.getExercises(
                cursor = cursor,
                pageSize = PAGE_SIZE,
                updatedAfter = updatedAfter
            )
            fetched += page.items.size
            latestDeltaToken = page.deltaToken ?: latestDeltaToken
            apiVersion = page.apiVersion ?: apiVersion

            val mergedEntities = page.items.map { dto ->
                val current = existingById[dto.id]
                val base = dto.toDomain(current)
                if (current == null) inserted += 1 else updated += 1
                val withMedia = try {
                    val thumbnail = mediaDownloadManager.cacheThumbnail(base)
                    mediaDownloaded += thumbnail.second
                    base.copy(
                        localThumbnailPath = thumbnail.first,
                        isDownloaded = thumbnail.first.isNotBlank() || base.localVideoPath.isNotBlank(),
                        mediaDownloadProgress = if (base.thumbnailUrl.isBlank()) 0f else 1f,
                        syncStatus = "synced"
                    )
                } catch (_: Throwable) {
                    failedMediaDownloads += 1
                    base.copy(syncStatus = "metadata_synced", mediaDownloadProgress = 0f)
                }
                withMedia.toEntity()
            }

            if (mergedEntities.isNotEmpty()) {
                exerciseDao.upsertExercises(mergedEntities)
                mergedEntities.forEach { existingById[it.id] = it }
            }

            cursor = page.nextCursor
            syncStateDao.upsertSyncState(
                ExerciseSyncStateEntity(
                    id = EXERCISE_SYNC_STATE_ID,
                    isSyncing = true,
                    isOnline = true,
                    lastAttemptedSyncAt = startedAt,
                    apiVersion = apiVersion,
                    deltaToken = latestDeltaToken,
                    totalExercises = exerciseDao.countExercises(),
                    downloadedImages = mediaDownloaded,
                    downloadedGifs = 0,
                    downloadedVideos = 0,
                    progress = if (cursor == null) 1f else 0.5f,
                    lastErrorMessage = null
                )
            )
        } while (cursor != null)

        syncStateDao.upsertSyncState(
            ExerciseSyncStateEntity(
                id = EXERCISE_SYNC_STATE_ID,
                isSyncing = false,
                isOnline = true,
                lastSuccessfulSyncAt = Instant.now().toString(),
                lastAttemptedSyncAt = startedAt,
                apiVersion = apiVersion,
                deltaToken = latestDeltaToken,
                totalExercises = exerciseDao.countExercises(),
                downloadedImages = mediaDownloaded,
                downloadedGifs = 0,
                downloadedVideos = 0,
                progress = 1f,
                lastErrorMessage = null
            )
        )

        ExerciseSyncReport(
            fetched = fetched,
            inserted = inserted,
            updated = updated,
            mediaDownloaded = mediaDownloaded,
            failedMediaDownloads = failedMediaDownloads,
            nextDeltaToken = latestDeltaToken,
            apiVersion = apiVersion
        )
    }.onFailure { error ->
        val previousState = syncStateDao.getSyncState() ?: ExerciseSyncStateEntity()
        syncStateDao.upsertSyncState(
            previousState.copy(
                isSyncing = false,
                isOnline = networkMonitor.isOnline(),
                lastErrorMessage = error.message
            )
        )
    }

    override suspend fun updateFavorite(exerciseId: String, isFavorite: Boolean) {
        exerciseDao.updateFavorite(exerciseId, isFavorite)
    }

    override suspend fun recordRecentlyViewed(exerciseId: String) {
        historyDao.upsertHistory(ExerciseHistoryEntity(exerciseId = exerciseId, lastViewedAt = Instant.now().toString()))
    }

    private suspend fun getAllExistingById(): MutableMap<String, ExerciseEntity> {
        return exerciseDao.getExercises(
            searchQuery = "",
            muscleGroup = null,
            difficulty = null,
            favoritesOnly = false,
            limit = Int.MAX_VALUE,
            offset = 0
        ).associateBy { it.id }.toMutableMap()
    }

    private fun ExerciseEntity.toDomain(): Exercise = Exercise(
        id = id,
        name = name,
        bodyPart = bodyPart,
        target = target,
        muscleGroup = muscleGroup,
        caloriesBurned = caloriesBurned,
        durationSeconds = durationSeconds,
        description = description,
        difficulty = difficulty,
        primaryMuscleGroup = muscleGroup.ifBlank { bodyPart },
        targetMuscles = listOf(target).filter { it.isNotBlank() },
        equipment = equipment,
        instructions = instructions,
        thumbnailUrl = thumbnailUrl,
        gifUrl = gifUrl,
        videoUrl = videoUrl,
        localThumbnailPath = localThumbnailPath,
        localGifPath = localGifPath,
        localVideoPath = localVideoPath,
        gifVersion = gifVersion,
        isDownloaded = isDownloaded,
        isFavorite = isFavorite,
        remoteVersion = remoteVersion,
        updatedAt = updatedAt,
        mediaUrl = if (localThumbnailPath.isNotBlank()) localThumbnailPath else thumbnailUrl,
        mediaType = "image",
        syncStatus = syncStatus,
        mediaDownloadProgress = mediaDownloadProgress
    )

    private fun Exercise.toEntity(): ExerciseEntity = ExerciseEntity(
        id = id,
        name = name,
        muscleGroup = muscleGroup.ifBlank { primaryMuscleGroup.ifBlank { bodyPart } },
        bodyPart = bodyPart.ifBlank { muscleGroup.ifBlank { primaryMuscleGroup } },
        target = target,
        caloriesBurned = caloriesBurned,
        durationSeconds = durationSeconds,
        difficulty = difficulty,
        equipment = equipment,
        description = description,
        instructions = instructions.ifBlank { description },
        thumbnailUrl = thumbnailUrl,
        gifUrl = gifUrl,
        videoUrl = videoUrl,
        localThumbnailPath = localThumbnailPath,
        localGifPath = localGifPath,
        localVideoPath = localVideoPath,
        gifVersion = gifVersion,
        isDownloaded = isDownloaded,
        isFavorite = isFavorite,
        remoteVersion = remoteVersion,
        updatedAt = updatedAt,
        syncStatus = syncStatus,
        mediaDownloadProgress = mediaDownloadProgress
    )

    private fun ExerciseDto.toDomain(current: ExerciseEntity?): Exercise = Exercise(
        id = id,
        name = name,
        bodyPart = muscleGroup.orEmpty(),
        target = target.orEmpty(),
        muscleGroup = muscleGroup.orEmpty(),
        caloriesBurned = calories ?: 0,
        durationSeconds = duration ?: 0,
        description = description.orEmpty(),
        difficulty = difficulty.orEmpty(),
        primaryMuscleGroup = muscleGroup.orEmpty(),
        targetMuscles = listOfNotNull(target).filter { it.isNotBlank() },
        equipment = equipment.orEmpty(),
        instructions = instructions.orEmpty().ifBlank { description.orEmpty() },
        thumbnailUrl = thumbnailUrl.orEmpty(),
        gifUrl = current?.gifUrl.orEmpty(),
        videoUrl = videoUrl.orEmpty(),
        localThumbnailPath = current?.localThumbnailPath.orEmpty(),
        localGifPath = current?.localGifPath.orEmpty(),
        localVideoPath = current?.localVideoPath.orEmpty(),
        gifVersion = current?.gifVersion ?: 0,
        isDownloaded = current?.isDownloaded ?: false,
        isFavorite = current?.isFavorite ?: false,
        remoteVersion = version.orEmpty(),
        updatedAt = updatedAt.orEmpty(),
        syncStatus = "pending",
        mediaDownloadProgress = 0f
    )

    private fun ExerciseSyncStateEntity.toDomain(): ExerciseSyncState = ExerciseSyncState(
        isSyncing = isSyncing,
        isOnline = isOnline,
        lastSuccessfulSyncAt = lastSuccessfulSyncAt,
        lastAttemptedSyncAt = lastAttemptedSyncAt,
        apiVersion = apiVersion,
        deltaToken = deltaToken,
        totalExercises = totalExercises,
        downloadedImages = downloadedImages,
        downloadedGifs = downloadedGifs,
        downloadedVideos = downloadedVideos,
        progress = progress,
        lastErrorMessage = lastErrorMessage
    )

    private companion object {
        const val PAGE_SIZE = 100
    }
}
