package com.os11.app.service.area

import com.os11.app.data.db.entities.AreaEntity
import com.os11.app.data.repository.AreaRepository
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
class AreaService @Inject constructor(
    private val areaRepository: AreaRepository,
    private val undoManager: UndoManager,
    private val clock: Clock,
    private val idGenerator: IdGenerator,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    fun observeAll(workspaceId: String): Flow<List<AreaEntity>> = areaRepository.observeAll(workspaceId)
    suspend fun getById(id: String): AreaEntity? = areaRepository.getById(id)

    suspend fun createArea(
        name: String,
        workspaceId: String,
        icon: String? = null,
        color: String? = null,
    ): AppResult<AreaEntity> = withContext(ioDispatcher) {
        if (name.trim().isEmpty()) {
            return@withContext AppResult.err(AppError.Validation("name", "Area name cannot be blank"))
        }

        try {
            val now = clock.nowIso()
            val area = AreaEntity(
                id = idGenerator.randomId(),
                name = name.trim(),
                workspaceId = workspaceId,
                icon = icon,
                color = color,
                createdAt = now,
                updatedAt = now,
            )
            val saved = areaRepository.save(area)
            AppResult.ok(saved)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppResult.err(AppError.Unexpected(e))
        }
    }

    suspend fun deleteArea(id: String): AppResult<UndoToken> = withContext(ioDispatcher) {
        try {
            val existing = areaRepository.getById(id)
                ?: return@withContext AppResult.err(AppError.NotFound)

            areaRepository.delete(id)

            val token = undoManager.registerUndo("Area deleted") {
                areaRepository.save(existing)
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
