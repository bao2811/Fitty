package com.example.fitty.notifications

import com.example.fitty.domain.repository.NotificationTokenRepository
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class FittyFirebaseMessagingService : FirebaseMessagingService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    @Inject lateinit var notificationTokenRepository: NotificationTokenRepository
    @Inject lateinit var notificationDispatcher: FittyNotificationDispatcher

    override fun onNewToken(token: String) {
        serviceScope.launch {
            notificationTokenRepository.syncNotificationToken(token)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val notification = message.notification
        val title = notification?.title ?: message.data["title"] ?: "Fitty"
        val body = notification?.body ?: message.data["body"] ?: "Den luc quay lai va tap luyen roi."
        notificationDispatcher.showRemoteNotification(title = title, body = body)
    }
}
