package com.os11.app.ui.features.inbox

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
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
import com.os11.app.service.tag.TagItem
import com.os11.app.ui.components.OSDatePickerSheet
import com.os11.app.ui.components.OSIconButton
import com.os11.app.ui.components.OSPriority
import com.os11.app.ui.components.OSPriorityPickerSheet
import com.os11.app.ui.components.OSTextField
import com.os11.app.ui.components.TagPickerSheet
import com.os11.app.ui.features.task.ReorderableTaskList
import com.os11.app.ui.features.task.TaskActionsSheet
import com.os11.app.ui.features.task.TaskBulkActionBar
import com.os11.app.ui.features.task.TaskDetailSheet
import com.os11.app.ui.features.task.TaskSortFilterSheet
import com.os11.app.ui.features.task.TaskUiModel
import com.os11.app.ui.features.task.TaskViewModel
import com.os11.app.ui.theme.OS11Theme

/**
 * Interactive Inbox screen satisfying Phase 8 specifications:
 * - Swipe right to complete, swipe left for When sheet (with visible non-gesture alternatives)
 * - Drag to reorder backed by ReorderableTaskList (zero writes during drag, one on drop)
 * - Multi-select mode with TaskBulkActionBar and Select All
 * - Bulk actions: complete, set date, priority, trash
 * - Date picker sheet with natural language, quick options, calendar grid, 15-min intervals
 * - TalkBack custom actions for every gesture
 */
@Composable
fun InboxScreen(modifier: Modifier = Modifier, viewModel: TaskViewModel = hiltViewModel()) {
    val spacing = OS11Theme.spacing

    val tasks by viewModel.inboxTasks.collectAsStateWithLifecycle()
    val sortOption by viewModel.sortOption.collectAsStateWithLifecycle()
    val filterOption by viewModel.filterOption.collectAsStateWithLifecycle()
    val selectedDetail by viewModel.selectedDetailTask.collectAsStateWithLifecycle()
    val detailSubtasks by viewModel.detailSubtasks.collectAsStateWithLifecycle()
    val detailTags by viewModel.detailTags.collectAsStateWithLifecycle()
    val detailAttachments by viewModel.detailAttachments.collectAsStateWithLifecycle()
    val allTags by viewModel.allTags.collectAsStateWithLifecycle()
    val selectedActionTask by viewModel.selectedActionsTask.collectAsStateWithLifecycle()
    val isSelectionMode by viewModel.isSelectionMode.collectAsStateWithLifecycle()
    val selectedTaskIds by viewModel.selectedTaskIds.collectAsStateWithLifecycle()

    var showSortFilterSheet by remember { mutableStateOf(false) }
    var showDatePickerForAction by remember { mutableStateOf<String?>(null) }
    var showBulkDatePicker by remember { mutableStateOf(false) }
    var showBulkPriorityPicker by remember { mutableStateOf(false) }
    var showBulkTagPicker by remember { mutableStateOf(false) }
    var quickAddText by remember { mutableStateOf("") }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(spacing.s4),
        ) {
            InboxHeaderRow(
                taskCount = tasks.size,
                isSelectionMode = isSelectionMode,
                onToggleSelectionMode = {
                    if (isSelectionMode) {
                        viewModel.clearSelection()
                    } else {
                        viewModel.enterSelectionMode()
                    }
                },
                onOpenSortFilter = { showSortFilterSheet = true },
            )

            Spacer(modifier = Modifier.height(spacing.s3))

            if (!isSelectionMode) {
                InboxQuickAddBar(
                    text = quickAddText,
                    onTextChange = { quickAddText = it },
                    onSave = {
                        viewModel.createTask(quickAddText.trim())
                        quickAddText = ""
                    },
                )
                Spacer(modifier = Modifier.height(spacing.s3))
            }

            ReorderableTaskList(
                tasks = tasks,
                onCompletedChange = { task, completed -> viewModel.toggleCompleted(task.id, completed) },
                onClick = { task ->
                    if (isSelectionMode) {
                        viewModel.toggleSelect(task.id)
                    } else {
                        viewModel.openDetail(task.id)
                    }
                },
                onLongClick = { task -> viewModel.openActions(task) },
                onStarClick = { task -> viewModel.toggleStarred(task.id, task.isStarred) },
                onWhenClick = { task -> showDatePickerForAction = task.id },
                onReorderDrop = { _, _, task, newOrder -> viewModel.reorderTask(task.id, newOrder) },
                isSelectionMode = isSelectionMode,
                selectedTaskIds = selectedTaskIds,
                onSelectionChange = { task, _ -> viewModel.toggleSelect(task.id) },
                emptyTitle = "Inbox Zero",
                emptyDescription = "Capture everything on your mind. Move items to projects when ready.",
                modifier = Modifier.weight(1f),
            )
        }

        if (isSelectionMode) {
            TaskBulkActionBar(
                selectedCount = selectedTaskIds.size,
                totalCount = tasks.size,
                onSelectAllToggle = { viewModel.toggleSelectAll(tasks.map { it.id }) },
                onComplete = { viewModel.bulkComplete(true) },
                onSetDate = { showBulkDatePicker = true },
                onMove = { /* Move handled via container picker */ },
                onTag = { showBulkTagPicker = true },
                onPriority = { showBulkPriorityPicker = true },
                onTrash = { viewModel.bulkTrash() },
                onClose = { viewModel.clearSelection() },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }

    selectedDetail?.let { detail ->
        InboxDetailSheetDelegate(
            detail = detail,
            detailSubtasks = detailSubtasks,
            detailTags = detailTags,
            detailAttachments = detailAttachments,
            allTags = allTags,
            viewModel = viewModel,
        )
    }

    selectedActionTask?.let { actionTask ->
        InboxActionsSheetDelegate(
            actionTask = actionTask,
            tasks = tasks,
            viewModel = viewModel,
            onShowDatePicker = { showDatePickerForAction = actionTask.id },
        )
    }

    showDatePickerForAction?.let { taskId ->
        OSDatePickerSheet(
            onDismissRequest = { showDatePickerForAction = null },
            onDateSelected = { date, _ ->
                viewModel.updateDueDate(taskId, date)
                showDatePickerForAction = null
            },
        )
    }

    if (showBulkDatePicker) {
        OSDatePickerSheet(
            onDismissRequest = { showBulkDatePicker = false },
            onDateSelected = { date, time ->
                viewModel.bulkSetDueDate(date, time)
                showBulkDatePicker = false
            },
        )
    }

    if (showBulkPriorityPicker) {
        OSPriorityPickerSheet(
            selectedPriority = OSPriority.None,
            onPrioritySelected = { priority ->
                viewModel.bulkSetPriority(priority.value)
                showBulkPriorityPicker = false
            },
            onDismissRequest = { showBulkPriorityPicker = false },
        )
    }

    if (showBulkTagPicker) {
        TagPickerSheet(
            tags = allTags,
            selectedTagIds = emptySet(),
            onTagToggled = { tagId ->
                viewModel.bulkAddTag(tagId)
                showBulkTagPicker = false
            },
            onCreateTag = { _, _ -> },
            onDismissRequest = { showBulkTagPicker = false },
        )
    }

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

@Composable
private fun InboxHeaderRow(
    taskCount: Int,
    isSelectionMode: Boolean,
    onToggleSelectionMode: () -> Unit,
    onOpenSortFilter: () -> Unit,
) {
    val colors = OS11Theme.colors
    val spacing = OS11Theme.spacing
    val type = OS11Theme.type

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
                text = "($taskCount)",
                style = type.subViewTitle.copy(color = colors.textTertiary),
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.s1),
        ) {
            OSIconButton(
                onClick = onToggleSelectionMode,
                contentDescription = if (isSelectionMode) "Exit selection mode" else "Multi-select mode",
            ) {
                val icon = if (isSelectionMode) {
                    Icons.Outlined.CheckCircle
                } else {
                    Icons.AutoMirrored.Outlined.List
                }
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelectionMode) colors.accent else colors.textSecondary,
                    modifier = Modifier.size(22.dp),
                )
            }

            OSIconButton(
                onClick = onOpenSortFilter,
                contentDescription = "Sort and filter options",
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = null,
                    tint = colors.textSecondary,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}

@Composable
private fun InboxQuickAddBar(text: String, onTextChange: (String) -> Unit, onSave: () -> Unit) {
    val colors = OS11Theme.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OSTextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier.weight(1f),
            placeholder = "Add task to Inbox (press + to save)...",
        )
        OSIconButton(
            onClick = {
                if (text.isNotBlank()) onSave()
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
}

@Composable
private fun InboxDetailSheetDelegate(
    detail: com.os11.app.service.task.TaskItem,
    detailSubtasks: List<TaskUiModel>,
    detailTags: List<TagItem>,
    detailAttachments: List<com.os11.app.service.attachment.AttachmentItem>,
    allTags: List<TagItem>,
    viewModel: TaskViewModel,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    TaskDetailSheet(
        title = detail.title,
        notes = detail.notes,
        dueDate = detail.dueDate,
        dueTime = detail.dueTime,
        priority = detail.priority,
        isCompleted = detail.isCompleted,
        subtasks = detailSubtasks,
        tags = detailTags,
        allTags = allTags,
        attachments = detailAttachments,
        onTitleChange = { newTitle -> viewModel.updateTitle(detail.id, newTitle) },
        onNotesChange = { newNotes -> viewModel.updateNotes(detail.id, newNotes) },
        onDueDateChange = { newDueDate -> viewModel.updateDueDate(detail.id, newDueDate) },
        onPriorityChange = { newPriority -> viewModel.updatePriority(detail.id, newPriority) },
        onCompletedChange = { completed -> viewModel.toggleCompleted(detail.id, completed) },
        onAddSubtask = { subtaskTitle -> viewModel.addSubtask(detail.id, subtaskTitle) },
        onSubtaskCompletedChange = { subtaskId, completed ->
            viewModel.toggleCompleted(subtaskId, completed)
        },
        onAddTag = { tagId -> viewModel.addTagToTask(detail.id, tagId) },
        onRemoveTag = { tagId -> viewModel.removeTagFromTask(detail.id, tagId) },
        onCreateTag = { name, parentId -> viewModel.createTagAndAddToTask(detail.id, name, parentId) },
        onAddFileAttachment = { uri -> viewModel.addFileAttachment(detail.id, uri) },
        onAddLinkAttachment = { url, title -> viewModel.addLinkAttachment(detail.id, url, title) },
        onOpenAttachment = { item -> viewModel.openAttachment(item, context) },
        onDeleteAttachment = { id -> viewModel.deleteAttachment(id) },
        onDismissRequest = { viewModel.closeDetail() },
    )
}

@Composable
private fun InboxActionsSheetDelegate(
    actionTask: TaskUiModel,
    tasks: List<TaskUiModel>,
    viewModel: TaskViewModel,
    onShowDatePicker: () -> Unit,
) {
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
            onShowDatePicker()
        },
        onAddSubtask = {
            viewModel.closeActions()
            viewModel.openDetail(actionTask.id)
        },
        onDuplicate = { viewModel.duplicateTask(actionTask.id) },
        onIndent = {
            val index = tasks.indexOfFirst { it.id == actionTask.id }
            if (index > 0) {
                val prevTask = tasks[index - 1]
                viewModel.indentSubtask(actionTask.id, prevTask.id)
            }
        },
        onOutdent = { viewModel.outdentSubtask(actionTask.id) },
        onPromote = { viewModel.promoteSubtask(actionTask.id) },
        onTrash = { viewModel.trashTask(actionTask.id) },
        onSelect = { viewModel.enterSelectionMode(actionTask.id) },
    )
}
