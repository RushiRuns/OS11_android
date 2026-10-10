package com.os11.app.platform.alarms

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Spike S-8: Exact alarm scheduling and reboot re-arming.
 *
 * Implements:
 * 1. Single next-alarm scheduling using AlarmManager.setExactAndAllowWhileIdle.
 * 2. Pre-check for exact alarm permissions on Android 12+ (SCHEDULE_EXACT_ALARM).
 * 3. Reboot re-arm triggering via BootReceiver.
 */
@Singleton
class AlarmScheduler @Inject constructor(@param:ApplicationContext private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

    /**
     * Checks if exact alarms can be scheduled (handles Android 12+ canScheduleExactAlarms).
     */
    fun canScheduleExactAlarms(): Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        alarmManager?.canScheduleExactAlarms() ?: false
    } else {
        true
    }

    /**
     * Schedules an exact alarm that fires even in Doze mode (setExactAndAllowWhileIdle).
     */
    fun scheduleExactAlarm(triggerAtMillis: Long, reminderId: String) {
        if (alarmManager == null) return
        if (!canScheduleExactAlarms()) {
            return
        }

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_FIRE_REMINDER
            putExtra(EXTRA_REMINDER_ID, reminderId)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            pendingIntent,
        )
    }

    /**
     * Cancels a previously scheduled exact alarm.
     */
    fun cancelAlarm(reminderId: String) {
        if (alarmManager == null) return
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_FIRE_REMINDER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId.hashCode(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    companion object {
        const val ACTION_FIRE_REMINDER = "com.os11.app.platform.alarms.FIRE_REMINDER"
        const val EXTRA_REMINDER_ID = "extra_reminder_id"
    }
}
