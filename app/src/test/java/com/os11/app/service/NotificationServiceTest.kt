package com.os11.app.service

import com.os11.app.data.db.entities.NotificationHistoryEntity
import com.os11.app.data.repository.NotificationRepository
import com.os11.app.platform.notifications.NotificationPlatformDispatcher
import com.os11.app.platform.notifications.OS11NotificationChannels
import com.os11.app.service.notification.NotificationService
import com.os11.domain.id.DefaultIdGenerator
import com.os11.domain.result.AppError
import com.os11.domain.result.AppResult
import com.os11.domain.time.TestClock
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class NotificationServiceTest {

    private val notificationRepository: NotificationRepository = mockk(relaxed = true)
    private val platformDispatcher: NotificationPlatformDispatcher = mockk(relaxed = true)
    private val clock = TestClock(2_000_000L)
    private val idGenerator = DefaultIdGenerator()

    private lateinit var notificationService: NotificationService

    @Before
    fun setup() {
        notificationService = NotificationService(
            notificationRepository = notificationRepository,
            platformDispatcher = platformDispatcher,
            clock = clock,
            idGenerator = idGenerator,
            ioDispatcher = Dispatchers.Unconfined,
        )
    }

    @Test
    fun `postNotification rejects blank title`() = runBlocking {
        val result = notificationService.postNotification(
            type = "reminder",
            taskId = null,
            title = "   ",
            body = "Drink water",
        )
        assertTrue(result.isErr)
        assertTrue((result as AppResult.Err).error is AppError.Validation)
    }

    @Test
    fun `postNotification saves to history and dispatches platform notification`() = runBlocking {
        coEvery { notificationRepository.add(any()) } returns Unit

        val result = notificationService.postNotification(
            type = "reminder",
            taskId = "task-123",
            title = "Task Due",
            body = "Complete the quarterly report",
            dispatchSystemNotification = true,
        )

        assertTrue(result.isOk)
        val item = (result as AppResult.Ok).value
        assertEquals("Task Due", item.title)
        assertEquals("Complete the quarterly report", item.body)
        assertEquals("task-123", item.taskId)

        coVerify { notificationRepository.add(match { it.title == "Task Due" && it.taskId == "task-123" }) }
        coVerify {
            platformDispatcher.postNotification(
                channelId = OS11NotificationChannels.CHANNEL_REMINDERS,
                notificationId = any(),
                title = "Task Due",
                content = "Complete the quarterly report",
                taskId = "task-123",
            )
        }
    }

    @Test
    fun `postNotification with dispatchSystemNotification false does not dispatch to system`() = runBlocking {
        val result = notificationService.postNotification(
            type = "system",
            taskId = null,
            title = "Sync Complete",
            body = "All items updated",
            dispatchSystemNotification = false,
        )

        assertTrue(result.isOk)
        coVerify { notificationRepository.add(any()) }
        coVerify(exactly = 0) { platformDispatcher.postNotification(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `markAsRead delegates to repository with current timestamp`() = runBlocking {
        val result = notificationService.markAsRead("notif-1")
        assertTrue(result.isOk)
        coVerify { notificationRepository.markAsRead("notif-1", clock.nowIso()) }
    }

    @Test
    fun `markAllAsRead delegates to repository`() = runBlocking {
        val result = notificationService.markAllAsRead()
        assertTrue(result.isOk)
        coVerify { notificationRepository.markAllAsRead(clock.nowIso()) }
    }

    @Test
    fun `deleteNotification delegates to repository`() = runBlocking {
        val result = notificationService.deleteNotification("notif-1")
        assertTrue(result.isOk)
        coVerify { notificationRepository.delete("notif-1") }
    }

    @Test
    fun `clearAll delegates to repository`() = runBlocking {
        val result = notificationService.clearAll()
        assertTrue(result.isOk)
        coVerify { notificationRepository.clearAll() }
    }

    @Test
    fun `observeNotifications and unreadCount stream mapped items`() = runBlocking {
        val entity = NotificationHistoryEntity(
            id = "notif-1",
            type = "pomodoro",
            taskId = null,
            title = "Focus Ended",
            body = "Take a break",
            createdAt = clock.nowIso(),
            readAt = null,
        )
        coEvery { notificationRepository.observeAll() } returns flowOf(listOf(entity))
        coEvery { notificationRepository.observeUnreadCount() } returns flowOf(1)

        val notifications = notificationService.observeNotifications().first()
        val count = notificationService.observeUnreadCount().first()

        assertEquals(1, notifications.size)
        assertEquals("Focus Ended", notifications[0].title)
        assertEquals(1, count)
    }
}
