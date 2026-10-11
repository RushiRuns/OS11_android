package com.os11.app.service

import com.os11.app.data.db.entities.TaskEntity
import com.os11.app.data.repository.CountsRepository
import com.os11.app.data.repository.TaskRepository
import com.os11.app.service.task.TaskItem
import com.os11.app.service.task.TaskService
import com.os11.app.service.undo.UndoManager
import com.os11.app.ui.features.dayplan.DayPlanBlock
import com.os11.app.ui.features.dayplan.DayPlanLaneCalculator
import com.os11.domain.id.IdGenerator
import com.os11.domain.result.AppResult
import com.os11.domain.time.Clock
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DayPlanSchedulingTest {

    private val taskRepository: TaskRepository = mockk(relaxed = true)
    private val countsRepository: CountsRepository = mockk(relaxed = true)
    private val undoManager = UndoManager()
    private val clock: Clock = mockk()
    private val idGenerator: IdGenerator = mockk()

    private lateinit var taskService: TaskService

    @Before
    fun setup() {
        coEvery { clock.today() } returns "2026-10-11"
        coEvery { clock.nowIso() } returns "2026-10-11T10:00:00.000Z"
        coEvery { idGenerator.randomId() } returns "rand-1"

        taskService = TaskService(
            taskRepository = taskRepository,
            countsRepository = countsRepository,
            undoManager = undoManager,
            clock = clock,
            idGenerator = idGenerator,
            ioDispatcher = Dispatchers.Unconfined,
        )
    }

    private fun sampleTask(
        id: String = "task-1",
        title: String = "Test Task",
        myDayDate: String? = "2026-10-11",
        startMin: Int? = null,
        durationMin: Int? = null,
    ) = TaskEntity(
        id = id,
        title = title,
        workspaceId = "ws-1",
        myDayDate = myDayDate,
        scheduledStartMin = startMin,
        scheduledDurationMin = durationMin,
        createdAt = "2026-10-11T08:00:00.000Z",
        updatedAt = "2026-10-11T08:00:00.000Z",
    )

    @Test
    fun `scheduleTimeBlock writes both start and duration together`() = runTest {
        val task = sampleTask()
        coEvery { taskRepository.getById("task-1") } returns task

        val captured = slot<TaskEntity>()
        coEvery { taskRepository.saveTask(capture(captured)) } answers { captured.captured }

        val result = taskService.scheduleTimeBlock("task-1", startMin = 540, durationMin = 45)

        assertTrue(result is AppResult.Ok)
        assertEquals(540, captured.captured.scheduledStartMin)
        assertEquals(45, captured.captured.scheduledDurationMin)
    }

    @Test
    fun `scheduleTimeBlock rejects duration outside 15 to 480 minutes`() = runTest {
        val task = sampleTask()
        coEvery { taskRepository.getById("task-1") } returns task

        // Less than 15 min
        val resultTooShort = taskService.scheduleTimeBlock("task-1", startMin = 540, durationMin = 10)
        assertTrue(resultTooShort is AppResult.Err)

        // Greater than 480 min
        val resultTooLong = taskService.scheduleTimeBlock("task-1", startMin = 540, durationMin = 500)
        assertTrue(resultTooLong is AppResult.Err)
    }

    @Test
    fun `removeScheduledTimeBlock clears both start and duration`() = runTest {
        val task = sampleTask(startMin = 540, durationMin = 60)
        coEvery { taskRepository.getById("task-1") } returns task

        val captured = slot<TaskEntity>()
        coEvery { taskRepository.saveTask(capture(captured)) } answers { captured.captured }

        val result = taskService.removeScheduledTimeBlock("task-1")

        assertTrue(result is AppResult.Ok)
        assertNull(captured.captured.scheduledStartMin)
        assertNull(captured.captured.scheduledDurationMin)
    }

    @Test
    fun `removeFromMyDay clears myDayDate and both scheduled fields`() = runTest {
        val task = sampleTask(startMin = 540, durationMin = 60)
        coEvery { taskRepository.getById("task-1") } returns task

        val captured = slot<TaskEntity>()
        coEvery { taskRepository.saveTask(capture(captured)) } answers { captured.captured }

        val result = taskService.removeFromMyDay("task-1")

        assertTrue(result is AppResult.Ok)
        assertNull(captured.captured.myDayDate)
        assertNull(captured.captured.scheduledStartMin)
        assertNull(captured.captured.scheduledDurationMin)
    }

    @Test
    fun `performRollover clears both scheduled fields`() = runTest {
        val task = sampleTask(startMin = 540, durationMin = 60)
        coEvery { taskRepository.getById("task-1") } returns task

        val captured = slot<TaskEntity>()
        coEvery { taskRepository.saveTask(capture(captured)) } answers { captured.captured }

        val result = taskService.performRollover(listOf("task-1"), targetDate = "2026-10-12")

        assertTrue(result is AppResult.Ok)
        assertEquals("2026-10-12", captured.captured.myDayDate)
        assertNull(captured.captured.scheduledStartMin)
        assertNull(captured.captured.scheduledDurationMin)
    }

    @Test
    fun `DayPlanLaneCalculator assigns non-overlapping blocks to single lane`() {
        val taskA = TaskItem(id = "a", title = "Task A", workspaceId = "ws-1", createdAt = "", updatedAt = "")
        val taskB = TaskItem(id = "b", title = "Task B", workspaceId = "ws-1", createdAt = "", updatedAt = "")

        val blockA = DayPlanBlock(task = taskA, startMin = 540, durationMin = 60) // 09:00 - 10:00
        val blockB = DayPlanBlock(task = taskB, startMin = 600, durationMin = 60) // 10:00 - 11:00

        val layout = DayPlanLaneCalculator.computeLanes(listOf(blockA, blockB))

        assertEquals(2, layout.size)
        assertEquals(0, layout[0].laneIndex)
        assertEquals(1, layout[0].totalLanes)
        assertEquals(0, layout[1].laneIndex)
        assertEquals(1, layout[1].totalLanes)
    }

    @Test
    fun `DayPlanLaneCalculator assigns overlapping blocks to separate lanes`() {
        val taskA = TaskItem(id = "a", title = "Task A", workspaceId = "ws-1", createdAt = "", updatedAt = "")
        val taskB = TaskItem(id = "b", title = "Task B", workspaceId = "ws-1", createdAt = "", updatedAt = "")

        val blockA = DayPlanBlock(task = taskA, startMin = 540, durationMin = 60) // 09:00 - 10:00
        val blockB = DayPlanBlock(task = taskB, startMin = 570, durationMin = 60) // 09:30 - 10:30

        val layout = DayPlanLaneCalculator.computeLanes(listOf(blockA, blockB))

        assertEquals(2, layout.size)
        assertEquals(0, layout[0].laneIndex)
        assertEquals(2, layout[0].totalLanes)
        assertEquals(1, layout[1].laneIndex)
        assertEquals(2, layout[1].totalLanes)
    }
}
