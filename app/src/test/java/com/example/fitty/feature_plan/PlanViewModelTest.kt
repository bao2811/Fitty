package com.example.fitty.feature_plan

import androidx.test.core.app.ApplicationProvider
import com.example.fitty.data.content.LocalContentFallbacks
import com.example.fitty.domain.model.CoachContentConfig
import com.example.fitty.domain.model.Exercise
import com.example.fitty.domain.model.ExercisePrescriptionContent
import com.example.fitty.domain.model.ExerciseQuery
import com.example.fitty.domain.model.ExerciseSyncReport
import com.example.fitty.domain.model.ExerciseSyncState
import com.example.fitty.domain.model.FittyStartupState
import com.example.fitty.domain.model.FittyUser
import com.example.fitty.domain.model.HomeContentConfig
import com.example.fitty.domain.model.HomeBehaviorConfig
import com.example.fitty.domain.model.HomeEmptyStateContent
import com.example.fitty.domain.model.HomeTaskCategory
import com.example.fitty.domain.model.HomeTaskTemplateContent
import com.example.fitty.domain.model.OnboardingContentConfig
import com.example.fitty.domain.model.PracticeCategoryContent
import com.example.fitty.domain.model.QuickWorkoutConfig
import com.example.fitty.domain.model.StarterPlanTemplate
import com.example.fitty.domain.model.TrackBehaviorConfig
import com.example.fitty.domain.repository.ContentRepository
import com.example.fitty.domain.repository.ExerciseCatalogRepository
import com.example.fitty.domain.repository.SessionRepository
import com.example.fitty.domain.usecase.exercise.ObserveExerciseSyncStateUseCase
import com.example.fitty.domain.usecase.exercise.SyncExercisesUseCase
import com.example.fitty.feature_track.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [35])
class PlanViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `plan view model uses category metadata from content repository`() = runTest {
        val exerciseRepository = FakeExerciseCatalogRepository(
            listOf(
                Exercise(id = "1", bodyPart = "back", name = "Row"),
                Exercise(id = "2", bodyPart = "back", name = "Pull Down"),
                Exercise(id = "3", bodyPart = "cardio", name = "Bike")
            )
        )
        val viewModel = PlanViewModel(
            context = ApplicationProvider.getApplicationContext(),
            localContentFallbacks = LocalContentFallbacks(ApplicationProvider.getApplicationContext()),
            contentRepository = FakePlanContentRepository(),
            exerciseRepository = exerciseRepository,
            sessionRepository = FakePlanSessionRepository(),
            observeExerciseSyncStateUseCase = ObserveExerciseSyncStateUseCase(exerciseRepository),
            syncExercisesUseCase = SyncExercisesUseCase(exerciseRepository)
        )

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Posterior Chain", state.categories.first().def.label)
        assertEquals(2, state.categories.first().exerciseCount)
        assertTrue(state.categories.any { it.def.id == "conditioning" && it.exerciseCount == 1 })
    }
}

private class FakePlanContentRepository : ContentRepository {
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

    override suspend fun getPracticeCategories(language: String): List<PracticeCategoryContent> {
        return listOf(
            PracticeCategoryContent("back_focus", "Posterior Chain", listOf("back"), "back.png", "#E8DEF8", 0),
            PracticeCategoryContent("conditioning", "Conditioning", listOf("cardio"), "cardiac.png", "#E8DEF8", 1)
        )
    }

    override suspend fun getExercisePrescriptions(language: String): List<ExercisePrescriptionContent> = emptyList()

    override suspend fun getStarterPlanTemplate(
        goal: String,
        fitnessLevel: String,
        equipment: String,
        language: String
    ): StarterPlanTemplate? = null
}

private class FakeExerciseCatalogRepository(
    exercises: List<Exercise>
) : ExerciseCatalogRepository {
    private val exerciseFlow = MutableStateFlow(exercises)

    override fun observeExercises(query: ExerciseQuery): Flow<List<Exercise>> = exerciseFlow
    override fun observeExercise(exerciseId: String): Flow<Exercise?> = flowOf(exerciseFlow.value.firstOrNull { it.id == exerciseId })
    override fun observeSyncState(): Flow<ExerciseSyncState> = flowOf(ExerciseSyncState())
    override suspend fun getExercise(exerciseId: String): Exercise? = exerciseFlow.value.firstOrNull { it.id == exerciseId }
    override suspend fun getExercises(query: ExerciseQuery): List<Exercise> = exerciseFlow.value
    override suspend fun getRecentlyViewed(limit: Int): List<Exercise> = emptyList()
    override suspend fun upsertExercises(exercises: List<Exercise>) = Unit
    override suspend fun syncExercises(force: Boolean): Result<ExerciseSyncReport> = Result.success(ExerciseSyncReport(usable = exerciseFlow.value.size))
    override suspend fun updateFavorite(exerciseId: String, isFavorite: Boolean) = Unit
    override suspend fun recordRecentlyViewed(exerciseId: String) = Unit
}

private class FakePlanSessionRepository : SessionRepository {
    override suspend fun saveStartupState(state: FittyStartupState) = Unit
    override suspend fun saveUserSession(user: FittyUser) = Unit
    override suspend fun setOnboardingCompleted(completed: Boolean) = Unit
    override suspend fun getCurrentUserId(): String? = "user-1"
    override fun observeCurrentUserId(): Flow<String?> = flowOf("user-1")
    override suspend fun getAppLanguage(): String? = "en"
    override suspend fun setAppLanguage(language: String) = Unit
    override suspend fun clearSession() = Unit
    override suspend fun isSignedInSessionExpired(nowMillis: Long, maxAgeMillis: Long): Boolean = false
    override suspend fun shouldShowWelcomeNotification(nowMillis: Long, cooldownMillis: Long): Boolean = false
    override suspend fun setLastWelcomeNotificationAt(timestampMillis: Long) = Unit
}
