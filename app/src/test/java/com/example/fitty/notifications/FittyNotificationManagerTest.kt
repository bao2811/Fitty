package com.example.fitty.notifications

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowNotificationManager

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [28])
class FittyNotificationManagerTest {

    private lateinit var context: Context
    private lateinit var notificationManager: NotificationManager
    private lateinit var shadowNotificationManager: ShadowNotificationManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        shadowNotificationManager = Shadow.extract(notificationManager)
        shadowNotificationManager.setNotificationsEnabled(true)
    }

    @Test
    fun `show task reminder notification posts system notification on fitty channel`() {
        FittyNotificationManager.showTaskReminderNotification(
            context = context,
            taskId = 42L,
            title = "Workout session",
            body = "Use today's workout focus as your main session."
        )

        val channel = notificationManager.getNotificationChannel(FittyNotificationManager.CHANNEL_ID)
        assertNotNull(channel)
        assertEquals(NotificationManager.IMPORTANCE_HIGH, channel.importance)

        val notification = shadowNotificationManager.getNotification(42)
        assertNotNull(notification)
        assertEquals(FittyNotificationManager.CHANNEL_ID, notification.channelId)
        assertEquals(Notification.CATEGORY_REMINDER, notification.category)
        assertEquals(
            "Workout session",
            notification.extras.getString(Notification.EXTRA_TITLE)
        )
        assertEquals(
            "Use today's workout focus as your main session.",
            notification.extras.getCharSequence(Notification.EXTRA_TEXT).toString()
        )
    }
}
