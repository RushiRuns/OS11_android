package com.os11.app.ui.features.task

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.os11.app.ui.components.OSCheckbox
import com.os11.app.ui.components.OSIconButton
import com.os11.app.ui.components.OSPriorityBar
import com.os11.app.ui.components.OSTagDot
import com.os11.app.ui.theme.OS11Theme

/**
 * Single task row component satisfying Phase 8 specifications:
 * - Density-aware height: 44dp (Compact), 52dp (Comfortable), 60dp (Cozy)
 * - Swipe right: Complete; Swipe left: When sheet; visible alternative for each
 * - TalkBack custom actions for swipe actions
 * - Multi-select mode support with left selection checkbox
 * - Drag handle support for reordering
 * - Checkbox (20dp visual, 48dp target, spring, strikethrough, haptic)
 * - Left priority bar
 * - Second line rendered ONLY when there is something to show
 * - Tag dots and project/area metadata
 * - Tree indent padding for subtasks
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TaskRow(
    task: TaskUiModel,
    onCompletedChange: (Boolean) -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onStarClick: () -> Unit,
    modifier: Modifier = Modifier,
    onWhenClick: (() -> Unit)? = null,
    enableSwipe: Boolean = true,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onSelectionChange: ((Boolean) -> Unit)? = null,
    dragHandleModifier: Modifier = Modifier,
) {
    val colors = OS11Theme.colors
    val spacing = OS11Theme.spacing
    val type = OS11Theme.type

    // If swipe is enabled and not in selection mode, wrap in SwipeToDismissBox
    if (enableSwipe && !isSelectionMode) {
        val dismissState = rememberSwipeToDismissBoxState(
            confirmValueChange = { dismissValue ->
                when (dismissValue) {
                    SwipeToDismissBoxValue.StartToEnd -> {
                        // Swipe right -> Complete
                        onCompletedChange(!task.isCompleted)
                        false // spring back so item remains or updates cleanly
                    }
                    SwipeToDismissBoxValue.EndToStart -> {
                        // Swipe left -> When date picker
                        onWhenClick?.invoke()
                        false // spring back
                    }
                    SwipeToDismissBoxValue.Settled -> false
                }
            },
        )

        SwipeToDismissBox(
            state = dismissState,
            enableDismissFromStartToEnd = true,
            enableDismissFromEndToStart = onWhenClick != null,
            backgroundContent = {
                val direction = dismissState.dismissDirection
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(colors.surfaceSidebar)
                        .padding(horizontal = spacing.s4),
                    contentAlignment = if (direction == SwipeToDismissBoxValue.StartToEnd) {
                        Alignment.CenterStart
                    } else {
                        Alignment.CenterEnd
                    },
                ) {
                    if (direction == SwipeToDismissBoxValue.StartToEnd) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(spacing.s2),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CheckCircle,
                                contentDescription = null,
                                tint = colors.accent,
                                modifier = Modifier.size(20.dp),
                            )
                            Text(
                                text = if (task.isCompleted) "Uncomplete" else "Complete",
                                style = type.meta.copy(color = colors.accent),
                            )
                        }
                    } else if (direction == SwipeToDismissBoxValue.EndToStart) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(spacing.s2),
                        ) {
                            Text(
                                text = "When",
                                style = type.meta.copy(color = colors.accent),
                            )
                            Icon(
                                imageVector = Icons.Outlined.DateRange,
                                contentDescription = null,
                                tint = colors.accent,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            },
            modifier = modifier,
        ) {
            TaskRowContent(
                task = task,
                onCompletedChange = onCompletedChange,
                onClick = onClick,
                onLongClick = onLongClick,
                onStarClick = onStarClick,
                onWhenClick = onWhenClick,
                isSelectionMode = false,
                isSelected = false,
                onSelectionChange = null,
                dragHandleModifier = dragHandleModifier,
            )
        }
    } else {
        TaskRowContent(
            task = task,
            onCompletedChange = onCompletedChange,
            onClick = {
                if (isSelectionMode) {
                    onSelectionChange?.invoke(!isSelected)
                } else {
                    onClick()
                }
            },
            onLongClick = onLongClick,
            onStarClick = onStarClick,
            onWhenClick = onWhenClick,
            isSelectionMode = isSelectionMode,
            isSelected = isSelected,
            onSelectionChange = onSelectionChange,
            dragHandleModifier = dragHandleModifier,
            modifier = modifier,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TaskRowContent(
    task: TaskUiModel,
    onCompletedChange: (Boolean) -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onStarClick: () -> Unit,
    onWhenClick: (() -> Unit)?,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onSelectionChange: ((Boolean) -> Unit)?,
    dragHandleModifier: Modifier,
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
            .background(if (isSelected) colors.surfaceSelected else colors.surfaceBase)
            .combinedClickable(
                role = Role.Button,
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .padding(start = indentPadding)
            .semantics {
                // TalkBack Custom Accessibility Actions for every swipe gesture
                customActions = listOfNotNull(
                    CustomAccessibilityAction(
                        label = if (task.isCompleted) "Mark task incomplete" else "Mark task complete",
                    ) {
                        onCompletedChange(!task.isCompleted)
                        true
                    },
                    if (onWhenClick != null) {
                        CustomAccessibilityAction(label = "Set date (When)") {
                            onWhenClick()
                            true
                        }
                    } else {
                        null
                    },
                    CustomAccessibilityAction(label = "More actions") {
                        onLongClick()
                        true
                    },
                )
            },
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

        // 2. Selection Checkbox OR Task Completion Checkbox
        if (isSelectionMode) {
            OSCheckbox(
                checked = isSelected,
                onCheckedChange = { onSelectionChange?.invoke(it) },
                modifier = Modifier.padding(horizontal = spacing.s1),
            )
        } else {
            OSCheckbox(
                checked = task.isCompleted,
                onCheckedChange = onCompletedChange,
                modifier = Modifier.padding(horizontal = spacing.s1),
            )
        }

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

        // 4. Star toggle action (hidden in selection mode)
        if (!isSelectionMode) {
            OSIconButton(
                onClick = onStarClick,
                contentDescription = if (task.isStarred) "Unstar task" else "Star task",
                modifier = Modifier.padding(end = spacing.s1),
            ) {
                Icon(
                    imageVector = if (task.isStarred) Icons.Filled.Star else Icons.Outlined.Star,
                    contentDescription = null,
                    tint = if (task.isStarred) colors.priorityMedium else colors.textTertiary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        // 5. Optional Reorder Drag Handle
        if (dragHandleModifier != Modifier) {
            Box(
                modifier = dragHandleModifier
                    .size(40.dp)
                    .padding(end = spacing.s2),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Menu,
                    contentDescription = "Reorder task",
                    tint = colors.textTertiary,
                    modifier = Modifier.size(18.dp),
                )
            }
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
                horizontalArrangement = Arrangement.spacedBy(spacing.s1),
            ) {
                task.tagDots.take(3).forEach { tagDot ->
                    val tagColor = colors.tags.fromNameOrHex(tagDot.colorHex)
                    OSTagDot(
                        color = tagColor,
                        shape = tagDot.shape,
                        variant = com.os11.app.ui.components.OSTagDotVariant.PillDot,
                        contentDescriptionText = "Tag ${tagDot.name} (${tagDot.shape.label})",
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
