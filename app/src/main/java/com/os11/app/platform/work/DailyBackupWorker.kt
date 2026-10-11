package com.os11.app.platform.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.os11.app.service.backup.BackupService
import com.os11.app.service.settings.SettingsService
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit

/**
 * Daily background worker that executes automated full system backups
 * into a user-picked SAF folder per Phase 17 specifications.
 */
class DailyBackupWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface DailyBackupEntryPoint {
        fun backupService(): BackupService
        fun settingsService(): SettingsService
    }

    override suspend fun doWork(): Result {
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            DailyBackupEntryPoint::class.java,
        )
        val backupService = entryPoint.backupService()
        val settingsService = entryPoint.settingsService()

        val isEnabled = settingsService.getSetting("auto_backup_enabled")
            ?.replace("\"", "")?.trim()?.toBooleanStrictOrNull() ?: false
        val treeUri = settingsService.getSetting("auto_backup_uri")
            ?.replace("\"", "")?.trim()

        if (!isEnabled || treeUri.isNullOrBlank()) {
            return Result.success()
        }

        val result = backupService.performAutoBackupToTreeUri(applicationContext, treeUri)
        return if (result.isOk) {
            Result.success()
        } else {
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "os11_daily_backup"

        fun scheduleDailyBackup(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .build()

            val request = PeriodicWorkRequestBuilder<DailyBackupWorker>(1, TimeUnit.DAYS)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }
    }
}
