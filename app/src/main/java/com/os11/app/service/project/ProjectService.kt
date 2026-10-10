package com.os11.app.service.project

import com.os11.app.data.db.entities.MilestoneEntity
import com.os11.app.data.db.entities.ProjectEntity
import com.os11.app.data.db.entities.SectionEntity
import com.os11.app.data.repository.DependencyRepository
import com.os11.app.data.repository.MilestoneRepository
import com.os11.app.data.repository.ProjectRepository
import com.os11.app.data.repository.SectionRepository
import com.os11.app.data.repository.TaskRepository
import com.os11.app.di.IoDispatcher
import com.os11.app.service.undo.UndoManager
import com.os11.app.service.undo.UndoToken
import com.os11.domain.id.IdGenerator
import com.os11.domain.logic.validation.DependencyCycleCheck
import com.os11.domain.result.AppError
import com.os11.domain.result.AppResult
import com.os11.domain.time.Clock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@Suppress("TooManyFunctions", "TooGenericExceptionCaught", "LongParameterList")
class ProjectService @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val sectionRepository: SectionRepository,
    private val milestoneRepository: MilestoneRepository,
    private val dependencyRepository: DependencyRepository,
    private val taskRepository: TaskRepository,
    private val undoManager: UndoManager,
    private val clock: Clock,
    private val idGenerator: IdGenerator,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    fun observeAll(workspaceId: String): Flow<List<ProjectEntity>> = projectRepository.observeAll(workspaceId)
    fun observeByArea(areaId: String): Flow<List<ProjectEntity>> = projectRepository.observeByArea(areaId)
    suspend fun getById(id: String): ProjectEntity? = projectRepository.getById(id)

    fun observeProjectItems(workspaceId: String): Flow<List<ProjectItem>> =
        projectRepository.observeAll(workspaceId).map { list ->
            list.map { it.toItem() }
        }

    fun observeProjectItemsByArea(areaId: String): Flow<List<ProjectItem>> =
        projectRepository.observeByArea(areaId).map { list ->
            list.map { it.toItem() }
        }

    suspend fun getProjectItem(id: String): ProjectItem? = projectRepository.getById(id)?.toItem()

    suspend fun updateProjectStatus(id: String, status: String): AppResult<Unit> = withContext(ioDispatcher) {
        try {
            val existing = projectRepository.getById(id)
                ?: return@withContext AppResult.err(AppError.NotFound)
            val updated = existing.copy(
                status = status,
                updatedAt = clock.nowIso(),
            )
            projectRepository.save(updated)
            AppResult.ok(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppResult.err(AppError.Unexpected(e))
        }
    }

    suspend fun updateDefaultView(id: String, defaultView: String): AppResult<Unit> = withContext(ioDispatcher) {
        try {
            val existing = projectRepository.getById(id)
                ?: return@withContext AppResult.err(AppError.NotFound)
            val updated = existing.copy(
                defaultView = defaultView,
                updatedAt = clock.nowIso(),
            )
            projectRepository.save(updated)
            AppResult.ok(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppResult.err(AppError.Unexpected(e))
        }
    }

    suspend fun archiveProject(id: String): AppResult<UndoToken> = withContext(ioDispatcher) {
        try {
            val existing = projectRepository.getById(id)
                ?: return@withContext AppResult.err(AppError.NotFound)
            val previousStatus = existing.status
            val updated = existing.copy(
                status = "archived",
                updatedAt = clock.nowIso(),
            )
            projectRepository.save(updated)

            val token = undoManager.registerUndo("Project archived") {
                val current = projectRepository.getById(id)
                    ?: return@registerUndo AppResult.err(AppError.NotFound)
                projectRepository.save(current.copy(status = previousStatus, updatedAt = clock.nowIso()))
                AppResult.ok(Unit)
            }
            AppResult.ok(token)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppResult.err(AppError.Unexpected(e))
        }
    }

    suspend fun unarchiveProject(id: String): AppResult<Unit> = withContext(ioDispatcher) {
        updateProjectStatus(id, "active")
    }

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

    // --- Sections ---

    fun observeSections(projectId: String): Flow<List<SectionItem>> =
        sectionRepository.observeByProject(projectId).map { list ->
            list.map { it.toItem() }
        }

    suspend fun createSection(projectId: String, name: String): AppResult<SectionItem> = withContext(ioDispatcher) {
        if (name.trim().isEmpty()) {
            return@withContext AppResult.err(AppError.Validation("name", "Section name cannot be blank"))
        }
        try {
            val existing = sectionRepository.getByProject(projectId)
            val nextSortOrder = (existing.size + 1) * SECTION_SORT_ORDER_STEP
            val now = clock.nowIso()
            val section = SectionEntity(
                id = idGenerator.randomId(),
                projectId = projectId,
                name = name.trim(),
                sortOrder = nextSortOrder,
                isCollapsed = false,
                createdAt = now,
                updatedAt = now,
            )
            val saved = sectionRepository.save(section)
            AppResult.ok(saved.toItem())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppResult.err(AppError.Unexpected(e))
        }
    }

    suspend fun renameSection(sectionId: String, newName: String): AppResult<Unit> = withContext(ioDispatcher) {
        if (newName.trim().isEmpty()) {
            return@withContext AppResult.err(AppError.Validation("name", "Section name cannot be blank"))
        }
        try {
            val existing = sectionRepository.getById(sectionId)
                ?: return@withContext AppResult.err(AppError.NotFound)
            val updated = existing.copy(name = newName.trim(), updatedAt = clock.nowIso())
            sectionRepository.save(updated)
            AppResult.ok(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppResult.err(AppError.Unexpected(e))
        }
    }

    suspend fun toggleSectionCollapse(sectionId: String, isCollapsed: Boolean): AppResult<Unit> =
        withContext(ioDispatcher) {
            try {
                val existing = sectionRepository.getById(sectionId)
                    ?: return@withContext AppResult.err(AppError.NotFound)
                val updated = existing.copy(isCollapsed = isCollapsed, updatedAt = clock.nowIso())
                sectionRepository.save(updated)
                AppResult.ok(Unit)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppResult.err(AppError.Unexpected(e))
            }
        }

    suspend fun reorderSections(projectId: String, sectionIds: List<String>): AppResult<Unit> =
        withContext(ioDispatcher) {
            try {
                val sections = sectionRepository.getByProject(projectId).associateBy { it.id }
                val now = clock.nowIso()
                sectionIds.forEachIndexed { index, id ->
                    val section = sections[id]
                    if (section != null) {
                        val updated = section.copy(sortOrder = (index + 1) * 10.0, updatedAt = now)
                        sectionRepository.save(updated)
                    }
                }
                AppResult.ok(Unit)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppResult.err(AppError.Unexpected(e))
            }
        }

    suspend fun deleteSection(sectionId: String): AppResult<UndoToken> = withContext(ioDispatcher) {
        try {
            val existing = sectionRepository.getById(sectionId)
                ?: return@withContext AppResult.err(AppError.NotFound)
            val linkedTasks = taskRepository.getTasksBySection(sectionId)

            // Unlink tasks before deleting section
            val now = clock.nowIso()
            for (task in linkedTasks) {
                taskRepository.saveTask(task.copy(sectionId = null, updatedAt = now))
            }
            sectionRepository.delete(sectionId)

            val token = undoManager.registerUndo("Section deleted") {
                sectionRepository.save(existing)
                for (task in linkedTasks) {
                    taskRepository.saveTask(task.copy(sectionId = existing.id, updatedAt = clock.nowIso()))
                }
                AppResult.ok(Unit)
            }
            AppResult.ok(token)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppResult.err(AppError.Unexpected(e))
        }
    }

    // --- Milestones ---

    fun observeMilestones(projectId: String): Flow<List<MilestoneItem>> =
        milestoneRepository.observeByProject(projectId).map { list ->
            list.map { it.toItem() }
        }

    suspend fun createMilestone(projectId: String, title: String, dueDate: String): AppResult<MilestoneItem> =
        withContext(ioDispatcher) {
            if (title.trim().isEmpty()) {
                return@withContext AppResult.err(AppError.Validation("title", "Milestone title cannot be blank"))
            }
            try {
                val milestone = MilestoneEntity(
                    id = idGenerator.randomId(),
                    projectId = projectId,
                    title = title.trim(),
                    dueDate = dueDate,
                    createdAt = clock.nowIso(),
                )
                val saved = milestoneRepository.save(milestone)
                AppResult.ok(saved.toItem())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppResult.err(AppError.Unexpected(e))
            }
        }

    suspend fun toggleMilestoneCompleted(milestoneId: String, isCompleted: Boolean): AppResult<Unit> =
        withContext(ioDispatcher) {
            try {
                val existing = milestoneRepository.getById(milestoneId)
                    ?: return@withContext AppResult.err(AppError.NotFound)
                milestoneRepository.save(existing.copy(isCompleted = isCompleted))
                AppResult.ok(Unit)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppResult.err(AppError.Unexpected(e))
            }
        }

    suspend fun deleteMilestone(milestoneId: String): AppResult<UndoToken> = withContext(ioDispatcher) {
        try {
            val existing = milestoneRepository.getById(milestoneId)
                ?: return@withContext AppResult.err(AppError.NotFound)
            milestoneRepository.delete(milestoneId)
            val token = undoManager.registerUndo("Milestone deleted") {
                milestoneRepository.save(existing)
                AppResult.ok(Unit)
            }
            AppResult.ok(token)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppResult.err(AppError.Unexpected(e))
        }
    }

    // --- Dependencies ---

    fun observeDependencies(): Flow<List<TaskDependencyItem>> =
        dependencyRepository.observeAllDependencyPairs().map { list ->
            list.map { TaskDependencyItem(it.first, it.second) }
        }

    suspend fun addDependency(taskId: String, dependsOnId: String): AppResult<Unit> = withContext(ioDispatcher) {
        if (taskId == dependsOnId) {
            return@withContext AppResult.err(
                AppError.Validation("dependency", "A task cannot depend on itself"),
            )
        }
        try {
            val existingPairs = dependencyRepository.getAllDependencyPairs()
            if (DependencyCycleCheck.wouldCreateCycle(existingPairs, taskId, dependsOnId)) {
                return@withContext AppResult.err(
                    AppError.Validation(
                        "dependency",
                        "Cannot add dependency: circular dependency loop detected between tasks.",
                    ),
                )
            }
            dependencyRepository.addDependency(taskId, dependsOnId)
            AppResult.ok(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppResult.err(AppError.Unexpected(e))
        }
    }

    suspend fun removeDependency(taskId: String, dependsOnId: String): AppResult<Unit> = withContext(ioDispatcher) {
        try {
            dependencyRepository.removeDependency(taskId, dependsOnId)
            AppResult.ok(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppResult.err(AppError.Unexpected(e))
        }
    }

    // --- CSV Export ---

    suspend fun exportProjectToCsv(projectId: String): AppResult<String> = withContext(ioDispatcher) {
        try {
            val project = projectRepository.getById(projectId)
                ?: return@withContext AppResult.err(AppError.NotFound)
            val tasks = taskRepository.observeByProjectSlim(projectId).first()
            val sections = sectionRepository.getByProject(projectId).associate { it.id to it.name }

            val builder = StringBuilder()
            builder.append("Project: ").append(project.name).append("\n")
            builder.append("Status: ").append(project.status).append("\n\n")
            builder.append("ID,Title,Section,Priority,Due Date,Due Time,Completed,Completed At,Created At\n")

            for (task in tasks) {
                val sectionName = task.sectionId?.let { sections[it] } ?: ""
                val row = listOf(
                    task.id,
                    task.title,
                    sectionName,
                    task.priority.toString(),
                    task.dueDate ?: "",
                    task.dueTime ?: "",
                    task.isCompleted.toString(),
                    task.completedAt ?: "",
                    task.createdAt,
                ).joinToString(",") { escapeCsv(it) }
                builder.append(row).append("\n")
            }
            AppResult.ok(builder.toString())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppResult.err(AppError.Unexpected(e))
        }
    }

    private fun escapeCsv(value: String): String {
        val escaped = value.replace("\"", "\"\"")
        return "\"$escaped\""
    }

    companion object {
        private const val SECTION_SORT_ORDER_STEP = 10.0
    }
}

private fun ProjectEntity.toItem() = ProjectItem(
    id = id,
    workspaceId = workspaceId,
    areaId = areaId,
    name = name,
    description = description,
    color = color,
    icon = icon,
    status = status,
    dueDate = dueDate,
    defaultView = defaultView,
    isSomeday = isSomeday,
    sortOrder = sortOrder,
)
