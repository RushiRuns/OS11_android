package com.os11.app.ui.features.notification

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.os11.app.service.notification.NotificationItem
import com.os11.app.ui.components.OSChip
import com.os11.app.ui.components.OSEmptyState
import com.os11.app.ui.components.OSIconButton
import com.os11.app.ui.theme.OS11Theme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationCenterScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    onTaskClick: (String) -> Unit = {},
    viewModel: NotificationCenterViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val colors = OS11Theme.colors
    val spacing = OS11Theme.spacing
    val type = OS11Theme.type

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.surfaceBase),
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.s4, vertical = spacing.s2),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OSIconButton(
                    onClick = onNavigateBack,
                    contentDescription = "Back",
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = null,
                        tint = colors.textPrimary,
                    )
                }

                Spacer(modifier = Modifier.width(spacing.s2))

                Text(
                    text = "Notifications",
                    style = type.viewTitle.copy(color = colors.textPrimary),
                )

                if (state.unreadCount > 0) {
                    Spacer(modifier = Modifier.width(spacing.s2))
                    OSChip(
                        label = "${state.unreadCount} new",
                        selected = true,
                        onClick = {},
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(spacing.s1)) {
                if (state.unreadCount > 0) {
                    OSIconButton(
                        onClick = viewModel::markAllAsRead,
                        contentDescription = "Mark all as read",
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            tint = colors.accent,
                        )
                    }
                }

                if (state.notifications.isNotEmpty()) {
                    OSIconButton(
                        onClick = viewModel::clearAll,
                        contentDescription = "Clear all notifications",
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = null,
                            tint = colors.textSecondary,
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(spacing.s2))

        // Notifications List or Empty State
        if (state.notifications.isEmpty() && !state.isLoading) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                OSEmptyState(
                    title = "No notifications",
                    description = "You're all caught up! Reminders and alerts will appear here.",
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = spacing.s4),
                verticalArrangement = Arrangement.spacedBy(spacing.s2),
            ) {
                items(
                    items = state.notifications,
                    key = { it.id },
                ) { item ->
                    NotificationCard(
                        item = item,
                        onClick = {
                            if (!item.isRead) {
                                viewModel.markAsRead(item.id)
                            }
                            item.taskId?.let { onTaskClick(it) }
                        },
                        onDismiss = { viewModel.deleteNotification(item.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationCard(
    item: NotificationItem,
    onClick: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = OS11Theme.colors
    val spacing = OS11Theme.spacing
    val radius = OS11Theme.radius
    val type = OS11Theme.type

    val shape = RoundedCornerShape(radius.md)
    val bgColor = if (item.isRead) colors.surfaceRaised else colors.surfaceSelected
    val borderColor = if (item.isRead) colors.borderDefault else colors.accent.copy(alpha = 0.40f)

    val icon = resolveNotificationIcon(item.type)
    val iconTint = if (item.isRead) colors.textSecondary else colors.accent

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(bgColor)
            .border(spacing.px, borderColor, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(spacing.s3)
            .semantics {
                contentDescription = "${item.title}: ${item.body}"
            },
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(colors.surfaceBase),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(18.dp),
            )
        }

        Spacer(modifier = Modifier.width(spacing.s3))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = item.title,
                    style = type.body.copy(
                        color = colors.textPrimary,
                    ),
                    maxLines = 1,
                )

                if (!item.isRead) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(colors.accent),
                    )
                }
            }

            Spacer(modifier = Modifier.height(spacing.s1))

            Text(
                text = item.body,
                style = type.meta.copy(color = colors.textSecondary),
            )

            Spacer(modifier = Modifier.height(spacing.s1))

            Text(
                text = item.createdAt.take(16).replace("T", " "),
                style = type.meta.copy(color = colors.textTertiary),
            )
        }

        Spacer(modifier = Modifier.width(spacing.s2))

        OSIconButton(
            onClick = onDismiss,
            contentDescription = "Dismiss notification",
        ) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = null,
                tint = colors.textTertiary,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

private fun resolveNotificationIcon(type: String): ImageVector = when (type.lowercase()) {
    "due" -> Icons.Outlined.DateRange
    "reminder" -> Icons.Outlined.DateRange
    "pomodoro" -> Icons.Outlined.Notifications
    "collaboration", "follow_up" -> Icons.Outlined.Person
    "system" -> Icons.Outlined.Info
    else -> Icons.Outlined.Notifications
}
