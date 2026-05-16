package com.example.fitty.data.local.notification

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AppNotificationDao {
    @Query(
        """
        SELECT * FROM app_notifications
        WHERE ownerId = :ownerId
        ORDER BY createdAt DESC, id DESC
        """
    )
    fun observeNotifications(ownerId: String): Flow<List<AppNotificationEntity>>

    @Query("SELECT COUNT(*) FROM app_notifications WHERE ownerId = :ownerId AND isRead = 0")
    fun observeUnreadCount(ownerId: String): Flow<Int>

    @Insert
    suspend fun insertNotification(notification: AppNotificationEntity): Long

    @Query(
        """
        UPDATE app_notifications
        SET isRead = 1, readAt = :readAt
        WHERE id = :notificationId
        """
    )
    suspend fun markAsRead(notificationId: Long, readAt: Long)

    @Query(
        """
        UPDATE app_notifications
        SET isRead = 1, readAt = :readAt
        WHERE ownerId = :ownerId AND isRead = 0
        """
    )
    suspend fun markAllAsRead(ownerId: String, readAt: Long)

    @Query("DELETE FROM app_notifications WHERE id = :notificationId")
    suspend fun deleteNotification(notificationId: Long)
}
