package com.os11.app.integration

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.os11.app.data.db.DatabaseInitializer
import com.os11.app.data.db.OS11Database
import com.os11.app.data.repository.CountsRepositoryImpl
import com.os11.app.data.repository.TaskRepositoryImpl
import com.os11.app.data.sync.TrackedWriter
import com.os11.app.service.task.TaskService
import com.os11.app.service.undo.UndoManager
import com.os11.domain.id.DefaultIdGenerator
import com.os11.domain.id.DeterministicIds
import com.os11.domain.logic.sync.DefaultHlcClock
import com.os11.domain.time.TestClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SubtaskHierarchyIntegrationTest {

    private lateinit var db: OS11Database
    private lateinit var testClock: TestClock
    private lateinit var trackedWriter: TrackedWriter
    private lateinit var taskRepository: TaskRepositoryImpl
    private lateinit var taskService: TaskService
    private lateinit var undoManager: UndoManager

    @Before
    fun setup() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = OS11Database.buildInMemory(context)
        testClock = TestClock(1_000_000L)
        val hlcClock = DefaultHlcClock(testClock, "subtask-hierarchy-test")
        val idGenerator = DefaultIdGenerator()
        trackedWriter = TrackedWriter(db, hlcClock, testClock, idGenerator)
        DatabaseInitializer.seedDefaults(db, testClock)

        taskRepository = TaskRepositoryImpl(db, trackedWriter, testClock)
        undoManager = UndoManager()

        taskService = TaskService(
            taskRepository = taskRepository,
            countsRepository = CountsRepositoryImpl(db),
            undoManager = undoManager,
            clock = testClock,
            idGenerator = idGenerator,
            ioDispatcher = Dispatchers.Unconfined,
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun test_subtask_creation_nesting_and_promotion_to_root() = runBlocking {
        val workspaceId = DeterministicIds.defaultWorkspaceId()

        // 1. Create root task
        val rootTask = taskService.createTask("Root Task", workspaceId).getOrThrow()
        assertNull(rootTask.parentTaskId)

        // 2. Create subtask
        val subtask = taskService.addSubtask(
            parentId = rootTask.id,
            title = "Subtask 1",
        ).getOrThrow()
        assertEquals(rootTask.id, subtask.parentTaskId)

        // Verify subtask count on root
        val subtasks = taskService.observeSubtasks(rootTask.id).first()
        assertEquals(1, subtasks.size)
        assertEquals(subtask.id, subtasks[0].id)

        // 3. Promote subtask to root
        val promoteResult = taskService.promoteSubtask(subtask.id)
        assertTrue(promoteResult.isOk)

        val promotedTask = taskRepository.getById(subtask.id)
        assertNotNull(promotedTask)
        assertNull(promotedTask?.parentTaskId)

        // Root task no longer has subtasks
        val subtasksAfterPromote = taskService.observeSubtasks(rootTask.id).first()
        assertTrue(subtasksAfterPromote.isEmpty())

        // Both tasks appear as roots in Inbox
        val inboxTasks = taskService.observeInboxTasks().first()
        assertTrue(inboxTasks.any { it.id == rootTask.id })
        assertTrue(inboxTasks.any { it.id == subtask.id })
    }

    @Test
    fun test_subtask_indent_and_outdent() = runBlocking {
        val workspaceId = DeterministicIds.defaultWorkspaceId()

        val taskA = taskService.createTask("Task A", workspaceId).getOrThrow()
        val taskB = taskService.createTask("Task B", workspaceId).getOrThrow()

        // Indent taskB under taskA
        val indentResult = taskService.indentSubtask(taskB.id, taskA.id)
        assertTrue(indentResult.isOk)

        var loadedB = taskRepository.getById(taskB.id)
        assertEquals(taskA.id, loadedB?.parentTaskId)

        // Outdent taskB back to root
        val outdentResult = taskService.outdentSubtask(taskB.id)
        assertTrue(outdentResult.isOk)

        loadedB = taskRepository.getById(taskB.id)
        assertNull(loadedB?.parentTaskId)
    }

    @Test
    fun test_cycle_prevention_rejects_parenting_to_descendant() = runBlocking {
        val workspaceId = DeterministicIds.defaultWorkspaceId()

        val parent = taskService.createTask("Parent", workspaceId).getOrThrow()
        val child = taskService.addSubtask(parent.id, "Child").getOrThrow()
        val grandchild = taskService.addSubtask(child.id, "Grandchild").getOrThrow()

        // Attempting to make parent a child of grandchild must fail
        val cycleResult = taskService.indentSubtask(subtaskId = parent.id, newParentId = grandchild.id)
        assertTrue(cycleResult.isErr)

        // Verify parent hierarchy remained unchanged
        val loadedParent = taskRepository.getById(parent.id)
        assertNull(loadedParent?.parentTaskId)
    }
}
