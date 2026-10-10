package com.os11.app.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.os11.app.ui.theme.OS11Motion
import com.os11.app.ui.theme.OS11Theme

enum class OSPriority {
    None,
    Low,
    Medium,
    High,
    Critical,
}

/**
 * Priority bar component.
 *
 * Rules:
 * 1. Priority is a 2.5dp left border only. Never a badge, icon, or separate element.
 * 2. Critical priority pulses (alpha only, reduced-motion aware).
 */
@Composable
fun OSPriorityBar(
    priority: OSPriority,
    modifier: Modifier = Modifier,
) {
    if (priority == OSPriority.None) return

    val colors = OS11Theme.colors
    val layout = OS11Theme.layout
    val reduceMotion = OS11Theme.reduceMotion

    val baseColor = when (priority) {
        OSPriority.None -> colors.priorityNone
        OSPriority.Low -> colors.priorityLow
        OSPriority.Medium -> colors.priorityMedium
        OSPriority.High -> colors.priorityHigh
        OSPriority.Critical -> colors.priorityCritical
    }

    val alpha = if (priority == OSPriority.Critical && !reduceMotion) {
        val infiniteTransition = rememberInfiniteTransition(label = "PriorityPulse")
        val animatedAlpha by infiniteTransition.animateFloat(
            initialValue = 1.0f,
            targetValue = 0.35f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = OS11Motion.CriticalPulse / 2,
                    easing = OS11Motion.EaseInOut,
                ),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "CriticalAlpha",
        )
        animatedAlpha
    } else {
        1.0f
    }

    Box(
        modifier = modifier
            .width(layout.priorityBorderWidth)
            .fillMaxHeight()
            .background(baseColor.copy(alpha = alpha)),
    )
}
