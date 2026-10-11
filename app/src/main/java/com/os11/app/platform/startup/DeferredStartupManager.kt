package com.os11.app.platform.startup

import android.content.Context
import com.os11.app.di.IoDispatcher
import com.os11.app.platform.work.TrashPurgeWorker
import com.os11.app.service.pomodoro.PomodoroService
import com.os11.app.service.reminder.ReminderService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@Suppress("TooGenericExceptionCaught")
class DeferredStartupManager @Inject constructor(
    private val reminderService: ReminderService,
    private val pomodoroService: PomodoroService,
    @param:ApplicationContext private val context: Context,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)
    private val hasRun = AtomicBoolean(false)

    fun onFirstFrameRendered() {
        if (!hasRun.compareAndSet(false, true)) {
            return
        }

        scope.launch {
            try {
                // 1. Reconcile exact reminder alarms with OS AlarmManager
                reminderService.reconcileAlarms()
                pomodoroService?.reconcileTimerAfterReboot()

                // 2. Perform 30-day trash retention inspection
                performTrashPurgeCheck()

                // 3. Signal widget update if widgets exist
                performWidgetRefresh()

                // 4. Start local network sync listener if paired (Phase S1 stub)
                startSyncEngineIfPaired()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Post-startup deferred work logs silently without crashing the UI
                android.util.Log.w("DeferredStartup", "Deferred initialization failed: ${e.message}")
            }
        }
    }

    private fun performTrashPurgeCheck() {
        TrashPurgeWorker.scheduleDailyPurge(context)
    }

    private fun performWidgetRefresh() {
        // Jetpack Glance widget state refresh hook
    }

    private fun startSyncEngineIfPaired() {
        // LAN sync transport startup hook
    }
}
