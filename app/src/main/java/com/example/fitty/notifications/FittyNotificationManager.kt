package com.example.fitty.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.Lifecycle
import com.example.fitty.MainActivity
import com.example.fitty.R
import java.util.Locale

object FittyNotificationManager {
    const val WELCOME_CHANNEL_ID = "fitty_welcome_back"
    private const val WELCOME_CHANNEL_NAME = "Welcome Back"
    private const val DEFAULT_CHANNEL_DESCRIPTION = "Motivational fitness notifications"
    private const val WELCOME_NOTIFICATION_ID = 1201

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            WELCOME_CHANNEL_ID,
            WELCOME_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = DEFAULT_CHANNEL_DESCRIPTION
        }
        notificationManager.createNotificationChannel(channel)
    }

    fun canPostNotifications(context: Context): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    }

    fun showWelcomeBackNotification(
        context: Context,
        displayName: String
    ) {
        if (isAppInForeground()) {
            FittyInAppBannerManager.show(
                title = "Chao mung ${displayName.ifBlank { "ban" }} da quay lai",
                message = welcomeMessageFor(displayName)
            )
            return
        }
        if (!canPostNotifications(context)) return
        createChannels(context)

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, WELCOME_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle("Chao mung ${displayName.ifBlank { "ban" }} da quay lai")
            .setContentText(welcomeMessageFor(displayName))
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    welcomeMessageFor(displayName)
                )
            )
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(context).notify(WELCOME_NOTIFICATION_ID, notification)
    }

    fun showRemoteNotification(
        context: Context,
        title: String,
        body: String
    ) {
        if (isAppInForeground()) {
            FittyInAppBannerManager.show(title = title, message = body)
            return
        }
        if (!canPostNotifications(context)) return
        createChannels(context)
        val notification = NotificationCompat.Builder(context, WELCOME_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        NotificationManagerCompat.from(context).notify((System.currentTimeMillis() % Int.MAX_VALUE).toInt(), notification)
    }

    private fun welcomeMessageFor(displayName: String): String {
        val firstName = displayName
            .trim()
            .substringBefore(" ")
            .ifBlank { "ban" }

        val messages = listOf(
            "$firstName oi, san sang dot calo chua? Hom nay la ngay de pha ky luc moi.",
            "Chien thoi $firstName. Chi can bat dau buoi tap dau tien, dong luc se theo sau.",
            "$firstName da tro lai roi. Xo giay, hit sau, va bien hom nay thanh mot buoi tap dang nho.",
            "Fitty dang doi $firstName. Vao tap nhe, co the hom nay se la ngay ban vuot chinh minh."
        )
        val index = (System.currentTimeMillis() / 1000L % messages.size).toInt()
        return messages[index].replaceFirstChar { char ->
            if (char.isLowerCase()) char.titlecase(Locale.getDefault()) else char.toString()
        }
    }

    private fun isAppInForeground(): Boolean {
        return ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
    }
}
