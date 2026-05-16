package com.example.fitty.data.local.notification

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_notifications")
data class AppNotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val ownerId: String,
    val title: String,
    val message: String,
    val type: String,
    val createdAt: Long,
    val isRead: Boolean,
    val readAt: Long?
)
