package com.os11.app.data.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "reminders",
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["task_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(name = "idx_reminders_task", value = ["task_id"]),
        Index(name = "idx_reminders_at", value = ["remind_at"]),
    ],
)
data class ReminderEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "task_id")
    val taskId: String,
    @ColumnInfo(name = "remind_at")
    val remindAt: String,
    @ColumnInfo(name = "created_at")
    val createdAt: String,
)

@Entity(
    tableName = "reminder_device_state",
    foreignKeys = [
        ForeignKey(
            entity = ReminderEntity::class,
            parentColumns = ["id"],
            childColumns = ["reminder_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(name = "idx_rds_pending", value = ["is_triggered"]),
    ],
)
data class ReminderDeviceStateEntity(
    @PrimaryKey
    @ColumnInfo(name = "reminder_id")
    val reminderId: String,
    @ColumnInfo(name = "is_triggered", defaultValue = "0")
    val isTriggered: Boolean = false,
    @ColumnInfo(name = "snoozed_until")
    val snoozedUntil: String? = null,
)
