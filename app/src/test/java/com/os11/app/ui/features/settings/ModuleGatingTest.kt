package com.os11.app.ui.features.settings

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.os11.app.data.db.DatabaseInitializer
import com.os11.app.data.db.OS11Database
import com.os11.app.data.repository.ModuleRepositoryImpl
import com.os11.app.data.repository.SettingsRepositoryImpl
import com.os11.app.data.sync.TrackedWriter
import com.os11.app.platform.shortcuts.AppShortcutManager
import com.os11.app.service.search.SearchService
import com.os11.app.service.settings.SettingsService
import com.os11.app.service.task.TaskService
import com.os11.app.ui.features.search.SearchActionKind
import com.os11.app.ui.features.search.SearchViewModel
import com.os11.domain.id.DefaultIdGenerator
import com.os11.domain.logic.sync.DefaultHlcClock
import com.os11.domain.time.TestClock
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ModuleGatingTest {

    private lateinit var db: OS11Database
    private lateinit var testClock: TestClock
    private lateinit var trackedWriter: TrackedWriter
    private lateinit var settingsService: SettingsService
    private lateinit var shortcutManager: AppShortcutManager
    private val searchService: SearchService = mockk(relaxed = true)
    private val taskService: TaskService = mockk(relaxed = true)

    @Before
    fun setup() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = OS11Database.buildInMemory(context)
        testClock = TestClock(1_000_000L)
        val hlcClock = DefaultHlcClock(testClock, "test-device")
        trackedWriter = TrackedWriter(db, hlcClock, testClock, DefaultIdGenerator())
        DatabaseInitializer.seedDefaults(db, testClock)

        val settingsRepo = SettingsRepositoryImpl(db, trackedWriter)
        val moduleRepo = ModuleRepositoryImpl(db, trackedWriter)
        settingsService = SettingsService(
            settingsRepository = settingsRepo,
            moduleRepository = moduleRepo,
            ioDispatcher = Dispatchers.Unconfined,
        )
        shortcutManager = AppShortcutManager(context)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun disabling_module_removes_action_from_search() {
        val viewModel = SearchViewModel(searchService, taskService, settingsService)

        // 1. Dashboard enabled
        val enabledModules = setOf("my_day", "dashboard", "pomodoro")
        val actionsWithDashboard = viewModel.buildActions("dash", enabledModules)
        assertTrue(
            actionsWithDashboard.any {
                it.kind == SearchActionKind.Navigate && it.id == "nav_dashboard"
            },
        )

        // 2. Dashboard disabled
        val disabledModules = setOf("my_day", "pomodoro")
        val actionsWithoutDashboard = viewModel.buildActions("dash", disabledModules)
        assertFalse(
            actionsWithoutDashboard.any {
                it.id == "nav_dashboard"
            },
        )
    }

    @Test
    fun disabling_pomodoro_removes_pomodoro_action_from_search() {
        val viewModel = SearchViewModel(searchService, taskService, settingsService)

        val actionsInitial = viewModel.buildActions("pomo", setOf("pomodoro", "my_day"))
        assertTrue(actionsInitial.any { it.id == "nav_pomodoro" })

        val actionsDisabled = viewModel.buildActions("pomo", setOf("my_day"))
        assertFalse(actionsDisabled.any { it.id == "nav_pomodoro" })
    }

    @Test
    fun shortcut_manager_removes_shortcuts_for_disabled_modules() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = AppShortcutManager(context)

        // 1. When all enabled: Quick Add, My Day, Pomodoro
        val initialShortcuts = manager.computeShortcutIds(setOf("my_day", "pomodoro"))
        assertTrue(initialShortcuts.contains("shortcut_quick_add"))
        assertTrue(initialShortcuts.contains("shortcut_my_day"))
        assertTrue(initialShortcuts.contains("shortcut_pomodoro"))

        // 2. When Pomodoro disabled: My Day preserved, Pomodoro removed
        val shortcutsWithoutPomodoro = manager.computeShortcutIds(setOf("my_day"))
        assertTrue(shortcutsWithoutPomodoro.contains("shortcut_quick_add"))
        assertTrue(shortcutsWithoutPomodoro.contains("shortcut_my_day"))
        assertFalse(shortcutsWithoutPomodoro.contains("shortcut_pomodoro"))

        // 3. When both disabled: only Quick Add preserved
        val shortcutsMinimal = manager.computeShortcutIds(emptySet())
        assertTrue(shortcutsMinimal.contains("shortcut_quick_add"))
        assertFalse(shortcutsMinimal.contains("shortcut_my_day"))
        assertFalse(shortcutsMinimal.contains("shortcut_pomodoro"))

        // Also ensure updateShortcuts runs safely without throwing
        manager.updateShortcuts(setOf("my_day", "pomodoro"))
        manager.updateShortcuts(setOf("my_day"))
        manager.updateShortcuts(emptySet())
    }

    @Test
    fun disabling_module_updates_enabled_modules_flow() = runBlocking {
        val initialModules = settingsService.observeEnabledModuleNames().first()
        assertTrue(initialModules.contains("agenda"))

        settingsService.setModuleEnabled("agenda", false)
        val updatedModules = settingsService.observeEnabledModuleNames().first()
        assertFalse(updatedModules.contains("agenda"))
    }

    @Test
    fun profile_preset_gating_updates_all_module_states() = runBlocking {
        // Minimal preset: daily focus only
        settingsService.applyProfilePreset("minimal")
        val minimalModules = settingsService.observeEnabledModuleNames().first()
        assertTrue(minimalModules.contains("my_day"))
        assertTrue(minimalModules.contains("nlp_parsing"))
        assertFalse(minimalModules.contains("project_management"))
        assertFalse(minimalModules.contains("pomodoro"))
        assertFalse(minimalModules.contains("dashboard"))
        assertFalse(minimalModules.contains("agenda"))

        // Full GTD preset: smart lists active
        settingsService.applyProfilePreset("gtd")
        val gtdModules = settingsService.observeEnabledModuleNames().first()
        assertTrue(gtdModules.contains("anytime"))
        assertTrue(gtdModules.contains("someday"))
        assertTrue(gtdModules.contains("waiting_for"))
        assertTrue(gtdModules.contains("pomodoro"))
        assertTrue(gtdModules.contains("dashboard"))
    }
}
