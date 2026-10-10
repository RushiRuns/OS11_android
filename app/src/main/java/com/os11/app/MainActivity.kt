package com.os11.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.os11.app.platform.startup.DeferredStartupManager
import com.os11.app.service.undo.UndoManager
import com.os11.app.ui.shell.OS11AppShell
import com.os11.app.ui.theme.OS11Theme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Single activity Compose host for OS11 Android.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var deferredStartupManager: DeferredStartupManager

    @Inject
    lateinit var undoManager: UndoManager

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OS11Theme {
                OS11AppShell(undoManager = undoManager)
            }
            LaunchedEffect(Unit) {
                deferredStartupManager.onFirstFrameRendered()
            }
        }
    }
}
