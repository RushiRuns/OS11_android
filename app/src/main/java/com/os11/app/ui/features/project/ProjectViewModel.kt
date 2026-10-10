package com.os11.app.ui.features.project

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.os11.app.service.area.AreaService
import com.os11.app.service.project.ProjectItem
import com.os11.app.service.project.ProjectService
import com.os11.app.service.task.TaskService
import com.os11.app.ui.features.task.TaskTreeFlattener
import com.os11.app.ui.features.task.TaskUiModel
import com.os11.domain.id.DeterministicIds
import com.os11.domain.time.Clock
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProjectUiState(
    val project: ProjectItem? = null,
    val tasks: List<TaskUiModel> = emptyList(),
    val completedCount: Int = 0,
    val totalCount: Int = 0,
    val isLoading: Boolean = false,
)

@HiltViewModel
class ProjectViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val projectService: ProjectService,
    private val areaService: AreaService,
    private val taskService: TaskService,
    private val clock: Clock,
) : ViewModel() {

    val projectId: String = checkNotNull(savedStateHandle["projectId"])
    private val workspaceId = DeterministicIds.defaultWorkspaceId()

    private val _projectItem = MutableStateFlow<ProjectItem?>(null)
    val projectItem: StateFlow<ProjectItem?> = _projectItem.asStateFlow()

    init {
        loadProject()
    }

    private fun loadProject() {
        viewModelScope.launch {
            _projectItem.value = projectService.getProjectItem(projectId)
        }
    }

    private val projectTasksFlow = taskService.observeTasksByProject(projectId)
    private val areasFlow = areaService.observeAreaItems(workspaceId)

    val uiState: StateFlow<ProjectUiState> = combine(
        _projectItem,
        projectTasksFlow,
        areasFlow,
    ) { project, tasks, areas ->
        val projectMap = if (project != null) mapOf(project.id to project.name) else emptyMap()
        val areaMap = areas.associate { it.id to it.name }

        val flattened = TaskTreeFlattener.flatten(
            tasks = tasks,
            todayIso = clock.today(),
            projectNames = projectMap,
            areaNames = areaMap,
        )

        val completed = flattened.count { it.isCompleted }
        val total = flattened.size

        ProjectUiState(
            project = project,
            tasks = flattened.toImmutableList(),
            completedCount = completed,
            totalCount = total,
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = ProjectUiState(isLoading = true),
    )

    fun addTaskToProject(title: String) {
        if (title.isBlank()) return
        viewModelScope.launch {
            taskService.createTask(
                title = title.trim(),
                workspaceId = workspaceId,
                projectId = projectId,
                areaId = _projectItem.value?.areaId,
            )
        }
    }

    fun setCompleted(taskId: String, completed: Boolean) {
        viewModelScope.launch {
            taskService.setCompleted(taskId, completed)
        }
    }

    fun toggleStar(taskId: String) {
        viewModelScope.launch {
            taskService.toggleStar(taskId)
        }
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
        viewModelScope.launch {
            taskService.trashTask(id)
        }
    }

    fun addSubtask(parentId: String, title: String) {
        if (title.isBlank()) return
        viewModelScope.launch {
            taskService.addSubtask(parentId, title.trim())
        }
    }
}
