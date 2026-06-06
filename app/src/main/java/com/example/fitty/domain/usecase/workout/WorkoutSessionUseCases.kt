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
            val planUpdateResult = planRepository.updateScheduledWorkoutStatus(uid, planId, scheduledWorkoutId, "completed")
            if (planUpdateResult.isFailure) {
                return Result.failure(planUpdateResult.exceptionOrNull() ?: IllegalStateException("Could not update plan workout status"))
            }
        }

        // 3. Update daily summary (create if not exists)
        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val existing = trackingRepository.getDailySummary(uid, today)
        val user = userRepository.getCurrentUser(uid)
        val baseSummary = existing ?: com.example.fitty.domain.model.DailySummary(
            dateKey = today,
            targets = com.example.fitty.domain.model.DailySummaryTargets(
                calories = user?.settings?.calorieTarget ?: 2100,
                waterMl = user?.settings?.waterGoalMl ?: 2500
            )
        )
        val sessionTitle = workoutSessionRepository.getSession(uid, sessionId)?.title.orEmpty()
        val updated = baseSummary.copy(
            todayWorkoutTitle = sessionTitle.ifBlank { baseSummary.todayWorkoutTitle },
            progress = baseSummary.progress.copy(
                workoutsCompleted = baseSummary.progress.workoutsCompleted + 1,
                caloriesBurned = baseSummary.progress.caloriesBurned + caloriesBurned,
                activeMinutes = baseSummary.progress.activeMinutes + durationMinutes
            )
        )
        val summaryUpdateResult = trackingRepository.updateDailySummary(uid, today, updated)
        if (summaryUpdateResult.isFailure) {
            return Result.failure(summaryUpdateResult.exceptionOrNull() ?: IllegalStateException("Could not update daily summary"))
        }

        // 4. Update user stats (increment totalWorkouts)
        if (user != null) {
            val updatedStats = user.stats.copy(
                totalWorkouts = user.stats.totalWorkouts + 1
            )
            val statsUpdateResult = userRepository.updateStats(uid, updatedStats)
            if (statsUpdateResult.isFailure) {
                return Result.failure(statsUpdateResult.exceptionOrNull() ?: IllegalStateException("Could not update workout stats"))
            }
        }

        return Result.success(Unit)
    }
}
