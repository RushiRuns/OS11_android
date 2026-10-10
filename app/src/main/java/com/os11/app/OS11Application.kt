package com.os11.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * OS11 Application Entry Point.
 *
 * Startup Rule 16 (ARCHITECTURE.md):
 * Application.onCreate does Hilt initialization only. Heavy systems
 * (sync, alarms, widgets, backup, trash purge) start lazily or after the first frame.
 */
@HiltAndroidApp
class OS11Application : Application()
