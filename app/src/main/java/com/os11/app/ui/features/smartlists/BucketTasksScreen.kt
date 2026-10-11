package com.os11.app.ui.features.smartlists

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.os11.app.service.area.AreaService
import com.os11.app.service.project.ProjectService
import com.os11.app.service.task.TaskSchedulingService
import com.os11.app.service.task.TaskService
import com.os11.app.ui.components.OSEmptyState
import com.os11.app.ui.components.OSIconButton
import com.os11.app.ui.components.OSTextField
import com.os11.app.ui.features.task.TaskActionsSheet
import com.os11.app.ui.features.task.TaskDetailSheet
import com.os11.app.ui.features.task.TaskRow
import com.os11.app.ui.features.task.TaskTreeFlattener
import com.os11.app.ui.features.task.TaskUiModel
import com.os11.app.ui.theme.OS11Theme
import com.os11.domain.id.DeterministicIds
import com.os11.domain.time.Clock
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BucketTasksViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val taskService: TaskService,
    private val taskSchedulingService: TaskSchedulingService,
    private val areaService: AreaService,
    private val projectService: ProjectService,
    private val clock: Clock,
) : ViewModel() {

    val bucketType: String = savedStateHandle["bucketType"] ?: "anytime"
    private val workspaceId = DeterministicIds.defaultWorkspaceId()

    private val sourceFlow = if (bucketType == "waiting") {
        taskService.observeWaitingTasks()
    } else {
        taskService.observeBucketTasks(bucketType)
    }

    val tasks: StateFlow<List<TaskUiModel>> = combine(
        sourceFlow,
        areaService.observeAreaItems(workspaceId),
        projectService.observeProjectItems(workspaceId),
    ) { rawTasks, areas, projects ->
        val projectMap = projects.associate { it.id to it.name }
        val areaMap = areas.associate { it.id to it.name }
        TaskTreeFlattener.flatten(
            tasks = rawTasks,
            todayIso = clock.today(),
            projectNames = projectMap,
            areaNames = areaMap,
        ).toImmutableList()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = emptyList(),
    )

    fun addTask(title: String) {
        if (title.isBlank()) return
        viewModelScope.launch {
            val result = taskService.createTask(
                title = title.trim(),
                workspaceId = workspaceId,
            )
            val taskId = result.getOrNull()?.id
            if (taskId != null) {
                if (bucketType == "waiting") {
                    taskSchedulingService.setWaiting(taskId, "Someone", null)
                } else {
                    taskSchedulingService.moveToBucket(taskId, bucketType)
                }
            }
        }
    }

    fun setCompleted(taskId: String, completed: Boolean) {
        viewModelScope.launch { taskService.setCompleted(taskId, completed) }
    }

    fun toggleStar(taskId: String) {
        viewModelScope.launch { taskService.toggleStar(taskId) }
    }

    fun editTask(id: String, title: String, notes: String?, dueDate: String?, priority: Int) {
        viewModelScope.launch {
            taskService.editTask(
                id = id,
                title = title,
                notes = notes,
                dueDate = dueDate,
                priority = priority,
            )
        }
    }

    fun trashTask(id: String) {
        viewModelScope.launch { taskService.trashTask(id) }
    }

    fun skipOccurrence(id: String) {
        viewModelScope.launch { taskSchedulingService.skipOccurrence(id) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BucketTasksScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BucketTasksViewModel = hiltViewModel(),
) {
    val bucketType = viewModel.bucketType
    val tasks by viewModel.tasks.collectAsState()
    val colors = OS11Theme.colors
    val spacing = OS11Theme.spacing
    val type = OS11Theme.type

    var showDetailSheet by remember { mutableStateOf(false) }
    var selectedTask by remember { mutableStateOf<TaskUiModel?>(null) }
    var showActionsSheet by remember { mutableStateOf(false) }
    var actionTask by remember { mutableStateOf<TaskUiModel?>(null) }
    var newTaskTitle by remember { mutableStateOf("") }

    val screenTitle = when (bucketType) {
        "waiting" -> "Waiting For"
        "someday" -> "Someday"
        else -> "Anytime"
    }

    val emptyMessage = when (bucketType) {
        "waiting" -> "No delegated or blocked tasks waiting for someone."
        "someday" -> "No ideas or future projects parked in Someday."
        else -> "No tasks scheduled for anytime execution."
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.surfaceBase),
    ) {
        // App bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.s2, vertical = spacing.s3),
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
            Text(
                text = screenTitle,
                style = type.viewTitle.copy(color = colors.textPrimary),
                modifier = Modifier.padding(start = spacing.s2),
            )
        }

        // Quick add row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.s4, vertical = spacing.s2),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OSTextField(
                value = newTaskTitle,
                onValueChange = { newTaskTitle = it },
                placeholder = "Add a task to $screenTitle...",
                modifier = Modifier.weight(1f),
            )
            OSIconButton(
                onClick = {
                    if (newTaskTitle.isNotBlank()) {
                        viewModel.addTask(newTaskTitle)
                        newTaskTitle = ""
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

        // Task list
        if (tasks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(spacing.s4),
                contentAlignment = Alignment.Center,
            ) {
                OSEmptyState(
                    title = "Nothing here",
                    description = emptyMessage,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = spacing.s4),
            ) {
                items(
                    items = tasks,
                    key = { it.id },
                ) { task ->
                    TaskRow(
                        task = task,
                        onCompletedChange = { completed ->
                            viewModel.setCompleted(task.id, completed)
                        },
                        onClick = {
                            selectedTask = task
                            showDetailSheet = true
                        },
                        onLongClick = {
                            actionTask = task
                            showActionsSheet = true
                        },
                        onStarClick = {
                            viewModel.toggleStar(task.id)
                        },
                    )
                }
            }
        }
    }

    // Detail Sheet
    if (showDetailSheet && selectedTask != null) {
        val task = selectedTask!!
        TaskDetailSheet(
            title = task.title,
            notes = null,
            dueDate = task.dueDate,
            dueTime = task.dueTime,
            priority = task.priority,
            isCompleted = task.isCompleted,
            subtasks = emptyList(),
            recurrenceRule = task.recurrenceRule,
            onTitleChange = { newTitle ->
                viewModel.editTask(task.id, newTitle, null, task.dueDate, task.priority)
            },
            onNotesChange = { /* notes edit */ },
            onDueDateChange = { newDate ->
                viewModel.editTask(task.id, task.title, null, newDate, task.priority)
            },
            onPriorityChange = { newPriority ->
                viewModel.editTask(task.id, task.title, null, task.dueDate, newPriority)
            },
            onCompletedChange = { completed ->
                viewModel.setCompleted(task.id, completed)
                showDetailSheet = false
            },
            onAddSubtask = { },
            onSubtaskCompletedChange = { _, _ -> },
            onDismissRequest = {
                showDetailSheet = false
                selectedTask = null
            },
        )
    }

    // Actions Sheet
    if (showActionsSheet && actionTask != null) {
        val task = actionTask!!
        TaskActionsSheet(
            task = task,
            onDismissRequest = {
                showActionsSheet = false
                actionTask = null
            },
            onToggleCompleted = {
                viewModel.setCompleted(task.id, !task.isCompleted)
            },
            onToggleStarred = {
                viewModel.toggleStar(task.id)
            },
            onEdit = {
                selectedTask = task
                showDetailSheet = true
            },
            onChangeDate = { },
            onAddSubtask = { },
            onDuplicate = { },
            onIndent = { },
            onOutdent = { },
            onPromote = { },
            onTrash = {
                viewModel.trashTask(task.id)
            },
            onSkipOccurrence = if (task.recurrenceRule != null) {
                { viewModel.skipOccurrence(task.id) }
            } else {
                null
            },
        )
    }
}
