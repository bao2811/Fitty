package com.example.fitty.domain.usecase.user

import com.example.fitty.domain.model.FittyOnboarding
import com.example.fitty.domain.model.FittyProfile
import com.example.fitty.domain.model.FittySettings
import com.example.fitty.domain.repository.SessionRepository
import com.example.fitty.domain.repository.UserRepository
import javax.inject.Inject

class UpdateProfileUseCase @Inject constructor(
    private val userRepository: UserRepository,
    private val sessionRepository: SessionRepository
) {
    suspend operator fun invoke(profile: FittyProfile, onboarding: FittyOnboarding? = null): Result<Unit> {
        val uid = sessionRepository.getCurrentUserId()
            ?: return Result.failure(IllegalStateException("Not signed in"))
        val profileResult = userRepository.updateProfile(uid, profile)
        if (profileResult.isFailure) return profileResult
        if (onboarding != null) {
            return userRepository.updateOnboarding(uid, onboarding)
        }
        return Result.success(Unit)
    }
}

class UpdateSettingsUseCase @Inject constructor(
    private val userRepository: UserRepository,
    private val sessionRepository: SessionRepository
) {
    suspend operator fun invoke(settings: FittySettings): Result<Unit> {
        val uid = sessionRepository.getCurrentUserId()
            ?: return Result.failure(IllegalStateException("Not signed in"))
        return userRepository.updateSettings(uid, settings)
    }
}

class UpdateGoalUseCase @Inject constructor(
    private val userRepository: UserRepository,
    private val sessionRepository: SessionRepository
) {
    suspend operator fun invoke(primaryGoal: String, targetWeightKg: Int?): Result<Unit> {
        val uid = sessionRepository.getCurrentUserId()
            ?: return Result.failure(IllegalStateException("Not signed in"))
        return userRepository.updateGoal(uid, primaryGoal, targetWeightKg)
    }
}
