package com.os11.app.service

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.os11.app.data.repository.NotificationRepository
import com.os11.app.data.repository.PomodoroRepository
import com.os11.app.data.repository.SettingsRepository
import com.os11.app.data.repository.TaskRepository
import com.os11.app.platform.alarms.AlarmScheduler
import com.os11.app.platform.notifications.NotificationPlatformDispatcher
import com.os11.app.service.pomodoro.PomodoroPhase
import com.os11.app.service.pomodoro.PomodoroService
import com.os11.app.service.pomodoro.PomodoroStatus
import com.os11.domain.id.IdGenerator
import com.os11.domain.time.TestClock
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PomodoroServiceTest {

    private lateinit var context: Context
    private val pomodoroRepository: PomodoroRepository = mockk(relaxed = true)
    private val taskRepository: TaskRepository = mockk(relaxed = true)
    private val settingsRepository: SettingsRepository = mockk(relaxed = true)
    private val notificationRepository: NotificationRepository = mockk(relaxed = true)
    private val alarmScheduler: AlarmScheduler = mockk(relaxed = true)
    private val notificationDispatcher: NotificationPlatformDispatcher = mockk(relaxed = true)
    private val idGenerator: IdGenerator = mockk(relaxed = true)

    private val baseTimeMs = 1_700_000_000_000L
    private lateinit var testClock: TestClock
    private lateinit var pomodoroService: PomodoroService

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        // Clear SharedPreferences before test
        context.getSharedPreferences("os11_pomodoro_prefs", Context.MODE_PRIVATE).edit().clear().commit()

        testClock = TestClock(baseTimeMs)
        coEvery { idGenerator.randomId() } returns "test-session-id"
        coEvery { settingsRepository.get("pomodoro_work_minutes") } returns "25"
        coEvery { settingsRepository.get("pomodoro_break_minutes") } returns "5"
        coEvery { settingsRepository.get("pomodoro_long_break_minutes") } returns "15"
        coEvery { settingsRepository.get("pomodoro_sessions_before_long_break") } returns "4"

        pomodoroService = PomodoroService(
            context = context,
            pomodoroRepository = pomodoroRepository,
            taskRepository = taskRepository,
            settingsRepository = settingsRepository,
            notificationRepository = notificationRepository,
            alarmScheduler = alarmScheduler,
            notificationDispatcher = notificationDispatcher,
            clock = testClock,
            idGenerator = idGenerator,
            ioDispatcher = Dispatchers.Unconfined,
        )
    }

    @Test
    fun `startSession sets running state and schedules exact alarm`() = runTest {
        val result = pomodoroService.startSession(
            phase = PomodoroPhase.WORK,
            taskId = "task-1",
            taskTitle = "Write Kotlin tests",
        )

        assertTrue(result.isOk)
        val state = pomodoroService.timerState.value
        assertEquals(PomodoroStatus.RUNNING, state.status)
        assertEquals(PomodoroPhase.WORK, state.phase)
        assertEquals("task-1", state.taskId)
        assertEquals("Write Kotlin tests", state.taskTitle)
        assertEquals(25 * 60, state.plannedDurationSeconds)

        val expectedEndMs = baseTimeMs + (25 * 60 * 1000L)
        assertEquals(expectedEndMs, state.endAlarmTime)

        coVerify { alarmScheduler.schedulePomodoroAlarm(expectedEndMs) }
        coVerify {
            notificationDispatcher.postPomodoroOngoingNotification(
                title = "Focus",
                content = "Write Kotlin tests",
                endAlarmTimeMillis = expectedEndMs,
                isPaused = false,
            )
        }
    }

    @Test
    fun `derived countdown correctly computes remaining time without background drift`() = runTest {
        pomodoroService.startSession(phase = PomodoroPhase.WORK, customMinutes = 25)

        val state = pomodoroService.timerState.value
        // Initial remaining: 1500s
        assertEquals(1500, state.remainingSeconds(baseTimeMs))

        // Advance clock by 10 minutes (600 seconds)
        val advancedTimeMs = baseTimeMs + (600 * 1000L)
        testClock.set(advancedTimeMs)

        assertEquals(900, state.remainingSeconds(advancedTimeMs))
        assertEquals(0.40f, state.progress(advancedTimeMs), 0.01f)
    }

    @Test
    fun `pause and resume preserves elapsed seconds across time leaps`() = runTest {
        pomodoroService.startSession(phase = PomodoroPhase.WORK, customMinutes = 25)

        // Advance 300s (5m) and pause
        val pauseTimeMs = baseTimeMs + (300 * 1000L)
        testClock.set(pauseTimeMs)
        pomodoroService.pauseSession()

        val pausedState = pomodoroService.timerState.value
        assertEquals(PomodoroStatus.PAUSED, pausedState.status)
        assertEquals(300, pausedState.elapsedBeforePauseSeconds)
        assertEquals(1200, pausedState.remainingSeconds(pauseTimeMs))
        coVerify { alarmScheduler.cancelPomodoroAlarm() }

        // Leap forward by 1 hour while paused
        val resumeTimeMs = pauseTimeMs + (3600 * 1000L)
        testClock.set(resumeTimeMs)
        pomodoroService.resumeSession()

        val resumedState = pomodoroService.timerState.value
        assertEquals(PomodoroStatus.RUNNING, resumedState.status)
        assertEquals(1200, resumedState.remainingSeconds(resumeTimeMs))

        val newExpectedEndMs = resumeTimeMs + (1200 * 1000L)
        assertEquals(newExpectedEndMs, resumedState.endAlarmTime)
        coVerify { alarmScheduler.schedulePomodoroAlarm(newExpectedEndMs) }
    }

    @Test
    fun `completeSession writes record increments task count and advances cycle`() = runTest {
        pomodoroService.startSession(
            phase = PomodoroPhase.WORK,
            taskId = "task-focus-1",
            taskTitle = "Deep Work",
            customMinutes = 25,
        )

        val result = pomodoroService.completeSession()
        assertTrue(result.isOk)

        coVerify { pomodoroRepository.save(any()) }
        coVerify { taskRepository.incrementPomodoroCount("task-focus-1") }
        coVerify { notificationDispatcher.cancelPomodoroNotification() }
        coVerify { notificationDispatcher.postPomodoroCompleteNotification(any(), any(), any()) }

        val after = pomodoroService.timerState.value
        assertEquals(PomodoroStatus.COMPLETED, after.status)
        assertEquals(PomodoroPhase.SHORT_BREAK, after.phase)
        assertEquals(1, after.completedSessionsInCycle)
    }

    @Test
    fun `fourth completed work session transitions to long break`() = runTest {
        // Complete 3 sessions
        repeat(3) {
            pomodoroService.startSession(phase = PomodoroPhase.WORK, customMinutes = 25)
            pomodoroService.completeSession()
            // Reset to work
            pomodoroService.selectPhase(PomodoroPhase.WORK)
        }

        assertEquals(3, pomodoroService.timerState.value.completedSessionsInCycle)

        // Complete 4th session
        pomodoroService.startSession(phase = PomodoroPhase.WORK, customMinutes = 25)
        pomodoroService.completeSession()

        val finalState = pomodoroService.timerState.value
        assertEquals(PomodoroPhase.LONG_BREAK, finalState.phase)
        assertEquals(0, finalState.completedSessionsInCycle)
    }

    @Test
    fun `reconcileTimerAfterReboot completes expired session when device was off`() = runTest {
        pomodoroService.startSession(phase = PomodoroPhase.WORK, customMinutes = 25)
        val endAlarm = pomodoroService.timerState.value.endAlarmTime

        // Advance clock past the alarm end time (e.g. +30 mins)
        testClock.set(endAlarm + 60_000L)

        pomodoroService.reconcileTimerAfterReboot()

        val state = pomodoroService.timerState.value
        assertEquals(PomodoroStatus.COMPLETED, state.status)
        coVerify { pomodoroRepository.save(any()) }
    }
}
