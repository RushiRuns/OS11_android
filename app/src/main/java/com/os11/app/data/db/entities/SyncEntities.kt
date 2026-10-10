package com.os11.app.data.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sync_queue",
    indices = [
        Index(name = "idx_sync_queue_id", value = ["id"], unique = true),
        Index(name = "idx_sync_queue_created", value = ["created_at"]),
    ],
)
data class SyncQueueEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "seq")
    val seq: Long = 0,
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "operation")
    val operation: String,
    @ColumnInfo(name = "entity")
    val entity: String,
    @ColumnInfo(name = "entity_id")
    val entityId: String,
    @ColumnInfo(name = "payload")
    val payload: String,
    @ColumnInfo(name = "target_device_id")
    val targetDeviceId: String? = null,
    @ColumnInfo(name = "created_at")
    val createdAt: String,
    @ColumnInfo(name = "retry_count", defaultValue = "0")
    val retryCount: Int = 0,
)

@Entity(
    tableName = "sync_field_clock",
    primaryKeys = ["entity", "entity_id", "field"],
)
data class SyncFieldClockEntity(
    @ColumnInfo(name = "entity")
    val entity: String,
    @ColumnInfo(name = "entity_id")
    val entityId: String,
    @ColumnInfo(name = "field")
    val field: String,
    @ColumnInfo(name = "hlc")
    val hlc: String,
)

@Entity(
    tableName = "sync_tombstone",
    primaryKeys = ["entity", "entity_id"],
)
data class SyncTombstoneEntity(
    @ColumnInfo(name = "entity")
    val entity: String,
    @ColumnInfo(name = "entity_id")
    val entityId: String,
    @ColumnInfo(name = "hlc")
    val hlc: String,
    @ColumnInfo(name = "deleted_at")
    val deletedAt: String,
)

@Entity(tableName = "sync_local_state")
data class SyncLocalStateEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: Long = 1,
    @ColumnInfo(name = "last_hlc")
    val lastHlc: String,
)

@Entity(
    tableName = "sync_peer_state",
    foreignKeys = [
        ForeignKey(
            entity = DeviceEntity::class,
            parentColumns = ["id"],
            childColumns = ["peer_device_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class SyncPeerStateEntity(
    @PrimaryKey
    @ColumnInfo(name = "peer_device_id")
    val peerDeviceId: String,
    @ColumnInfo(name = "last_sent_seq", defaultValue = "0")
    val lastSentSeq: Long = 0,
    @ColumnInfo(name = "last_received_hlc")
    val lastReceivedHlc: String? = null,
    @ColumnInfo(name = "last_synced_at")
    val lastSyncedAt: String? = null,
)

@Entity(tableName = "sync_pending")
data class SyncPendingEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "entity")
    val entity: String,
    @ColumnInfo(name = "entity_id")
    val entityId: String,
    @ColumnInfo(name = "payload")
    val payload: String,
    @ColumnInfo(name = "waiting_for_entity")
    val waitingForEntity: String,
    @ColumnInfo(name = "waiting_for_entity_id")
    val waitingForEntityId: String,
    @ColumnInfo(name = "received_at")
    val receivedAt: String,
)

@Entity(
    tableName = "sync_conflict_log",
    indices = [
        Index(name = "idx_conflict_entity", value = ["entity", "entity_id"]),
    ],
)
data class SyncConflictLogEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "entity")
    val entity: String,
    @ColumnInfo(name = "entity_id")
    val entityId: String,
    @ColumnInfo(name = "field")
    val field: String,
    @ColumnInfo(name = "losing_value")
    val losingValue: String? = null,
    @ColumnInfo(name = "winning_value")
    val winningValue: String? = null,
    @ColumnInfo(name = "losing_device_id")
    val losingDeviceId: String? = null,
    @ColumnInfo(name = "occurred_at")
    val occurredAt: String,
)
