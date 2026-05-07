package com.example.fitty.data.preferences

import com.example.fitty.domain.model.FittyStartupState
import com.example.fitty.domain.model.FittyUser
import com.example.fitty.domain.repository.SessionRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesSessionRepository @Inject constructor(
    private val preferences: AppPreferencesDataSource
) : SessionRepository {
    override suspend fun saveStartupState(state: FittyStartupState) {
        preferences.setCurrentUserId(state.uid)
        preferences.setGuestModeEnabled(state.isGuest)
        preferences.setSignedIn(state.isSignedIn)
        preferences.setOnboardingCompleted(state.onboardingCompleted)
    }

    override suspend fun saveUserSession(user: FittyUser) {
        preferences.setCurrentUserId(user.uid)
        preferences.setSignedIn(!user.guest)
        preferences.setGuestModeEnabled(user.guest)
        preferences.setOnboardingCompleted(user.onboardingCompleted)
    }

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        preferences.setOnboardingCompleted(completed)
    }

    override suspend fun getCurrentUserId(): String? = preferences.currentUserId()

    override suspend fun clearSession() {
        preferences.clearSession()
    }

    override suspend fun shouldShowWelcomeNotification(
        nowMillis: Long,
        cooldownMillis: Long
    ): Boolean {
        return preferences.shouldShowWelcomeNotification(nowMillis, cooldownMillis)
    }

    override suspend fun setLastWelcomeNotificationAt(timestampMillis: Long) {
        preferences.setLastWelcomeNotificationAt(timestampMillis)
    }
}
