package com.os11.app.data.repository

import com.os11.app.data.db.OS11Database
import com.os11.app.data.db.entities.ModuleEntity
import com.os11.app.data.db.entities.NotificationHistoryEntity
import com.os11.app.data.db.entities.SettingEntity
import com.os11.app.data.db.entities.TaskHistoryEntity
import com.os11.app.data.sync.TrackedWriter
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun observeAll(): Flow<List<SettingEntity>>
    suspend fun get(key: String): String?
    suspend fun save(key: String, value: String)
}

internal class SettingsRepositoryImpl(private val db: OS11Database, private val trackedWriter: TrackedWriter) :
    SettingsRepository {
    override fun observeAll(): Flow<List<SettingEntity>> = db.settingDao().observeAll()
    override suspend fun get(key: String): String? = db.settingDao().getValue(key)
    override suspend fun save(key: String, value: String) = trackedWriter.writeSetting(key, value)
}

interface ModuleRepository {
    fun observeAll(): Flow<List<ModuleEntity>>
    suspend fun isEnabled(moduleName: String): Boolean
    suspend fun setEnabled(moduleName: String, enabled: Boolean)
}

internal class ModuleRepositoryImpl(private val db: OS11Database, private val trackedWriter: TrackedWriter) :
    ModuleRepository {
    override fun observeAll(): Flow<List<ModuleEntity>> = db.moduleDao().observeAll()

    override suspend fun isEnabled(moduleName: String): Boolean = db.moduleDao().isEnabled(moduleName) ?: false

    override suspend fun setEnabled(moduleName: String, enabled: Boolean) {
        trackedWriter.writeModule(ModuleEntity(moduleName, enabled))
    }
}

interface NotificationRepository {
    fun observeAll(): Flow<List<NotificationHistoryEntity>>
    fun observeUnreadCount(): Flow<Int>
    suspend fun add(item: NotificationHistoryEntity)
    suspend fun markAsRead(id: String, readAt: String)
    suspend fun markAllAsRead(readAt: String)
    suspend fun delete(id: String)
    suspend fun clearAll()
    suspend fun purgeOlderThan(cutoffIso: String)
}

internal class NotificationRepositoryImpl(private val db: OS11Database, private val trackedWriter: TrackedWriter) :
    NotificationRepository {
    override fun observeAll(): Flow<List<NotificationHistoryEntity>> = db.notificationHistoryDao().observeAll()

    override fun observeUnreadCount(): Flow<Int> = db.notificationHistoryDao().observeUnreadCount()

    override suspend fun add(item: NotificationHistoryEntity) = trackedWriter.writeNotification(item)

    override suspend fun markAsRead(id: String, readAt: String) = trackedWriter.markNotificationRead(id, readAt)

    override suspend fun markAllAsRead(readAt: String) = trackedWriter.markAllNotificationsRead(readAt)

    override suspend fun delete(id: String) = trackedWriter.deleteNotification(id)

    override suspend fun clearAll() = trackedWriter.clearNotifications()

    override suspend fun purgeOlderThan(cutoffIso: String) = trackedWriter.purgeNotifications(cutoffIso)
}

interface HistoryRepository {
    fun observeByTask(taskId: String): Flow<List<TaskHistoryEntity>>
    suspend fun purgeOlderThan(cutoffIso: String)
}

internal class HistoryRepositoryImpl(private val db: OS11Database, private val trackedWriter: TrackedWriter) :
    HistoryRepository {
    override fun observeByTask(taskId: String): Flow<List<TaskHistoryEntity>> {
        val dao = db.taskHistoryDao()
        return dao.observeByTask(taskId)
    }

    override suspend fun purgeOlderThan(cutoffIso: String) = trackedWriter.purgeTaskHistory(cutoffIso)
}
