package com.os11.app.platform.startup

import android.content.Context
import com.os11.app.BuildConfig
import com.os11.app.di.IoDispatcher
import com.os11.app.platform.shortcuts.AppShortcutManager
import com.os11.app.platform.widgets.WidgetUpdateManager
import com.os11.app.platform.work.DailyBackupWorker
import com.os11.app.platform.work.TrashPurgeWorker
import com.os11.app.service.pomodoro.PomodoroService
import com.os11.app.service.reminder.ReminderService
import com.os11.app.service.settings.SettingsService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@Suppress("TooGenericExceptionCaught", "LongParameterList")
class DeferredStartupManager @Inject constructor(
    private val reminderService: ReminderService,
    private val pomodoroService: PomodoroService,
    private val widgetUpdateManager: WidgetUpdateManager,
    private val appShortcutManager: AppShortcutManager,
    private val settingsService: SettingsService,
    @param:ApplicationContext private val context: Context,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    // Field-injected, not a constructor parameter, so the perf-only seeder does not widen the constructor that
    // tests build by hand. It is only touched when BuildConfig.PERF_SEED is true (the "perf" build type).
    @Inject
    lateinit var perfDataSeeder: PerfDataSeeder

    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)
    private val hasRun = AtomicBoolean(false)

    fun onFirstFrameRendered() {
        if (!hasRun.compareAndSet(false, true)) {
            return
        }

        scope.launch {
            try {
                // 0. Perf build only: fill the database with realistic volume for measuring
                if (BuildConfig.PERF_SEED) perfDataSeeder.seedIfNeeded()

                // 1. Reconcile exact reminder alarms with OS AlarmManager
                reminderService.reconcileAlarms()
                pomodoroService.reconcileTimerAfterReboot()

                // 2. Perform 30-day trash retention inspection
                performTrashPurgeCheck()

                // 3. Schedule automated daily backup check
                DailyBackupWorker.scheduleDailyBackup(context)

                // 4. Update dynamic launcher shortcuts
                performShortcutUpdate()

                // 5. Signal widget update if widgets exist
                performWidgetRefresh()

                // 6. Start local network sync listener if paired (Phase S1 stub)
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

    private suspend fun performShortcutUpdate() {
        val enabledModules = settingsService.observeEnabledModuleNames().first()
        appShortcutManager.updateShortcuts(enabledModules)
    }

    private fun performWidgetRefresh() {
        widgetUpdateManager.requestUpdate()
    }

    private fun startSyncEngineIfPaired() {
        // LAN sync transport startup hook
    }
}
