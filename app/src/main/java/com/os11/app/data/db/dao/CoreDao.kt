package com.os11.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.os11.app.data.db.entities.AreaEntity
import com.os11.app.data.db.entities.LocalIdentityEntity
import com.os11.app.data.db.entities.ProjectEntity
import com.os11.app.data.db.entities.SectionEntity
import com.os11.app.data.db.entities.WorkspaceEntity
import kotlinx.coroutines.flow.Flow

@Dao
internal interface LocalIdentityDao {
    @Query("SELECT * FROM local_identity LIMIT 1")
    suspend fun get(): LocalIdentityEntity?

    @Query("SELECT * FROM local_identity LIMIT 1")
    fun observe(): Flow<LocalIdentityEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(identity: LocalIdentityEntity)
}

@Dao
internal interface WorkspaceDao {
    @Query("SELECT * FROM workspaces WHERE id = :id")
    suspend fun getById(id: String): WorkspaceEntity?

    @Query("SELECT * FROM workspaces")
    suspend fun getAll(): List<WorkspaceEntity>

    @Query("SELECT * FROM workspaces LIMIT 1")
    fun observeDefault(): Flow<WorkspaceEntity?>

    // @Upsert, not REPLACE: REPLACE would cascade-delete every area, project and task in the workspace.
    @Upsert
    suspend fun upsert(workspace: WorkspaceEntity)

    @Update
    suspend fun update(workspace: WorkspaceEntity)
}

@Dao
internal interface AreaDao {
    @Query("SELECT * FROM areas WHERE id = :id")
    suspend fun getById(id: String): AreaEntity?

    @Query("SELECT * FROM areas ORDER BY sort_order ASC, id ASC")
    suspend fun getAll(): List<AreaEntity>

    @Query("SELECT * FROM areas WHERE workspace_id = :workspaceId ORDER BY sort_order ASC, id ASC")
    fun observeAll(workspaceId: String): Flow<List<AreaEntity>>

    // @Upsert, not REPLACE: REPLACE would null out area_id on every project and task in the area.
    @Upsert
    suspend fun upsert(area: AreaEntity)

    @Update
    suspend fun update(area: AreaEntity)

    @Query("DELETE FROM areas WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
internal interface ProjectDao {
    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun getById(id: String): ProjectEntity?

    @Query("SELECT * FROM projects ORDER BY sort_order ASC, id ASC")
    suspend fun getAll(): List<ProjectEntity>

    @Query("SELECT * FROM projects WHERE workspace_id = :workspaceId ORDER BY sort_order ASC, id ASC")
    fun observeAll(workspaceId: String): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE area_id = :areaId ORDER BY sort_order ASC, id ASC")
    fun observeByArea(areaId: String): Flow<List<ProjectEntity>>

    // @Upsert, not REPLACE: REPLACE would delete its sections and milestones and null out project_id on its tasks.
    @Upsert
    suspend fun upsert(project: ProjectEntity)

    @Update
    suspend fun update(project: ProjectEntity)

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
internal interface SectionDao {
    @Query("SELECT * FROM sections WHERE id = :id")
    suspend fun getById(id: String): SectionEntity?

    @Query("SELECT * FROM sections ORDER BY sort_order ASC, id ASC")
    suspend fun getAll(): List<SectionEntity>

    @Query("SELECT * FROM sections WHERE project_id = :projectId ORDER BY sort_order ASC, id ASC")
    fun observeByProject(projectId: String): Flow<List<SectionEntity>>

    @Query("SELECT * FROM sections WHERE project_id = :projectId ORDER BY sort_order ASC, id ASC")
    suspend fun getByProject(projectId: String): List<SectionEntity>

    // @Upsert, not REPLACE: REPLACE would null out section_id on every task in the section.
    @Upsert
    suspend fun upsert(section: SectionEntity)

    @Update
    suspend fun update(section: SectionEntity)

    @Query("DELETE FROM sections WHERE id = :id")
    suspend fun deleteById(id: String)
}
