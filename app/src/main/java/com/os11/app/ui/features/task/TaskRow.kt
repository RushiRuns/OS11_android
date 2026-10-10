package com.os11.app.ui.features.task

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.os11.app.ui.components.OSCheckbox
import com.os11.app.ui.components.OSIconButton
import com.os11.app.ui.components.OSPriorityBar
import com.os11.app.ui.components.OSTagDot
import com.os11.app.ui.theme.OS11Theme

/**
 * Single task row component satisfying Phase 5 specifications:
 * - Density-aware height: 44dp (Compact), 52dp (Comfortable), 60dp (Cozy)
 * - Checkbox (20dp visual, 48dp target, spring, strikethrough, haptic)
 * - Left priority bar
 * - Second line rendered ONLY when there is something to show
 * - Tag dots and project/area metadata
 * - Tree indent padding for subtasks
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TaskRow(
    task: TaskUiModel,
    onCompletedChange: (Boolean) -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onStarClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = OS11Theme.colors
    val spacing = OS11Theme.spacing
    val type = OS11Theme.type
    val density = OS11Theme.density
    val rowMinHeight = OS11Theme.layout.taskHeight(density)

    val indentPadding = (task.indentDepth * 16).dp

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = rowMinHeight)
            .background(colors.surfaceBase)
            .combinedClickable(
                role = Role.Button,
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .padding(start = indentPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 1. Left priority bar indicator
        OSPriorityBar(
            priority = task.priority.toOSPriority(),
            modifier = Modifier
                .width(OS11Theme.layout.priorityBorderWidth)
                .fillMaxHeight(),
        )

        Spacer(modifier = Modifier.width(spacing.s2))

        // 2. Checkbox with 48dp touch target
        OSCheckbox(
            checked = task.isCompleted,
            onCheckedChange = onCompletedChange,
            modifier = Modifier.padding(horizontal = spacing.s1),
        )

        Spacer(modifier = Modifier.width(spacing.s2))

        // 3. Main content (Title + optional Second line)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = spacing.s2),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = task.title,
                style = type.body.copy(
                    color = if (task.isCompleted) colors.textTertiary else colors.textPrimary,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            // Second line: Rendered ONLY when there is something to show
            if (task.hasSecondLine) {
                Spacer(modifier = Modifier.height(2.dp))
                TaskMetadataRow(task = task)
            }
        }

        // 4. Star toggle action
        OSIconButton(
            onClick = onStarClick,
            contentDescription = if (task.isStarred) "Unstar task" else "Star task",
            modifier = Modifier.padding(end = spacing.s2),
        ) {
            Icon(
                imageVector = if (task.isStarred) Icons.Filled.Star else Icons.Outlined.Star,
                contentDescription = null,
                tint = if (task.isStarred) colors.priorityMedium else colors.textTertiary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun TaskMetadataRow(task: TaskUiModel) {
    val colors = OS11Theme.colors
    val spacing = OS11Theme.spacing
    val type = OS11Theme.type

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.s2),
    ) {
        // Due date chip / text
        if (task.formattedDueDate != null) {
            val dateColor = if (task.isOverdue) colors.priorityCritical else colors.textSecondary
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.DateRange,
                    contentDescription = null,
                    tint = dateColor,
                    modifier = Modifier.size(12.dp),
                )
                Text(
                    text = task.formattedDueDate,
                    style = type.meta.copy(color = dateColor),
                )
            }
        }

        // Project / Area label
        val containerLabel = task.projectName ?: task.areaName
        if (containerLabel != null) {
            Text(
                text = containerLabel,
                style = type.meta.copy(color = colors.textSecondary),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        // Subtasks progress "2/5"
        if (task.subtaskProgress != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.List,
                    contentDescription = null,
                    tint = colors.textSecondary,
                    modifier = Modifier.size(12.dp),
                )
                Text(
                    text = task.subtaskProgress,
                    style = type.meta.copy(color = colors.textSecondary),
                )
            }
        }

        // Notes indicator
        if (task.hasNotes) {
            Icon(
                imageVector = Icons.Outlined.Edit,
                contentDescription = "Has notes",
                tint = colors.textTertiary,
                modifier = Modifier.size(12.dp),
            )
        }

        // Tag dots
        if (task.tagDots.isNotEmpty()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                task.tagDots.take(3).forEach { _ ->
                    OSTagDot(
                        color = colors.accent,
                        variant = com.os11.app.ui.components.OSTagDotVariant.PillDot,
                    )
                }
            }
        }
    }
}

private fun Int.toOSPriority(): com.os11.app.ui.components.OSPriority = when (this) {
    1 -> com.os11.app.ui.components.OSPriority.Low
    2 -> com.os11.app.ui.components.OSPriority.Medium
    3 -> com.os11.app.ui.components.OSPriority.High
    4 -> com.os11.app.ui.components.OSPriority.Critical
    else -> com.os11.app.ui.components.OSPriority.None
}
