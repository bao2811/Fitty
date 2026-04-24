package com.example.fitty.notifications

import com.example.fitty.data.firebase.FittyFirebaseRepository
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class FittyFirebaseMessagingService : FirebaseMessagingService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        serviceScope.launch {
            FittyFirebaseRepository().syncNotificationToken(token)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val notification = message.notification
        val title = notification?.title ?: message.data["title"] ?: "Fitty"
        val body = notification?.body ?: message.data["body"] ?: "Den luc quay lai va tap luyen roi."
        FittyNotificationManager.showRemoteNotification(
            context = applicationContext,
            title = title,
            body = body
        )
    }
}
