package com.example.fitty.feature_home

import androidx.test.core.app.ApplicationProvider
import com.example.fitty.data.content.LocalContentFallbacks
import com.example.fitty.domain.model.AppNotification
import com.example.fitty.domain.model.AppNotificationType
import com.example.fitty.domain.model.BodyMeasurement
import com.example.fitty.domain.model.BodyScan
import com.example.fitty.domain.model.CoachContentConfig
import com.example.fitty.domain.model.DailySummary
import com.example.fitty.domain.model.DailySummaryProgress
import com.example.fitty.domain.model.DailySummaryTargets
import com.example.fitty.domain.model.Exercise
import com.example.fitty.domain.model.ExercisePrescriptionContent
import com.example.fitty.domain.model.FittyOnboarding
import com.example.fitty.domain.model.FittyProfile
import com.example.fitty.domain.model.FittySettings
import com.example.fitty.domain.model.FittyStartupState
import com.example.fitty.domain.model.FittyStats
import com.example.fitty.domain.model.FittyUser
import com.example.fitty.domain.model.FoodItem
import com.example.fitty.domain.model.HomeBehaviorConfig
import com.example.fitty.domain.model.HomeContentConfig
import com.example.fitty.domain.model.HomeTask
import com.example.fitty.domain.model.HomeTaskCategory
import com.example.fitty.domain.model.HomeTaskDraft
import com.example.fitty.domain.model.HomeTaskStatus
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
import com.example.fitty.domain.repository.AppNotificationRepository
import com.example.fitty.domain.repository.ContentRepository
import com.example.fitty.domain.repository.HomeTaskRepository
import com.example.fitty.domain.repository.PlanRepository
import com.example.fitty.domain.repository.SessionRepository
import com.example.fitty.domain.repository.TrackingRepository
import com.example.fitty.domain.repository.UserRepository
import com.example.fitty.domain.usecase.home.GetHomeDashboardUseCase
import com.example.fitty.domain.usecase.user.GetCurrentUserUseCase
import com.example.fitty.domain.usecase.user.UpdateStreakUseCase
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
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [35])
class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val today: String = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)

    @Test
    fun `init loads user dashboard body metrics and nutrition`() = runTest {
        val user = homeUser(
            profile = FittyProfile(heightCm = 180, weightKg = 81, targetWeightKg = 75),
            settings = FittySettings(mealTargetPerDay = 3),
            stats = FittyStats(activePlanId = "plan-1")
        )
        val trackingRepository = FakeHomeTrackingRepository(
            dailySummary = DailySummary(
                dateKey = today,
                targets = DailySummaryTargets(calories = 2200, waterMl = 2500, workouts = 1),
                progress = DailySummaryProgress(workoutsCompleted = 1, waterMl = 1000, mealsLogged = 1),
                insightText = "Keep protein steady"
            ),
            mealLogs = listOf(MealLog(mealType = "breakfast", totalCalories = 450, totalProtein = 30, totalCarbs = 45, totalFat = 12))
        )
        val viewModel = homeHarness(
            userRepository = FakeHomeUserRepository(user),
            trackingRepository = trackingRepository,
            planRepository = FakeHomePlanRepository(
                plan = PlanInstance(id = "plan-1", name = "Starter Plan"),
                workouts = listOf(ScheduledWorkout(id = "workout-1", planId = "plan-1", title = "Upper Body", status = "scheduled"))
            )
        ).viewModel()

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("fitty", state.displayName)
        assertEquals(81f / (1.8f * 1.8f), state.bmi ?: 0f, 0.01f)
        assertTrue(state.focusMetrics.any { it.value == "1/1" })
        assertTrue(state.focusMetrics.any { it.value == "1/3" })
        assertTrue(state.nutrition.summary.contains("450"))
        assertNotNull(state.planWorkoutTask)
        assertEquals("Upper Body", state.planWorkoutTask?.title)
    }

    @Test
    fun `task flow maps observed tasks and writes repository operations`() = runTest {
        val taskRepository = FakeHomeTaskRepository(
            initialTasks = listOf(
                HomeTask(
                    id = 7L,
                    title = "Walk",
                    description = "20 minutes",
                    dateKey = today,
                    timeMinutes = 20,
                    category = HomeTaskCategory.Workout,
                    reminderEnabled = true
                )
            )
        )
        val viewModel = homeHarness(taskRepository = taskRepository).viewModel()
        advanceUntilIdle()

        assertEquals("Walk", viewModel.uiState.value.customTasks.single().title)

        viewModel.addTask("Water", "Drink", 5, HomeTaskCategory.Water, false)
        viewModel.setTaskStatus(7L, HomeTaskStatus.Completed)
        viewModel.toggleTaskReminder(7L, false)
        viewModel.deleteTask(7L)
        viewModel.setTaskStatus(0L, HomeTaskStatus.Completed)
        advanceUntilIdle()

        assertEquals("Water", taskRepository.addedTask?.title)
        assertEquals(7L to HomeTaskStatus.Completed, taskRepository.lastStatusUpdate)
        assertEquals(7L to false, taskRepository.lastReminderUpdate)
        assertEquals(7L, taskRepository.deletedTaskId)
    }

    @Test
    fun `notification flow maps unread count and actions`() = runTest {
        val notificationRepository = FakeHomeNotificationRepository(
            initialNotifications = listOf(
                AppNotification(id = 3L, title = "Meal", message = "Log lunch", type = AppNotificationType.Meal, isRead = false)
            ),
            initialUnreadCount = 1
        )
        val viewModel = homeHarness(notificationRepository = notificationRepository).viewModel()
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.unreadNotificationCount)
        assertEquals("Meal", viewModel.uiState.value.notifications.single().title)

        viewModel.toggleNotifications()
        assertTrue(viewModel.uiState.value.showNotifications)
        viewModel.dismissNotifications()
        assertFalse(viewModel.uiState.value.showNotifications)

        viewModel.markNotificationRead(3L)
        viewModel.markAllNotificationsRead()
        viewModel.deleteNotification(3L)
        advanceUntilIdle()

        assertEquals(3L, notificationRepository.markedReadId)
        assertTrue(notificationRepository.markedAllRead)
        assertEquals(3L, notificationRepository.deletedNotificationId)
    }

    @Test
    fun `update body metrics updates profile saves measurement and closes dialog`() = runTest {
        val userRepository = FakeHomeUserRepository(
            homeUser(profile = FittyProfile(heightCm = 170, weightKg = 70, targetWeightKg = 68))
        )
        val trackingRepository = FakeHomeTrackingRepository()
        val viewModel = homeHarness(userRepository = userRepository, trackingRepository = trackingRepository).viewModel()
        advanceUntilIdle()

        viewModel.toggleEditBodyDialog(true)
        viewModel.updateBodyMetrics(newHeightCm = 172, newWeightKg = 72, newTargetWeightKg = 67)
        advanceUntilIdle()

        assertEquals(172, userRepository.updatedProfile?.heightCm)
        assertEquals(72, userRepository.updatedProfile?.weightKg)
        assertEquals(67, userRepository.updatedProfile?.targetWeightKg)
        assertEquals(72f, trackingRepository.savedBodyMeasurement?.weightKg)
        assertEquals(72f / (1.72f * 1.72f), viewModel.uiState.value.bmi ?: 0f, 0.01f)
        assertFalse(viewModel.uiState.value.showEditBodyDialog)
    }

    private fun homeHarness(
        userRepository: FakeHomeUserRepository = FakeHomeUserRepository(homeUser()),
        taskRepository: FakeHomeTaskRepository = FakeHomeTaskRepository(),
        notificationRepository: FakeHomeNotificationRepository = FakeHomeNotificationRepository(),
        trackingRepository: FakeHomeTrackingRepository = FakeHomeTrackingRepository(),
        planRepository: FakeHomePlanRepository = FakeHomePlanRepository(),
        sessionRepository: FakeHomeSessionRepository = FakeHomeSessionRepository()
    ): HomeHarness = HomeHarness(
        userRepository = userRepository,
        taskRepository = taskRepository,
        notificationRepository = notificationRepository,
        trackingRepository = trackingRepository,
        planRepository = planRepository,
        sessionRepository = sessionRepository
    )
}

private class HomeHarness(
    private val userRepository: FakeHomeUserRepository,
    private val taskRepository: FakeHomeTaskRepository,
    private val notificationRepository: FakeHomeNotificationRepository,
    private val trackingRepository: FakeHomeTrackingRepository,
    private val planRepository: FakeHomePlanRepository,
    private val sessionRepository: FakeHomeSessionRepository
) {
    fun viewModel(): HomeViewModel {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        return HomeViewModel(
            getCurrentUserUseCase = GetCurrentUserUseCase(userRepository, sessionRepository),
            getHomeDashboardUseCase = GetHomeDashboardUseCase(userRepository, planRepository, trackingRepository, sessionRepository),
            updateStreakUseCase = UpdateStreakUseCase(userRepository, sessionRepository),
            homeTaskRepository = taskRepository,
            appNotificationRepository = notificationRepository,
            localContentFallbacks = LocalContentFallbacks(context),
            contentRepository = FakeHomeContentRepository(),
            userRepository = userRepository,
            sessionRepository = sessionRepository,
            trackingRepository = trackingRepository,
            context = context
        )
    }
}

private class FakeHomeTaskRepository(initialTasks: List<HomeTask> = emptyList()) : HomeTaskRepository {
    private val tasks = MutableStateFlow(initialTasks)
    var addedTask: HomeTaskDraft? = null
    var lastStatusUpdate: Pair<Long, HomeTaskStatus>? = null
    var lastReminderUpdate: Pair<Long, Boolean>? = null
    var deletedTaskId: Long? = null

    override fun observeTasks(dateKey: String): Flow<List<HomeTask>> = tasks
    override suspend fun ensureTasks(dateKey: String, defaults: List<HomeTaskDraft>) = Unit
    override suspend fun addTask(task: HomeTaskDraft) {
        addedTask = task
    }
    override suspend fun updateTaskStatus(taskId: Long, status: HomeTaskStatus) {
        lastStatusUpdate = taskId to status
    }
    override suspend fun updateTaskReminder(taskId: Long, enabled: Boolean) {
        lastReminderUpdate = taskId to enabled
    }
    override suspend fun deleteTask(taskId: Long) {
        deletedTaskId = taskId
    }
}

private class FakeHomeNotificationRepository(
    initialNotifications: List<AppNotification> = emptyList(),
    initialUnreadCount: Int = 0
) : AppNotificationRepository {
    private val notifications = MutableStateFlow(initialNotifications)
    private val unreadCount = MutableStateFlow(initialUnreadCount)
    var markedReadId: Long? = null
    var markedAllRead = false
    var deletedNotificationId: Long? = null

    override fun observeNotifications(): Flow<List<AppNotification>> = notifications
    override fun observeUnreadCount(): Flow<Int> = unreadCount
    override suspend fun addNotification(title: String, message: String, type: AppNotificationType) = Unit
    override suspend fun markAsRead(notificationId: Long) {
        markedReadId = notificationId
    }
    override suspend fun markAllAsRead() {
        markedAllRead = true
    }
    override suspend fun deleteNotification(notificationId: Long) {
        deletedNotificationId = notificationId
    }
}

private class FakeHomeUserRepository(var user: FittyUser?) : UserRepository {
    var updatedProfile: FittyProfile? = null
    var updatedStats: FittyStats? = null

    override suspend fun getCurrentUser(uid: String?): FittyUser? = user
    override suspend fun updateProfile(uid: String, profile: FittyProfile): Result<Unit> {
        updatedProfile = profile
        user = user?.copy(profile = profile)
        return Result.success(Unit)
    }
    override suspend fun updateOnboarding(uid: String, onboarding: FittyOnboarding): Result<Unit> = Result.success(Unit)
    override suspend fun updateSettings(uid: String, settings: FittySettings): Result<Unit> = Result.success(Unit)
    override suspend fun updateGoal(uid: String, primaryGoal: String, targetWeightKg: Int?): Result<Unit> = Result.success(Unit)
    override suspend fun updateStats(uid: String, stats: FittyStats): Result<Unit> {
        updatedStats = stats
        user = user?.copy(stats = stats)
        return Result.success(Unit)
    }
    override suspend fun deleteUserData(uid: String): Result<Unit> = Result.success(Unit)
    override suspend fun updateDisplayName(uid: String, name: String): Result<Unit> = Result.success(Unit)
    override suspend fun uploadProfilePhoto(uid: String, imageUri: String): Result<String> = Result.success(imageUri)
}

private class FakeHomeTrackingRepository(
    private val dailySummary: DailySummary? = null,
    private val mealLogs: List<MealLog> = emptyList()
) : TrackingRepository {
    var savedBodyMeasurement: BodyMeasurement? = null

    override suspend fun saveMealLog(uid: String, mealLog: MealLog): Result<String> = Result.success("meal")
    override suspend fun getMealLogs(uid: String, dateKey: String): List<MealLog> = mealLogs
    override suspend fun getMealLog(uid: String, mealId: String): MealLog? = null
    override suspend fun deleteMealLog(uid: String, mealId: String): Result<Unit> = Result.success(Unit)
    override suspend fun saveMealScanRecord(uid: String, record: MealScanRecord): Result<String> = Result.success("scan")
    override suspend fun getMealScanHistory(uid: String, limit: Int): List<MealScanRecord> = emptyList()
    override suspend fun uploadScanImage(uid: String, localImageUri: String): Result<String> = Result.success(localImageUri)
    override suspend fun uploadBodyScanImage(uid: String, localImageUri: String): Result<String> = Result.success(localImageUri)
    override suspend fun saveBodyScan(uid: String, bodyScan: BodyScan): Result<String> = Result.success("body")
    override suspend fun getBodyScans(uid: String, limit: Int): List<BodyScan> = emptyList()
    override suspend fun getLatestBodyScan(uid: String): BodyScan? = null
    override suspend fun saveBodyMeasurement(uid: String, measurement: BodyMeasurement): Result<String> {
        savedBodyMeasurement = measurement
        return Result.success("measurement")
    }
    override suspend fun getBodyMeasurements(uid: String, limit: Int): List<BodyMeasurement> = emptyList()
    override suspend fun getDailySummary(uid: String, dateKey: String): DailySummary? = dailySummary
    override suspend fun getDailySummaries(uid: String, fromDate: String, toDate: String): List<DailySummary> = emptyList()
    override suspend fun updateDailySummary(uid: String, dateKey: String, summary: DailySummary): Result<Unit> = Result.success(Unit)
    override suspend fun getProgressStats(uid: String, days: Int): ProgressStats = ProgressStats()
}

private class FakeHomePlanRepository(
    private val plan: PlanInstance? = null,
    private val workouts: List<ScheduledWorkout> = emptyList()
) : PlanRepository {
    override suspend fun getActivePlan(uid: String): PlanInstance? = plan
    override suspend fun getPlanInstance(uid: String, planId: String): PlanInstance? = plan
    override suspend fun getAllPlans(uid: String): List<PlanInstance> = plan?.let(::listOf) ?: emptyList()
    override suspend fun savePlanInstance(uid: String, plan: PlanInstance): Result<String> = Result.success(plan.id)
    override suspend fun updatePlanStatus(uid: String, planId: String, status: String): Result<Unit> = Result.success(Unit)
    override suspend fun deletePlan(uid: String, planId: String): Result<Unit> = Result.success(Unit)
    override suspend fun getScheduledWorkouts(uid: String, planId: String, dateKey: String?): List<ScheduledWorkout> = workouts
    override suspend fun getScheduledWorkout(uid: String, planId: String, workoutId: String): ScheduledWorkout? = workouts.firstOrNull { it.id == workoutId }
    override suspend fun saveScheduledWorkout(uid: String, planId: String, workout: ScheduledWorkout): Result<String> = Result.success(workout.id)
    override suspend fun updateScheduledWorkoutStatus(uid: String, planId: String, workoutId: String, status: String): Result<Unit> = Result.success(Unit)
    override suspend fun replaceScheduledWorkout(uid: String, planId: String, workoutId: String, newWorkout: ScheduledWorkout): Result<String> = Result.success(newWorkout.id)
    override suspend fun getExerciseLibrary(): List<Exercise> = emptyList()
    override suspend fun getExercise(exerciseId: String): Exercise? = null
    override suspend fun searchExercises(query: String, muscleGroup: String?, difficulty: String?, equipment: String?): List<Exercise> = emptyList()
    override suspend fun getProgramTemplates(goal: String?, difficulty: String?, equipment: String?): List<ProgramTemplate> = emptyList()
    override suspend fun getProgramTemplate(programId: String): ProgramTemplate? = null
}

private class FakeHomeSessionRepository : SessionRepository {
    override suspend fun saveStartupState(state: FittyStartupState) = Unit
    override suspend fun saveUserSession(user: FittyUser) = Unit
    override suspend fun setOnboardingCompleted(completed: Boolean) = Unit
    override suspend fun getCurrentUserId(): String? = "uid"
    override fun observeCurrentUserId(): Flow<String?> = MutableStateFlow("uid")
    override suspend fun getAppLanguage(): String? = "en"
    override suspend fun setAppLanguage(language: String) = Unit
    override suspend fun clearSession() = Unit
    override suspend fun isSignedInSessionExpired(nowMillis: Long, maxAgeMillis: Long): Boolean = false
    override suspend fun shouldShowWelcomeNotification(nowMillis: Long, cooldownMillis: Long): Boolean = false
    override suspend fun setLastWelcomeNotificationAt(timestampMillis: Long) = Unit
}

private class FakeHomeContentRepository : ContentRepository {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val fallbacks = LocalContentFallbacks(context)

    override suspend fun getHomeContent(language: String): HomeContentConfig = fallbacks.home(language)
    override suspend fun getCoachContent(language: String): CoachContentConfig = fallbacks.coach()
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

private fun homeUser(
    profile: FittyProfile = FittyProfile(),
    settings: FittySettings = FittySettings(),
    stats: FittyStats = FittyStats()
): FittyUser = FittyUser(
    uid = "uid",
    email = "fitty@example.com",
    displayName = "Fitty User",
    username = "fitty",
    authProvider = "password",
    guest = false,
    onboardingCompleted = true,
    profile = profile,
    settings = settings,
    stats = stats
)
