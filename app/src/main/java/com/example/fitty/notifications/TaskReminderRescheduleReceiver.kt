package com.example.fitty.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.fitty.data.local.task.HomeTaskDao
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class TaskReminderRescheduleReceiver : BroadcastReceiver() {
    @Inject lateinit var homeTaskDao: HomeTaskDao
    @Inject lateinit var reminderScheduler: TaskReminderScheduler

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action.orEmpty()
        if (
            action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_MY_PACKAGE_REPLACED &&
            action != Intent.ACTION_TIMEZONE_CHANGED
        ) {
            return
        }

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            runCatching {
                homeTaskDao.getActiveReminderTasks().forEach { task ->
                    reminderScheduler.schedule(
                        taskId = task.id,
                        title = task.title,
                        message = task.description,
                        dateKey = task.dateKey,
                        timeMinutes = task.timeMinutes
                    )
                }
            }
            pendingResult.finish()
        }
    }
}
