package com.example.fitty.domain.repository

interface NotificationTokenRepository {
    suspend fun syncNotificationToken(token: String)
}
