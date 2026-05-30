package com.example.fitty.domain.repository

import com.example.fitty.domain.model.CoachContentConfig
import com.example.fitty.domain.model.ExercisePrescriptionContent
import com.example.fitty.domain.model.HomeContentConfig
import com.example.fitty.domain.model.HomeBehaviorConfig
import com.example.fitty.domain.model.OnboardingContentConfig
import com.example.fitty.domain.model.PracticeCategoryContent
import com.example.fitty.domain.model.QuickWorkoutConfig
import com.example.fitty.domain.model.StarterPlanTemplate
import com.example.fitty.domain.model.TrackBehaviorConfig

interface ContentRepository {
    suspend fun getHomeContent(language: String): HomeContentConfig
    suspend fun getCoachContent(language: String): CoachContentConfig
    suspend fun getOnboardingContent(language: String): OnboardingContentConfig
    suspend fun getHomeBehaviorConfig(): HomeBehaviorConfig
    suspend fun getTrackBehaviorConfig(): TrackBehaviorConfig
    suspend fun getQuickWorkoutConfig(language: String): QuickWorkoutConfig
    suspend fun getPracticeCategories(language: String): List<PracticeCategoryContent>
    suspend fun getExercisePrescriptions(language: String): List<ExercisePrescriptionContent>
    suspend fun getStarterPlanTemplate(
        goal: String,
        fitnessLevel: String,
        equipment: String,
        language: String
    ): StarterPlanTemplate?

    fun usedFallbackFor(key: String): Boolean = false

    fun fallbackDetailFor(key: String): String? = null
}
