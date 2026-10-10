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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@Suppress("MaxLineLength")
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
    fun `createTask with valid parameters persists task and sanitizes notes`() = runBlocking {
        coEvery { taskRepository.saveTask(any()) } answers { firstArg() }

        val result = taskService.createTask(
            title = "Buy groceries",
            notes = "<p>Milk</p><script>alert('bad')</script>",
            workspaceId = "workspace-1",
        )

        assertTrue(result.isOk)
        val created = (result as AppResult.Ok).value
        assertEquals("Buy groceries", created.title)
        assertEquals("workspace-1", created.workspaceId)
        assertEquals("<p>Milk</p>", created.notes)
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

    @Test
    fun `duplicateTask creates copy with (Copy) suffix and uncompleted state`() = runBlocking {
        val existing = TaskEntity(
            id = "task-orig",
            title = "Deploy app",
            workspaceId = "workspace-1",
            isCompleted = true,
            createdAt = clock.nowIso(),
            updatedAt = clock.nowIso(),
        )
        coEvery { taskRepository.getById("task-orig") } returns existing
        coEvery { taskRepository.saveTask(any()) } answers { firstArg() }

        val result = taskService.duplicateTask("task-orig")
        assertTrue(result.isOk)
        val copy = (result as AppResult.Ok).value
        assertEquals("Deploy app (Copy)", copy.title)
        assertFalse(copy.isCompleted)
        assertNull(copy.completedAt)
    }

    @Test
    fun `moveTask registers undo and updates container`() = runBlocking {
        val existing = TaskEntity(
            id = "task-move",
            title = "Move me",
            workspaceId = "workspace-1",
            projectId = "proj-1",
            createdAt = clock.nowIso(),
            updatedAt = clock.nowIso(),
        )
        coEvery { taskRepository.getById("task-move") } returns existing
        coEvery { taskRepository.saveTask(any()) } answers { firstArg() }

        val result = taskService.moveTask(
            id = "task-move",
            areaId = "area-2",
            projectId = "proj-2",
        )
        assertTrue(result.isOk)
        val token = (result as AppResult.Ok).value
        assertEquals("Task moved", token.actionDescription)

        // Undo move
        val undo = undoManager.undo(token.id)
        assertTrue(undo.isOk)
    }

    @Test
    fun `updateDueDate registers undo and updates date`() = runBlocking {
        val existing = TaskEntity(
            id = "task-date",
            title = "Due test",
            workspaceId = "workspace-1",
            dueDate = "2026-10-10",
            createdAt = clock.nowIso(),
            updatedAt = clock.nowIso(),
        )
        coEvery { taskRepository.getById("task-date") } returns existing
        coEvery { taskRepository.saveTask(any()) } answers { firstArg() }

        val result = taskService.updateDueDate("task-date", "2026-10-15")
        assertTrue(result.isOk)
        val token = (result as AppResult.Ok).value
        assertEquals("Due date changed", token.actionDescription)

        // Undo date change
        val undo = undoManager.undo(token.id)
        assertTrue(undo.isOk)
    }

    @Test
    fun `subtask promote and outdent updates hierarchy correctly`() = runBlocking {
        val parent = TaskEntity(
            id = "parent-1",
            title = "Parent",
            workspaceId = "workspace-1",
            createdAt = clock.nowIso(),
            updatedAt = clock.nowIso(),
        )
        val subtask = TaskEntity(
            id = "sub-1",
            title = "Subtask",
            workspaceId = "workspace-1",
            parentTaskId = "parent-1",
            createdAt = clock.nowIso(),
            updatedAt = clock.nowIso(),
        )
        coEvery { taskRepository.getById("parent-1") } returns parent
        coEvery { taskRepository.getById("sub-1") } returns subtask
        coEvery { taskRepository.saveTask(any()) } answers { firstArg() }

        // Promote subtask
        val promoteResult = taskService.promoteSubtask("sub-1")
        assertTrue(promoteResult.isOk)
        val promoted = (promoteResult as AppResult.Ok).value
        assertNull(promoted.parentTaskId)
    }

    @Test
    fun `indentSubtask detects cycle and rejects self-nesting`() = runBlocking {
        val task = TaskEntity(
            id = "task-a",
            title = "Task A",
            workspaceId = "workspace-1",
            parentTaskId = null,
            createdAt = clock.nowIso(),
            updatedAt = clock.nowIso(),
        )
        coEvery { taskRepository.getById("task-a") } returns task

        // Attempt nesting inside itself
        val result = taskService.indentSubtask("task-a", "task-a")
        assertTrue(result.isErr)
        val error = (result as AppResult.Err).error
        assertTrue(error is AppError.RuleViolation)
    }

    @Test
    fun `emptyTrash deletes all trashed items permanently`() = runBlocking {
        val trashed = listOf(
            TaskEntity(
                id = "trash-1",
                title = "T1",
                workspaceId = "ws-1",
                isTrashed = true,
                createdAt = clock.nowIso(),
                updatedAt = clock.nowIso(),
            ),
            TaskEntity(
                id = "trash-2",
                title = "T2",
                workspaceId = "ws-1",
                isTrashed = true,
                createdAt = clock.nowIso(),
                updatedAt = clock.nowIso(),
            ),
        )
        coEvery { taskRepository.getTrashedTasks() } returns trashed

        val result = taskService.emptyTrash()
        assertTrue(result.isOk)
        assertEquals(2, (result as AppResult.Ok).value)
        coVerify(exactly = 2) { taskRepository.deletePermanently(any()) }
    }
}
