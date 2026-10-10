package com.os11.app.data.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "attachments",
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["task_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(name = "idx_attachments_task", value = ["task_id"]),
    ],
)
data class AttachmentEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "task_id")
    val taskId: String,
    @ColumnInfo(name = "filename")
    val filename: String,
    @ColumnInfo(name = "original_name")
    val originalName: String,
    @ColumnInfo(name = "mime_type")
    val mimeType: String,
    @ColumnInfo(name = "size_bytes")
    val sizeBytes: Long,
    @ColumnInfo(name = "is_link", defaultValue = "0")
    val isLink: Boolean = false,
    @ColumnInfo(name = "link_url")
    val linkUrl: String? = null,
    @ColumnInfo(name = "blob_state", defaultValue = "present")
    val blobState: String = "present",
    @ColumnInfo(name = "created_at")
    val createdAt: String,
)

@Entity(
    tableName = "comments",
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["task_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = CommentEntity::class,
            parentColumns = ["id"],
            childColumns = ["parent_comment_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(name = "idx_comments_task", value = ["task_id"]),
        Index(name = "idx_comments_parent", value = ["parent_comment_id"]),
    ],
)
data class CommentEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "task_id")
    val taskId: String,
    @ColumnInfo(name = "author_device_id", defaultValue = "local")
    val authorDeviceId: String = "local",
    @ColumnInfo(name = "author_name")
    val authorName: String,
    @ColumnInfo(name = "body")
    val body: String,
    @ColumnInfo(name = "parent_comment_id")
    val parentCommentId: String? = null,
    @ColumnInfo(name = "is_resolved", defaultValue = "0")
    val isResolved: Boolean = false,
    @ColumnInfo(name = "created_at")
    val createdAt: String,
    @ColumnInfo(name = "updated_at")
    val updatedAt: String,
)

@Entity(
    tableName = "comment_reactions",
    primaryKeys = ["comment_id", "reactor_device_id", "emoji"],
    foreignKeys = [
        ForeignKey(
            entity = CommentEntity::class,
            parentColumns = ["id"],
            childColumns = ["comment_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class CommentReactionEntity(
    @ColumnInfo(name = "comment_id")
    val commentId: String,
    @ColumnInfo(name = "reactor_device_id", defaultValue = "local")
    val reactorDeviceId: String = "local",
    @ColumnInfo(name = "emoji")
    val emoji: String,
)

@Entity(
    tableName = "pomodoro_sessions",
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["task_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index(name = "idx_pomodoro_task", value = ["task_id"]),
        Index(name = "idx_pomodoro_started", value = ["started_at"]),
    ],
)
data class PomodoroSessionEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "task_id")
    val taskId: String? = null,
    @ColumnInfo(name = "type")
    val type: String,
    @ColumnInfo(name = "duration_seconds")
    val durationSeconds: Int,
    @ColumnInfo(name = "started_at")
    val startedAt: String,
    @ColumnInfo(name = "ended_at")
    val endedAt: String? = null,
    @ColumnInfo(name = "was_completed", defaultValue = "0")
    val wasCompleted: Boolean = false,
    @ColumnInfo(name = "created_at")
    val createdAt: String,
)
