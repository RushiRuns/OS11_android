package com.os11.app.service

import com.os11.app.data.db.entities.TaskEntity
import com.os11.app.data.repository.TaskRepository
import com.os11.app.service.task.TaskService
import com.os11.app.service.undo.UndoManager
import com.os11.domain.id.DefaultIdGenerator
import com.os11.domain.result.AppError
import com.os11.domain.result.AppResult
import com.os11.domain.time.TestClock
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TaskServiceTest {

    private val taskRepository: TaskRepository = mockk(relaxed = true)
    private val undoManager: UndoManager = UndoManager()
    private val clock = TestClock(1_000_000L)
    private val idGenerator = DefaultIdGenerator()
    private lateinit var taskService: TaskService

    @Before
    fun setup() {
        taskService = TaskService(
            taskRepository = taskRepository,
            undoManager = undoManager,
            clock = clock,
            idGenerator = idGenerator,
            ioDispatcher = Dispatchers.Unconfined,
        )
    }

    @Test
    fun `createTask validates blank title and returns validation error`() = runBlocking {
        val result = taskService.createTask(
            title = "   ",
            workspaceId = "workspace-1",
        )

        assertTrue(result.isErr)
        val error = (result as AppResult.Err).error as AppError.Validation
        assertEquals("Task title cannot be blank", error.reason)
        assertEquals("title: Task title cannot be blank", error.userMessage())
    }

    @Test
    fun `createTask with valid parameters persists task and returns Ok`() = runBlocking {
        coEvery { taskRepository.saveTask(any()) } answers { firstArg() }

        val result = taskService.createTask(
            title = "Buy groceries",
            workspaceId = "workspace-1",
        )

        assertTrue(result.isOk)
        val created = (result as AppResult.Ok).value
        assertEquals("Buy groceries", created.title)
        assertEquals("workspace-1", created.workspaceId)
        coVerify { taskRepository.saveTask(any()) }
    }

    @Test
    fun `setCompleted registers undo action and toggles completion`() = runBlocking {
        val existing = TaskEntity(
            id = "task-1",
            title = "Sample",
            workspaceId = "workspace-1",
            isCompleted = false,
            createdAt = clock.nowIso(),
            updatedAt = clock.nowIso(),
        )
        coEvery { taskRepository.getById("task-1") } returns existing
        coEvery { taskRepository.saveTask(any()) } answers { firstArg() }

        val result = taskService.setCompleted("task-1", true)
        assertTrue(result.isOk)
        val token = (result as AppResult.Ok).value
        assertEquals("Task completed", token.actionDescription)
        assertEquals(1, undoManager.count())

        // Execute undo
        val undoResult = undoManager.undo(token.id)
        assertTrue(undoResult.isOk)
        coVerify(exactly = 2) { taskRepository.saveTask(any()) }
    }

    @Test
    fun `trashTask calls trash and registers undo restoration`() = runBlocking {
        val existing = TaskEntity(
            id = "task-1",
            title = "Sample",
            workspaceId = "workspace-1",
            createdAt = clock.nowIso(),
            updatedAt = clock.nowIso(),
        )
        coEvery { taskRepository.getById("task-1") } returns existing

        val result = taskService.trashTask("task-1")
        assertTrue(result.isOk)
        val token = (result as AppResult.Ok).value
        assertEquals("Task moved to trash", token.actionDescription)

        coVerify { taskRepository.trashTask("task-1") }

        // Execute undo
        undoManager.undo(token.id)
        coVerify { taskRepository.restoreTask("task-1") }
    }
}
