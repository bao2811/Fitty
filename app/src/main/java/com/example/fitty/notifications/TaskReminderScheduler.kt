package com.example.fitty.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun schedule(
        taskId: Long,
        title: String,
        message: String,
        dateKey: String,
        timeMinutes: Int
    ) {
        val triggerAt = scheduledTimeMillis(dateKey = dateKey, timeMinutes = timeMinutes)
        if (triggerAt <= System.currentTimeMillis()) {
            cancel(taskId)
            return
        }
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAt,
            pendingIntent(taskId = taskId, title = title, message = message)
        )
    }

    fun cancel(taskId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent(taskId = taskId, title = "", message = ""))
    }

    private fun pendingIntent(taskId: Long, title: String, message: String): PendingIntent {
        val intent = Intent(context, TaskReminderReceiver::class.java).apply {
            putExtra(TaskReminderReceiver.EXTRA_TASK_ID, taskId)
            putExtra(TaskReminderReceiver.EXTRA_TASK_TITLE, title)
            putExtra(TaskReminderReceiver.EXTRA_TASK_MESSAGE, message)
        }
        return PendingIntent.getBroadcast(
            context,
            taskId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun scheduledTimeMillis(dateKey: String, timeMinutes: Int): Long {
        val date = LocalDate.parse(dateKey)
        val hour = timeMinutes / 60
        val minute = timeMinutes % 60
        return LocalDateTime.of(date.year, date.month, date.dayOfMonth, hour, minute)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }
}
