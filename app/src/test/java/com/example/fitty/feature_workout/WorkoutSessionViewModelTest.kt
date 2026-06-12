package com.example.fitty.feature_workout

import androidx.test.core.app.ApplicationProvider
import com.example.fitty.data.content.ExercisePrescriptionResolver
import com.example.fitty.data.content.ExerciseWeightAdvisor
import com.example.fitty.data.content.LocalContentFallbacks
import com.example.fitty.data.exercise.ExerciseGifDownloader
import com.example.fitty.domain.model.BodyMeasurement
import com.example.fitty.domain.model.BodyScan
import com.example.fitty.domain.model.CoachContentConfig
import com.example.fitty.domain.model.DailySummary
import com.example.fitty.domain.model.Exercise
import com.example.fitty.domain.model.ExerciseLog
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
import com.example.fitty.domain.model.PlanInstance
import com.example.fitty.domain.model.PracticeCategoryContent
import com.example.fitty.domain.model.ProgramTemplate
import com.example.fitty.domain.model.ProgressStats
import com.example.fitty.domain.model.QuickWorkoutConfig
import com.example.fitty.domain.model.ScheduledWorkout
import com.example.fitty.domain.model.StarterPlanTemplate
import com.example.fitty.domain.model.TrackBehaviorConfig
import com.example.fitty.domain.repository.ContentRepository
import com.example.fitty.domain.repository.ExerciseCatalogRepository
import com.example.fitty.domain.repository.PlanRepository
import com.example.fitty.domain.repository.SessionRepository
import com.example.fitty.domain.repository.TrackingRepository
import com.example.fitty.domain.repository.UserRepository
import com.example.fitty.domain.repository.WorkoutSessionRepository
import com.example.fitty.domain.usecase.user.GetCurrentUserUseCase
import com.example.fitty.domain.usecase.user.UpdateStreakUseCase
import com.example.fitty.domain.usecase.workout.CompleteWorkoutSessionUseCase
import com.example.fitty.feature_track.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [35])
class WorkoutSessionViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `initialize loads quick workout exercises and marks first active`() = runTest {
        val viewModel = workoutHarness(
            exerciseRepository = FakeWorkoutExerciseRepository(
                listOf(
                    workoutExercise(id = "pushup", name = "Push Up", bodyPart = "chest"),
                    workoutExercise(id = "row", name = "Row", bodyPart = "back")
                )
            )
        ).viewModel()

        viewModel.initialize(sessionId = "")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoadingExercises)
        assertTrue(state.hasResolvedExercises)
        assertEquals(2, state.exerciseItems.size)
        assertEquals("Push Up", state.exerciseItems.first().exercise.name)
        assertTrue(state.exerciseItems.first().isActive)
    }

    @Test
    fun `set inputs keep only digits and one decimal for active exercise`() = runTest {
        val viewModel = workoutHarness(
            exerciseRepository = FakeWorkoutExerciseRepository(
                listOf(workoutExercise(id = "pushup", defaultRepsText = "10", bodyPart = "chest"))
            )
        ).viewModel()
        viewModel.initialize(sessionId = "")
        advanceUntilIdle()

        viewModel.updateSetReps(setIndex = 0, value = "1a2")
        viewModel.updateSetWeight(setIndex = 0, value = "7.5.3kg")

        val active = viewModel.uiState.value.activeExercise
        assertEquals("12", active?.repsBySetInput?.first())
        assertEquals("7.53", active?.weightKgBySetInput?.first())
    }

    @Test
    fun `start workout without session shows start error and does not run`() = runTest {
        val viewModel = workoutHarness(
            sessionRepository = FakeWorkoutSessionRepositoryProvider(uid = null),
            exerciseRepository = FakeWorkoutExerciseRepository(listOf(workoutExercise(id = "pushup")))
        ).viewModel()
        viewModel.initialize(sessionId = "")
        advanceUntilIdle()

        viewModel.startWorkout()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isRunning)
        assertNotNull(viewModel.uiState.value.error)
    }

    @Test
    fun `start workout repository failure shows error`() = runTest {
        val workoutRepository = FakeWorkoutSessionRepository(
            startResult = Result.failure(IllegalStateException("Start failed"))
        )
        val viewModel = workoutHarness(
            workoutRepository = workoutRepository,
            exerciseRepository = FakeWorkoutExerciseRepository(listOf(workoutExercise(id = "pushup")))
        ).viewModel()
        viewModel.initialize(sessionId = "")
        advanceUntilIdle()

        viewModel.startWorkout()
        advanceUntilIdle()

        assertEquals("Start failed", viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isRunning)
        assertEquals(1, workoutRepository.startedSessions.size)
    }

    @Test
    fun `finish without session id fails immediately`() = runTest {
        val viewModel = workoutHarness(
            exerciseRepository = FakeWorkoutExerciseRepository(listOf(workoutExercise(id = "pushup")))
        ).viewModel()
        viewModel.initialize(sessionId = "")
        advanceUntilIdle()

        viewModel.finishWorkout()

        assertFalse(viewModel.uiState.value.isSubmittingSession)
        assertNotNull(viewModel.uiState.value.error)
    }

    private fun workoutHarness(
        exerciseRepository: FakeWorkoutExerciseRepository = FakeWorkoutExerciseRepository(),
        sessionRepository: FakeWorkoutSessionRepositoryProvider = FakeWorkoutSessionRepositoryProvider(),
        workoutRepository: FakeWorkoutSessionRepository = FakeWorkoutSessionRepository(),
        userRepository: FakeWorkoutUserRepository = FakeWorkoutUserRepository(),
        planRepository: FakeWorkoutPlanRepository = FakeWorkoutPlanRepository(),
        trackingRepository: FakeWorkoutTrackingRepository = FakeWorkoutTrackingRepository(),
        contentRepository: ContentRepository = FakeWorkoutContentRepository()
    ): WorkoutHarness = WorkoutHarness(
        exerciseRepository,
        sessionRepository,
        workoutRepository,
        userRepository,
        planRepository,
        trackingRepository,
        contentRepository
    )
}

private class WorkoutHarness(
    private val exerciseRepository: FakeWorkoutExerciseRepository,
    private val sessionRepository: FakeWorkoutSessionRepositoryProvider,
    private val workoutRepository: FakeWorkoutSessionRepository,
    private val userRepository: FakeWorkoutUserRepository,
    private val planRepository: FakeWorkoutPlanRepository,
    private val trackingRepository: FakeWorkoutTrackingRepository,
    private val contentRepository: ContentRepository
) {
    fun viewModel(): WorkoutSessionViewModel {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        return WorkoutSessionViewModel(
            completeWorkoutSessionUseCase = CompleteWorkoutSessionUseCase(
                workoutSessionRepository = workoutRepository,
                planRepository = planRepository,
                trackingRepository = trackingRepository,
                userRepository = userRepository,
                sessionRepository = sessionRepository
            ),
            updateStreakUseCase = UpdateStreakUseCase(userRepository, sessionRepository),
            workoutSessionRepository = workoutRepository,
            sessionRepository = sessionRepository,
            exerciseRepository = exerciseRepository,
            planRepository = planRepository,
            contentRepository = contentRepository,
            getCurrentUserUseCase = GetCurrentUserUseCase(userRepository, sessionRepository),
            exercisePrescriptionResolver = ExercisePrescriptionResolver(ExerciseWeightAdvisor()),
            gifDownloadManager = FakeWorkoutGifDownloader(),
            context = context
        )
    }
}

private class FakeWorkoutExerciseRepository(
    initialExercises: List<Exercise> = listOf(workoutExercise(id = "pushup"))
) : ExerciseCatalogRepository {
    private val exercises = MutableStateFlow(initialExercises)

    override fun observeExercises(query: ExerciseQuery): Flow<List<Exercise>> = exercises
    override fun observeExercise(exerciseId: String): Flow<Exercise?> = MutableStateFlow(exercises.value.firstOrNull { it.id == exerciseId })
    override fun observeSyncState(): Flow<ExerciseSyncState> = MutableStateFlow(ExerciseSyncState())
    override suspend fun getExercise(exerciseId: String): Exercise? = exercises.value.firstOrNull { it.id == exerciseId }
    override suspend fun getExercises(query: ExerciseQuery): List<Exercise> = exercises.value
    override suspend fun getRecentlyViewed(limit: Int): List<Exercise> = emptyList()
    override suspend fun upsertExercises(exercises: List<Exercise>) {
        this.exercises.value = exercises
    }
    override suspend fun syncExercises(force: Boolean): Result<ExerciseSyncReport> = Result.success(ExerciseSyncReport())
    override suspend fun updateFavorite(exerciseId: String, isFavorite: Boolean) = Unit
    override suspend fun recordRecentlyViewed(exerciseId: String) = Unit
}

private class FakeWorkoutSessionRepositoryProvider(private val uid: String? = "uid") : SessionRepository {
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

private class FakeWorkoutSessionRepository(
    private val startResult: Result<String> = Result.success("session-1")
) : WorkoutSessionRepository {
    val startedSessions = mutableListOf<com.example.fitty.domain.model.WorkoutSession>()

    override suspend fun startSession(uid: String, session: com.example.fitty.domain.model.WorkoutSession): Result<String> {
        startedSessions += session
        return startResult
    }
    override suspend fun getSession(uid: String, sessionId: String): com.example.fitty.domain.model.WorkoutSession? =
        com.example.fitty.domain.model.WorkoutSession(id = sessionId, title = "Workout", exercises = startedSessions.lastOrNull()?.exercises.orEmpty())
    override suspend fun getActiveSessions(uid: String): List<com.example.fitty.domain.model.WorkoutSession> = emptyList()
    override suspend fun completeSession(
        uid: String,
        sessionId: String,
        durationMinutes: Int,
        caloriesBurned: Int,
        completionRate: Float,
        perceivedEffort: Int?,
        exercises: List<ExerciseLog>
    ): Result<Unit> = Result.success(Unit)
    override suspend fun updateExerciseLog(uid: String, sessionId: String, exercise: ExerciseLog): Result<Unit> = Result.success(Unit)
    override suspend fun abandonSession(uid: String, sessionId: String): Result<Unit> = Result.success(Unit)
    override suspend fun getRecentSessions(uid: String, limit: Int): List<com.example.fitty.domain.model.WorkoutSession> = emptyList()
}

private class FakeWorkoutUserRepository : UserRepository {
    override suspend fun getCurrentUser(uid: String?): FittyUser? = FittyUser(
        uid = uid ?: "uid",
        email = "fitty@example.com",
        displayName = "Fitty",
        username = "fitty",
        authProvider = "password",
        guest = false,
        onboardingCompleted = true,
        profile = FittyProfile(weightKg = 72),
        settings = FittySettings(language = "en")
    )
    override suspend fun updateProfile(uid: String, profile: FittyProfile): Result<Unit> = Result.success(Unit)
    override suspend fun updateOnboarding(uid: String, onboarding: FittyOnboarding): Result<Unit> = Result.success(Unit)
    override suspend fun updateSettings(uid: String, settings: FittySettings): Result<Unit> = Result.success(Unit)
    override suspend fun updateGoal(uid: String, primaryGoal: String, targetWeightKg: Int?): Result<Unit> = Result.success(Unit)
    override suspend fun updateStats(uid: String, stats: FittyStats): Result<Unit> = Result.success(Unit)
    override suspend fun deleteUserData(uid: String): Result<Unit> = Result.success(Unit)
    override suspend fun updateDisplayName(uid: String, name: String): Result<Unit> = Result.success(Unit)
    override suspend fun uploadProfilePhoto(uid: String, imageUri: String): Result<String> = Result.success(imageUri)
}

private class FakeWorkoutPlanRepository : PlanRepository {
    override suspend fun getActivePlan(uid: String): PlanInstance? = null
    override suspend fun getPlanInstance(uid: String, planId: String): PlanInstance? = null
    override suspend fun getAllPlans(uid: String): List<PlanInstance> = emptyList()
    override suspend fun savePlanInstance(uid: String, plan: PlanInstance): Result<String> = Result.success(plan.id)
    override suspend fun updatePlanStatus(uid: String, planId: String, status: String): Result<Unit> = Result.success(Unit)
    override suspend fun deletePlan(uid: String, planId: String): Result<Unit> = Result.success(Unit)
    override suspend fun getScheduledWorkouts(uid: String, planId: String, dateKey: String?): List<ScheduledWorkout> = emptyList()
    override suspend fun getScheduledWorkout(uid: String, planId: String, workoutId: String): ScheduledWorkout? = null
    override suspend fun saveScheduledWorkout(uid: String, planId: String, workout: ScheduledWorkout): Result<String> = Result.success(workout.id)
    override suspend fun updateScheduledWorkoutStatus(uid: String, planId: String, workoutId: String, status: String): Result<Unit> = Result.success(Unit)
    override suspend fun replaceScheduledWorkout(uid: String, planId: String, workoutId: String, newWorkout: ScheduledWorkout): Result<String> = Result.success(newWorkout.id)
    override suspend fun getExerciseLibrary(): List<Exercise> = emptyList()
    override suspend fun getExercise(exerciseId: String): Exercise? = null
    override suspend fun searchExercises(query: String, muscleGroup: String?, difficulty: String?, equipment: String?): List<Exercise> = emptyList()
    override suspend fun getProgramTemplates(goal: String?, difficulty: String?, equipment: String?): List<ProgramTemplate> = emptyList()
    override suspend fun getProgramTemplate(programId: String): ProgramTemplate? = null
}

private class FakeWorkoutTrackingRepository : TrackingRepository {
    override suspend fun saveMealLog(uid: String, mealLog: MealLog): Result<String> = Result.success("meal")
    override suspend fun getMealLogs(uid: String, dateKey: String): List<MealLog> = emptyList()
    override suspend fun getMealLog(uid: String, mealId: String): MealLog? = null
    override suspend fun deleteMealLog(uid: String, mealId: String): Result<Unit> = Result.success(Unit)
    override suspend fun saveMealScanRecord(uid: String, record: MealScanRecord): Result<String> = Result.success("scan")
    override suspend fun getMealScanHistory(uid: String, limit: Int): List<MealScanRecord> = emptyList()
    override suspend fun uploadScanImage(uid: String, localImageUri: String): Result<String> = Result.success(localImageUri)
    override suspend fun uploadBodyScanImage(uid: String, localImageUri: String): Result<String> = Result.success(localImageUri)
    override suspend fun saveBodyScan(uid: String, bodyScan: BodyScan): Result<String> = Result.success("body")
    override suspend fun getBodyScans(uid: String, limit: Int): List<BodyScan> = emptyList()
    override suspend fun getLatestBodyScan(uid: String): BodyScan? = null
    override suspend fun saveBodyMeasurement(uid: String, measurement: BodyMeasurement): Result<String> = Result.success("measurement")
    override suspend fun getBodyMeasurements(uid: String, limit: Int): List<BodyMeasurement> = emptyList()
    override suspend fun getDailySummary(uid: String, dateKey: String): DailySummary? = null
    override suspend fun getDailySummaries(uid: String, fromDate: String, toDate: String): List<DailySummary> = emptyList()
    override suspend fun updateDailySummary(uid: String, dateKey: String, summary: DailySummary): Result<Unit> = Result.success(Unit)
    override suspend fun getProgressStats(uid: String, days: Int): ProgressStats = ProgressStats()
}

private class FakeWorkoutContentRepository : ContentRepository {
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
    override suspend fun getStarterPlanTemplate(goal: String, fitnessLevel: String, equipment: String, language: String): StarterPlanTemplate? = null
}

private class FakeWorkoutGifDownloader : ExerciseGifDownloader {
    override suspend fun download(exercise: Exercise): Result<String> = Result.success(exercise.localGifPath)
}

private fun workoutExercise(
    id: String,
    name: String = id,
    bodyPart: String = "chest",
    defaultRepsText: String = "",
    caloriesBurned: Int = 60
): Exercise = Exercise(
    id = id,
    name = name,
    bodyPart = bodyPart,
    target = bodyPart,
    muscleGroup = bodyPart,
    primaryMuscleGroup = bodyPart,
    caloriesBurned = caloriesBurned,
    durationSeconds = 30,
    difficulty = "beginner",
    equipment = "bodyweight",
    description = "Description",
    instructions = "Instructions",
    thumbnailUrl = "https://example.com/thumb.jpg",
    gifUrl = "",
    defaultRepsText = defaultRepsText
)
