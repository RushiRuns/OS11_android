package com.os11.app.ui.features.project

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
import com.os11.app.ui.components.OSProgressBar
import com.os11.app.ui.components.OSTextField
import com.os11.app.ui.features.task.TaskActionsSheet
import com.os11.app.ui.features.task.TaskDetailSheet
import com.os11.app.ui.features.task.TaskRow
import com.os11.app.ui.features.task.TaskUiModel
import com.os11.app.ui.theme.OS11Theme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProjectViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val colors = OS11Theme.colors
    val spacing = OS11Theme.spacing
    val type = OS11Theme.type

    var quickAddText by remember { mutableStateOf("") }
    var selectedTaskForDetail by remember { mutableStateOf<TaskUiModel?>(null) }
    var selectedTaskForActions by remember { mutableStateOf<TaskUiModel?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.surfaceBase),
    ) {
        // 1. Header: Back button + Project title + Status badge
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.s4, vertical = spacing.s2),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.s2),
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
                Text(
                    text = state.project?.name ?: "Project",
                    style = type.subViewTitle.copy(color = colors.textPrimary),
                )
            }
            state.project?.let { proj ->
                OSChip(
                    label = proj.status.replaceFirstChar { it.uppercase() },
                    selected = false,
                    onClick = { },
                )
            }
        }

        // Progress indicator
        if (state.totalCount > 0) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.s4, vertical = spacing.s1),
            ) {
                Text(
                    text = "${state.completedCount} of ${state.totalCount} tasks completed",
                    style = type.meta.copy(color = colors.textSecondary),
                )
                Spacer(modifier = Modifier.height(spacing.s1))
                val progress = state.completedCount.toFloat() / state.totalCount.toFloat()
                OSProgressBar(
                    progress = progress,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        // 2. Inline quick add input
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.s4, vertical = spacing.s2),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OSTextField(
                value = quickAddText,
                onValueChange = { quickAddText = it },
                modifier = Modifier.weight(1f),
                placeholder = "Add task in this project...",
            )
            OSIconButton(
                onClick = {
                    if (quickAddText.isNotBlank()) {
                        viewModel.addTaskToProject(quickAddText.trim())
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

        // 3. Project Tasks List
        if (state.tasks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(spacing.s4),
                contentAlignment = Alignment.Center,
            ) {
                OSEmptyState(
                    title = "No tasks yet",
                    description = "Add tasks to plan and track deliverables for this project.",
                )
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(
                    items = state.tasks,
                    key = { it.id },
                    contentType = { "task_row" },
                ) { task ->
                    TaskRow(
                        task = task,
                        onCompletedChange = { viewModel.setCompleted(task.id, it) },
                        onClick = { selectedTaskForDetail = task },
                        onLongClick = { selectedTaskForActions = task },
                        onStarClick = { viewModel.toggleStar(task.id) },
                    )
                }
            }
        }
    }

    // Detail Sheet
    selectedTaskForDetail?.let { task ->
        TaskDetailSheet(
            title = task.title,
            notes = null,
            dueDate = task.dueDate,
            dueTime = task.dueTime,
            priority = task.priority,
            isCompleted = task.isCompleted,
            subtasks = emptyList(),
            onTitleChange = { newTitle ->
                viewModel.editTask(task.id, newTitle, null, task.dueDate, task.priority)
            },
            onNotesChange = { newNotes ->
                viewModel.editTask(task.id, task.title, newNotes, task.dueDate, task.priority)
            },
            onDueDateChange = { newDueDate ->
                viewModel.editTask(task.id, task.title, null, newDueDate, task.priority)
            },
            onPriorityChange = { newPriority ->
                viewModel.editTask(task.id, task.title, null, task.dueDate, newPriority)
            },
            onCompletedChange = { completed ->
                viewModel.setCompleted(task.id, completed)
            },
            onAddSubtask = { subtaskTitle ->
                viewModel.addSubtask(task.id, subtaskTitle)
            },
            onSubtaskCompletedChange = { subtaskId, subtaskCompleted ->
                viewModel.setCompleted(subtaskId, subtaskCompleted)
            },
            onDismissRequest = { selectedTaskForDetail = null },
        )
    }

    // Actions Sheet
    selectedTaskForActions?.let { task ->
        TaskActionsSheet(
            task = task,
            onDismissRequest = { selectedTaskForActions = null },
            onToggleCompleted = { viewModel.setCompleted(task.id, !task.isCompleted) },
            onToggleStarred = { viewModel.toggleStar(task.id) },
            onEdit = {
                selectedTaskForActions = null
                selectedTaskForDetail = task
            },
            onChangeDate = {
                selectedTaskForActions = null
                selectedTaskForDetail = task
            },
            onAddSubtask = {
                selectedTaskForActions = null
                selectedTaskForDetail = task
            },
            onDuplicate = {
                selectedTaskForActions = null
            },
            onIndent = { },
            onOutdent = { },
            onPromote = { },
            onTrash = {
                selectedTaskForActions = null
                viewModel.trashTask(task.id)
            },
        )
    }
}
