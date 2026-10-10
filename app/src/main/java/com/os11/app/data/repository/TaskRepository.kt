package com.os11.app.data.repository

import com.os11.app.data.db.OS11Database
import com.os11.app.data.db.dao.SlimTask
import com.os11.app.data.db.dao.TaskDao
import com.os11.app.data.db.entities.TaskEntity
import com.os11.app.data.sync.TrackedWriter
import com.os11.domain.time.Clock
import com.os11.domain.time.SystemClock
import kotlinx.coroutines.flow.Flow

@Suppress("TooManyFunctions")
interface TaskRepository {
    fun observeById(id: String): Flow<TaskEntity?>
    suspend fun getById(id: String): TaskEntity?
    fun observeAllSlim(): Flow<List<SlimTask>>
    fun observeInboxSlim(): Flow<List<SlimTask>>
    fun observeByProjectSlim(projectId: String): Flow<List<SlimTask>>
    fun observeByAreaSlim(areaId: String): Flow<List<SlimTask>>
    fun observeSubtasksSlim(parentId: String): Flow<List<SlimTask>>
    fun observeMyDaySlim(date: String): Flow<List<SlimTask>>
    fun observeImportantSlim(): Flow<List<SlimTask>>
    fun observePlannedSlim(): Flow<List<SlimTask>>
    fun observeCompletedSlim(): Flow<List<SlimTask>>
    fun observeMyDaySuggestions(today: String): Flow<List<SlimTask>>
    fun observeTrashSlim(): Flow<List<SlimTask>>
    fun observeTasksByTagSlim(tagId: String): Flow<List<SlimTask>>
    fun observeActiveTaskCount(): Flow<Int>

    suspend fun saveTask(task: TaskEntity): TaskEntity
    suspend fun trashTask(id: String)
    suspend fun restoreTask(id: String)
    suspend fun deletePermanently(id: String)
    suspend fun getTrashedTasks(): List<TaskEntity>
    suspend fun getTrashedTasksOlderThan(cutoffIso: String): List<TaskEntity>
    suspend fun getSubtasks(parentId: String): List<TaskEntity>
    suspend fun getTasksBySection(sectionId: String): List<TaskEntity>
    suspend fun getIncompleteMyDayBefore(today: String): List<TaskEntity>
}

@Suppress("TooManyFunctions")
internal class TaskRepositoryImpl(
    private val db: OS11Database,
    private val trackedWriter: TrackedWriter,
    private val clock: Clock = SystemClock(),
) : TaskRepository {

    private val taskDao: TaskDao get() = db.taskDao()

    override fun observeById(id: String): Flow<TaskEntity?> = taskDao.observeById(id)

    override suspend fun getById(id: String): TaskEntity? = taskDao.getById(id)

    override fun observeAllSlim(): Flow<List<SlimTask>> = taskDao.observeAllSlim()

    override fun observeInboxSlim(): Flow<List<SlimTask>> = taskDao.observeInboxSlim()

    override fun observeByProjectSlim(projectId: String): Flow<List<SlimTask>> = taskDao.observeByProjectSlim(projectId)

    override fun observeByAreaSlim(areaId: String): Flow<List<SlimTask>> = taskDao.observeByAreaSlim(areaId)

    override fun observeSubtasksSlim(parentId: String): Flow<List<SlimTask>> = taskDao.observeSubtasksSlim(parentId)

    override fun observeMyDaySlim(date: String): Flow<List<SlimTask>> = taskDao.observeMyDaySlim(date)

    override fun observeImportantSlim(): Flow<List<SlimTask>> = taskDao.observeImportantSlim()

    override fun observePlannedSlim(): Flow<List<SlimTask>> = taskDao.observePlannedSlim()

    override fun observeCompletedSlim(): Flow<List<SlimTask>> = taskDao.observeCompletedSlim()

    override fun observeMyDaySuggestions(today: String): Flow<List<SlimTask>> = taskDao.observeMyDaySuggestions(today)

    override fun observeTrashSlim(): Flow<List<SlimTask>> = taskDao.observeTrashSlim()

    override fun observeTasksByTagSlim(tagId: String): Flow<List<SlimTask>> = taskDao.observeTasksByTagSlim(tagId)

    override fun observeActiveTaskCount(): Flow<Int> = taskDao.observeActiveTaskCount()

    override suspend fun getIncompleteMyDayBefore(today: String): List<TaskEntity> =
        taskDao.getIncompleteMyDayBefore(today)

    override suspend fun saveTask(task: TaskEntity): TaskEntity = trackedWriter.writeTask(task)

    override suspend fun trashTask(id: String) {
        val existing = getById(id) ?: return
        val trashed = existing.copy(
            isTrashed = true,
            trashedAt = clock.nowIso(),
            updatedAt = clock.nowIso(),
        )
        trackedWriter.writeTask(trashed)
    }

    override suspend fun restoreTask(id: String) {
        val existing = getById(id) ?: return
        val restored = existing.copy(
            isTrashed = false,
            trashedAt = null,
            updatedAt = clock.nowIso(),
        )
        trackedWriter.writeTask(restored)
    }

    override suspend fun deletePermanently(id: String) {
        trackedWriter.deleteTask(id)
    }

    override suspend fun getTrashedTasks(): List<TaskEntity> = taskDao.getTrashedTasks()

    override suspend fun getTrashedTasksOlderThan(cutoffIso: String): List<TaskEntity> =
        taskDao.getTrashedTasksOlderThan(cutoffIso)

    override suspend fun getSubtasks(parentId: String): List<TaskEntity> = taskDao.getSubtasks(parentId)

    override suspend fun getTasksBySection(sectionId: String): List<TaskEntity> = taskDao.getTasksBySection(sectionId)
}
