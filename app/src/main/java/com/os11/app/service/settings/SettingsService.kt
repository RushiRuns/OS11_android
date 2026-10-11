package com.os11.app.service.settings

import com.os11.app.data.db.entities.ModuleEntity
import com.os11.app.data.db.entities.SettingEntity
import com.os11.app.data.repository.ModuleRepository
import com.os11.app.data.repository.SettingsRepository
import com.os11.app.di.IoDispatcher
import com.os11.domain.result.AppError
import com.os11.domain.result.AppResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@Suppress("TooGenericExceptionCaught")
class SettingsService @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val moduleRepository: ModuleRepository,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    fun observeAllSettings(): Flow<List<SettingEntity>> = settingsRepository.observeAll()
    fun observeAllModules(): Flow<List<ModuleEntity>> = moduleRepository.observeAll()
    fun observeEnabledModules(): Flow<List<ModuleEntity>> =
        moduleRepository.observeAll().map { modules -> modules.filter { it.isEnabled } }

    fun observeModuleEnabled(name: String): Flow<Boolean> = moduleRepository.observeAll().map { modules ->
        modules.firstOrNull {
            it.moduleName == name || (name == "goals" && it.moduleName == "goals_habits")
        }?.isEnabled ?: true
    }

    suspend fun isModuleEnabled(name: String): Boolean =
        moduleRepository.isEnabled(name) || (name == "goals" && moduleRepository.isEnabled("goals_habits"))

    suspend fun getSetting(key: String): String? = settingsRepository.get(key)

    suspend fun setSetting(key: String, value: String): AppResult<Unit> = withContext(ioDispatcher) {
        try {
            settingsRepository.save(key, value)
            AppResult.ok(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppResult.err(AppError.Unexpected(e))
        }
    }

    suspend fun setModuleEnabled(name: String, enabled: Boolean): AppResult<Unit> = withContext(ioDispatcher) {
        try {
            moduleRepository.setEnabled(name, enabled)
            AppResult.ok(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppResult.err(AppError.Unexpected(e))
        }
    }

    suspend fun getDayStartsAt(): String {
        val raw = getSetting("day_starts_at")?.replace("\"", "")?.trim()
        return if (!raw.isNullOrBlank()) raw else "00:00"
    }

    suspend fun calculateLogicalToday(now: java.time.ZonedDateTime = java.time.ZonedDateTime.now()): String {
        val dayStartsAt = getDayStartsAt()
        val parts = dayStartsAt.split(":")
        val startHour = parts.getOrNull(0)?.toIntOrNull() ?: 0
        val startMin = parts.getOrNull(1)?.toIntOrNull() ?: 0

        val startMinutesOfDay = startHour * MINUTES_PER_HOUR + startMin
        val currentMinutesOfDay = now.hour * MINUTES_PER_HOUR + now.minute

        val logicalDate = if (currentMinutesOfDay < startMinutesOfDay) {
            now.minusDays(1).toLocalDate()
        } else {
            now.toLocalDate()
        }
        return logicalDate.toString()
    }

    companion object {
        private const val MINUTES_PER_HOUR = 60
    }
}
