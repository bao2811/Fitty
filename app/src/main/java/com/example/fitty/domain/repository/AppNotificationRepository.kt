package com.example.fitty.domain.repository

import com.example.fitty.domain.model.AppNotification
import com.example.fitty.domain.model.AppNotificationType
import kotlinx.coroutines.flow.Flow

interface AppNotificationRepository {
    fun observeNotifications(): Flow<List<AppNotification>>
    fun observeUnreadCount(): Flow<Int>
    suspend fun addNotification(
        title: String,
        message: String,
        type: AppNotificationType = AppNotificationType.General
    )
    suspend fun markAsRead(notificationId: Long)
    suspend fun markAllAsRead()
    suspend fun deleteNotification(notificationId: Long)
}
