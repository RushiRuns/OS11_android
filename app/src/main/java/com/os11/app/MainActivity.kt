package com.os11.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.os11.app.ui.theme.OS11Theme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Single activity Compose host for OS11 Android.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OS11Theme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = OS11Theme.colors.surfaceBase,
                ) {
                    // Empty app placeholder for Phase 0
                }
            }
        }
    }
}
