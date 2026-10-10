package com.os11.app.ui.features.project

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.os11.app.service.project.SectionItem
import com.os11.app.ui.components.OSButton
import com.os11.app.ui.components.OSChip
import com.os11.app.ui.components.OSIconButton
import com.os11.app.ui.components.OSProgressRing
import com.os11.app.ui.components.OSSnackbarHost
import com.os11.app.ui.components.OSTextField
import com.os11.app.ui.features.task.TaskActionsSheet
import com.os11.app.ui.features.task.TaskDetailSheet
import com.os11.app.ui.features.task.TaskUiModel
import com.os11.app.ui.theme.OS11Theme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("LongMethod", "CyclomaticComplexMethod")
fun ProjectScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProjectViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val colors = OS11Theme.colors
    val spacing = OS11Theme.spacing
    val type = OS11Theme.type
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var quickAddText by remember { mutableStateOf("") }
    var selectedTaskForDetail by remember { mutableStateOf<TaskUiModel?>(null) }
    var selectedTaskForActions by remember { mutableStateOf<TaskUiModel?>(null) }
    var selectedTaskForDependencies by remember { mutableStateOf<TaskUiModel?>(null) }

    var showAddSectionDialog by remember { mutableStateOf(false) }
    var editingSectionForRename by remember { mutableStateOf<SectionItem?>(null) }
    var showMilestonesSheet by remember { mutableStateOf(false) }
    var projectMenuExpanded by remember { mutableStateOf(false) }
    var statusMenuExpanded by remember { mutableStateOf(false) }
    var exportedCsvContent by remember { mutableStateOf<String?>(null) }

    val views = remember { listOf("list", "board", "timeline", "calendar", "table") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.surfaceBase),
    ) {
        // 1. Header: Back + Title + Progress Ring + Status + Options
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
                modifier = Modifier.weight(1f),
            ) {
                OSIconButton(onClick = onNavigateBack, contentDescription = "Back") {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = null,
                        tint = colors.textPrimary,
                    )
                }
                Text(
                    text = state.project?.name ?: "Project",
                    style = type.subViewTitle.copy(color = colors.textPrimary),
                    maxLines = 1,
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.s2),
            ) {
                // Progress Ring
                if (state.totalCount > 0) {
                    val progress = state.completedCount.toFloat() / state.totalCount.toFloat()
                    OSProgressRing(
                        progress = progress,
                        size = 28.dp,
                        strokeWidth = 3.dp,
                    )
                }

                // Status chip & menu
                Box {
                    state.project?.let { proj ->
                        OSChip(
                            label = proj.status.replaceFirstChar { it.uppercase() },
                            selected = false,
                            onClick = { statusMenuExpanded = true },
                        )
                    }

                    DropdownMenu(
                        expanded = statusMenuExpanded,
                        onDismissRequest = { statusMenuExpanded = false },
                    ) {
                        listOf("active", "on_hold", "completed", "archived").forEach { st ->
                            DropdownMenuItem(
                                text = { Text(st.replaceFirstChar { it.uppercase() }) },
                                onClick = {
                                    statusMenuExpanded = false
                                    viewModel.updateProjectStatus(st)
                                },
                            )
                        }
                    }
                }

                // More Options Menu
                Box {
                    OSIconButton(
                        onClick = { projectMenuExpanded = true },
                        contentDescription = "Project options",
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = null,
                            tint = colors.textPrimary,
                        )
                    }

                    DropdownMenu(
                        expanded = projectMenuExpanded,
                        onDismissRequest = { projectMenuExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("Add Section") },
                            onClick = {
                                projectMenuExpanded = false
                                showAddSectionDialog = true
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Milestones") },
                            onClick = {
                                projectMenuExpanded = false
                                showMilestonesSheet = true
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Export CSV") },
                            onClick = {
                                projectMenuExpanded = false
                                viewModel.exportProjectToCsv { csv -> exportedCsvContent = csv }
                            },
                        )
                        if (state.project?.status == "archived") {
                            DropdownMenuItem(
                                text = { Text("Unarchive Project") },
                                onClick = {
                                    projectMenuExpanded = false
                                    viewModel.unarchiveProject()
                                },
                            )
                        } else {
                            DropdownMenuItem(
                                text = { Text("Archive Project") },
                                onClick = {
                                    projectMenuExpanded = false
                                    viewModel.archiveProject()
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Project archived")
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }

        // 2. Archived Warning Banner
        if (state.project?.status == "archived") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.surfaceRaised)
                    .padding(horizontal = spacing.s4, vertical = spacing.s2),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.s2),
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = colors.textSecondary,
                    )
                    Text(
                        text = "This project is archived",
                        style = type.body.copy(color = colors.textSecondary),
                    )
                }
                OSButton(
                    onClick = { viewModel.unarchiveProject() },
                ) {
                    Text("Unarchive")
                }
            }
        }

        // 3. View Switcher Chips (List, Board, Timeline, Calendar, Table)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = spacing.s4, vertical = spacing.s1),
            horizontalArrangement = Arrangement.spacedBy(spacing.s2),
        ) {
            views.forEach { viewOption ->
                OSChip(
                    label = viewOption.replaceFirstChar { it.uppercase() },
                    selected = state.selectedView == viewOption,
                    onClick = { viewModel.selectView(viewOption) },
                )
            }
        }

        // 4. Quick Add Bar (for List, Board, Table views)
        if (state.selectedView in listOf("list", "table", "board")) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.s4, vertical = spacing.s1),
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
        }

        // 5. Active View Body
        Box(modifier = Modifier.weight(1f)) {
            when (state.selectedView) {
                "board" -> {
                    ProjectBoardView(
                        sections = state.sections,
                        tasks = state.tasks,
                        onCompletedChange = { id, comp -> viewModel.setCompleted(id, comp) },
                        onTaskClick = { selectedTaskForDetail = it },
                        onTaskLongClick = { selectedTaskForActions = it },
                        onMoveTaskToSection = { taskId, targetSecId ->
                            viewModel.moveTaskToSection(taskId, targetSecId)
                        },
                        onAddTaskToSection = { secId ->
                            viewModel.addTaskToProject("New task", secId)
                        },
                    )
                }
                "timeline" -> {
                    ProjectTimelineView(
                        tasks = state.tasks,
                        milestones = state.milestones,
                        dependencies = state.dependencies,
                        zoomWeeks = state.timelineZoomWeeks,
                        onToggleZoom = { viewModel.toggleTimelineZoom() },
                        onOpenMilestones = { showMilestonesSheet = true },
                        onOpenDependencies = { selectedTaskForDependencies = it },
                    )
                }
                "calendar" -> {
                    ProjectCalendarView(
                        tasks = state.tasks,
                        referenceDate = state.referenceDate,
                        selectedDate = state.selectedDate,
                        datesWithTasks = state.datesWithTasks,
                        onDateSelected = { viewModel.onDateSelected(it) },
                        onWeekOffsetChange = { viewModel.onWeekOffsetChange(it) },
                        onCompletedChange = { id, comp -> viewModel.setCompleted(id, comp) },
                        onTaskClick = { selectedTaskForDetail = it },
                        onTaskLongClick = { selectedTaskForActions = it },
                        onToggleStar = { viewModel.toggleStar(it) },
                    )
                }
                else -> {
                    // "list" or "table"
                    Column(modifier = Modifier.fillMaxSize()) {
                        if (state.selectedView == "table") {
                            Text(
                                text = "Table View (List Layout)",
                                style = type.meta.copy(color = colors.textTertiary),
                                modifier = Modifier.padding(horizontal = spacing.s4, vertical = 2.dp),
                            )
                        }
                        ProjectListView(
                            sections = state.sections,
                            tasks = state.tasks,
                            onCompletedChange = { id, comp -> viewModel.setCompleted(id, comp) },
                            onTaskClick = { selectedTaskForDetail = it },
                            onTaskLongClick = { selectedTaskForActions = it },
                            onToggleStar = { viewModel.toggleStar(it) },
                            onToggleCollapse = { secId, coll ->
                                viewModel.toggleSectionCollapse(secId, coll)
                            },
                            onRenameSection = { editingSectionForRename = it },
                            onMoveSectionUp = { sec ->
                                val ids = state.sections.map { it.id }.toMutableList()
                                val idx = ids.indexOf(sec.id)
                                if (idx > 0) {
                                    ids.removeAt(idx)
                                    ids.add(idx - 1, sec.id)
                                    viewModel.reorderSections(ids)
                                }
                            },
                            onMoveSectionDown = { sec ->
                                val ids = state.sections.map { it.id }.toMutableList()
                                val idx = ids.indexOf(sec.id)
                                if (idx in 0 until ids.size - 1) {
                                    ids.removeAt(idx)
                                    ids.add(idx + 1, sec.id)
                                    viewModel.reorderSections(ids)
                                }
                            },
                            onDeleteSection = { sec -> viewModel.deleteSection(sec.id) },
                            onAddSectionClick = { showAddSectionDialog = true },
                            onAddTaskToSection = { secId ->
                                viewModel.addTaskToProject("New task", secId)
                            },
                        )
                    }
                }
            }
        }

        OSSnackbarHost(hostState = snackbarHostState)
    }

    // Dialogs & Sheets
    if (showAddSectionDialog) {
        AddSectionDialog(
            onDismiss = { showAddSectionDialog = false },
            onConfirm = { name -> viewModel.createSection(name) },
        )
    }

    editingSectionForRename?.let { section ->
        RenameSectionDialog(
            initialName = section.name,
            onDismiss = { editingSectionForRename = null },
            onConfirm = { newName -> viewModel.renameSection(section.id, newName) },
        )
    }

    if (showMilestonesSheet) {
        ProjectMilestonesSheet(
            milestones = state.milestones,
            onToggleCompleted = { id, comp -> viewModel.toggleMilestoneCompleted(id, comp) },
            onDeleteMilestone = { id -> viewModel.deleteMilestone(id) },
            onCreateMilestone = { title, date -> viewModel.createMilestone(title, date) },
            onDismissRequest = { showMilestonesSheet = false },
        )
    }

    selectedTaskForDependencies?.let { task ->
        ProjectDependenciesSheet(
            task = task,
            allTasks = state.tasks,
            dependencies = state.dependencies,
            errorMessage = state.errorMessage,
            onAddDependency = { tId, depId -> viewModel.addDependency(tId, depId) },
            onRemoveDependency = { tId, depId -> viewModel.removeDependency(tId, depId) },
            onClearErrorMessage = { viewModel.clearErrorMessage() },
            onDismissRequest = {
                viewModel.clearErrorMessage()
                selectedTaskForDependencies = null
            },
        )
    }

    exportedCsvContent?.let { csv ->
        ExportCsvDialog(
            csv = csv,
            onShare = {
                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                    putExtra(Intent.EXTRA_TEXT, csv)
                    setType("text/plain")
                }
                context.startActivity(Intent.createChooser(sendIntent, "Share Project CSV"))
            },
            onDismiss = { exportedCsvContent = null },
        )
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
            onCompletedChange = { completed -> viewModel.setCompleted(task.id, completed) },
            onAddSubtask = { subtaskTitle -> viewModel.addSubtask(task.id, subtaskTitle) },
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
            onDuplicate = { selectedTaskForActions = null },
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExportCsvDialog(csv: String, onShare: () -> Unit, onDismiss: () -> Unit) {
    val colors = OS11Theme.colors
    val spacing = OS11Theme.spacing
    val type = OS11Theme.type

    BasicAlertDialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(OS11Theme.radius.lg))
                .background(colors.surfaceRaised)
                .padding(spacing.s4),
        ) {
            Text(
                text = "Export Project CSV",
                style = type.panelTitle.copy(color = colors.textPrimary),
            )
            Spacer(modifier = Modifier.height(spacing.s2))
            Text(
                text = "CSV data successfully generated (${csv.lines().size} lines).",
                style = type.body.copy(color = colors.textSecondary),
            )
            Spacer(modifier = Modifier.height(spacing.s4))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.s2),
            ) {
                OSButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Close")
                }
                OSButton(
                    onClick = {
                        onShare()
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Share")
                }
            }
        }
    }
}
