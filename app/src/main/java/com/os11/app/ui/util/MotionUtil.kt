package com.os11.app.ui.util

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.os11.app.ui.theme.OS11Theme

/**
 * Returns true if system animator duration scale is 0 (animations off) or
 * if app-level reduced motion is active.
 *
 * Every animated component reads LocalReduceMotion or this helper (ARCHITECTURE.md rule 13).
 */
@Composable
fun rememberReducedMotion(userReducedMotionSetting: Boolean = OS11Theme.reduceMotion): Boolean {
    val context = LocalContext.current
    return remember(context, userReducedMotionSetting) {
        if (userReducedMotionSetting) {
            true
        } else {
            val scale = try {
                Settings.Global.getFloat(
                    context.contentResolver,
                    Settings.Global.ANIMATOR_DURATION_SCALE,
                    1.0f,
                )
            } catch (_: Exception) {
                1.0f
            }
            scale == 0f
        }
    }
}
