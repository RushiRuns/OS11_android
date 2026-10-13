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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.os11.app.service.area.AreaService
import com.os11.app.service.project.ProjectService
import com.os11.app.service.task.TaskService
import com.os11.app.ui.components.OSEmptyState
import com.os11.app.ui.components.OSIconButton
import com.os11.app.ui.features.task.TaskActionsSheet
import com.os11.app.ui.features.task.TaskDetailSheet
import com.os11.app.ui.features.task.TaskRow
import com.os11.app.ui.features.task.TaskTreeFlattener
import com.os11.app.ui.features.task.TaskUiModel
import com.os11.app.ui.theme.OS11Theme
import com.os11.domain.id.DeterministicIds
import android.util.Log // FLOWDBG
import com.os11.domain.time.Clock
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach // FLOWDBG
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CompletedViewModel @Inject constructor(
    private val taskService: TaskService,
    private val areaService: AreaService,
    private val projectService: ProjectService,
    private val clock: Clock,
) : ViewModel() {

    private val workspaceId = DeterministicIds.defaultWorkspaceId()

    val tasks: StateFlow<List<TaskUiModel>> = combine(
        taskService.observeCompletedTasks(),
        areaService.observeAreaItems(workspaceId),
        projectService.observeProjectItems(workspaceId),
    ) { tasks, areas, projects ->
        val projectMap = projects.associate { it.id to it.name }
        val areaMap = areas.associate { it.id to it.name }
        TaskTreeFlattener.flatten(
            tasks = tasks,
            todayIso = clock.today(),
            projectNames = projectMap,
            areaNames = areaMap,
        ).toImmutableList()
    }
        .onEach { Log.d("FlowDbg", "vm completed emit size=${it.size}") } // FLOWDBG
        .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = emptyList(),
    )

    fun uncompleteTask(taskId: String) {
        viewModelScope.launch { taskService.setCompleted(taskId, false) }
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

    fun addSubtask(parentId: String, title: String) {
        if (title.isBlank()) return
        viewModelScope.launch { taskService.addSubtask(parentId, title.trim()) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompletedScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CompletedViewModel = hiltViewModel(),
) {
    val tasks by viewModel.tasks.collectAsState()
    val colors = OS11Theme.colors
    val spacing = OS11Theme.spacing
    val type = OS11Theme.type

    var selectedTaskForDetail by remember { mutableStateOf<TaskUiModel?>(null) }
    var selectedTaskForActions by remember { mutableStateOf<TaskUiModel?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.surfaceBase),
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.s4, vertical = spacing.s2),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OSIconButton(onClick = onNavigateBack, contentDescription = "Back") {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = null,
                    tint = colors.textPrimary,
                )
            }
            Text(
                text = "Completed (${tasks.size})",
                style = type.subViewTitle.copy(color = colors.textPrimary),
            )
        }

        if (tasks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(spacing.s4),
                contentAlignment = Alignment.Center,
            ) {
                OSEmptyState(
                    title = "No completed tasks",
                    description = "Tasks you complete will appear here.",
                )
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(items = tasks, key = { it.id }, contentType = { "task_row" }) { task ->
                    TaskRow(
                        task = task,
                        onCompletedChange = { viewModel.uncompleteTask(task.id) },
                        onClick = { selectedTaskForDetail = task },
                        onLongClick = { selectedTaskForActions = task },
                        onStarClick = { viewModel.toggleStar(task.id) },
                    )
                }
            }
        }
    }

    selectedTaskForDetail?.let { task ->
        TaskDetailSheet(
            title = task.title,
            notes = null,
            dueDate = task.dueDate,
            dueTime = task.dueTime,
            priority = task.priority,
            isCompleted = task.isCompleted,
            subtasks = emptyList(),
            onTitleChange = { viewModel.editTask(task.id, it, null, task.dueDate, task.priority) },
            onNotesChange = { viewModel.editTask(task.id, task.title, it, task.dueDate, task.priority) },
            onDueDateChange = { viewModel.editTask(task.id, task.title, null, it, task.priority) },
            onPriorityChange = { viewModel.editTask(task.id, task.title, null, task.dueDate, it) },
            onCompletedChange = { completed ->
                if (!completed) viewModel.uncompleteTask(task.id)
            },
            onAddSubtask = { viewModel.addSubtask(task.id, it) },
            onSubtaskCompletedChange = { _, _ -> },
            onDismissRequest = { selectedTaskForDetail = null },
        )
    }

    selectedTaskForActions?.let { task ->
        TaskActionsSheet(
            task = task,
            onDismissRequest = { selectedTaskForActions = null },
            onToggleCompleted = { viewModel.uncompleteTask(task.id) },
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
