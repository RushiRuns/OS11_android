package com.os11.app.platform.biometrics

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.os11.app.service.settings.SettingsService
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class AppLockManagerTest {

    private val testDispatcher = StandardTestDispatcher()
    private val settingsService: SettingsService = mockk(relaxed = true)
    private lateinit var appLockManager: AppLockManager

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        val context = ApplicationProvider.getApplicationContext<Context>()
        appLockManager = AppLockManager(context, settingsService)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun lock_is_not_triggered_when_app_lock_disabled() = runTest {
        coEvery { settingsService.isAppLockEnabled() } returns false

        appLockManager.onAppBackgrounded()
        advanceUntilIdle()
        appLockManager.onAppForegrounded()
        advanceUntilIdle()

        assertFalse(appLockManager.isLocked.value)
    }

    @Test
    fun lock_is_triggered_when_enabled_and_timeout_is_zero() = runTest {
        coEvery { settingsService.isAppLockEnabled() } returns true
        coEvery { settingsService.getAppLockTimeoutSeconds() } returns 0

        appLockManager.onAppBackgrounded()
        advanceUntilIdle()
        appLockManager.onAppForegrounded()
        advanceUntilIdle()

        assertTrue(appLockManager.isLocked.value)

        appLockManager.unlock()
        assertFalse(appLockManager.isLocked.value)
    }

    @Test
    fun manual_lock_sets_locked_state() {
        appLockManager.lock()
        assertTrue(appLockManager.isLocked.value)

        appLockManager.unlock()
        assertFalse(appLockManager.isLocked.value)
    }
}
