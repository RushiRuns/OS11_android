package com.os11.app.ui.features.planned

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import com.os11.app.ui.components.OSEmptyState
import com.os11.app.ui.components.OSIconButton
import com.os11.app.ui.components.OSTextField
import com.os11.app.ui.components.calendar.WeekStrip
import com.os11.app.ui.features.task.TaskActionsSheet
import com.os11.app.ui.features.task.TaskDetailSheet
import com.os11.app.ui.features.task.TaskRow
import com.os11.app.ui.features.task.TaskUiModel
import com.os11.app.ui.theme.OS11Theme
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlannedScreen(modifier: Modifier = Modifier, viewModel: PlannedViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    val colors = OS11Theme.colors
    val spacing = OS11Theme.spacing

    var selectedTaskForDetail by remember { mutableStateOf<TaskUiModel?>(null) }
    var selectedTaskForActions by remember { mutableStateOf<TaskUiModel?>(null) }
    var quickAddText by remember { mutableStateOf("") }

    val activeTargetDate = (state.selectedDate ?: LocalDate.now()).toString()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.surfaceBase),
    ) {
        // 1. WeekStrip calendar component
        WeekStrip(
            referenceDate = state.referenceDate,
            selectedDate = state.selectedDate,
            datesWithTasks = state.datesWithTasks,
            onDateSelected = viewModel::onDateSelected,
            onWeekOffsetChange = viewModel::onWeekOffsetChange,
        )

        // 2. Inline quick add input with active target date
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
                placeholder = "Add task for $activeTargetDate...",
            )
            OSIconButton(
                onClick = {
                    if (quickAddText.isNotBlank()) {
                        viewModel.addTaskWithDate(quickAddText.trim(), activeTargetDate)
                        quickAddText = ""
                    }
                },
                contentDescription = "Add planned task",
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = colors.accent,
                )
            }
        }

        // 3. Tasks list
        val hasAnyTasks = if (state.selectedDate != null) {
            state.filteredTasks.isNotEmpty()
        } else {
            state.overdueTasks.isNotEmpty() ||
                state.todayTasks.isNotEmpty() ||
                state.tomorrowTasks.isNotEmpty() ||
                state.upcomingTasks.isNotEmpty()
        }

        if (!hasAnyTasks) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(spacing.s4),
                contentAlignment = Alignment.Center,
            ) {
                OSEmptyState(
                    title = if (state.selectedDate != null) "No tasks for this day" else "Nothing scheduled",
                    description = "Upcoming tasks with due dates will appear organized by date.",
                )
            }
        } else {
            PlannedTasksContent(
                state = state,
                onCompletedChange = { id, comp -> viewModel.setCompleted(id, comp) },
                onSelectTask = { selectedTaskForDetail = it },
                onSelectTaskForActions = { selectedTaskForActions = it },
                onToggleStar = { viewModel.toggleStar(it) },
            )
        }
    }

    // Detail Bottom Sheet
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

    // Actions Bottom Sheet
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
private fun PlannedSectionHeader(title: String, count: Int, headerColor: Color, modifier: Modifier = Modifier) {
    val spacing = OS11Theme.spacing
    val type = OS11Theme.type

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.s4, vertical = spacing.s2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = title,
            style = type.sectionHeader.copy(color = headerColor),
        )
        Text(
            text = count.toString(),
            style = type.meta.copy(color = headerColor),
        )
    }
}

@Composable
private fun PlannedTasksContent(
    state: PlannedUiState,
    onCompletedChange: (String, Boolean) -> Unit,
    onSelectTask: (TaskUiModel) -> Unit,
    onSelectTaskForActions: (TaskUiModel) -> Unit,
    onToggleStar: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = OS11Theme.colors
    LazyColumn(modifier = modifier.fillMaxSize()) {
        if (state.selectedDate != null) {
            items(
                items = state.filteredTasks,
                key = { it.id },
                contentType = { "task_row" },
            ) { task ->
                TaskRow(
                    task = task,
                    onCompletedChange = { onCompletedChange(task.id, it) },
                    onClick = { onSelectTask(task) },
                    onLongClick = { onSelectTaskForActions(task) },
                    onStarClick = { onToggleStar(task.id) },
                )
            }
        } else {
            plannedTaskSection(
                title = "Overdue",
                tasks = state.overdueTasks,
                headerColor = colors.priorityCritical,
                onCompletedChange = onCompletedChange,
                onSelectTask = onSelectTask,
                onSelectTaskForActions = onSelectTaskForActions,
                onToggleStar = onToggleStar,
            )
            plannedTaskSection(
                title = "Today",
                tasks = state.todayTasks,
                headerColor = colors.accent,
                onCompletedChange = onCompletedChange,
                onSelectTask = onSelectTask,
                onSelectTaskForActions = onSelectTaskForActions,
                onToggleStar = onToggleStar,
            )
            plannedTaskSection(
                title = "Tomorrow",
                tasks = state.tomorrowTasks,
                headerColor = colors.textPrimary,
                onCompletedChange = onCompletedChange,
                onSelectTask = onSelectTask,
                onSelectTaskForActions = onSelectTaskForActions,
                onToggleStar = onToggleStar,
            )
            plannedTaskSection(
                title = "Upcoming",
                tasks = state.upcomingTasks,
                headerColor = colors.textSecondary,
                onCompletedChange = onCompletedChange,
                onSelectTask = onSelectTask,
                onSelectTaskForActions = onSelectTaskForActions,
                onToggleStar = onToggleStar,
            )
        }
    }
}

private fun LazyListScope.plannedTaskSection(
    title: String,
    tasks: List<TaskUiModel>,
    headerColor: Color,
    onCompletedChange: (String, Boolean) -> Unit,
    onSelectTask: (TaskUiModel) -> Unit,
    onSelectTaskForActions: (TaskUiModel) -> Unit,
    onToggleStar: (String) -> Unit,
) {
    if (tasks.isEmpty()) return
    item(key = "header_$title", contentType = "section_header") {
        PlannedSectionHeader(
            title = title,
            count = tasks.size,
            headerColor = headerColor,
        )
    }
    items(
        items = tasks,
        key = { it.id },
        contentType = { "task_row" },
    ) { task ->
        TaskRow(
            task = task,
            onCompletedChange = { onCompletedChange(task.id, it) },
            onClick = { onSelectTask(task) },
            onLongClick = { onSelectTaskForActions(task) },
            onStarClick = { onToggleStar(task.id) },
        )
    }
}
