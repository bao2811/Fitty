package com.example.fitty.domain.usecase.auth

import com.example.fitty.domain.repository.AuthRepository
import com.example.fitty.domain.repository.SessionRepository
import com.example.fitty.domain.repository.UserRepository
import javax.inject.Inject

class DeleteAccountUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val sessionRepository: SessionRepository
) {
    suspend operator fun invoke(): Result<Unit> {
        val uid = sessionRepository.getCurrentUserId()
            ?: return Result.failure(IllegalStateException("Not signed in"))
        val deleteDataResult = userRepository.deleteUserData(uid)
        if (deleteDataResult.isFailure) return deleteDataResult
        val deleteAuthResult = authRepository.deleteAccount()
        if (deleteAuthResult.isFailure) return deleteAuthResult
        sessionRepository.clearSession()
        return Result.success(Unit)
    }

    suspend fun reauthenticateWithPassword(password: String): Result<Unit> {
        return authRepository.reauthenticateWithPassword(password)
    }

    suspend fun reauthenticateWithGoogle(idToken: String): Result<Unit> {
        return authRepository.reauthenticateWithGoogle(idToken)
    }
}
