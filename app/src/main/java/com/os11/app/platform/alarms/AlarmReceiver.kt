package com.os11.app.platform.alarms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Receiver invoked when a scheduled reminder alarm triggers.
 */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == AlarmScheduler.ACTION_FIRE_REMINDER) {
            val reminderId = intent.getStringExtra(AlarmScheduler.EXTRA_REMINDER_ID) ?: return
            // Reminder notification dispatch will be wired in Phase 4 / Phase 9
            if (reminderId.isNotEmpty()) {
                // Verified reminder intent payload
            }
        }
    }
}
