package com.os11.app.data.repository

import com.os11.app.data.db.OS11Database
import com.os11.app.data.db.entities.AreaEntity
import com.os11.app.data.db.entities.ProjectEntity
import com.os11.app.data.db.entities.SectionEntity
import com.os11.app.data.db.entities.WorkspaceEntity
import com.os11.app.data.sync.TrackedWriter
import kotlinx.coroutines.flow.Flow

interface WorkspaceRepository {
    fun observeDefault(): Flow<WorkspaceEntity?>
    suspend fun save(workspace: WorkspaceEntity): WorkspaceEntity
}

internal class WorkspaceRepositoryImpl(private val db: OS11Database, private val trackedWriter: TrackedWriter) :
    WorkspaceRepository {
    override fun observeDefault(): Flow<WorkspaceEntity?> = db.workspaceDao().observeDefault()
    override suspend fun save(workspace: WorkspaceEntity): WorkspaceEntity = trackedWriter.writeWorkspace(workspace)
}

interface AreaRepository {
    fun observeAll(workspaceId: String): Flow<List<AreaEntity>>
    suspend fun getById(id: String): AreaEntity?
    suspend fun save(area: AreaEntity): AreaEntity
    suspend fun delete(id: String)
}

internal class AreaRepositoryImpl(private val db: OS11Database, private val trackedWriter: TrackedWriter) :
    AreaRepository {
    override fun observeAll(workspaceId: String): Flow<List<AreaEntity>> = db.areaDao().observeAll(workspaceId)

    override suspend fun getById(id: String): AreaEntity? = db.areaDao().getById(id)

    override suspend fun save(area: AreaEntity): AreaEntity = trackedWriter.writeArea(area)

    override suspend fun delete(id: String) = trackedWriter.deleteArea(id)
}

interface ProjectRepository {
    fun observeAll(workspaceId: String): Flow<List<ProjectEntity>>
    fun observeByArea(areaId: String): Flow<List<ProjectEntity>>
    suspend fun getById(id: String): ProjectEntity?
    suspend fun save(project: ProjectEntity): ProjectEntity
    suspend fun delete(id: String)
}

internal class ProjectRepositoryImpl(private val db: OS11Database, private val trackedWriter: TrackedWriter) :
    ProjectRepository {
    override fun observeAll(workspaceId: String): Flow<List<ProjectEntity>> = db.projectDao().observeAll(workspaceId)

    override fun observeByArea(areaId: String): Flow<List<ProjectEntity>> = db.projectDao().observeByArea(areaId)

    override suspend fun getById(id: String): ProjectEntity? = db.projectDao().getById(id)

    override suspend fun save(project: ProjectEntity): ProjectEntity = trackedWriter.writeProject(project)

    override suspend fun delete(id: String) = trackedWriter.deleteProject(id)
}

interface SectionRepository {
    fun observeByProject(projectId: String): Flow<List<SectionEntity>>
    suspend fun getByProject(projectId: String): List<SectionEntity>
    suspend fun getById(id: String): SectionEntity?
    suspend fun save(section: SectionEntity): SectionEntity
    suspend fun delete(id: String)
}

internal class SectionRepositoryImpl(private val db: OS11Database, private val trackedWriter: TrackedWriter) :
    SectionRepository {
    override fun observeByProject(projectId: String): Flow<List<SectionEntity>> {
        val dao = db.sectionDao()
        return dao.observeByProject(projectId)
    }

    override suspend fun getByProject(projectId: String): List<SectionEntity> = db.sectionDao().getByProject(projectId)

    override suspend fun getById(id: String): SectionEntity? = db.sectionDao().getById(id)

    override suspend fun save(section: SectionEntity): SectionEntity = trackedWriter.writeSection(section)

    override suspend fun delete(id: String) = trackedWriter.deleteSection(id)
}
