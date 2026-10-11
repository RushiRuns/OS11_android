package com.os11.app.ui.features.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.os11.app.platform.biometrics.AppLockManager
import com.os11.app.platform.shortcuts.AppShortcutManager
import com.os11.app.service.area.AreaService
import com.os11.app.service.backup.BackupService
import com.os11.app.service.project.ProjectService
import com.os11.app.service.settings.AppearanceSettings
import com.os11.app.service.settings.BackupSettings
import com.os11.app.service.settings.ModuleItem
import com.os11.app.service.settings.NotificationSettings
import com.os11.app.service.settings.ProfilePreset
import com.os11.app.service.settings.SecuritySettings
import com.os11.app.service.settings.SettingsService
import com.os11.app.service.tag.TagService
import com.os11.app.service.task.TaskService
import com.os11.domain.id.DeterministicIds
import com.os11.domain.result.AppResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsDataCounts(
    val taskCount: Int = 0,
    val projectCount: Int = 0,
    val areaCount: Int = 0,
    val tagCount: Int = 0,
)

data class SettingsUiState(
    val appearance: AppearanceSettings = AppearanceSettings(),
    val notification: NotificationSettings = NotificationSettings(),
    val security: SecuritySettings = SecuritySettings(),
    val backup: BackupSettings = BackupSettings(),
    val modules: List<ModuleItem> = emptyList(),
    val activePreset: String = "custom",
    val dataCounts: SettingsDataCounts = SettingsDataCounts(),
    val canAuthenticate: Boolean = false,
    val statusMessage: String? = null,
)

@HiltViewModel
@Suppress("TooManyFunctions", "LongParameterList")
class SettingsViewModel @Inject constructor(
    private val settingsService: SettingsService,
    private val backupService: BackupService,
    private val appLockManager: AppLockManager,
    private val appShortcutManager: AppShortcutManager,
    private val taskService: TaskService,
    private val areaService: AreaService,
    private val projectService: ProjectService,
    private val tagService: TagService,
) : ViewModel() {

    private val workspaceId = DeterministicIds.defaultWorkspaceId()
    private val statusMessage = MutableStateFlow<String?>(null)

    private val countsFlow = combine(
        taskService.observeAllTasks(),
        areaService.observeAreaItems(workspaceId),
        projectService.observeProjectItems(workspaceId),
        tagService.observeTagItems(),
    ) { tasks, areas, projects, tags ->
        SettingsDataCounts(
            taskCount = tasks.size,
            areaCount = areas.size,
            projectCount = projects.size,
            tagCount = tags.size,
        )
    }

    private val appearanceAndSecurityFlow = combine(
        settingsService.observeAppearanceSettings(),
        settingsService.observeNotificationSettings(),
        settingsService.observeSecuritySettings(),
    ) { appearance, notification, security ->
        Triple(appearance, notification, security)
    }

    private val modulesAndCountsFlow = combine(
        settingsService.observeModuleItems(),
        settingsService.observeActiveProfilePreset(),
        countsFlow,
    ) { modules, preset, counts ->
        Triple(modules, preset, counts)
    }

    val uiState: StateFlow<SettingsUiState> = combine(
        appearanceAndSecurityFlow,
        modulesAndCountsFlow,
        settingsService.observeBackupSettings(),
        statusMessage,
    ) { (appearance, notification, security), (modules, preset, counts), backup, statusMsg ->
        SettingsUiState(
            appearance = appearance,
            notification = notification,
            security = security,
            backup = backup,
            modules = modules,
            activePreset = preset,
            dataCounts = counts,
            canAuthenticate = appLockManager.canAuthenticate(),
            statusMessage = statusMsg,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = SettingsUiState(),
    )

    fun setAccentColor(hex: String) {
        viewModelScope.launch {
            settingsService.setAccentColor(hex)
        }
    }

    fun setFontSize(step: String) {
        viewModelScope.launch {
            settingsService.setFontSize(step)
        }
    }

    fun setDensity(density: String) {
        viewModelScope.launch {
            settingsService.setDensity(density)
        }
    }

    fun setReduceMotion(enabled: Boolean) {
        viewModelScope.launch {
            settingsService.setReduceMotion(enabled)
        }
    }

    fun setHapticsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsService.setHapticsEnabled(enabled)
        }
    }

    fun setAppLockEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsService.setAppLockEnabled(enabled)
        }
    }

    fun setAppLockTimeoutSeconds(seconds: Int) {
        viewModelScope.launch {
            settingsService.setAppLockTimeoutSeconds(seconds)
        }
    }

    fun setSecureScreen(enabled: Boolean) {
        viewModelScope.launch {
            settingsService.setSecureScreen(enabled)
        }
    }

    fun setFollowUpNotifyTime(time: String) {
        viewModelScope.launch {
            settingsService.setFollowUpNotifyTime(time)
        }
    }

    fun setWaitingStaleDays(days: Int) {
        viewModelScope.launch {
            settingsService.setWaitingStaleDays(days)
        }
    }

    fun setDayStartsAt(time: String) {
        viewModelScope.launch {
            settingsService.setDayStartsAt(time)
        }
    }

    fun setModuleEnabled(name: String, enabled: Boolean) {
        viewModelScope.launch {
            settingsService.setModuleEnabled(name, enabled)
            val currentEnabled = uiState.value.modules
                .filter { if (it.name == name) enabled else it.isEnabled }
                .map { it.name }
                .toSet()
            appShortcutManager.updateShortcuts(currentEnabled)
        }
    }

    fun applyProfilePreset(preset: ProfilePreset) {
        viewModelScope.launch {
            settingsService.applyProfilePreset(preset.id)
            val enabledSet = preset.moduleMap.filter { it.value }.keys
            appShortcutManager.updateShortcuts(enabledSet)
        }
    }

    fun exportJson(onResult: (String) -> Unit) {
        viewModelScope.launch {
            when (val result = backupService.exportToJson()) {
                is AppResult.Ok -> {
                    statusMessage.value = "JSON export generated successfully"
                    onResult(result.value)
                }
                is AppResult.Err -> {
                    statusMessage.value = "Failed to export JSON: ${result.error.userMessage()}"
                }
            }
        }
    }

    fun exportCsv(onResult: (String) -> Unit) {
        viewModelScope.launch {
            when (val result = backupService.exportToCsv()) {
                is AppResult.Ok -> {
                    statusMessage.value = "CSV export generated successfully"
                    onResult(result.value)
                }
                is AppResult.Err -> {
                    statusMessage.value = "Failed to export CSV: ${result.error.userMessage()}"
                }
            }
        }
    }

    fun importJson(jsonString: String, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            when (val result = backupService.importFromJson(jsonString)) {
                is AppResult.Ok -> {
                    val importResult = result.value
                    val msg =
                        "Imported ${importResult.tasksCount} tasks, " +
                            "${importResult.projectsCount} projects, " +
                            "${importResult.areasCount} areas"
                    statusMessage.value = msg
                    onComplete(true, msg)
                }
                is AppResult.Err -> {
                    val msg = "Failed to import JSON: ${result.error.userMessage()}"
                    statusMessage.value = msg
                    onComplete(false, msg)
                }
            }
        }
    }

    fun setAutoBackupEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsService.setAutoBackupEnabled(enabled)
        }
    }

    fun setAutoBackupUri(uri: String) {
        viewModelScope.launch {
            settingsService.setAutoBackupUri(uri)
            statusMessage.value = "Backup folder updated"
        }
    }

    fun performBackupNow(context: Context, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val uri = uiState.value.backup.autoBackupUri
            if (uri.isNullOrBlank()) {
                val msg = "Please pick a backup folder first"
                statusMessage.value = msg
                onResult(false, msg)
                return@launch
            }
            when (val result = backupService.performAutoBackupToTreeUri(context, uri)) {
                is AppResult.Ok -> {
                    val msg = "Backup completed: ${result.value}"
                    statusMessage.value = msg
                    onResult(true, msg)
                }
                is AppResult.Err -> {
                    val msg = "Backup failed: ${result.error.userMessage()}"
                    statusMessage.value = msg
                    onResult(false, msg)
                }
            }
        }
    }

    fun clearStatusMessage() {
        statusMessage.value = null
    }
}
