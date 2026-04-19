package com.example.fitty.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
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

    val currentUserId: Flow<Long?> = context.fittyDataStore.data.map { preferences ->
        preferences[CURRENT_USER_ID]
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

    suspend fun setCurrentUserId(userId: Long?) {
        context.fittyDataStore.edit { preferences ->
            if (userId == null) {
                preferences.remove(CURRENT_USER_ID)
            } else {
                preferences[CURRENT_USER_ID] = userId
            }
        }
    }

    suspend fun clearSession() {
        context.fittyDataStore.edit { preferences ->
            preferences[GUEST_MODE_ENABLED] = false
            preferences[SIGNED_IN] = false
            preferences[ONBOARDING_COMPLETED] = false
            preferences.remove(CURRENT_USER_ID)
        }
    }

    private companion object {
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val GUEST_MODE_ENABLED = booleanPreferencesKey("guest_mode_enabled")
        val SIGNED_IN = booleanPreferencesKey("signed_in")
        val CURRENT_USER_ID = longPreferencesKey("current_user_id")
    }
}
