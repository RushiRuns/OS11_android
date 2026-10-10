package com.os11.app.ui.features.inbox

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.os11.app.ui.components.OSDatePickerSheet
import com.os11.app.ui.components.OSIconButton
import com.os11.app.ui.components.OSTextField
import com.os11.app.ui.features.task.TaskActionsSheet
import com.os11.app.ui.features.task.TaskDetailSheet
import com.os11.app.ui.features.task.TaskList
import com.os11.app.ui.features.task.TaskSortFilterSheet
import com.os11.app.ui.features.task.TaskViewModel
import com.os11.app.ui.theme.OS11Theme

/**
 * Fully interactive Inbox screen for Phase 5 core task management.
 * Supports task list rendering, inline add, detail sheet, actions sheet, and sort/filter.
 */
@Composable
fun InboxScreen(modifier: Modifier = Modifier, viewModel: TaskViewModel = hiltViewModel()) {
    val colors = OS11Theme.colors
    val spacing = OS11Theme.spacing
    val type = OS11Theme.type

    val tasks by viewModel.inboxTasks.collectAsStateWithLifecycle()
    val sortOption by viewModel.sortOption.collectAsStateWithLifecycle()
    val filterOption by viewModel.filterOption.collectAsStateWithLifecycle()
    val selectedDetail by viewModel.selectedDetailTask.collectAsStateWithLifecycle()
    val detailSubtasks by viewModel.detailSubtasks.collectAsStateWithLifecycle()
    val selectedActionTask by viewModel.selectedActionsTask.collectAsStateWithLifecycle()

    var showSortFilterSheet by remember { mutableStateOf(false) }
    var showDatePickerForAction by remember { mutableStateOf<String?>(null) }
    var quickAddText by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(spacing.s4),
    ) {
        // Top action row: Title + Task Count + Sort/Filter button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.s2),
            ) {
                Text(
                    text = "Inbox",
                    style = type.viewTitle.copy(color = colors.textPrimary),
                )
                Text(
                    text = "(${tasks.size})",
                    style = type.subViewTitle.copy(color = colors.textTertiary),
                )
            }

            OSIconButton(
                onClick = { showSortFilterSheet = true },
                contentDescription = "Sort and filter options",
            ) {
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    tint = colors.textSecondary,
                    modifier = Modifier.size(22.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(spacing.s3))

        // Inline Quick Add Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OSTextField(
                value = quickAddText,
                onValueChange = { quickAddText = it },
                modifier = Modifier.weight(1f),
                placeholder = "Add task to Inbox (press + to save)...",
            )
            OSIconButton(
                onClick = {
                    if (quickAddText.isNotBlank()) {
                        viewModel.createTask(quickAddText.trim())
                        quickAddText = ""
                    }
                },
                contentDescription = "Save task",
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = colors.accent,
                )
            }
        }

        Spacer(modifier = Modifier.height(spacing.s3))

        // Task List with empty and loaded states
        TaskList(
            tasks = tasks,
            onCompletedChange = { task, completed -> viewModel.toggleCompleted(task.id, completed) },
            onClick = { task -> viewModel.openDetail(task.id) },
            onLongClick = { task -> viewModel.openActions(task) },
            onStarClick = { task -> viewModel.toggleStarred(task.id, task.isStarred) },
            emptyTitle = "Inbox Zero",
            emptyDescription = "Capture everything on your mind. Move items to projects when ready.",
            modifier = Modifier.weight(1f),
        )
    }

    // Detail Bottom Sheet
    selectedDetail?.let { detail ->
        TaskDetailSheet(
            title = detail.title,
            notes = detail.notes,
            dueDate = detail.dueDate,
            dueTime = detail.dueTime,
            priority = detail.priority,
            isCompleted = detail.isCompleted,
            subtasks = detailSubtasks,
            onTitleChange = { newTitle -> viewModel.updateTitle(detail.id, newTitle) },
            onNotesChange = { newNotes -> viewModel.updateNotes(detail.id, newNotes) },
            onDueDateChange = { newDueDate -> viewModel.updateDueDate(detail.id, newDueDate) },
            onPriorityChange = { newPriority -> viewModel.updatePriority(detail.id, newPriority) },
            onCompletedChange = { completed -> viewModel.toggleCompleted(detail.id, completed) },
            onAddSubtask = { subtaskTitle -> viewModel.addSubtask(detail.id, subtaskTitle) },
            onSubtaskCompletedChange = { subtaskId, completed ->
                viewModel.toggleCompleted(subtaskId, completed)
            },
            onDismissRequest = { viewModel.closeDetail() },
        )
    }

    // Actions Bottom Sheet
    selectedActionTask?.let { actionTask ->
        TaskActionsSheet(
            task = actionTask,
            onDismissRequest = { viewModel.closeActions() },
            onToggleCompleted = { viewModel.toggleCompleted(actionTask.id, !actionTask.isCompleted) },
            onToggleStarred = { viewModel.toggleStarred(actionTask.id, actionTask.isStarred) },
            onEdit = {
                viewModel.closeActions()
                viewModel.openDetail(actionTask.id)
            },
            onChangeDate = {
                viewModel.closeActions()
                showDatePickerForAction = actionTask.id
            },
            onAddSubtask = {
                viewModel.closeActions()
                viewModel.openDetail(actionTask.id)
            },
            onDuplicate = { viewModel.duplicateTask(actionTask.id) },
            onIndent = {
                // If there is a preceding task in the list, indent under it
                val index = tasks.indexOfFirst { it.id == actionTask.id }
                if (index > 0) {
                    val prevTask = tasks[index - 1]
                    viewModel.indentSubtask(actionTask.id, prevTask.id)
                }
            },
            onOutdent = { viewModel.outdentSubtask(actionTask.id) },
            onPromote = { viewModel.promoteSubtask(actionTask.id) },
            onTrash = { viewModel.trashTask(actionTask.id) },
        )
    }

    // Date Picker for Long-press Action
    showDatePickerForAction?.let { taskId ->
        OSDatePickerSheet(
            onDismissRequest = { showDatePickerForAction = null },
            onDateSelected = { selectedDate ->
                showDatePickerForAction = null
                viewModel.updateDueDate(taskId, selectedDate)
            },
        )
    }

    // Sort & Filter Bottom Sheet
    if (showSortFilterSheet) {
        TaskSortFilterSheet(
            selectedSort = sortOption,
            selectedFilter = filterOption,
            onSortSelected = { viewModel.setSort(it) },
            onFilterSelected = { viewModel.setFilter(it) },
            onDismissRequest = { showSortFilterSheet = false },
        )
    }
}
