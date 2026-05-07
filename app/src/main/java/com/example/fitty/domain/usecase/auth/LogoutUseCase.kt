package com.example.fitty.domain.usecase.auth

import com.example.fitty.domain.repository.AuthRepository
import com.example.fitty.domain.repository.SessionRepository
import javax.inject.Inject

class LogoutUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val sessionRepository: SessionRepository
) {
    suspend operator fun invoke() {
        authRepository.signOut()
        sessionRepository.clearSession()
    }
}
