package com.example.fitty.notifications

import android.content.Context
import com.example.fitty.data.firebase.FittyUser
import com.example.fitty.data.firebase.FittyFirebaseRepository
import com.example.fitty.data.firebase.FittyStartupState
import com.example.fitty.data.preferences.AppPreferencesDataSource
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await

class FittyMessagingCoordinator(
    private val context: Context,
    private val repository: FittyFirebaseRepository,
    private val preferences: AppPreferencesDataSource
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
            displayName = user.displayName,
            forceNotification = forceNotification
        )
    }

    private suspend fun syncNotificationToken() {
        val token = FirebaseMessaging.getInstance().token.await()
        repository.syncNotificationToken(token)
    }

    private suspend fun maybeShowWelcomeBackNotification(
        displayName: String,
        forceNotification: Boolean = false
    ) {
        val now = System.currentTimeMillis()
        if (!forceNotification && !preferences.shouldShowWelcomeNotification(now, WELCOME_BACK_COOLDOWN_MS)) return
        FittyNotificationManager.showWelcomeBackNotification(
            context = context,
            displayName = displayName
        )
        preferences.setLastWelcomeNotificationAt(now)
    }

    private companion object {
        const val WELCOME_BACK_COOLDOWN_MS = 12 * 60 * 60 * 1000L
    }
}
