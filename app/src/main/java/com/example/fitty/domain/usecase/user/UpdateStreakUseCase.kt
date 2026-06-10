package com.example.fitty.domain.usecase.user

import com.example.fitty.domain.model.withRecalculatedAchievements
import com.example.fitty.domain.repository.SessionRepository
import com.example.fitty.domain.repository.UserRepository
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/**
 * Updates the user's streak based on activity.
 *
 * **Streak Condition:**
 * A day counts as "active" when the user performs at least ONE of:
 * - Signs in / opens the app for the day
 * - Completes a workout session (any duration)
 * - Logs a meal via Track > Meals > Confirm
 * - Saves a body scan via Track > Body > Save
 *
 * The streak increments if the user is active on consecutive days.
 * If user misses a day, streak resets to 1 on next activity.
 * If user is already active today, no changes.
 */
class UpdateStreakUseCase @Inject constructor(
    private val userRepository: UserRepository,
    private val sessionRepository: SessionRepository
) {
    /**
     * @param reason The type of activity: "login", "workout", "meal", or "body_scan"
     */
    suspend operator fun invoke(
        reason: String,
        incrementActivityCounters: Boolean = true
    ): Result<Unit> {
        val uid = sessionRepository.getCurrentUserId()
            ?: return Result.failure(IllegalStateException("Not signed in"))

        val user = userRepository.getCurrentUser(uid)
            ?: return Result.failure(IllegalStateException("User not found"))

        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val stats = user.stats

        // Always increment activity counters regardless of streak status
        val newTotalWorkouts = if (incrementActivityCounters && reason == "workout") {
            stats.totalWorkouts + 1
        } else {
            stats.totalWorkouts
        }
        val newMealsLogged = if (incrementActivityCounters && reason == "meal") {
            stats.mealsLogged + 1
        } else {
            stats.mealsLogged
        }

        // If already active today, only update counters — skip streak recalculation
        if (stats.lastActiveDate == today) {
            val updatedStats = stats.copy(
                totalWorkouts = newTotalWorkouts,
                mealsLogged = newMealsLogged
            ).withRecalculatedAchievements()
            return userRepository.updateStats(uid, updatedStats)
        }

        val yesterday = LocalDate.now().minusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE)
        val isConsecutive = stats.lastActiveDate == yesterday

        val newStreak = if (isConsecutive) stats.currentStreak + 1 else 1
        val newBest = maxOf(stats.bestStreak, newStreak)

        // Keep last 7 active dates for UI display
        val newActiveDates = (stats.streakActiveDates + today).takeLast(7)

        val updatedStats = stats.copy(
            currentStreak = newStreak,
            bestStreak = newBest,
            lastActiveDate = today,
            streakActiveDates = newActiveDates,
            totalWorkouts = newTotalWorkouts,
            mealsLogged = newMealsLogged
        ).withRecalculatedAchievements()

        return userRepository.updateStats(uid, updatedStats)
    }
}
