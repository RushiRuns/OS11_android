package com.os11.app.platform.alarms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.os11.app.service.reminder.ReminderService
import com.os11.app.service.task.TaskService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Receiver handling interactive notification actions:
 * - Complete
 * - Snooze 15 minutes
 * - Snooze 1 hour
 * - Snooze until tomorrow morning
 */
@AndroidEntryPoint
class ReminderActionReceiver : BroadcastReceiver() {

    @Inject
    lateinit var taskService: TaskService

    @Inject
    lateinit var reminderService: ReminderService

    @Suppress("ReturnCount")
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val taskId = intent.getStringExtra(EXTRA_TASK_ID) ?: return
        val reminderId = intent.getStringExtra(EXTRA_REMINDER_ID) ?: return
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)

        if (notificationId != -1) {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.cancel(notificationId)
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (action) {
                    ACTION_COMPLETE -> {
                        taskService.setCompleted(taskId, completed = true)
                        reminderService.markReminderTriggered(reminderId)
                    }
                    ACTION_SNOOZE_15M -> {
                        reminderService.snoozeReminder(reminderId, SNOOZE_15M_MINUTES)
                    }
                    ACTION_SNOOZE_1H -> {
                        reminderService.snoozeReminder(reminderId, SNOOZE_1H_MINUTES)
                    }
                    ACTION_SNOOZE_TOMORROW -> {
                        reminderService.snoozeUntilTomorrowMorning(reminderId)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_COMPLETE = "com.os11.app.platform.alarms.ACTION_COMPLETE"
        const val ACTION_SNOOZE_15M = "com.os11.app.platform.alarms.ACTION_SNOOZE_15M"
        const val ACTION_SNOOZE_1H = "com.os11.app.platform.alarms.ACTION_SNOOZE_1H"
        const val ACTION_SNOOZE_TOMORROW = "com.os11.app.platform.alarms.ACTION_SNOOZE_TOMORROW"

        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_REMINDER_ID = "extra_reminder_id"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"

        const val SNOOZE_15M_MINUTES = 15L
        const val SNOOZE_1H_MINUTES = 60L
    }
}
