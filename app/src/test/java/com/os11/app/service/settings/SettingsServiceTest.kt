package com.os11.app.service.settings

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.os11.app.data.db.DatabaseInitializer
import com.os11.app.data.db.OS11Database
import com.os11.app.data.repository.ModuleRepositoryImpl
import com.os11.app.data.repository.SettingsRepositoryImpl
import com.os11.app.data.sync.TrackedWriter
import com.os11.app.ui.theme.OS11Density
import com.os11.app.ui.theme.OS11FontStep
import com.os11.domain.id.DefaultIdGenerator
import com.os11.domain.logic.sync.DefaultHlcClock
import com.os11.domain.time.TestClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SettingsServiceTest {

    private lateinit var db: OS11Database
    private lateinit var testClock: TestClock
    private lateinit var trackedWriter: TrackedWriter
    private lateinit var settingsService: SettingsService

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
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun get_and_set_setting_updates_value() = runBlocking {
        assertEquals("\"#4da3ff\"", settingsService.getSetting("accent_color"))
        settingsService.setAccentColor("#10b981")
        assertEquals("\"#10b981\"", settingsService.getSetting("accent_color"))
    }

    @Test
    fun observeAppearanceSettings_maps_defaults_and_updates() = runBlocking {
        val initial = settingsService.observeAppearanceSettings().first()
        assertEquals("#4da3ff", initial.accentColorHex)
        assertEquals(OS11FontStep.Md, initial.fontStep)
        assertEquals(OS11Density.Comfortable, initial.density)
        assertFalse(initial.reduceMotion)
        assertTrue(initial.hapticsEnabled)

        settingsService.setFontSize("lg")
        settingsService.setDensity("compact")
        settingsService.setReduceMotion(true)
        settingsService.setHapticsEnabled(false)

        val updated = settingsService.observeAppearanceSettings().first()
        assertEquals(OS11FontStep.Lg, updated.fontStep)
        assertEquals(OS11Density.Compact, updated.density)
        assertTrue(updated.reduceMotion)
        assertFalse(updated.hapticsEnabled)
    }

    @Test
    fun observeNotificationSettings_maps_and_updates() = runBlocking {
        val initial = settingsService.observeNotificationSettings().first()
        assertEquals("09:00", initial.followUpNotifyTime)
        assertEquals(7, initial.waitingStaleDays)
        assertEquals("00:00", initial.dayStartsAt)

        settingsService.setFollowUpNotifyTime("10:00")
        settingsService.setWaitingStaleDays(14)
        settingsService.setDayStartsAt("04:00")

        val updated = settingsService.observeNotificationSettings().first()
        assertEquals("10:00", updated.followUpNotifyTime)
        assertEquals(14, updated.waitingStaleDays)
        assertEquals("04:00", updated.dayStartsAt)
    }

    @Test
    fun observeSecuritySettings_maps_and_updates() = runBlocking {
        val initial = settingsService.observeSecuritySettings().first()
        assertFalse(initial.appLockEnabled)
        assertEquals(0, initial.appLockTimeoutSeconds)
        assertFalse(initial.secureScreen)

        settingsService.setAppLockEnabled(true)
        settingsService.setAppLockTimeoutSeconds(300)
        settingsService.setSecureScreen(true)

        val updated = settingsService.observeSecuritySettings().first()
        assertTrue(updated.appLockEnabled)
        assertEquals(300, updated.appLockTimeoutSeconds)
        assertTrue(updated.secureScreen)
    }

    @Test
    fun applyProfilePreset_minimal_enables_only_core_tasks() = runBlocking {
        settingsService.applyProfilePreset(ProfilePreset.Minimal.id)
        val enabledModules = settingsService.observeEnabledModuleNames().first()

        assertTrue(enabledModules.contains("my_day"))
        assertTrue(enabledModules.contains("nlp_parsing"))
        assertFalse(enabledModules.contains("project_management"))
        assertFalse(enabledModules.contains("pomodoro"))
        assertFalse(enabledModules.contains("dashboard"))
        assertFalse(enabledModules.contains("anytime"))

        assertEquals("minimal", settingsService.observeActiveProfilePreset().first())
    }

    @Test
    fun applyProfilePreset_gtd_enables_smart_list_buckets() = runBlocking {
        settingsService.applyProfilePreset(ProfilePreset.GTD.id)
        val enabledModules = settingsService.observeEnabledModuleNames().first()

        assertTrue(enabledModules.contains("anytime"))
        assertTrue(enabledModules.contains("someday"))
        assertTrue(enabledModules.contains("waiting_for"))
        assertTrue(enabledModules.contains("dashboard"))
        assertTrue(enabledModules.contains("pomodoro"))

        assertEquals("gtd", settingsService.observeActiveProfilePreset().first())
    }

    @Test
    fun toggling_individual_module_resets_preset_to_custom() = runBlocking {
        settingsService.applyProfilePreset(ProfilePreset.Standard.id)
        assertEquals("standard", settingsService.observeActiveProfilePreset().first())

        settingsService.setModuleEnabled("pomodoro", false)
        assertFalse(settingsService.isModuleEnabled("pomodoro"))
        assertEquals("custom", settingsService.observeActiveProfilePreset().first())
    }
}
