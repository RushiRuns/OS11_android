package com.os11.app.service.pomodoro

import android.content.Context
import android.content.SharedPreferences
import com.os11.app.data.db.entities.NotificationHistoryEntity
import com.os11.app.data.db.entities.PomodoroSessionEntity
import com.os11.app.data.repository.NotificationRepository
import com.os11.app.data.repository.PomodoroRepository
import com.os11.app.data.repository.SettingsRepository
import com.os11.app.data.repository.TaskRepository
import com.os11.app.di.IoDispatcher
import com.os11.app.platform.alarms.AlarmScheduler
import com.os11.app.platform.notifications.NotificationPlatformDispatcher
import com.os11.domain.id.IdGenerator
import com.os11.domain.result.AppError
import com.os11.domain.result.AppResult
import com.os11.domain.time.Clock
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service managing Pomodoro focus sessions and breaks.
 *
 * Requirements per Phase 12 and ADR-A09:
 * 1. Timer state is derived from stored timestamps (wall clock); no background ticker coroutine.
 * 2. End of a running session is an exact alarm scheduled with AlarmScheduler.
 * 3. Survives process death and device reboots via persistent SharedPreferences and AlarmManager.
 * 4. Ongoing notification uses Android system chronometer for zero-battery countdown rendering.
 * 5. Completed sessions written to pomodoro_sessions; task.pomodoro_count incremented device-locally.
 * 6. Long break cycle triggered after N sessions (default 4).
 */
@Singleton
@Suppress("TooGenericExceptionCaught", "TooManyFunctions", "LongParameterList")
class PomodoroService @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val pomodoroRepository: PomodoroRepository,
    private val taskRepository: TaskRepository,
    private val settingsRepository: SettingsRepository,
    private val notificationRepository: NotificationRepository,
    private val alarmScheduler: AlarmScheduler,
    private val notificationDispatcher: NotificationPlatformDispatcher,
    private val clock: Clock,
    private val idGenerator: IdGenerator,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _timerState = MutableStateFlow(loadPersistedState())
    val timerState: StateFlow<PomodoroState> = _timerState.asStateFlow()

    fun observeAll(): Flow<List<PomodoroSessionItem>> =
        pomodoroRepository.observeAll().map { list -> list.map { it.toItem() } }

    fun observeByTask(taskId: String): Flow<List<PomodoroSessionItem>> =
        pomodoroRepository.observeByTask(taskId).map { list -> list.map { it.toItem() } }

    fun observeTodayCompletedSessions(): Flow<Int> {
        val todayStartIso = clock.today() + "T00:00:00.000Z"
        return pomodoroRepository.observeTodayCompletedCount(todayStartIso)
    }

    fun observeTodayFocusMinutes(): Flow<Int> {
        val todayStartIso = clock.today() + "T00:00:00.000Z"
        return pomodoroRepository.observeTodayFocusSeconds(todayStartIso).map { seconds ->
            seconds / SECONDS_PER_MINUTE
        }
    }

    suspend fun startSession(
        phase: PomodoroPhase = PomodoroPhase.WORK,
        taskId: String? = null,
        taskTitle: String? = null,
        customMinutes: Int? = null,
    ): AppResult<Unit> = withContext(ioDispatcher) {
        try {
            val plannedMinutes = customMinutes ?: getDurationMinutesForPhase(phase)
            val plannedSeconds = plannedMinutes * SECONDS_PER_MINUTE
            val nowMs = clock.wallMs()
            val endAlarmMs = nowMs + (plannedSeconds * MILLIS_PER_SECOND)

            val current = _timerState.value
            val effectiveTaskId = taskId ?: current.taskId
            val effectiveTaskTitle = taskTitle ?: current.taskTitle
            val sessionsBefore = getSessionsBeforeLongBreak()

            val newState = PomodoroState(
                phase = phase,
                status = PomodoroStatus.RUNNING,
                taskId = effectiveTaskId,
                taskTitle = effectiveTaskTitle,
                startTimestamp = nowMs,
                plannedDurationSeconds = plannedSeconds,
                elapsedBeforePauseSeconds = 0,
                endAlarmTime = endAlarmMs,
                completedSessionsInCycle = current.completedSessionsInCycle,
                sessionsBeforeLongBreak = sessionsBefore,
            )

            persistState(newState)
            _timerState.value = newState

            alarmScheduler.schedulePomodoroAlarm(endAlarmMs)
            notificationDispatcher.postPomodoroOngoingNotification(
                title = phase.label,
                content = effectiveTaskTitle ?: "Focus session in progress",
                endAlarmTimeMillis = endAlarmMs,
                isPaused = false,
            )

            AppResult.ok(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppResult.err(AppError.Unexpected(e))
        }
    }

    suspend fun pauseSession(): AppResult<Unit> = withContext(ioDispatcher) {
        try {
            val current = _timerState.value
            if (current.status != PomodoroStatus.RUNNING) {
                return@withContext AppResult.ok(Unit)
            }

            val nowMs = clock.wallMs()
            val elapsed = current.currentElapsedSeconds(nowMs)

            alarmScheduler.cancelPomodoroAlarm()

            val pausedState = current.copy(
                status = PomodoroStatus.PAUSED,
                elapsedBeforePauseSeconds = elapsed,
                endAlarmTime = 0L,
            )

            persistState(pausedState)
            _timerState.value = pausedState

            notificationDispatcher.postPomodoroOngoingNotification(
                title = current.phase.label,
                content = current.taskTitle ?: "Session paused",
                endAlarmTimeMillis = 0L,
                isPaused = true,
            )

            AppResult.ok(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppResult.err(AppError.Unexpected(e))
        }
    }

    suspend fun resumeSession(): AppResult<Unit> = withContext(ioDispatcher) {
        try {
            val current = _timerState.value
            if (current.status != PomodoroStatus.PAUSED) {
                return@withContext AppResult.ok(Unit)
            }

            val nowMs = clock.wallMs()
            val remainingSec = current.remainingSeconds(nowMs)
            val endAlarmMs = nowMs + (remainingSec * MILLIS_PER_SECOND)

            val runningState = current.copy(
                status = PomodoroStatus.RUNNING,
                startTimestamp = nowMs,
                endAlarmTime = endAlarmMs,
            )

            persistState(runningState)
            _timerState.value = runningState

            alarmScheduler.schedulePomodoroAlarm(endAlarmMs)
            notificationDispatcher.postPomodoroOngoingNotification(
                title = current.phase.label,
                content = current.taskTitle ?: "Focus session in progress",
                endAlarmTimeMillis = endAlarmMs,
                isPaused = false,
            )

            AppResult.ok(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppResult.err(AppError.Unexpected(e))
        }
    }

    suspend fun resetSession(): AppResult<Unit> = withContext(ioDispatcher) {
        try {
            alarmScheduler.cancelPomodoroAlarm()
            notificationDispatcher.cancelPomodoroNotification()

            val current = _timerState.value
            val defaultSeconds = getDurationMinutesForPhase(current.phase) * SECONDS_PER_MINUTE
            val resetState = current.copy(
                status = PomodoroStatus.IDLE,
                startTimestamp = 0L,
                plannedDurationSeconds = defaultSeconds,
                elapsedBeforePauseSeconds = 0,
                endAlarmTime = 0L,
            )

            persistState(resetState)
            _timerState.value = resetState
            AppResult.ok(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppResult.err(AppError.Unexpected(e))
        }
    }

    suspend fun completeSession(): AppResult<Unit> = withContext(ioDispatcher) {
        try {
            val current = _timerState.value
            if (current.status == PomodoroStatus.COMPLETED || current.status == PomodoroStatus.IDLE) {
                return@withContext AppResult.ok(Unit)
            }

            alarmScheduler.cancelPomodoroAlarm()
            notificationDispatcher.cancelPomodoroNotification()

            val nowIso = clock.nowIso()
            recordCompletedSession(current, nowIso)

            val (nextPhase, nextCompletedCount) = calculateNextPhaseAndCycle(current)
            dispatchCompletionNotifications(current, nextPhase, nowIso)

            val nextPlannedSeconds = getDurationMinutesForPhase(nextPhase) * SECONDS_PER_MINUTE
            val completedState = current.copy(
                status = PomodoroStatus.COMPLETED,
                phase = nextPhase,
                plannedDurationSeconds = nextPlannedSeconds,
                elapsedBeforePauseSeconds = 0,
                endAlarmTime = 0L,
                completedSessionsInCycle = nextCompletedCount,
            )

            persistState(completedState)
            _timerState.value = completedState
            AppResult.ok(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppResult.err(AppError.Unexpected(e))
        }
    }

    private suspend fun recordCompletedSession(current: PomodoroState, nowIso: String) {
        val sessionRecord = PomodoroSessionEntity(
            id = idGenerator.randomId(),
            taskId = current.taskId,
            type = current.phase.raw,
            durationSeconds = current.plannedDurationSeconds,
            startedAt = if (current.startTimestamp > 0L) {
                java.time.Instant.ofEpochMilli(current.startTimestamp).toString()
            } else {
                nowIso
            },
            endedAt = nowIso,
            wasCompleted = true,
            createdAt = nowIso,
        )
        pomodoroRepository.save(sessionRecord)

        if (current.phase == PomodoroPhase.WORK && current.taskId != null) {
            taskRepository.incrementPomodoroCount(current.taskId)
        }
    }

    private fun calculateNextPhaseAndCycle(current: PomodoroState): Pair<PomodoroPhase, Int> {
        if (current.phase != PomodoroPhase.WORK) {
            return Pair(PomodoroPhase.WORK, current.completedSessionsInCycle)
        }
        val count = current.completedSessionsInCycle + 1
        return if (count >= current.sessionsBeforeLongBreak) {
            Pair(PomodoroPhase.LONG_BREAK, 0)
        } else {
            Pair(PomodoroPhase.SHORT_BREAK, count)
        }
    }

    private suspend fun dispatchCompletionNotifications(
        current: PomodoroState,
        nextPhase: PomodoroPhase,
        nowIso: String,
    ) {
        val notifTitle = if (current.phase == PomodoroPhase.WORK) {
            "Focus Session Complete!"
        } else {
            "Break Finished!"
        }

        val notifContent = if (current.phase == PomodoroPhase.WORK) {
            val taskPart = current.taskTitle?.let { " on '$it'" } ?: ""
            val breakLabel = if (nextPhase == PomodoroPhase.LONG_BREAK) "a long break" else "a short break"
            "Completed ${current.plannedDurationSeconds / SECONDS_PER_MINUTE}m$taskPart. Ready for $breakLabel?"
        } else {
            "Time to dive back into focus!"
        }

        notificationDispatcher.postPomodoroCompleteNotification(
            title = notifTitle,
            content = notifContent,
            nextPhase = nextPhase.raw,
        )

        notificationRepository.add(
            NotificationHistoryEntity(
                id = idGenerator.randomId(),
                type = "pomodoro",
                taskId = current.taskId,
                title = notifTitle,
                body = notifContent,
                createdAt = nowIso,
            ),
        )
    }

    suspend fun startNextPhase(): AppResult<Unit> {
        val current = _timerState.value
        return startSession(
            phase = current.phase,
            taskId = current.taskId,
            taskTitle = current.taskTitle,
        )
    }

    suspend fun skipToNextPhase(): AppResult<Unit> = withContext(ioDispatcher) {
        try {
            alarmScheduler.cancelPomodoroAlarm()
            notificationDispatcher.cancelPomodoroNotification()

            val current = _timerState.value
            val nextPhase = when (current.phase) {
                PomodoroPhase.WORK -> PomodoroPhase.SHORT_BREAK
                PomodoroPhase.SHORT_BREAK -> PomodoroPhase.WORK
                PomodoroPhase.LONG_BREAK -> PomodoroPhase.WORK
            }
            val defaultSeconds = getDurationMinutesForPhase(nextPhase) * SECONDS_PER_MINUTE

            val skippedState = current.copy(
                phase = nextPhase,
                status = PomodoroStatus.IDLE,
                startTimestamp = 0L,
                plannedDurationSeconds = defaultSeconds,
                elapsedBeforePauseSeconds = 0,
                endAlarmTime = 0L,
            )

            persistState(skippedState)
            _timerState.value = skippedState
            AppResult.ok(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppResult.err(AppError.Unexpected(e))
        }
    }

    fun linkTask(taskId: String, taskTitle: String): AppResult<Unit> {
        val current = _timerState.value
        val updated = current.copy(taskId = taskId, taskTitle = taskTitle)
        persistState(updated)
        _timerState.value = updated
        return AppResult.ok(Unit)
    }

    fun unlinkTask(): AppResult<Unit> {
        val current = _timerState.value
        val updated = current.copy(taskId = null, taskTitle = null)
        persistState(updated)
        _timerState.value = updated
        return AppResult.ok(Unit)
    }

    suspend fun selectPhase(phase: PomodoroPhase): AppResult<Unit> = withContext(ioDispatcher) {
        try {
            val current = _timerState.value
            if (current.phase == phase && current.status == PomodoroStatus.IDLE) {
                return@withContext AppResult.ok(Unit)
            }

            alarmScheduler.cancelPomodoroAlarm()
            notificationDispatcher.cancelPomodoroNotification()

            val defaultSeconds = getDurationMinutesForPhase(phase) * SECONDS_PER_MINUTE
            val updated = current.copy(
                phase = phase,
                status = PomodoroStatus.IDLE,
                startTimestamp = 0L,
                plannedDurationSeconds = defaultSeconds,
                elapsedBeforePauseSeconds = 0,
                endAlarmTime = 0L,
            )

            persistState(updated)
            _timerState.value = updated
            AppResult.ok(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppResult.err(AppError.Unexpected(e))
        }
    }

    suspend fun reconcileTimerAfterReboot(): AppResult<Unit> = withContext(ioDispatcher) {
        try {
            val current = _timerState.value
            if (current.status == PomodoroStatus.RUNNING) {
                val nowMs = clock.wallMs()
                if (current.endAlarmTime in 1..nowMs) {
                    // Session ended while device was powered down or process was dead
                    completeSession()
                } else if (current.endAlarmTime > nowMs) {
                    // Session is still ongoing: re-arm exact alarm and restore chronometer notification
                    alarmScheduler.schedulePomodoroAlarm(current.endAlarmTime)
                    notificationDispatcher.postPomodoroOngoingNotification(
                        title = current.phase.label,
                        content = current.taskTitle ?: "Focus session in progress",
                        endAlarmTimeMillis = current.endAlarmTime,
                        isPaused = false,
                    )
                }
            }
            AppResult.ok(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppResult.err(AppError.Unexpected(e))
        }
    }

    suspend fun recordSession(
        taskId: String? = null,
        type: String = "work",
        durationMinutes: Int = 25,
        completed: Boolean = true,
    ): AppResult<PomodoroSessionEntity> = withContext(ioDispatcher) {
        try {
            val now = clock.nowIso()
            val session = PomodoroSessionEntity(
                id = idGenerator.randomId(),
                taskId = taskId,
                type = type,
                durationSeconds = durationMinutes * SECONDS_PER_MINUTE,
                startedAt = now,
                endedAt = now,
                wasCompleted = completed,
                createdAt = now,
            )
            val saved = pomodoroRepository.save(session)
            if (type == "work" && completed && taskId != null) {
                taskRepository.incrementPomodoroCount(taskId)
            }
            AppResult.ok(saved)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppResult.err(AppError.Unexpected(e))
        }
    }

    suspend fun getWorkMinutes(): Int =
        settingsRepository.get("pomodoro_work_minutes")?.toIntOrNull() ?: DEFAULT_WORK_MINUTES

    suspend fun getBreakMinutes(): Int =
        settingsRepository.get("pomodoro_break_minutes")?.toIntOrNull() ?: DEFAULT_BREAK_MINUTES

    suspend fun getLongBreakMinutes(): Int =
        settingsRepository.get("pomodoro_long_break_minutes")?.toIntOrNull() ?: DEFAULT_LONG_BREAK_MINUTES

    suspend fun getSessionsBeforeLongBreak(): Int =
        settingsRepository.get("pomodoro_sessions_before_long_break")?.toIntOrNull()
            ?: DEFAULT_SESSIONS_BEFORE_LONG_BREAK

    suspend fun updateSettings(
        workMin: Int,
        breakMin: Int,
        longBreakMin: Int,
        sessionsBeforeLongBreak: Int,
    ): AppResult<Unit> = withContext(ioDispatcher) {
        try {
            settingsRepository.save("pomodoro_work_minutes", workMin.toString())
            settingsRepository.save("pomodoro_break_minutes", breakMin.toString())
            settingsRepository.save("pomodoro_long_break_minutes", longBreakMin.toString())
            settingsRepository.save("pomodoro_sessions_before_long_break", sessionsBeforeLongBreak.toString())

            val current = _timerState.value
            if (current.status == PomodoroStatus.IDLE) {
                val newSeconds = when (current.phase) {
                    PomodoroPhase.WORK -> workMin * SECONDS_PER_MINUTE
                    PomodoroPhase.SHORT_BREAK -> breakMin * SECONDS_PER_MINUTE
                    PomodoroPhase.LONG_BREAK -> longBreakMin * SECONDS_PER_MINUTE
                }
                val updated = current.copy(
                    plannedDurationSeconds = newSeconds,
                    sessionsBeforeLongBreak = sessionsBeforeLongBreak,
                )
                persistState(updated)
                _timerState.value = updated
            }
            AppResult.ok(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppResult.err(AppError.Unexpected(e))
        }
    }

    private suspend fun getDurationMinutesForPhase(phase: PomodoroPhase): Int = when (phase) {
        PomodoroPhase.WORK -> getWorkMinutes()
        PomodoroPhase.SHORT_BREAK -> getBreakMinutes()
        PomodoroPhase.LONG_BREAK -> getLongBreakMinutes()
    }

    private fun loadPersistedState(): PomodoroState {
        val phaseRaw = prefs.getString(KEY_PHASE, PomodoroPhase.WORK.raw) ?: PomodoroPhase.WORK.raw
        val statusName = prefs.getString(KEY_STATUS, PomodoroStatus.IDLE.name) ?: PomodoroStatus.IDLE.name
        val taskId = prefs.getString(KEY_TASK_ID, null)
        val taskTitle = prefs.getString(KEY_TASK_TITLE, null)
        val startTimestamp = prefs.getLong(KEY_START_TIMESTAMP, 0L)
        val plannedSeconds = prefs.getInt(KEY_PLANNED_DURATION_SECONDS, DEFAULT_WORK_MINUTES * SECONDS_PER_MINUTE)
        val elapsedPause = prefs.getInt(KEY_ELAPSED_BEFORE_PAUSE_SECONDS, 0)
        val endAlarm = prefs.getLong(KEY_END_ALARM_TIME, 0L)
        val completedSessions = prefs.getInt(KEY_COMPLETED_SESSIONS, 0)
        val sessionsBefore = prefs.getInt(KEY_SESSIONS_BEFORE_LONG_BREAK, DEFAULT_SESSIONS_BEFORE_LONG_BREAK)

        val status = runCatching { PomodoroStatus.valueOf(statusName) }.getOrDefault(PomodoroStatus.IDLE)
        val phase = PomodoroPhase.fromRaw(phaseRaw)

        return PomodoroState(
            phase = phase,
            status = status,
            taskId = taskId,
            taskTitle = taskTitle,
            startTimestamp = startTimestamp,
            plannedDurationSeconds = plannedSeconds,
            elapsedBeforePauseSeconds = elapsedPause,
            endAlarmTime = endAlarm,
            completedSessionsInCycle = completedSessions,
            sessionsBeforeLongBreak = sessionsBefore,
        )
    }

    private fun persistState(state: PomodoroState) {
        prefs.edit()
            .putString(KEY_PHASE, state.phase.raw)
            .putString(KEY_STATUS, state.status.name)
            .putString(KEY_TASK_ID, state.taskId)
            .putString(KEY_TASK_TITLE, state.taskTitle)
            .putLong(KEY_START_TIMESTAMP, state.startTimestamp)
            .putInt(KEY_PLANNED_DURATION_SECONDS, state.plannedDurationSeconds)
            .putInt(KEY_ELAPSED_BEFORE_PAUSE_SECONDS, state.elapsedBeforePauseSeconds)
            .putLong(KEY_END_ALARM_TIME, state.endAlarmTime)
            .putInt(KEY_COMPLETED_SESSIONS, state.completedSessionsInCycle)
            .putInt(KEY_SESSIONS_BEFORE_LONG_BREAK, state.sessionsBeforeLongBreak)
            .apply()
    }

    private fun PomodoroSessionEntity.toItem() = PomodoroSessionItem(
        id = id,
        taskId = taskId,
        type = type,
        durationSeconds = durationSeconds,
        startedAt = startedAt,
        endedAt = endedAt,
        wasCompleted = wasCompleted,
    )

    companion object {
        private const val PREFS_NAME = "os11_pomodoro_prefs"
        private const val KEY_PHASE = "phase"
        private const val KEY_STATUS = "status"
        private const val KEY_TASK_ID = "task_id"
        private const val KEY_TASK_TITLE = "task_title"
        private const val KEY_START_TIMESTAMP = "start_timestamp"
        private const val KEY_PLANNED_DURATION_SECONDS = "planned_duration_seconds"
        private const val KEY_ELAPSED_BEFORE_PAUSE_SECONDS = "elapsed_before_pause_seconds"
        private const val KEY_END_ALARM_TIME = "end_alarm_time"
        private const val KEY_COMPLETED_SESSIONS = "completed_sessions"
        private const val KEY_SESSIONS_BEFORE_LONG_BREAK = "sessions_before_long_break"

        private const val SECONDS_PER_MINUTE = 60
        private const val MILLIS_PER_SECOND = 1000L
        private const val DEFAULT_WORK_MINUTES = 25
        private const val DEFAULT_BREAK_MINUTES = 5
        private const val DEFAULT_LONG_BREAK_MINUTES = 15
        private const val DEFAULT_SESSIONS_BEFORE_LONG_BREAK = 4
    }
}
