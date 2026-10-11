package com.os11.app.ui.features.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.os11.app.platform.biometrics.AppLockManager
import com.os11.app.platform.shortcuts.AppShortcutManager
import com.os11.app.service.area.AreaService
import com.os11.app.service.project.ProjectService
import com.os11.app.service.settings.AppearanceSettings
import com.os11.app.service.settings.ModuleItem
import com.os11.app.service.settings.NotificationSettings
import com.os11.app.service.settings.ProfilePreset
import com.os11.app.service.settings.SecuritySettings
import com.os11.app.service.settings.SettingsService
import com.os11.app.service.tag.TagService
import com.os11.app.service.task.TaskService
import com.os11.domain.id.DeterministicIds
import dagger.hilt.android.lifecycle.HiltViewModel
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
    val modules: List<ModuleItem> = emptyList(),
    val activePreset: String = "custom",
    val dataCounts: SettingsDataCounts = SettingsDataCounts(),
    val canAuthenticate: Boolean = false,
)

@HiltViewModel
@Suppress("TooManyFunctions")
class SettingsViewModel @Inject constructor(
    private val settingsService: SettingsService,
    private val appLockManager: AppLockManager,
    private val appShortcutManager: AppShortcutManager,
    private val taskService: TaskService,
    private val areaService: AreaService,
    private val projectService: ProjectService,
    private val tagService: TagService,
) : ViewModel() {

    private val workspaceId = DeterministicIds.defaultWorkspaceId()

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
    ) { (appearance, notification, security), (modules, preset, counts) ->
        SettingsUiState(
            appearance = appearance,
            notification = notification,
            security = security,
            modules = modules,
            activePreset = preset,
            dataCounts = counts,
            canAuthenticate = appLockManager.canAuthenticate(),
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
}
