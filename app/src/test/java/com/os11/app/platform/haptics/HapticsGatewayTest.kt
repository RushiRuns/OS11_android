package com.os11.app.platform.haptics

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.os11.app.service.settings.SettingsService
import com.os11.app.ui.theme.OS11Haptic
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class HapticsGatewayTest {

    private val settingsService: SettingsService = mockk(relaxed = true)
    private lateinit var hapticsGateway: HapticsGateway

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        every { settingsService.observeHapticsEnabled() } returns flowOf(true)
        hapticsGateway = HapticsGateway(context, settingsService)
    }

    @Test
    fun performHaptic_runs_for_all_variants_without_throwing() {
        for (haptic in OS11Haptic.values()) {
            hapticsGateway.performHaptic(haptic)
        }
    }
}
