package com.os11.app.service.task

import com.os11.app.data.db.dao.SlimTask
import com.os11.app.data.db.entities.TaskEntity
import com.os11.app.data.repository.CountsRepository
import com.os11.app.data.repository.TaskRepository
import com.os11.app.di.IoDispatcher
import com.os11.app.service.note.NoteSanitizer
import com.os11.app.service.undo.UndoManager
import com.os11.app.service.undo.UndoToken
import com.os11.domain.id.IdGenerator
import com.os11.domain.logic.validation.TaskValidation
import com.os11.domain.logic.validation.TaskValidationInput
import com.os11.domain.result.AppError
import com.os11.domain.result.AppResult
import com.os11.domain.time.Clock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Subtask completion statistics for task lists and task detail headers.
 */
data class SubtaskProgress(val completed: Int, val total: Int) {
    val formatted: String get() = "$completed/$total"
}

@Singleton
@Suppress("TooManyFunctions", "TooGenericExceptionCaught", "LargeClass")
class TaskService @Inject constructor(
    private val taskRepository: TaskRepository,
    private val countsRepository: CountsRepository,
    private val undoManager: UndoManager,
    private val clock: Clock,
    private val idGenerator: IdGenerator,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    fun observeById(id: String): Flow<TaskEntity?> = taskRepository.observeById(id)
    fun observeAllSlim(): Flow<List<SlimTask>> = taskRepository.observeAllSlim()
    fun observeInboxSlim(): Flow<List<SlimTask>> = taskRepository.observeInboxSlim()
    fun observeMyDaySlim(date: String = clock.today()): Flow<List<SlimTask>> = taskRepository.observeMyDaySlim(date)
    fun observeByProjectSlim(projectId: String): Flow<List<SlimTask>> = taskRepository.observeByProjectSlim(projectId)
    fun observeByAreaSlim(areaId: String): Flow<List<SlimTask>> = taskRepository.observeByAreaSlim(areaId)
    fun observeSubtasksSlim(parentId: String): Flow<List<SlimTask>> = taskRepository.observeSubtasksSlim(parentId)
    fun observeTrashSlim(): Flow<List<SlimTask>> = taskRepository.observeTrashSlim()
    fun observeActiveTaskCount(): Flow<Int> = taskRepository.observeActiveTaskCount()

    fun observeInboxTasks(): Flow<List<TaskItem>> =
        taskRepository.observeInboxSlim().map { list -> list.map { it.toTaskItem() } }

    fun observeAllTasks(): Flow<List<TaskItem>> =
        taskRepository.observeAllSlim().map { list -> list.map { it.toTaskItem() } }

    fun observeTrashTasks(): Flow<List<TaskItem>> =
        taskRepository.observeTrashSlim().map { list -> list.map { it.toTaskItem() } }

    fun observeImportantTasks(): Flow<List<TaskItem>> =
        taskRepository.observeImportantSlim().map { list -> list.map { it.toTaskItem() } }

    fun observePlannedTasks(): Flow<List<TaskItem>> =
        taskRepository.observePlannedSlim().map { list -> list.map { it.toTaskItem() } }

    fun observeCompletedTasks(): Flow<List<TaskItem>> =
        taskRepository.observeCompletedSlim().map { list -> list.map { it.toTaskItem() } }

    fun observeMyDayTasks(date: String = clock.today()): Flow<List<TaskItem>> =
        taskRepository.observeMyDaySlim(date).map { list -> list.map { it.toTaskItem() } }

    fun observeMyDaySuggestions(today: String = clock.today()): Flow<List<TaskItem>> =
        taskRepository.observeMyDaySuggestions(today).map { list -> list.map { it.toTaskItem() } }

    fun observeTasksByProject(projectId: String): Flow<List<TaskItem>> =
        taskRepository.observeByProjectSlim(projectId).map { list -> list.map { it.toTaskItem() } }

    fun observeTasksByArea(areaId: String): Flow<List<TaskItem>> =
        taskRepository.observeByAreaSlim(areaId).map { list -> list.map { it.toTaskItem() } }

    fun observeBrowseCounts(today: String = clock.today()): Flow<BrowseCountsItem> =
        countsRepository.observeBrowseCounts(today).map { counts ->
            BrowseCountsItem(
                inboxCount = counts.inboxCount,
                myDayCount = counts.myDayCount,
                plannedCount = counts.plannedCount,
                importantCount = counts.importantCount,
                anytimeCount = counts.anytimeCount,
                somedayCount = counts.somedayCount,
                waitingForCount = counts.waitingForCount,
                completedCount = counts.completedCount,
                trashCount = counts.trashCount,
            )
        }

    fun observeSubtasks(parentId: String): Flow<List<TaskItem>> =
        taskRepository.observeSubtasksSlim(parentId).map { list -> list.map { it.toTaskItem() } }

    suspend fun addToMyDay(taskId: String, date: String = clock.today()): AppResult<UndoToken> =
        withContext(ioDispatcher) {
            try {
                val existing = taskRepository.getById(taskId)
                    ?: return@withContext AppResult.err(AppError.NotFound)
                val previousDate = existing.myDayDate
                val updated = existing.copy(
                    myDayDate = date,
                    updatedAt = clock.nowIso(),
                )
                taskRepository.saveTask(updated)
                val token = undoManager.registerUndo("Added to My Day") {
                    val current = taskRepository.getById(taskId)
                    if (current != null) {
                        taskRepository.saveTask(current.copy(myDayDate = previousDate, updatedAt = clock.nowIso()))
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

    suspend fun removeFromMyDay(taskId: String): AppResult<UndoToken> = withContext(ioDispatcher) {
        try {
            val existing = taskRepository.getById(taskId)
                ?: return@withContext AppResult.err(AppError.NotFound)
            val previousDate = existing.myDayDate
            val updated = existing.copy(
                myDayDate = null,
                updatedAt = clock.nowIso(),
            )
            taskRepository.saveTask(updated)
            val token = undoManager.registerUndo("Removed from My Day") {
                val current = taskRepository.getById(taskId)
                if (current != null) {
                    taskRepository.saveTask(current.copy(myDayDate = previousDate, updatedAt = clock.nowIso()))
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

    suspend fun getIncompleteYesterdayMyDay(today: String = clock.today()): List<TaskItem> = withContext(ioDispatcher) {
        taskRepository.getIncompleteMyDayBefore(today).map { it.toTaskItem() }
    }

    suspend fun performRollover(uncompletedTaskIds: List<String>, targetDate: String?): AppResult<Unit> =
        withContext(ioDispatcher) {
            try {
                val nowIso = clock.nowIso()
                for (id in uncompletedTaskIds) {
                    val task = taskRepository.getById(id) ?: continue
                    taskRepository.saveTask(
                        task.copy(
                            myDayDate = targetDate,
                            updatedAt = nowIso,
                        ),
                    )
                }
                AppResult.ok(Unit)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppResult.err(AppError.Unexpected(e))
            }
        }

    suspend fun getTaskItemById(id: String): TaskItem? = withContext(ioDispatcher) {
        taskRepository.getById(id)?.toTaskItem()
    }

    fun observeSubtaskProgress(parentId: String): Flow<SubtaskProgress> =
        taskRepository.observeSubtasksSlim(parentId).map { subtasks ->
            val total = subtasks.size
            val completed = subtasks.count { it.isCompleted }
            SubtaskProgress(completed = completed, total = total)
        }

    suspend fun getById(id: String): TaskEntity? = withContext(ioDispatcher) {
        taskRepository.getById(id)
    }

    suspend fun createTask(
        title: String,
        workspaceId: String,
        areaId: String? = null,
        projectId: String? = null,
        sectionId: String? = null,
        parentId: String? = null,
        dueDate: String? = null,
        dueTime: String? = null,
        bucket: String? = null,
        priority: Int = 0,
        notes: String = "",
    ): AppResult<TaskEntity> = withContext(ioDispatcher) {
        val validationErrors = TaskValidation.validate(
            TaskValidationInput(
                title = title,
                dueDate = dueDate,
                bucket = bucket,
                priority = priority,
            ),
        )
        if (validationErrors.isNotEmpty()) {
            val first = validationErrors.first()
            return@withContext AppResult.err(AppError.Validation("title", first.message))
        }

        try {
            val now = clock.nowIso()
            val sanitizedNotes = NoteSanitizer.sanitize(notes)
            val task = TaskEntity(
                id = idGenerator.randomId(),
                title = title.trim(),
                notes = sanitizedNotes.ifBlank { null },
                workspaceId = workspaceId,
                areaId = areaId,
                projectId = projectId,
                sectionId = sectionId,
                parentTaskId = parentId,
                dueDate = dueDate,
                dueTime = dueTime,
                bucket = bucket,
                priority = priority,
                createdAt = now,
                updatedAt = now,
            )
            val saved = taskRepository.saveTask(task)
            AppResult.ok(saved)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppResult.err(AppError.Unexpected(e))
        }
    }

    suspend fun editTask(
        id: String,
        title: String? = null,
        notes: String? = null,
        dueDate: String? = null,
        dueTime: String? = null,
        priority: Int? = null,
        bucket: String? = null,
    ): AppResult<TaskEntity> = withContext(ioDispatcher) {
        try {
            val existing = taskRepository.getById(id)
                ?: return@withContext AppResult.err(AppError.NotFound)

            val updatedTitle = title?.trim() ?: existing.title
            val updatedDueDate = if (dueDate != null) dueDate.ifBlank { null } else existing.dueDate
            val updatedDueTime = if (dueTime != null) dueTime.ifBlank { null } else existing.dueTime
            val updatedPriority = priority ?: existing.priority
            val updatedBucket = if (bucket != null) bucket.ifBlank { null } else existing.bucket

            val validationErrors = TaskValidation.validate(
                TaskValidationInput(
                    title = updatedTitle,
                    dueDate = updatedDueDate,
                    bucket = updatedBucket,
                    priority = updatedPriority,
                ),
            )
            if (validationErrors.isNotEmpty()) {
                val first = validationErrors.first()
                return@withContext AppResult.err(AppError.Validation("title", first.message))
            }

            val sanitizedNotes = if (notes != null) NoteSanitizer.sanitize(notes) else existing.notes

            val updated = existing.copy(
                title = updatedTitle,
                notes = sanitizedNotes?.ifBlank { null },
                dueDate = updatedDueDate,
                dueTime = updatedDueTime,
                priority = updatedPriority,
                bucket = updatedBucket,
                updatedAt = clock.nowIso(),
            )
            val saved = taskRepository.saveTask(updated)
            AppResult.ok(saved)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppResult.err(AppError.Unexpected(e))
        }
    }

    suspend fun setCompleted(id: String, completed: Boolean): AppResult<UndoToken> = withContext(ioDispatcher) {
        try {
            val existing = taskRepository.getById(id)
                ?: return@withContext AppResult.err(AppError.NotFound)

            val now = clock.nowIso()
            val updated = existing.copy(
                isCompleted = completed,
                completedAt = if (completed) now else null,
                updatedAt = now,
            )
            taskRepository.saveTask(updated)

            val token = undoManager.registerUndo(
                description = if (completed) "Task completed" else "Task uncompleted",
            ) {
                val current = taskRepository.getById(id) ?: return@registerUndo AppResult.err(AppError.NotFound)
                taskRepository.saveTask(
                    current.copy(
                        isCompleted = !completed,
                        completedAt = if (!completed) clock.nowIso() else null,
                        updatedAt = clock.nowIso(),
                    ),
                )
                AppResult.ok(Unit)
            }
            AppResult.ok(token)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppResult.err(AppError.Unexpected(e))
        }
    }

    suspend fun setStarred(id: String, starred: Boolean): AppResult<TaskEntity> = withContext(ioDispatcher) {
        try {
            val existing = taskRepository.getById(id)
                ?: return@withContext AppResult.err(AppError.NotFound)

            val updated = existing.copy(
                isStarred = starred,
                updatedAt = clock.nowIso(),
            )
            val saved = taskRepository.saveTask(updated)
            AppResult.ok(saved)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppResult.err(AppError.Unexpected(e))
        }
    }

    suspend fun toggleStar(id: String): AppResult<TaskEntity> = withContext(ioDispatcher) {
        val existing = taskRepository.getById(id)
            ?: return@withContext AppResult.err(AppError.NotFound)
        setStarred(id, !existing.isStarred)
    }

    suspend fun duplicateTask(id: String): AppResult<TaskEntity> = withContext(ioDispatcher) {
        try {
            val existing = taskRepository.getById(id)
                ?: return@withContext AppResult.err(AppError.NotFound)

            val now = clock.nowIso()
            val cloned = existing.copy(
                id = idGenerator.randomId(),
                title = "${existing.title} (Copy)",
                isCompleted = false,
                completedAt = null,
                createdAt = now,
                updatedAt = now,
            )
            val saved = taskRepository.saveTask(cloned)
            AppResult.ok(saved)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppResult.err(AppError.Unexpected(e))
        }
    }

    suspend fun moveTask(
        id: String,
        areaId: String?,
        projectId: String?,
        sectionId: String? = null,
    ): AppResult<UndoToken> = withContext(ioDispatcher) {
        try {
            val existing = taskRepository.getById(id)
                ?: return@withContext AppResult.err(AppError.NotFound)

            val previousArea = existing.areaId
            val previousProject = existing.projectId
            val previousSection = existing.sectionId

            val updated = existing.copy(
                areaId = areaId,
                projectId = projectId,
                sectionId = sectionId,
                updatedAt = clock.nowIso(),
            )
            taskRepository.saveTask(updated)

            val token = undoManager.registerUndo(description = "Task moved") {
                val current = taskRepository.getById(id) ?: return@registerUndo AppResult.err(AppError.NotFound)
                taskRepository.saveTask(
                    current.copy(
                        areaId = previousArea,
                        projectId = previousProject,
                        sectionId = previousSection,
                        updatedAt = clock.nowIso(),
                    ),
                )
                AppResult.ok(Unit)
            }
            AppResult.ok(token)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppResult.err(AppError.Unexpected(e))
        }
    }

    suspend fun moveTaskToSection(id: String, sectionId: String?): AppResult<UndoToken> = withContext(ioDispatcher) {
        val existing = taskRepository.getById(id)
            ?: return@withContext AppResult.err(AppError.NotFound)
        moveTask(
            id = id,
            areaId = existing.areaId,
            projectId = existing.projectId,
            sectionId = sectionId,
        )
    }

    suspend fun updateDueDate(id: String, dueDate: String?, dueTime: String? = null): AppResult<UndoToken> =
        withContext(ioDispatcher) {
            try {
                val existing = taskRepository.getById(id)
                    ?: return@withContext AppResult.err(AppError.NotFound)

                val previousDueDate = existing.dueDate
                val previousDueTime = existing.dueTime

                val updated = existing.copy(
                    dueDate = dueDate,
                    dueTime = dueTime,
                    updatedAt = clock.nowIso(),
                )
                taskRepository.saveTask(updated)

                val token = undoManager.registerUndo(description = "Due date changed") {
                    val current = taskRepository.getById(id) ?: return@registerUndo AppResult.err(AppError.NotFound)
                    taskRepository.saveTask(
                        current.copy(
                            dueDate = previousDueDate,
                            dueTime = previousDueTime,
                            updatedAt = clock.nowIso(),
                        ),
                    )
                    AppResult.ok(Unit)
                }
                AppResult.ok(token)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppResult.err(AppError.Unexpected(e))
            }
        }

    suspend fun addSubtask(parentId: String, title: String): AppResult<TaskEntity> = withContext(ioDispatcher) {
        try {
            val parent = taskRepository.getById(parentId)
                ?: return@withContext AppResult.err(AppError.NotFound)

            createTask(
                title = title,
                workspaceId = parent.workspaceId,
                areaId = parent.areaId,
                projectId = parent.projectId,
                sectionId = parent.sectionId,
                parentId = parentId,
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppResult.err(AppError.Unexpected(e))
        }
    }

    suspend fun promoteSubtask(subtaskId: String): AppResult<TaskEntity> = withContext(ioDispatcher) {
        try {
            val existing = taskRepository.getById(subtaskId)
                ?: return@withContext AppResult.err(AppError.NotFound)

            if (existing.parentTaskId == null) {
                return@withContext AppResult.ok(existing)
            }

            val updated = existing.copy(
                parentTaskId = null,
                updatedAt = clock.nowIso(),
            )
            val saved = taskRepository.saveTask(updated)
            AppResult.ok(saved)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppResult.err(AppError.Unexpected(e))
        }
    }

    suspend fun indentSubtask(subtaskId: String, newParentId: String): AppResult<TaskEntity> =
        withContext(ioDispatcher) {
            try {
                val existing = taskRepository.getById(subtaskId)
                    ?: return@withContext AppResult.err(AppError.NotFound)

                // Walk ancestor chain of newParentId to verify no cycle
                var currentAncestorId: String? = newParentId
                val visited = mutableSetOf<String>()
                while (currentAncestorId != null) {
                    if (currentAncestorId == subtaskId || !visited.add(currentAncestorId)) {
                        return@withContext AppResult.err(
                            AppError.RuleViolation("Cannot nest task inside its own subtask"),
                        )
                    }
                    currentAncestorId = taskRepository.getById(currentAncestorId)?.parentTaskId
                }

                val updated = existing.copy(
                    parentTaskId = newParentId,
                    updatedAt = clock.nowIso(),
                )
                val saved = taskRepository.saveTask(updated)
                AppResult.ok(saved)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppResult.err(AppError.Unexpected(e))
            }
        }

    suspend fun outdentSubtask(subtaskId: String): AppResult<TaskEntity> = withContext(ioDispatcher) {
        try {
            val existing = taskRepository.getById(subtaskId)
                ?: return@withContext AppResult.err(AppError.NotFound)

            val currentParentId = existing.parentTaskId ?: return@withContext AppResult.ok(existing)
            val currentParent = taskRepository.getById(currentParentId)

            val newParentId = currentParent?.parentTaskId
            val updated = existing.copy(
                parentTaskId = newParentId,
                updatedAt = clock.nowIso(),
            )
            val saved = taskRepository.saveTask(updated)
            AppResult.ok(saved)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppResult.err(AppError.Unexpected(e))
        }
    }

    suspend fun trashTask(id: String): AppResult<UndoToken> = withContext(ioDispatcher) {
        try {
            taskRepository.getById(id)
                ?: return@withContext AppResult.err(AppError.NotFound)

            taskRepository.trashTask(id)

            val token = undoManager.registerUndo(
                description = "Task moved to trash",
            ) {
                taskRepository.restoreTask(id)
                AppResult.ok(Unit)
            }
            AppResult.ok(token)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppResult.err(AppError.Unexpected(e))
        }
    }

    suspend fun restoreTask(id: String): AppResult<Unit> = withContext(ioDispatcher) {
        try {
            taskRepository.restoreTask(id)
            AppResult.ok(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppResult.err(AppError.Unexpected(e))
        }
    }

    suspend fun deletePermanently(id: String): AppResult<Unit> = withContext(ioDispatcher) {
        try {
            taskRepository.deletePermanently(id)
            AppResult.ok(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppResult.err(AppError.Unexpected(e))
        }
    }

    suspend fun emptyTrash(): AppResult<Int> = withContext(ioDispatcher) {
        try {
            val trashed = taskRepository.getTrashedTasks()
            for (task in trashed) {
                taskRepository.deletePermanently(task.id)
            }
            AppResult.ok(trashed.size)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppResult.err(AppError.Unexpected(e))
        }
    }

    suspend fun purgeTrashOlderThan(cutoffIso: String): AppResult<Int> = withContext(ioDispatcher) {
        try {
            val expired = taskRepository.getTrashedTasksOlderThan(cutoffIso)
            for (task in expired) {
                taskRepository.deletePermanently(task.id)
            }
            AppResult.ok(expired.size)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppResult.err(AppError.Unexpected(e))
        }
    }

    suspend fun reorderTask(taskId: String, newSortOrder: Double): AppResult<Unit> = withContext(ioDispatcher) {
        try {
            val existing = taskRepository.getById(taskId)
                ?: return@withContext AppResult.err(AppError.NotFound)
            val updated = existing.copy(
                sortOrder = newSortOrder,
                updatedAt = clock.nowIso(),
            )
            taskRepository.saveTask(updated)
            AppResult.ok(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppResult.err(AppError.Unexpected(e))
        }
    }

    suspend fun bulkComplete(taskIds: List<String>, completed: Boolean): AppResult<UndoToken> =
        withContext(ioDispatcher) {
            if (taskIds.isEmpty()) {
                return@withContext AppResult.err(
                    AppError.Validation("taskIds", "No tasks specified"),
                )
            }
            try {
                val now = clock.nowIso()
                val previousStates = mutableMapOf<String, Pair<Boolean, String?>>()
                for (id in taskIds) {
                    val task = taskRepository.getById(id) ?: continue
                    previousStates[id] = Pair(task.isCompleted, task.completedAt)
                    val updated = task.copy(
                        isCompleted = completed,
                        completedAt = if (completed) now else null,
                        updatedAt = now,
                    )
                    taskRepository.saveTask(updated)
                }
                val token = undoManager.registerUndo(
                    description = if (completed) {
                        "${previousStates.size} tasks completed"
                    } else {
                        "${previousStates.size} tasks uncompleted"
                    },
                ) {
                    for ((id, prev) in previousStates) {
                        val task = taskRepository.getById(id) ?: continue
                        taskRepository.saveTask(
                            task.copy(
                                isCompleted = prev.first,
                                completedAt = prev.second,
                                updatedAt = clock.nowIso(),
                            ),
                        )
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

    suspend fun bulkTrash(taskIds: List<String>): AppResult<UndoToken> = withContext(ioDispatcher) {
        if (taskIds.isEmpty()) {
            return@withContext AppResult.err(
                AppError.Validation("taskIds", "No tasks specified"),
            )
        }
        try {
            val trashedIds = mutableListOf<String>()
            for (id in taskIds) {
                taskRepository.getById(id) ?: continue
                taskRepository.trashTask(id)
                trashedIds.add(id)
            }
            val token = undoManager.registerUndo(
                description = "${trashedIds.size} tasks moved to trash",
            ) {
                for (id in trashedIds) {
                    taskRepository.restoreTask(id)
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

    suspend fun bulkMove(
        taskIds: List<String>,
        areaId: String?,
        projectId: String?,
        sectionId: String? = null,
    ): AppResult<UndoToken> = withContext(ioDispatcher) {
        if (taskIds.isEmpty()) {
            return@withContext AppResult.err(
                AppError.Validation("taskIds", "No tasks specified"),
            )
        }
        try {
            val now = clock.nowIso()
            val previousLocations = mutableMapOf<String, Triple<String?, String?, String?>>()
            for (id in taskIds) {
                val task = taskRepository.getById(id) ?: continue
                previousLocations[id] = Triple(task.areaId, task.projectId, task.sectionId)
                val updated = task.copy(
                    areaId = areaId,
                    projectId = projectId,
                    sectionId = sectionId,
                    updatedAt = now,
                )
                taskRepository.saveTask(updated)
            }
            val token = undoManager.registerUndo(
                description = "${previousLocations.size} tasks moved",
            ) {
                for ((id, prev) in previousLocations) {
                    val task = taskRepository.getById(id) ?: continue
                    taskRepository.saveTask(
                        task.copy(
                            areaId = prev.first,
                            projectId = prev.second,
                            sectionId = prev.third,
                            updatedAt = clock.nowIso(),
                        ),
                    )
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

    suspend fun bulkSetDueDate(
        taskIds: List<String>,
        dueDate: String?,
        dueTime: String? = null,
    ): AppResult<UndoToken> = withContext(ioDispatcher) {
        if (taskIds.isEmpty()) {
            return@withContext AppResult.err(
                AppError.Validation("taskIds", "No tasks specified"),
            )
        }
        try {
            val now = clock.nowIso()
            val previousDates = mutableMapOf<String, Pair<String?, String?>>()
            for (id in taskIds) {
                val task = taskRepository.getById(id) ?: continue
                previousDates[id] = Pair(task.dueDate, task.dueTime)
                val updated = task.copy(
                    dueDate = dueDate,
                    dueTime = dueTime,
                    updatedAt = now,
                )
                taskRepository.saveTask(updated)
            }
            val token = undoManager.registerUndo(
                description = "Due date updated for ${previousDates.size} tasks",
            ) {
                for ((id, prev) in previousDates) {
                    val task = taskRepository.getById(id) ?: continue
                    taskRepository.saveTask(
                        task.copy(
                            dueDate = prev.first,
                            dueTime = prev.second,
                            updatedAt = clock.nowIso(),
                        ),
                    )
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

    suspend fun bulkSetPriority(taskIds: List<String>, priority: Int): AppResult<UndoToken> =
        withContext(ioDispatcher) {
            if (taskIds.isEmpty()) {
                return@withContext AppResult.err(
                    AppError.Validation("taskIds", "No tasks specified"),
                )
            }
            try {
                val now = clock.nowIso()
                val previousPriorities = mutableMapOf<String, Int>()
                for (id in taskIds) {
                    val task = taskRepository.getById(id) ?: continue
                    previousPriorities[id] = task.priority
                    val updated = task.copy(
                        priority = priority,
                        updatedAt = now,
                    )
                    taskRepository.saveTask(updated)
                }
                val token = undoManager.registerUndo(
                    description = "Priority updated for ${previousPriorities.size} tasks",
                ) {
                    for ((id, prev) in previousPriorities) {
                        val task = taskRepository.getById(id) ?: continue
                        taskRepository.saveTask(
                            task.copy(
                                priority = prev,
                                updatedAt = clock.nowIso(),
                            ),
                        )
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
}
