package com.example.fitty.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.fitty.domain.model.AppNotificationType
import com.example.fitty.domain.repository.AppNotificationRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class TaskReminderReceiver : BroadcastReceiver() {
    @Inject lateinit var appNotificationRepository: AppNotificationRepository
    @Inject lateinit var notificationDispatcher: FittyNotificationDispatcher

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(EXTRA_TASK_ID, 0L)
        val title = intent.getStringExtra(EXTRA_TASK_TITLE).orEmpty().ifBlank { "Task reminder" }
        val message = intent.getStringExtra(EXTRA_TASK_MESSAGE).orEmpty().ifBlank { "You planned this task for now." }
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            runCatching {
                appNotificationRepository.addNotification(
                    title = title,
                    message = message,
                    type = AppNotificationType.Reminder
                )
                notificationDispatcher.showTaskReminderNotification(
                    taskId = taskId,
                    title = title,
                    body = message
                )
            }
            pendingResult.finish()
        }
    }

    companion object {
        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_TASK_TITLE = "extra_task_title"
        const val EXTRA_TASK_MESSAGE = "extra_task_message"
    }
}
