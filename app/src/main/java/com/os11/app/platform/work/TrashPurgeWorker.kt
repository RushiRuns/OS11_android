package com.os11.app.platform.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.os11.app.service.task.TaskService
import com.os11.domain.time.Clock
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.time.Instant
import java.util.concurrent.TimeUnit

/**
 * Daily background worker that purges tasks residing in the trash for longer
 * than 30 days per Phase 5 specifications.
 */
class TrashPurgeWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface TrashPurgeEntryPoint {
        fun taskService(): TaskService
        fun clock(): Clock
    }

    override suspend fun doWork(): Result {
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            TrashPurgeEntryPoint::class.java,
        )
        val taskService = entryPoint.taskService()
        val clock = entryPoint.clock()

        val cutoffIso = Instant.ofEpochMilli(
            clock.wallMs() - TimeUnit.DAYS.toMillis(PURGE_RETENTION_DAYS),
        ).toString()

        val result = taskService.purgeTrashOlderThan(cutoffIso)
        return if (result.isOk) {
            Result.success()
        } else {
            Result.retry()
        }
    }

    companion object {
        private const val PURGE_RETENTION_DAYS = 30L
        const val WORK_NAME = "os11_daily_trash_purge"

        fun scheduleDailyPurge(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .build()

            val request = PeriodicWorkRequestBuilder<TrashPurgeWorker>(1, TimeUnit.DAYS)
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
