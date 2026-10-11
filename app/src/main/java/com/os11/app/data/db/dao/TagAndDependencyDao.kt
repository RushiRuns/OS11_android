package com.os11.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.os11.app.data.db.entities.MilestoneEntity
import com.os11.app.data.db.entities.TagEntity
import com.os11.app.data.db.entities.TaskDependencyEntity
import com.os11.app.data.db.entities.TaskTagEntity
import kotlinx.coroutines.flow.Flow

@Dao
internal interface TagDao {
    @Query("SELECT * FROM tags WHERE id = :id")
    suspend fun getById(id: String): TagEntity?

    @Query("SELECT * FROM tags WHERE name = :name")
    suspend fun getByName(name: String): TagEntity?

    @Query("SELECT * FROM tags ORDER BY sort_order ASC, name ASC")
    fun observeAll(): Flow<List<TagEntity>>

    @Query("SELECT * FROM tags ORDER BY sort_order ASC, name ASC")
    suspend fun getAll(): List<TagEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(tag: TagEntity)

    @Update
    suspend fun update(tag: TagEntity)

    @Query("DELETE FROM tags WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM tags WHERE parent_tag_id = :parentId ORDER BY sort_order ASC, name ASC")
    fun observeChildTags(parentId: String): Flow<List<TagEntity>>

    @Query("SELECT * FROM tags WHERE parent_tag_id = :parentId")
    suspend fun getChildTags(parentId: String): List<TagEntity>
}

@Dao
@Suppress("TooManyFunctions")
internal interface TaskTagDao {
    @Query("SELECT tag_id FROM task_tags WHERE task_id = :taskId")
    suspend fun getTagIdsForTask(taskId: String): List<String>

    @Query(
        """
        SELECT t.* FROM tags t
        JOIN task_tags tt ON tt.tag_id = t.id
        WHERE tt.task_id = :taskId
        ORDER BY t.sort_order ASC, t.name ASC
        """,
    )
    fun observeTagsForTask(taskId: String): Flow<List<TagEntity>>

    @Query(
        """
        SELECT * FROM task_tags
        WHERE task_id IN (:taskIds)
        """,
    )
    suspend fun getTagsForTasks(taskIds: List<String>): List<TaskTagEntity>

    @Query(
        """
        SELECT * FROM task_tags
        WHERE task_id IN (:taskIds)
        """,
    )
    fun observeTagsForTasks(taskIds: List<String>): Flow<List<TaskTagEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(taskTag: TaskTagEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(taskTags: List<TaskTagEntity>)

    @Query("DELETE FROM task_tags WHERE task_id = :taskId AND tag_id = :tagId")
    suspend fun delete(taskId: String, tagId: String)

    @Query("DELETE FROM task_tags WHERE task_id = :taskId")
    suspend fun deleteByTask(taskId: String)

    @Query("SELECT task_id FROM task_tags WHERE tag_id = :tagId")
    suspend fun getTaskIdsForTag(tagId: String): List<String>

    @Query("SELECT COUNT(*) FROM task_tags WHERE tag_id = :tagId")
    fun observeTaskCountForTag(tagId: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM task_tags WHERE tag_id = :tagId")
    suspend fun getTaskCountForTag(tagId: String): Int

    @Query("SELECT * FROM task_tags")
    suspend fun getAll(): List<TaskTagEntity>
}

@Dao
internal interface TaskDependencyDao {
    @Query("SELECT depends_on_id FROM task_dependencies WHERE task_id = :taskId")
    suspend fun getDependenciesForTask(taskId: String): List<String>

    @Query("SELECT task_id, depends_on_id FROM task_dependencies")
    suspend fun getAllDependencyPairs(): List<TaskDependencyEntity>

    @Query("SELECT task_id, depends_on_id FROM task_dependencies")
    fun observeAllDependencyPairs(): Flow<List<TaskDependencyEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(dependency: TaskDependencyEntity)

    @Query("DELETE FROM task_dependencies WHERE task_id = :taskId AND depends_on_id = :dependsOnId")
    suspend fun delete(taskId: String, dependsOnId: String)
}

@Dao
internal interface MilestoneDao {
    @Query("SELECT * FROM milestones WHERE id = :id")
    suspend fun getById(id: String): MilestoneEntity?

    @Query("SELECT * FROM milestones ORDER BY due_date ASC, sort_order ASC")
    suspend fun getAll(): List<MilestoneEntity>

    @Query("SELECT * FROM milestones WHERE project_id = :projectId ORDER BY due_date ASC, sort_order ASC")
    fun observeByProject(projectId: String): Flow<List<MilestoneEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(milestone: MilestoneEntity)

    @Update
    suspend fun update(milestone: MilestoneEntity)

    @Query("DELETE FROM milestones WHERE id = :id")
    suspend fun deleteById(id: String)
}
