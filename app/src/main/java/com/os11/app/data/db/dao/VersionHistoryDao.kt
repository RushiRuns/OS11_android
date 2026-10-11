package com.os11.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.os11.app.data.db.entities.VersionHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VersionHistoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: VersionHistoryEntity)

    @Query("SELECT * FROM version_history ORDER BY created_at DESC")
    fun observeAll(): Flow<List<VersionHistoryEntity>>

    @Query(
        "SELECT * FROM version_history " +
            "WHERE entity_type = :entityType AND entity_id = :entityId " +
            "ORDER BY version_number DESC",
    )
    suspend fun getForEntity(entityType: String, entityId: String): List<VersionHistoryEntity>

    @Query("SELECT * FROM version_history ORDER BY created_at DESC LIMIT :limit")
    suspend fun getRecent(limit: Int = 50): List<VersionHistoryEntity>

    @Query("SELECT COUNT(*) FROM version_history")
    suspend fun count(): Int

    @Query("DELETE FROM version_history WHERE id = :id")
    suspend fun deleteById(id: String)
}
