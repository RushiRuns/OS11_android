package com.os11.app.service.goal

import com.os11.app.data.db.entities.GoalEntity
import com.os11.app.data.repository.GoalRepository
import com.os11.app.di.IoDispatcher
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
class GoalService @Inject constructor(
    private val goalRepository: GoalRepository,
    private val clock: Clock,
    private val idGenerator: IdGenerator,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    fun observeAll(): Flow<List<GoalEntity>> = goalRepository.observeAll()
    suspend fun getById(id: String): GoalEntity? = goalRepository.getById(id)

    suspend fun createGoal(
        title: String,
        description: String = "",
        goalType: String = "target",
        targetDate: String? = null,
        targetValue: Double = 100.0,
    ): AppResult<GoalEntity> = withContext(ioDispatcher) {
        if (title.trim().isEmpty()) {
            return@withContext AppResult.err(AppError.Validation("title", "Goal title cannot be blank"))
        }

        try {
            val now = clock.nowIso()
            val goal = GoalEntity(
                id = idGenerator.randomId(),
                title = title.trim(),
                description = description,
                goalType = goalType,
                targetDate = targetDate,
                targetValue = targetValue,
                createdAt = now,
                updatedAt = now,
            )
            val saved = goalRepository.save(goal)
            AppResult.ok(saved)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppResult.err(AppError.Unexpected(e))
        }
    }

    suspend fun deleteGoal(id: String): AppResult<Unit> = withContext(ioDispatcher) {
        try {
            goalRepository.delete(id)
            AppResult.ok(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppResult.err(AppError.Unexpected(e))
        }
    }
}
