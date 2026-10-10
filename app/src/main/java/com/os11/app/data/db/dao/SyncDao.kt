package com.os11.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.os11.app.data.db.entities.SyncConflictLogEntity
import com.os11.app.data.db.entities.SyncFieldClockEntity
import com.os11.app.data.db.entities.SyncLocalStateEntity
import com.os11.app.data.db.entities.SyncPeerStateEntity
import com.os11.app.data.db.entities.SyncPendingEntity
import com.os11.app.data.db.entities.SyncQueueEntity
import com.os11.app.data.db.entities.SyncTombstoneEntity

@Dao
internal interface SyncQueueDao {
    @Insert
    suspend fun enqueue(item: SyncQueueEntity): Long

    @Query("SELECT * FROM sync_queue ORDER BY seq ASC LIMIT :limit")
    suspend fun getNextBatch(limit: Int): List<SyncQueueEntity>

    @Query("DELETE FROM sync_queue WHERE seq <= :seq")
    suspend fun deleteSeqLessThanOrEqual(seq: Long)

    @Query("SELECT COUNT(*) FROM sync_queue")
    suspend fun count(): Int

    @Query(
        """
        DELETE FROM sync_queue
        WHERE seq NOT IN (
            SELECT seq FROM sync_queue ORDER BY seq DESC LIMIT :keepMax
        )
        """,
    )
    suspend fun pruneOldest(keepMax: Int)
}

@Dao
internal interface SyncFieldClockDao {
    @Query("SELECT * FROM sync_field_clock WHERE entity = :entity AND entity_id = :entityId AND field = :field")
    suspend fun getClock(entity: String, entityId: String, field: String): SyncFieldClockEntity?

    @Query("SELECT * FROM sync_field_clock WHERE entity = :entity AND entity_id = :entityId")
    suspend fun getClocksForEntity(entity: String, entityId: String): List<SyncFieldClockEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(clock: SyncFieldClockEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(clocks: List<SyncFieldClockEntity>)
}

@Dao
internal interface SyncTombstoneDao {
    @Query("SELECT * FROM sync_tombstone WHERE entity = :entity AND entity_id = :entityId")
    suspend fun getTombstone(entity: String, entityId: String): SyncTombstoneEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(tombstone: SyncTombstoneEntity)

    @Query("DELETE FROM sync_tombstone WHERE deleted_at < :cutoffIso")
    suspend fun deleteOlderThan(cutoffIso: String)
}

@Dao
internal interface SyncLocalStateDao {
    @Query("SELECT * FROM sync_local_state WHERE id = 1 LIMIT 1")
    suspend fun get(): SyncLocalStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun set(state: SyncLocalStateEntity)
}

@Dao
internal interface SyncPeerStateDao {
    @Query("SELECT * FROM sync_peer_state WHERE peer_device_id = :peerDeviceId")
    suspend fun getPeerState(peerDeviceId: String): SyncPeerStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(state: SyncPeerStateEntity)
}

@Dao
internal interface SyncPendingDao {
    @Query(
        """
        SELECT * FROM sync_pending
        WHERE waiting_for_entity = :waitingForEntity AND waiting_for_entity_id = :waitingForEntityId
        """,
    )
    suspend fun getPendingFor(waitingForEntity: String, waitingForEntityId: String): List<SyncPendingEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(pending: SyncPendingEntity)

    @Query("DELETE FROM sync_pending WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
internal interface SyncConflictLogDao {
    @Query("SELECT * FROM sync_conflict_log WHERE entity = :entity AND entity_id = :entityId ORDER BY occurred_at DESC")
    suspend fun getConflictsForEntity(entity: String, entityId: String): List<SyncConflictLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(conflict: SyncConflictLogEntity)
}
