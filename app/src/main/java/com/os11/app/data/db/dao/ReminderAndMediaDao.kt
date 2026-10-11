package com.os11.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.os11.app.data.db.entities.AttachmentEntity
import com.os11.app.data.db.entities.CommentEntity
import com.os11.app.data.db.entities.CommentReactionEntity
import com.os11.app.data.db.entities.PomodoroSessionEntity
import com.os11.app.data.db.entities.ReminderDeviceStateEntity
import com.os11.app.data.db.entities.ReminderEntity
import kotlinx.coroutines.flow.Flow

@Dao
internal interface ReminderDao {
    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun getById(id: String): ReminderEntity?

    @Query("SELECT * FROM reminders WHERE task_id = :taskId ORDER BY remind_at ASC")
    fun observeByTask(taskId: String): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE task_id = :taskId")
    suspend fun getByTask(taskId: String): List<ReminderEntity>

    @Query(
        """
        SELECT r.* FROM reminders r
        LEFT JOIN reminder_device_state rds ON rds.reminder_id = r.id
        WHERE rds.is_triggered IS NULL OR rds.is_triggered = 0
        ORDER BY r.remind_at ASC
        """,
    )
    suspend fun getPendingReminders(): List<ReminderEntity>

    @Query(
        """
        SELECT r.* FROM reminders r
        LEFT JOIN reminder_device_state rds ON rds.reminder_id = r.id
        WHERE (rds.is_triggered IS NULL OR rds.is_triggered = 0)
           OR (rds.snoozed_until IS NOT NULL AND rds.snoozed_until > :nowIso)
        ORDER BY CASE WHEN rds.snoozed_until IS NOT NULL THEN rds.snoozed_until ELSE r.remind_at END ASC
        """,
    )
    suspend fun getPendingReminders(nowIso: String): List<ReminderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(reminder: ReminderEntity)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
internal interface ReminderDeviceStateDao {
    @Query("SELECT * FROM reminder_device_state WHERE reminder_id = :reminderId")
    suspend fun getByReminderId(reminderId: String): ReminderDeviceStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(state: ReminderDeviceStateEntity)
}

@Dao
internal interface AttachmentDao {
    @Query("SELECT * FROM attachments WHERE id = :id")
    suspend fun getById(id: String): AttachmentEntity?

    @Query("SELECT * FROM attachments WHERE task_id = :taskId ORDER BY created_at ASC")
    fun observeByTask(taskId: String): Flow<List<AttachmentEntity>>

    @Query("SELECT * FROM attachments WHERE task_id = :taskId")
    suspend fun getByTask(taskId: String): List<AttachmentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(attachment: AttachmentEntity)

    @Query("DELETE FROM attachments WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
internal interface CommentDao {
    @Query("SELECT * FROM comments WHERE id = :id")
    suspend fun getById(id: String): CommentEntity?

    @Query("SELECT * FROM comments WHERE task_id = :taskId ORDER BY created_at ASC")
    fun observeByTask(taskId: String): Flow<List<CommentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(comment: CommentEntity)

    @Update
    suspend fun update(comment: CommentEntity)

    @Query("DELETE FROM comments WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
internal interface CommentReactionDao {
    @Query("SELECT * FROM comment_reactions WHERE comment_id = :commentId")
    fun observeByComment(commentId: String): Flow<List<CommentReactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(reaction: CommentReactionEntity)

    @Query(
        """
        DELETE FROM comment_reactions
        WHERE comment_id = :commentId AND reactor_device_id = :reactorDeviceId AND emoji = :emoji
        """,
    )
    suspend fun delete(commentId: String, reactorDeviceId: String, emoji: String)
}

@Dao
internal interface PomodoroSessionDao {
    @Query("SELECT * FROM pomodoro_sessions WHERE id = :id")
    suspend fun getById(id: String): PomodoroSessionEntity?

    @Query("SELECT * FROM pomodoro_sessions WHERE task_id = :taskId ORDER BY started_at DESC")
    fun observeByTask(taskId: String): Flow<List<PomodoroSessionEntity>>

    @Query("SELECT * FROM pomodoro_sessions ORDER BY started_at DESC")
    fun observeAll(): Flow<List<PomodoroSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(session: PomodoroSessionEntity)
}
