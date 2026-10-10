package com.os11.app.ui.features.tags

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.os11.app.ui.components.OSChip
import com.os11.app.ui.components.OSEmptyState
import com.os11.app.ui.components.OSIconButton
import com.os11.app.ui.components.OSTagDot
import com.os11.app.ui.components.OSTagDotVariant
import com.os11.app.ui.components.OSTagShape
import com.os11.app.ui.components.OSTextField
import com.os11.app.ui.features.task.TaskActionsSheet
import com.os11.app.ui.features.task.TaskDetailSheet
import com.os11.app.ui.features.task.TaskRow
import com.os11.app.ui.features.task.TaskUiModel
import com.os11.app.ui.theme.OS11Theme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagDetailScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TagDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val colors = OS11Theme.colors
    val spacing = OS11Theme.spacing
    val type = OS11Theme.type

    var quickAddText by remember { mutableStateOf("") }
    var selectedTaskForDetail by remember { mutableStateOf<TaskUiModel?>(null) }
    var selectedTaskForActions by remember { mutableStateOf<TaskUiModel?>(null) }

    val currentTag = state.tag
    val tagColor = colors.tags.fromNameOrHex(currentTag?.color)
    val tagShape = if (currentTag != null) {
        OSTagShape.fromTag(currentTag.name, currentTag.color)
    } else {
        OSTagShape.Circle
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.surfaceBase),
    ) {
        // 1. Top Header Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.s4, vertical = spacing.s2),
            verticalAlignment = Alignment.CenterVertically,
        ) {
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

            OSTagDot(
                color = tagColor,
                shape = tagShape,
                variant = OSTagDotVariant.TaskRow,
            )

            Spacer(modifier = Modifier.width(spacing.s2))

            Text(
                text = "#${currentTag?.name ?: ""}",
                style = type.viewTitle.copy(color = colors.textPrimary),
                modifier = Modifier.weight(1f),
            )

            OSChip(
                label = "${state.completedCount}/${state.totalCount}",
                selected = false,
                onClick = {},
            )
        }

        Spacer(modifier = Modifier.height(spacing.s2))

        // 2. Inline Quick Add
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.s4),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.s2),
        ) {
            OSTextField(
                value = quickAddText,
                onValueChange = { quickAddText = it },
                placeholder = "Add a task with this tag...",
                modifier = Modifier.weight(1f),
            )
            OSIconButton(
                onClick = {
                    if (quickAddText.isNotBlank()) {
                        viewModel.addTask(quickAddText)
                        quickAddText = ""
                    }
                },
                contentDescription = "Add task",
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = colors.accent,
                )
            }
        }

        Spacer(modifier = Modifier.height(spacing.s3))

        // 3. Task List or Empty State
        if (state.tasks.isEmpty() && !state.isLoading) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                OSEmptyState(
                    title = "No tasks for #${currentTag?.displayName ?: "tag"}",
                    description = "Tasks with this tag will appear here.",
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = spacing.s4),
                verticalArrangement = Arrangement.spacedBy(spacing.s1),
            ) {
                items(
                    items = state.tasks,
                    key = { it.id },
                ) { task ->
                    TaskRow(
                        task = task,
                        onCompletedChange = { isChecked ->
                            viewModel.setTaskCompleted(task.id, isChecked)
                        },
                        onClick = { selectedTaskForDetail = task },
                        onLongClick = { selectedTaskForActions = task },
                        onStarClick = {},
                    )
                }
            }
        }
    }

    // Task Actions Bottom Sheet
    selectedTaskForActions?.let { task ->
        TaskActionsSheet(
            task = task,
            onDismissRequest = { selectedTaskForActions = null },
            onToggleCompleted = {
                viewModel.setTaskCompleted(task.id, !task.isCompleted)
                selectedTaskForActions = null
            },
            onToggleStarred = { selectedTaskForActions = null },
            onEdit = {
                selectedTaskForDetail = task
                selectedTaskForActions = null
            },
            onChangeDate = { selectedTaskForActions = null },
            onAddSubtask = { selectedTaskForActions = null },
            onDuplicate = { selectedTaskForActions = null },
            onIndent = { selectedTaskForActions = null },
            onOutdent = { selectedTaskForActions = null },
            onPromote = { selectedTaskForActions = null },
            onTrash = {
                viewModel.removeTagFromTask(task.id)
                selectedTaskForActions = null
            },
        )
    }

    // Task Detail Bottom Sheet
    selectedTaskForDetail?.let { task ->
        TaskDetailSheet(
            title = task.title,
            notes = null,
            dueDate = task.dueDate,
            dueTime = task.dueTime,
            priority = task.priority,
            isCompleted = task.isCompleted,
            subtasks = emptyList(),
            onTitleChange = {},
            onNotesChange = {},
            onDueDateChange = {},
            onPriorityChange = {},
            onCompletedChange = { isChecked ->
                viewModel.setTaskCompleted(task.id, isChecked)
            },
            onAddSubtask = {},
            onSubtaskCompletedChange = { _, _ -> },
            onDismissRequest = { selectedTaskForDetail = null },
        )
    }
}
