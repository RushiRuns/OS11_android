package com.os11.app.ui.features.pomodoro

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.os11.app.service.pomodoro.PomodoroPhase
import com.os11.app.service.pomodoro.PomodoroService
import com.os11.app.service.pomodoro.PomodoroState
import com.os11.app.service.pomodoro.PomodoroStatus
import com.os11.app.service.task.TaskItem
import com.os11.app.service.task.TaskService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PomodoroUiState(
    val phase: PomodoroPhase = PomodoroPhase.WORK,
    val status: PomodoroStatus = PomodoroStatus.IDLE,
    val taskId: String? = null,
    val taskTitle: String? = null,
    val remainingSeconds: Int = 25 * 60,
    val totalDurationSeconds: Int = 25 * 60,
    val progress: Float = 0f,
    val completedSessionsInCycle: Int = 0,
    val sessionsBeforeLongBreak: Int = 4,
    val todayCompletedSessions: Int = 0,
    val todayFocusMinutes: Int = 0,
    val workMinutesSetting: Int = 25,
    val breakMinutesSetting: Int = 5,
    val longBreakMinutesSetting: Int = 15,
)

@HiltViewModel
@Suppress("TooManyFunctions")
class PomodoroViewModel @Inject constructor(
    private val pomodoroService: PomodoroService,
    private val taskService: TaskService,
) : ViewModel() {

    private val tickMillis = MutableStateFlow(System.currentTimeMillis())

    val rawTimerState: StateFlow<PomodoroState> = pomodoroService.timerState

    val availableTasks: StateFlow<List<TaskItem>> = taskService.observeAllTasks()
        .combine(taskService.observeInboxTasks()) { all, inbox ->
            (inbox + all).distinctBy { it.id }.filter { !it.isCompleted }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    private val _uiState = MutableStateFlow(computeUiState(pomodoroService.timerState.value))
    val uiState: StateFlow<PomodoroUiState> = _uiState.asStateFlow()

    init {
        // Ticker loop only active when ViewModel is active
        viewModelScope.launch {
            while (isActive) {
                delay(TICK_INTERVAL_MS)
                tickMillis.value = System.currentTimeMillis()
                val currentTimer = pomodoroService.timerState.value
                val remaining = currentTimer.remainingSeconds(tickMillis.value)
                if (currentTimer.status == PomodoroStatus.RUNNING && remaining <= 0) {
                    pomodoroService.completeSession()
                } else {
                    _uiState.value = computeUiState(currentTimer)
                }
            }
        }

        // React to timer state changes
        viewModelScope.launch {
            pomodoroService.timerState.collect { state ->
                _uiState.value = computeUiState(state)
            }
        }

        // React to today's stats
        viewModelScope.launch {
            combine(
                pomodoroService.observeTodayCompletedSessions(),
                pomodoroService.observeTodayFocusMinutes(),
            ) { count, minutes ->
                _uiState.value = _uiState.value.copy(
                    todayCompletedSessions = count,
                    todayFocusMinutes = minutes,
                )
            }.collect {}
        }

        // Load duration settings
        viewModelScope.launch {
            val workMin = pomodoroService.getWorkMinutes()
            val breakMin = pomodoroService.getBreakMinutes()
            val longBreakMin = pomodoroService.getLongBreakMinutes()
            _uiState.value = _uiState.value.copy(
                workMinutesSetting = workMin,
                breakMinutesSetting = breakMin,
                longBreakMinutesSetting = longBreakMin,
            )
        }
    }

    fun togglePlayPause() {
        val current = pomodoroService.timerState.value
        viewModelScope.launch {
            when (current.status) {
                PomodoroStatus.IDLE -> pomodoroService.startSession(phase = current.phase)
                PomodoroStatus.RUNNING -> pomodoroService.pauseSession()
                PomodoroStatus.PAUSED -> pomodoroService.resumeSession()
                PomodoroStatus.COMPLETED -> pomodoroService.startNextPhase()
            }
        }
    }

    fun resetTimer() {
        viewModelScope.launch {
            pomodoroService.resetSession()
        }
    }

    fun selectPhase(phase: PomodoroPhase) {
        viewModelScope.launch {
            pomodoroService.selectPhase(phase)
        }
    }

    fun skipToNext() {
        viewModelScope.launch {
            pomodoroService.skipToNextPhase()
        }
    }

    fun linkTask(task: TaskItem) {
        pomodoroService.linkTask(task.id, task.title)
    }

    fun unlinkTask() {
        pomodoroService.unlinkTask()
    }

    fun updateSettings(workMin: Int, breakMin: Int, longBreakMin: Int, sessions: Int) {
        viewModelScope.launch {
            pomodoroService.updateSettings(workMin, breakMin, longBreakMin, sessions)
            _uiState.value = _uiState.value.copy(
                workMinutesSetting = workMin,
                breakMinutesSetting = breakMin,
                longBreakMinutesSetting = longBreakMin,
                sessionsBeforeLongBreak = sessions,
            )
        }
    }

    private fun computeUiState(timer: PomodoroState): PomodoroUiState {
        val now = System.currentTimeMillis()
        val remaining = timer.remainingSeconds(now)
        val progress = timer.progress(now)

        return _uiState.value.copy(
            phase = timer.phase,
            status = timer.status,
            taskId = timer.taskId,
            taskTitle = timer.taskTitle,
            remainingSeconds = remaining,
            totalDurationSeconds = timer.plannedDurationSeconds,
            progress = progress,
            completedSessionsInCycle = timer.completedSessionsInCycle,
            sessionsBeforeLongBreak = timer.sessionsBeforeLongBreak,
        )
    }

    companion object {
        private const val TICK_INTERVAL_MS = 500L
    }
}
