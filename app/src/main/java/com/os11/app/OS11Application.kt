package com.os11.app

import android.app.Application
import android.os.StrictMode
import dagger.hilt.android.HiltAndroidApp

/**
 * OS11 Application Entry Point.
 *
 * Startup Rule 16 (ARCHITECTURE.md):
 * Application.onCreate does Hilt initialization only. Heavy systems
 * (sync, alarms, widgets, backup, trash purge) start lazily or after the first frame.
 * Debug builds configure StrictMode (detectAll + penaltyLog).
 */
@HiltAndroidApp
class OS11Application : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build(),
            )
            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build(),
            )
        }
    }
}
