package com.os11.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.os11.app.ui.theme.OS11AnimationSpecs
import com.os11.app.ui.theme.OS11Theme

/**
 * Circular progress ring drawn directly with Compose Canvas.
 */
@Composable
fun OSProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    strokeWidth: Dp = 3.dp,
    activeColor: Color = OS11Theme.colors.accent,
    trackColor: Color = OS11Theme.colors.surfaceRaised,
) {
    val reduceMotion = OS11Theme.reduceMotion
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = OS11AnimationSpecs.defaultTween(reduceMotion),
        label = "ProgressRingSweep",
    )

    Canvas(
        modifier = modifier
            .size(size)
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(
                    current = progress,
                    range = 0f..1f,
                )
            },
    ) {
        val stroke = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
        // Background track
        drawCircle(color = trackColor, style = stroke)
        // Foreground progress arc
        drawArc(
            color = activeColor,
            startAngle = -90f,
            sweepAngle = 360f * animatedProgress,
            useCenter = false,
            style = stroke,
        )
    }
}

/**
 * Linear progress bar styled with OS11 tokens.
 */
@Composable
fun OSProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    height: Dp = 6.dp,
    activeColor: Color = OS11Theme.colors.accent,
    trackColor: Color = OS11Theme.colors.surfaceRaised,
) {
    val radius = OS11Theme.radius
    val reduceMotion = OS11Theme.reduceMotion
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = OS11AnimationSpecs.defaultTween(reduceMotion),
        label = "ProgressBarFill",
    )

    val shape = RoundedCornerShape(radius.full)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(shape)
            .background(trackColor)
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(
                    current = progress,
                    range = 0f..1f,
                )
            },
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(animatedProgress)
                .fillMaxHeight()
                .clip(shape)
                .background(activeColor),
        )
    }
}
