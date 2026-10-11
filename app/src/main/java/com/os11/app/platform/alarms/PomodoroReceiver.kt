package com.os11.app.platform.alarms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.os11.app.service.pomodoro.PomodoroService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Broadcast receiver handling Pomodoro exact alarm completions and
 * interactive notification actions (Pause, Resume, Stop, Start Next).
 */
@AndroidEntryPoint
class PomodoroReceiver : BroadcastReceiver() {

    @Inject
    lateinit var pomodoroService: PomodoroService

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (action) {
                    AlarmScheduler.ACTION_FIRE_POMODORO -> {
                        pomodoroService.completeSession()
                    }
                    ACTION_PAUSE -> {
                        pomodoroService.pauseSession()
                    }
                    ACTION_RESUME -> {
                        pomodoroService.resumeSession()
                    }
                    ACTION_STOP -> {
                        pomodoroService.resetSession()
                    }
                    ACTION_START_NEXT -> {
                        pomodoroService.startNextPhase()
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_PAUSE = "com.os11.app.platform.alarms.POMODORO_PAUSE"
        const val ACTION_RESUME = "com.os11.app.platform.alarms.POMODORO_RESUME"
        const val ACTION_STOP = "com.os11.app.platform.alarms.POMODORO_STOP"
        const val ACTION_START_NEXT = "com.os11.app.platform.alarms.POMODORO_START_NEXT"
    }
}
