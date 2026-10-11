package com.os11.app.platform.alarms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.os11.app.service.pomodoro.PomodoroService
import com.os11.app.service.reminder.ReminderService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Re-arms scheduled reminder alarms and Pomodoro timer following device reboot, package update,
 * device clock adjustment, or time-zone change.
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var reminderService: ReminderService

    @Inject
    lateinit var pomodoroService: PomodoroService

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val rearmActions = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
        )
        if (action in rearmActions) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    reminderService.reconcileAlarms()
                    pomodoroService.reconcileTimerAfterReboot()
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
