package com.example.fitty.domain.usecase.workout

import com.example.fitty.domain.model.DailySummary
import com.example.fitty.domain.model.ExerciseLog
import com.example.fitty.domain.model.FittyOnboarding
import com.example.fitty.domain.model.FittyProfile
import com.example.fitty.domain.model.FittySettings
import com.example.fitty.domain.model.FittyStartupState
import com.example.fitty.domain.model.FittyStats
import com.example.fitty.domain.model.FittyUser
import com.example.fitty.domain.model.ProgressStats
import com.example.fitty.domain.model.BodyMeasurement
import com.example.fitty.domain.model.BodyScan
import com.example.fitty.domain.model.MealLog
import com.example.fitty.domain.model.MealScanRecord
import com.example.fitty.domain.repository.PlanRepository
import com.example.fitty.domain.repository.SessionRepository
import com.example.fitty.domain.repository.TrackingRepository
import com.example.fitty.domain.repository.UserRepository
import com.example.fitty.domain.repository.WorkoutSessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutSessionUseCasesTest {

    @Test
    fun `complete workout updates summary active minutes and workout totals`() = runTest {
        val planRepository = FakePlanRepository()
        val trackingRepository = FakeTrackingRepository()
        val userRepository = FakeUserRepository()
        val workoutRepository = FakeWorkoutSessionRepository()
        val useCase = CompleteWorkoutSessionUseCase(
            workoutSessionRepository = workoutRepository,
            planRepository = planRepository,
            trackingRepository = trackingRepository,
            userRepository = userRepository,
            sessionRepository = FakeSessionRepository()
        )

        val result = useCase(
            sessionId = "session-1",
            durationMinutes = 30,
            caloriesBurned = 220,
            completionRate = 1f,
            perceivedEffort = null,
            exercises = listOf(ExerciseLog(id = "log-1", exerciseId = "push_up", completed = true)),
            planId = "",
            scheduledWorkoutId = ""
        )

        assertTrue(result.isSuccess)
        assertEquals(1, trackingRepository.updatedSummaries.size)
        assertEquals(30, trackingRepository.updatedSummaries.single().progress.activeMinutes)
        assertEquals(1, userRepository.updatedStatsCount)
    }

    @Test
    fun `complete workout fails when scheduled workout status update fails`() = runTest {
        val planRepository = FakePlanRepository(updateStatusResult = Result.failure(IllegalStateException("plan update failed")))
        val trackingRepository = FakeTrackingRepository()
        val userRepository = FakeUserRepository()
        val workoutRepository = FakeWorkoutSessionRepository()
        val useCase = CompleteWorkoutSessionUseCase(
            workoutSessionRepository = workoutRepository,
            planRepository = planRepository,
            trackingRepository = trackingRepository,
            userRepository = userRepository,
            sessionRepository = FakeSessionRepository()
        )

        val result = useCase(
            sessionId = "session-1",
            durationMinutes = 30,
            caloriesBurned = 220,
            completionRate = 1f,
            perceivedEffort = null,
            exercises = listOf(ExerciseLog(id = "log-1", exerciseId = "push_up", completed = true)),
            planId = "plan-1",
            scheduledWorkoutId = "scheduled-1"
        )

        assertTrue(result.isFailure)
        assertEquals(0, trackingRepository.updatedSummaries.size)
        assertEquals(0, userRepository.updatedStatsCount)
    }
}

private class FakeWorkoutSessionRepository : WorkoutSessionRepository {
    override suspend fun startSession(uid: String, session: com.example.fitty.domain.model.WorkoutSession): Result<String> = Result.success("session-1")

    override suspend fun getSession(uid: String, sessionId: String): com.example.fitty.domain.model.WorkoutSession? {
        return com.example.fitty.domain.model.WorkoutSession(id = sessionId, title = "Leg Day")
    }

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

    override suspend fun updateExerciseLog(
        uid: String,
        sessionId: String,
        exercise: ExerciseLog
    ): Result<Unit> = Result.success(Unit)

    override suspend fun abandonSession(uid: String, sessionId: String): Result<Unit> = Result.success(Unit)

    override suspend fun getRecentSessions(uid: String, limit: Int): List<com.example.fitty.domain.model.WorkoutSession> = emptyList()
}

private class FakePlanRepository(
    private val updateStatusResult: Result<Unit> = Result.success(Unit)
) : PlanRepository {
    override suspend fun getActivePlan(uid: String) = null
    override suspend fun getPlanInstance(uid: String, planId: String) = null
    override suspend fun getAllPlans(uid: String) = emptyList<com.example.fitty.domain.model.PlanInstance>()
    override suspend fun savePlanInstance(uid: String, plan: com.example.fitty.domain.model.PlanInstance): Result<String> = Result.success("plan-1")
    override suspend fun updatePlanStatus(uid: String, planId: String, status: String): Result<Unit> = Result.success(Unit)
    override suspend fun deletePlan(uid: String, planId: String): Result<Unit> = Result.success(Unit)
    override suspend fun getScheduledWorkouts(uid: String, planId: String, dateKey: String?) = emptyList<com.example.fitty.domain.model.ScheduledWorkout>()
    override suspend fun getScheduledWorkout(uid: String, planId: String, workoutId: String) = null
    override suspend fun saveScheduledWorkout(uid: String, planId: String, workout: com.example.fitty.domain.model.ScheduledWorkout): Result<String> = Result.success("scheduled-1")
    override suspend fun updateScheduledWorkoutStatus(uid: String, planId: String, workoutId: String, status: String): Result<Unit> = updateStatusResult
    override suspend fun replaceScheduledWorkout(uid: String, planId: String, workoutId: String, newWorkout: com.example.fitty.domain.model.ScheduledWorkout): Result<String> = Result.success("scheduled-2")
    override suspend fun getExerciseLibrary() = emptyList<com.example.fitty.domain.model.Exercise>()
    override suspend fun getExercise(exerciseId: String) = null
    override suspend fun searchExercises(query: String, muscleGroup: String?, difficulty: String?, equipment: String?) = emptyList<com.example.fitty.domain.model.Exercise>()
    override suspend fun getProgramTemplates(goal: String?, difficulty: String?, equipment: String?) = emptyList<com.example.fitty.domain.model.ProgramTemplate>()
    override suspend fun getProgramTemplate(programId: String) = null
}

private class FakeTrackingRepository : TrackingRepository {
    val updatedSummaries = mutableListOf<DailySummary>()

    override suspend fun saveMealLog(uid: String, mealLog: MealLog): Result<String> = Result.success("meal-1")
    override suspend fun getMealLogs(uid: String, dateKey: String): List<MealLog> = emptyList()
    override suspend fun getMealLog(uid: String, mealId: String): MealLog? = null
    override suspend fun deleteMealLog(uid: String, mealId: String): Result<Unit> = Result.success(Unit)
    override suspend fun saveMealScanRecord(uid: String, record: MealScanRecord): Result<String> = Result.success("scan-1")
    override suspend fun getMealScanHistory(uid: String, limit: Int): List<MealScanRecord> = emptyList()
    override suspend fun uploadScanImage(uid: String, localImageUri: String): Result<String> = Result.success("uploaded://image")
    override suspend fun uploadBodyScanImage(uid: String, localImageUri: String): Result<String> = Result.success("uploaded://body")
    override suspend fun saveBodyScan(uid: String, bodyScan: BodyScan): Result<String> = Result.success("body-1")
    override suspend fun getBodyScans(uid: String, limit: Int): List<BodyScan> = emptyList()
    override suspend fun getLatestBodyScan(uid: String): BodyScan? = null
    override suspend fun saveBodyMeasurement(uid: String, measurement: BodyMeasurement): Result<String> = Result.success("measure-1")
    override suspend fun getBodyMeasurements(uid: String, limit: Int): List<BodyMeasurement> = emptyList()
    override suspend fun getDailySummary(uid: String, dateKey: String): DailySummary? = DailySummary(dateKey = dateKey)
    override suspend fun getDailySummaries(uid: String, fromDate: String, toDate: String): List<DailySummary> = emptyList()
    override suspend fun updateDailySummary(uid: String, dateKey: String, summary: DailySummary): Result<Unit> {
        updatedSummaries += summary
        return Result.success(Unit)
    }
    override suspend fun getProgressStats(uid: String, days: Int): ProgressStats = ProgressStats()
}

private class FakeUserRepository : UserRepository {
    var updatedStatsCount = 0

    override suspend fun getCurrentUser(uid: String?): FittyUser {
        return FittyUser(
            uid = "user-1",
            email = "fitty@example.com",
            displayName = "Fitty",
            username = "fitty",
            authProvider = "test",
            guest = false,
            onboardingCompleted = true
        )
    }

    override suspend fun updateProfile(uid: String, profile: FittyProfile): Result<Unit> = Result.success(Unit)
    override suspend fun updateOnboarding(uid: String, onboarding: FittyOnboarding): Result<Unit> = Result.success(Unit)
    override suspend fun updateSettings(uid: String, settings: FittySettings): Result<Unit> = Result.success(Unit)
    override suspend fun updateGoal(uid: String, primaryGoal: String, targetWeightKg: Int?): Result<Unit> = Result.success(Unit)
    override suspend fun updateStats(uid: String, stats: FittyStats): Result<Unit> {
        updatedStatsCount += 1
        return Result.success(Unit)
    }
    override suspend fun deleteUserData(uid: String): Result<Unit> = Result.success(Unit)
    override suspend fun updateDisplayName(uid: String, name: String): Result<Unit> = Result.success(Unit)
    override suspend fun uploadProfilePhoto(uid: String, imageUri: String): Result<String> = Result.success("profile://photo")
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
