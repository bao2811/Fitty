package com.example.fitty.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.fittyDataStore by preferencesDataStore(name = "fitty_preferences")

class AppPreferencesDataSource(
    private val context: Context
) {
    val onboardingCompleted: Flow<Boolean> = context.fittyDataStore.data.map { preferences ->
        preferences[ONBOARDING_COMPLETED] ?: false
    }

    val guestModeEnabled: Flow<Boolean> = context.fittyDataStore.data.map { preferences ->
        preferences[GUEST_MODE_ENABLED] ?: false
    }

    val signedIn: Flow<Boolean> = context.fittyDataStore.data.map { preferences ->
        preferences[SIGNED_IN] ?: false
    }

    val currentUserId: Flow<String?> = context.fittyDataStore.data.map { preferences ->
        preferences[CURRENT_USER_ID]
    }

    val appLanguage: Flow<String?> = context.fittyDataStore.data.map { preferences ->
        preferences[APP_LANGUAGE]
    }

    val lastWelcomeNotificationAt: Flow<Long?> = context.fittyDataStore.data.map { preferences ->
        preferences[LAST_WELCOME_NOTIFICATION_AT]
    }

    val lastSignedInAt: Flow<Long?> = context.fittyDataStore.data.map { preferences ->
        preferences[LAST_SIGNED_IN_AT]
    }

    val exerciseGifPreloadCompleted: Flow<Boolean> = context.fittyDataStore.data.map { preferences ->
        preferences[EXERCISE_GIF_PRELOAD_COMPLETED] ?: false
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.fittyDataStore.edit { preferences ->
            preferences[ONBOARDING_COMPLETED] = completed
        }
    }

    suspend fun setGuestModeEnabled(enabled: Boolean) {
        context.fittyDataStore.edit { preferences ->
            preferences[GUEST_MODE_ENABLED] = enabled
        }
    }

    suspend fun setSignedIn(enabled: Boolean) {
        context.fittyDataStore.edit { preferences ->
            preferences[SIGNED_IN] = enabled
        }
    }

    suspend fun setCurrentUserId(userId: String?) {
        context.fittyDataStore.edit { preferences ->
            if (userId == null) {
                preferences.remove(CURRENT_USER_ID)
            } else {
                preferences[CURRENT_USER_ID] = userId
            }
        }
    }

    suspend fun setAppLanguage(language: String?) {
        context.fittyDataStore.edit { preferences ->
            if (language.isNullOrBlank()) {
                preferences.remove(APP_LANGUAGE)
            } else {
                preferences[APP_LANGUAGE] = language
            }
        }
    }

    suspend fun setLastSignedInAt(timestampMillis: Long?) {
        context.fittyDataStore.edit { preferences ->
            if (timestampMillis == null) {
                preferences.remove(LAST_SIGNED_IN_AT)
            } else {
                preferences[LAST_SIGNED_IN_AT] = timestampMillis
            }
        }
    }

    suspend fun clearSession() {
        context.fittyDataStore.edit { preferences ->
            preferences[GUEST_MODE_ENABLED] = false
            preferences[SIGNED_IN] = false
            preferences[ONBOARDING_COMPLETED] = false
            preferences.remove(CURRENT_USER_ID)
            preferences.remove(LAST_SIGNED_IN_AT)
        }
    }

    suspend fun setLastWelcomeNotificationAt(timestampMillis: Long) {
        context.fittyDataStore.edit { preferences ->
            preferences[LAST_WELCOME_NOTIFICATION_AT] = timestampMillis
        }
    }

    suspend fun setExerciseGifPreloadCompleted(completed: Boolean) {
        context.fittyDataStore.edit { preferences ->
            preferences[EXERCISE_GIF_PRELOAD_COMPLETED] = completed
        }
    }

    suspend fun isExerciseGifPreloadCompleted(): Boolean = exerciseGifPreloadCompleted.first()

    suspend fun shouldShowWelcomeNotification(
        nowMillis: Long,
        cooldownMillis: Long
    ): Boolean {
        val lastShownAt = lastWelcomeNotificationAt.first()
        return lastShownAt == null || nowMillis - lastShownAt >= cooldownMillis
    }

    suspend fun isSignedInSessionExpired(
        nowMillis: Long,
        maxAgeMillis: Long
    ): Boolean {
        val lastSignedIn = lastSignedInAt.first() ?: return true
        return nowMillis - lastSignedIn >= maxAgeMillis
    }

    suspend fun currentUserId(): String? = currentUserId.first()

    suspend fun appLanguage(): String? = appLanguage.first()

    private companion object {
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val GUEST_MODE_ENABLED = booleanPreferencesKey("guest_mode_enabled")
        val SIGNED_IN = booleanPreferencesKey("signed_in")
        val CURRENT_USER_ID = stringPreferencesKey("current_user_id")
        val APP_LANGUAGE = stringPreferencesKey("app_language")
        val LAST_WELCOME_NOTIFICATION_AT = longPreferencesKey("last_welcome_notification_at")
        val LAST_SIGNED_IN_AT = longPreferencesKey("last_signed_in_at")
        val EXERCISE_GIF_PRELOAD_COMPLETED = booleanPreferencesKey("exercise_gif_preload_completed")
    }
}
