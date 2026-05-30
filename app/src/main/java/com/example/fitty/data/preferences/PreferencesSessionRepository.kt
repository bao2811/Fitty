package com.example.fitty.data.preferences

import com.example.fitty.domain.model.FittyStartupState
import com.example.fitty.domain.model.FittyUser
import com.example.fitty.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
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
        if (!state.isSignedIn) {
            preferences.setLastSignedInAt(null)
        }
    }

    override suspend fun saveUserSession(user: FittyUser) {
        preferences.setCurrentUserId(user.uid)
        preferences.setSignedIn(!user.guest)
        preferences.setGuestModeEnabled(user.guest)
        preferences.setOnboardingCompleted(user.onboardingCompleted)
        preferences.setAppLanguage(user.settings.language)
        preferences.setLastSignedInAt(
            if (user.guest) null else System.currentTimeMillis()
        )
    }

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        preferences.setOnboardingCompleted(completed)
    }

    override suspend fun getCurrentUserId(): String? = preferences.currentUserId()

    override fun observeCurrentUserId(): Flow<String?> = preferences.currentUserId

    override suspend fun getAppLanguage(): String? = preferences.appLanguage()

    override suspend fun setAppLanguage(language: String) {
        preferences.setAppLanguage(language)
    }

    override suspend fun clearSession() {
        preferences.clearSession()
    }

    override suspend fun isSignedInSessionExpired(
        nowMillis: Long,
        maxAgeMillis: Long
    ): Boolean {
        return preferences.isSignedInSessionExpired(nowMillis, maxAgeMillis)
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
