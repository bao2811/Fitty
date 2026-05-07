package com.example.fitty.domain.usecase.auth

import com.example.fitty.domain.model.FittyAuthResult
import com.example.fitty.domain.repository.AuthRepository
import com.example.fitty.domain.repository.SessionRepository
import com.example.fitty.notifications.FittyMessagingCoordinator
import javax.inject.Inject

class SignUpUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val sessionRepository: SessionRepository,
    private val messagingCoordinator: FittyMessagingCoordinator
) {
    suspend operator fun invoke(username: String, email: String, password: String): FittyAuthResult {
        val result = authRepository.createPasswordUser(username, email, password)
        val user = result.user ?: return result
        sessionRepository.saveUserSession(user)
        runCatching {
            messagingCoordinator.syncTokenAndWelcomeUser(
                user = user,
                forceNotification = true
            )
        }
        return result
    }
}
