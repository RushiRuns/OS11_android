package com.os11.app.service.tag

import com.os11.app.data.db.entities.TagEntity
import com.os11.app.data.db.entities.TaskTagEntity
import com.os11.app.data.repository.TagRepository
import com.os11.app.di.IoDispatcher
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
@Suppress("TooGenericExceptionCaught")
class TagService @Inject constructor(
    private val tagRepository: TagRepository,
    private val clock: Clock,
    private val idGenerator: IdGenerator,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    fun observeAll(): Flow<List<TagEntity>> = tagRepository.observeAll()
    fun observeTagsForTask(taskId: String): Flow<List<TagEntity>> = tagRepository.observeTagsForTask(taskId)
    suspend fun getTagsForTasks(taskIds: List<String>): List<TaskTagEntity> = tagRepository.getTagsForTasks(taskIds)

    fun observeTagItems(): Flow<List<TagItem>> = tagRepository.observeAll().map { list ->
        list.map { TagItem(id = it.id, name = it.name, color = it.color) }
    }

    suspend fun getOrCreateTag(name: String, color: String? = null): AppResult<TagItem> = withContext(ioDispatcher) {
        val trimmed = name.trim().lowercase()
        if (trimmed.isEmpty()) {
            return@withContext AppResult.err(AppError.Validation("name", "Tag name cannot be blank"))
        }
        val existing = tagRepository.getByName(trimmed)
        if (existing != null) {
            return@withContext AppResult.ok(TagItem(id = existing.id, name = existing.name, color = existing.color))
        }
        when (val created = createTag(trimmed, color)) {
            is AppResult.Ok -> AppResult.ok(
                TagItem(id = created.value.id, name = created.value.name, color = created.value.color),
            )
            is AppResult.Err -> AppResult.err(created.error)
        }
    }

    suspend fun createTag(name: String, color: String? = null): AppResult<TagEntity> = withContext(ioDispatcher) {
        if (name.trim().isEmpty()) {
            return@withContext AppResult.err(AppError.Validation("name", "Tag name cannot be blank"))
        }

        try {
            val now = clock.nowIso()
            val tag = TagEntity(
                id = idGenerator.randomId(),
                name = name.trim().lowercase(),
                color = color,
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
}
