package com.example.fitty.data.content

import com.example.fitty.domain.model.Exercise
import com.example.fitty.domain.model.ExercisePrescriptionContent
import com.example.fitty.domain.model.ExercisePrescriptionRule
import com.example.fitty.domain.model.FittyOnboarding
import com.example.fitty.domain.model.FittyProfile
import com.example.fitty.domain.model.FittySettings
import com.example.fitty.domain.model.FittyStats
import com.example.fitty.domain.model.FittyUser
import com.example.fitty.domain.model.StarterExerciseTemplate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExercisePrescriptionResolverTest {

    private val resolver = ExercisePrescriptionResolver(ExerciseWeightAdvisor())

    @Test
    fun `resolve selects matching rule and calculates target weight from current body weight`() {
        val recommendation = resolver.resolve(
            exercise = exercise(id = "split_squat"),
            user = user(weightKg = 72, fitnessLevel = "intermediate", equipment = "gym", goal = "gain_muscle"),
            language = "en",
            catalog = listOf(
                ExercisePrescriptionContent(
                    exerciseId = "split_squat",
                    rules = listOf(
                        ExercisePrescriptionRule(
                            fitnessLevels = listOf("beginner"),
                            sets = 3,
                            reps = "10/side",
                            targetWeightMode = StarterExerciseTemplate.TargetWeightMode.LowerBody
                        ),
                        ExercisePrescriptionRule(
                            goal = "gain_muscle",
                            fitnessLevels = listOf("intermediate"),
                            equipments = listOf("gym"),
                            minWeightKg = 60,
                            maxWeightKg = 85,
                            sets = 4,
                            reps = "8/side",
                            targetWeightMode = StarterExerciseTemplate.TargetWeightMode.LowerBody,
                            bodyWeightMultiplier = 0.18f,
                            minSuggestedWeightKg = 8f,
                            maxSuggestedWeightKg = 18f
                        )
                    )
                )
            )
        )

        assertEquals(4, recommendation?.sets)
        assertEquals("8/side", recommendation?.reps)
        assertEquals(13.0f, recommendation?.targetWeightKg)
    }

    @Test
    fun `resolve falls back to exercise defaults when firebase has no matching config`() {
        val recommendation = resolver.resolve(
            exercise = exercise(id = "push_up", defaultRepsText = "12"),
            user = null,
            language = "vi",
            catalog = emptyList()
        )

        assertEquals(3, recommendation?.sets)
        assertEquals("12", recommendation?.reps)
        assertNull(recommendation?.targetWeightKg)
    }

    @Test
    fun `resolve creates strength prescription for weighted exercise without firebase config`() {
        val recommendation = resolver.resolve(
            exercise = exercise(
                id = "barbell_bench_press",
                bodyPart = "chest",
                target = "pectorals",
                equipment = "barbell",
                defaultDurationSeconds = 30
            ),
            user = user(weightKg = 80, fitnessLevel = "intermediate", equipment = "home_none", goal = "gain_muscle"),
            language = "en",
            catalog = emptyList()
        )

        assertEquals(3, recommendation?.sets)
        assertEquals("8-12", recommendation?.reps)
        assertEquals(8.0f, recommendation?.targetWeightKg)
        assertNull(recommendation?.durationSeconds)
    }

    private fun exercise(
        id: String,
        defaultRepsText: String = "",
        bodyPart: String = "",
        target: String = "",
        equipment: String = "",
        defaultDurationSeconds: Int? = null
    ): Exercise {
        return Exercise(
            id = id,
            name = id,
            description = "",
            muscleGroup = "",
            bodyPart = bodyPart,
            target = target,
            equipment = equipment,
            difficulty = "",
            caloriesBurned = 0,
            durationSeconds = 0,
            defaultDurationSeconds = defaultDurationSeconds,
            defaultRepsText = defaultRepsText
        )
    }

    private fun user(
        weightKg: Int,
        fitnessLevel: String,
        equipment: String,
        goal: String
    ): FittyUser {
        return FittyUser(
            uid = "u1",
            email = "demo@example.com",
            displayName = "Demo",
            username = "demo",
            authProvider = "password",
            guest = false,
            onboardingCompleted = true,
            profile = FittyProfile(
                weightKg = weightKg,
                fitnessLevel = fitnessLevel,
                primaryGoal = goal
            ),
            onboarding = FittyOnboarding(
                equipmentAccess = equipment
            ),
            stats = FittyStats(),
            settings = FittySettings()
        )
    }
}
