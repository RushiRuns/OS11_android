package com.os11.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.os11.app.ui.shell.OS11AppShell
import com.os11.app.ui.theme.OS11Theme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Single activity Compose host for OS11 Android.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OS11Theme {
                OS11AppShell()
            }
        }
    }
}
