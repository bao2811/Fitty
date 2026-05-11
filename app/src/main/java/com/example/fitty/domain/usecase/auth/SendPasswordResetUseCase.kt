package com.example.fitty.domain.usecase.auth

import com.example.fitty.domain.repository.AuthRepository
import javax.inject.Inject

class SendPasswordResetUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(email: String): Result<Unit> {
        val trimmed = email.trim()
        if (trimmed.isBlank() || "@" !in trimmed) {
            return Result.failure(IllegalArgumentException("Enter a valid email address"))
        }
        return authRepository.sendPasswordReset(trimmed)
    }
}
