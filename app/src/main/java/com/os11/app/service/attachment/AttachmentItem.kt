package com.os11.app.service.attachment

/**
 * Service-level DTO representing an attachment.
 * Isolates Room AttachmentEntity from UI composables per clean architecture rules.
 */
data class AttachmentItem(
    val id: String,
    val taskId: String,
    val filename: String,
    val originalName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val isLink: Boolean,
    val linkUrl: String?,
    val blobState: String, // "present" | "missing" | "pending"
    val createdAt: String,
    val filePath: String? = null,
    val thumbnailPath: String? = null,
    val formattedSize: String = "",
) {
    val isImage: Boolean
        get() = !isLink && mimeType.startsWith("image/", ignoreCase = true)

    val isPdf: Boolean
        get() = !isLink &&
            (
                mimeType.equals("application/pdf", ignoreCase = true) ||
                    originalName.endsWith(".pdf", ignoreCase = true)
                )

    val isAudio: Boolean
        get() = !isLink && mimeType.startsWith("audio/", ignoreCase = true)

    val isVideo: Boolean
        get() = !isLink && mimeType.startsWith("video/", ignoreCase = true)
}
