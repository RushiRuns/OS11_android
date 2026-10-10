package com.os11.app.service

import com.os11.app.data.db.entities.TaskEntity
import com.os11.app.data.repository.CountsRepository
import com.os11.app.data.repository.TaskRepository
import com.os11.app.service.task.TaskService
import com.os11.app.service.undo.UndoManager
import com.os11.domain.id.DefaultIdGenerator
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

class TaskServiceBulkAndReorderTest {

    private val taskRepository: TaskRepository = mockk(relaxed = true)
    private val countsRepository: CountsRepository = mockk(relaxed = true)
    private val undoManager: UndoManager = UndoManager()
    private val clock = TestClock(2_000_000L)
    private val idGenerator = DefaultIdGenerator()
    private lateinit var taskService: TaskService

    private val task1 = TaskEntity(
        id = "task-1",
        title = "First task",
        workspaceId = "workspace-1",
        sortOrder = 1.0,
        priority = 1,
        isCompleted = false,
        createdAt = "2026-10-10T00:00:00Z",
        updatedAt = "2026-10-10T00:00:00Z",
    )

    private val task2 = TaskEntity(
        id = "task-2",
        title = "Second task",
        workspaceId = "workspace-1",
        sortOrder = 2.0,
        priority = 2,
        isCompleted = false,
        createdAt = "2026-10-10T00:00:00Z",
        updatedAt = "2026-10-10T00:00:00Z",
    )

    @Before
    fun setup() {
        taskService = TaskService(
            taskRepository = taskRepository,
            countsRepository = countsRepository,
            undoManager = undoManager,
            clock = clock,
            idGenerator = idGenerator,
            ioDispatcher = Dispatchers.Unconfined,
        )

        coEvery { taskRepository.getById("task-1") } returns task1
        coEvery { taskRepository.getById("task-2") } returns task2
        coEvery { taskRepository.saveTask(any()) } answers { firstArg() }
    }

    @Test
    fun `reorderTask updates sort_order for specified task`() = runBlocking {
        val result = taskService.reorderTask("task-1", 3.5)

        assertTrue(result is AppResult.Ok)
        coVerify {
            taskRepository.saveTask(
                match { it.id == "task-1" && it.sortOrder == 3.5 },
            )
        }
    }

    @Test
    fun `bulkComplete updates isCompleted for all tasks and can undo`() = runBlocking {
        val result = taskService.bulkComplete(listOf("task-1", "task-2"), true)

        assertTrue(result is AppResult.Ok)
        val token = (result as AppResult.Ok).value
        assertEquals("2 tasks completed", token.actionDescription)

        coVerify {
            taskRepository.saveTask(match { it.id == "task-1" && it.isCompleted })
        }
        coVerify {
            taskRepository.saveTask(match { it.id == "task-2" && it.isCompleted })
        }

        // Execute Undo
        val undoResult = undoManager.undo(token.id)
        assertTrue(undoResult is AppResult.Ok)

        coVerify {
            taskRepository.saveTask(match { it.id == "task-1" && !it.isCompleted })
        }
        coVerify {
            taskRepository.saveTask(match { it.id == "task-2" && !it.isCompleted })
        }
    }

    @Test
    fun `bulkTrash trashes all specified tasks and registers undo`() = runBlocking {
        val result = taskService.bulkTrash(listOf("task-1", "task-2"))

        assertTrue(result is AppResult.Ok)
        val token = (result as AppResult.Ok).value
        assertEquals("2 tasks moved to trash", token.actionDescription)

        coVerify { taskRepository.trashTask("task-1") }
        coVerify { taskRepository.trashTask("task-2") }

        // Execute Undo
        val undoResult = undoManager.undo(token.id)
        assertTrue(undoResult is AppResult.Ok)

        coVerify { taskRepository.restoreTask("task-1") }
        coVerify { taskRepository.restoreTask("task-2") }
    }

    @Test
    fun `bulkMove updates area and project for all tasks and can undo`() = runBlocking {
        val result = taskService.bulkMove(
            taskIds = listOf("task-1", "task-2"),
            areaId = "area-new",
            projectId = "project-new",
        )

        assertTrue(result is AppResult.Ok)
        val token = (result as AppResult.Ok).value
        coVerify {
            taskRepository.saveTask(
                match { it.id == "task-1" && it.areaId == "area-new" && it.projectId == "project-new" },
            )
        }

        // Execute Undo
        val undoResult = undoManager.undo(token.id)
        assertTrue(undoResult is AppResult.Ok)

        coVerify {
            taskRepository.saveTask(
                match { it.id == "task-1" && it.areaId == null && it.projectId == null },
            )
        }
    }

    @Test
    fun `bulkSetDueDate updates due dates for all tasks and can undo`() = runBlocking {
        val result = taskService.bulkSetDueDate(
            taskIds = listOf("task-1", "task-2"),
            dueDate = "2026-10-15",
            dueTime = "14:00",
        )

        assertTrue(result is AppResult.Ok)
        val token = (result as AppResult.Ok).value
        coVerify {
            taskRepository.saveTask(
                match { it.id == "task-1" && it.dueDate == "2026-10-15" && it.dueTime == "14:00" },
            )
        }

        // Execute Undo
        val undoResult = undoManager.undo(token.id)
        assertTrue(undoResult is AppResult.Ok)

        coVerify {
            taskRepository.saveTask(
                match { it.id == "task-1" && it.dueDate == null && it.dueTime == null },
            )
        }
    }

    @Test
    fun `bulkSetPriority updates priority for all tasks and can undo`() = runBlocking {
        val result = taskService.bulkSetPriority(
            taskIds = listOf("task-1", "task-2"),
            priority = 4,
        )

        assertTrue(result is AppResult.Ok)
        val token = (result as AppResult.Ok).value
        coVerify {
            taskRepository.saveTask(
                match { it.id == "task-1" && it.priority == 4 },
            )
        }

        // Execute Undo
        val undoResult = undoManager.undo(token.id)
        assertTrue(undoResult is AppResult.Ok)

        coVerify {
            taskRepository.saveTask(
                match { it.id == "task-1" && it.priority == 1 },
            )
        }
    }
}
