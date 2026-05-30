package com.example.fitty.data.local.notification

import com.example.fitty.domain.model.AppNotification
import com.example.fitty.domain.model.AppNotificationType
import com.example.fitty.domain.model.FittyStartupState
import com.example.fitty.domain.model.FittyUser
import com.example.fitty.domain.repository.SessionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RoomAppNotificationRepositoryTest {

    @Test
    fun `observe notifications switches owner scope when session changes`() = runTest {
        val dao = FakeAppNotificationDao(
            initialNotifications = listOf(
                notification(id = 1, ownerId = "user-a", title = "A"),
                notification(id = 2, ownerId = "user-b", title = "B")
            )
        )
        val sessionRepository = SwitchingNotificationSessionRepository("user-a")
        val repository = RoomAppNotificationRepository(
            notificationDao = dao,
            sessionRepository = sessionRepository
        )

        val firstNotifications = repository.observeNotifications().first()
        sessionRepository.setCurrentUserId("user-b")
        val secondNotifications = repository.observeNotifications().first()

        assertEquals(listOf("A"), firstNotifications.map(AppNotification::title))
        assertEquals(listOf("B"), secondNotifications.map(AppNotification::title))
    }

    @Test
    fun `observe unread count switches owner scope when session changes`() = runTest {
        val dao = FakeAppNotificationDao(
            initialNotifications = listOf(
                notification(id = 1, ownerId = "user-a", title = "A", isRead = false),
                notification(id = 2, ownerId = "user-a", title = "A2", isRead = true),
                notification(id = 3, ownerId = "user-b", title = "B", isRead = false),
                notification(id = 4, ownerId = "user-b", title = "B2", isRead = false)
            )
        )
        val sessionRepository = SwitchingNotificationSessionRepository("user-a")
        val repository = RoomAppNotificationRepository(
            notificationDao = dao,
            sessionRepository = sessionRepository
        )

        val firstUnread = repository.observeUnreadCount().first()
        sessionRepository.setCurrentUserId("user-b")
        val secondUnread = repository.observeUnreadCount().first()

        assertEquals(1, firstUnread)
        assertEquals(2, secondUnread)
    }

    private fun notification(
        id: Long,
        ownerId: String,
        title: String,
        isRead: Boolean = false
    ): AppNotificationEntity {
        return AppNotificationEntity(
            id = id,
            ownerId = ownerId,
            title = title,
            message = "message-$title",
            type = AppNotificationType.General.name,
            createdAt = id,
            isRead = isRead,
            readAt = if (isRead) id else null
        )
    }
}

private class FakeAppNotificationDao(
    initialNotifications: List<AppNotificationEntity> = emptyList()
) : AppNotificationDao {
    private val notifications = MutableStateFlow(initialNotifications)

    override fun observeNotifications(ownerId: String): Flow<List<AppNotificationEntity>> {
        return notifications.map { current ->
            current.filter { it.ownerId == ownerId }
                .sortedWith(compareByDescending<AppNotificationEntity> { it.createdAt }.thenByDescending { it.id })
        }
    }

    override fun observeUnreadCount(ownerId: String): Flow<Int> {
        return notifications.map { current ->
            current.count { it.ownerId == ownerId && !it.isRead }
        }
    }

    override suspend fun insertNotification(notification: AppNotificationEntity): Long {
        notifications.value = notifications.value + notification
        return notification.id
    }

    override suspend fun markAsRead(notificationId: Long, readAt: Long) {
        notifications.value = notifications.value.map { current ->
            if (current.id == notificationId) current.copy(isRead = true, readAt = readAt) else current
        }
    }

    override suspend fun markAllAsRead(ownerId: String, readAt: Long) {
        notifications.value = notifications.value.map { current ->
            if (current.ownerId == ownerId && !current.isRead) current.copy(isRead = true, readAt = readAt) else current
        }
    }

    override suspend fun deleteNotification(notificationId: Long) {
        notifications.value = notifications.value.filterNot { it.id == notificationId }
    }
}

private class SwitchingNotificationSessionRepository(
    initialUserId: String?
) : SessionRepository {
    private val currentUserIdFlow = MutableStateFlow(initialUserId)

    fun setCurrentUserId(userId: String?) {
        currentUserIdFlow.value = userId
    }

    override suspend fun saveStartupState(state: FittyStartupState) = Unit
    override suspend fun saveUserSession(user: FittyUser) = Unit
    override suspend fun setOnboardingCompleted(completed: Boolean) = Unit
    override suspend fun getCurrentUserId(): String? = currentUserIdFlow.value
    override fun observeCurrentUserId(): Flow<String?> = currentUserIdFlow
    override suspend fun getAppLanguage(): String? = "en"
    override suspend fun setAppLanguage(language: String) = Unit
    override suspend fun clearSession() = Unit
    override suspend fun isSignedInSessionExpired(nowMillis: Long, maxAgeMillis: Long): Boolean = false
    override suspend fun shouldShowWelcomeNotification(nowMillis: Long, cooldownMillis: Long): Boolean = false
    override suspend fun setLastWelcomeNotificationAt(timestampMillis: Long) = Unit
}
