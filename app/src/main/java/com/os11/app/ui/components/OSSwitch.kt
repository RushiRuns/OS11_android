package com.os11.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.os11.app.ui.theme.OS11AnimationSpecs
import com.os11.app.ui.theme.OS11Theme

/**
 * Switch toggle component styled with OS11 tokens.
 *
 * Guaranteed 48dp hit area, track width 44dp, track height 24dp, thumb 20dp.
 */
@Composable
fun OSSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentDescription: String? = null,
) {
    val colors = OS11Theme.colors
    val spacing = OS11Theme.spacing
    val radius = OS11Theme.radius
    val layout = OS11Theme.layout
    val reduceMotion = OS11Theme.reduceMotion
    val interactionSource = remember { MutableInteractionSource() }

    val trackWidth = 44.dp
    val trackHeight = 24.dp
    val thumbSize = 20.dp
    val thumbTravel = trackWidth - thumbSize - 4.dp // 2dp padding each side

    val thumbOffsetFraction by animateFloatAsState(
        targetValue = if (checked) 1.0f else 0.0f,
        animationSpec = OS11AnimationSpecs.fastTween(reduceMotion),
        label = "SwitchThumbOffset",
    )

    val trackColor = when {
        !enabled -> colors.surfaceRaised
        checked -> colors.accent
        else -> colors.surfaceRaised
    }

    val borderColor = if (checked) colors.accent else colors.borderDefault
    val thumbColor = if (enabled) Color.White else colors.textDisabled

    Box(
        modifier = modifier
            .size(width = layout.minTouchTarget, height = layout.minTouchTarget)
            .clickable(
                enabled = enabled && onCheckedChange != null,
                role = Role.Switch,
                interactionSource = interactionSource,
                indication = ripple(bounded = false, radius = layout.minTouchTarget / 2),
                onClick = { onCheckedChange?.invoke(!checked) },
            )
            .semantics {
                role = Role.Switch
                if (contentDescription != null) {
                    this.contentDescription = contentDescription
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        val trackShape = RoundedCornerShape(radius.full)
        Box(
            modifier = Modifier
                .size(width = trackWidth, height = trackHeight)
                .clip(trackShape)
                .border(spacing.px, borderColor, trackShape)
                .background(trackColor)
                .padding(2.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Box(
                modifier = Modifier
                    .offset(x = thumbTravel * thumbOffsetFraction)
                    .size(thumbSize)
                    .clip(CircleShape)
                    .background(thumbColor),
            )
        }
    }
}
