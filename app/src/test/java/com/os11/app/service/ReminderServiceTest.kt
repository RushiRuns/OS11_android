package com.os11.app.service

import com.os11.app.data.db.entities.ReminderEntity
import com.os11.app.data.repository.ReminderRepository
import com.os11.app.data.sync.TrackedWriter
import com.os11.app.platform.alarms.AlarmScheduler
import com.os11.app.service.reminder.ReminderService
import com.os11.domain.time.Clock
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

class ReminderServiceTest {

    private val reminderRepository: ReminderRepository = mockk(relaxed = true)
    private val alarmScheduler: AlarmScheduler = mockk(relaxed = true)
    private val trackedWriter: TrackedWriter = mockk(relaxed = true)
    private val clock: Clock = mockk()
    private val idGenerator: com.os11.domain.id.IdGenerator = mockk()

    private lateinit var reminderService: ReminderService

    @Before
    fun setup() {
        coEvery { clock.nowIso() } returns "2026-10-11T10:00:00.000Z"
        coEvery { idGenerator.randomId() } returns "reminder-id-123"

        reminderService = ReminderService(
            reminderRepository = reminderRepository,
            alarmScheduler = alarmScheduler,
            trackedWriter = trackedWriter,
            clock = clock,
            idGenerator = idGenerator,
            ioDispatcher = Dispatchers.Unconfined,
        )
    }

    @Test
    fun `reconcileAlarms arms only the earliest pending reminder`() = runTest {
        val r1 = ReminderEntity(
            id = "r-later",
            taskId = "t-1",
            remindAt = "2026-10-11T12:00:00.000Z",
            createdAt = "2026-10-10T10:00:00.000Z",
        )
        val r2 = ReminderEntity(
            id = "r-earliest",
            taskId = "t-2",
            remindAt = "2026-10-11T10:30:00.000Z",
            createdAt = "2026-10-10T10:00:00.000Z",
        )

        coEvery { reminderRepository.getPendingReminders("2026-10-11T10:00:00.000Z") } returns listOf(r1, r2)
        coEvery { reminderRepository.getDeviceState(any()) } returns null

        reminderService.reconcileAlarms()

        coVerify {
            alarmScheduler.scheduleExactAlarm(
                triggerAtMillis = Instant.parse("2026-10-11T10:30:00.000Z").toEpochMilli(),
                reminderId = "r-earliest",
            )
        }
    }

    @Test
    fun `createReminder saves reminder and reconciles alarms`() = runTest {
        val reminder = ReminderEntity(
            id = "reminder-id-123",
            taskId = "task-1",
            remindAt = "2026-10-11T11:00:00.000Z",
            createdAt = "2026-10-11T10:00:00.000Z",
        )
        coEvery { reminderRepository.save(any()) } returns reminder
        coEvery { reminderRepository.getPendingReminders(any()) } returns listOf(reminder)

        val result = reminderService.createReminder("task-1", "2026-10-11T11:00:00.000Z")

        assertTrue(result.isOk)
        assertEquals("reminder-id-123", result.getOrNull()?.id)
        coVerify { reminderRepository.save(any()) }
    }

    @Test
    fun `deleteReminder deletes from repository and cancels alarm`() = runTest {
        coEvery { reminderRepository.getPendingReminders(any()) } returns emptyList()

        val result = reminderService.deleteReminder("reminder-1")

        assertTrue(result.isOk)
        coVerify { reminderRepository.delete("reminder-1") }
        coVerify { alarmScheduler.cancelAlarm("reminder-1") }
    }

    @Test
    fun `snoozeReminder sets snoozedUntil and triggers reconciliation`() = runTest {
        coEvery { reminderRepository.getPendingReminders(any()) } returns emptyList()

        val result = reminderService.snoozeReminder("reminder-1", 15)

        assertTrue(result.isOk)
        coVerify { reminderRepository.snoozeReminder("reminder-1", any()) }
    }

    @Test
    fun `markReminderTriggered marks triggered and reconciles`() = runTest {
        coEvery { reminderRepository.getPendingReminders(any()) } returns emptyList()

        val result = reminderService.markReminderTriggered("reminder-1")

        assertTrue(result.isOk)
        coVerify { reminderRepository.markTriggered("reminder-1") }
    }
}
