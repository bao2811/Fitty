package com.example.fitty.domain.usecase.onboarding

import com.example.fitty.domain.repository.OnboardingRepository
import com.example.fitty.domain.repository.SessionRepository
import javax.inject.Inject

class CompleteOnboardingUseCase @Inject constructor(
    private val onboardingRepository: OnboardingRepository,
    private val sessionRepository: SessionRepository
) {
    suspend operator fun invoke() {
        val uid = sessionRepository.getCurrentUserId() ?: return
        onboardingRepository.markOnboardingCompleted(uid)
        sessionRepository.setOnboardingCompleted(true)
    }
}
