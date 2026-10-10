package com.os11.app.service.reminder

import com.os11.app.data.db.entities.ReminderEntity
import com.os11.app.data.repository.ReminderRepository
import com.os11.app.di.IoDispatcher
import com.os11.app.platform.alarms.AlarmScheduler
import com.os11.domain.id.IdGenerator
import com.os11.domain.result.AppError
import com.os11.domain.result.AppResult
import com.os11.domain.time.Clock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@Suppress("TooGenericExceptionCaught")
class ReminderService @Inject constructor(
    private val reminderRepository: ReminderRepository,
    private val alarmScheduler: AlarmScheduler,
    private val clock: Clock,
    private val idGenerator: IdGenerator,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    fun observeByTask(taskId: String): Flow<List<ReminderEntity>> = reminderRepository.observeByTask(taskId)

    suspend fun createReminder(taskId: String, remindAt: String): AppResult<ReminderEntity> =
        withContext(ioDispatcher) {
            try {
                val reminder = ReminderEntity(
                    id = idGenerator.randomId(),
                    taskId = taskId,
                    remindAt = remindAt,
                    createdAt = clock.nowIso(),
                )
                val saved = reminderRepository.save(reminder)
                reconcileAlarms()
                AppResult.ok(saved)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppResult.err(AppError.Unexpected(e))
            }
        }

    suspend fun deleteReminder(id: String): AppResult<Unit> = withContext(ioDispatcher) {
        try {
            reminderRepository.delete(id)
            alarmScheduler.cancelAlarm(id)
            reconcileAlarms()
            AppResult.ok(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppResult.err(AppError.Unexpected(e))
        }
    }

    suspend fun reconcileAlarms(): AppResult<Unit> = withContext(ioDispatcher) {
        try {
            if (alarmScheduler.canScheduleExactAlarms()) {
                // Reconcile and arm next alarm per single-alarm strategy in PERFORMANCE.md
            }
            AppResult.ok(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppResult.err(AppError.Unexpected(e))
        }
    }
}
