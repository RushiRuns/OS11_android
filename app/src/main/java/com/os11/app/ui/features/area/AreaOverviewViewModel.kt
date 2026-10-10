package com.os11.app.ui.features.area

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.os11.app.service.area.AreaItem
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

data class AreaOverviewUiState(
    val area: AreaItem? = null,
    val projects: List<ProjectItem> = emptyList(),
    val looseTasks: List<TaskUiModel> = emptyList(),
    val totalTaskCount: Int = 0,
    val isLoading: Boolean = false,
)

@HiltViewModel
class AreaOverviewViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val areaService: AreaService,
    private val projectService: ProjectService,
    private val taskService: TaskService,
    private val clock: Clock,
) : ViewModel() {

    val areaId: String = checkNotNull(savedStateHandle["areaId"])
    private val workspaceId = DeterministicIds.defaultWorkspaceId()

    private val _areaItem = MutableStateFlow<AreaItem?>(null)
    val areaItem: StateFlow<AreaItem?> = _areaItem.asStateFlow()

    init {
        loadArea()
    }

    private fun loadArea() {
        viewModelScope.launch {
            _areaItem.value = areaService.getAreaItem(areaId)
        }
    }

    private val projectsFlow = projectService.observeProjectItemsByArea(areaId)
    private val areaTasksFlow = taskService.observeTasksByArea(areaId)

    val uiState: StateFlow<AreaOverviewUiState> = combine(
        _areaItem,
        projectsFlow,
        areaTasksFlow,
    ) { area, projects, areaTasks ->
        val loose = areaTasks.filter { it.projectId == null && !it.isTrashed }
        val projectMap = projects.associate { it.id to it.name }
        val areaMap = if (area != null) mapOf(area.id to area.name) else emptyMap()

        val flattened = TaskTreeFlattener.flatten(
            tasks = loose,
            todayIso = clock.today(),
            projectNames = projectMap,
            areaNames = areaMap,
        )

        AreaOverviewUiState(
            area = area,
            projects = projects.toImmutableList(),
            looseTasks = flattened.toImmutableList(),
            totalTaskCount = areaTasks.count { !it.isTrashed && !it.isCompleted },
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = AreaOverviewUiState(isLoading = true),
    )

    fun addLooseTask(title: String) {
        if (title.isBlank()) return
        viewModelScope.launch {
            taskService.createTask(
                title = title.trim(),
                workspaceId = workspaceId,
                areaId = areaId,
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
