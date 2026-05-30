package com.example.fitty.domain.repository

import com.example.fitty.domain.model.FittyStartupState
import com.example.fitty.domain.model.FittyUser
import kotlinx.coroutines.flow.Flow

interface SessionRepository {
    suspend fun saveStartupState(state: FittyStartupState)
    suspend fun saveUserSession(user: FittyUser)
    suspend fun setOnboardingCompleted(completed: Boolean)
    suspend fun getCurrentUserId(): String?
    fun observeCurrentUserId(): Flow<String?>
    suspend fun getAppLanguage(): String?
    suspend fun setAppLanguage(language: String)
    suspend fun clearSession()
    suspend fun isSignedInSessionExpired(nowMillis: Long, maxAgeMillis: Long): Boolean
    suspend fun shouldShowWelcomeNotification(nowMillis: Long, cooldownMillis: Long): Boolean
    suspend fun setLastWelcomeNotificationAt(timestampMillis: Long)
}
