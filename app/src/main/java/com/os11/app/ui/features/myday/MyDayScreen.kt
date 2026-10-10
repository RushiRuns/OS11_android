package com.os11.app.ui.features.myday

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Star
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.os11.app.ui.components.OSButton
import com.os11.app.ui.components.OSButtonVariant
import com.os11.app.ui.components.OSEmptyState
import com.os11.app.ui.components.OSIconButton
import com.os11.app.ui.components.OSProgressBar
import com.os11.app.ui.components.OSSheet
import com.os11.app.ui.components.OSTextField
import com.os11.app.ui.features.task.TaskActionsSheet
import com.os11.app.ui.features.task.TaskDetailSheet
import com.os11.app.ui.features.task.TaskRow
import com.os11.app.ui.features.task.TaskUiModel
import com.os11.app.ui.theme.OS11Theme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyDayScreen(modifier: Modifier = Modifier, viewModel: MyDayViewModel = hiltViewModel()) {
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
        // 1. Header: Date and Completion Summary
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.s4, vertical = spacing.s2),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = state.logicalToday.ifEmpty { "Today" },
                    style = type.subViewTitle.copy(color = colors.textPrimary),
                )
                if (state.totalCount > 0) {
                    Text(
                        text = "${state.completedCount} of ${state.totalCount} completed",
                        style = type.meta.copy(color = colors.textSecondary),
                    )
                }
            }

            if (state.totalCount > 0) {
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
                placeholder = "Add task to My Day...",
            )
            OSIconButton(
                onClick = {
                    if (quickAddText.isNotBlank()) {
                        viewModel.addTaskToMyDay(quickAddText.trim())
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

        // 3. Suggestions Section
        if (state.suggestions.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(role = Role.Button) { viewModel.toggleSuggestionsExpanded() }
                    .padding(horizontal = spacing.s4, vertical = spacing.s2),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.s2),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Star,
                        contentDescription = null,
                        tint = colors.priorityMedium,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = "Suggestions (${state.suggestions.size})",
                        style = type.sectionHeader.copy(color = colors.textPrimary),
                    )
                }
                Icon(
                    imageVector = if (state.isSuggestionsExpanded) {
                        Icons.Outlined.KeyboardArrowDown
                    } else {
                        Icons.AutoMirrored.Outlined.KeyboardArrowRight
                    },
                    contentDescription = if (state.isSuggestionsExpanded) "Collapse" else "Expand",
                    tint = colors.textTertiary,
                    modifier = Modifier.size(20.dp),
                )
            }

            if (state.isSuggestionsExpanded) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    state.suggestions.take(5).forEach { suggestion ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = spacing.s4, vertical = spacing.s1),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = suggestion.title,
                                style = type.body.copy(color = colors.textPrimary),
                                modifier = Modifier.weight(1f),
                            )
                            OSIconButton(
                                onClick = { viewModel.addSuggestionToMyDay(suggestion.id) },
                                contentDescription = "Add to My Day",
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = colors.accent,
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. Main Tasks List
        if (state.tasks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(spacing.s4),
                contentAlignment = Alignment.Center,
            ) {
                OSEmptyState(
                    title = "Focus on today",
                    description = "Tasks you plan for today will appear here. Add from suggestions or type above.",
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

    // Midnight Rollover Modal Sheet
    if (state.showRolloverSheet) {
        OSSheet(onDismissRequest = viewModel::dismissRollover) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(spacing.s4),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Previous My Day Tasks",
                    style = type.subViewTitle.copy(color = colors.textPrimary),
                )
                Spacer(modifier = Modifier.height(spacing.s2))
                Text(
                    text = "You have ${state.uncompletedYesterdayTasks.size} incomplete tasks " +
                        "from your previous My Day. What would you like to do?",
                    style = type.body.copy(color = colors.textSecondary),
                )
                Spacer(modifier = Modifier.height(spacing.s4))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.s2),
                ) {
                    OSButton(
                        onClick = { viewModel.handleRollover(keepForToday = true) },
                        variant = OSButtonVariant.Primary,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Keep for Today")
                    }
                    OSButton(
                        onClick = { viewModel.handleRollover(keepForToday = false) },
                        variant = OSButtonVariant.Secondary,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Backlog")
                    }
                }
                Spacer(modifier = Modifier.height(spacing.s2))
                OSButton(
                    onClick = viewModel::dismissRollover,
                    variant = OSButtonVariant.Ghost,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Dismiss")
                }
            }
        }
    }
}
