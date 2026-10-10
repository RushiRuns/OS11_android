package com.os11.app.service.pomodoro

import com.os11.app.data.db.entities.PomodoroSessionEntity
import com.os11.app.data.repository.PomodoroRepository
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
class PomodoroService @Inject constructor(
    private val pomodoroRepository: PomodoroRepository,
    private val clock: Clock,
    private val idGenerator: IdGenerator,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    fun observeAll(): Flow<List<PomodoroSessionEntity>> = pomodoroRepository.observeAll()
    fun observeByTask(taskId: String): Flow<List<PomodoroSessionEntity>> = pomodoroRepository.observeByTask(taskId)

    suspend fun recordSession(
        taskId: String? = null,
        type: String = "work",
        durationMinutes: Int = 25,
        completed: Boolean = true,
    ): AppResult<PomodoroSessionEntity> = withContext(ioDispatcher) {
        try {
            val now = clock.nowIso()
            val session = PomodoroSessionEntity(
                id = idGenerator.randomId(),
                taskId = taskId,
                type = type,
                durationSeconds = durationMinutes * 60,
                startedAt = now,
                endedAt = now,
                wasCompleted = completed,
                createdAt = now,
            )
            val saved = pomodoroRepository.save(session)
            AppResult.ok(saved)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppResult.err(AppError.Unexpected(e))
        }
    }
}
