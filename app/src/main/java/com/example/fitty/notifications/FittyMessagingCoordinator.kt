package com.example.fitty.notifications

import com.example.fitty.domain.model.FittyStartupState
import com.example.fitty.domain.model.FittyUser
import com.example.fitty.domain.model.preferredDisplayName
import com.example.fitty.domain.repository.NotificationTokenRepository
import com.example.fitty.domain.repository.SessionRepository
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FittyMessagingCoordinator @Inject constructor(
    private val firebaseMessaging: FirebaseMessaging,
    private val notificationTokenRepository: NotificationTokenRepository,
    private val sessionRepository: SessionRepository,
    private val notificationDispatcher: FittyNotificationDispatcher
) {
    suspend fun syncTokenAndWelcomeUser(session: FittyStartupState) {
        if (!session.isSignedIn || session.isGuest) return

        runCatching { syncNotificationToken() }
        maybeShowWelcomeBackNotification(session.displayName)
    }

    suspend fun syncTokenAndWelcomeUser(
        user: FittyUser,
        forceNotification: Boolean = false
    ) {
        if (user.guest) return

        runCatching { syncNotificationToken() }
        maybeShowWelcomeBackNotification(
            displayName = user.preferredDisplayName(),
            forceNotification = forceNotification
        )
    }

    private suspend fun syncNotificationToken() {
        val token = firebaseMessaging.token.await()
        notificationTokenRepository.syncNotificationToken(token)
    }

    private suspend fun maybeShowWelcomeBackNotification(
        displayName: String,
        forceNotification: Boolean = false
    ) {
        val now = System.currentTimeMillis()
        if (!forceNotification && !sessionRepository.shouldShowWelcomeNotification(now, WELCOME_BACK_COOLDOWN_MS)) return
        notificationDispatcher.showWelcomeBackNotification(displayName)
        sessionRepository.setLastWelcomeNotificationAt(now)
    }

    private companion object {
        const val WELCOME_BACK_COOLDOWN_MS = 12 * 60 * 60 * 1000L
    }
}
