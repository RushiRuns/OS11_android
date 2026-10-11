package com.os11.app.data.files

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
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
import java.io.File
import java.io.FileOutputStream

@RunWith(RobolectricTestRunner::class)
class AttachmentStorageManagerTest {

    private lateinit var context: Context
    private lateinit var storageManager: AttachmentStorageManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        storageManager = AttachmentStorageManager(context, Dispatchers.Unconfined)
        storageManager.getAttachmentsBaseDir().deleteRecursively()
    }

    @After
    fun tearDown() {
        storageManager.getAttachmentsBaseDir().deleteRecursively()
    }

    @Test
    fun copyFromUri_stores_file_in_private_storage_without_path_in_db() = runBlocking {
        val taskId = "task-123"
        val attachmentId = "att-abc"

        // Create temporary source file
        val tempSource = File(context.cacheDir, "sample_document.pdf")
        tempSource.writeText("Sample PDF content byte payload")
        val sourceUri = Uri.fromFile(tempSource)

        val stored = storageManager.copyFromUri(
            taskId = taskId,
            id = attachmentId,
            uri = sourceUri,
            providedName = "sample_document.pdf",
            providedMimeType = "application/pdf",
        )

        assertEquals("att-abc.pdf", stored.filename)
        assertEquals("sample_document.pdf", stored.originalName)
        assertEquals("application/pdf", stored.mimeType)
        assertTrue(stored.sizeBytes > 0)
        assertNull("Non-images should not generate image thumbnails", stored.thumbnailPath)

        val destFile = storageManager.getAttachmentFile(taskId, stored.filename)
        assertTrue("Stored file must exist on disk", destFile.exists())
        assertEquals("Sample PDF content byte payload", destFile.readText())
    }

    @Test
    fun copyFromUri_generates_thumbnail_for_images() = runBlocking {
        val taskId = "task-456"
        val attachmentId = "att-img-1"

        // Create a bitmap image file
        val tempImg = File(context.cacheDir, "photo.png")
        val bitmap = Bitmap.createBitmap(500, 500, Bitmap.Config.ARGB_8888)
        FileOutputStream(tempImg).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        bitmap.recycle()
        val sourceUri = Uri.fromFile(tempImg)

        val stored = storageManager.copyFromUri(
            taskId = taskId,
            id = attachmentId,
            uri = sourceUri,
            providedName = "photo.png",
            providedMimeType = "image/png",
        )

        assertEquals("att-img-1.png", stored.filename)
        assertNotNull("Image thumbnail path must not be null", stored.thumbnailPath)

        val thumbFile = storageManager.getThumbnailFile(taskId, attachmentId)
        assertNotNull("Thumbnail file must be retrievable", thumbFile)
        assertTrue("Thumbnail file must exist on disk", thumbFile!!.exists())
    }

    @Test
    fun deleteAttachmentFile_removes_file_and_thumbnail() = runBlocking {
        val taskId = "task-789"
        val attachmentId = "att-del-1"

        val tempImg = File(context.cacheDir, "delete_me.png")
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        FileOutputStream(tempImg).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        bitmap.recycle()

        val stored = storageManager.copyFromUri(
            taskId = taskId,
            id = attachmentId,
            uri = Uri.fromFile(tempImg),
            providedName = "delete_me.png",
            providedMimeType = "image/png",
        )

        val destFile = storageManager.getAttachmentFile(taskId, stored.filename)
        val thumbFile = storageManager.getThumbnailFile(taskId, attachmentId)
        assertTrue(destFile.exists())
        assertTrue(thumbFile!!.exists())

        val deleted = storageManager.deleteAttachmentFile(taskId, stored.filename, attachmentId)
        assertTrue("deleteAttachmentFile must return true", deleted)
        assertFalse("Attachment file must no longer exist", destFile.exists())
        assertFalse("Thumbnail must no longer exist", thumbFile.exists())
    }

    @Test
    fun deleteTaskAttachments_leaves_zero_orphan_files() = runBlocking {
        val taskId = "task-orphan-check"

        // Store 3 attachments for this task
        for (i in 1..3) {
            val temp = File(context.cacheDir, "file_$i.txt")
            temp.writeText("content $i")
            storageManager.copyFromUri(
                taskId = taskId,
                id = "att-$i",
                uri = Uri.fromFile(temp),
                providedName = "file_$i.txt",
                providedMimeType = "text/plain",
            )
        }

        val taskDir = storageManager.getTaskDir(taskId)
        assertTrue("Task dir must exist with stored files", taskDir.exists())
        assertEquals(3, taskDir.listFiles { _, name -> name.endsWith(".txt") }?.size)

        // Delete entire task directory
        val purged = storageManager.deleteTaskAttachments(taskId)
        assertTrue("deleteTaskAttachments must return true", purged)
        assertFalse("Task directory must be deleted completely", taskDir.exists())
    }
}
