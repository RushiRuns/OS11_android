package com.os11.app.ui.features.area

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.os11.app.service.project.ProjectItem
import com.os11.app.ui.components.OSEmptyState
import com.os11.app.ui.components.OSIconButton
import com.os11.app.ui.components.OSTextField
import com.os11.app.ui.features.task.TaskActionsSheet
import com.os11.app.ui.features.task.TaskDetailSheet
import com.os11.app.ui.features.task.TaskRow
import com.os11.app.ui.features.task.TaskUiModel
import com.os11.app.ui.theme.OS11Theme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AreaOverviewScreen(
    onNavigateBack: () -> Unit,
    onProjectClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AreaOverviewViewModel = hiltViewModel(),
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
        // 1. Header with back button and Area name
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
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(colors.accent),
                )
                Text(
                    text = state.area?.name ?: "Area Overview",
                    style = type.subViewTitle.copy(color = colors.textPrimary),
                )
            }
            Text(
                text = "${state.totalTaskCount} tasks",
                style = type.meta.copy(color = colors.textSecondary),
            )
        }

        // 2. Project Cards Carousel / Row
        if (state.projects.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = spacing.s2),
            ) {
                Text(
                    text = "Projects (${state.projects.size})",
                    style = type.sectionHeader.copy(color = colors.textPrimary),
                    modifier = Modifier.padding(horizontal = spacing.s4),
                )
                Spacer(modifier = Modifier.height(spacing.s2))
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.s2),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = spacing.s4),
                ) {
                    items(
                        items = state.projects,
                        key = { it.id },
                    ) { project ->
                        ProjectOverviewCard(
                            project = project,
                            onClick = { onProjectClick(project.id) },
                        )
                    }
                }
            }
        }

        // 3. Loose Tasks Header & Inline Add
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
                placeholder = "Add loose task in this area...",
            )
            OSIconButton(
                onClick = {
                    if (quickAddText.isNotBlank()) {
                        viewModel.addLooseTask(quickAddText.trim())
                        quickAddText = ""
                    }
                },
                contentDescription = "Add loose task",
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = colors.accent,
                )
            }
        }

        // 4. Loose Tasks List
        if (state.looseTasks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(spacing.s4),
                contentAlignment = Alignment.Center,
            ) {
                OSEmptyState(
                    title = "No loose tasks",
                    description = "Tasks not associated with a project in this area will appear here.",
                )
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(
                    items = state.looseTasks,
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

@Composable
private fun ProjectOverviewCard(project: ProjectItem, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = OS11Theme.colors
    val spacing = OS11Theme.spacing
    val type = OS11Theme.type
    val radius = OS11Theme.radius

    Column(
        modifier = modifier
            .size(width = 140.dp, height = 80.dp)
            .clip(RoundedCornerShape(radius.md))
            .background(colors.surfaceRaised)
            .border(spacing.px, colors.borderSubtle, RoundedCornerShape(radius.md))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(spacing.s3),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = project.name,
            style = type.body.copy(color = colors.textPrimary),
            maxLines = 1,
        )
        Text(
            text = project.status.replaceFirstChar { it.uppercase() },
            style = type.meta.copy(color = colors.textSecondary),
        )
    }
}
