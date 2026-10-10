package com.os11.app.ui.features.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.os11.app.ui.navigation.OS11Route
import com.os11.app.ui.theme.OS11Theme

@Composable
fun SettingsScreen(onNavigate: (OS11Route) -> Unit = {}, modifier: Modifier = Modifier) {
    val colors = OS11Theme.colors
    val spacing = OS11Theme.spacing
    val type = OS11Theme.type
    val radius = OS11Theme.radius

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.surfaceBase)
            .padding(spacing.s4),
        verticalArrangement = Arrangement.spacedBy(spacing.s4),
    ) {
        Text(
            text = "Settings",
            style = type.viewTitle.copy(color = colors.textPrimary),
        )

        Text(
            text = "Organization & Metadata",
            style = type.sectionHeader.copy(color = colors.textSecondary),
        )

        SettingsNavRow(
            title = "Tag Manager",
            subtitle = "Rename, recolor, and merge tags",
            icon = Icons.AutoMirrored.Outlined.List,
            onClick = { onNavigate(OS11Route.TagManager) },
        )

        Text(
            text = "Notifications & Activity",
            style = type.sectionHeader.copy(color = colors.textSecondary),
        )

        SettingsNavRow(
            title = "Notification Center",
            subtitle = "View in-app notification history and alerts",
            icon = Icons.Outlined.Notifications,
            onClick = { onNavigate(OS11Route.NotificationCenter) },
        )
    }
}

@Composable
private fun SettingsNavRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = OS11Theme.colors
    val spacing = OS11Theme.spacing
    val type = OS11Theme.type
    val radius = OS11Theme.radius

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(radius.md))
            .background(colors.surfaceRaised)
            .border(spacing.px, colors.borderSubtle, RoundedCornerShape(radius.md))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = spacing.s4, vertical = spacing.s3),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.s3),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(24.dp),
            )
            Column {
                Text(
                    text = title,
                    style = type.body.copy(color = colors.textPrimary),
                )
                Text(
                    text = subtitle,
                    style = type.meta.copy(color = colors.textSecondary),
                )
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
            contentDescription = null,
            tint = colors.textTertiary,
            modifier = Modifier.size(20.dp),
        )
    }
}
