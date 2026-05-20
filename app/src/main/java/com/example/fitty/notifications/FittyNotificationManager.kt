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
    const val CHANNEL_ID = "fitty_alerts_v2"
    private const val CHANNEL_NAME = "Fitty Alerts"
    private const val OLD_CHANNEL_ID = "fitty_welcome_back"
    private const val CHANNEL_DESCRIPTION = "Workout reminders, meal tracking and fitness notifications"
    private const val WELCOME_NOTIFICATION_ID = 1201

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        // Delete old low-priority channel
        notificationManager.deleteNotificationChannel(OLD_CHANNEL_ID)
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = CHANNEL_DESCRIPTION
            enableVibration(true)
            setShowBadge(true)
        }
        notificationManager.createNotificationChannel(channel)
    }

    fun canPostNotifications(context: Context): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    }

    fun showWelcomeBackNotification(
        context: Context,
        displayName: String,
        onForegroundMessage: ((title: String, message: String) -> Unit)? = null
    ) {
        val title = "Chao mung ${displayName.ifBlank { "ban" }} da quay lai"
        val message = welcomeMessageFor(displayName)
        if (isAppInForeground()) {
            onForegroundMessage?.invoke(title, message)
        }
        if (!canPostNotifications(context)) return
        createChannels(context)
        val pendingIntent = openAppPendingIntent(context, WELCOME_NOTIFICATION_ID)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    message
                )
            )
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        NotificationManagerCompat.from(context).notify(WELCOME_NOTIFICATION_ID, notification)
    }

    fun showRemoteNotification(
        context: Context,
        title: String,
        body: String,
        onForegroundMessage: ((title: String, message: String) -> Unit)? = null
    ) {
        if (isAppInForeground()) {
            onForegroundMessage?.invoke(title, body)
        }
        if (!canPostNotifications(context)) return
        createChannels(context)
        showBasicNotification(
            context = context,
            notificationId = (System.currentTimeMillis() % Int.MAX_VALUE).toInt(),
            title = title,
            body = body
        )
    }

    fun showTaskReminderNotification(
        context: Context,
        taskId: Long,
        title: String,
        body: String,
        onForegroundMessage: ((title: String, message: String) -> Unit)? = null
    ) {
        if (isAppInForeground()) {
            onForegroundMessage?.invoke(title, body)
        }
        if (!canPostNotifications(context)) return
        createChannels(context)
        showBasicNotification(
            context = context,
            notificationId = taskId.toInt(),
            title = title,
            body = body
        )
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

    private fun showBasicNotification(
        context: Context,
        notificationId: Int,
        title: String,
        body: String
    ) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(openAppPendingIntent(context, notificationId))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }

    private fun openAppPendingIntent(context: Context, requestCode: Int): PendingIntent {
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
