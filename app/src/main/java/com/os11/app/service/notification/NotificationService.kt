package com.os11.app.service.notification

import com.os11.app.data.db.entities.NotificationHistoryEntity
import com.os11.app.data.repository.NotificationRepository
import com.os11.app.di.IoDispatcher
import com.os11.app.platform.notifications.NotificationPlatformDispatcher
import com.os11.app.platform.notifications.OS11NotificationChannels
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

private const val MAX_NOTIFICATION_ID = 100_000

@Singleton
@Suppress("TooGenericExceptionCaught")
class NotificationService @Inject constructor(
    private val notificationRepository: NotificationRepository,
    private val platformDispatcher: NotificationPlatformDispatcher,
    private val clock: Clock,
    private val idGenerator: IdGenerator,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    fun observeNotifications(): Flow<List<NotificationItem>> = notificationRepository.observeAll().map { entities ->
        entities.map {
            NotificationItem(
                id = it.id,
                type = it.type,
                taskId = it.taskId,
                title = it.title,
                body = it.body,
                createdAt = it.createdAt,
                readAt = it.readAt,
            )
        }
    }

    fun observeUnreadCount(): Flow<Int> = notificationRepository.observeUnreadCount()

    suspend fun postNotification(
        type: String,
        taskId: String?,
        title: String,
        body: String,
        dispatchSystemNotification: Boolean = true,
    ): AppResult<NotificationItem> = withContext(ioDispatcher) {
        if (title.isBlank()) {
            return@withContext AppResult.err(AppError.Validation("title", "Title cannot be blank"))
        }

        try {
            val now = clock.nowIso()
            val id = idGenerator.randomId()
            val entity = NotificationHistoryEntity(
                id = id,
                type = type,
                taskId = taskId,
                title = title.trim(),
                body = body.trim(),
                createdAt = now,
                readAt = null,
            )
            notificationRepository.add(entity)

            if (dispatchSystemNotification) {
                val channelId = when (type.lowercase()) {
                    "reminder", "due" -> OS11NotificationChannels.CHANNEL_REMINDERS
                    "pomodoro" -> OS11NotificationChannels.CHANNEL_POMODORO
                    "follow_up", "waiting" -> OS11NotificationChannels.CHANNEL_FOLLOW_UPS
                    else -> OS11NotificationChannels.CHANNEL_SYSTEM
                }
                val notificationId = (id.hashCode().let { if (it < 0) -it else it }) % MAX_NOTIFICATION_ID
                platformDispatcher.postNotification(
                    channelId = channelId,
                    notificationId = notificationId,
                    title = title,
                    content = body,
                    taskId = taskId,
                )
            }

            AppResult.ok(
                NotificationItem(
                    id = entity.id,
                    type = entity.type,
                    taskId = entity.taskId,
                    title = entity.title,
                    body = entity.body,
                    createdAt = entity.createdAt,
                    readAt = entity.readAt,
                ),
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppResult.err(AppError.Unexpected(e))
        }
    }

    suspend fun markAsRead(id: String): AppResult<Unit> = withContext(ioDispatcher) {
        try {
            val now = clock.nowIso()
            notificationRepository.markAsRead(id, now)
            AppResult.ok(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppResult.err(AppError.Unexpected(e))
        }
    }

    suspend fun markAllAsRead(): AppResult<Unit> = withContext(ioDispatcher) {
        try {
            val now = clock.nowIso()
            notificationRepository.markAllAsRead(now)
            AppResult.ok(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppResult.err(AppError.Unexpected(e))
        }
    }

    suspend fun deleteNotification(id: String): AppResult<Unit> = withContext(ioDispatcher) {
        try {
            notificationRepository.delete(id)
            AppResult.ok(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppResult.err(AppError.Unexpected(e))
        }
    }

    suspend fun clearAll(): AppResult<Unit> = withContext(ioDispatcher) {
        try {
            notificationRepository.clearAll()
            AppResult.ok(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppResult.err(AppError.Unexpected(e))
        }
    }
}
