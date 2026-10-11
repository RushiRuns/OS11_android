package com.os11.app.platform.widgets

import com.os11.app.data.repository.TaskRepository
import com.os11.app.platform.biometrics.AppLockManager
import com.os11.app.service.settings.SettingsService
import com.os11.app.service.task.TaskService
import com.os11.domain.time.Clock
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Hilt EntryPoint for accessing dependencies from Glance AppWidgets and ActionCallbacks.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun taskService(): TaskService
    fun taskRepository(): TaskRepository
    fun settingsService(): SettingsService
    fun appLockManager(): AppLockManager
    fun clock(): Clock
}
