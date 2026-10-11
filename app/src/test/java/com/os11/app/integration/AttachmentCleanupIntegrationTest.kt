package com.os11.app.integration

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.os11.app.data.db.DatabaseInitializer
import com.os11.app.data.db.OS11Database
import com.os11.app.data.db.entities.AttachmentEntity
import com.os11.app.data.files.AttachmentStorageManager
import com.os11.app.data.repository.AttachmentRepositoryImpl
import com.os11.app.data.repository.CountsRepositoryImpl
import com.os11.app.data.repository.TaskRepositoryImpl
import com.os11.app.data.sync.TrackedWriter
import com.os11.app.service.attachment.AttachmentService
import com.os11.app.service.task.TaskService
import com.os11.app.service.undo.UndoManager
import com.os11.domain.id.DefaultIdGenerator
import com.os11.domain.id.DeterministicIds
import com.os11.domain.logic.sync.DefaultHlcClock
import com.os11.domain.time.TestClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class AttachmentCleanupIntegrationTest {

    private lateinit var db: OS11Database
    private lateinit var testClock: TestClock
    private lateinit var trackedWriter: TrackedWriter
    private lateinit var taskRepository: TaskRepositoryImpl
    private lateinit var attachmentRepository: AttachmentRepositoryImpl
    private lateinit var taskService: TaskService
    private lateinit var attachmentService: AttachmentService
    private lateinit var storageManager: AttachmentStorageManager
    private lateinit var undoManager: UndoManager

    @Before
    fun setup() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = OS11Database.buildInMemory(context)
        testClock = TestClock(1_000_000L)
        val hlcClock = DefaultHlcClock(testClock, "attachment-cleanup-test")
        val idGenerator = DefaultIdGenerator()
        trackedWriter = TrackedWriter(db, hlcClock, testClock, idGenerator)
        DatabaseInitializer.seedDefaults(db, testClock)

        taskRepository = TaskRepositoryImpl(db, trackedWriter, testClock)
        attachmentRepository = AttachmentRepositoryImpl(db, trackedWriter)
        undoManager = UndoManager()

        storageManager = AttachmentStorageManager(context, Dispatchers.Unconfined)
        storageManager.getAttachmentsBaseDir().deleteRecursively()

        attachmentService = AttachmentService(
            attachmentRepository = attachmentRepository,
            storageManager = storageManager,
            clock = testClock,
            idGenerator = idGenerator,
            ioDispatcher = Dispatchers.Unconfined,
        )

        taskService = TaskService(
            taskRepository = taskRepository,
            countsRepository = CountsRepositoryImpl(db),
            undoManager = undoManager,
            clock = testClock,
            idGenerator = idGenerator,
            ioDispatcher = Dispatchers.Unconfined,
            attachmentStorageManager = storageManager,
        )
    }

    @After
    fun tearDown() {
        storageManager.getAttachmentsBaseDir().deleteRecursively()
        db.close()
    }

    @Test
    fun test_permanent_task_deletion_purges_task_attachment_directory() = runBlocking {
        val workspaceId = DeterministicIds.defaultWorkspaceId()
        val task = taskService.createTask("Task with attachments", workspaceId).getOrThrow()

        // Create physical file in attachment directory
        val taskDir = storageManager.getTaskDir(task.id)
        taskDir.mkdirs()
        val attachedFile = File(taskDir, "sample-doc.pdf")
        attachedFile.writeText("PDF sample content for cleanup test")
        assertTrue(attachedFile.exists())

        // Save DB record for attachment
        attachmentRepository.save(
            AttachmentEntity(
                id = "att-1",
                taskId = task.id,
                filename = "sample-doc.pdf",
                originalName = "sample-doc.pdf",
                sizeBytes = attachedFile.length(),
                mimeType = "application/pdf",
                blobState = "present",
                createdAt = testClock.nowIso(),
            ),
        )

        // Delete task permanently
        val deleteResult = taskService.deletePermanently(task.id)
        assertTrue(deleteResult.isOk)

        // Verify task row deleted
        assertNull(taskRepository.getById(task.id))

        // Verify attachment DB row deleted
        assertNull(attachmentRepository.getById("att-1"))

        // Verify task directory and physical file purged
        assertFalse(attachedFile.exists())
        assertFalse(taskDir.exists())
    }

    @Test
    fun test_empty_trash_purges_all_trashed_task_attachment_directories() = runBlocking {
        val workspaceId = DeterministicIds.defaultWorkspaceId()
        val task = taskService.createTask("Trashed Task", workspaceId).getOrThrow()
        taskService.trashTask(task.id)

        val taskDir = storageManager.getTaskDir(task.id)
        taskDir.mkdirs()
        val file = File(taskDir, "image.webp")
        file.writeText("image bytes")
        assertTrue(file.exists())

        // Empty trash
        val emptyResult = taskService.emptyTrash()
        assertTrue(emptyResult.isOk)

        // Verify files purged from disk
        assertFalse(file.exists())
        assertFalse(taskDir.exists())
    }
}
