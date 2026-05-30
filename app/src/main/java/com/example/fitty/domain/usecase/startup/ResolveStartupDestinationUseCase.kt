package com.example.fitty.domain.usecase.startup

import com.example.fitty.domain.model.StartupDestination
import com.example.fitty.domain.repository.AuthRepository
import com.example.fitty.domain.repository.SessionRepository
import com.example.fitty.domain.repository.StartupRepository
import com.example.fitty.notifications.FittyMessagingCoordinator
import kotlinx.coroutines.delay
import javax.inject.Inject

class ResolveStartupDestinationUseCase @Inject constructor(
    private val startupRepository: StartupRepository,
    private val sessionRepository: SessionRepository,
    private val authRepository: AuthRepository,
    private val messagingCoordinator: FittyMessagingCoordinator
) {
    suspend operator fun invoke(): StartupDestination {
        delay(1200)
        val session = startupRepository.getStartupState()
        val now = System.currentTimeMillis()
        if (session.isSignedIn &&
            sessionRepository.isSignedInSessionExpired(
                nowMillis = now,
                maxAgeMillis = SIGNED_IN_SESSION_MAX_AGE_MS
            )
        ) {
            authRepository.signOut()
            sessionRepository.clearSession()
            return StartupDestination.SignIn
        }
        sessionRepository.saveStartupState(session)
        runCatching { messagingCoordinator.syncTokenAndWelcomeUser(session) }
        return when {
            session.isSignedIn && session.onboardingCompleted -> StartupDestination.Main
            session.isSignedIn -> StartupDestination.Onboarding
            session.isGuest && session.onboardingCompleted -> StartupDestination.Main
            session.isGuest -> StartupDestination.Onboarding
            else -> StartupDestination.SignIn
        }
    }

    private companion object {
        const val SIGNED_IN_SESSION_MAX_AGE_MS = 7L * 24 * 60 * 60 * 1000
    }
}
