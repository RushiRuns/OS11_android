package com.os11.app.service.task

import com.os11.app.data.db.dao.SlimTask
import com.os11.app.data.db.entities.TaskEntity
import com.os11.app.data.repository.TaskRepository
import com.os11.app.di.IoDispatcher
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
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@Suppress("TooManyFunctions", "TooGenericExceptionCaught")
class TaskService @Inject constructor(
    private val taskRepository: TaskRepository,
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

    suspend fun createTask(
        title: String,
        workspaceId: String,
        areaId: String? = null,
        projectId: String? = null,
        sectionId: String? = null,
        parentId: String? = null,
        dueDate: String? = null,
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
            val task = TaskEntity(
                id = idGenerator.randomId(),
                title = title.trim(),
                notes = notes,
                workspaceId = workspaceId,
                areaId = areaId,
                projectId = projectId,
                sectionId = sectionId,
                parentTaskId = parentId,
                dueDate = dueDate,
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
}
