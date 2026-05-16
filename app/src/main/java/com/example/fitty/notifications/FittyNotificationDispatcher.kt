package com.example.fitty.notifications

import android.content.Context
import com.example.fitty.domain.model.AppNotificationType
import com.example.fitty.domain.repository.AppNotificationRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FittyNotificationDispatcher @Inject constructor(
    @ApplicationContext private val context: Context,
    private val bannerController: FittyBannerController,
    private val appNotificationRepository: AppNotificationRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun showWelcomeBackNotification(displayName: String) {
        val title = "Chao mung ${displayName.ifBlank { "ban" }} da quay lai"
        val message = "Fitty san sang dong hanh cung ban trong buoi tap hom nay."
        persist(title = title, message = message, type = AppNotificationType.General)
        FittyNotificationManager.showWelcomeBackNotification(
            context = context,
            displayName = displayName,
            onForegroundMessage = bannerController::show
        )
    }

    fun showRemoteNotification(title: String, body: String) {
        persist(title = title, message = body, type = AppNotificationType.General)
        FittyNotificationManager.showRemoteNotification(
            context = context,
            title = title,
            body = body,
            onForegroundMessage = bannerController::show
        )
    }

    fun showTaskReminderNotification(taskId: Long, title: String, body: String) {
        FittyNotificationManager.showTaskReminderNotification(
            context = context,
            taskId = taskId,
            title = title,
            body = body,
            onForegroundMessage = bannerController::show
        )
    }

    private fun persist(title: String, message: String, type: AppNotificationType) {
        scope.launch {
            appNotificationRepository.addNotification(
                title = title,
                message = message,
                type = type
            )
        }
    }
}
