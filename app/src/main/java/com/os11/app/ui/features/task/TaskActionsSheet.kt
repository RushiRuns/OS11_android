package com.os11.app.ui.features.task

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.os11.app.ui.components.OSSheet
import com.os11.app.ui.theme.OS11Theme

/**
 * Task row long-press actions bottom sheet with full action set.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskActionsSheet(
    task: TaskUiModel,
    onDismissRequest: () -> Unit,
    onToggleCompleted: () -> Unit,
    onToggleStarred: () -> Unit,
    onEdit: () -> Unit,
    onChangeDate: () -> Unit,
    onAddSubtask: () -> Unit,
    onDuplicate: () -> Unit,
    onIndent: () -> Unit,
    onOutdent: () -> Unit,
    onPromote: () -> Unit,
    onTrash: () -> Unit,
    modifier: Modifier = Modifier,
    onSelect: (() -> Unit)? = null,
) {
    val colors = OS11Theme.colors
    val spacing = OS11Theme.spacing
    val type = OS11Theme.type

    OSSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.s4),
        ) {
            // Task title header
            Text(
                text = task.title,
                style = type.subViewTitle.copy(color = colors.textPrimary),
                modifier = Modifier.padding(bottom = spacing.s3),
            )

            ActionItem(
                icon = Icons.Outlined.CheckCircle,
                title = if (task.isCompleted) "Mark incomplete" else "Mark complete",
                onClick = {
                    onDismissRequest()
                    onToggleCompleted()
                },
            )

            if (onSelect != null) {
                ActionItem(
                    icon = Icons.Outlined.CheckCircle,
                    title = "Select task (multi-select)",
                    onClick = {
                        onDismissRequest()
                        onSelect()
                    },
                )
            }

            ActionItem(
                icon = if (task.isStarred) Icons.Filled.Star else Icons.Outlined.Star,
                title = if (task.isStarred) "Unstar" else "Star",
                onClick = {
                    onDismissRequest()
                    onToggleStarred()
                },
            )

            ActionItem(
                icon = Icons.Outlined.Edit,
                title = "Edit details",
                onClick = {
                    onDismissRequest()
                    onEdit()
                },
            )

            ActionItem(
                icon = Icons.Outlined.DateRange,
                title = "Change date (When)",
                onClick = {
                    onDismissRequest()
                    onChangeDate()
                },
            )

            ActionItem(
                icon = Icons.AutoMirrored.Outlined.List,
                title = "Add subtask",
                onClick = {
                    onDismissRequest()
                    onAddSubtask()
                },
            )

            ActionItem(
                icon = Icons.Outlined.FavoriteBorder,
                title = "Duplicate task",
                onClick = {
                    onDismissRequest()
                    onDuplicate()
                },
            )

            if (task.parentTaskId != null) {
                ActionItem(
                    icon = Icons.AutoMirrored.Outlined.ArrowBack,
                    title = "Outdent subtask",
                    onClick = {
                        onDismissRequest()
                        onOutdent()
                    },
                )

                ActionItem(
                    icon = Icons.Default.Home,
                    title = "Promote to top level",
                    onClick = {
                        onDismissRequest()
                        onPromote()
                    },
                )
            } else {
                ActionItem(
                    icon = Icons.AutoMirrored.Outlined.ArrowForward,
                    title = "Indent subtask",
                    onClick = {
                        onDismissRequest()
                        onIndent()
                    },
                )
            }

            Spacer(modifier = Modifier.height(spacing.s2))

            ActionItem(
                icon = Icons.Outlined.Delete,
                title = "Move to trash",
                tint = colors.priorityCritical,
                onClick = {
                    onDismissRequest()
                    onTrash()
                },
            )

            Spacer(modifier = Modifier.height(spacing.s4))
        }
    }
}

@Composable
private fun ActionItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    tint: androidx.compose.ui.graphics.Color = OS11Theme.colors.textPrimary,
) {
    val spacing = OS11Theme.spacing
    val type = OS11Theme.type

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = spacing.s3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.width(spacing.s3))
        Text(
            text = title,
            style = type.body.copy(color = tint),
        )
    }
}
