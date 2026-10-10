package com.os11.app.data.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "display_name")
    val displayName: String,
    @ColumnInfo(name = "avatar_emoji")
    val avatarEmoji: String? = null,
    @ColumnInfo(name = "last_seen_at")
    val lastSeenAt: String? = null,
    @ColumnInfo(name = "created_at")
    val createdAt: String,
)

@Entity(tableName = "devices")
data class DeviceEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "device_name")
    val deviceName: String,
    @ColumnInfo(name = "device_type")
    val deviceType: String,
    @ColumnInfo(name = "last_connected")
    val lastConnected: String? = null,
    @ColumnInfo(name = "is_trusted", defaultValue = "1")
    val isTrusted: Boolean = true,
    @ColumnInfo(name = "created_at")
    val createdAt: String,
)

@Entity(
    tableName = "collaboration_members",
    foreignKeys = [
        ForeignKey(
            entity = DeviceEntity::class,
            parentColumns = ["id"],
            childColumns = ["device_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(name = "idx_collab_device_resource", value = ["resource_id", "device_id"], unique = true),
        Index(name = "idx_collab_device", value = ["device_id"]),
    ],
)
data class CollaborationMemberEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "resource_type")
    val resourceType: String,
    @ColumnInfo(name = "resource_id")
    val resourceId: String,
    @ColumnInfo(name = "device_id")
    val deviceId: String,
    @ColumnInfo(name = "permission", defaultValue = "editor")
    val permission: String = "editor",
    @ColumnInfo(name = "invited_at")
    val invitedAt: String,
    @ColumnInfo(name = "accepted_at")
    val acceptedAt: String? = null,
)
