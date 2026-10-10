package com.os11.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.os11.app.data.db.entities.CollaborationMemberEntity
import com.os11.app.data.db.entities.DeviceEntity
import com.os11.app.data.db.entities.GoalEntity
import com.os11.app.data.db.entities.GoalLinkEntity
import com.os11.app.data.db.entities.ModuleEntity
import com.os11.app.data.db.entities.NotificationHistoryEntity
import com.os11.app.data.db.entities.SettingEntity
import com.os11.app.data.db.entities.TaskHistoryEntity
import com.os11.app.data.db.entities.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
internal interface GoalDao {
    @Query("SELECT * FROM goals WHERE id = :id")
    suspend fun getById(id: String): GoalEntity?

    @Query("SELECT * FROM goals ORDER BY created_at DESC")
    fun observeAll(): Flow<List<GoalEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(goal: GoalEntity)

    @Update
    suspend fun update(goal: GoalEntity)

    @Query("DELETE FROM goals WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
internal interface GoalLinkDao {
    @Query("SELECT * FROM goal_links WHERE goal_id = :goalId")
    fun observeByGoal(goalId: String): Flow<List<GoalLinkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(link: GoalLinkEntity)

    @Query("DELETE FROM goal_links WHERE goal_id = :goalId AND resource_type = :type AND resource_id = :resourceId")
    suspend fun delete(goalId: String, type: String, resourceId: String)
}

@Dao
internal interface NotificationHistoryDao {
    @Query("SELECT * FROM notification_history ORDER BY created_at DESC")
    fun observeAll(): Flow<List<NotificationHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: NotificationHistoryEntity)

    @Query("SELECT COUNT(*) FROM notification_history WHERE read_at IS NULL")
    fun observeUnreadCount(): Flow<Int>

    @Query("UPDATE notification_history SET read_at = :readAt WHERE id = :id")
    suspend fun markAsRead(id: String, readAt: String)

    @Query("UPDATE notification_history SET read_at = :readAt WHERE read_at IS NULL")
    suspend fun markAllAsRead(readAt: String)

    @Query("DELETE FROM notification_history WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM notification_history")
    suspend fun clearAll()

    @Query("DELETE FROM notification_history WHERE created_at < :cutoffIso")
    suspend fun purgeOlderThan(cutoffIso: String)
}

@Dao
internal interface TaskHistoryDao {
    @Query("SELECT * FROM task_history WHERE task_id = :taskId ORDER BY changed_at DESC")
    fun observeByTask(taskId: String): Flow<List<TaskHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(history: TaskHistoryEntity)

    @Query("DELETE FROM task_history WHERE changed_at < :cutoffIso")
    suspend fun purgeOlderThan(cutoffIso: String)
}

@Dao
internal interface SettingDao {
    @Query("SELECT value FROM settings WHERE key = :key")
    suspend fun getValue(key: String): String?

    @Query("SELECT * FROM settings")
    fun observeAll(): Flow<List<SettingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(setting: SettingEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(settings: List<SettingEntity>)
}

@Dao
internal interface ModuleDao {
    @Query("SELECT is_enabled FROM modules WHERE module_name = :name")
    suspend fun isEnabled(name: String): Boolean?

    @Query("SELECT * FROM modules")
    fun observeAll(): Flow<List<ModuleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(module: ModuleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(modules: List<ModuleEntity>)
}

@Dao
internal interface UserDao {
    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun getById(id: String): UserEntity?

    @Query("SELECT * FROM users ORDER BY display_name ASC")
    fun observeAll(): Flow<List<UserEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(user: UserEntity)
}

@Dao
internal interface DeviceDao {
    @Query("SELECT * FROM devices WHERE id = :id")
    suspend fun getById(id: String): DeviceEntity?

    @Query("SELECT * FROM devices ORDER BY created_at DESC")
    fun observeAll(): Flow<List<DeviceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(device: DeviceEntity)

    @Query("DELETE FROM devices WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
internal interface CollaborationMemberDao {
    @Query("SELECT * FROM collaboration_members WHERE resource_id = :resourceId")
    fun observeByResource(resourceId: String): Flow<List<CollaborationMemberEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(member: CollaborationMemberEntity)

    @Query("DELETE FROM collaboration_members WHERE id = :id")
    suspend fun deleteById(id: String)
}
