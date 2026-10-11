package com.os11.app.platform.notifications

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.os11.app.MainActivity
import com.os11.app.platform.alarms.PomodoroReceiver
import com.os11.app.platform.alarms.ReminderActionReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

interface NotificationPlatformDispatcher {
    fun postNotification(
        channelId: String,
        notificationId: Int,
        title: String,
        content: String,
        taskId: String? = null,
    )

    fun postReminderNotification(
        notificationId: Int,
        taskId: String,
        reminderId: String,
        title: String,
        content: String,
    )

    fun postPomodoroOngoingNotification(title: String, content: String, endAlarmTimeMillis: Long, isPaused: Boolean)

    fun postPomodoroCompleteNotification(title: String, content: String, nextPhase: String)

    fun cancelPomodoroNotification()

    fun cancelNotification(notificationId: Int)
}

@Singleton
class SystemNotificationPlatformDispatcher @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : NotificationPlatformDispatcher {

    init {
        OS11NotificationChannels.createChannels(context)
    }

    override fun postNotification(
        channelId: String,
        notificationId: Int,
        title: String,
        content: String,
        taskId: String?,
    ) {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) {
            return
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val permissionGranted = ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            if (!permissionGranted) {
                return
            }
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(content)
            .setAutoCancel(true)
            .setPriority(
                if (channelId == OS11NotificationChannels.CHANNEL_REMINDERS) {
                    NotificationCompat.PRIORITY_HIGH
                } else {
                    NotificationCompat.PRIORITY_DEFAULT
                },
            )

        manager.notify(notificationId, builder.build())
    }

    @Suppress("MagicNumber", "LongMethod")
    override fun postReminderNotification(
        notificationId: Int,
        taskId: String,
        reminderId: String,
        title: String,
        content: String,
    ) {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) {
            return
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val permissionGranted = ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            if (!permissionGranted) {
                return
            }
        }

        val completeIntent = Intent(context, ReminderActionReceiver::class.java).apply {
            action = ReminderActionReceiver.ACTION_COMPLETE
            putExtra(ReminderActionReceiver.EXTRA_TASK_ID, taskId)
            putExtra(ReminderActionReceiver.EXTRA_REMINDER_ID, reminderId)
            putExtra(ReminderActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
        }
        val completePendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId * 10 + 1,
            completeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val snooze15Intent = Intent(context, ReminderActionReceiver::class.java).apply {
            action = ReminderActionReceiver.ACTION_SNOOZE_15M
            putExtra(ReminderActionReceiver.EXTRA_TASK_ID, taskId)
            putExtra(ReminderActionReceiver.EXTRA_REMINDER_ID, reminderId)
            putExtra(ReminderActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
        }
        val snooze15PendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId * 10 + 2,
            snooze15Intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val snooze1hIntent = Intent(context, ReminderActionReceiver::class.java).apply {
            action = ReminderActionReceiver.ACTION_SNOOZE_1H
            putExtra(ReminderActionReceiver.EXTRA_TASK_ID, taskId)
            putExtra(ReminderActionReceiver.EXTRA_REMINDER_ID, reminderId)
            putExtra(ReminderActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
        }
        val snooze1hPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId * 10 + 3,
            snooze1hIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val snoozeTomorrowIntent = Intent(context, ReminderActionReceiver::class.java).apply {
            action = ReminderActionReceiver.ACTION_SNOOZE_TOMORROW
            putExtra(ReminderActionReceiver.EXTRA_TASK_ID, taskId)
            putExtra(ReminderActionReceiver.EXTRA_REMINDER_ID, reminderId)
            putExtra(ReminderActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
        }
        val snoozeTomorrowPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId * 10 + 4,
            snoozeTomorrowIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val builder = NotificationCompat.Builder(context, OS11NotificationChannels.CHANNEL_REMINDERS)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(content)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .addAction(0, "Complete", completePendingIntent)
            .addAction(0, "Snooze 15m", snooze15PendingIntent)
            .addAction(0, "Snooze 1h", snooze1hPendingIntent)
            .addAction(0, "Tomorrow", snoozeTomorrowPendingIntent)

        manager.notify(notificationId, builder.build())
    }

    override fun cancelNotification(notificationId: Int) {
        val manager = NotificationManagerCompat.from(context)
        manager.cancel(notificationId)
    }

    override fun postPomodoroOngoingNotification(
        title: String,
        content: String,
        endAlarmTimeMillis: Long,
        isPaused: Boolean,
    ) {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val permissionGranted = ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            if (!permissionGranted) return
        }

        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("nav_target", "pomodoro")
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            POMODORO_NOTIFICATION_ID,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val pauseResumeIntent = Intent(context, PomodoroReceiver::class.java).apply {
            action = if (isPaused) PomodoroReceiver.ACTION_RESUME else PomodoroReceiver.ACTION_PAUSE
        }
        val pauseResumePendingIntent = PendingIntent.getBroadcast(
            context,
            POMODORO_NOTIFICATION_ID + 1,
            pauseResumeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val stopIntent = Intent(context, PomodoroReceiver::class.java).apply {
            action = PomodoroReceiver.ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getBroadcast(
            context,
            POMODORO_NOTIFICATION_ID + 2,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val builder = NotificationCompat.Builder(context, OS11NotificationChannels.CHANNEL_POMODORO)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(if (isPaused) "Paused • $content" else content)
            .setContentIntent(contentPendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        if (!isPaused && endAlarmTimeMillis > 0) {
            builder.setUsesChronometer(true)
            builder.setChronometerCountDown(true)
            builder.setWhen(endAlarmTimeMillis)
            builder.setShowWhen(true)
        }

        builder.addAction(0, if (isPaused) "Resume" else "Pause", pauseResumePendingIntent)
        builder.addAction(0, "Stop", stopPendingIntent)

        manager.notify(POMODORO_NOTIFICATION_ID, builder.build())
    }

    override fun postPomodoroCompleteNotification(title: String, content: String, nextPhase: String) {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val permissionGranted = ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            if (!permissionGranted) return
        }

        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("nav_target", "pomodoro")
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            POMODORO_COMPLETE_NOTIFICATION_ID,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val startNextIntent = Intent(context, PomodoroReceiver::class.java).apply {
            action = PomodoroReceiver.ACTION_START_NEXT
        }
        val startNextPendingIntent = PendingIntent.getBroadcast(
            context,
            POMODORO_COMPLETE_NOTIFICATION_ID + 1,
            startNextIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val builder = NotificationCompat.Builder(context, OS11NotificationChannels.CHANNEL_POMODORO)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(content)
            .setContentIntent(contentPendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .addAction(0, if (nextPhase == "work") "Start Focus" else "Start Break", startNextPendingIntent)

        manager.notify(POMODORO_COMPLETE_NOTIFICATION_ID, builder.build())
    }

    override fun cancelPomodoroNotification() {
        val manager = NotificationManagerCompat.from(context)
        manager.cancel(POMODORO_NOTIFICATION_ID)
    }

    companion object {
        const val POMODORO_NOTIFICATION_ID = 11001
        const val POMODORO_COMPLETE_NOTIFICATION_ID = 11002
    }
}
