package com.os11.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.os11.app.ui.theme.OS11Theme

/**
 * Standard empty state component per DONE.md Rule 2.
 */
@Composable
fun OSEmptyState(
    title: String,
    description: String? = null,
    modifier: Modifier = Modifier,
    icon: (@Composable () -> Unit)? = null,
    action: (@Composable () -> Unit)? = null,
) {
    val colors = OS11Theme.colors
    val spacing = OS11Theme.spacing
    val type = OS11Theme.type

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(spacing.s8),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            icon()
            Spacer(modifier = Modifier.height(spacing.s4))
        }

        Text(
            text = title,
            style = type.panelTitle.copy(color = colors.textPrimary),
            textAlign = TextAlign.Center,
        )

        if (description != null) {
            Spacer(modifier = Modifier.height(spacing.s2))
            Text(
                text = description,
                style = type.meta.copy(color = colors.textSecondary),
                textAlign = TextAlign.Center,
            )
        }

        if (action != null) {
            Spacer(modifier = Modifier.height(spacing.s6))
            action()
        }
    }
}
