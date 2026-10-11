package com.os11.app.service.task

import com.os11.app.data.db.entities.TaskEntity
import com.os11.app.data.repository.TagRepository
import com.os11.app.data.repository.TaskRepository
import com.os11.app.di.IoDispatcher
import com.os11.app.service.reminder.ReminderService
import com.os11.app.service.undo.UndoManager
import com.os11.app.service.undo.UndoToken
import com.os11.domain.logic.recurrence.RecurrenceEngine
import com.os11.domain.logic.scheduling.BucketType
import com.os11.domain.logic.scheduling.RecurrenceBasis
import com.os11.domain.logic.scheduling.ScheduleState
import com.os11.domain.logic.scheduling.SchedulingAction
import com.os11.domain.logic.scheduling.SchedulingTransitions
import com.os11.domain.result.AppError
import com.os11.domain.result.AppResult
import com.os11.domain.time.Clock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Architecture Rule 15: The sole writer of guarded scheduling fields:
 * - bucket
 * - waiting_on, waiting_since, follow_up_date, follow_up_notified_on
 * - reviewed_at
 * - due_date, due_time, all_day
 * - recurrence_rule, recurrence_basis
 */
@Singleton
@Suppress("TooGenericExceptionCaught", "TooManyFunctions")
class TaskSchedulingService @Inject constructor(
    private val taskRepository: TaskRepository,
    private val tagRepository: TagRepository,
    private val reminderService: ReminderService,
    private val undoManager: UndoManager,
    private val clock: Clock,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {

    suspend fun scheduleDate(
        taskId: String,
        dueDate: String,
        dueTime: String? = null,
        allDay: Boolean = dueTime == null,
        recurrenceRule: String? = null,
        recurrenceBasis: String? = RecurrenceBasis.FIXED.value,
    ): AppResult<UndoToken> = withContext(ioDispatcher) {
        try {
            val existing = taskRepository.getById(taskId)
                ?: return@withContext AppResult.err(AppError.NotFound)

            val previousState = extractScheduleState(existing)
            val action = SchedulingAction.ScheduleDueDate(
                dueDate = dueDate,
                dueTime = dueTime,
                allDay = allDay,
                recurrenceRule = recurrenceRule,
                recurrenceBasis = recurrenceBasis,
            )

            val transition = SchedulingTransitions.transition(
                current = previousState,
                action = action,
                referenceDate = clock.today(),
                referenceTimestamp = clock.nowIso(),
            )

            if (!transition.isSuccess) {
                return@withContext AppResult.err(
                    AppError.Validation("dueDate", transition.errors.first().message),
                )
            }

            val updated = applyScheduleState(existing, transition.state)
            taskRepository.saveTask(updated)
            reminderService.reconcileAlarms()

            val token = undoManager.registerUndo(description = "Date scheduled") {
                val current = taskRepository.getById(taskId)
                    ?: return@registerUndo AppResult.err(AppError.NotFound)
                taskRepository.saveTask(applyScheduleState(current, previousState))
                reminderService.reconcileAlarms()
                AppResult.ok(Unit)
            }
            AppResult.ok(token)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppResult.err(AppError.Unexpected(e))
        }
    }

    suspend fun moveToBucket(taskId: String, bucket: String): AppResult<UndoToken> = withContext(ioDispatcher) {
        try {
            val bucketType = BucketType.fromValue(bucket)
                ?: return@withContext AppResult.err(
                    AppError.Validation("bucket", "Bucket must be 'anytime' or 'someday'"),
                )

            val existing = taskRepository.getById(taskId)
                ?: return@withContext AppResult.err(AppError.NotFound)

            val previousState = extractScheduleState(existing)
            val transition = SchedulingTransitions.transition(
                current = previousState,
                action = SchedulingAction.MoveToBucket(bucketType),
                referenceDate = clock.today(),
                referenceTimestamp = clock.nowIso(),
            )

            if (!transition.isSuccess) {
                return@withContext AppResult.err(
                    AppError.Validation("bucket", transition.errors.first().message),
                )
            }

            val updated = applyScheduleState(existing, transition.state)
            taskRepository.saveTask(updated)
            reminderService.reconcileAlarms()

            val token = undoManager.registerUndo(description = "Moved to ${bucketType.value}") {
                val current = taskRepository.getById(taskId)
                    ?: return@registerUndo AppResult.err(AppError.NotFound)
                taskRepository.saveTask(applyScheduleState(current, previousState))
                reminderService.reconcileAlarms()
                AppResult.ok(Unit)
            }
            AppResult.ok(token)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppResult.err(AppError.Unexpected(e))
        }
    }

    suspend fun clearSchedule(taskId: String): AppResult<UndoToken> = withContext(ioDispatcher) {
        try {
            val existing = taskRepository.getById(taskId)
                ?: return@withContext AppResult.err(AppError.NotFound)

            val previousState = extractScheduleState(existing)
            val transition = SchedulingTransitions.transition(
                current = previousState,
                action = SchedulingAction.ClearTemporalSchedule,
                referenceDate = clock.today(),
                referenceTimestamp = clock.nowIso(),
            )

            val updated = applyScheduleState(existing, transition.state)
            taskRepository.saveTask(updated)
            reminderService.reconcileAlarms()

            val token = undoManager.registerUndo(description = "Schedule cleared") {
                val current = taskRepository.getById(taskId)
                    ?: return@registerUndo AppResult.err(AppError.NotFound)
                taskRepository.saveTask(applyScheduleState(current, previousState))
                reminderService.reconcileAlarms()
                AppResult.ok(Unit)
            }
            AppResult.ok(token)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppResult.err(AppError.Unexpected(e))
        }
    }

    suspend fun setWaiting(taskId: String, waitingOn: String, followUpDate: String? = null): AppResult<UndoToken> =
        withContext(ioDispatcher) {
            try {
                val existing = taskRepository.getById(taskId)
                    ?: return@withContext AppResult.err(AppError.NotFound)

                val previousState = extractScheduleState(existing)
                val transition = SchedulingTransitions.transition(
                    current = previousState,
                    action = SchedulingAction.SetWaiting(waitingOn, followUpDate),
                    referenceDate = clock.today(),
                    referenceTimestamp = clock.nowIso(),
                )

                if (!transition.isSuccess) {
                    return@withContext AppResult.err(
                        AppError.Validation("waitingOn", transition.errors.first().message),
                    )
                }

                val updated = applyScheduleState(existing, transition.state)
                taskRepository.saveTask(updated)

                val token = undoManager.registerUndo(description = "Waiting for $waitingOn") {
                    val current = taskRepository.getById(taskId)
                        ?: return@registerUndo AppResult.err(AppError.NotFound)
                    taskRepository.saveTask(applyScheduleState(current, previousState))
                    AppResult.ok(Unit)
                }
                AppResult.ok(token)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppResult.err(AppError.Unexpected(e))
            }
        }

    suspend fun clearWaiting(taskId: String): AppResult<UndoToken> = withContext(ioDispatcher) {
        try {
            val existing = taskRepository.getById(taskId)
                ?: return@withContext AppResult.err(AppError.NotFound)

            val previousState = extractScheduleState(existing)
            val transition = SchedulingTransitions.transition(
                current = previousState,
                action = SchedulingAction.ClearWaiting,
                referenceDate = clock.today(),
                referenceTimestamp = clock.nowIso(),
            )

            val updated = applyScheduleState(existing, transition.state)
            taskRepository.saveTask(updated)

            val token = undoManager.registerUndo(description = "Cleared waiting") {
                val current = taskRepository.getById(taskId)
                    ?: return@registerUndo AppResult.err(AppError.NotFound)
                taskRepository.saveTask(applyScheduleState(current, previousState))
                AppResult.ok(Unit)
            }
            AppResult.ok(token)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppResult.err(AppError.Unexpected(e))
        }
    }

    suspend fun setRecurrence(
        taskId: String,
        recurrenceRule: String,
        recurrenceBasis: String = RecurrenceBasis.FIXED.value,
    ): AppResult<UndoToken> = withContext(ioDispatcher) {
        try {
            val existing = taskRepository.getById(taskId)
                ?: return@withContext AppResult.err(AppError.NotFound)

            val previousState = extractScheduleState(existing)
            val transition = SchedulingTransitions.transition(
                current = previousState,
                action = SchedulingAction.SetRecurrence(recurrenceRule, recurrenceBasis),
                referenceDate = clock.today(),
                referenceTimestamp = clock.nowIso(),
            )

            if (!transition.isSuccess) {
                return@withContext AppResult.err(
                    AppError.Validation("recurrenceRule", transition.errors.first().message),
                )
            }

            val updated = applyScheduleState(existing, transition.state)
            taskRepository.saveTask(updated)

            val token = undoManager.registerUndo(description = "Recurrence updated") {
                val current = taskRepository.getById(taskId)
                    ?: return@registerUndo AppResult.err(AppError.NotFound)
                taskRepository.saveTask(applyScheduleState(current, previousState))
                AppResult.ok(Unit)
            }
            AppResult.ok(token)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppResult.err(AppError.Unexpected(e))
        }
    }

    suspend fun clearRecurrence(taskId: String): AppResult<UndoToken> = withContext(ioDispatcher) {
        try {
            val existing = taskRepository.getById(taskId)
                ?: return@withContext AppResult.err(AppError.NotFound)

            val previousState = extractScheduleState(existing)
            val transition = SchedulingTransitions.transition(
                current = previousState,
                action = SchedulingAction.ClearRecurrence,
                referenceDate = clock.today(),
                referenceTimestamp = clock.nowIso(),
            )

            val updated = applyScheduleState(existing, transition.state)
            taskRepository.saveTask(updated)

            val token = undoManager.registerUndo(description = "Recurrence removed") {
                val current = taskRepository.getById(taskId)
                    ?: return@registerUndo AppResult.err(AppError.NotFound)
                taskRepository.saveTask(applyScheduleState(current, previousState))
                AppResult.ok(Unit)
            }
            AppResult.ok(token)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppResult.err(AppError.Unexpected(e))
        }
    }

    suspend fun skipOccurrence(taskId: String): AppResult<String> = withContext(ioDispatcher) {
        try {
            val existing = taskRepository.getById(taskId)
                ?: return@withContext AppResult.err(AppError.NotFound)

            val rule = existing.recurrenceRule
                ?: return@withContext AppResult.err(AppError.Validation("recurrenceRule", "Task does not recur"))
            val currentDue = existing.dueDate
                ?: return@withContext AppResult.err(AppError.Validation("dueDate", "Recurring task has no due date"))

            val nextDue = RecurrenceEngine.skipOccurrence(rule, currentDue)
                ?: return@withContext AppResult.err(
                    AppError.Validation("recurrenceRule", "No next occurrence found"),
                )

            val previousState = extractScheduleState(existing)
            val transition = SchedulingTransitions.transition(
                current = previousState,
                action = SchedulingAction.AdvanceRecurrence(nextDue),
                referenceDate = clock.today(),
                referenceTimestamp = clock.nowIso(),
            )

            val updated = applyScheduleState(existing, transition.state)
            taskRepository.saveTask(updated)
            reminderService.reconcileAlarms()

            AppResult.ok(nextDue)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppResult.err(AppError.Unexpected(e))
        }
    }

    suspend fun completeRecurringTask(taskId: String): AppResult<TaskItem> = withContext(ioDispatcher) {
        try {
            val existing = taskRepository.getById(taskId)
                ?: return@withContext AppResult.err(AppError.NotFound)

            val rule = existing.recurrenceRule
                ?: return@withContext AppResult.err(AppError.Validation("recurrenceRule", "Task does not recur"))
            val currentDue = existing.dueDate
                ?: return@withContext AppResult.err(AppError.Validation("dueDate", "Recurring task has no due date"))

            val basis = existing.recurrenceBasis ?: RecurrenceBasis.FIXED.value
            val today = clock.today()
            val nowIso = clock.nowIso()

            val nextResult = RecurrenceEngine.getNextOccurrence(
                recurrenceRule = rule,
                recurrenceBasis = basis,
                currentDueDate = currentDue,
                completedDate = today,
                sourceTaskId = taskId,
            ) ?: return@withContext AppResult.err(AppError.Validation("recurrenceRule", "Recurrence ended"))

            // 1. Mark existing task completed
            val completedExisting = existing.copy(
                isCompleted = true,
                completedAt = nowIso,
                updatedAt = nowIso,
            )
            taskRepository.saveTask(completedExisting)

            // 2. Spawn next occurrence with deterministic UUIDv5 ID
            val nextTask = existing.copy(
                id = nextResult.occurrenceId,
                dueDate = nextResult.nextDueDate,
                isCompleted = false,
                completedAt = null,
                createdAt = nowIso,
                updatedAt = nowIso,
            )
            val savedNext = taskRepository.saveTask(nextTask)

            // 3. Copy tags to next occurrence
            val existingTags = tagRepository.getTagsForTasks(listOf(taskId))
            for (tagLink in existingTags) {
                tagRepository.addTagToTask(savedNext.id, tagLink.tagId)
            }

            reminderService.reconcileAlarms()
            AppResult.ok(savedNext.toTaskItem())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppResult.err(AppError.Unexpected(e))
        }
    }

    suspend fun markReviewed(taskId: String): AppResult<Unit> = withContext(ioDispatcher) {
        try {
            val existing = taskRepository.getById(taskId)
                ?: return@withContext AppResult.err(AppError.NotFound)

            val previousState = extractScheduleState(existing)
            val transition = SchedulingTransitions.transition(
                current = previousState,
                action = SchedulingAction.MarkReviewed,
                referenceDate = clock.today(),
                referenceTimestamp = clock.nowIso(),
            )

            val updated = applyScheduleState(existing, transition.state)
            taskRepository.saveTask(updated)
            AppResult.ok(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppResult.err(AppError.Unexpected(e))
        }
    }

    suspend fun acknowledgeFollowUp(taskId: String, date: String = clock.today()): AppResult<Unit> =
        withContext(ioDispatcher) {
            try {
                val existing = taskRepository.getById(taskId)
                    ?: return@withContext AppResult.err(AppError.NotFound)

                val previousState = extractScheduleState(existing)
                val transition = SchedulingTransitions.transition(
                    current = previousState,
                    action = SchedulingAction.AcknowledgeFollowUpNotification(date),
                    referenceDate = clock.today(),
                    referenceTimestamp = clock.nowIso(),
                )

                val updated = applyScheduleState(existing, transition.state)
                taskRepository.saveTask(updated)
                AppResult.ok(Unit)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppResult.err(AppError.Unexpected(e))
            }
        }

    suspend fun getWaitingTasks(): List<TaskItem> = withContext(ioDispatcher) {
        taskRepository.getWaitingTasks().map { it.toTaskItem() }
    }

    suspend fun getStaleWaitingTasks(staleDaysThreshold: Long = 7): List<TaskItem> = withContext(ioDispatcher) {
        val today = LocalDate.parse(clock.today())
        taskRepository.getWaitingTasks()
            .filter { task ->
                val waitingSince = task.waitingSince?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                if (waitingSince != null) {
                    ChronoUnit.DAYS.between(waitingSince, today) >= staleDaysThreshold
                } else {
                    false
                }
            }
            .map { it.toTaskItem() }
    }

    suspend fun getDueFollowUps(today: String = clock.today()): List<TaskItem> = withContext(ioDispatcher) {
        taskRepository.getWaitingTasks()
            .filter { task ->
                val followUp = task.followUpDate
                followUp != null && followUp <= today && task.followUpNotifiedOn != today
            }
            .map { it.toTaskItem() }
    }

    private fun extractScheduleState(task: TaskEntity): ScheduleState = ScheduleState(
        bucket = task.bucket,
        dueDate = task.dueDate,
        dueTime = task.dueTime,
        allDay = task.allDay,
        recurrenceRule = task.recurrenceRule,
        recurrenceBasis = task.recurrenceBasis,
        waitingOn = task.waitingOn,
        waitingSince = task.waitingSince,
        followUpDate = task.followUpDate,
        followUpNotifiedOn = task.followUpNotifiedOn,
        reviewedAt = task.reviewedAt,
    )

    private fun applyScheduleState(task: TaskEntity, state: ScheduleState): TaskEntity = task.copy(
        bucket = state.bucket,
        dueDate = state.dueDate,
        dueTime = state.dueTime,
        allDay = state.allDay,
        recurrenceRule = state.recurrenceRule,
        recurrenceBasis = state.recurrenceBasis,
        waitingOn = state.waitingOn,
        waitingSince = state.waitingSince,
        followUpDate = state.followUpDate,
        followUpNotifiedOn = state.followUpNotifiedOn,
        reviewedAt = state.reviewedAt,
        updatedAt = clock.nowIso(),
    )
}
