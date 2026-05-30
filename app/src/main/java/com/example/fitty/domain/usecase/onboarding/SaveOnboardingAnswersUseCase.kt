package com.example.fitty.domain.usecase.onboarding

import com.example.fitty.domain.model.FittyOnboardingAnswers
import com.example.fitty.domain.repository.OnboardingRepository
import com.example.fitty.domain.repository.SessionRepository
import javax.inject.Inject

class SaveOnboardingAnswersUseCase @Inject constructor(
    private val onboardingRepository: OnboardingRepository,
    private val sessionRepository: SessionRepository
) {
    suspend operator fun invoke(answers: FittyOnboardingAnswers): Result<Unit> {
        val uid = sessionRepository.getCurrentUserId()
            ?: return Result.failure(IllegalStateException("Start a session before saving onboarding"))
        return onboardingRepository.saveOnboardingAnswers(uid, answers)
    }
}
