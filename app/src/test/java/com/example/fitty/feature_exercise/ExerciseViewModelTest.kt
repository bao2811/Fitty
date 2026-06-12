package com.example.fitty.feature_exercise

import androidx.lifecycle.SavedStateHandle
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.fitty.data.content.ExercisePrescriptionResolver
import com.example.fitty.data.content.ExerciseWeightAdvisor
import com.example.fitty.data.content.LocalContentFallbacks
import com.example.fitty.data.exercise.ExerciseGifDownloadManager
import com.example.fitty.data.exercise.ExerciseMediaDownloadManager
import com.example.fitty.data.exercise.ExerciseMediaFileStore
import com.example.fitty.data.exercise.ExerciseVideoDownloadManager
import com.example.fitty.data.local.FittyDatabase
import com.example.fitty.data.remote.exercise.ExerciseApiService
import com.example.fitty.data.remote.exercise.ExercisePageDto
import com.example.fitty.domain.model.BodyMeasurement
import com.example.fitty.domain.model.BodyScan
import com.example.fitty.domain.model.CoachContentConfig
import com.example.fitty.domain.model.DailySummary
import com.example.fitty.domain.model.Exercise
import com.example.fitty.domain.model.ExercisePrescriptionContent
import com.example.fitty.domain.model.ExerciseQuery
import com.example.fitty.domain.model.ExerciseSyncReport
import com.example.fitty.domain.model.ExerciseSyncState
import com.example.fitty.domain.model.FittyOnboarding
import com.example.fitty.domain.model.FittyProfile
import com.example.fitty.domain.model.FittySettings
import com.example.fitty.domain.model.FittyStartupState
import com.example.fitty.domain.model.FittyStats
import com.example.fitty.domain.model.FittyUser
import com.example.fitty.domain.model.HomeBehaviorConfig
import com.example.fitty.domain.model.HomeContentConfig
import com.example.fitty.domain.model.MealLog
import com.example.fitty.domain.model.MealScanRecord
import com.example.fitty.domain.model.OnboardingContentConfig
import com.example.fitty.domain.model.PracticeCategoryContent
import com.example.fitty.domain.model.ProgressStats
import com.example.fitty.domain.model.QuickWorkoutConfig
import com.example.fitty.domain.model.StarterPlanTemplate
import com.example.fitty.domain.model.TrackBehaviorConfig
import com.example.fitty.domain.repository.ContentRepository
import com.example.fitty.domain.repository.ExerciseCatalogRepository
import com.example.fitty.domain.repository.SessionRepository
import com.example.fitty.domain.repository.TrackingRepository
import com.example.fitty.domain.repository.UserRepository
import com.example.fitty.domain.usecase.exercise.GetExerciseUseCase
import com.example.fitty.domain.usecase.exercise.ObserveExerciseSyncStateUseCase
import com.example.fitty.domain.usecase.exercise.RecordRecentlyViewedExerciseUseCase
import com.example.fitty.domain.usecase.user.GetCurrentUserUseCase
import com.example.fitty.domain.usecase.user.UpdateStreakUseCase
import com.example.fitty.feature_plan.CategoryExerciseListViewModel
import com.example.fitty.feature_track.MainDispatcherRule
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [35])
class ExerciseViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var database: FittyDatabase

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            FittyDatabase::class.java
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `exercise detail loads exercise and records recently viewed`() = runTest {
        val repository = FakeExerciseRepository(listOf(testExercise(id = "pushup", name = "Push Up")))
        val viewModel = ExerciseDetailViewModel(
            savedStateHandle = SavedStateHandle(mapOf("exerciseId" to "pushup")),
            getExerciseUseCase = GetExerciseUseCase(repository),
            recordRecentlyViewedExerciseUseCase = RecordRecentlyViewedExerciseUseCase(repository),
            videoDownloadManager = videoDownloadManager(),
            contentRepository = FakeExerciseContentRepository(),
            sessionRepository = FakeExerciseSessionRepository(),
            getCurrentUserUseCase = GetCurrentUserUseCase(FakeExerciseUserRepository(), FakeExerciseSessionRepository()),
            exercisePrescriptionResolver = resolver(),
            context = ApplicationProvider.getApplicationContext()
        )

        advanceUntilIdle()

        assertEquals("Push Up", viewModel.uiState.value.exercise?.name)
        assertEquals(listOf("pushup"), repository.recordedRecentlyViewed)
    }

    @Test
    fun `video player loads exercise and records recently viewed`() = runTest {
        val repository = FakeExerciseRepository(listOf(testExercise(id = "row", name = "Row")))

        val viewModel = ExerciseVideoPlayerViewModel(
            savedStateHandle = SavedStateHandle(mapOf("exerciseId" to "row")),
            getExerciseUseCase = GetExerciseUseCase(repository),
            recordRecentlyViewedExerciseUseCase = RecordRecentlyViewedExerciseUseCase(repository)
        )
        advanceUntilIdle()

        assertEquals("Row", viewModel.exercise.value?.name)
        assertEquals(listOf("row"), repository.recordedRecentlyViewed)
    }

    @Test
    fun `category list filters by body part and search and selects item`() = runTest {
        val repository = FakeExerciseRepository(
            listOf(
                testExercise(id = "pushup", name = "Push Up", bodyPart = "chest", target = "chest"),
                testExercise(id = "row", name = "Row", bodyPart = "back", target = "back"),
                testExercise(id = "fly", name = "Chest Fly", bodyPart = "chest", target = "chest")
            )
        )
        val viewModel = categoryViewModel(repository, categoryLabel = "Chest", bodyPartKeys = "chest")

        advanceUntilIdle()

        assertEquals(listOf("fly", "pushup"), viewModel.uiState.value.exerciseItems.map { it.exercise.id }.sorted())

        viewModel.updateSearchQuery("fly")
        assertEquals(listOf("fly"), viewModel.uiState.value.filteredExerciseItems.map { it.exercise.id })

        viewModel.selectExercise(testExercise(id = "fly", name = "Chest Fly", bodyPart = "chest", target = "chest"))
        advanceUntilIdle()

        assertEquals("fly", viewModel.uiState.value.selectedExerciseId)
        assertNotNull(viewModel.uiState.value.selectedItem)
    }

    @Test
    fun `category mark completed updates daily summary once`() = runTest {
        val trackingRepository = FakeExerciseTrackingRepository()
        val userRepository = FakeExerciseUserRepository()
        val sessionRepository = FakeExerciseSessionRepository()
        val repository = FakeExerciseRepository(
            listOf(testExercise(id = "pushup", name = "Push Up", bodyPart = "chest", caloriesBurned = 80))
        )
        val viewModel = categoryViewModel(
            repository = repository,
            categoryLabel = "Chest",
            bodyPartKeys = "chest",
            trackingRepository = trackingRepository,
            userRepository = userRepository,
            sessionRepository = sessionRepository
        )
        advanceUntilIdle()

        viewModel.markExerciseCompleted("pushup", elapsedSeconds = 61)
        viewModel.markExerciseCompleted("pushup", elapsedSeconds = 61)
        advanceUntilIdle()

        assertEquals(setOf("pushup"), viewModel.uiState.value.completedExerciseIds)
        assertEquals(1, trackingRepository.updateCount)
        assertEquals(1, trackingRepository.updatedSummary?.progress?.workoutsCompleted)
        assertEquals(80, trackingRepository.updatedSummary?.progress?.caloriesBurned)
        assertNotNull(userRepository.updatedStats)
    }

    private fun categoryViewModel(
        repository: FakeExerciseRepository,
        categoryLabel: String,
        bodyPartKeys: String,
        trackingRepository: FakeExerciseTrackingRepository = FakeExerciseTrackingRepository(),
        userRepository: FakeExerciseUserRepository = FakeExerciseUserRepository(),
        sessionRepository: FakeExerciseSessionRepository = FakeExerciseSessionRepository()
    ): CategoryExerciseListViewModel = CategoryExerciseListViewModel(
        savedStateHandle = SavedStateHandle(
            mapOf(
                "categoryLabel" to categoryLabel,
                "bodyPartKeys" to bodyPartKeys
            )
        ),
        exerciseRepository = repository,
        observeExerciseSyncStateUseCase = ObserveExerciseSyncStateUseCase(repository),
        gifDownloadManager = ExerciseGifDownloadManager(mediaDownloadManager(), database.exerciseDao()),
        contentRepository = FakeExerciseContentRepository(),
        sessionRepository = sessionRepository,
        getCurrentUserUseCase = GetCurrentUserUseCase(userRepository, sessionRepository),
        exercisePrescriptionResolver = resolver(),
        trackingRepository = trackingRepository,
        updateStreakUseCase = UpdateStreakUseCase(userRepository, sessionRepository),
        context = ApplicationProvider.getApplicationContext()
    )

    private fun videoDownloadManager(): ExerciseVideoDownloadManager =
        ExerciseVideoDownloadManager(mediaDownloadManager(), database.exerciseDao())

    private fun mediaDownloadManager(): ExerciseMediaDownloadManager {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        return ExerciseMediaDownloadManager(
            apiService = FakeExerciseViewModelApiService(),
            fileStore = ExerciseMediaFileStore(context),
            firebaseStorage = FirebaseStorage.getInstance()
        )
    }

    private fun resolver(): ExercisePrescriptionResolver = ExercisePrescriptionResolver(ExerciseWeightAdvisor())
}

private class FakeExerciseRepository(initialExercises: List<Exercise>) : ExerciseCatalogRepository {
    private val exercises = MutableStateFlow(initialExercises)
    val recordedRecentlyViewed = mutableListOf<String>()

    override fun observeExercises(query: ExerciseQuery): Flow<List<Exercise>> = exercises.map { list ->
        list.filter { exercise ->
            (query.muscleGroup == null || exercise.muscleGroup == query.muscleGroup) &&
                (query.difficulty == null || exercise.difficulty == query.difficulty) &&
                (!query.favoritesOnly || exercise.isFavorite) &&
                (query.searchQuery.isBlank() || exercise.name.contains(query.searchQuery, ignoreCase = true))
        }.take(query.limit)
    }
    override fun observeExercise(exerciseId: String): Flow<Exercise?> = exercises.map { list -> list.firstOrNull { it.id == exerciseId } }
    override fun observeSyncState(): Flow<ExerciseSyncState> = MutableStateFlow(ExerciseSyncState(statusCode = "cached"))
    override suspend fun getExercise(exerciseId: String): Exercise? = exercises.value.firstOrNull { it.id == exerciseId }
    override suspend fun getExercises(query: ExerciseQuery): List<Exercise> = observeExercises(query).let { exercises.value }
    override suspend fun getRecentlyViewed(limit: Int): List<Exercise> = recordedRecentlyViewed.takeLast(limit).mapNotNull { getExercise(it) }
    override suspend fun upsertExercises(exercises: List<Exercise>) {
        this.exercises.value = exercises
    }
    override suspend fun syncExercises(force: Boolean): Result<ExerciseSyncReport> = Result.success(ExerciseSyncReport())
    override suspend fun updateFavorite(exerciseId: String, isFavorite: Boolean) {
        exercises.value = exercises.value.map { if (it.id == exerciseId) it.copy(isFavorite = isFavorite) else it }
    }
    override suspend fun recordRecentlyViewed(exerciseId: String) {
        recordedRecentlyViewed += exerciseId
    }
}

private class FakeExerciseContentRepository : ContentRepository {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val fallbacks = LocalContentFallbacks(context)

    override suspend fun getHomeContent(language: String): HomeContentConfig = fallbacks.home(language)
    override suspend fun getCoachContent(language: String): CoachContentConfig = fallbacks.coach()
    override suspend fun getOnboardingContent(language: String): OnboardingContentConfig = fallbacks.onboarding(language)
    override suspend fun getHomeBehaviorConfig(): HomeBehaviorConfig = fallbacks.homeBehaviorConfig()
    override suspend fun getTrackBehaviorConfig(): TrackBehaviorConfig = fallbacks.trackBehaviorConfig()
    override suspend fun getQuickWorkoutConfig(language: String): QuickWorkoutConfig = fallbacks.quickWorkoutConfig()
    override suspend fun getPracticeCategories(language: String): List<PracticeCategoryContent> = fallbacks.practiceCategories()
    override suspend fun getExercisePrescriptions(language: String): List<ExercisePrescriptionContent> = emptyList()
    override suspend fun getStarterPlanTemplate(
        goal: String,
        fitnessLevel: String,
        equipment: String,
        language: String
    ): StarterPlanTemplate? = null
}

private class FakeExerciseSessionRepository(private val uid: String? = "uid") : SessionRepository {
    override suspend fun saveStartupState(state: FittyStartupState) = Unit
    override suspend fun saveUserSession(user: FittyUser) = Unit
    override suspend fun setOnboardingCompleted(completed: Boolean) = Unit
    override suspend fun getCurrentUserId(): String? = uid
    override fun observeCurrentUserId(): Flow<String?> = MutableStateFlow(uid)
    override suspend fun getAppLanguage(): String? = "en"
    override suspend fun setAppLanguage(language: String) = Unit
    override suspend fun clearSession() = Unit
    override suspend fun isSignedInSessionExpired(nowMillis: Long, maxAgeMillis: Long): Boolean = false
    override suspend fun shouldShowWelcomeNotification(nowMillis: Long, cooldownMillis: Long): Boolean = false
    override suspend fun setLastWelcomeNotificationAt(timestampMillis: Long) = Unit
}

private class FakeExerciseUserRepository : UserRepository {
    var updatedStats: FittyStats? = null

    override suspend fun getCurrentUser(uid: String?): FittyUser? = FittyUser(
        uid = uid ?: "uid",
        email = "fitty@example.com",
        displayName = "Fitty",
        username = "fitty",
        authProvider = "password",
        guest = false,
        onboardingCompleted = true,
        settings = FittySettings(calorieTarget = 2200, waterGoalMl = 2500),
        stats = FittyStats()
    )
    override suspend fun updateProfile(uid: String, profile: FittyProfile): Result<Unit> = Result.success(Unit)
    override suspend fun updateOnboarding(uid: String, onboarding: FittyOnboarding): Result<Unit> = Result.success(Unit)
    override suspend fun updateSettings(uid: String, settings: FittySettings): Result<Unit> = Result.success(Unit)
    override suspend fun updateGoal(uid: String, primaryGoal: String, targetWeightKg: Int?): Result<Unit> = Result.success(Unit)
    override suspend fun updateStats(uid: String, stats: FittyStats): Result<Unit> {
        updatedStats = stats
        return Result.success(Unit)
    }
    override suspend fun deleteUserData(uid: String): Result<Unit> = Result.success(Unit)
    override suspend fun updateDisplayName(uid: String, name: String): Result<Unit> = Result.success(Unit)
    override suspend fun uploadProfilePhoto(uid: String, imageUri: String): Result<String> = Result.success(imageUri)
}

private class FakeExerciseTrackingRepository : TrackingRepository {
    var updatedSummary: DailySummary? = null
    var updateCount = 0

    override suspend fun saveMealLog(uid: String, mealLog: MealLog): Result<String> = Result.success("meal")
    override suspend fun getMealLogs(uid: String, dateKey: String): List<MealLog> = emptyList()
    override suspend fun getMealLog(uid: String, mealId: String): MealLog? = null
    override suspend fun deleteMealLog(uid: String, mealId: String): Result<Unit> = Result.success(Unit)
    override suspend fun saveMealScanRecord(uid: String, record: MealScanRecord): Result<String> = Result.success("scan")
    override suspend fun getMealScanHistory(uid: String, limit: Int): List<MealScanRecord> = emptyList()
    override suspend fun uploadScanImage(uid: String, localImageUri: String): Result<String> = Result.success(localImageUri)
    override suspend fun uploadBodyScanImage(uid: String, localImageUri: String): Result<String> = Result.success(localImageUri)
    override suspend fun saveBodyScan(uid: String, bodyScan: BodyScan): Result<String> = Result.success("scan")
    override suspend fun getBodyScans(uid: String, limit: Int): List<BodyScan> = emptyList()
    override suspend fun getLatestBodyScan(uid: String): BodyScan? = null
    override suspend fun saveBodyMeasurement(uid: String, measurement: BodyMeasurement): Result<String> = Result.success("measurement")
    override suspend fun getBodyMeasurements(uid: String, limit: Int): List<BodyMeasurement> = emptyList()
    override suspend fun getDailySummary(uid: String, dateKey: String): DailySummary? = null
    override suspend fun getDailySummaries(uid: String, fromDate: String, toDate: String): List<DailySummary> = emptyList()
    override suspend fun updateDailySummary(uid: String, dateKey: String, summary: DailySummary): Result<Unit> {
        updateCount++
        updatedSummary = summary
        assertEquals(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE), dateKey)
        return Result.success(Unit)
    }
    override suspend fun getProgressStats(uid: String, days: Int): ProgressStats = ProgressStats()
}

private class FakeExerciseViewModelApiService : ExerciseApiService {
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

private fun testExercise(
    id: String,
    name: String = id,
    bodyPart: String = "chest",
    target: String = bodyPart,
    muscleGroup: String = bodyPart,
    caloriesBurned: Int = 60,
    durationSeconds: Int = 45,
    videoUrl: String = "https://example.com/video.mp4",
    localVideoPath: String = ""
): Exercise = Exercise(
    id = id,
    name = name,
    bodyPart = bodyPart,
    target = target,
    muscleGroup = muscleGroup,
    primaryMuscleGroup = muscleGroup,
    caloriesBurned = caloriesBurned,
    durationSeconds = durationSeconds,
    difficulty = "beginner",
    equipment = "bodyweight",
    description = "Description",
    instructions = "Instructions",
    thumbnailUrl = "https://example.com/thumb.jpg",
    gifUrl = "",
    videoUrl = videoUrl,
    localVideoPath = localVideoPath,
    remoteVersion = "1",
    updatedAt = "2026-06-12T00:00:00Z"
)
