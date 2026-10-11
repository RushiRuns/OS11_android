package com.os11.app.ui.features.focus

import androidx.lifecycle.SavedStateHandle
import com.os11.app.data.db.entities.TaskEntity
import com.os11.app.service.pomodoro.PomodoroPhase
import com.os11.app.service.pomodoro.PomodoroService
import com.os11.app.service.pomodoro.PomodoroState
import com.os11.app.service.pomodoro.PomodoroStatus
import com.os11.app.service.task.TaskService
import com.os11.app.service.undo.UndoToken
import com.os11.domain.result.AppResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FocusModeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val pomodoroService: PomodoroService = mockk(relaxed = true)
    private val taskService: TaskService = mockk(relaxed = true)
    private val timerStateFlow = MutableStateFlow(PomodoroState())

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { pomodoroService.timerState } returns timerStateFlow
        coEvery { pomodoroService.startSession(any(), any(), any(), any()) } coAnswers {
            val phase = firstArg<PomodoroPhase>()
            val taskId = secondArg<String?>()
            val taskTitle = thirdArg<String?>()
            timerStateFlow.value = PomodoroState(
                phase = phase,
                status = PomodoroStatus.RUNNING,
                taskId = taskId,
                taskTitle = taskTitle,
                startTimestamp = 1_000_000L,
                plannedDurationSeconds = 1500,
                elapsedBeforePauseSeconds = 0,
                endAlarmTime = 2_500_000L,
                completedSessionsInCycle = 0,
                sessionsBeforeLongBreak = 4,
            )
            AppResult.ok(Unit)
        }
        coEvery { pomodoroService.pauseSession() } coAnswers {
            timerStateFlow.value = timerStateFlow.value.copy(status = PomodoroStatus.PAUSED)
            AppResult.ok(Unit)
        }
        coEvery { pomodoroService.resumeSession() } coAnswers {
            timerStateFlow.value = timerStateFlow.value.copy(status = PomodoroStatus.RUNNING)
            AppResult.ok(Unit)
        }
        coEvery { pomodoroService.skipToNextPhase() } coAnswers {
            timerStateFlow.value = timerStateFlow.value.copy(phase = PomodoroPhase.SHORT_BREAK)
            AppResult.ok(Unit)
        }
        coEvery { pomodoroService.resetSession() } coAnswers {
            timerStateFlow.value = PomodoroState()
            AppResult.ok(Unit)
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun focus_mode_without_task_id_starts_idle() = runBlocking {
        val savedStateHandle = SavedStateHandle()
        val viewModel = FocusModeViewModel(
            pomodoroService = pomodoroService,
            taskService = taskService,
            savedStateHandle = savedStateHandle,
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Focus Session", state.taskTitle)
        assertEquals(PomodoroStatus.IDLE, state.status)
        assertTrue(state.keepScreenOn)
    }

    @Test
    fun focus_mode_with_task_id_loads_task_and_starts_session() = runBlocking {
        val task = TaskEntity(
            id = "task-1",
            workspaceId = "ws-1",
            title = "Write design document",
            sortOrder = 0.0,
            createdAt = "2026-10-11T00:00:00Z",
            updatedAt = "2026-10-11T00:00:00Z",
        )
        coEvery { taskService.getById("task-1") } returns task
        every { taskService.observeById("task-1") } returns flowOf(task)

        val savedStateHandle = SavedStateHandle(mapOf("taskId" to "task-1"))
        val viewModel = FocusModeViewModel(
            pomodoroService = pomodoroService,
            taskService = taskService,
            savedStateHandle = savedStateHandle,
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Write design document", state.taskTitle)
        assertEquals(PomodoroStatus.RUNNING, state.status)
        assertEquals(PomodoroPhase.WORK, state.phase)
    }

    @Test
    fun toggle_play_pause_and_skip() = runBlocking {
        val savedStateHandle = SavedStateHandle()
        val viewModel = FocusModeViewModel(
            pomodoroService = pomodoroService,
            taskService = taskService,
            savedStateHandle = savedStateHandle,
        )
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.togglePlayPause()
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(PomodoroStatus.RUNNING, viewModel.uiState.value.status)

        viewModel.togglePlayPause()
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(PomodoroStatus.PAUSED, viewModel.uiState.value.status)

        viewModel.togglePlayPause()
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(PomodoroStatus.RUNNING, viewModel.uiState.value.status)

        viewModel.skipToNext()
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(PomodoroPhase.SHORT_BREAK, viewModel.uiState.value.phase)
    }

    @Test
    fun toggle_keep_screen_on() = runBlocking {
        val savedStateHandle = SavedStateHandle()
        val viewModel = FocusModeViewModel(
            pomodoroService = pomodoroService,
            taskService = taskService,
            savedStateHandle = savedStateHandle,
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.keepScreenOn)
        viewModel.toggleKeepScreenOn()
        assertFalse(viewModel.uiState.value.keepScreenOn)
        viewModel.toggleKeepScreenOn()
        assertTrue(viewModel.uiState.value.keepScreenOn)
    }

    @Test
    fun complete_task_marks_task_completed_and_stops_timer() = runBlocking {
        val task = TaskEntity(
            id = "task-2",
            workspaceId = "ws-1",
            title = "Refactor network layer",
            sortOrder = 0.0,
            createdAt = "2026-10-11T00:00:00Z",
            updatedAt = "2026-10-11T00:00:00Z",
        )
        coEvery { taskService.getById("task-2") } returns task
        every { taskService.observeById("task-2") } returns flowOf(task)
        val dummyToken = UndoToken(id = "undo-1", actionDescription = "Completed")
        coEvery { taskService.bulkComplete(listOf("task-2"), true) } returns AppResult.ok(dummyToken)

        val savedStateHandle = SavedStateHandle(mapOf("taskId" to "task-2"))
        val viewModel = FocusModeViewModel(
            pomodoroService = pomodoroService,
            taskService = taskService,
            savedStateHandle = savedStateHandle,
        )
        testDispatcher.scheduler.advanceUntilIdle()

        var completedCallbackCalled = false
        viewModel.completeTask {
            completedCallbackCalled = true
        }
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(completedCallbackCalled)
        assertTrue(viewModel.uiState.value.isTaskCompleted)
        assertEquals(PomodoroStatus.IDLE, viewModel.uiState.value.status)
        coVerify { taskService.bulkComplete(listOf("task-2"), true) }
    }
}
