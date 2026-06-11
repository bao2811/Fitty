package com.example.fitty.feature_onboarding

import androidx.test.core.app.ApplicationProvider
import com.example.fitty.data.content.LocalContentFallbacks
import com.example.fitty.domain.model.CoachContentConfig
import com.example.fitty.domain.model.ExercisePrescriptionContent
import com.example.fitty.domain.model.FittyOnboardingAnswers
import com.example.fitty.domain.model.FittyStartupState
import com.example.fitty.domain.model.FittyUser
import com.example.fitty.domain.model.HomeBehaviorConfig
import com.example.fitty.domain.model.HomeContentConfig
import com.example.fitty.domain.model.OnboardingContentConfig
import com.example.fitty.domain.model.PracticeCategoryContent
import com.example.fitty.domain.model.QuickWorkoutConfig
import com.example.fitty.domain.model.StarterPlanTemplate
import com.example.fitty.domain.model.TrackBehaviorConfig
import com.example.fitty.domain.repository.ContentRepository
import com.example.fitty.domain.repository.OnboardingRepository
import com.example.fitty.domain.repository.SessionRepository
import com.example.fitty.domain.usecase.onboarding.SaveOnboardingAnswersUseCase
import com.example.fitty.feature_track.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [35])
class OnboardingViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `initial state starts with fallback content before remote load completes`() = runTest {
        val viewModel = onboardingHarness().viewModel()

        val state = viewModel.uiState.value

        assertNotNull(state.content)
        assertEquals(0, state.step)
        assertTrue(state.contentSources.first().detail.orEmpty().contains("fallback", ignoreCase = true))
    }

    @Test
    fun `numeric fields strip non digits`() = runTest {
        val viewModel = onboardingHarness().viewModel()

        viewModel.updateAge("2a9")
        viewModel.updateHeight("1x75cm")
        viewModel.updateWeight("7.2kg")
        viewModel.updateTargetWeight("68!")

        val state = viewModel.uiState.value
        assertEquals("29", state.age)
        assertEquals("175", state.height)
        assertEquals("72", state.weight)
        assertEquals("68", state.targetWeight)
    }

    @Test
    fun `validation prevents advancing each required step`() = runTest {
        val viewModel = onboardingHarness().viewModel()

        viewModel.next {}
        assertEquals(0, viewModel.uiState.value.step)
        assertNotNull(viewModel.uiState.value.errorMessage)

        viewModel.selectGoal("lose_weight")
        viewModel.next {}
        assertEquals(1, viewModel.uiState.value.step)
        viewModel.next {}
        assertEquals(1, viewModel.uiState.value.step)
        assertNotNull(viewModel.uiState.value.errorMessage)

        fillBodyMetrics(viewModel)
        viewModel.next {}
        assertEquals(2, viewModel.uiState.value.step)
        viewModel.next {}
        assertEquals(2, viewModel.uiState.value.step)

        viewModel.selectFitnessLevel("beginner")
        viewModel.next {}
        assertEquals(3, viewModel.uiState.value.step)
        viewModel.next {}
        assertEquals(3, viewModel.uiState.value.step)

        viewModel.toggleWorkoutDay("monday")
        viewModel.next {}
        assertEquals(4, viewModel.uiState.value.step)
        viewModel.next {}
        assertEquals(4, viewModel.uiState.value.step)

        viewModel.selectPreferredTime("morning")
        viewModel.next {}
        assertEquals(5, viewModel.uiState.value.step)
        viewModel.next {}
        assertEquals(5, viewModel.uiState.value.step)

        viewModel.selectDuration("30 min")
        viewModel.next {}
        assertEquals(6, viewModel.uiState.value.step)
        viewModel.next {}
        assertEquals(6, viewModel.uiState.value.step)

        viewModel.selectEquipment("home")
        viewModel.next {}
        assertEquals(7, viewModel.uiState.value.step)
        viewModel.next {}
        assertEquals(7, viewModel.uiState.value.step)
    }

    @Test
    fun `happy path saves mapped answers and calls finished`() = runTest {
        val onboardingRepository = FakeOnboardingRepository()
        val viewModel = onboardingHarness(onboardingRepository = onboardingRepository).viewModel()
        var finished = false

        completeRequiredAnswers(viewModel)
        viewModel.toggleRestriction("vegetarian")
        viewModel.updateInjuryNote("knee")
        viewModel.next {}
        viewModel.toggleReminder("morning")
        viewModel.next { finished = true }
        advanceUntilIdle()

        val answers = onboardingRepository.savedAnswers
        assertTrue(finished)
        assertNotNull(answers)
        assertEquals("lose_weight", answers?.goal)
        assertEquals(29, answers?.age)
        assertEquals(175, answers?.heightCm)
        assertEquals(72, answers?.weightKg)
        assertEquals(68, answers?.targetWeightKg)
        assertEquals(30, answers?.durationMinutes)
        assertEquals(setOf("monday"), answers?.workoutDays)
        assertEquals(setOf("vegetarian"), answers?.restrictions)
        assertEquals(setOf("morning"), answers?.reminders)
        assertEquals("knee", answers?.injuryNote)
    }

    @Test
    fun `save failure keeps final step and shows error`() = runTest {
        val onboardingRepository = FakeOnboardingRepository(
            saveResult = Result.failure(IllegalStateException("Save failed"))
        )
        val viewModel = onboardingHarness(onboardingRepository = onboardingRepository).viewModel()
        var finished = false

        completeRequiredAnswers(viewModel)
        viewModel.next {}
        viewModel.next { finished = true }
        advanceUntilIdle()

        assertFalse(finished)
        assertEquals(8, viewModel.uiState.value.step)
        assertEquals("Save failed", viewModel.uiState.value.errorMessage)
    }

    private fun onboardingHarness(
        onboardingRepository: FakeOnboardingRepository = FakeOnboardingRepository(),
        sessionRepository: FakeOnboardingSessionRepository = FakeOnboardingSessionRepository(),
        contentRepository: ContentRepository = FakeOnboardingContentRepository()
    ): OnboardingHarness = OnboardingHarness(onboardingRepository, sessionRepository, contentRepository)

    private fun fillBodyMetrics(viewModel: OnboardingViewModel) {
        viewModel.updateAge("29")
        viewModel.updateHeight("175")
        viewModel.updateWeight("72")
        viewModel.updateTargetWeight("68")
    }

    private fun completeRequiredAnswers(viewModel: OnboardingViewModel) {
        viewModel.selectGoal("lose_weight")
        viewModel.next {}
        fillBodyMetrics(viewModel)
        viewModel.next {}
        viewModel.selectFitnessLevel("beginner")
        viewModel.next {}
        viewModel.toggleWorkoutDay("monday")
        viewModel.next {}
        viewModel.selectPreferredTime("morning")
        viewModel.next {}
        viewModel.selectDuration("30 min")
        viewModel.next {}
        viewModel.selectEquipment("home")
        viewModel.next {}
        viewModel.selectNutrition("balanced")
    }
}

private class OnboardingHarness(
    private val onboardingRepository: FakeOnboardingRepository,
    private val sessionRepository: FakeOnboardingSessionRepository,
    private val contentRepository: ContentRepository
) {
    fun viewModel(): OnboardingViewModel {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        return OnboardingViewModel(
            saveOnboardingAnswersUseCase = SaveOnboardingAnswersUseCase(onboardingRepository, sessionRepository),
            localContentFallbacks = LocalContentFallbacks(context),
            contentRepository = contentRepository,
            sessionRepository = sessionRepository,
            context = context
        )
    }
}

private class FakeOnboardingRepository(
    private val saveResult: Result<Unit> = Result.success(Unit)
) : OnboardingRepository {
    var savedUid: String? = null
    var savedAnswers: FittyOnboardingAnswers? = null

    override suspend fun saveOnboardingAnswers(uid: String, answers: FittyOnboardingAnswers): Result<Unit> {
        savedUid = uid
        savedAnswers = answers
        return saveResult
    }

    override suspend fun markOnboardingCompleted(uid: String): Result<Unit> = Result.success(Unit)
}

private class FakeOnboardingSessionRepository : SessionRepository {
    override suspend fun saveStartupState(state: FittyStartupState) = Unit
    override suspend fun saveUserSession(user: FittyUser) = Unit
    override suspend fun setOnboardingCompleted(completed: Boolean) = Unit
    override suspend fun getCurrentUserId(): String? = "uid"
    override fun observeCurrentUserId(): Flow<String?> = flowOf("uid")
    override suspend fun getAppLanguage(): String? = "en"
    override suspend fun setAppLanguage(language: String) = Unit
    override suspend fun clearSession() = Unit
    override suspend fun isSignedInSessionExpired(nowMillis: Long, maxAgeMillis: Long): Boolean = false
    override suspend fun shouldShowWelcomeNotification(nowMillis: Long, cooldownMillis: Long): Boolean = false
    override suspend fun setLastWelcomeNotificationAt(timestampMillis: Long) = Unit
}

private class FakeOnboardingContentRepository : ContentRepository {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val fallbacks = LocalContentFallbacks(context)

    override suspend fun getHomeContent(language: String): HomeContentConfig = fallbacks.home(language)
    override suspend fun getCoachContent(language: String): CoachContentConfig = fallbacks.coach()
    override suspend fun getOnboardingContent(language: String): OnboardingContentConfig = fallbacks.onboarding(language)
    override suspend fun getHomeBehaviorConfig(): HomeBehaviorConfig = fallbacks.homeBehaviorConfig()
    override suspend fun getTrackBehaviorConfig(): TrackBehaviorConfig = fallbacks.trackBehaviorConfig()
    override suspend fun getQuickWorkoutConfig(language: String): QuickWorkoutConfig = fallbacks.quickWorkoutConfig()
    override suspend fun getPracticeCategories(language: String): List<PracticeCategoryContent> = fallbacks.practiceCategories()
    override suspend fun getExercisePrescriptions(language: String): List<ExercisePrescriptionContent> = fallbacks.exercisePrescriptions()
    override suspend fun getStarterPlanTemplate(
        goal: String,
        fitnessLevel: String,
        equipment: String,
        language: String
    ): StarterPlanTemplate? = null
}
