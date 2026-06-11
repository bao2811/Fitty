package com.example.fitty.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FittyAchievementCatalogTest {
    @Test
    fun `unlocked counts follow workout meal and streak milestones`() {
        val stats = FittyStats(
            totalWorkouts = 5,
            mealsLogged = 3,
            bestStreak = 7
        )

        val unlocked = FittyAchievementCatalog.unlocked(stats)

        assertEquals(6, unlocked.size)
        assertTrue(unlocked.all { it.unlocked })
    }

    @Test
    fun `next locked milestone uses catalog order and clamped progress`() {
        val stats = FittyStats(totalWorkouts = 1)

        val next = FittyAchievementCatalog.nextLocked(stats)

        assertEquals(FittyAchievementCatalog.FIVE_WORKOUTS, next?.id)
        assertEquals(1, next?.displayProgress)
        assertEquals(5, next?.target)
    }
}
