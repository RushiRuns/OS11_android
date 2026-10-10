package com.os11.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import com.os11.app.ui.theme.OS11Theme

/**
 * Menu item model for OSGlassMenu.
 */
data class OSMenuItem(
    val title: String,
    val leadingIcon: (@Composable () -> Unit)? = null,
    val trailingContent: (@Composable () -> Unit)? = null,
    val onClick: () -> Unit,
)

/**
 * Glass menu surface per ADR-A07.
 *
 * Uses a translucent surface token (surfaceGlass) plus a 1dp border (borderGlass).
 * Used by Profile Avatar Menu, popovers, and quick selector menus.
 */
@Composable
fun OSGlassMenu(
    items: List<OSMenuItem>,
    modifier: Modifier = Modifier,
    header: (@Composable () -> Unit)? = null,
) {
    val colors = OS11Theme.colors
    val spacing = OS11Theme.spacing
    val radius = OS11Theme.radius
    val shape = RoundedCornerShape(radius.lg)

    Column(
        modifier = modifier
            .clip(shape)
            .border(spacing.px, colors.borderGlass, shape)
            .background(colors.surfaceGlass)
            .padding(spacing.s2),
        verticalArrangement = Arrangement.spacedBy(spacing.s1),
    ) {
        if (header != null) {
            header()
        }

        items.forEach { item ->
            OSMenuItemRow(item = item)
        }
    }
}

@Composable
fun OSMenuItemRow(item: OSMenuItem, modifier: Modifier = Modifier) {
    val colors = OS11Theme.colors
    val spacing = OS11Theme.spacing
    val radius = OS11Theme.radius
    val layout = OS11Theme.layout
    val type = OS11Theme.type
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = layout.minTouchTarget)
            .clip(RoundedCornerShape(radius.profileMenuItem))
            .clickable(
                role = Role.Button,
                interactionSource = interactionSource,
                indication = ripple(color = colors.surfaceProfileMenuHover),
                onClick = item.onClick,
            )
            .padding(horizontal = spacing.s3, vertical = spacing.s2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.s3),
    ) {
        if (item.leadingIcon != null) {
            item.leadingIcon.invoke()
        }

        Text(
            text = item.title,
            style = type.body.copy(color = colors.textPrimary),
            modifier = Modifier.weight(1f),
        )

        if (item.trailingContent != null) {
            item.trailingContent.invoke()
        }
    }
}
