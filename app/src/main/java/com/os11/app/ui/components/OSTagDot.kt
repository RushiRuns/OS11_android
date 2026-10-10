package com.os11.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.os11.app.ui.theme.OS11Theme

enum class OSTagDotVariant {
    TaskRow, // 18dp circle resting on task rows
    ProjectTree, // 10dp circle in the Browse tree
    PillDot, // 6dp dot in pills
}

/**
 * Tag / Project indicator dot component supporting color-blind safe shapes.
 */
@Composable
fun OSTagDot(
    color: Color,
    modifier: Modifier = Modifier,
    shape: OSTagShape = OSTagShape.Circle,
    variant: OSTagDotVariant = OSTagDotVariant.TaskRow,
    contentDescriptionText: String? = null,
) {
    val layout = OS11Theme.layout
    val radius = OS11Theme.radius
    val dotSize = when (variant) {
        OSTagDotVariant.TaskRow -> layout.tagDot
        OSTagDotVariant.ProjectTree -> layout.projectDot
        OSTagDotVariant.PillDot -> layout.pillDot
    }

    val semanticsModifier = modifier.semantics {
        contentDescription = contentDescriptionText ?: "Tag ${shape.label}"
    }

    when (shape) {
        OSTagShape.Circle -> {
            Box(
                modifier = semanticsModifier
                    .size(dotSize)
                    .clip(CircleShape)
                    .background(color),
            )
        }
        OSTagShape.Square -> {
            Box(
                modifier = semanticsModifier
                    .size(dotSize)
                    .clip(RoundedCornerShape(radius.xs))
                    .background(color),
            )
        }
        OSTagShape.Triangle -> {
            Canvas(modifier = semanticsModifier.size(dotSize)) {
                val path = Path().apply {
                    moveTo(size.width / 2f, 0f)
                    lineTo(size.width, size.height)
                    lineTo(0f, size.height)
                    close()
                }
                drawPath(path, color)
            }
        }
        OSTagShape.Diamond -> {
            Canvas(modifier = semanticsModifier.size(dotSize)) {
                val path = Path().apply {
                    moveTo(size.width / 2f, 0f)
                    lineTo(size.width, size.height / 2f)
                    lineTo(size.width / 2f, size.height)
                    lineTo(0f, size.height / 2f)
                    close()
                }
                drawPath(path, color)
            }
        }
    }
}
