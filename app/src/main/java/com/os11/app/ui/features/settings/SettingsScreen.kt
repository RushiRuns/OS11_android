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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Warning
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

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(colors.surfaceBase)
            .padding(spacing.s4),
        verticalArrangement = Arrangement.spacedBy(spacing.s4),
    ) {
        item(key = "title") {
            Text(
                text = "Settings",
                style = type.viewTitle.copy(color = colors.textPrimary),
            )
        }

        // Section 1: Preferences & Modules
        item(key = "core_settings_group") {
            Text(
                text = "Configuration & Modules",
                style = type.sectionHeader.copy(color = colors.textSecondary),
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = spacing.s2)
                    .clip(RoundedCornerShape(radius.lg))
                    .background(colors.surfaceRaised)
                    .border(1.dp, colors.borderGlass, RoundedCornerShape(radius.lg)),
            ) {
                SettingsNavRow(
                    title = "Appearance",
                    subtitle = "Accent color, font size, density, motion",
                    icon = Icons.Outlined.Edit,
                    onClick = { onNavigate(OS11Route.SettingsAppearance) },
                )
                SettingsNavRow(
                    title = "Notifications & Alarms",
                    subtitle = "Follow-up time, stale days, quiet hours",
                    icon = Icons.Outlined.Notifications,
                    onClick = { onNavigate(OS11Route.SettingsNotifications) },
                )
                SettingsNavRow(
                    title = "Modules & Presets",
                    subtitle = "Profile presets and device-local feature toggles",
                    icon = Icons.Outlined.Settings,
                    onClick = { onNavigate(OS11Route.SettingsModules) },
                )
            }
        }

        // Section 2: Storage & Security
        item(key = "data_security_group") {
            Text(
                text = "Storage & Security",
                style = type.sectionHeader.copy(color = colors.textSecondary),
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = spacing.s2)
                    .clip(RoundedCornerShape(radius.lg))
                    .background(colors.surfaceRaised)
                    .border(1.dp, colors.borderGlass, RoundedCornerShape(radius.lg)),
            ) {
                SettingsNavRow(
                    title = "Data & Storage",
                    subtitle = "Local SQLite metrics, synced settings allowlist",
                    icon = Icons.Outlined.Info,
                    onClick = { onNavigate(OS11Route.SettingsData) },
                )
                SettingsNavRow(
                    title = "Privacy & App Lock",
                    subtitle = "Biometric lock, PIN fallback, secure screen",
                    icon = Icons.Outlined.Lock,
                    onClick = { onNavigate(OS11Route.SettingsPrivacy) },
                )
                SettingsNavRow(
                    title = "About OS11",
                    subtitle = "Version 1.0.0, architecture pillars",
                    icon = Icons.Outlined.Info,
                    onClick = { onNavigate(OS11Route.SettingsAbout) },
                )
            }
        }

        // Section 3: Shortcuts & Tools
        item(key = "tools_group") {
            Text(
                text = "Productivity Tools",
                style = type.sectionHeader.copy(color = colors.textSecondary),
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = spacing.s2)
                    .clip(RoundedCornerShape(radius.lg))
                    .background(colors.surfaceRaised)
                    .border(1.dp, colors.borderGlass, RoundedCornerShape(radius.lg)),
            ) {
                SettingsNavRow(
                    title = "Tag Manager",
                    subtitle = "Rename, recolor, and merge tags",
                    icon = Icons.AutoMirrored.Outlined.List,
                    onClick = { onNavigate(OS11Route.TagManager) },
                )
                SettingsNavRow(
                    title = "Notification Center",
                    subtitle = "View in-app notification history and alerts",
                    icon = Icons.Outlined.Notifications,
                    onClick = { onNavigate(OS11Route.NotificationCenter) },
                )
                SettingsNavRow(
                    title = "Reminder Reliability",
                    subtitle = "Battery optimization & exact alarm settings",
                    icon = Icons.Outlined.Warning,
                    onClick = { onNavigate(OS11Route.BatteryGuidance) },
                )
            }
        }
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

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(spacing.s4),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.s3),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colors.accent,
            modifier = Modifier.size(24.dp),
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = type.body.copy(color = colors.textPrimary),
            )
            Text(
                text = subtitle,
                style = type.meta.copy(color = colors.textSecondary),
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
            contentDescription = null,
            tint = colors.textTertiary,
            modifier = Modifier.size(18.dp),
        )
    }
}
