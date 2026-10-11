package com.os11.app.service

import com.os11.app.data.db.entities.TaskEntity
import com.os11.app.data.repository.TagRepository
import com.os11.app.data.repository.TaskRepository
import com.os11.app.service.reminder.ReminderService
import com.os11.app.service.task.TaskSchedulingService
import com.os11.app.service.undo.UndoManager
import com.os11.domain.id.DeterministicIds
import com.os11.domain.id.IdGenerator
import com.os11.domain.time.Clock
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.UUID

class TaskSchedulingServiceTest {

    private val taskRepository: TaskRepository = mockk(relaxed = true)
    private val tagRepository: TagRepository = mockk(relaxed = true)
    private val reminderService: ReminderService = mockk(relaxed = true)
    private val undoManager = UndoManager()
    private val clock: Clock = mockk()
    private val idGenerator: IdGenerator = mockk()

    private lateinit var schedulingService: TaskSchedulingService

    @Before
    fun setup() {
        coEvery { clock.today() } returns "2026-10-11"
        coEvery { clock.nowIso() } returns "2026-10-11T10:00:00.000Z"
        coEvery { idGenerator.randomId() } returns "random-id-123"
        coEvery { idGenerator.deterministicId(any(), any()) } answers {
            DeterministicIds.uuidv5(firstArg<UUID>(), secondArg<String>()).toString()
        }

        schedulingService = TaskSchedulingService(
            taskRepository = taskRepository,
            tagRepository = tagRepository,
            reminderService = reminderService,
            undoManager = undoManager,
            clock = clock,
            ioDispatcher = Dispatchers.Unconfined,
        )
    }

    private fun sampleTask(
        id: String = "task-1",
        bucket: String? = null,
        dueDate: String? = null,
        recurrenceRule: String? = null,
    ) = TaskEntity(
        id = id,
        title = "Test Task",
        workspaceId = "workspace-1",
        bucket = bucket,
        dueDate = dueDate,
        recurrenceRule = recurrenceRule,
        createdAt = "2026-10-10T10:00:00.000Z",
        updatedAt = "2026-10-10T10:00:00.000Z",
    )

    @Test
    fun `scheduleDate clears bucket and sets due date`() = runTest {
        val task = sampleTask(bucket = "anytime")
        coEvery { taskRepository.getById("task-1") } returns task
        val slot = slot<TaskEntity>()
        coEvery { taskRepository.saveTask(capture(slot)) } answers { slot.captured }

        val result = schedulingService.scheduleDate("task-1", "2026-10-15")
        assertTrue(result.isOk)

        val saved = slot.captured
        assertNull(saved.bucket)
        assertEquals("2026-10-15", saved.dueDate)
        coVerify { reminderService.reconcileAlarms() }
    }

    @Test
    fun `moveToBucket clears due date and sets bucket`() = runTest {
        val task = sampleTask(dueDate = "2026-10-15", recurrenceRule = "FREQ=DAILY")
        coEvery { taskRepository.getById("task-1") } returns task
        val slot = slot<TaskEntity>()
        coEvery { taskRepository.saveTask(capture(slot)) } answers { slot.captured }

        val result = schedulingService.moveToBucket("task-1", "someday")
        assertTrue(result.isOk)

        val saved = slot.captured
        assertEquals("someday", saved.bucket)
        assertNull(saved.dueDate)
        assertNull(saved.recurrenceRule)
        coVerify { reminderService.reconcileAlarms() }
    }

    @Test
    fun `clearSchedule clears both bucket and due date`() = runTest {
        val task = sampleTask(dueDate = "2026-10-15")
        coEvery { taskRepository.getById("task-1") } returns task
        val slot = slot<TaskEntity>()
        coEvery { taskRepository.saveTask(capture(slot)) } answers { slot.captured }

        val result = schedulingService.clearSchedule("task-1")
        assertTrue(result.isOk)

        val saved = slot.captured
        assertNull(saved.bucket)
        assertNull(saved.dueDate)
    }

    @Test
    fun `setWaiting sets waiting_on and waiting_since`() = runTest {
        val task = sampleTask()
        coEvery { taskRepository.getById("task-1") } returns task
        val slot = slot<TaskEntity>()
        coEvery { taskRepository.saveTask(capture(slot)) } answers { slot.captured }

        val result = schedulingService.setWaiting("task-1", "Alice", "2026-10-20")
        assertTrue(result.isOk)

        val saved = slot.captured
        assertEquals("Alice", saved.waitingOn)
        assertEquals("2026-10-11", saved.waitingSince)
        assertEquals("2026-10-20", saved.followUpDate)
    }

    @Test
    fun `clearWaiting resets waiting fields`() = runTest {
        val task = sampleTask().copy(
            waitingOn = "Alice",
            waitingSince = "2026-10-01",
            followUpDate = "2026-10-15",
            followUpNotifiedOn = "2026-10-15",
        )
        coEvery { taskRepository.getById("task-1") } returns task
        val slot = slot<TaskEntity>()
        coEvery { taskRepository.saveTask(capture(slot)) } answers { slot.captured }

        val result = schedulingService.clearWaiting("task-1")
        assertTrue(result.isOk)

        val saved = slot.captured
        assertNull(saved.waitingOn)
        assertNull(saved.waitingSince)
        assertNull(saved.followUpDate)
        assertNull(saved.followUpNotifiedOn)
    }

    @Test
    fun `skipOccurrence advances due date for recurring task`() = runTest {
        val task = sampleTask(dueDate = "2026-10-11", recurrenceRule = "FREQ=DAILY;INTERVAL=2")
        coEvery { taskRepository.getById("task-1") } returns task
        val slot = slot<TaskEntity>()
        coEvery { taskRepository.saveTask(capture(slot)) } answers { slot.captured }

        val result = schedulingService.skipOccurrence("task-1")
        assertTrue(result.isOk)
        assertEquals("2026-10-13", result.getOrNull())

        val saved = slot.captured
        assertEquals("2026-10-13", saved.dueDate)
    }

    @Test
    fun `completeRecurringTask marks current completed and spawns next with deterministic ID`() = runTest {
        val task = sampleTask(id = "task-rec", dueDate = "2026-10-11", recurrenceRule = "FREQ=DAILY")
        coEvery { taskRepository.getById("task-rec") } returns task
        val savedTasks = mutableListOf<TaskEntity>()
        coEvery { taskRepository.saveTask(capture(savedTasks)) } answers { firstArg() }
        coEvery { tagRepository.getTagsForTasks(listOf("task-rec")) } returns emptyList()

        val result = schedulingService.completeRecurringTask("task-rec")
        assertTrue(result.isOk)

        assertEquals(2, savedTasks.size)
        val completedTask = savedTasks[0]
        val spawnedNext = savedTasks[1]

        assertTrue(completedTask.isCompleted)
        assertNotNull(completedTask.completedAt)

        assertEquals("2026-10-12", spawnedNext.dueDate)
        val expectedDeterministicId = DeterministicIds.recurringOccurrenceId("task-rec", "2026-10-12")
        assertEquals(expectedDeterministicId, spawnedNext.id)
        assertTrue(!spawnedNext.isCompleted)
        assertNull(spawnedNext.completedAt)
    }
}
