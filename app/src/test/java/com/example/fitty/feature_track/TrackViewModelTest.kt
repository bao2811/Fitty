package com.example.fitty.feature_track

import androidx.test.core.app.ApplicationProvider
import com.example.fitty.data.content.LocalContentFallbacks
import com.example.fitty.domain.model.BodyMeasurement
import com.example.fitty.domain.model.BodyScan
import com.example.fitty.domain.model.BodyScanAnalysisEngine
import com.example.fitty.domain.model.BodyScanAnalysisResult
import com.example.fitty.domain.model.CoachContentConfig
import com.example.fitty.domain.model.DailySummary
import com.example.fitty.domain.model.DailySummaryProgress
import com.example.fitty.domain.model.DailySummaryTargets
import com.example.fitty.domain.model.ExercisePrescriptionContent
import com.example.fitty.domain.model.FittyProfile
import com.example.fitty.domain.model.FittySettings
import com.example.fitty.domain.model.FittyStartupState
import com.example.fitty.domain.model.FittyStats
import com.example.fitty.domain.model.FittyUser
import com.example.fitty.domain.model.FoodItem
import com.example.fitty.domain.model.HomeBehaviorConfig
import com.example.fitty.domain.model.HomeContentConfig
import com.example.fitty.domain.model.MealAnalysisEngine
import com.example.fitty.domain.model.MealAnalysisResult
import com.example.fitty.domain.model.MealLog
import com.example.fitty.domain.model.MealScanRecord
import com.example.fitty.domain.model.OnboardingContentConfig
import com.example.fitty.domain.model.PracticeCategoryContent
import com.example.fitty.domain.model.ProgressStats
import com.example.fitty.domain.model.QuickWorkoutConfig
import com.example.fitty.domain.model.StarterPlanTemplate
import com.example.fitty.domain.model.TrackBehaviorConfig
import com.example.fitty.domain.repository.ContentRepository
import com.example.fitty.domain.repository.SessionRepository
import com.example.fitty.domain.repository.TrackingRepository
import com.example.fitty.domain.repository.UserRepository
import com.example.fitty.domain.usecase.track.AnalyzeBodyScanUseCase
import com.example.fitty.domain.usecase.track.AnalyzeMealImageUseCase
import com.example.fitty.domain.usecase.track.ConfirmMealLogUseCase
import com.example.fitty.domain.usecase.track.GetBodyScansUseCase
import com.example.fitty.domain.usecase.track.GetMealLogsUseCase
import com.example.fitty.domain.usecase.track.GetMealScanHistoryUseCase
import com.example.fitty.domain.usecase.track.GetProgressStatsUseCase
import com.example.fitty.domain.usecase.track.SaveBodyScanUseCase
import com.example.fitty.domain.usecase.user.UpdateStreakUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
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
class TrackViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)

    @Test
    fun `init loads stats and scan history into ui state`() = runTest {
        val trackingRepository = FakeTrackingRepository(
            progressStats = ProgressStats(
                totalWorkouts = 6,
                totalMealsLogged = 4,
                currentStreak = 3,
                bestStreak = 5,
                latestWeight = 72.5f,
                targetWeight = 70f,
                bmi = 22.4f,
                dailySummaries = listOf(
                    DailySummary(
                        dateKey = "2026-05-19",
                        progress = DailySummaryProgress(
                            workoutsCompleted = 1,
                            mealsLogged = 2,
                            caloriesBurned = 300,
                            proteinGrams = 90,
                            carbsGrams = 120,
                            fatGrams = 40
                        )
                    ),
                    DailySummary(
                        dateKey = "2026-05-20",
                        progress = DailySummaryProgress(
                            workoutsCompleted = 0,
                            mealsLogged = 1,
                            caloriesBurned = 200,
                            proteinGrams = 60,
                            carbsGrams = 80,
                            fatGrams = 20
                        )
                    )
                )
            ),
            mealLogsByDate = mutableMapOf(
                today to listOf(
                    MealLog(mealType = "breakfast", totalCalories = 450)
                )
            ),
            mealScanHistory = mutableListOf(
                MealScanRecord(
                    id = "scan-1",
                    imageUrl = "remote://meal.jpg",
                    totalCalories = 450,
                    totalProtein = 30,
                    totalCarbs = 50,
                    totalFat = 10,
                    foodItems = listOf(FoodItem(name = "Egg", calories = 80)),
                    dateKey = "2026-05-21",
                    timestamp = 1234L
                )
            )
        )

        val viewModel = createViewModel(trackingRepository = trackingRepository)

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("6", state.statWorkouts)
        assertEquals("4", state.statMeals)
        assertEquals("3 days", state.statStreak)
        assertEquals("72.5 kg", state.progressWeight)
        assertEquals("70.0 kg", state.targetWeight)
        assertEquals("22.4", state.bmi)
        assertEquals("Breakfast", state.mealHistory.single().first)
        assertEquals("450 kcal", state.mealHistory.single().second)
        assertEquals(1, state.scanHistory.size)
        assertEquals("remote://meal.jpg", state.scanHistory.single().imageUrl)
    }

    @Test
    fun `init uses persisted workout minutes and explicit meal target in progress state`() = runTest {
        val trackingRepository = FakeTrackingRepository(
            progressStats = ProgressStats(
                totalWorkouts = 6,
                totalMealsLogged = 4,
                mealTargetPerDay = 6,
                dailySummaries = listOf(
                    DailySummary(
                        dateKey = "2026-05-19",
                        targets = DailySummaryTargets(workouts = 2),
                        progress = DailySummaryProgress(
                            workoutsCompleted = 1,
                            mealsLogged = 2,
                            caloriesBurned = 300,
                            activeMinutes = 30
                        )
                    ),
                    DailySummary(
                        dateKey = "2026-05-20",
                        targets = DailySummaryTargets(workouts = 3),
                        progress = DailySummaryProgress(
                            workoutsCompleted = 2,
                            mealsLogged = 2,
                            caloriesBurned = 240,
                            activeMinutes = 40
                        )
                    )
                )
            )
        )

        val viewModel = createViewModel(trackingRepository = trackingRepository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("3/5", state.progressWorkouts)
        assertEquals(0.6f, state.progressWorkoutPercent, 0.001f)
        assertEquals("4/6", state.progressMeals)
        assertEquals(4f / 6f, state.progressMealPercent, 0.001f)
        assertEquals("70", state.statActiveMin)
    }

    @Test
    fun `submit captured meal image updates analysis result`() = runTest {
        val mealResult = MealAnalysisResult(
            mealLog = MealLog(
                mealType = "lunch",
                totalCalories = 620,
                totalProtein = 35,
                totalCarbs = 70,
                totalFat = 18,
                foodItems = listOf(
                    FoodItem(name = "Chicken breast", calories = 280),
                    FoodItem(name = "Rice", calories = 220)
                )
            ),
            confidence = 0.87f
        )
        val viewModel = createViewModel(
            mealAnalysisEngine = FakeMealAnalysisEngine(mealResult = mealResult)
        )
        advanceUntilIdle()

        viewModel.selectTab(TrackTab.Meals)
        viewModel.setCapturedImage("file://meal.jpg")
        viewModel.submitCapturedImage()
        advanceUntilIdle()

        val result = viewModel.uiState.value.analysisResult
        assertNotNull(result)
        assertEquals("Meal Analysis", result?.title)
        assertEquals("620 kcal", result?.rows?.first { it.label == "Calories" }?.value)
        assertTrue(result?.summary?.contains("87%") == true)
        assertFalse(viewModel.uiState.value.isSubmittingImage)
    }

    @Test
    fun `confirm meal persists data and marks meal confirmed`() = runTest {
        val trackingRepository = FakeTrackingRepository()
        val userRepository = FakeUserRepository()
        val mealResult = MealAnalysisResult(
            mealLog = MealLog(
                mealType = "dinner",
                totalCalories = 700,
                totalProtein = 45,
                totalCarbs = 65,
                totalFat = 22,
                confidence = 0.9f,
                foodItems = listOf(FoodItem(name = "Salmon", calories = 320))
            ),
            confidence = 0.9f
        )
        val viewModel = createViewModel(
            trackingRepository = trackingRepository,
            userRepository = userRepository,
            mealAnalysisEngine = FakeMealAnalysisEngine(mealResult = mealResult)
        )
        advanceUntilIdle()

        viewModel.selectTab(TrackTab.Meals)
        viewModel.setCapturedImage("file://dinner.jpg")
        viewModel.submitCapturedImage()
        advanceUntilIdle()

        viewModel.confirmMeal()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.mealConfirmed)
        assertEquals(1, trackingRepository.savedMealLogs.size)
        assertEquals(1, trackingRepository.savedMealScanRecords.size)
        assertEquals(1, state.scanHistory.size)
        assertEquals(2, userRepository.updatedStats.size)
        assertEquals(1, trackingRepository.updatedDailySummaries.size)
    }

    @Test
    fun `save body scan persists scan and marks success`() = runTest {
        val trackingRepository = FakeTrackingRepository()
        val userRepository = FakeUserRepository()
        val bodyResult = BodyScanAnalysisResult(
            bodyScan = BodyScan(
                summary = "Lean progress",
                estimatedBodyFatPercent = 18.6f,
                postureScore = 88,
                status = "good"
            ),
            confidence = 0.82f
        )
        val viewModel = createViewModel(
            trackingRepository = trackingRepository,
            userRepository = userRepository,
            bodyScanAnalysisEngine = FakeBodyScanAnalysisEngine(bodyResult = bodyResult)
        )
        advanceUntilIdle()

        viewModel.selectTab(TrackTab.Body)
        viewModel.setCapturedImage("file://body.jpg")
        viewModel.submitCapturedImage()
        advanceUntilIdle()

        viewModel.saveBodyScan()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.bodyScanSaved)
        assertEquals(1, trackingRepository.savedBodyScans.size)
        assertEquals(1, trackingRepository.savedBodyMeasurements.size)
        assertEquals(1, trackingRepository.updatedDailySummaries.size)
        assertEquals(1, userRepository.updatedStats.size)
    }

    private fun createViewModel(
        trackingRepository: FakeTrackingRepository = FakeTrackingRepository(),
        userRepository: FakeUserRepository = FakeUserRepository(),
        sessionRepository: FakeSessionRepository = FakeSessionRepository(),
        contentRepository: FakeContentRepository = FakeContentRepository(),
        mealAnalysisEngine: FakeMealAnalysisEngine = FakeMealAnalysisEngine(),
        bodyScanAnalysisEngine: FakeBodyScanAnalysisEngine = FakeBodyScanAnalysisEngine()
    ): TrackViewModel {
        return TrackViewModel(
            analyzeMealImageUseCase = AnalyzeMealImageUseCase(mealAnalysisEngine),
            confirmMealLogUseCase = ConfirmMealLogUseCase(
                trackingRepository = trackingRepository,
                sessionRepository = sessionRepository,
                userRepository = userRepository
            ),
            analyzeBodyScanUseCase = AnalyzeBodyScanUseCase(bodyScanAnalysisEngine),
            saveBodyScanUseCase = SaveBodyScanUseCase(trackingRepository, sessionRepository, userRepository),
            getProgressStatsUseCase = GetProgressStatsUseCase(trackingRepository, sessionRepository),
            getMealLogsUseCase = GetMealLogsUseCase(trackingRepository, sessionRepository),
            getMealScanHistoryUseCase = GetMealScanHistoryUseCase(trackingRepository, sessionRepository),
            getBodyScansUseCase = GetBodyScansUseCase(trackingRepository, sessionRepository),
            updateStreakUseCase = UpdateStreakUseCase(userRepository, sessionRepository),
            localContentFallbacks = LocalContentFallbacks(ApplicationProvider.getApplicationContext()),
            contentRepository = contentRepository,
            context = ApplicationProvider.getApplicationContext()
        )
    }
}

private class FakeContentRepository(
    private val trackBehaviorConfig: TrackBehaviorConfig = TrackBehaviorConfig()
) : ContentRepository {
    override suspend fun getHomeContent(language: String): HomeContentConfig = HomeContentConfig(
        emptyState = com.example.fitty.domain.model.HomeEmptyStateContent("", "", "", ""),
        insightActions = emptyList(),
        suggestedTaskPresets = emptyList(),
        defaultTaskTemplates = emptyList()
    )

    override suspend fun getCoachContent(language: String): CoachContentConfig = CoachContentConfig("", emptyList())

    override suspend fun getOnboardingContent(language: String): OnboardingContentConfig = OnboardingContentConfig(
        stepTitles = emptyList(),
        goals = emptyList(),
        fitnessLevels = emptyList(),
        preferredTimes = emptyList(),
        durations = emptyList(),
        equipments = emptyList(),
        nutritionStyles = emptyList(),
        workoutDays = emptyList(),
        restrictions = emptyList(),
        reminders = emptyList()
    )

    override suspend fun getHomeBehaviorConfig(): HomeBehaviorConfig = HomeBehaviorConfig()

    override suspend fun getTrackBehaviorConfig(): TrackBehaviorConfig = trackBehaviorConfig

    override suspend fun getQuickWorkoutConfig(language: String): QuickWorkoutConfig = QuickWorkoutConfig()

    override suspend fun getPracticeCategories(language: String): List<PracticeCategoryContent> = emptyList()

    override suspend fun getExercisePrescriptions(language: String): List<ExercisePrescriptionContent> = emptyList()

    override suspend fun getStarterPlanTemplate(
        goal: String,
        fitnessLevel: String,
        equipment: String,
        language: String
    ): StarterPlanTemplate? = null
}

private class FakeMealAnalysisEngine(
    private val mealResult: MealAnalysisResult = MealAnalysisResult(
        mealLog = MealLog(totalCalories = 0),
        confidence = 0f
    )
) : MealAnalysisEngine {
    override suspend fun analyzeMealImage(imageUri: String): MealAnalysisResult = mealResult
}

private class FakeBodyScanAnalysisEngine(
    private val bodyResult: BodyScanAnalysisResult = BodyScanAnalysisResult(
        bodyScan = BodyScan(),
        confidence = 0f
    )
) : BodyScanAnalysisEngine {
    override suspend fun analyzeBodyScan(frontImageUri: String, sideImageUri: String?): BodyScanAnalysisResult = bodyResult
}

private class FakeTrackingRepository(
    var progressStats: ProgressStats = ProgressStats(),
    var mealLogsByDate: MutableMap<String, List<MealLog>> = mutableMapOf(),
    var mealScanHistory: MutableList<MealScanRecord> = mutableListOf()
) : TrackingRepository {
    val savedMealLogs = mutableListOf<MealLog>()
    val savedMealScanRecords = mutableListOf<MealScanRecord>()
    val savedBodyScans = mutableListOf<BodyScan>()
    val savedBodyMeasurements = mutableListOf<BodyMeasurement>()
    val updatedDailySummaries = mutableListOf<DailySummary>()
    private val dailySummaries = mutableMapOf<String, DailySummary>()

    override suspend fun saveMealLog(uid: String, mealLog: MealLog): Result<String> {
        savedMealLogs += mealLog
        val dateKey = mealLog.dateKey
        if (dateKey.isNotBlank()) {
            mealLogsByDate[dateKey] = mealLogsByDate[dateKey].orEmpty() + mealLog
        }
        return Result.success("meal-${savedMealLogs.size}")
    }

    override suspend fun getMealLogs(uid: String, dateKey: String): List<MealLog> = mealLogsByDate[dateKey].orEmpty()

    override suspend fun getMealLog(uid: String, mealId: String): MealLog? = savedMealLogs.firstOrNull { it.id == mealId }

    override suspend fun deleteMealLog(uid: String, mealId: String): Result<Unit> = Result.success(Unit)

    override suspend fun saveMealScanRecord(uid: String, record: MealScanRecord): Result<String> {
        savedMealScanRecords += record
        mealScanHistory.add(0, record)
        return Result.success("scan-${savedMealScanRecords.size}")
    }

    override suspend fun getMealScanHistory(uid: String, limit: Int): List<MealScanRecord> = mealScanHistory.take(limit)

    override suspend fun uploadScanImage(uid: String, localImageUri: String): Result<String> {
        return Result.success("uploaded://$localImageUri")
    }

    override suspend fun uploadBodyScanImage(uid: String, localImageUri: String): Result<String> {
        return Result.success("uploaded-body://$localImageUri")
    }

    override suspend fun saveBodyScan(uid: String, bodyScan: BodyScan): Result<String> {
        savedBodyScans += bodyScan
        return Result.success("body-${savedBodyScans.size}")
    }

    override suspend fun getBodyScans(uid: String, limit: Int): List<BodyScan> = savedBodyScans.take(limit)

    override suspend fun getLatestBodyScan(uid: String): BodyScan? = savedBodyScans.lastOrNull()

    override suspend fun saveBodyMeasurement(uid: String, measurement: BodyMeasurement): Result<String> {
        savedBodyMeasurements += measurement
        return Result.success("measurement-${savedBodyMeasurements.size}")
    }

    override suspend fun getBodyMeasurements(uid: String, limit: Int): List<BodyMeasurement> = savedBodyMeasurements.take(limit)

    override suspend fun getDailySummary(uid: String, dateKey: String): DailySummary? = dailySummaries[dateKey]

    override suspend fun getDailySummaries(uid: String, fromDate: String, toDate: String): List<DailySummary> {
        return dailySummaries.values.toList()
    }

    override suspend fun updateDailySummary(uid: String, dateKey: String, summary: DailySummary): Result<Unit> {
        dailySummaries[dateKey] = summary
        updatedDailySummaries += summary
        return Result.success(Unit)
    }

    override suspend fun getProgressStats(uid: String, days: Int): ProgressStats = progressStats
}

private class FakeSessionRepository(
    private val userId: String = "user-1"
) : SessionRepository {
    override suspend fun saveStartupState(state: FittyStartupState) = Unit

    override suspend fun saveUserSession(user: FittyUser) = Unit

    override suspend fun setOnboardingCompleted(completed: Boolean) = Unit

    override suspend fun getCurrentUserId(): String? = userId

    override fun observeCurrentUserId(): Flow<String?> = flowOf(userId)

    override suspend fun getAppLanguage(): String? = "en"

    override suspend fun setAppLanguage(language: String) = Unit

    override suspend fun clearSession() = Unit

    override suspend fun isSignedInSessionExpired(nowMillis: Long, maxAgeMillis: Long): Boolean = false

    override suspend fun shouldShowWelcomeNotification(nowMillis: Long, cooldownMillis: Long): Boolean = false

    override suspend fun setLastWelcomeNotificationAt(timestampMillis: Long) = Unit
}

private class FakeUserRepository(
    initialUser: FittyUser = FittyUser(
        uid = "user-1",
        email = "fitty@example.com",
        displayName = "Fitty",
        username = "fitty",
        authProvider = "test",
        guest = false,
        onboardingCompleted = true
    )
) : UserRepository {
    var currentUser: FittyUser = initialUser
    val updatedStats = mutableListOf<FittyStats>()

    override suspend fun getCurrentUser(uid: String?): FittyUser? = currentUser

    override suspend fun updateProfile(uid: String, profile: FittyProfile): Result<Unit> {
        currentUser = currentUser.copy(profile = profile)
        return Result.success(Unit)
    }

    override suspend fun updateOnboarding(uid: String, onboarding: com.example.fitty.domain.model.FittyOnboarding): Result<Unit> {
        currentUser = currentUser.copy(onboarding = onboarding)
        return Result.success(Unit)
    }

    override suspend fun updateSettings(uid: String, settings: FittySettings): Result<Unit> {
        currentUser = currentUser.copy(settings = settings)
        return Result.success(Unit)
    }

    override suspend fun updateGoal(uid: String, primaryGoal: String, targetWeightKg: Int?): Result<Unit> = Result.success(Unit)

    override suspend fun updateStats(uid: String, stats: FittyStats): Result<Unit> {
        updatedStats += stats
        currentUser = currentUser.copy(stats = stats)
        return Result.success(Unit)
    }

    override suspend fun deleteUserData(uid: String): Result<Unit> = Result.success(Unit)

    override suspend fun updateDisplayName(uid: String, name: String): Result<Unit> = Result.success(Unit)

    override suspend fun uploadProfilePhoto(uid: String, imageUri: String): Result<String> = Result.success("profile://photo")
}
