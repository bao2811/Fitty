package com.example.fitty.domain.usecase.track

import com.example.fitty.domain.model.BodyMeasurement
import com.example.fitty.domain.model.BodyScan
import com.example.fitty.domain.model.DailySummary
import com.example.fitty.domain.model.FittyOnboarding
import com.example.fitty.domain.model.FittyProfile
import com.example.fitty.domain.model.FittySettings
import com.example.fitty.domain.model.FittyStartupState
import com.example.fitty.domain.model.FittyStats
import com.example.fitty.domain.model.FittyUser
import com.example.fitty.domain.model.MealLog
import com.example.fitty.domain.model.MealScanRecord
import com.example.fitty.domain.model.ProgressStats
import com.example.fitty.domain.repository.SessionRepository
import com.example.fitty.domain.repository.TrackingRepository
import com.example.fitty.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ConfirmMealLogUseCaseTest {

    @Test
    fun `confirm meal updates summary for meal date instead of today`() = runTest {
        val trackingRepository = FakeTrackingRepository()
        val userRepository = FakeUserRepository()
        val sessionRepository = FakeSessionRepository()
        val useCase = ConfirmMealLogUseCase(
            trackingRepository = trackingRepository,
            sessionRepository = sessionRepository,
            userRepository = userRepository
        )
        val mealDate = "2026-05-01"

        val result = useCase(
            mealLog = MealLog(
                mealType = "dinner",
                dateKey = mealDate,
                totalCalories = 640,
                totalProtein = 42,
                totalCarbs = 58,
                totalFat = 18
            )
        )

        assertEquals(true, result.isSuccess)
        val savedSummary = trackingRepository.updatedSummaries[mealDate]
        assertNotNull(savedSummary)
        assertEquals(1, savedSummary?.mealsLoggedCount)
        assertEquals(1, savedSummary?.progress?.mealsLogged)
        assertEquals(640, savedSummary?.progress?.caloriesConsumed)
        assertEquals(setOf(mealDate), trackingRepository.updatedSummaries.keys)
    }
}

private class FakeTrackingRepository : TrackingRepository {
    val updatedSummaries = linkedMapOf<String, DailySummary>()

    override suspend fun saveMealLog(uid: String, mealLog: MealLog): Result<String> = Result.success("meal-1")

    override suspend fun getMealLogs(uid: String, dateKey: String): List<MealLog> = emptyList()

    override suspend fun getMealLog(uid: String, mealId: String): MealLog? = null

    override suspend fun deleteMealLog(uid: String, mealId: String): Result<Unit> = Result.success(Unit)

    override suspend fun saveMealScanRecord(uid: String, record: MealScanRecord): Result<String> = Result.success("scan-1")

    override suspend fun getMealScanHistory(uid: String, limit: Int): List<MealScanRecord> = emptyList()

    override suspend fun uploadScanImage(uid: String, localImageUri: String): Result<String> = Result.success("uploaded://image")

    override suspend fun saveBodyScan(uid: String, bodyScan: BodyScan): Result<String> = Result.success("body-1")

    override suspend fun getBodyScans(uid: String, limit: Int): List<BodyScan> = emptyList()

    override suspend fun getLatestBodyScan(uid: String): BodyScan? = null

    override suspend fun saveBodyMeasurement(uid: String, measurement: BodyMeasurement): Result<String> = Result.success("measure-1")

    override suspend fun getBodyMeasurements(uid: String, limit: Int): List<BodyMeasurement> = emptyList()

    override suspend fun getDailySummary(uid: String, dateKey: String): DailySummary? = updatedSummaries[dateKey]

    override suspend fun getDailySummaries(uid: String, fromDate: String, toDate: String): List<DailySummary> = emptyList()

    override suspend fun updateDailySummary(uid: String, dateKey: String, summary: DailySummary): Result<Unit> {
        updatedSummaries[dateKey] = summary
        return Result.success(Unit)
    }

    override suspend fun getProgressStats(uid: String, days: Int): ProgressStats = ProgressStats()
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

private class FakeUserRepository : UserRepository {
    private var currentUser = FittyUser(
        uid = "user-1",
        email = "fitty@example.com",
        displayName = "Fitty",
        username = "fitty",
        authProvider = "test",
        guest = false,
        onboardingCompleted = true
    )

    override suspend fun getCurrentUser(uid: String?): FittyUser = currentUser

    override suspend fun updateProfile(uid: String, profile: FittyProfile): Result<Unit> = Result.success(Unit)

    override suspend fun updateOnboarding(uid: String, onboarding: FittyOnboarding): Result<Unit> = Result.success(Unit)

    override suspend fun updateSettings(uid: String, settings: FittySettings): Result<Unit> = Result.success(Unit)

    override suspend fun updateGoal(uid: String, primaryGoal: String, targetWeightKg: Int?): Result<Unit> = Result.success(Unit)

    override suspend fun updateStats(uid: String, stats: FittyStats): Result<Unit> {
        currentUser = currentUser.copy(stats = stats)
        return Result.success(Unit)
    }

    override suspend fun deleteUserData(uid: String): Result<Unit> = Result.success(Unit)

    override suspend fun updateDisplayName(uid: String, name: String): Result<Unit> = Result.success(Unit)

    override suspend fun uploadProfilePhoto(uid: String, imageUri: String): Result<String> = Result.success("profile://photo")
}
