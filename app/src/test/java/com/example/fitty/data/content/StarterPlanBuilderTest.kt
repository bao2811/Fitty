package com.example.fitty.data.content

import androidx.test.core.app.ApplicationProvider
import com.example.fitty.domain.model.CoachContentConfig
import com.example.fitty.domain.model.ExercisePrescriptionContent
import com.example.fitty.domain.model.FittyOnboardingAnswers
import com.example.fitty.domain.model.HomeContentConfig
import com.example.fitty.domain.model.HomeBehaviorConfig
import com.example.fitty.domain.model.HomeEmptyStateContent
import com.example.fitty.domain.model.HomeTaskCategory
import com.example.fitty.domain.model.HomeTaskTemplateContent
import com.example.fitty.domain.model.OnboardingContentConfig
import com.example.fitty.domain.model.PracticeCategoryContent
import com.example.fitty.domain.model.QuickWorkoutConfig
import com.example.fitty.domain.model.StarterExerciseTemplate
import com.example.fitty.domain.model.StarterPlanTemplate
import com.example.fitty.domain.model.TrackBehaviorConfig
import com.example.fitty.domain.repository.ContentRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [35])
class StarterPlanBuilderTest {

    @Test
    fun `builder keeps preview and saved workouts aligned to same template`() = runTest {
        val builder = StarterPlanBuilder(
            contentRepository = FakeContentRepository(
                starterPlanTemplate = StarterPlanTemplate(
                    goal = "gain_muscle",
                    sourceProgramId = "remote_template",
                    planNameTemplate = "{{goalLabel}} Remote Plan",
                    trainingStyle = "strength",
                    previewTitleTemplate = "{{goalLabel}} Remote Plan",
                    previewSubtitle = "Remote subtitle",
                    previewGoalTitle = "Goal",
                    previewGoalBodyTemplate = "Goal: {{goalLabel}}",
                    previewCaloriesTitle = "Calories",
                    previewCaloriesBodyTemplate = "{{caloriesTarget}}",
                    previewWorkoutDaysTitle = "Days",
                    previewWorkoutDaysBodyTemplate = "{{schedule}}",
                    previewDurationTitle = "Duration",
                    previewDurationBodyTemplate = "{{durationLabel}}",
                    previewWhyTitle = "Why",
                    previewWhyBodyTemplate = "Because {{fitnessLabel}}",
                    scheduledWorkoutTitles = listOf("Remote Day A", "Remote Day B"),
                    explanationTemplate = "Explanation {{preferredTimeLabel}}",
                    exercises = listOf(
                        StarterExerciseTemplate("push_up", "Push Up", 3, reps = "10"),
                        StarterExerciseTemplate("split_squat", "Split Squat", 3, reps = "8", targetWeightMode = StarterExerciseTemplate.TargetWeightMode.LowerBody)
                    )
                )
            ),
            localContentFallbacks = LocalContentFallbacks(ApplicationProvider.getApplicationContext()),
            weightAdvisor = ExerciseWeightAdvisor()
        )

        val result = builder.buildForAnswers(
            answers = FittyOnboardingAnswers(
                goal = "gain_muscle",
                age = 26,
                heightCm = 175,
                weightKg = 72,
                targetWeightKg = 78,
                fitnessLevel = "intermediate",
                workoutDays = setOf("mon", "wed"),
                durationMinutes = 45,
                preferredTime = "evening",
                equipment = "gym",
                injuryNote = "",
                nutrition = "high_protein",
                restrictions = emptySet(),
                reminders = emptySet()
            ),
            language = "en"
        )

        assertEquals("Gain Muscle Remote Plan", result.plan.name)
        assertEquals("remote_template", result.plan.sourceProgramId)
        assertEquals("Gain Muscle Remote Plan", result.preview.title)
        assertEquals("Remote Day A", result.scheduledWorkouts.first().title)
        assertEquals("Push Up", result.preview.exercises.first().name)
        assertEquals(
            result.scheduledWorkouts.first().exercises.map { it.name },
            result.preview.exercises.map { it.name }
        )
        assertTrue(result.plan.explanation.contains("evening"))
    }
}

private class FakeContentRepository(
    private val starterPlanTemplate: StarterPlanTemplate
) : ContentRepository {
    override suspend fun getHomeContent(language: String): HomeContentConfig = HomeContentConfig(
        emptyState = HomeEmptyStateContent("", "", "", ""),
        insightActions = emptyList(),
        suggestedTaskPresets = listOf(HomeTaskTemplateContent("workout", "", "", 0, HomeTaskCategory.Workout, true)),
        defaultTaskTemplates = emptyList()
    )

    override suspend fun getCoachContent(language: String): CoachContentConfig = CoachContentConfig("", emptyList())

    override suspend fun getOnboardingContent(language: String): OnboardingContentConfig =
        LocalContentFallbacks(ApplicationProvider.getApplicationContext()).onboarding(language)

    override suspend fun getHomeBehaviorConfig(): HomeBehaviorConfig = HomeBehaviorConfig()

    override suspend fun getTrackBehaviorConfig(): TrackBehaviorConfig = TrackBehaviorConfig()

    override suspend fun getQuickWorkoutConfig(language: String): QuickWorkoutConfig = QuickWorkoutConfig()

    override suspend fun getPracticeCategories(language: String): List<PracticeCategoryContent> = emptyList()

    override suspend fun getExercisePrescriptions(language: String): List<ExercisePrescriptionContent> = emptyList()

    override suspend fun getStarterPlanTemplate(
        goal: String,
        fitnessLevel: String,
        equipment: String,
        language: String
    ): StarterPlanTemplate = starterPlanTemplate
}
