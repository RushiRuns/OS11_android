package com.os11.app

import android.content.Intent
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

    private val quickAddLaunchText = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    private val initialRoute = kotlinx.coroutines.flow.MutableStateFlow<com.os11.app.ui.navigation.OS11Route?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleIntent(intent)
        observeSecureScreen()

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
            val quickAddText by quickAddLaunchText.collectAsState()
            val targetRoute by initialRoute.collectAsState()
            val onboardingCompleted by settingsService.observeOnboardingCompleted()
                .collectAsState(initial = null)

            val effectiveInitialRoute = remember(targetRoute, onboardingCompleted) {
                when {
                    targetRoute != null -> targetRoute
                    onboardingCompleted == false -> com.os11.app.ui.navigation.OS11Route.Onboarding
                    else -> null
                }
            }

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
                    OS11AppShell(
                        undoManager = undoManager,
                        quickAddLaunchText = quickAddText,
                        initialRoute = effectiveInitialRoute,
                        onDismissQuickAdd = { quickAddLaunchText.value = null },
                    )
                }
            }

            LaunchedEffect(Unit) {
                deferredStartupManager.onFirstFrameRendered()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(incomingIntent: Intent?) {
        if (incomingIntent == null) return
        when (incomingIntent.action) {
            Intent.ACTION_SEND -> {
                val text = incomingIntent.getStringExtra(Intent.EXTRA_TEXT).orEmpty()
                val subject = incomingIntent.getStringExtra(Intent.EXTRA_SUBJECT).orEmpty()
                val content = when {
                    subject.isNotBlank() && text.isNotBlank() && subject != text -> "$subject $text"
                    text.isNotBlank() -> text
                    else -> subject
                }
                quickAddLaunchText.value = content
            }
            com.os11.app.platform.shortcuts.AppShortcutManager.ACTION_QUICK_ADD -> {
                quickAddLaunchText.value = ""
            }
            com.os11.app.platform.shortcuts.AppShortcutManager.ACTION_MY_DAY -> {
                initialRoute.value = com.os11.app.ui.navigation.OS11Route.MyDay
            }
            com.os11.app.platform.shortcuts.AppShortcutManager.ACTION_POMODORO -> {
                initialRoute.value = com.os11.app.ui.navigation.OS11Route.Pomodoro
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

    private fun observeSecureScreen() {
        lifecycleScope.launch {
            settingsService.observeSecuritySettings().collect { security ->
                if (security.secureScreen) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
            }
        }
    }
}
