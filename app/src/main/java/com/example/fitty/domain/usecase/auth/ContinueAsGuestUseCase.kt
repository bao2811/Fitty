package com.example.fitty.domain.usecase.auth

import com.example.fitty.domain.model.FittyAuthResult
import com.example.fitty.domain.repository.AuthRepository
import com.example.fitty.domain.repository.SessionRepository
import javax.inject.Inject

class ContinueAsGuestUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val sessionRepository: SessionRepository
) {
    suspend operator fun invoke(): FittyAuthResult {
        val result = authRepository.continueAsGuest()
        result.user?.let { user ->
            sessionRepository.saveUserSession(user)
        }
        return result
    }
}
