package com.os11.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.os11.app.ui.theme.OS11Theme

enum class OSTagDotVariant {
    TaskRow, // 18dp circle resting on task rows
    ProjectTree, // 10dp circle in the Browse tree
    PillDot, // 6dp dot in pills
}

/**
 * Tag / Project indicator dot component.
 */
@Composable
fun OSTagDot(color: Color, modifier: Modifier = Modifier, variant: OSTagDotVariant = OSTagDotVariant.TaskRow) {
    val layout = OS11Theme.layout
    val dotSize = when (variant) {
        OSTagDotVariant.TaskRow -> layout.tagDot
        OSTagDotVariant.ProjectTree -> layout.projectDot
        OSTagDotVariant.PillDot -> layout.pillDot
    }

    Box(
        modifier = modifier
            .size(dotSize)
            .clip(CircleShape)
            .background(color),
    )
}
