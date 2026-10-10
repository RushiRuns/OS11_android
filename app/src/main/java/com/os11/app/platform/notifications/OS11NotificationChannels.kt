package com.os11.app.platform.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

/**
 * Definition of system notification channels for OS11 Android.
 *
 * Requirements per Phase 9 and ARCHITECTURE.md:
 * - reminders: high importance for exact alarms and due dates
 * - pomodoro: default/low importance for focus sessions
 * - follow-ups: default importance for waiting-on follow-up alerts
 * - system: low importance for backup, sync status, and internal alerts
 */
object OS11NotificationChannels {
    const val CHANNEL_REMINDERS = "os11_reminders"
    const val CHANNEL_POMODORO = "os11_pomodoro"
    const val CHANNEL_FOLLOW_UPS = "os11_follow_ups"
    const val CHANNEL_SYSTEM = "os11_system"

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return

            val remindersChannel = NotificationChannel(
                CHANNEL_REMINDERS,
                "Reminders",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Task reminders and due date alerts"
                enableVibration(true)
            }

            val pomodoroChannel = NotificationChannel(
                CHANNEL_POMODORO,
                "Pomodoro Timer",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "Focus timer phase updates and completions"
            }

            val followUpsChannel = NotificationChannel(
                CHANNEL_FOLLOW_UPS,
                "Follow-ups",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "Delegated and waiting-for follow-up reminders"
            }

            val systemChannel = NotificationChannel(
                CHANNEL_SYSTEM,
                "System",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Backups, sync engine status, and maintenance notices"
            }

            notificationManager.createNotificationChannels(
                listOf(remindersChannel, pomodoroChannel, followUpsChannel, systemChannel),
            )
        }
    }
}
