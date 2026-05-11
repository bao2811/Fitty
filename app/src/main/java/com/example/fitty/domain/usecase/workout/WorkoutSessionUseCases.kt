package com.example.fitty.domain.usecase.workout

import com.example.fitty.domain.model.ExerciseLog
import com.example.fitty.domain.model.ScheduledWorkout
import com.example.fitty.domain.model.WorkoutSession
import com.example.fitty.domain.repository.PlanRepository
import com.example.fitty.domain.repository.SessionRepository
import com.example.fitty.domain.repository.TrackingRepository
import com.example.fitty.domain.repository.UserRepository
import com.example.fitty.domain.repository.WorkoutSessionRepository
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class StartWorkoutSessionUseCase @Inject constructor(
    private val workoutSessionRepository: WorkoutSessionRepository,
    private val sessionRepository: SessionRepository
) {
    suspend operator fun invoke(
        scheduledWorkout: ScheduledWorkout,
        planId: String
    ): Result<String> {
        val uid = sessionRepository.getCurrentUserId()
            ?: return Result.failure(IllegalStateException("Not signed in"))
        val session = WorkoutSession(
            planId = planId,
            scheduledWorkoutId = scheduledWorkout.id,
            title = scheduledWorkout.title,
            source = "plan",
            status = "in_progress",
            startedAt = System.currentTimeMillis(),
            exercises = scheduledWorkout.exercises.mapIndexed { index, ex ->
                ExerciseLog(
                    exerciseId = ex.exerciseId,
                    name = ex.name,
                    orderIndex = index,
                    plannedSets = ex.sets,
                    completedSets = 0,
                    completed = false
                )
            }
        )
        return workoutSessionRepository.startSession(uid, session)
    }
}

class CompleteWorkoutSessionUseCase @Inject constructor(
    private val workoutSessionRepository: WorkoutSessionRepository,
    private val planRepository: PlanRepository,
    private val trackingRepository: TrackingRepository,
    private val userRepository: UserRepository,
    private val sessionRepository: SessionRepository
) {
    suspend operator fun invoke(
        sessionId: String,
        durationMinutes: Int,
        caloriesBurned: Int,
        completionRate: Float,
        perceivedEffort: Int?,
        exercises: List<ExerciseLog>,
        planId: String,
        scheduledWorkoutId: String
    ): Result<Unit> {
        val uid = sessionRepository.getCurrentUserId()
            ?: return Result.failure(IllegalStateException("Not signed in"))

        // 1. Complete the session
        val result = workoutSessionRepository.completeSession(
            uid, sessionId, durationMinutes, caloriesBurned, completionRate, perceivedEffort, exercises
        )
        if (result.isFailure) return result

        // 2. Update scheduled workout status
        if (planId.isNotBlank() && scheduledWorkoutId.isNotBlank()) {
            planRepository.updateScheduledWorkoutStatus(uid, planId, scheduledWorkoutId, "completed")
        }

        // 3. Update daily summary
        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val existing = trackingRepository.getDailySummary(uid, today)
        if (existing != null) {
            val updated = existing.copy(
                progress = existing.progress.copy(
                    workoutsCompleted = existing.progress.workoutsCompleted + 1
                ),
                todayWorkoutTitle = "",
                currentStreak = existing.currentStreak + 1
            )
            trackingRepository.updateDailySummary(uid, today, updated)
        }

        // 4. Update user stats
        val user = userRepository.getCurrentUser(uid)
        if (user != null) {
            val newStreak = user.stats.currentStreak + 1
            val newBest = maxOf(user.stats.bestStreak, newStreak)
            val updatedProfile = user.profile
            userRepository.updateProfile(uid, updatedProfile)
        }

        return Result.success(Unit)
    }
}
