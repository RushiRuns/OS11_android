package com.os11.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
 * Custom OS11 Snackbar banner adhering to design tokens and reversible action pattern (Undo).
 */
@Composable
fun OSSnackbar(
    snackbarData: SnackbarData,
    modifier: Modifier = Modifier,
) {
    val colors = OS11Theme.colors
    val spacing = OS11Theme.spacing
    val radius = OS11Theme.radius
    val layout = OS11Theme.layout
    val type = OS11Theme.type
    val shape = RoundedCornerShape(radius.md)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .border(spacing.px, colors.borderStrong, shape)
            .background(colors.surfaceOverlay)
            .padding(horizontal = spacing.s4, vertical = spacing.s2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = snackbarData.visuals.message,
            style = type.body.copy(color = colors.textPrimary),
            modifier = Modifier.weight(1f),
        )

        val actionLabel = snackbarData.visuals.actionLabel
        if (actionLabel != null) {
            val interactionSource = remember { MutableInteractionSource() }
            Box(
                modifier = Modifier
                    .defaultMinSize(minHeight = layout.minTouchTarget)
                    .clip(RoundedCornerShape(radius.sm))
                    .clickable(
                        role = Role.Button,
                        interactionSource = interactionSource,
                        indication = ripple(color = colors.surfaceHover),
                        onClick = { snackbarData.performAction() },
                    )
                    .padding(horizontal = spacing.s2, vertical = spacing.s1),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = actionLabel,
                    style = type.chip.copy(color = colors.accent),
                )
            }
        }
    }
}

/**
 * SnackbarHost wired to the custom OSSnackbar design.
 */
@Composable
fun OSSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    SnackbarHost(
        hostState = hostState,
        modifier = modifier.padding(OS11Theme.spacing.s4),
        snackbar = { data -> OSSnackbar(snackbarData = data) },
    )
}
