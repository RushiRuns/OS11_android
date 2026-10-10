package com.os11.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import com.os11.app.ui.theme.OS11Theme

enum class OSButtonVariant {
    Primary,
    Secondary,
    Danger,
    Ghost,
}

/**
 * Standard button component adhering to OS11 tokens and touch target guidelines.
 *
 * Guaranteed 48dp minimum hit area, token-based colors and radii, semantics role Button.
 */
@Composable
fun OSButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    variant: OSButtonVariant = OSButtonVariant.Primary,
    leadingIcon: (@Composable () -> Unit)? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val colors = OS11Theme.colors
    val spacing = OS11Theme.spacing
    val radius = OS11Theme.radius
    val layout = OS11Theme.layout

    val (backgroundColor, contentColor) = resolveButtonColors(variant, enabled, colors)

    val shape = RoundedCornerShape(radius.sm)
    val borderModifier = if (variant == OSButtonVariant.Secondary && enabled) {
        Modifier.border(spacing.px, colors.borderDefault, shape)
    } else {
        Modifier
    }

    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .defaultMinSize(
                minWidth = layout.minTouchTarget,
                minHeight = layout.minTouchTarget,
            )
            .clip(shape)
            .then(borderModifier)
            .background(backgroundColor)
            .clickable(
                enabled = enabled,
                role = Role.Button,
                interactionSource = interactionSource,
                indication = ripple(color = colors.surfaceHover),
                onClick = onClick,
            )
            .padding(horizontal = spacing.s4, vertical = spacing.s2),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.material3.LocalContentColor provides contentColor,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing.s2),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (leadingIcon != null) {
                    leadingIcon()
                }
                content()
            }
        }
    }
}

private fun resolveButtonColors(
    variant: OSButtonVariant,
    enabled: Boolean,
    colors: com.os11.app.ui.theme.OS11Colors,
): Pair<Color, Color> {
    if (!enabled) return colors.surfaceRaised to colors.textDisabled
    return when (variant) {
        OSButtonVariant.Primary -> colors.accent to colors.textOnAccent
        OSButtonVariant.Secondary -> colors.surfaceRaised to colors.textPrimary
        OSButtonVariant.Danger -> colors.danger to colors.textOnAccent
        OSButtonVariant.Ghost -> Color.Transparent to colors.accent
    }
}

/**
 * Icon button with guaranteed 48dp minimum touch target.
 */
@Composable
fun OSIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentDescription: String? = null,
    content: @Composable () -> Unit,
) {
    val colors = OS11Theme.colors
    val radius = OS11Theme.radius
    val layout = OS11Theme.layout
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .size(layout.minTouchTarget)
            .clip(RoundedCornerShape(radius.sm))
            .clickable(
                enabled = enabled,
                role = Role.Button,
                interactionSource = interactionSource,
                indication = ripple(color = colors.surfaceHover),
                onClick = onClick,
            )
            .semantics {
                role = Role.Button
                if (contentDescription != null) {
                    this.contentDescription = contentDescription
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}
