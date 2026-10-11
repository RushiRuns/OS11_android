package com.os11.app.service.attachment

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.os11.app.data.db.entities.AttachmentEntity
import com.os11.app.data.files.AttachmentStorageManager
import com.os11.app.data.repository.AttachmentRepository
import com.os11.domain.id.IdGenerator
import com.os11.domain.result.AppResult
import com.os11.domain.time.Clock
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class AttachmentServiceTest {

    private val repository: AttachmentRepository = mockk(relaxed = true)
    private val clock: Clock = mockk()
    private val idGenerator: IdGenerator = mockk()

    private lateinit var context: Context
    private lateinit var storageManager: AttachmentStorageManager
    private lateinit var attachmentService: AttachmentService

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        storageManager = AttachmentStorageManager(context, Dispatchers.Unconfined)
        storageManager.getAttachmentsBaseDir().deleteRecursively()

        coEvery { clock.nowIso() } returns "2026-10-11T12:00:00.000Z"
        coEvery { idGenerator.randomId() } returns "att-uuid-1"

        attachmentService = AttachmentService(
            attachmentRepository = repository,
            storageManager = storageManager,
            clock = clock,
            idGenerator = idGenerator,
            ioDispatcher = Dispatchers.Unconfined,
        )
    }

    @Test
    fun addFileAttachment_persists_metadata_and_copies_file_without_stored_path() = runTest {
        val taskId = "task-test-1"
        val tempSource = File(context.cacheDir, "spec.txt")
        tempSource.writeText("Feature specification")

        val entitySlot = slot<AttachmentEntity>()
        coEvery { repository.save(capture(entitySlot)) } answers { entitySlot.captured }

        val result = attachmentService.addFileAttachment(
            taskId = taskId,
            uri = Uri.fromFile(tempSource),
            originalName = "spec.txt",
            mimeType = "text/plain",
        )

        assertTrue(result is AppResult.Ok)
        val item = (result as AppResult.Ok).value

        assertEquals("att-uuid-1", item.id)
        assertEquals(taskId, item.taskId)
        assertEquals("att-uuid-1.txt", item.filename)
        assertEquals("spec.txt", item.originalName)
        assertEquals("text/plain", item.mimeType)
        assertEquals("present", item.blobState)
        assertFalse(item.isLink)

        // Verify entity sent to repository has no stored path column
        val savedEntity = entitySlot.captured
        assertEquals("att-uuid-1", savedEntity.id)
        assertEquals("att-uuid-1.txt", savedEntity.filename)
        assertEquals("present", savedEntity.blobState)

        // Verify physical file exists on disk
        val destFile = storageManager.getAttachmentFile(taskId, "att-uuid-1.txt")
        assertTrue(destFile.exists())
        assertEquals("Feature specification", destFile.readText())
    }

    @Test
    fun addLinkAttachment_normalizes_url_and_persists_link_metadata() = runTest {
        val taskId = "task-test-2"
        val entitySlot = slot<AttachmentEntity>()
        coEvery { repository.save(capture(entitySlot)) } answers { entitySlot.captured }

        val result = attachmentService.addLinkAttachment(
            taskId = taskId,
            url = "github.com/project/issues/42",
            title = "Issue #42",
        )

        assertTrue(result is AppResult.Ok)
        val item = (result as AppResult.Ok).value

        assertEquals("att-uuid-1", item.id)
        assertEquals(taskId, item.taskId)
        assertTrue(item.isLink)
        assertEquals("https://github.com/project/issues/42", item.linkUrl)
        assertEquals("Issue #42", item.originalName)
        assertEquals("present", item.blobState)
        assertEquals(0L, item.sizeBytes)

        val saved = entitySlot.captured
        assertEquals(true, saved.isLink)
        assertEquals("https://github.com/project/issues/42", saved.linkUrl)
        assertEquals("present", saved.blobState)
    }

    @Test
    fun addLinkAttachment_rejects_empty_url() = runTest {
        val result = attachmentService.addLinkAttachment(
            taskId = "task-3",
            url = "   ",
            title = "Empty",
        )

        assertTrue("Blank URL must return error", result is AppResult.Err)
    }

    @Test
    fun deleteAttachment_removes_from_repository_and_deletes_disk_file() = runTest {
        val taskId = "task-del"
        val attId = "att-to-delete"

        val temp = File(context.cacheDir, "sample.txt")
        temp.writeText("delete me")
        val stored = storageManager.copyFromUri(
            taskId = taskId,
            id = attId,
            uri = Uri.fromFile(temp),
            providedName = "sample.txt",
            providedMimeType = "text/plain",
        )

        val entity = AttachmentEntity(
            id = attId,
            taskId = taskId,
            filename = stored.filename,
            originalName = "sample.txt",
            mimeType = "text/plain",
            sizeBytes = stored.sizeBytes,
            isLink = false,
            linkUrl = null,
            blobState = "present",
            createdAt = "2026-10-11T12:00:00.000Z",
        )

        coEvery { repository.getById(attId) } returns entity
        coEvery { repository.delete(attId) } returns Unit

        val fileOnDisk = storageManager.getAttachmentFile(taskId, stored.filename)
        assertTrue(fileOnDisk.exists())

        val deleteResult = attachmentService.deleteAttachment(attId)
        assertTrue(deleteResult is AppResult.Ok)

        coVerify { repository.delete(attId) }
        assertFalse("Physical file must be deleted from disk", fileOnDisk.exists())
    }

    @Test
    fun deleteTaskAttachments_cleans_up_all_files_for_task() = runTest {
        val taskId = "task-bulk-cleanup"

        for (i in 1..2) {
            val temp = File(context.cacheDir, "doc_$i.txt")
            temp.writeText("hello $i")
            storageManager.copyFromUri(
                taskId = taskId,
                id = "id-$i",
                uri = Uri.fromFile(temp),
                providedName = "doc_$i.txt",
                providedMimeType = "text/plain",
            )
        }

        val taskDir = storageManager.getTaskDir(taskId)
        assertTrue(taskDir.exists())

        attachmentService.deleteTaskAttachments(taskId)
        assertFalse("Task directory and all its files must be deleted", taskDir.exists())
    }

    @Test
    fun updateBlobState_updates_local_blob_state() = runTest {
        val attId = "att-blob-1"
        val existing = AttachmentEntity(
            id = attId,
            taskId = "task-1",
            filename = "file.png",
            originalName = "file.png",
            mimeType = "image/png",
            sizeBytes = 100,
            isLink = false,
            linkUrl = null,
            blobState = "present",
            createdAt = "2026-10-11T12:00:00.000Z",
        )

        coEvery { repository.getById(attId) } returns existing
        val entitySlot = slot<AttachmentEntity>()
        coEvery { repository.save(capture(entitySlot)) } answers { entitySlot.captured }

        val res = attachmentService.updateBlobState(attId, "missing")
        assertTrue(res is AppResult.Ok)
        assertEquals("missing", entitySlot.captured.blobState)
    }

    @Test
    fun observeAttachments_maps_entities_to_items_with_formatted_size() = runTest {
        val entity = AttachmentEntity(
            id = "att-1",
            taskId = "task-obs",
            filename = "att-1.png",
            originalName = "my_photo.png",
            mimeType = "image/png",
            sizeBytes = 2_097_152L, // 2 MB
            isLink = false,
            linkUrl = null,
            blobState = "present",
            createdAt = "2026-10-11T12:00:00.000Z",
        )

        coEvery { repository.observeByTask("task-obs") } returns flowOf(listOf(entity))

        val items = attachmentService.observeAttachments("task-obs").first()
        assertEquals(1, items.size)
        val item = items[0]
        assertEquals("my_photo.png", item.originalName)
        assertEquals("2.0 MB", item.formattedSize)
        assertTrue(item.isImage)
    }
}
