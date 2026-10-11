package com.os11.app.ui.features.settings

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.os11.app.ui.components.OSChip
import com.os11.app.ui.components.OSIconButton
import com.os11.app.ui.navigation.OS11Route
import com.os11.app.ui.theme.OS11Theme

@Composable
@Suppress("TooGenericExceptionCaught")
fun SettingsNotificationsScreen(
    onNavigateBack: () -> Unit,
    onNavigate: (OS11Route) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val colors = OS11Theme.colors
    val spacing = OS11Theme.spacing
    val type = OS11Theme.type
    val radius = OS11Theme.radius
    val context = LocalContext.current

    val followUpTimes = listOf("08:00", "09:00", "10:00", "18:00")
    val staleDayOptions = listOf(3, 7, 14, 30)
    val dayStartTimes = listOf("00:00", "04:00", "06:00", "08:00")

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(colors.surfaceBase)
            .padding(horizontal = spacing.s4),
        verticalArrangement = Arrangement.spacedBy(spacing.s4),
    ) {
        item(key = "top_bar") {
            Spacer(modifier = Modifier.height(spacing.s2))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.s2),
            ) {
                OSIconButton(
                    onClick = onNavigateBack,
                    contentDescription = "Navigate back",
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        tint = colors.textSecondary,
                    )
                }
                Text(
                    text = "Notifications",
                    style = type.viewTitle.copy(color = colors.textPrimary),
                )
            }
        }

        // 1. Follow-up notify time
        item(key = "follow_up_time_section") {
            Text(
                text = "Follow-up Notification Time",
                style = type.sectionHeader.copy(color = colors.textPrimary),
            )
            Text(
                text = "Daily time to receive morning alerts for tasks requiring follow-up (synced)",
                style = type.meta.copy(color = colors.textSecondary),
            )
            Spacer(modifier = Modifier.height(spacing.s2))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.s2),
            ) {
                followUpTimes.forEach { time ->
                    OSChip(
                        label = time,
                        selected = state.notification.followUpNotifyTime == time,
                        onClick = { viewModel.setFollowUpNotifyTime(time) },
                    )
                }
            }
        }

        // 2. Waiting stale days
        item(key = "stale_days_section") {
            Text(
                text = "Waiting For Stale Threshold",
                style = type.sectionHeader.copy(color = colors.textPrimary),
            )
            Text(
                text = "Highlight delegated tasks as stale after this many days of inactivity (synced)",
                style = type.meta.copy(color = colors.textSecondary),
            )
            Spacer(modifier = Modifier.height(spacing.s2))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.s2),
            ) {
                staleDayOptions.forEach { days ->
                    OSChip(
                        label = "$days Days",
                        selected = state.notification.waitingStaleDays == days,
                        onClick = { viewModel.setWaitingStaleDays(days) },
                    )
                }
            }
        }

        // 3. Day starts at
        item(key = "day_starts_at_section") {
            Text(
                text = "Day Starts At",
                style = type.sectionHeader.copy(color = colors.textPrimary),
            )
            Text(
                text = "Hour when 'Today' rolls over to the next day for night owls (synced)",
                style = type.meta.copy(color = colors.textSecondary),
            )
            Spacer(modifier = Modifier.height(spacing.s2))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.s2),
            ) {
                dayStartTimes.forEach { time ->
                    val label = if (time == "00:00") "Midnight" else time
                    OSChip(
                        label = label,
                        selected = state.notification.dayStartsAt == time,
                        onClick = { viewModel.setDayStartsAt(time) },
                    )
                }
            }
        }

        // 4. System Channels & Navigation Links
        item(key = "system_links_section") {
            Text(
                text = "System & Channels",
                style = type.sectionHeader.copy(color = colors.textPrimary),
            )
            Spacer(modifier = Modifier.height(spacing.s2))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(radius.lg))
                    .background(colors.surfaceRaised)
                    .border(1.dp, colors.borderGlass, RoundedCornerShape(radius.lg)),
            ) {
                SettingsActionRow(
                    title = "System Notification Channels",
                    subtitle = "Manage Android system channel sounds and priority",
                    icon = Icons.Outlined.Settings,
                    onClick = {
                        try {
                            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            }
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            // Fallback if OEM settings intent fails
                        }
                    },
                )
                SettingsActionRow(
                    title = "Battery Optimization Guidance",
                    subtitle = "Ensure exact alarms fire reliably in background",
                    icon = Icons.Outlined.Warning,
                    onClick = { onNavigate(OS11Route.BatteryGuidance) },
                )
                SettingsActionRow(
                    title = "Notification Center",
                    subtitle = "View in-app notifications and history",
                    icon = Icons.Outlined.Notifications,
                    onClick = { onNavigate(OS11Route.NotificationCenter) },
                )
            }
        }
    }
}

@Composable
private fun SettingsActionRow(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit) {
    val colors = OS11Theme.colors
    val spacing = OS11Theme.spacing
    val type = OS11Theme.type

    Row(
        modifier = Modifier
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
