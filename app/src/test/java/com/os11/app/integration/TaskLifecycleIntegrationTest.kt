package com.os11.app.integration

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.os11.app.data.db.DatabaseInitializer
import com.os11.app.data.db.OS11Database
import com.os11.app.data.repository.CountsRepositoryImpl
import com.os11.app.data.repository.TagRepository
import com.os11.app.data.repository.TaskRepositoryImpl
import com.os11.app.data.sync.TrackedWriter
import com.os11.app.service.reminder.ReminderService
import com.os11.app.service.task.TaskSchedulingService
import com.os11.app.service.task.TaskService
import com.os11.app.service.undo.UndoManager
import com.os11.domain.id.DefaultIdGenerator
import com.os11.domain.id.DeterministicIds
import com.os11.domain.logic.sync.DefaultHlcClock
import com.os11.domain.time.TestClock
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TaskLifecycleIntegrationTest {

    private lateinit var db: OS11Database
    private lateinit var testClock: TestClock
    private lateinit var trackedWriter: TrackedWriter
    private lateinit var taskRepository: TaskRepositoryImpl
    private lateinit var countsRepository: CountsRepositoryImpl
    private lateinit var undoManager: UndoManager
    private lateinit var taskService: TaskService
    private lateinit var schedulingService: TaskSchedulingService

    @Before
    fun setup() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = OS11Database.buildInMemory(context)
        testClock = TestClock(1_000_000L)
        val hlcClock = DefaultHlcClock(testClock, "lifecycle-test-device")
        val idGenerator = DefaultIdGenerator()
        trackedWriter = TrackedWriter(db, hlcClock, testClock, idGenerator)
        DatabaseInitializer.seedDefaults(db, testClock)

        taskRepository = TaskRepositoryImpl(db, trackedWriter, testClock)
        countsRepository = CountsRepositoryImpl(db)
        undoManager = UndoManager()

        taskService = TaskService(
            taskRepository = taskRepository,
            countsRepository = countsRepository,
            undoManager = undoManager,
            clock = testClock,
            idGenerator = idGenerator,
            ioDispatcher = Dispatchers.Unconfined,
        )

        val tagRepository = mockk<TagRepository>(relaxed = true)
        val reminderService = mockk<ReminderService>(relaxed = true)
        schedulingService = TaskSchedulingService(
            taskRepository = taskRepository,
            tagRepository = tagRepository,
            reminderService = reminderService,
            undoManager = undoManager,
            clock = testClock,
            ioDispatcher = Dispatchers.Unconfined,
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun test_complete_task_lifecycle_create_schedule_complete_undo_trash_restore_delete() = runBlocking {
        val workspaceId = DeterministicIds.defaultWorkspaceId()

        // 1. Create task in Inbox
        val createResult = taskService.createTask(
            title = "Integration test release task",
            workspaceId = workspaceId,
            notes = "<p>Task notes</p>",
            priority = 2,
        )
        assertTrue(createResult.isOk)
        val taskId = createResult.getOrThrow().id

        var task = taskRepository.getById(taskId)
        assertNotNull(task)
        assertEquals("Integration test release task", task?.title)
        assertFalse(task?.isCompleted == true)
        assertFalse(task?.isTrashed == true)

        // Verify outbox entry exists for create
        val outboxAfterCreate = db.syncQueueDao().getNextBatch(100)
        assertTrue(outboxAfterCreate.any { it.entityId == taskId && it.operation == "upsert" })

        // 2. Schedule task (set due date & bucket)
        val scheduleResult = schedulingService.scheduleDate(taskId, "2026-10-15", "10:00")
        assertTrue(scheduleResult.isOk)
        task = taskRepository.getById(taskId)
        assertEquals("2026-10-15", task?.dueDate)
        assertEquals("10:00", task?.dueTime)
        assertNull(task?.bucket)

        // Move to bucket
        val bucketResult = schedulingService.moveToBucket(taskId, "anytime")
        assertTrue(bucketResult.isOk)
        task = taskRepository.getById(taskId)
        assertEquals("anytime", task?.bucket)
        assertNull(task?.dueDate)

        // 3. Complete task with undo token
        val completeResult = taskService.bulkComplete(listOf(taskId), true)
        assertTrue(completeResult.isOk)
        task = taskRepository.getById(taskId)
        assertTrue(task?.isCompleted == true)
        assertNotNull(task?.completedAt)

        // 4. Undo completion
        val undoResult = undoManager.undoLatest()
        assertTrue(undoResult.isOk)
        task = taskRepository.getById(taskId)
        assertFalse(task?.isCompleted == true)
        assertNull(task?.completedAt)

        // 5. Move to trash
        val trashResult = taskService.trashTask(taskId)
        assertTrue(trashResult.isOk)
        task = taskRepository.getById(taskId)
        assertTrue(task?.isTrashed == true)
        assertNotNull(task?.trashedAt)

        // Active inbox query does not return trashed task
        val inboxTasks = taskService.observeInboxTasks().first()
        assertFalse(inboxTasks.any { it.id == taskId })

        // 6. Restore task from trash
        val restoreResult = taskService.restoreTask(taskId)
        assertTrue(restoreResult.isOk)
        task = taskRepository.getById(taskId)
        assertFalse(task?.isTrashed == true)
        assertNull(task?.trashedAt)

        // 7. Permanently delete
        val deleteResult = taskService.deletePermanently(taskId)
        assertTrue(deleteResult.isOk)
        task = taskRepository.getById(taskId)
        assertNull(task)

        // Verify tombstone in sync outbox
        val outboxFinal = db.syncQueueDao().getNextBatch(100)
        assertTrue(outboxFinal.any { it.entityId == taskId && it.operation == "delete" })
    }
}
