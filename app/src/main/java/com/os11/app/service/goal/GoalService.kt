package com.os11.app.service.goal

import com.os11.app.data.db.entities.GoalEntity
import com.os11.app.data.repository.GoalRepository
import com.os11.app.data.repository.TaskRepository
import com.os11.app.di.IoDispatcher
import com.os11.domain.id.IdGenerator
import com.os11.domain.result.AppError
import com.os11.domain.result.AppResult
import com.os11.domain.time.Clock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@Suppress("TooGenericExceptionCaught", "TooManyFunctions")
class GoalService @Inject constructor(
    private val goalRepository: GoalRepository,
    private val taskRepository: TaskRepository,
    private val clock: Clock,
    private val idGenerator: IdGenerator,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    fun observeAll(): Flow<List<GoalEntity>> = goalRepository.observeAll()

    fun observeGoalItems(): Flow<List<GoalItem>> = combine(
        goalRepository.observeAll(),
        goalRepository.observeAllLinks(),
        taskRepository.observeAllSlim(),
    ) { goals, links, tasks ->
        val completedTaskIds = tasks.filter { it.isCompleted }.map { it.id }.toSet()
        val linksByGoal = links.groupBy { it.goalId }

        goals.map { goal ->
            val goalLinks = linksByGoal[goal.id] ?: emptyList()
            val taskIds = goalLinks.filter { it.resourceType == RESOURCE_TYPE_TASK }.map { it.resourceId }
            val projectIds = goalLinks.filter { it.resourceType == RESOURCE_TYPE_PROJECT }.map { it.resourceId }
            val completedCount = taskIds.count { it in completedTaskIds }

            goal.toGoalItem(
                linkedTaskIds = taskIds,
                linkedProjectIds = projectIds,
                completedTasksCount = completedCount,
            )
        }
    }

    fun observeGoalItem(id: String): Flow<GoalItem?> =
        observeGoalItems().map { list -> list.firstOrNull { it.id == id } }

    suspend fun getById(id: String): GoalEntity? = goalRepository.getById(id)

    suspend fun createGoal(
        title: String,
        description: String = "",
        goalType: String = DEFAULT_GOAL_TYPE,
        targetDate: String? = null,
        targetValue: Double = GoalItem.DEFAULT_TARGET_VALUE,
    ): AppResult<GoalItem> = withContext(ioDispatcher) {
        if (title.trim().isEmpty()) {
            return@withContext AppResult.err(AppError.Validation("title", "Goal title cannot be blank"))
        }

        try {
            val now = clock.nowIso()
            val goal = GoalEntity(
                id = idGenerator.randomId(),
                title = title.trim(),
                description = description.ifBlank { null },
                goalType = goalType,
                targetDate = targetDate,
                targetValue = targetValue,
                currentValue = 0.0,
                streakCount = 0,
                lastProgressAt = null,
                createdAt = now,
                updatedAt = now,
            )
            val saved = goalRepository.save(goal)
            AppResult.ok(saved.toGoalItem())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppResult.err(AppError.Unexpected(e))
        }
    }

    suspend fun updateGoal(
        id: String,
        title: String,
        description: String? = null,
        targetDate: String? = null,
        targetValue: Double? = null,
        currentValue: Double? = null,
    ): AppResult<Unit> = withContext(ioDispatcher) {
        if (title.trim().isEmpty()) {
            return@withContext AppResult.err(AppError.Validation("title", "Goal title cannot be blank"))
        }

        try {
            val existing = goalRepository.getById(id) ?: return@withContext AppResult.err(AppError.NotFound)
            val updated = existing.copy(
                title = title.trim(),
                description = description,
                targetDate = targetDate,
                targetValue = targetValue ?: existing.targetValue,
                currentValue = currentValue ?: existing.currentValue,
                updatedAt = clock.nowIso(),
            )
            goalRepository.save(updated)
            AppResult.ok(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppResult.err(AppError.Unexpected(e))
        }
    }

    suspend fun recordProgress(id: String, incrementValue: Double = DEFAULT_INCREMENT): AppResult<Unit> =
        withContext(ioDispatcher) {
            try {
                val goal = goalRepository.getById(id) ?: return@withContext AppResult.err(AppError.NotFound)
                val today = clock.today()
                val now = clock.nowIso()

                val newStreak = calculateUpdatedStreak(goal.lastProgressAt, today, goal.streakCount)
                val newValue = minOf(goal.targetValue, goal.currentValue + incrementValue)

                val updated = goal.copy(
                    currentValue = newValue,
                    streakCount = newStreak,
                    lastProgressAt = now,
                    updatedAt = now,
                )
                goalRepository.save(updated)
                AppResult.ok(Unit)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppResult.err(AppError.Unexpected(e))
            }
        }

    private fun calculateUpdatedStreak(lastProgressAt: String?, today: String, currentStreak: Int): Int {
        val lastDate = lastProgressAt?.take(DATE_ISO_LENGTH)
        val isConsecutive = lastDate != null &&
            runCatching {
                LocalDate.parse(lastDate).plusDays(1).toString() == today
            }.getOrDefault(false)

        return when {
            lastDate == null -> 1
            lastDate == today -> currentStreak
            isConsecutive -> currentStreak + 1
            else -> 1
        }
    }

    suspend fun linkTask(goalId: String, taskId: String): AppResult<Unit> = withContext(ioDispatcher) {
        try {
            goalRepository.addLink(goalId, RESOURCE_TYPE_TASK, taskId)
            AppResult.ok(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppResult.err(AppError.Unexpected(e))
        }
    }

    suspend fun unlinkTask(goalId: String, taskId: String): AppResult<Unit> = withContext(ioDispatcher) {
        try {
            goalRepository.removeLink(goalId, RESOURCE_TYPE_TASK, taskId)
            AppResult.ok(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppResult.err(AppError.Unexpected(e))
        }
    }

    suspend fun linkProject(goalId: String, projectId: String): AppResult<Unit> = withContext(ioDispatcher) {
        try {
            goalRepository.addLink(goalId, RESOURCE_TYPE_PROJECT, projectId)
            AppResult.ok(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppResult.err(AppError.Unexpected(e))
        }
    }

    suspend fun unlinkProject(goalId: String, projectId: String): AppResult<Unit> = withContext(ioDispatcher) {
        try {
            goalRepository.removeLink(goalId, RESOURCE_TYPE_PROJECT, projectId)
            AppResult.ok(Unit)
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

    companion object {
        const val RESOURCE_TYPE_TASK = "task"
        const val RESOURCE_TYPE_PROJECT = "project"
        const val DEFAULT_GOAL_TYPE = "target"
        const val DEFAULT_INCREMENT = 1.0
        private const val DATE_ISO_LENGTH = 10
    }
}
