package com.example.fitty.data.exercise

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.fitty.data.local.FittyDatabase
import com.example.fitty.data.remote.exercise.ExerciseApiService
import com.example.fitty.data.remote.exercise.ExercisePageDto
import com.example.fitty.domain.model.Exercise
import com.example.fitty.domain.model.ExerciseQuery
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [35])
class OfflineFirstExerciseRepositoryTest {

    private lateinit var database: FittyDatabase
    private lateinit var repository: OfflineFirstExerciseRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        database = Room.inMemoryDatabaseBuilder(context, FittyDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val mediaDownloadManager = ExerciseMediaDownloadManager(
            apiService = FakeExerciseApiService(),
            fileStore = ExerciseMediaFileStore(context),
            firebaseStorage = FirebaseStorage.getInstance()
        )
        repository = OfflineFirstExerciseRepository(
            exerciseDao = database.exerciseDao(),
            syncStateDao = database.exerciseSyncStateDao(),
            historyDao = database.exerciseHistoryDao(),
            firestore = FirebaseFirestore.getInstance(),
            networkMonitor = NetworkMonitor(context),
            mediaDownloadManager = mediaDownloadManager
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `upsert and query maps exercises through room`() = runTest {
        repository.upsertExercises(
            listOf(
                exercise(id = "pushup", name = "Push Up", muscleGroup = "chest", isFavorite = true),
                exercise(id = "row", name = "Row", muscleGroup = "back"),
                exercise(id = "squat", name = "Squat", muscleGroup = "legs")
            )
        )

        val all = repository.observeExercises(ExerciseQuery(limit = 20)).first()
        val chest = repository.getExercises(ExerciseQuery(muscleGroup = "chest", limit = 20))

        assertEquals(listOf("pushup", "row", "squat"), all.map { it.id })
        assertEquals(listOf("pushup"), chest.map { it.id })
        assertTrue(all.first().isFavorite)
    }

    @Test
    fun `update favorite persists and observe exercise returns changed value`() = runTest {
        repository.upsertExercises(listOf(exercise(id = "row", name = "Row")))

        repository.updateFavorite("row", true)

        val updated = repository.observeExercise("row").first()
        assertTrue(updated?.isFavorite == true)
    }

    @Test
    fun `recently viewed returns existing exercises in history order`() = runTest {
        repository.upsertExercises(
            listOf(
                exercise(id = "pushup", name = "Push Up"),
                exercise(id = "row", name = "Row")
            )
        )

        repository.recordRecentlyViewed("pushup")
        repository.recordRecentlyViewed("missing")
        repository.recordRecentlyViewed("row")

        val recent = repository.getRecentlyViewed(limit = 5)
        assertEquals(listOf("row", "pushup"), recent.map { it.id })
    }

    @Test
    fun `observe sync state defaults to idle when no row exists`() = runTest {
        val state = repository.observeSyncState().first()

        assertFalse(state.isSyncing)
        assertEquals(0, state.totalExercises)
    }
}

private class FakeExerciseApiService : ExerciseApiService {
    override suspend fun getExercises(
        cursor: String?,
        pageSize: Int,
        updatedAfter: String?,
        search: String?,
        muscleGroup: String?,
        difficulty: String?
    ): ExercisePageDto = ExercisePageDto()

    override suspend fun downloadFile(fileUrl: String): ResponseBody {
        throw UnsupportedOperationException("Network download is not used by this test")
    }
}

private fun exercise(
    id: String,
    name: String = id,
    muscleGroup: String = "chest",
    bodyPart: String = muscleGroup,
    target: String = muscleGroup,
    isFavorite: Boolean = false,
    localThumbnailPath: String = "",
    thumbnailUrl: String = "https://example.com/thumb.jpg",
    gifUrl: String = "https://example.com/demo.gif"
): Exercise = Exercise(
    id = id,
    name = name,
    bodyPart = bodyPart,
    target = target,
    muscleGroup = muscleGroup,
    primaryMuscleGroup = muscleGroup,
    caloriesBurned = 60,
    durationSeconds = 45,
    difficulty = "beginner",
    equipment = "bodyweight",
    description = "Description",
    instructions = "Instructions",
    thumbnailUrl = thumbnailUrl,
    gifUrl = gifUrl,
    localThumbnailPath = localThumbnailPath,
    isFavorite = isFavorite,
    remoteVersion = "1",
    updatedAt = "2026-06-12T00:00:00Z",
    syncStatus = "synced"
)
