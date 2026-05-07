package com.example.fitty.domain.usecase.startup

import com.example.fitty.domain.model.StartupDestination
import com.example.fitty.domain.repository.SessionRepository
import com.example.fitty.domain.repository.StartupRepository
import com.example.fitty.notifications.FittyMessagingCoordinator
import kotlinx.coroutines.delay
import javax.inject.Inject

class ResolveStartupDestinationUseCase @Inject constructor(
    private val startupRepository: StartupRepository,
    private val sessionRepository: SessionRepository,
    private val messagingCoordinator: FittyMessagingCoordinator
) {
    suspend operator fun invoke(): StartupDestination {
        delay(1200)
        val session = startupRepository.getStartupState()
        sessionRepository.saveStartupState(session)
        runCatching { messagingCoordinator.syncTokenAndWelcomeUser(session) }
        return when {
            (session.isGuest || session.isSignedIn) && session.onboardingCompleted -> StartupDestination.Main
            session.isGuest || session.isSignedIn -> StartupDestination.Onboarding
            else -> StartupDestination.Welcome
        }
    }
}
