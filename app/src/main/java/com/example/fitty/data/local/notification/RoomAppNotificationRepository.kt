package com.example.fitty.data.local.notification

import com.example.fitty.domain.model.AppNotification
import com.example.fitty.domain.model.AppNotificationType
import com.example.fitty.domain.repository.AppNotificationRepository
import com.example.fitty.domain.repository.SessionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@OptIn(ExperimentalCoroutinesApi::class)
class RoomAppNotificationRepository @Inject constructor(
    private val notificationDao: AppNotificationDao,
    private val sessionRepository: SessionRepository
) : AppNotificationRepository {

    override fun observeNotifications(): Flow<List<AppNotification>> {
        return observeOwnerId()
            .flatMapLatest { ownerId -> notificationDao.observeNotifications(ownerId) }
            .map { notifications -> notifications.map { notification -> notification.toDomain() } }
    }

    override fun observeUnreadCount(): Flow<Int> =
        observeOwnerId().flatMapLatest { ownerId -> notificationDao.observeUnreadCount(ownerId) }

    override suspend fun addNotification(
        title: String,
        message: String,
        type: AppNotificationType
    ) {
        notificationDao.insertNotification(
            AppNotificationEntity(
                ownerId = currentOwnerId(),
                title = title,
                message = message,
                type = type.name,
                createdAt = System.currentTimeMillis(),
                isRead = false,
                readAt = null
            )
        )
    }

    override suspend fun markAsRead(notificationId: Long) {
        notificationDao.markAsRead(notificationId = notificationId, readAt = System.currentTimeMillis())
    }

    override suspend fun markAllAsRead() {
        notificationDao.markAllAsRead(ownerId = currentOwnerId(), readAt = System.currentTimeMillis())
    }

    override suspend fun deleteNotification(notificationId: Long) {
        notificationDao.deleteNotification(notificationId)
    }

    private fun AppNotificationEntity.toDomain(): AppNotification = AppNotification(
        id = id,
        title = title,
        message = message,
        type = type.toNotificationType(),
        createdAt = createdAt,
        isRead = isRead
    )

    private fun String.toNotificationType(): AppNotificationType = runCatching {
        enumValueOf<AppNotificationType>(this)
    }.getOrDefault(AppNotificationType.General)

    private suspend fun currentOwnerId(): String = sessionRepository.getCurrentUserId().orEmpty().ifBlank { GUEST_OWNER_ID }

    private fun observeOwnerId(): Flow<String> =
        sessionRepository.observeCurrentUserId()
            .map { ownerId -> ownerId.orEmpty().ifBlank { GUEST_OWNER_ID } }
            .distinctUntilChanged()

    private companion object {
        const val GUEST_OWNER_ID = "guest"
    }
}
