package com.os11.app.ui.features.focus

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.os11.app.service.pomodoro.PomodoroPhase
import com.os11.app.service.pomodoro.PomodoroService
import com.os11.app.service.pomodoro.PomodoroStatus
import com.os11.app.service.task.TaskService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FocusModeUiState(
    val taskId: String? = null,
    val taskTitle: String = "Focus Session",
    val taskNotes: String? = null,
    val isTaskCompleted: Boolean = false,
    val phase: PomodoroPhase = PomodoroPhase.WORK,
    val status: PomodoroStatus = PomodoroStatus.IDLE,
    val remainingSeconds: Int = DEFAULT_WORK_SECONDS,
    val totalSeconds: Int = DEFAULT_WORK_SECONDS,
    val progress: Float = 1.0f,
    val keepScreenOn: Boolean = true,
) {
    companion object {
        const val DEFAULT_WORK_SECONDS = 25 * 60
    }
}

/**
 * ViewModel driving the immersive Focus Mode screen.
 * Links active Pomodoro countdown and current task details with keep-screen-on toggling.
 */
@HiltViewModel
class FocusModeViewModel @Inject constructor(
    private val pomodoroService: PomodoroService,
    private val taskService: TaskService,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val navTaskId: String? = savedStateHandle["taskId"]

    private val _uiState = MutableStateFlow(FocusModeUiState(taskId = navTaskId))
    val uiState: StateFlow<FocusModeUiState> = _uiState.asStateFlow()

    init {
        // 1. Observe timer state from PomodoroService
        viewModelScope.launch {
            pomodoroService.timerState.collect { pState ->
                val remaining = pState.remainingSeconds()
                val total = pState.plannedDurationSeconds.coerceAtLeast(1)
                val prog = pState.progress()

                _uiState.update { current ->
                    current.copy(
                        phase = pState.phase,
                        status = pState.status,
                        remainingSeconds = remaining,
                        totalSeconds = total,
                        progress = prog,
                        taskId = current.taskId ?: pState.taskId,
                        taskTitle = pState.taskTitle ?: current.taskTitle,
                    )
                }
            }
        }

        // 2. Load task details if provided
        if (navTaskId != null) {
            viewModelScope.launch {
                val entity = taskService.getById(navTaskId)
                if (entity != null) {
                    _uiState.update { current ->
                        current.copy(
                            taskTitle = entity.title,
                            taskNotes = entity.notes,
                            isTaskCompleted = entity.isCompleted,
                        )
                    }
                    // If no timer running, start one linked to this task
                    if (pomodoroService.timerState.value.status == PomodoroStatus.IDLE) {
                        pomodoroService.startSession(
                            phase = PomodoroPhase.WORK,
                            taskId = entity.id,
                            taskTitle = entity.title,
                        )
                    }
                }
                taskService.observeById(navTaskId).collect { updated ->
                    if (updated != null) {
                        _uiState.update { current ->
                            current.copy(
                                taskTitle = updated.title,
                                taskNotes = updated.notes,
                                isTaskCompleted = updated.isCompleted,
                            )
                        }
                    }
                }
            }
        }
    }

    fun togglePlayPause() {
        viewModelScope.launch {
            val currentStatus = _uiState.value.status
            when (currentStatus) {
                PomodoroStatus.RUNNING -> pomodoroService.pauseSession()
                PomodoroStatus.PAUSED -> pomodoroService.resumeSession()
                PomodoroStatus.COMPLETED -> pomodoroService.startNextPhase()
                PomodoroStatus.IDLE -> {
                    val state = _uiState.value
                    pomodoroService.startSession(
                        phase = state.phase,
                        taskId = state.taskId,
                        taskTitle = state.taskTitle,
                    )
                }
            }
        }
    }

    fun skipToNext() {
        viewModelScope.launch {
            pomodoroService.skipToNextPhase()
        }
    }

    fun resetTimer() {
        viewModelScope.launch {
            pomodoroService.resetSession()
        }
    }

    fun toggleKeepScreenOn() {
        _uiState.update { it.copy(keepScreenOn = !it.keepScreenOn) }
    }

    fun completeTask(onCompleted: () -> Unit = {}) {
        val targetId = _uiState.value.taskId
        if (targetId != null) {
            viewModelScope.launch {
                taskService.bulkComplete(listOf(targetId), true)
                _uiState.update { it.copy(isTaskCompleted = true) }
                pomodoroService.resetSession()
                onCompleted()
            }
        }
    }
}
