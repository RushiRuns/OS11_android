package com.os11.app.service.attachment

import android.net.Uri
import com.os11.app.data.db.entities.AttachmentEntity
import com.os11.app.data.files.AttachmentStorageManager
import com.os11.app.data.repository.AttachmentRepository
import com.os11.app.di.IoDispatcher
import com.os11.domain.id.IdGenerator
import com.os11.domain.result.AppError
import com.os11.domain.result.AppResult
import com.os11.domain.time.Clock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@Suppress("TooGenericExceptionCaught", "MagicNumber", "TooManyFunctions")
class AttachmentService @Inject constructor(
    private val attachmentRepository: AttachmentRepository,
    private val storageManager: AttachmentStorageManager,
    private val clock: Clock,
    private val idGenerator: IdGenerator,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {

    fun observeAttachments(taskId: String): Flow<List<AttachmentItem>> =
        attachmentRepository.observeByTask(taskId).map { entities ->
            entities.map { entity -> toItem(entity) }
        }

    suspend fun getAttachmentById(id: String): AttachmentItem? = withContext(ioDispatcher) {
        val entity = attachmentRepository.getById(id) ?: return@withContext null
        toItem(entity)
    }

    suspend fun addFileAttachment(
        taskId: String,
        uri: Uri,
        originalName: String? = null,
        mimeType: String? = null,
    ): AppResult<AttachmentItem> = withContext(ioDispatcher) {
        try {
            val id = idGenerator.randomId()
            val stored = storageManager.copyFromUri(
                taskId = taskId,
                id = id,
                uri = uri,
                providedName = originalName,
                providedMimeType = mimeType,
            )

            val entity = AttachmentEntity(
                id = id,
                taskId = taskId,
                filename = stored.filename,
                originalName = stored.originalName,
                mimeType = stored.mimeType,
                sizeBytes = stored.sizeBytes,
                isLink = false,
                linkUrl = null,
                blobState = "present",
                createdAt = clock.nowIso(),
            )

            val saved = attachmentRepository.save(entity)
            AppResult.ok(toItem(saved, stored.thumbnailPath))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppResult.err(AppError.Unexpected(e))
        }
    }

    suspend fun addLinkAttachment(taskId: String, url: String, title: String? = null): AppResult<AttachmentItem> =
        withContext(ioDispatcher) {
            try {
                val trimmedUrl = url.trim()
                if (trimmedUrl.isBlank()) {
                    return@withContext AppResult.err(AppError.Validation("url", "Link URL cannot be empty"))
                }

                val normalizedUrl = if (!trimmedUrl.startsWith("http://") && !trimmedUrl.startsWith("https://")) {
                    "https://$trimmedUrl"
                } else {
                    trimmedUrl
                }

                val id = idGenerator.randomId()
                val displayName = if (!title.isNullOrBlank()) title.trim() else normalizedUrl

                val entity = AttachmentEntity(
                    id = id,
                    taskId = taskId,
                    filename = normalizedUrl,
                    originalName = displayName,
                    mimeType = "text/uri-list",
                    sizeBytes = 0L,
                    isLink = true,
                    linkUrl = normalizedUrl,
                    blobState = "present",
                    createdAt = clock.nowIso(),
                )

                val saved = attachmentRepository.save(entity)
                AppResult.ok(toItem(saved))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppResult.err(AppError.Unexpected(e))
            }
        }

    suspend fun deleteAttachment(id: String): AppResult<Unit> = withContext(ioDispatcher) {
        try {
            val existing = attachmentRepository.getById(id)
                ?: return@withContext AppResult.ok(Unit)

            if (!existing.isLink) {
                storageManager.deleteAttachmentFile(
                    taskId = existing.taskId,
                    filename = existing.filename,
                    id = existing.id,
                )
            }

            attachmentRepository.delete(id)
            AppResult.ok(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppResult.err(AppError.Unexpected(e))
        }
    }

    suspend fun deleteTaskAttachments(taskId: String): AppResult<Unit> = withContext(ioDispatcher) {
        try {
            storageManager.deleteTaskAttachments(taskId)
            AppResult.ok(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppResult.err(AppError.Unexpected(e))
        }
    }

    suspend fun updateBlobState(id: String, blobState: String): AppResult<Unit> = withContext(ioDispatcher) {
        try {
            val existing = attachmentRepository.getById(id)
                ?: return@withContext AppResult.err(AppError.NotFound)

            val updated = existing.copy(blobState = blobState)
            attachmentRepository.save(updated)
            AppResult.ok(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppResult.err(AppError.Unexpected(e))
        }
    }

    fun getAttachmentFile(taskId: String, filename: String): File = storageManager.getAttachmentFile(taskId, filename)

    fun getShareableUri(file: File): Uri = storageManager.getShareableUri(file)

    private fun toItem(entity: AttachmentEntity, explicitThumbPath: String? = null): AttachmentItem {
        val file = if (!entity.isLink) storageManager.getAttachmentFile(entity.taskId, entity.filename) else null
        val filePath = if (file?.exists() == true) file.absolutePath else null
        val thumbFile = if (!entity.isLink) storageManager.getThumbnailFile(entity.taskId, entity.id) else null
        val thumbPath = explicitThumbPath ?: thumbFile?.absolutePath

        val formattedSize = if (entity.isLink) {
            "Link"
        } else {
            formatFileSize(entity.sizeBytes)
        }

        return AttachmentItem(
            id = entity.id,
            taskId = entity.taskId,
            filename = entity.filename,
            originalName = entity.originalName,
            mimeType = entity.mimeType,
            sizeBytes = entity.sizeBytes,
            isLink = entity.isLink,
            linkUrl = entity.linkUrl,
            blobState = entity.blobState,
            createdAt = entity.createdAt,
            filePath = filePath,
            thumbnailPath = thumbPath,
            formattedSize = formattedSize,
        )
    }

    private fun formatFileSize(bytes: Long): String = when {
        bytes < 1024L -> "$bytes B"
        bytes < 1024L * 1024L -> String.format(Locale.US, "%.1f KB", bytes / 1024f)
        bytes < 1024L * 1024L * 1024L -> String.format(Locale.US, "%.1f MB", bytes / (1024f * 1024f))
        else -> String.format(Locale.US, "%.1f GB", bytes / (1024f * 1024f * 1024f))
    }
}
