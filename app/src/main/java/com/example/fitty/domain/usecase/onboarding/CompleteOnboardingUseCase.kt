package com.example.fitty.domain.usecase.onboarding

import com.example.fitty.domain.repository.OnboardingRepository
import com.example.fitty.domain.repository.SessionRepository
import javax.inject.Inject

class CompleteOnboardingUseCase @Inject constructor(
    private val onboardingRepository: OnboardingRepository,
    private val sessionRepository: SessionRepository
) {
    suspend operator fun invoke(): Result<Unit> {
        val uid = sessionRepository.getCurrentUserId()
            ?: return Result.failure(IllegalStateException("Not signed in"))
        val result = onboardingRepository.markOnboardingCompleted(uid)
        if (result.isFailure) return result
        sessionRepository.setOnboardingCompleted(true)
        return Result.success(Unit)
    }
}
