package com.os11.app.service.reminder

import com.os11.app.data.db.entities.ReminderEntity
import com.os11.app.data.repository.ReminderRepository
import com.os11.app.data.sync.TrackedWriter
import com.os11.app.di.IoDispatcher
import com.os11.app.platform.alarms.AlarmScheduler
import com.os11.domain.id.IdGenerator
import com.os11.domain.result.AppError
import com.os11.domain.result.AppResult
import com.os11.domain.time.Clock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@Suppress("TooGenericExceptionCaught", "TooManyFunctions")
class ReminderService @Inject constructor(
    private val reminderRepository: ReminderRepository,
    private val alarmScheduler: AlarmScheduler,
    private val trackedWriter: TrackedWriter? = null,
    private val clock: Clock,
    private val idGenerator: IdGenerator,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    private val serviceScope = CoroutineScope(SupervisorJob() + ioDispatcher)

    init {
        trackedWriter?.registerListener { entity, _, _ ->
            if (entity == "reminders" || entity == "tasks") {
                serviceScope.launch {
                    reconcileAlarms()
                }
            }
        }
    }

    fun observeByTask(taskId: String): Flow<List<ReminderEntity>> = reminderRepository.observeByTask(taskId)

    suspend fun getRemindersForTask(taskId: String): List<ReminderEntity> = withContext(ioDispatcher) {
        reminderRepository.getPendingReminders().filter { it.taskId == taskId }
    }

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

    suspend fun markReminderTriggered(reminderId: String): AppResult<Unit> = withContext(ioDispatcher) {
        try {
            reminderRepository.markTriggered(reminderId)
            reconcileAlarms()
            AppResult.ok(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppResult.err(AppError.Unexpected(e))
        }
    }

    suspend fun snoozeReminder(reminderId: String, snoozeMinutes: Long): AppResult<Unit> = withContext(ioDispatcher) {
        try {
            val now = Instant.now()
            val snoozedUntil = now.plus(snoozeMinutes, ChronoUnit.MINUTES).toString()
            reminderRepository.snoozeReminder(reminderId, snoozedUntil)
            reconcileAlarms()
            AppResult.ok(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppResult.err(AppError.Unexpected(e))
        }
    }

    @Suppress("MagicNumber")
    suspend fun snoozeUntilTomorrowMorning(reminderId: String): AppResult<Unit> = withContext(ioDispatcher) {
        try {
            val tomorrowMorning = LocalDate.now().plusDays(1).atTime(9, 0)
            val snoozedUntil = tomorrowMorning.atZone(ZoneId.systemDefault()).toInstant().toString()
            reminderRepository.snoozeReminder(reminderId, snoozedUntil)
            reconcileAlarms()
            AppResult.ok(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppResult.err(AppError.Unexpected(e))
        }
    }

    /**
     * Arms ONLY the next due reminder across all tasks via setExactAndAllowWhileIdle.
     * Evaluates effective trigger times against local reminder_device_state (snoozed_until).
     */
    suspend fun reconcileAlarms(): AppResult<Unit> = withContext(ioDispatcher) {
        try {
            val nowIso = clock.nowIso()
            val pending = reminderRepository.getPendingReminders(nowIso)

            var earliestReminder: ReminderEntity? = null
            var earliestTriggerEpoch = Long.MAX_VALUE

            for (reminder in pending) {
                val state = reminderRepository.getDeviceState(reminder.id)
                if (state?.isTriggered == true && state.snoozedUntil == null) {
                    continue
                }

                val effectiveTime = state?.snoozedUntil ?: reminder.remindAt
                val triggerEpoch = parseTimeToEpochMillis(effectiveTime)

                if (triggerEpoch in 1..<earliestTriggerEpoch) {
                    earliestTriggerEpoch = triggerEpoch
                    earliestReminder = reminder
                }
            }

            if (earliestReminder != null) {
                alarmScheduler.scheduleExactAlarm(earliestTriggerEpoch, earliestReminder.id)
            }

            AppResult.ok(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppResult.err(AppError.Unexpected(e))
        }
    }

    private fun parseTimeToEpochMillis(isoString: String): Long = try {
        Instant.parse(isoString).toEpochMilli()
    } catch (_: Exception) {
        try {
            LocalDateTime.parse(isoString, DateTimeFormatter.ISO_DATE_TIME)
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        } catch (_: Exception) {
            -1L
        }
    }
}
