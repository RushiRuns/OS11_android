package com.os11.app.data.files

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import com.os11.app.di.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

data class StoredAttachment(
    val filename: String,
    val originalName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val thumbnailPath: String?,
)

/**
 * Manages physical attachment storage on app-private disk per SCHEMA.md:
 * Files live at filesDir/attachments/<task_id>/<id>.<ext>.
 * Thumbnails live at filesDir/attachments/<task_id>/thumbs/<id>.thumb.webp.
 */
@Singleton
@Suppress("TooGenericExceptionCaught", "TooManyFunctions", "MagicNumber", "NestedBlockDepth", "ReturnCount")
class AttachmentStorageManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {

    fun getAttachmentsBaseDir(): File = File(context.filesDir, "attachments")

    fun getTaskDir(taskId: String): File = File(getAttachmentsBaseDir(), taskId)

    fun getThumbsDir(taskId: String): File = File(getTaskDir(taskId), "thumbs")

    fun getAttachmentFile(taskId: String, filename: String): File = File(getTaskDir(taskId), filename)

    fun getThumbnailFile(taskId: String, id: String): File? {
        val thumb = File(getThumbsDir(taskId), "$id.thumb.webp")
        return if (thumb.exists()) thumb else null
    }

    suspend fun copyFromUri(
        taskId: String,
        id: String,
        uri: Uri,
        providedName: String? = null,
        providedMimeType: String? = null,
    ): StoredAttachment = withContext(ioDispatcher) {
        val resolvedName = providedName ?: queryDisplayName(uri) ?: "attachment_$id"
        val resolvedMime = providedMimeType ?: context.contentResolver.getType(uri) ?: guessMimeType(resolvedName)

        val extension = resolveExtension(resolvedName, resolvedMime)
        val sanitizedFilename = "$id.$extension"

        val taskDir = getTaskDir(taskId)
        taskDir.mkdirs()

        val destFile = File(taskDir, sanitizedFilename)
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(destFile).use { output ->
                input.copyTo(output)
            }
        } ?: error("Unable to open input stream for URI: $uri")

        val sizeBytes = destFile.length()

        val thumbnailPath = if (isImageMimeType(resolvedMime)) {
            val thumbsDir = getThumbsDir(taskId)
            thumbsDir.mkdirs()
            val thumbFile = File(thumbsDir, "$id.thumb.webp")
            if (generateThumbnail(destFile, thumbFile)) {
                thumbFile.absolutePath
            } else {
                null
            }
        } else {
            null
        }

        StoredAttachment(
            filename = sanitizedFilename,
            originalName = resolvedName,
            mimeType = resolvedMime,
            sizeBytes = sizeBytes,
            thumbnailPath = thumbnailPath,
        )
    }

    suspend fun deleteAttachmentFile(taskId: String, filename: String, id: String): Boolean =
        withContext(ioDispatcher) {
            val file = File(getTaskDir(taskId), filename)
            val thumb = File(getThumbsDir(taskId), "$id.thumb.webp")

            val fileDeleted = if (file.exists()) file.delete() else true
            val thumbDeleted = if (thumb.exists()) thumb.delete() else true

            cleanEmptyDirs(taskId)
            fileDeleted && thumbDeleted
        }

    suspend fun deleteTaskAttachments(taskId: String): Boolean = withContext(ioDispatcher) {
        val taskDir = getTaskDir(taskId)
        if (taskDir.exists()) {
            taskDir.deleteRecursively()
        } else {
            true
        }
    }

    fun getShareableUri(file: File): Uri {
        val authority = "${context.packageName}.fileprovider"
        return FileProvider.getUriForFile(context, authority, file)
    }

    private fun queryDisplayName(uri: Uri): String? = try {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) cursor.getString(idx) else null
            } else {
                null
            }
        }
    } catch (_: Exception) {
        null
    }

    private fun guessMimeType(filename: String): String {
        val dot = filename.lastIndexOf('.')
        if (dot >= 0) {
            val ext = filename.substring(dot + 1).lowercase(Locale.ROOT)
            val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
            if (mime != null) return mime
        }
        return "application/octet-stream"
    }

    private fun resolveExtension(name: String, mimeType: String): String {
        val dot = name.lastIndexOf('.')
        if (dot in 0 until name.length - 1) {
            val ext = name.substring(dot + 1).lowercase(Locale.ROOT)
            if (ext.length in 1..8 && ext.all { it.isLetterOrDigit() }) {
                return ext
            }
        }
        val fromMime = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
        return fromMime ?: "bin"
    }

    private fun isImageMimeType(mimeType: String): Boolean = mimeType.startsWith("image/", ignoreCase = true)

    private fun generateThumbnail(sourceFile: File, outputFile: File): Boolean {
        return try {
            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(sourceFile.absolutePath, boundsOptions)
            val origW = boundsOptions.outWidth
            val origH = boundsOptions.outHeight
            if (origW <= 0 || origH <= 0) return false

            val maxDim = 256
            var sample = 1
            while ((origW / sample / 2) >= maxDim && (origH / sample / 2) >= maxDim) {
                sample *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sample }
            val bitmap = BitmapFactory.decodeFile(sourceFile.absolutePath, decodeOptions) ?: return false

            outputFile.parentFile?.mkdirs()
            FileOutputStream(outputFile).use { out ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, 80, out)
                } else {
                    @Suppress("DEPRECATION")
                    bitmap.compress(Bitmap.CompressFormat.WEBP, 80, out)
                }
            }
            bitmap.recycle()
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun cleanEmptyDirs(taskId: String) {
        val thumbs = getThumbsDir(taskId)
        if (thumbs.exists() && thumbs.list().isNullOrEmpty()) {
            thumbs.delete()
        }
        val taskDir = getTaskDir(taskId)
        if (taskDir.exists() && taskDir.list().isNullOrEmpty()) {
            taskDir.delete()
        }
    }
}
