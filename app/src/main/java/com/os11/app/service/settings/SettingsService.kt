package com.os11.app.service.settings

import com.os11.app.data.db.entities.LocalIdentityEntity
import com.os11.app.data.db.entities.ModuleEntity
import com.os11.app.data.db.entities.SettingEntity
import com.os11.app.data.repository.LocalIdentityRepository
import com.os11.app.data.repository.ModuleRepository
import com.os11.app.data.repository.SettingsRepository
import com.os11.app.di.IoDispatcher
import com.os11.app.ui.theme.OS11Density
import com.os11.app.ui.theme.OS11FontStep
import com.os11.domain.id.DefaultIdGenerator
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
@Suppress("TooGenericExceptionCaught", "TooManyFunctions")
class SettingsService @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val moduleRepository: ModuleRepository,
    private val localIdentityRepository: LocalIdentityRepository,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    fun observeAllSettings(): Flow<List<SettingEntity>> = settingsRepository.observeAll()
    fun observeAllModules(): Flow<List<ModuleEntity>> = moduleRepository.observeAll()
    fun observeEnabledModules(): Flow<List<ModuleEntity>> =
        moduleRepository.observeAll().map { modules -> modules.filter { it.isEnabled } }

    fun observeEnabledModuleNames(): Flow<Set<String>> = moduleRepository.observeAll().map { modules ->
        modules.filter { it.isEnabled }.map { it.moduleName }.toSet()
    }

    fun observeModuleEnabled(name: String): Flow<Boolean> = moduleRepository.observeAll().map { modules ->
        modules.firstOrNull {
            it.moduleName == name || (name == "goals" && it.moduleName == "goals_habits")
        }?.isEnabled ?: true
    }

    suspend fun isModuleEnabled(name: String): Boolean =
        moduleRepository.isEnabled(name) || (name == "goals" && moduleRepository.isEnabled("goals_habits"))

    suspend fun getSetting(key: String): String? = settingsRepository.get(key)

    fun observeSetting(key: String): Flow<String?> = settingsRepository.observeAll().map { list ->
        list.firstOrNull { it.key == key }?.value
    }

    fun observeSettingsMap(): Flow<Map<String, String>> = settingsRepository.observeAll().map { list ->
        list.associate { it.key to it.value.replace("\"", "").trim() }
    }

    fun observeModuleItems(): Flow<List<ModuleItem>> = moduleRepository.observeAll().map { list ->
        list.map { entity ->
            val meta = MODULE_METADATA[entity.moduleName] ?: Pair(entity.moduleName, "Module description")
            ModuleItem(
                name = entity.moduleName,
                isEnabled = entity.isEnabled,
                title = meta.first,
                description = meta.second,
            )
        }
    }

    fun observeAppearanceSettings(): Flow<AppearanceSettings> = observeSettingsMap().map { map ->
        val accent = map["accent_color"] ?: "#4da3ff"
        val fontStep = when (map["font_size"]?.lowercase()) {
            "sm" -> OS11FontStep.Sm
            "lg" -> OS11FontStep.Lg
            "xl" -> OS11FontStep.Xl
            else -> OS11FontStep.Md
        }
        val density = when (map["density"]?.lowercase()) {
            "compact" -> OS11Density.Compact
            "cozy" -> OS11Density.Cozy
            else -> OS11Density.Comfortable
        }
        val reduceMotion = map["reduce_motion"]?.toBooleanStrictOrNull() ?: false
        val haptics = map["haptics_enabled"]?.toBooleanStrictOrNull() ?: true

        AppearanceSettings(
            accentColorHex = accent,
            fontStep = fontStep,
            density = density,
            reduceMotion = reduceMotion,
            hapticsEnabled = haptics,
        )
    }

    fun observeNotificationSettings(): Flow<NotificationSettings> = observeSettingsMap().map { map ->
        NotificationSettings(
            followUpNotifyTime = map["follow_up_notify_time"] ?: "09:00",
            waitingStaleDays = map["waiting_stale_days"]?.toIntOrNull() ?: 7,
            dayStartsAt = map["day_starts_at"] ?: "00:00",
        )
    }

    fun observeSecuritySettings(): Flow<SecuritySettings> = observeSettingsMap().map { map ->
        SecuritySettings(
            appLockEnabled = map["app_lock_enabled"]?.toBooleanStrictOrNull() ?: false,
            appLockTimeoutSeconds = map["app_lock_timeout_seconds"]?.toIntOrNull() ?: 0,
            secureScreen = map["secure_screen"]?.toBooleanStrictOrNull() ?: false,
            hapticsEnabled = map["haptics_enabled"]?.toBooleanStrictOrNull() ?: true,
        )
    }

    fun observeBackupSettings(): Flow<BackupSettings> = observeSettingsMap().map { map ->
        BackupSettings(
            autoBackupEnabled = map["auto_backup_enabled"]?.toBooleanStrictOrNull() ?: false,
            autoBackupUri = map["auto_backup_uri"],
            lastBackupTimestamp = map["last_backup_timestamp"],
        )
    }

    suspend fun setAutoBackupEnabled(enabled: Boolean): AppResult<Unit> =
        setSetting("auto_backup_enabled", enabled.toString())

    suspend fun setAutoBackupUri(uri: String): AppResult<Unit> = setSetting("auto_backup_uri", uri)

    fun observeActiveProfilePreset(): Flow<String> = observeSetting("active_profile_preset").map { raw ->
        raw?.replace("\"", "")?.trim() ?: "custom"
    }

    fun observeHapticsEnabled(): Flow<Boolean> = observeSetting("haptics_enabled").map { raw ->
        raw?.replace("\"", "")?.trim()?.toBooleanStrictOrNull() ?: true
    }

    suspend fun isHapticsEnabled(): Boolean =
        getSetting("haptics_enabled")?.replace("\"", "")?.trim()?.toBooleanStrictOrNull() ?: true

    suspend fun isAppLockEnabled(): Boolean =
        getSetting("app_lock_enabled")?.replace("\"", "")?.trim()?.toBooleanStrictOrNull() ?: false

    suspend fun getAppLockTimeoutSeconds(): Int =
        getSetting("app_lock_timeout_seconds")?.replace("\"", "")?.trim()?.toIntOrNull() ?: 0

    fun observeLocalIdentity(): Flow<LocalIdentityEntity?> = localIdentityRepository.observe()

    suspend fun getLocalIdentity(): LocalIdentityEntity? = localIdentityRepository.get()

    suspend fun updateLocalIdentity(displayName: String, avatarEmoji: String?): AppResult<Unit> =
        withContext(ioDispatcher) {
            try {
                val current = localIdentityRepository.get()
                val id = current?.id ?: DefaultIdGenerator().randomId()
                val createdAt = current?.createdAt ?: java.time.Instant.now().toString()
                localIdentityRepository.save(
                    LocalIdentityEntity(
                        id = id,
                        displayName = displayName.trim().ifEmpty { "My Android" },
                        avatarEmoji = avatarEmoji,
                        createdAt = createdAt,
                    ),
                )
                AppResult.ok(Unit)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppResult.err(AppError.Unexpected(e))
            }
        }

    fun observeOnboardingCompleted(): Flow<Boolean> = observeSetting("onboarding_completed").map { raw ->
        raw?.replace("\"", "")?.trim()?.toBooleanStrictOrNull() ?: false
    }

    suspend fun isOnboardingCompleted(): Boolean =
        getSetting("onboarding_completed")?.replace("\"", "")?.trim()?.toBooleanStrictOrNull() ?: false

    suspend fun setOnboardingCompleted(completed: Boolean): AppResult<Unit> =
        setSetting("onboarding_completed", completed.toString())

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
            settingsRepository.save("active_profile_preset", "\"custom\"")
            AppResult.ok(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppResult.err(AppError.Unexpected(e))
        }
    }

    suspend fun applyProfilePreset(presetId: String): AppResult<Unit> = withContext(ioDispatcher) {
        try {
            val preset = ProfilePreset.AllPresets.firstOrNull { it.id == presetId }
            if (preset != null) {
                for ((moduleName, enabled) in preset.moduleMap) {
                    moduleRepository.setEnabled(moduleName, enabled)
                }
                settingsRepository.save("active_profile_preset", "\"${preset.id}\"")
            }
            AppResult.ok(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppResult.err(AppError.Unexpected(e))
        }
    }

    suspend fun setAccentColor(colorHex: String): AppResult<Unit> = setSetting("accent_color", "\"$colorHex\"")

    suspend fun setFontSize(step: String): AppResult<Unit> = setSetting("font_size", "\"$step\"")

    suspend fun setDensity(density: String): AppResult<Unit> = setSetting("density", "\"$density\"")

    suspend fun setReduceMotion(enabled: Boolean): AppResult<Unit> = setSetting("reduce_motion", enabled.toString())

    suspend fun setHapticsEnabled(enabled: Boolean): AppResult<Unit> = setSetting("haptics_enabled", enabled.toString())

    suspend fun setAppLockEnabled(enabled: Boolean): AppResult<Unit> =
        setSetting("app_lock_enabled", enabled.toString())

    suspend fun setAppLockTimeoutSeconds(seconds: Int): AppResult<Unit> =
        setSetting("app_lock_timeout_seconds", seconds.toString())

    suspend fun setSecureScreen(enabled: Boolean): AppResult<Unit> = setSetting("secure_screen", enabled.toString())

    suspend fun setFollowUpNotifyTime(time: String): AppResult<Unit> = setSetting("follow_up_notify_time", "\"$time\"")

    suspend fun setWaitingStaleDays(days: Int): AppResult<Unit> = setSetting("waiting_stale_days", days.toString())

    suspend fun setDayStartsAt(time: String): AppResult<Unit> = setSetting("day_starts_at", "\"$time\"")

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

        val MODULE_METADATA = mapOf(
            "my_day" to Pair("My Day", "Daily focus planning and midnight rollover"),
            "project_management" to Pair("Projects & Areas", "Hierarchical folders, project sections, and milestones"),
            "pomodoro" to Pair("Pomodoro Focus", "25-min interval timer with chronometer notifications"),
            "agenda" to Pair("Day Plan & Timeline", "24-hour vertical timeline with lane overlap scheduling"),
            "goals_habits" to Pair("Goals & Habits", "Target tracking, progress logging, and consecutive streaks"),
            "dashboard" to Pair("Analytics Dashboard", "High-performance metrics, completion trends, and heatmaps"),
            "file_attachments" to Pair("Attachments", "SAF file picker, Coil image thumbnails, and link attachments"),
            "nlp_parsing" to Pair("NLP Quick Add", "Natural language parsing for dates, priority, and tags"),
            "anytime" to Pair("Anytime Smart List", "GTD smart list for tasks actionable without date constraints"),
            "someday" to Pair("Someday Smart List", "GTD smart list for deferred ideas and aspirational tasks"),
            "waiting_for" to Pair("Waiting For Smart List", "GTD smart list for delegated tasks awaiting follow-up"),
            "habit_tracker" to Pair("Habit Tracker", "Dedicated habit completion tracking and streaks"),
        )
    }
}
