package com.os11.app.ui.features.planned

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.os11.app.service.area.AreaService
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
import java.time.LocalDate
import javax.inject.Inject

data class PlannedUiState(
    val referenceDate: LocalDate = LocalDate.now(),
    val selectedDate: LocalDate? = null,
    val datesWithTasks: Set<String> = emptySet(),
    val overdueTasks: List<TaskUiModel> = emptyList(),
    val todayTasks: List<TaskUiModel> = emptyList(),
    val tomorrowTasks: List<TaskUiModel> = emptyList(),
    val upcomingTasks: List<TaskUiModel> = emptyList(),
    val filteredTasks: List<TaskUiModel> = emptyList(),
    val isLoading: Boolean = false,
)

@HiltViewModel
class PlannedViewModel @Inject constructor(
    private val taskService: TaskService,
    private val areaService: AreaService,
    private val projectService: ProjectService,
    private val clock: Clock,
) : ViewModel() {

    private val workspaceId = DeterministicIds.defaultWorkspaceId()

    private val _referenceDate = MutableStateFlow(LocalDate.now())
    val referenceDate: StateFlow<LocalDate> = _referenceDate.asStateFlow()

    private val _selectedDate = MutableStateFlow<LocalDate?>(null)
    val selectedDate: StateFlow<LocalDate?> = _selectedDate.asStateFlow()

    private val areasFlow = areaService.observeAreaItems(workspaceId)
    private val projectsFlow = projectService.observeProjectItems(workspaceId)
    private val tasksFlow = taskService.observePlannedTasks()

    val uiState: StateFlow<PlannedUiState> = combine(
        tasksFlow,
        areasFlow,
        projectsFlow,
        _referenceDate,
        _selectedDate,
    ) { rawTasks, areas, projects, refDate, selDate ->
        val todayIso = clock.today()
        val today = LocalDate.now()
        val tomorrow = today.plusDays(1)
        val tomorrowIso = tomorrow.toString()

        val projectMap = projects.associate { it.id to it.name }
        val areaMap = areas.associate { it.id to it.name }

        val flattened = TaskTreeFlattener.flatten(
            tasks = rawTasks,
            todayIso = todayIso,
            projectNames = projectMap,
            areaNames = areaMap,
        )

        val datesWithTasks = rawTasks.mapNotNull { it.dueDate }.toSet()

        val overdue = flattened.filter { it.dueDate != null && it.dueDate < todayIso && !it.isCompleted }
        val todayList = flattened.filter { it.dueDate == todayIso }
        val tomorrowList = flattened.filter { it.dueDate == tomorrowIso }
        val upcoming = flattened.filter { it.dueDate != null && it.dueDate > tomorrowIso }

        val filtered = if (selDate != null) {
            val selIso = selDate.toString()
            flattened.filter { it.dueDate == selIso }
        } else {
            emptyList()
        }

        PlannedUiState(
            referenceDate = refDate,
            selectedDate = selDate,
            datesWithTasks = datesWithTasks,
            overdueTasks = overdue.toImmutableList(),
            todayTasks = todayList.toImmutableList(),
            tomorrowTasks = tomorrowList.toImmutableList(),
            upcomingTasks = upcoming.toImmutableList(),
            filteredTasks = filtered.toImmutableList(),
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = PlannedUiState(isLoading = true),
    )

    fun onDateSelected(date: LocalDate?) {
        _selectedDate.value = date
    }

    fun onWeekOffsetChange(weeks: Int) {
        _referenceDate.value = _referenceDate.value.plusWeeks(weeks.toLong())
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

    fun addTaskWithDate(title: String, dueDate: String?) {
        if (title.isBlank()) return
        viewModelScope.launch {
            taskService.createTask(
                title = title.trim(),
                workspaceId = workspaceId,
                dueDate = dueDate,
            )
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
