package com.example.fitty.domain.repository

import com.example.fitty.domain.model.FittyStartupState
import com.example.fitty.domain.model.FittyUser

interface SessionRepository {
    suspend fun saveStartupState(state: FittyStartupState)
    suspend fun saveUserSession(user: FittyUser)
    suspend fun setOnboardingCompleted(completed: Boolean)
    suspend fun getCurrentUserId(): String?
    suspend fun clearSession()
    suspend fun shouldShowWelcomeNotification(nowMillis: Long, cooldownMillis: Long): Boolean
    suspend fun setLastWelcomeNotificationAt(timestampMillis: Long)
}
