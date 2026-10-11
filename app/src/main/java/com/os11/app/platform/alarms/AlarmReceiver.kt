package com.os11.app.platform.alarms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.os11.app.data.repository.ReminderRepository
import com.os11.app.data.repository.TaskRepository
import com.os11.app.platform.notifications.NotificationPlatformDispatcher
import com.os11.app.service.reminder.ReminderService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Receiver invoked when a scheduled reminder alarm triggers.
 *
 * Dispatches a high-priority reminder notification with interactive actions
 * (Complete, Snooze 15m, Snooze 1h, Tomorrow morning), marks the reminder as
 * triggered in reminder_device_state, and re-arms the next due alarm.
 */
@AndroidEntryPoint
class AlarmReceiver : BroadcastReceiver() {

    @Inject
    lateinit var reminderRepository: ReminderRepository

    @Inject
    lateinit var taskRepository: TaskRepository

    @Inject
    lateinit var reminderService: ReminderService

    @Inject
    lateinit var notificationPlatformDispatcher: NotificationPlatformDispatcher

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == AlarmScheduler.ACTION_FIRE_REMINDER) {
            val reminderId = intent.getStringExtra(AlarmScheduler.EXTRA_REMINDER_ID) ?: return
            if (reminderId.isEmpty()) return

            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val reminder = reminderRepository.getById(reminderId) ?: return@launch
                    val task = taskRepository.getById(reminder.taskId) ?: return@launch

                    if (!task.isCompleted && !task.isTrashed) {
                        notificationPlatformDispatcher.postReminderNotification(
                            notificationId = reminderId.hashCode(),
                            taskId = task.id,
                            reminderId = reminder.id,
                            title = task.title,
                            content = "Reminder",
                        )
                    }
                    reminderRepository.markTriggered(reminderId)
                    reminderService.reconcileAlarms()
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
