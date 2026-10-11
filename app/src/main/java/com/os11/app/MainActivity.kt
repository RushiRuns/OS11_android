package com.os11.app

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.os11.app.platform.biometrics.AppLockManager
import com.os11.app.platform.startup.DeferredStartupManager
import com.os11.app.service.settings.AppearanceSettings
import com.os11.app.service.settings.SettingsService
import com.os11.app.service.undo.UndoManager
import com.os11.app.ui.features.settings.lock.AppLockScreen
import com.os11.app.ui.shell.OS11AppShell
import com.os11.app.ui.theme.OS11DefaultAccent
import com.os11.app.ui.theme.OS11Theme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Single activity Compose host for OS11 Android.
 */
@AndroidEntryPoint
@Suppress("TooGenericExceptionCaught")
class MainActivity : FragmentActivity() {

    @Inject
    lateinit var deferredStartupManager: DeferredStartupManager

    @Inject
    lateinit var undoManager: UndoManager

    @Inject
    lateinit var appLockManager: AppLockManager

    @Inject
    lateinit var settingsService: SettingsService

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Observe secure screen setting (FLAG_SECURE)
        lifecycleScope.launch {
            settingsService.observeSecuritySettings().collect { security ->
                if (security.secureScreen) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
            }
        }

        setContent {
            val appearance by settingsService.observeAppearanceSettings()
                .collectAsState(initial = AppearanceSettings())

            val accent = remember(appearance.accentColorHex) {
                try {
                    Color(android.graphics.Color.parseColor(appearance.accentColorHex))
                } catch (_: Exception) {
                    OS11DefaultAccent
                }
            }

            val isLocked by appLockManager.isLocked.collectAsState()

            OS11Theme(
                accent = accent,
                fontStep = appearance.fontStep,
                density = appearance.density,
                reduceMotion = appearance.reduceMotion,
            ) {
                if (isLocked) {
                    AppLockScreen(
                        onUnlockClick = {
                            appLockManager.promptBiometric(this@MainActivity)
                        },
                    )
                } else {
                    OS11AppShell(undoManager = undoManager)
                }
            }

            LaunchedEffect(Unit) {
                deferredStartupManager.onFirstFrameRendered()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        appLockManager.onAppForegrounded()
    }

    override fun onStop() {
        super.onStop()
        appLockManager.onAppBackgrounded()
    }
}
