package com.example.fitty.data.local.exercise

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.fitty.data.local.FittyDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [35])
class ExerciseDaoTest {

    private lateinit var database: FittyDatabase
    private lateinit var exerciseDao: ExerciseDao
    private lateinit var syncStateDao: ExerciseSyncStateDao
    private lateinit var historyDao: ExerciseHistoryDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            FittyDatabase::class.java
        ).allowMainThreadQueries().build()
        exerciseDao = database.exerciseDao()
        syncStateDao = database.exerciseSyncStateDao()
        historyDao = database.exerciseHistoryDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `get exercises filters by search muscle difficulty and favorites then orders favorites first`() = runTest {
        exerciseDao.upsertExercises(
            listOf(
                exerciseEntity(id = "pushup", name = "Push Up", muscleGroup = "chest", difficulty = "beginner"),
                exerciseEntity(id = "row", name = "Row", muscleGroup = "back", difficulty = "intermediate", isFavorite = true),
                exerciseEntity(id = "pull", name = "Pull Down", muscleGroup = "back", difficulty = "beginner", isFavorite = true),
                exerciseEntity(id = "squat", name = "Squat", muscleGroup = "legs", difficulty = "beginner")
            )
        )

        val backFavorites = exerciseDao.getExercises(
            searchQuery = "",
            muscleGroup = "back",
            difficulty = null,
            favoritesOnly = true,
            limit = 20,
            offset = 0
        )
        val beginnerSearch = exerciseDao.getExercises(
            searchQuery = "pu",
            muscleGroup = null,
            difficulty = "beginner",
            favoritesOnly = false,
            limit = 20,
            offset = 0
        )

        assertEquals(listOf("pull", "row"), backFavorites.map { it.id })
        assertEquals(listOf("pull", "pushup"), beginnerSearch.map { it.id })
    }

    @Test
    fun `observe exercise emits updated favorite and media state`() = runTest {
        exerciseDao.upsertExercises(listOf(exerciseEntity(id = "pushup", name = "Push Up")))

        exerciseDao.updateFavorite("pushup", true)
        exerciseDao.updateMediaState(
            exerciseId = "pushup",
            localThumbnailPath = "/tmp/thumb.jpg",
            localGifPath = "/tmp/push.gif",
            localVideoPath = "/tmp/push.mp4",
            isDownloaded = true,
            mediaDownloadProgress = 1f,
            syncStatus = "downloaded",
            updatedAt = "2026-06-12T00:00:00Z"
        )

        val updated = exerciseDao.observeExercise("pushup").first()

        assertTrue(updated?.isFavorite == true)
        assertEquals("/tmp/push.gif", updated?.localGifPath)
        assertEquals("/tmp/push.mp4", updated?.localVideoPath)
        assertEquals("downloaded", updated?.syncStatus)
        assertEquals(1f, updated?.mediaDownloadProgress ?: 0f, 0.001f)
    }

    @Test
    fun `counts exercises missing preview media and returns sorted muscle groups`() = runTest {
        exerciseDao.upsertExercises(
            listOf(
                exerciseEntity(id = "a", muscleGroup = "legs", thumbnailUrl = "", gifUrl = ""),
                exerciseEntity(id = "b", muscleGroup = "back", thumbnailUrl = "https://thumb", gifUrl = ""),
                exerciseEntity(id = "c", muscleGroup = "back", thumbnailUrl = "", gifUrl = "https://gif"),
                exerciseEntity(id = "d", muscleGroup = "", thumbnailUrl = "", gifUrl = "")
            )
        )

        assertEquals(4, exerciseDao.countExercises())
        assertEquals(2, exerciseDao.countExercisesMissingPreviewMedia())
        assertEquals(listOf("back", "legs"), exerciseDao.getMuscleGroups())
    }

    @Test
    fun `sync state upsert replaces existing state`() = runTest {
        assertNull(syncStateDao.getSyncState())

        syncStateDao.upsertSyncState(ExerciseSyncStateEntity(isSyncing = true, totalExercises = 3, deltaToken = "a"))
        syncStateDao.upsertSyncState(ExerciseSyncStateEntity(isSyncing = false, totalExercises = 4, deltaToken = "b"))

        val state = syncStateDao.observeSyncState().first()
        assertFalse(state?.isSyncing == true)
        assertEquals(4, state?.totalExercises)
        assertEquals("b", state?.deltaToken)
    }

    @Test
    fun `history returns most recently viewed ids and replaces duplicate exercise`() = runTest {
        historyDao.upsertHistory(ExerciseHistoryEntity(exerciseId = "pushup", lastViewedAt = "2026-06-12T07:00:00Z"))
        historyDao.upsertHistory(ExerciseHistoryEntity(exerciseId = "row", lastViewedAt = "2026-06-12T08:00:00Z"))
        historyDao.upsertHistory(ExerciseHistoryEntity(exerciseId = "squat", lastViewedAt = "2026-06-12T09:00:00Z"))
        historyDao.upsertHistory(ExerciseHistoryEntity(exerciseId = "pushup", lastViewedAt = "2026-06-12T10:00:00Z"))

        assertEquals(listOf("pushup", "squat"), historyDao.getRecentlyViewedIds(limit = 2))
    }
}

private fun exerciseEntity(
    id: String,
    name: String = id,
    muscleGroup: String = "chest",
    bodyPart: String = muscleGroup,
    target: String = muscleGroup,
    caloriesBurned: Int = 60,
    durationSeconds: Int = 45,
    difficulty: String = "beginner",
    equipment: String = "bodyweight",
    description: String = "Description",
    instructions: String = "Instructions",
    thumbnailUrl: String = "https://example.com/thumb.jpg",
    thumbnailStoragePath: String = "",
    gifUrl: String = "https://example.com/demo.gif",
    gifStoragePath: String = "",
    videoUrl: String = "https://example.com/demo.mp4",
    localThumbnailPath: String = "",
    localGifPath: String = "",
    localVideoPath: String = "",
    gifVersion: Int = 1,
    isDownloaded: Boolean = false,
    isFavorite: Boolean = false,
    remoteVersion: String = "1",
    updatedAt: String = "2026-06-12T00:00:00Z",
    syncStatus: String = "synced",
    mediaDownloadProgress: Float = 0f
): ExerciseEntity = ExerciseEntity(
    id = id,
    name = name,
    muscleGroup = muscleGroup,
    bodyPart = bodyPart,
    target = target,
    caloriesBurned = caloriesBurned,
    durationSeconds = durationSeconds,
    difficulty = difficulty,
    equipment = equipment,
    description = description,
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
    syncStatus = syncStatus,
    mediaDownloadProgress = mediaDownloadProgress
)
