package com.example.fitty.feature_coach

import androidx.test.core.app.ApplicationProvider
import com.example.fitty.R
import com.example.fitty.data.content.LocalContentFallbacks
import com.example.fitty.domain.model.BodyMeasurement
import com.example.fitty.domain.model.BodyScan
import com.example.fitty.domain.model.CoachContentConfig
import com.example.fitty.domain.model.CoachContext
import com.example.fitty.domain.model.CoachEngine
import com.example.fitty.domain.model.CoachMessage
import com.example.fitty.domain.model.CoachSuggestion
import com.example.fitty.domain.model.CoachThread
import com.example.fitty.domain.model.DailySummary
import com.example.fitty.domain.model.Exercise
import com.example.fitty.domain.model.ExercisePrescriptionContent
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
import com.example.fitty.domain.repository.CoachRepository
import com.example.fitty.domain.repository.ContentRepository
import com.example.fitty.domain.repository.PlanRepository
import com.example.fitty.domain.repository.SessionRepository
import com.example.fitty.domain.repository.TrackingRepository
import com.example.fitty.domain.repository.UserRepository
import com.example.fitty.domain.usecase.coach.ApplyCoachSuggestionUseCase
import com.example.fitty.domain.usecase.coach.BuildCoachContextUseCase
import com.example.fitty.domain.usecase.coach.SendCoachMessageUseCase
import com.example.fitty.feature_track.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [35])
class CoachViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `init loads welcome and prompts from content repository`() = runTest {
        val viewModel = coachHarness(
            contentRepository = FakeCoachContentRepository(
                coachContent = CoachContentConfig("Remote welcome", listOf("Plan my workout", "Meal idea"))
            )
        ).viewModel()

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Remote welcome", state.messages.first().body)
        assertEquals(listOf("Plan my workout", "Meal idea"), state.prompts)
    }

    @Test
    fun `select prompt and update input update state`() = runTest {
        val viewModel = coachHarness().viewModel()

        viewModel.selectPrompt("Plan my workout")
        assertEquals("Plan my workout", viewModel.uiState.value.input)

        viewModel.updateInput("New message")
        assertEquals("New message", viewModel.uiState.value.input)
    }

    @Test
    fun `send blank message does not call engine`() = runTest {
        val engine = FakeCoachEngine()
        val viewModel = coachHarness(engine = engine).viewModel()

        viewModel.updateInput("   ")
        viewModel.sendMessage()
        advanceUntilIdle()

        assertEquals(0, engine.calls)
        assertNull(viewModel.uiState.value.lastSubmittedPrompt)
    }

    @Test
    fun `send success appends messages clears input sets thread and maps suggestions`() = runTest {
        val engine = FakeCoachEngine(
            response = CoachMessage(
                role = "assistant",
                text = "Try this meal",
                suggestions = listOf(
                    CoachSuggestion.MealIdea(
                        title = "Greek yogurt",
                        description = "Yogurt and fruit",
                        estimatedCalories = 250,
                        estimatedProtein = 22
                    )
                )
            )
        )
        val viewModel = coachHarness(engine = engine).viewModel()
        advanceUntilIdle()

        viewModel.updateInput("I need breakfast")
        viewModel.sendMessage()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("", state.input)
        assertEquals("I need breakfast", state.lastSubmittedPrompt)
        assertEquals("thread-1", state.threadId)
        assertTrue(state.messages.any { it.body == "I need breakfast" })
        assertTrue(state.messages.any { it.body == "Try this meal" })
        assertEquals(CoachSuggestionType.Meal, state.messages.last().suggestions.single().type)
        assertFalse(state.isSending)
    }

    @Test
    fun `send failure maps sign in error`() = runTest {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val viewModel = coachHarness(sessionRepository = FakeCoachSessionRepository(currentUserId = null)).viewModel()

        viewModel.updateInput("Help")
        viewModel.sendMessage()
        advanceUntilIdle()

        assertEquals(context.getString(R.string.coach_error_sign_in), viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isSending)
    }

    @Test
    fun `send failure maps quota error`() = runTest {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val engine = FakeCoachEngine(error = IllegalStateException("quota exceeded 429"))
        val viewModel = coachHarness(engine = engine).viewModel()

        viewModel.updateInput("Help")
        viewModel.sendMessage()
        advanceUntilIdle()

        assertEquals(context.getString(R.string.coach_error_quota), viewModel.uiState.value.error)
    }

    @Test
    fun `retry sends last submitted prompt and keeps it in input`() = runTest {
        val engine = FakeCoachEngine(response = CoachMessage(role = "assistant", text = "First"))
        val viewModel = coachHarness(engine = engine).viewModel()
        advanceUntilIdle()

        viewModel.updateInput("Retry this")
        viewModel.sendMessage()
        advanceUntilIdle()
        engine.response = CoachMessage(role = "assistant", text = "Second")

        viewModel.retryLastMessage()
        advanceUntilIdle()

        assertEquals(2, engine.calls)
        assertEquals("Retry this", engine.lastUserMessage)
        assertEquals("Retry this", viewModel.uiState.value.input)
        assertTrue(viewModel.uiState.value.messages.any { it.body == "Second" })
    }

    @Test
    fun `apply suggestion success appends confirmation`() = runTest {
        val trackingRepository = FakeCoachTrackingRepository()
        val viewModel = coachHarness(trackingRepository = trackingRepository).viewModel()
        val suggestion = mealSuggestionUi()

        viewModel.applySuggestion(suggestion)
        advanceUntilIdle()

        assertEquals(1, trackingRepository.savedMealLogs.size)
        assertFalse(viewModel.uiState.value.isApplying)
        assertTrue(viewModel.uiState.value.messages.last().body.contains(suggestion.title))
    }

    @Test
    fun `apply suggestion failure sets error`() = runTest {
        val viewModel = coachHarness(sessionRepository = FakeCoachSessionRepository(currentUserId = null)).viewModel()

        viewModel.applySuggestion(mealSuggestionUi())
        advanceUntilIdle()

        assertEquals("Not signed in", viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isApplying)
    }

    @Test
    fun `apply suggestion ignores second request while applying`() = runTest {
        val trackingRepository = FakeCoachTrackingRepository()
        val viewModel = coachHarness(trackingRepository = trackingRepository).viewModel()
        val suggestion = mealSuggestionUi()

        viewModel.applySuggestion(suggestion)
        viewModel.applySuggestion(suggestion)
        advanceUntilIdle()

        assertEquals(1, trackingRepository.savedMealLogs.size)
    }

    private fun coachHarness(
        coachRepository: FakeCoachRepository = FakeCoachRepository(),
        engine: FakeCoachEngine = FakeCoachEngine(),
        userRepository: FakeCoachUserRepository = FakeCoachUserRepository(),
        planRepository: FakeCoachPlanRepository = FakeCoachPlanRepository(),
        trackingRepository: FakeCoachTrackingRepository = FakeCoachTrackingRepository(),
        sessionRepository: FakeCoachSessionRepository = FakeCoachSessionRepository(),
        contentRepository: ContentRepository = FakeCoachContentRepository()
    ): CoachHarness = CoachHarness(
        coachRepository,
        engine,
        userRepository,
        planRepository,
        trackingRepository,
        sessionRepository,
        contentRepository
    )
}

private class CoachHarness(
    private val coachRepository: FakeCoachRepository,
    private val engine: FakeCoachEngine,
    private val userRepository: FakeCoachUserRepository,
    private val planRepository: FakeCoachPlanRepository,
    private val trackingRepository: FakeCoachTrackingRepository,
    private val sessionRepository: FakeCoachSessionRepository,
    private val contentRepository: ContentRepository
) {
    fun viewModel(): CoachViewModel {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val buildContext = BuildCoachContextUseCase(userRepository, planRepository, trackingRepository, sessionRepository)
        return CoachViewModel(
            sendCoachMessageUseCase = SendCoachMessageUseCase(coachRepository, engine, buildContext, sessionRepository),
            applyCoachSuggestionUseCase = ApplyCoachSuggestionUseCase(planRepository, trackingRepository, sessionRepository),
            localContentFallbacks = LocalContentFallbacks(context),
            contentRepository = contentRepository,
            sessionRepository = sessionRepository,
            context = context
        )
    }
}

private class FakeCoachEngine(
    var response: CoachMessage = CoachMessage(role = "assistant", text = "Coach response"),
    private val error: Throwable? = null
) : CoachEngine {
    var calls = 0
    var lastUserMessage: String? = null

    override suspend fun generateResponse(
        context: CoachContext,
        messages: List<CoachMessage>,
        userMessage: String
    ): CoachMessage {
        calls++
        lastUserMessage = userMessage
        error?.let { throw it }
        return response
    }
}

private class FakeCoachRepository : CoachRepository {
    private val thread = CoachThread(id = "thread-1", title = "Coach")
    private val messages = mutableListOf<CoachMessage>()

    override suspend fun getOrCreateThread(uid: String): CoachThread = thread
    override suspend fun getThread(uid: String, threadId: String): CoachThread? = thread.takeIf { it.id == threadId }
    override suspend fun getThreads(uid: String): List<CoachThread> = listOf(thread)
    override suspend fun getMessages(uid: String, threadId: String, limit: Int): List<CoachMessage> = messages.takeLast(limit)
    override suspend fun saveMessage(uid: String, threadId: String, message: CoachMessage): Result<String> {
        messages += message.copy(id = "message-${messages.size + 1}", threadId = threadId)
        return Result.success(messages.last().id)
    }
    override suspend fun updateThreadPreview(uid: String, threadId: String, preview: String, messageCount: Int): Result<Unit> = Result.success(Unit)
}

private class FakeCoachUserRepository(
    private val user: FittyUser? = coachUser()
) : UserRepository {
    override suspend fun getCurrentUser(uid: String?): FittyUser? = user
    override suspend fun updateProfile(uid: String, profile: FittyProfile): Result<Unit> = Result.success(Unit)
    override suspend fun updateOnboarding(uid: String, onboarding: FittyOnboarding): Result<Unit> = Result.success(Unit)
    override suspend fun updateSettings(uid: String, settings: FittySettings): Result<Unit> = Result.success(Unit)
    override suspend fun updateGoal(uid: String, primaryGoal: String, targetWeightKg: Int?): Result<Unit> = Result.success(Unit)
    override suspend fun updateStats(uid: String, stats: FittyStats): Result<Unit> = Result.success(Unit)
    override suspend fun deleteUserData(uid: String): Result<Unit> = Result.success(Unit)
    override suspend fun updateDisplayName(uid: String, name: String): Result<Unit> = Result.success(Unit)
    override suspend fun uploadProfilePhoto(uid: String, imageUri: String): Result<String> = Result.success(imageUri)
}

private class FakeCoachPlanRepository : PlanRepository {
    var savedWorkout: ScheduledWorkout? = null
    var updatedStatus: String? = null

    override suspend fun getActivePlan(uid: String): PlanInstance? = PlanInstance(id = "plan-1", name = "Starter")
    override suspend fun getPlanInstance(uid: String, planId: String): PlanInstance? = PlanInstance(id = planId, name = "Starter")
    override suspend fun getAllPlans(uid: String): List<PlanInstance> = emptyList()
    override suspend fun savePlanInstance(uid: String, plan: PlanInstance): Result<String> = Result.success(plan.id)
    override suspend fun updatePlanStatus(uid: String, planId: String, status: String): Result<Unit> = Result.success(Unit)
    override suspend fun deletePlan(uid: String, planId: String): Result<Unit> = Result.success(Unit)
    override suspend fun getScheduledWorkouts(uid: String, planId: String, dateKey: String?): List<ScheduledWorkout> =
        listOf(ScheduledWorkout(id = "workout-1", planId = planId, dateKey = dateKey.orEmpty(), title = "Workout"))
    override suspend fun getScheduledWorkout(uid: String, planId: String, workoutId: String): ScheduledWorkout? = null
    override suspend fun saveScheduledWorkout(uid: String, planId: String, workout: ScheduledWorkout): Result<String> {
        savedWorkout = workout
        return Result.success(workout.id)
    }
    override suspend fun updateScheduledWorkoutStatus(uid: String, planId: String, workoutId: String, status: String): Result<Unit> {
        updatedStatus = status
        return Result.success(Unit)
    }
    override suspend fun replaceScheduledWorkout(uid: String, planId: String, workoutId: String, newWorkout: ScheduledWorkout): Result<String> = Result.success(newWorkout.id)
    override suspend fun getExerciseLibrary(): List<Exercise> = emptyList()
    override suspend fun getExercise(exerciseId: String): Exercise? = null
    override suspend fun searchExercises(query: String, muscleGroup: String?, difficulty: String?, equipment: String?): List<Exercise> = emptyList()
    override suspend fun getProgramTemplates(goal: String?, difficulty: String?, equipment: String?): List<ProgramTemplate> = emptyList()
    override suspend fun getProgramTemplate(programId: String): ProgramTemplate? = null
}

private class FakeCoachTrackingRepository : TrackingRepository {
    val savedMealLogs = mutableListOf<MealLog>()

    override suspend fun saveMealLog(uid: String, mealLog: MealLog): Result<String> {
        savedMealLogs += mealLog
        return Result.success("meal-${savedMealLogs.size}")
    }
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

private class FakeCoachSessionRepository(private val currentUserId: String? = "uid") : SessionRepository {
    override suspend fun saveStartupState(state: FittyStartupState) = Unit
    override suspend fun saveUserSession(user: FittyUser) = Unit
    override suspend fun setOnboardingCompleted(completed: Boolean) = Unit
    override suspend fun getCurrentUserId(): String? = currentUserId
    override fun observeCurrentUserId(): Flow<String?> = MutableStateFlow(currentUserId)
    override suspend fun getAppLanguage(): String? = "en"
    override suspend fun setAppLanguage(language: String) = Unit
    override suspend fun clearSession() = Unit
    override suspend fun isSignedInSessionExpired(nowMillis: Long, maxAgeMillis: Long): Boolean = false
    override suspend fun shouldShowWelcomeNotification(nowMillis: Long, cooldownMillis: Long): Boolean = false
    override suspend fun setLastWelcomeNotificationAt(timestampMillis: Long) = Unit
}

private class FakeCoachContentRepository(
    private val coachContent: CoachContentConfig = CoachContentConfig("Welcome", listOf("Prompt"))
) : ContentRepository {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val fallbacks = LocalContentFallbacks(context)

    override suspend fun getHomeContent(language: String): HomeContentConfig = fallbacks.home(language)
    override suspend fun getCoachContent(language: String): CoachContentConfig = coachContent
    override suspend fun getOnboardingContent(language: String): OnboardingContentConfig = fallbacks.onboarding(language)
    override suspend fun getHomeBehaviorConfig(): HomeBehaviorConfig = fallbacks.homeBehaviorConfig()
    override suspend fun getTrackBehaviorConfig(): TrackBehaviorConfig = fallbacks.trackBehaviorConfig()
    override suspend fun getQuickWorkoutConfig(language: String): QuickWorkoutConfig = fallbacks.quickWorkoutConfig()
    override suspend fun getPracticeCategories(language: String): List<PracticeCategoryContent> = fallbacks.practiceCategories()
    override suspend fun getExercisePrescriptions(language: String): List<ExercisePrescriptionContent> = fallbacks.exercisePrescriptions()
    override suspend fun getStarterPlanTemplate(
        goal: String,
        fitnessLevel: String,
        equipment: String,
        language: String
    ): StarterPlanTemplate? = null
}

private fun mealSuggestionUi(): CoachSuggestionUi {
    val suggestion = CoachSuggestion.MealIdea(
        title = "Greek yogurt",
        actionLabel = "Save",
        mealType = "snack",
        description = "Yogurt and berries",
        estimatedCalories = 250,
        estimatedProtein = 20
    )
    return CoachSuggestionUi(
        type = CoachSuggestionType.Meal,
        title = suggestion.title,
        body = suggestion.description,
        action = suggestion.actionLabel,
        domainSuggestion = suggestion
    )
}

private fun coachUser(): FittyUser = FittyUser(
    uid = "uid",
    email = "fitty@example.com",
    displayName = "Fitty User",
    username = "fitty",
    authProvider = "password",
    guest = false,
    onboardingCompleted = true,
    profile = FittyProfile(primaryGoal = "lose_weight", fitnessLevel = "beginner", weightKg = 72),
    settings = FittySettings(language = "en"),
    stats = FittyStats(activePlanId = "plan-1", currentStreak = 4)
)
