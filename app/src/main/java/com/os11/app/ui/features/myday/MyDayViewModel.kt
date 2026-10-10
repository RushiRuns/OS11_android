package com.os11.app.ui.features.myday

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.os11.app.service.area.AreaService
import com.os11.app.service.project.ProjectService
import com.os11.app.service.settings.SettingsService
import com.os11.app.service.task.TaskItem
import com.os11.app.service.task.TaskService
import com.os11.app.ui.features.task.TaskTreeFlattener
import com.os11.app.ui.features.task.TaskUiModel
import com.os11.domain.id.DeterministicIds
import com.os11.domain.result.onSuccess
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

data class MyDayUiState(
    val logicalToday: String = "",
    val tasks: List<TaskUiModel> = emptyList(),
    val suggestions: List<TaskUiModel> = emptyList(),
    val completedCount: Int = 0,
    val totalCount: Int = 0,
    val uncompletedYesterdayTasks: List<TaskItem> = emptyList(),
    val showRolloverSheet: Boolean = false,
    val isSuggestionsExpanded: Boolean = false,
    val isLoading: Boolean = false,
)

@HiltViewModel
@Suppress("TooManyFunctions")
class MyDayViewModel @Inject constructor(
    private val taskService: TaskService,
    private val areaService: AreaService,
    private val projectService: ProjectService,
    private val settingsService: SettingsService,
    private val clock: Clock,
) : ViewModel() {

    private val workspaceId = DeterministicIds.defaultWorkspaceId()

    private val _logicalToday = MutableStateFlow(clock.today())
    val logicalToday: StateFlow<String> = _logicalToday.asStateFlow()

    private val isSuggestionsExpandedState = MutableStateFlow(false)
    private val showRolloverSheetState = MutableStateFlow(false)
    private val uncompletedYesterdayTasksState = MutableStateFlow<List<TaskItem>>(emptyList())

    init {
        viewModelScope.launch {
            val today = settingsService.calculateLogicalToday()
            _logicalToday.value = today
            val yesterdayIncomplete = taskService.getIncompleteYesterdayMyDay(today)
            if (yesterdayIncomplete.isNotEmpty()) {
                uncompletedYesterdayTasksState.value = yesterdayIncomplete
                showRolloverSheetState.value = true
            }
        }
    }

    private data class MyDayTaskData(
        val myDayRaw: List<TaskItem>,
        val suggestionsRaw: List<TaskItem>,
        val areas: List<com.os11.app.service.area.AreaItem>,
        val projects: List<com.os11.app.service.project.ProjectItem>,
    )

    private val areasFlow = areaService.observeAreaItems(workspaceId)
    private val projectsFlow = projectService.observeProjectItems(workspaceId)

    private val taskDataFlow = combine(
        taskService.observeMyDayTasks(_logicalToday.value),
        taskService.observeMyDaySuggestions(_logicalToday.value),
        areasFlow,
        projectsFlow,
    ) { myDayRaw, suggestionsRaw, areas, projects ->
        MyDayTaskData(myDayRaw, suggestionsRaw, areas, projects)
    }

    val uiState: StateFlow<MyDayUiState> = combine(
        taskDataFlow,
        _logicalToday,
        isSuggestionsExpandedState,
        showRolloverSheetState,
        uncompletedYesterdayTasksState,
    ) { data, today, isExpanded, showRollover, uncompletedYesterday ->
        val projectMap = data.projects.associate { it.id to it.name }
        val areaMap = data.areas.associate { it.id to it.name }

        val flattenedTasks = TaskTreeFlattener.flatten(
            tasks = data.myDayRaw,
            todayIso = today,
            projectNames = projectMap,
            areaNames = areaMap,
        )

        val flattenedSuggestions = TaskTreeFlattener.flatten(
            tasks = data.suggestionsRaw,
            todayIso = today,
            projectNames = projectMap,
            areaNames = areaMap,
        )

        val completed = flattenedTasks.count { it.isCompleted }
        val total = flattenedTasks.size

        MyDayUiState(
            logicalToday = today,
            tasks = flattenedTasks.toImmutableList(),
            suggestions = flattenedSuggestions.toImmutableList(),
            completedCount = completed,
            totalCount = total,
            uncompletedYesterdayTasks = uncompletedYesterday,
            showRolloverSheet = showRollover,
            isSuggestionsExpanded = isExpanded,
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = MyDayUiState(isLoading = true),
    )

    fun toggleSuggestionsExpanded() {
        isSuggestionsExpandedState.value = !isSuggestionsExpandedState.value
    }

    fun dismissRollover() {
        showRolloverSheetState.value = false
    }

    fun handleRollover(keepForToday: Boolean) {
        val taskIds = uncompletedYesterdayTasksState.value.map { it.id }
        val targetDay = if (keepForToday) _logicalToday.value else null
        viewModelScope.launch {
            taskService.performRollover(taskIds, targetDay)
            showRolloverSheetState.value = false
        }
    }

    fun addTaskToMyDay(title: String) {
        if (title.isBlank()) return
        viewModelScope.launch {
            val result = taskService.createTask(
                title = title.trim(),
                workspaceId = workspaceId,
            )
            result.onSuccess { task ->
                taskService.addToMyDay(task.id, _logicalToday.value)
            }
        }
    }

    fun addSuggestionToMyDay(taskId: String) {
        viewModelScope.launch {
            taskService.addToMyDay(taskId, _logicalToday.value)
        }
    }

    fun removeFromMyDay(taskId: String) {
        viewModelScope.launch {
            taskService.removeFromMyDay(taskId)
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
