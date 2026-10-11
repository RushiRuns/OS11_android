package com.os11.app.data.repository

import com.os11.app.data.db.OS11Database
import com.os11.app.data.db.entities.AttachmentEntity
import com.os11.app.data.db.entities.CommentEntity
import com.os11.app.data.db.entities.GoalEntity
import com.os11.app.data.db.entities.PomodoroSessionEntity
import com.os11.app.data.db.entities.ReminderDeviceStateEntity
import com.os11.app.data.db.entities.ReminderEntity
import com.os11.app.data.sync.TrackedWriter
import kotlinx.coroutines.flow.Flow

interface ReminderRepository {
    fun observeByTask(taskId: String): Flow<List<ReminderEntity>>
    suspend fun getById(id: String): ReminderEntity?
    suspend fun getPendingReminders(): List<ReminderEntity>
    suspend fun getPendingReminders(nowIso: String): List<ReminderEntity>
    suspend fun save(reminder: ReminderEntity): ReminderEntity
    suspend fun delete(id: String)
    suspend fun getDeviceState(reminderId: String): ReminderDeviceStateEntity?
    suspend fun markTriggered(reminderId: String)
    suspend fun snoozeReminder(reminderId: String, snoozedUntilIso: String)
}

internal class ReminderRepositoryImpl(private val db: OS11Database, private val trackedWriter: TrackedWriter) :
    ReminderRepository {
    override fun observeByTask(taskId: String): Flow<List<ReminderEntity>> = db.reminderDao().observeByTask(taskId)

    override suspend fun getById(id: String): ReminderEntity? = db.reminderDao().getById(id)

    override suspend fun getPendingReminders(): List<ReminderEntity> = db.reminderDao().getPendingReminders()

    override suspend fun getPendingReminders(nowIso: String): List<ReminderEntity> =
        db.reminderDao().getPendingReminders(nowIso)

    override suspend fun save(reminder: ReminderEntity): ReminderEntity = trackedWriter.writeReminder(reminder)

    override suspend fun delete(id: String) = trackedWriter.deleteReminder(id)

    override suspend fun getDeviceState(reminderId: String): ReminderDeviceStateEntity? =
        db.reminderDeviceStateDao().getByReminderId(reminderId)

    override suspend fun markTriggered(reminderId: String) {
        val existing = db.reminderDeviceStateDao().getByReminderId(reminderId)
        val updated = existing?.copy(isTriggered = true, snoozedUntil = null)
            ?: ReminderDeviceStateEntity(reminderId = reminderId, isTriggered = true, snoozedUntil = null)
        trackedWriter.writeReminderDeviceState(updated)
    }

    override suspend fun snoozeReminder(reminderId: String, snoozedUntilIso: String) {
        val existing = db.reminderDeviceStateDao().getByReminderId(reminderId)
        val updated = existing?.copy(isTriggered = false, snoozedUntil = snoozedUntilIso)
            ?: ReminderDeviceStateEntity(reminderId = reminderId, isTriggered = false, snoozedUntil = snoozedUntilIso)
        trackedWriter.writeReminderDeviceState(updated)
    }
}

interface AttachmentRepository {
    fun observeByTask(taskId: String): Flow<List<AttachmentEntity>>
    suspend fun getById(id: String): AttachmentEntity?
    suspend fun save(attachment: AttachmentEntity): AttachmentEntity
    suspend fun delete(id: String)
}

internal class AttachmentRepositoryImpl(private val db: OS11Database, private val trackedWriter: TrackedWriter) :
    AttachmentRepository {
    override fun observeByTask(taskId: String): Flow<List<AttachmentEntity>> {
        val dao = db.attachmentDao()
        return dao.observeByTask(taskId)
    }

    override suspend fun getById(id: String): AttachmentEntity? = db.attachmentDao().getById(id)

    override suspend fun save(attachment: AttachmentEntity): AttachmentEntity {
        val written = trackedWriter.writeAttachment(attachment)
        return written
    }

    override suspend fun delete(id: String) = trackedWriter.deleteAttachment(id)
}

interface CommentRepository {
    fun observeByTask(taskId: String): Flow<List<CommentEntity>>
    suspend fun save(comment: CommentEntity): CommentEntity
}

internal class CommentRepositoryImpl(private val db: OS11Database, private val trackedWriter: TrackedWriter) :
    CommentRepository {
    override fun observeByTask(taskId: String): Flow<List<CommentEntity>> = db.commentDao().observeByTask(taskId)

    override suspend fun save(comment: CommentEntity): CommentEntity = trackedWriter.writeComment(comment)
}

interface PomodoroRepository {
    fun observeAll(): Flow<List<PomodoroSessionEntity>>
    fun observeByTask(taskId: String): Flow<List<PomodoroSessionEntity>>
    fun observeTodayCompletedCount(sinceIso: String): Flow<Int>
    fun observeTodayFocusSeconds(sinceIso: String): Flow<Int>
    suspend fun save(session: PomodoroSessionEntity): PomodoroSessionEntity
}

internal class PomodoroRepositoryImpl(private val db: OS11Database, private val trackedWriter: TrackedWriter) :
    PomodoroRepository {
    override fun observeAll(): Flow<List<PomodoroSessionEntity>> = db.pomodoroSessionDao().observeAll()

    override fun observeByTask(taskId: String): Flow<List<PomodoroSessionEntity>> {
        val dao = db.pomodoroSessionDao()
        return dao.observeByTask(taskId)
    }

    override fun observeTodayCompletedCount(sinceIso: String): Flow<Int> =
        db.pomodoroSessionDao().observeTodayCompletedCount(sinceIso)

    override fun observeTodayFocusSeconds(sinceIso: String): Flow<Int> =
        db.pomodoroSessionDao().observeTodayFocusSeconds(sinceIso)

    override suspend fun save(session: PomodoroSessionEntity): PomodoroSessionEntity {
        val written = trackedWriter.writePomodoroSession(session)
        return written
    }
}

interface GoalRepository {
    fun observeAll(): Flow<List<GoalEntity>>
    suspend fun getById(id: String): GoalEntity?
    suspend fun save(goal: GoalEntity): GoalEntity
    suspend fun delete(id: String)
}

internal class GoalRepositoryImpl(private val db: OS11Database, private val trackedWriter: TrackedWriter) :
    GoalRepository {
    override fun observeAll(): Flow<List<GoalEntity>> = db.goalDao().observeAll()

    override suspend fun getById(id: String): GoalEntity? = db.goalDao().getById(id)

    override suspend fun save(goal: GoalEntity): GoalEntity = trackedWriter.writeGoal(goal)

    override suspend fun delete(id: String) = trackedWriter.deleteGoal(id)
}
