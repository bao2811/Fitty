package com.example.fitty.data.exercise

import android.util.Log
import com.example.fitty.data.firebase.toExerciseFirestoreDocument
import com.example.fitty.data.local.exercise.EXERCISE_SYNC_STATE_ID
import com.example.fitty.data.local.exercise.ExerciseDao
import com.example.fitty.data.local.exercise.ExerciseEntity
import com.example.fitty.data.local.exercise.ExerciseHistoryDao
import com.example.fitty.data.local.exercise.ExerciseHistoryEntity
import com.example.fitty.data.local.exercise.ExerciseSyncStateDao
import com.example.fitty.data.local.exercise.ExerciseSyncStateEntity
import com.example.fitty.data.remote.exercise.ExerciseDto
import com.example.fitty.domain.model.Exercise
import com.example.fitty.domain.model.ExerciseQuery
import com.example.fitty.domain.model.ExerciseSyncReport
import com.example.fitty.domain.model.ExerciseSyncState
import com.example.fitty.domain.repository.ExerciseCatalogRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OfflineFirstExerciseRepository @Inject constructor(
    private val exerciseDao: ExerciseDao,
    private val syncStateDao: ExerciseSyncStateDao,
    private val historyDao: ExerciseHistoryDao,
    private val firestore: FirebaseFirestore,
    private val networkMonitor: NetworkMonitor,
    private val mediaDownloadManager: ExerciseMediaDownloadManager
) : ExerciseCatalogRepository {
    private companion object {
        const val TAG = "ExerciseSync"
    }

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
        val hasIncompletePreviewMedia = exerciseDao.countExercisesMissingPreviewMedia() > 0
        val shouldForceFullRefresh = force || hasIncompletePreviewMedia
        syncStateDao.upsertSyncState(
            previousState.copy(
                isSyncing = true,
                isOnline = online,
                lastAttemptedSyncAt = startedAt,
                statusCode = null,
                lastErrorMessage = null,
                progress = 0f
            )
        )

        if (!online) {
            syncStateDao.upsertSyncState(
                previousState.copy(
                    isSyncing = false,
                    isOnline = false,
                    lastAttemptedSyncAt = startedAt,
                    statusCode = ExerciseCatalogSyncPolicy.STATUS_OFFLINE_CACHED,
                    lastErrorMessage = "No internet connection. Loading cached metadata from Room."
                )
            )
            throw IllegalStateException("No internet connection. Loading cached metadata from Room.")
        }

        var fetched = 0
        var inserted = 0
        var updated = 0
        var mediaDownloaded = 0
        var failedMediaDownloads = 0
        var latestDeltaToken = previousState.deltaToken
        var apiVersion = previousState.apiVersion
        val existingById = getAllExistingById()
        val remoteDocuments = firestore.collection("exercises")
            .get()
            .await()
            .documents
            .mapNotNull { document -> document.toExerciseFirestoreDocument() }

        fetched = remoteDocuments.size
        apiVersion = "firestore"
        latestDeltaToken = if (shouldForceFullRefresh) null else latestDeltaToken

        var usable = 0
        var droppedMissingId = 0
        var droppedMissingName = 0
        var droppedMissingBodyPart = 0
        var droppedInvalidBodyPart = 0

        val normalizedExercises = remoteDocuments.mapNotNull { remote ->
            val result = ExerciseCatalogSyncPolicy.validate(remote)
            when (result.issue) {
                "missing_id" -> droppedMissingId += 1
                "missing_name" -> droppedMissingName += 1
                "missing_body_part" -> droppedMissingBodyPart += 1
                "invalid_body_part" -> droppedInvalidBodyPart += 1
            }
            result.normalized
        }.map { normalized ->
            usable += 1
            normalized.toDomain()
        }

        val validationSummary = ExerciseCatalogSyncPolicy.ValidationSummary(
            fetched = fetched,
            usable = usable,
            droppedMissingId = droppedMissingId,
            droppedMissingName = droppedMissingName,
            droppedMissingBodyPart = droppedMissingBodyPart,
            droppedInvalidBodyPart = droppedInvalidBodyPart
        )

        Log.i(
            TAG,
            "Exercise sync fetched=$fetched usable=$usable missingId=$droppedMissingId " +
                "missingName=$droppedMissingName missingBodyPart=$droppedMissingBodyPart invalidBodyPart=$droppedInvalidBodyPart"
        )

        if (validationSummary.usable == 0) {
            val statusCode = validationSummary.statusCode ?: ExerciseCatalogSyncPolicy.STATUS_EMPTY_REMOTE_DATA
            val message = when (statusCode) {
                ExerciseCatalogSyncPolicy.STATUS_INVALID_REMOTE_MAPPING ->
                    "Firebase exercises data exists but bodyPart values do not match supported categories."
                else ->
                    "Firebase exercises collection is empty or contains no usable exercise metadata."
            }
            syncStateDao.upsertSyncState(
                ExerciseSyncStateEntity(
                    id = EXERCISE_SYNC_STATE_ID,
                    isSyncing = false,
                    isOnline = true,
                    lastAttemptedSyncAt = startedAt,
                    apiVersion = apiVersion,
                    deltaToken = latestDeltaToken,
                    totalExercises = exerciseDao.countExercises(),
                    downloadedImages = 0,
                    downloadedGifs = 0,
                    downloadedVideos = 0,
                    progress = 1f,
                    statusCode = statusCode,
                    lastErrorMessage = message
                )
            )
            throw IllegalStateException(message)
        }

        val mergedEntities = normalizedExercises.map { remote ->
            val current = existingById[remote.id]
            val base = remote.mergeWithCurrent(current)
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
                progress = 1f,
                statusCode = ExerciseCatalogSyncPolicy.STATUS_SUCCESS,
                lastErrorMessage = null
            )
        )

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
                statusCode = ExerciseCatalogSyncPolicy.STATUS_SUCCESS,
                lastErrorMessage = null
            )
        )

        ExerciseSyncReport(
            fetched = fetched,
            inserted = inserted,
            updated = updated,
            usable = usable,
            droppedMissingId = droppedMissingId,
            droppedMissingName = droppedMissingName,
            droppedMissingBodyPart = droppedMissingBodyPart,
            droppedInvalidBodyPart = droppedInvalidBodyPart,
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
                statusCode = previousState.statusCode ?: if (networkMonitor.isOnline()) {
                    ExerciseCatalogSyncPolicy.STATUS_NETWORK_ERROR
                } else {
                    ExerciseCatalogSyncPolicy.STATUS_OFFLINE_CACHED
                },
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
        thumbnailStoragePath = thumbnailStoragePath,
        gifUrl = gifUrl,
        gifStoragePath = gifStoragePath,
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
        thumbnailStoragePath = thumbnailStoragePath,
        gifUrl = gifUrl,
        gifStoragePath = gifStoragePath,
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
        thumbnailUrl = thumbnailUrl.orEmpty().ifBlank { gifUrl.orEmpty() },
        thumbnailStoragePath = current?.thumbnailStoragePath.orEmpty(),
        gifUrl = gifUrl.orEmpty().ifBlank { current?.gifUrl.orEmpty() },
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

    private fun Exercise.mergeWithCurrent(current: ExerciseEntity?): Exercise = copy(
        muscleGroup = primaryMuscleGroup.ifBlank { current?.muscleGroup.orEmpty() },
        caloriesBurned = current?.caloriesBurned ?: caloriesBurned,
        durationSeconds = current?.durationSeconds ?: durationSeconds,
        difficulty = difficulty.ifBlank { current?.difficulty.orEmpty() },
        description = description.ifBlank { current?.description.orEmpty() },
        instructions = instructions.ifBlank { current?.instructions.orEmpty() },
        thumbnailUrl = thumbnailUrl.ifBlank { current?.thumbnailUrl.orEmpty() },
        thumbnailStoragePath = thumbnailStoragePath.ifBlank { current?.thumbnailStoragePath.orEmpty() },
        gifUrl = gifUrl.ifBlank { current?.gifUrl.orEmpty() },
        videoUrl = videoUrl.ifBlank { current?.videoUrl.orEmpty() },
        localThumbnailPath = current?.localThumbnailPath.orEmpty(),
        localGifPath = current?.localGifPath.orEmpty(),
        localVideoPath = current?.localVideoPath.orEmpty(),
        gifVersion = if (gifVersion == 0) current?.gifVersion ?: 0 else gifVersion,
        isDownloaded = current?.isDownloaded ?: false,
        isFavorite = current?.isFavorite ?: false,
        remoteVersion = current?.remoteVersion.orEmpty(),
        updatedAt = updatedAt.ifBlank { current?.updatedAt.orEmpty() },
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
        statusCode = statusCode,
        lastErrorMessage = lastErrorMessage
    )
}
