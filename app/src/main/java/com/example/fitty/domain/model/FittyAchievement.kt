package com.example.fitty.domain.model

data class FittyAchievementProgress(
    val id: String,
    val progress: Int,
    val target: Int
) {
    val unlocked: Boolean
        get() = progress >= target

    val displayProgress: Int
        get() = progress.coerceIn(0, target)
}

object FittyAchievementCatalog {
    const val FIRST_WORKOUT = "first_workout"
    const val FIVE_WORKOUTS = "five_workouts"
    const val FIRST_MEAL = "first_meal"
    const val THREE_MEALS = "three_meals"
    const val THREE_DAY_STREAK = "three_day_streak"
    const val SEVEN_DAY_STREAK = "seven_day_streak"

    fun evaluate(stats: FittyStats): List<FittyAchievementProgress> {
        val streakProgress = maxOf(stats.currentStreak, stats.bestStreak)
        return listOf(
            milestone(FIRST_WORKOUT, stats.totalWorkouts, 1),
            milestone(FIVE_WORKOUTS, stats.totalWorkouts, 5),
            milestone(FIRST_MEAL, stats.mealsLogged, 1),
            milestone(THREE_MEALS, stats.mealsLogged, 3),
            milestone(THREE_DAY_STREAK, streakProgress, 3),
            milestone(SEVEN_DAY_STREAK, streakProgress, 7)
        )
    }

    fun unlocked(stats: FittyStats): List<FittyAchievementProgress> {
        return evaluate(stats).filter { it.unlocked }
    }

    fun nextLocked(stats: FittyStats): FittyAchievementProgress? {
        return evaluate(stats).firstOrNull { !it.unlocked }
    }

    private fun milestone(id: String, progress: Int, target: Int): FittyAchievementProgress {
        return FittyAchievementProgress(id = id, progress = progress.coerceAtLeast(0), target = target)
    }
}

fun FittyStats.withRecalculatedAchievements(): FittyStats {
    return copy(achievementsUnlocked = FittyAchievementCatalog.unlocked(this).size)
}
