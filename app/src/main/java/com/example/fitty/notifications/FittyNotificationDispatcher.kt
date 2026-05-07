package com.example.fitty.notifications

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FittyNotificationDispatcher @Inject constructor(
    @ApplicationContext private val context: Context,
    private val bannerController: FittyBannerController
) {
    fun showWelcomeBackNotification(displayName: String) {
        FittyNotificationManager.showWelcomeBackNotification(
            context = context,
            displayName = displayName,
            onForegroundMessage = bannerController::show
        )
    }

    fun showRemoteNotification(title: String, body: String) {
        FittyNotificationManager.showRemoteNotification(
            context = context,
            title = title,
            body = body,
            onForegroundMessage = bannerController::show
        )
    }
}
