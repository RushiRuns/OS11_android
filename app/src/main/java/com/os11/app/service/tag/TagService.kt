package com.os11.app.service.tag

import com.os11.app.data.db.entities.TagEntity
import com.os11.app.data.db.entities.TaskTagEntity
import com.os11.app.data.repository.TagRepository
import com.os11.app.data.repository.TaskRepository
import com.os11.app.di.IoDispatcher
import com.os11.app.service.task.TaskItem
import com.os11.app.service.task.toTaskItem
import com.os11.app.service.undo.UndoManager
import com.os11.app.service.undo.UndoToken
import com.os11.domain.id.IdGenerator
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

@Singleton
@Suppress("TooGenericExceptionCaught", "TooManyFunctions")
class TagService @Inject constructor(
    private val tagRepository: TagRepository,
    private val taskRepository: TaskRepository,
    private val undoManager: UndoManager,
    private val clock: Clock,
    private val idGenerator: IdGenerator,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    fun observeAll(): Flow<List<TagEntity>> = tagRepository.observeAll()
    fun observeTagsForTask(taskId: String): Flow<List<TagEntity>> = tagRepository.observeTagsForTask(taskId)
    suspend fun getTagsForTasks(taskIds: List<String>): List<TaskTagEntity> = tagRepository.getTagsForTasks(taskIds)
    fun observeTagCount(tagId: String): Flow<Int> = tagRepository.observeTaskCountForTag(tagId)

    fun observeTagItems(): Flow<List<TagItem>> = tagRepository.observeAll().map { list ->
        list.map { entity ->
            val count = tagRepository.getTaskCountForTag(entity.id)
            TagItem(
                id = entity.id,
                name = entity.name,
                color = entity.color,
                parentTagId = entity.parentTagId,
                sortOrder = entity.sortOrder,
                taskCount = count,
            )
        }
    }

    fun observeTasksByTag(tagId: String): Flow<List<TaskItem>> =
        taskRepository.observeTasksByTagSlim(tagId).map { tasks -> tasks.map { it.toTaskItem() } }

    suspend fun getOrCreateTag(name: String, color: String? = null, parentTagId: String? = null): AppResult<TagItem> =
        withContext(ioDispatcher) {
            val trimmed = name.trim().lowercase()
            if (trimmed.isEmpty()) {
                return@withContext AppResult.err(AppError.Validation("name", "Tag name cannot be blank"))
            }
            val existing = tagRepository.getByName(trimmed)
            if (existing != null) {
                val count = tagRepository.getTaskCountForTag(existing.id)
                return@withContext AppResult.ok(
                    TagItem(
                        id = existing.id,
                        name = existing.name,
                        color = existing.color,
                        parentTagId = existing.parentTagId,
                        sortOrder = existing.sortOrder,
                        taskCount = count,
                    ),
                )
            }
            when (val created = createTag(trimmed, color, parentTagId)) {
                is AppResult.Ok -> AppResult.ok(
                    TagItem(
                        id = created.value.id,
                        name = created.value.name,
                        color = created.value.color,
                        parentTagId = created.value.parentTagId,
                        sortOrder = created.value.sortOrder,
                        taskCount = 0,
                    ),
                )
                is AppResult.Err -> AppResult.err(created.error)
            }
        }

    suspend fun createTag(name: String, color: String? = null, parentTagId: String? = null): AppResult<TagEntity> =
        withContext(ioDispatcher) {
            val trimmed = name.trim().lowercase()
            if (trimmed.isEmpty()) {
                return@withContext AppResult.err(AppError.Validation("name", "Tag name cannot be blank"))
            }

            try {
                var resolvedParentId = parentTagId
                var resolvedName = trimmed

                // Resolve nested hierarchy if parent path specified
                if (resolvedParentId == null && trimmed.contains("/")) {
                    val parentName = trimmed.substringBeforeLast("/")
                    val parentResult = getOrCreateTag(parentName)
                    if (parentResult is AppResult.Ok) {
                        resolvedParentId = parentResult.value.id
                    }
                } else if (resolvedParentId != null) {
                    val parent = tagRepository.getById(resolvedParentId)
                    if (parent != null && !trimmed.startsWith("${parent.name}/")) {
                        resolvedName = "${parent.name}/$trimmed"
                    }
                }

                val existing = tagRepository.getByName(resolvedName)
                if (existing != null) {
                    return@withContext AppResult.ok(existing)
                }

                val now = clock.nowIso()
                val tag = TagEntity(
                    id = idGenerator.randomId(),
                    name = resolvedName,
                    color = color,
                    parentTagId = resolvedParentId,
                    createdAt = now,
                )
                val saved = tagRepository.save(tag)
                AppResult.ok(saved)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppResult.err(AppError.Unexpected(e))
            }
        }

    suspend fun renameTag(id: String, newName: String): AppResult<TagItem> = withContext(ioDispatcher) {
        val trimmed = newName.trim().lowercase()
        if (trimmed.isEmpty()) {
            return@withContext AppResult.err(AppError.Validation("name", "Tag name cannot be blank"))
        }

        try {
            val existing = tagRepository.getById(id) ?: return@withContext AppResult.err(AppError.NotFound)
            if (existing.name == trimmed) {
                val count = tagRepository.getTaskCountForTag(id)
                return@withContext AppResult.ok(
                    TagItem(
                        id = existing.id,
                        name = existing.name,
                        color = existing.color,
                        parentTagId = existing.parentTagId,
                        sortOrder = existing.sortOrder,
                        taskCount = count,
                    ),
                )
            }

            val nameConflict = tagRepository.getByName(trimmed)
            if (nameConflict != null && nameConflict.id != id) {
                return@withContext AppResult.err(AppError.Validation("name", "Tag with name '$trimmed' already exists"))
            }

            val oldPrefix = "${existing.name}/"
            val newPrefix = "$trimmed/"

            // Query child tags before saving parent to prevent foreign key SET_NULL cascades
            val childTags = tagRepository.getChildTags(id)

            val updated = existing.copy(name = trimmed)
            tagRepository.save(updated)

            // Cascade rename to nested child tags
            for (child in childTags) {
                val childNewName = if (child.name.startsWith(oldPrefix)) {
                    child.name.replaceFirst(oldPrefix, newPrefix)
                } else {
                    "$trimmed/${child.name.substringAfterLast("/")}"
                }
                tagRepository.save(child.copy(name = childNewName, parentTagId = id))
            }

            val count = tagRepository.getTaskCountForTag(id)
            AppResult.ok(
                TagItem(
                    id = updated.id,
                    name = updated.name,
                    color = updated.color,
                    parentTagId = updated.parentTagId,
                    sortOrder = updated.sortOrder,
                    taskCount = count,
                ),
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppResult.err(AppError.Unexpected(e))
        }
    }

    suspend fun recolorTag(id: String, newColor: String?): AppResult<TagItem> = withContext(ioDispatcher) {
        try {
            val existing = tagRepository.getById(id) ?: return@withContext AppResult.err(AppError.NotFound)
            val updated = existing.copy(color = newColor)
            tagRepository.save(updated)

            val count = tagRepository.getTaskCountForTag(id)
            AppResult.ok(
                TagItem(
                    id = updated.id,
                    name = updated.name,
                    color = updated.color,
                    parentTagId = updated.parentTagId,
                    sortOrder = updated.sortOrder,
                    taskCount = count,
                ),
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppResult.err(AppError.Unexpected(e))
        }
    }

    /**
     * Merges sourceTag into targetTag.
     * All tasks tagged with sourceTag are moved to targetTag.
     * Leaves zero orphan task_tags rows, deletes sourceTag, and registers an undo action.
     */
    suspend fun mergeTags(sourceTagId: String, targetTagId: String): AppResult<UndoToken> = withContext(ioDispatcher) {
        if (sourceTagId == targetTagId) {
            return@withContext AppResult.err(AppError.Validation("tag", "Cannot merge a tag into itself"))
        }

        try {
            val sourceTag = tagRepository.getById(sourceTagId) ?: return@withContext AppResult.err(AppError.NotFound)
            val targetTag = tagRepository.getById(targetTagId) ?: return@withContext AppResult.err(AppError.NotFound)

            val sourceTaskIds = tagRepository.getTaskIdsForTag(sourceTagId)
            val targetTaskIds = tagRepository.getTaskIdsForTag(targetTagId).toSet()

            // Repoint task_tags
            val onlyInSource = mutableListOf<String>()
            val inBoth = mutableListOf<String>()

            for (taskId in sourceTaskIds) {
                if (taskId in targetTaskIds) {
                    inBoth.add(taskId)
                    tagRepository.removeTagFromTask(taskId, sourceTagId)
                } else {
                    onlyInSource.add(taskId)
                    tagRepository.addTagToTask(taskId, targetTagId)
                    tagRepository.removeTagFromTask(taskId, sourceTagId)
                }
            }

            // Repoint child tags
            val childTags = tagRepository.getChildTags(sourceTagId)
            for (child in childTags) {
                tagRepository.save(child.copy(parentTagId = targetTagId))
            }

            // Delete source tag
            tagRepository.delete(sourceTagId)

            // Register undo operation in UndoManager
            val token = undoManager.registerUndo("Merged #${sourceTag.name} into #${targetTag.name}") {
                tagRepository.save(sourceTag)
                for (taskId in onlyInSource) {
                    tagRepository.removeTagFromTask(taskId, targetTagId)
                    tagRepository.addTagToTask(taskId, sourceTagId)
                }
                for (taskId in inBoth) {
                    tagRepository.addTagToTask(taskId, sourceTagId)
                }
                for (child in childTags) {
                    tagRepository.save(child.copy(parentTagId = sourceTagId))
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

    suspend fun deleteTag(id: String): AppResult<Unit> = withContext(ioDispatcher) {
        try {
            tagRepository.delete(id)
            AppResult.ok(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppResult.err(AppError.Unexpected(e))
        }
    }

    suspend fun addTagToTask(taskId: String, tagId: String): AppResult<Unit> = withContext(ioDispatcher) {
        try {
            tagRepository.addTagToTask(taskId, tagId)
            AppResult.ok(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppResult.err(AppError.Unexpected(e))
        }
    }

    suspend fun removeTagFromTask(taskId: String, tagId: String): AppResult<Unit> = withContext(ioDispatcher) {
        try {
            tagRepository.removeTagFromTask(taskId, tagId)
            AppResult.ok(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppResult.err(AppError.Unexpected(e))
        }
    }

    suspend fun bulkAddTag(taskIds: List<String>, tagId: String): AppResult<Unit> = withContext(ioDispatcher) {
        if (taskIds.isEmpty()) return@withContext AppResult.ok(Unit)
        try {
            for (taskId in taskIds) {
                tagRepository.addTagToTask(taskId, tagId)
            }
            AppResult.ok(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppResult.err(AppError.Unexpected(e))
        }
    }
}
