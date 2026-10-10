package com.os11.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import com.os11.app.ui.theme.OS11Theme

/**
 * OS11 Chip component for tags, date filters, and status badges.
 */
@Composable
fun OSChip(
    label: String,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
    leadingContent: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    tintColor: Color? = null,
) {
    val colors = OS11Theme.colors
    val spacing = OS11Theme.spacing
    val radius = OS11Theme.radius
    val layout = OS11Theme.layout
    val type = OS11Theme.type

    val shape = RoundedCornerShape(radius.xs)
    val interactionSource = remember { MutableInteractionSource() }

    val bgColor = when {
        selected -> colors.surfaceSelected
        tintColor != null -> tintColor.copy(alpha = 0.12f)
        else -> colors.surfaceRaised
    }

    val borderColor = when {
        selected -> colors.accent
        tintColor != null -> tintColor.copy(alpha = 0.30f)
        else -> colors.borderDefault
    }

    val textColor = when {
        selected -> colors.accent
        tintColor != null -> tintColor
        else -> colors.textSecondary
    }

    val clickableModifier = if (onClick != null) {
        Modifier
            .defaultMinSize(minHeight = layout.minTouchTarget)
            .clickable(
                role = Role.Button,
                interactionSource = interactionSource,
                indication = ripple(color = colors.surfaceHover),
                onClick = onClick,
            )
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .then(clickableModifier)
            .clip(shape)
            .border(spacing.px, borderColor, shape)
            .background(bgColor)
            .padding(horizontal = spacing.s2, vertical = spacing.s1),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.s1),
        ) {
            if (leadingContent != null) {
                leadingContent()
            }

            Text(
                text = label,
                style = type.chip.copy(color = textColor),
            )

            if (trailingContent != null) {
                trailingContent()
            }
        }
    }
}
