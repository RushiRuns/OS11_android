package com.os11.app.data.repository

import com.os11.app.data.db.OS11Database
import com.os11.app.data.db.entities.MilestoneEntity
import com.os11.app.data.db.entities.TagEntity
import com.os11.app.data.db.entities.TaskTagEntity
import com.os11.app.data.sync.TrackedWriter
import kotlinx.coroutines.flow.Flow

interface TagRepository {
    fun observeAll(): Flow<List<TagEntity>>
    suspend fun getById(id: String): TagEntity?
    suspend fun getByName(name: String): TagEntity?
    fun observeTagsForTask(taskId: String): Flow<List<TagEntity>>
    suspend fun getTagsForTasks(taskIds: List<String>): List<TaskTagEntity>
    fun observeTagsForTasks(taskIds: List<String>): Flow<List<TaskTagEntity>>

    suspend fun save(tag: TagEntity): TagEntity
    suspend fun delete(id: String)
    suspend fun addTagToTask(taskId: String, tagId: String)
    suspend fun removeTagFromTask(taskId: String, tagId: String)
}

internal class TagRepositoryImpl(private val db: OS11Database, private val trackedWriter: TrackedWriter) :
    TagRepository {
    override fun observeAll(): Flow<List<TagEntity>> = db.tagDao().observeAll()

    override suspend fun getById(id: String): TagEntity? = db.tagDao().getById(id)

    override suspend fun getByName(name: String): TagEntity? = db.tagDao().getByName(name)

    override fun observeTagsForTask(taskId: String): Flow<List<TagEntity>> {
        val dao = db.taskTagDao()
        return dao.observeTagsForTask(taskId)
    }

    override suspend fun getTagsForTasks(taskIds: List<String>): List<TaskTagEntity> {
        val dao = db.taskTagDao()
        return dao.getTagsForTasks(taskIds)
    }

    override fun observeTagsForTasks(taskIds: List<String>): Flow<List<TaskTagEntity>> {
        val dao = db.taskTagDao()
        return dao.observeTagsForTasks(taskIds)
    }

    override suspend fun save(tag: TagEntity): TagEntity = trackedWriter.writeTag(tag)

    override suspend fun delete(id: String) = trackedWriter.deleteTag(id)

    override suspend fun addTagToTask(taskId: String, tagId: String) = trackedWriter.writeTaskTag(taskId, tagId)

    override suspend fun removeTagFromTask(taskId: String, tagId: String) = trackedWriter.deleteTaskTag(taskId, tagId)
}

interface DependencyRepository {
    suspend fun getDependenciesForTask(taskId: String): List<String>
    suspend fun getAllDependencyPairs(): List<Pair<String, String>>
    suspend fun addDependency(taskId: String, dependsOnId: String)
    suspend fun removeDependency(taskId: String, dependsOnId: String)
}

internal class DependencyRepositoryImpl(private val db: OS11Database, private val trackedWriter: TrackedWriter) :
    DependencyRepository {
    override suspend fun getDependenciesForTask(taskId: String): List<String> {
        val dao = db.taskDependencyDao()
        return dao.getDependenciesForTask(taskId)
    }

    override suspend fun getAllDependencyPairs(): List<Pair<String, String>> {
        val dao = db.taskDependencyDao()
        return dao.getAllDependencyPairs().map { it.taskId to it.dependsOnId }
    }

    override suspend fun addDependency(taskId: String, dependsOnId: String) {
        trackedWriter.writeDependency(taskId, dependsOnId)
    }

    override suspend fun removeDependency(taskId: String, dependsOnId: String) {
        trackedWriter.deleteDependency(taskId, dependsOnId)
    }
}

interface MilestoneRepository {
    fun observeByProject(projectId: String): Flow<List<MilestoneEntity>>
    suspend fun getById(id: String): MilestoneEntity?
    suspend fun save(milestone: MilestoneEntity): MilestoneEntity
    suspend fun delete(id: String)
}

internal class MilestoneRepositoryImpl(private val db: OS11Database, private val trackedWriter: TrackedWriter) :
    MilestoneRepository {
    override fun observeByProject(projectId: String): Flow<List<MilestoneEntity>> {
        val dao = db.milestoneDao()
        return dao.observeByProject(projectId)
    }

    override suspend fun getById(id: String): MilestoneEntity? = db.milestoneDao().getById(id)

    override suspend fun save(milestone: MilestoneEntity): MilestoneEntity = trackedWriter.writeMilestone(milestone)

    override suspend fun delete(id: String) = trackedWriter.deleteMilestone(id)
}
