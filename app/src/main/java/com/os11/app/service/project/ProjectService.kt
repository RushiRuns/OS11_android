package com.os11.app.service.project

import com.os11.app.data.db.entities.ProjectEntity
import com.os11.app.data.repository.ProjectRepository
import com.os11.app.di.IoDispatcher
import com.os11.app.service.undo.UndoManager
import com.os11.app.service.undo.UndoToken
import com.os11.domain.id.IdGenerator
import com.os11.domain.result.AppError
import com.os11.domain.result.AppResult
import com.os11.domain.time.Clock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@Suppress("TooGenericExceptionCaught")
class ProjectService @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val undoManager: UndoManager,
    private val clock: Clock,
    private val idGenerator: IdGenerator,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    fun observeAll(workspaceId: String): Flow<List<ProjectEntity>> = projectRepository.observeAll(workspaceId)
    fun observeByArea(areaId: String): Flow<List<ProjectEntity>> = projectRepository.observeByArea(areaId)
    suspend fun getById(id: String): ProjectEntity? = projectRepository.getById(id)

    suspend fun createProject(
        name: String,
        workspaceId: String,
        areaId: String? = null,
        color: String? = null,
    ): AppResult<ProjectEntity> = withContext(ioDispatcher) {
        if (name.trim().isEmpty()) {
            return@withContext AppResult.err(AppError.Validation("name", "Project name cannot be blank"))
        }

        try {
            val now = clock.nowIso()
            val project = ProjectEntity(
                id = idGenerator.randomId(),
                name = name.trim(),
                workspaceId = workspaceId,
                areaId = areaId,
                color = color,
                createdAt = now,
                updatedAt = now,
            )
            val saved = projectRepository.save(project)
            AppResult.ok(saved)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppResult.err(AppError.Unexpected(e))
        }
    }

    suspend fun deleteProject(id: String): AppResult<UndoToken> = withContext(ioDispatcher) {
        try {
            val existing = projectRepository.getById(id)
                ?: return@withContext AppResult.err(AppError.NotFound)

            projectRepository.delete(id)

            val token = undoManager.registerUndo("Project deleted") {
                projectRepository.save(existing)
                AppResult.ok(Unit)
            }
            AppResult.ok(token)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppResult.err(AppError.Unexpected(e))
        }
    }
}
