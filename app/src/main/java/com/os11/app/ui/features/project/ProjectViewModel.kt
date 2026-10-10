package com.os11.app.ui.features.project

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.os11.app.service.area.AreaService
import com.os11.app.service.project.ProjectItem
import com.os11.app.service.project.ProjectService
import com.os11.app.service.task.TaskService
import com.os11.app.ui.features.task.TaskTreeFlattener
import com.os11.domain.id.DeterministicIds
import com.os11.domain.result.AppError
import com.os11.domain.result.onFailure
import com.os11.domain.result.onSuccess
import com.os11.domain.time.Clock
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
@Suppress("TooManyFunctions")
class ProjectViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val projectService: ProjectService,
    private val areaService: AreaService,
    private val taskService: TaskService,
    private val clock: Clock,
) : ViewModel() {

    val projectId: String = checkNotNull(savedStateHandle["projectId"])
    private val workspaceId = DeterministicIds.defaultWorkspaceId()

    private val userSelectedView = MutableStateFlow<String?>(null)
    private val selectedDate = MutableStateFlow<LocalDate?>(null)
    private val referenceDate = MutableStateFlow(LocalDate.now())
    private val timelineZoomWeeks = MutableStateFlow(false)
    private val errorMessage = MutableStateFlow<String?>(null)

    private val projectFlow: StateFlow<ProjectItem?> = projectService.observeProjectItems(workspaceId)
        .map { projects -> projects.find { it.id == projectId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), null)

    private val projectTasksFlow = taskService.observeTasksByProject(projectId)
    private val sectionsFlow = projectService.observeSections(projectId)
    private val milestonesFlow = projectService.observeMilestones(projectId)
    private val dependenciesFlow = projectService.observeDependencies()
    private val areasFlow = areaService.observeAreaItems(workspaceId)

    @Suppress("LongParameterList")
    val uiState: StateFlow<ProjectUiState> = combine(
        combine(projectFlow, projectTasksFlow, sectionsFlow) { project, tasks, sections ->
            Triple(project, tasks, sections)
        },
        combine(milestonesFlow, dependenciesFlow, areasFlow) { milestones, dependencies, areas ->
            Triple(milestones, dependencies, areas)
        },
        combine(
            userSelectedView,
            selectedDate,
            referenceDate,
            timelineZoomWeeks,
            errorMessage,
        ) { view, date, refDate, zoom, error ->
            UiControls(view, date, refDate, zoom, error)
        },
    ) { (project, tasks, sections), (milestones, dependencies, areas), controls ->
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
        val effectiveView = controls.view ?: project?.defaultView ?: "list"

        val datesWithTasks = flattened.mapNotNull { it.dueDate }.toSet()

        ProjectUiState(
            project = project,
            tasks = flattened.toImmutableList(),
            sections = sections.toImmutableList(),
            milestones = milestones.toImmutableList(),
            dependencies = dependencies.toImmutableList(),
            selectedView = effectiveView,
            selectedDate = controls.selectedDate,
            referenceDate = controls.referenceDate,
            datesWithTasks = datesWithTasks,
            timelineZoomWeeks = controls.timelineZoomWeeks,
            completedCount = completed,
            totalCount = total,
            isLoading = false,
            errorMessage = controls.errorMessage,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = ProjectUiState(isLoading = true),
    )

    fun selectView(view: String) {
        userSelectedView.value = view
        viewModelScope.launch {
            projectService.updateDefaultView(projectId, view)
        }
    }

    fun addTaskToProject(title: String, sectionId: String? = null) {
        if (title.isBlank()) return
        viewModelScope.launch {
            taskService.createTask(
                title = title.trim(),
                workspaceId = workspaceId,
                projectId = projectId,
                areaId = projectFlow.value?.areaId,
                sectionId = sectionId,
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

    fun moveTaskToSection(taskId: String, sectionId: String?) {
        viewModelScope.launch {
            taskService.moveTaskToSection(taskId, sectionId)
        }
    }

    // --- Sections ---

    fun createSection(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            projectService.createSection(projectId, name.trim())
        }
    }

    fun renameSection(sectionId: String, newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch {
            projectService.renameSection(sectionId, newName.trim())
        }
    }

    fun toggleSectionCollapse(sectionId: String, isCollapsed: Boolean) {
        viewModelScope.launch {
            projectService.toggleSectionCollapse(sectionId, isCollapsed)
        }
    }

    fun reorderSections(sectionIds: List<String>) {
        viewModelScope.launch {
            projectService.reorderSections(projectId, sectionIds)
        }
    }

    fun deleteSection(sectionId: String) {
        viewModelScope.launch {
            projectService.deleteSection(sectionId)
        }
    }

    // --- Milestones ---

    fun createMilestone(title: String, dueDate: String) {
        if (title.isBlank() || dueDate.isBlank()) return
        viewModelScope.launch {
            projectService.createMilestone(projectId, title.trim(), dueDate.trim())
        }
    }

    fun toggleMilestoneCompleted(milestoneId: String, isCompleted: Boolean) {
        viewModelScope.launch {
            projectService.toggleMilestoneCompleted(milestoneId, isCompleted)
        }
    }

    fun deleteMilestone(milestoneId: String) {
        viewModelScope.launch {
            projectService.deleteMilestone(milestoneId)
        }
    }

    // --- Dependencies ---

    fun addDependency(taskId: String, dependsOnId: String) {
        viewModelScope.launch {
            val result = projectService.addDependency(taskId, dependsOnId)
            result.onFailure { error ->
                val message = if (error is AppError.Validation) {
                    error.reason
                } else {
                    error.userMessage()
                }
                errorMessage.value = message
            }
        }
    }

    fun removeDependency(taskId: String, dependsOnId: String) {
        viewModelScope.launch {
            projectService.removeDependency(taskId, dependsOnId)
        }
    }

    fun clearErrorMessage() {
        errorMessage.value = null
    }

    // --- Status and Export ---

    fun updateProjectStatus(status: String) {
        viewModelScope.launch {
            projectService.updateProjectStatus(projectId, status)
        }
    }

    fun archiveProject() {
        viewModelScope.launch {
            projectService.archiveProject(projectId)
        }
    }

    fun unarchiveProject() {
        viewModelScope.launch {
            projectService.unarchiveProject(projectId)
        }
    }

    fun exportProjectToCsv(onResult: (String) -> Unit) {
        viewModelScope.launch {
            val result = projectService.exportProjectToCsv(projectId)
            result.onSuccess { csv -> onResult(csv) }
        }
    }

    // --- Calendar & Timeline Controls ---

    fun onDateSelected(date: LocalDate?) {
        selectedDate.value = date
    }

    fun onWeekOffsetChange(offsetWeeks: Int) {
        referenceDate.value = referenceDate.value.plusWeeks(offsetWeeks.toLong())
    }

    fun toggleTimelineZoom() {
        timelineZoomWeeks.value = !timelineZoomWeeks.value
    }

    private data class UiControls(
        val view: String?,
        val selectedDate: LocalDate?,
        val referenceDate: LocalDate,
        val timelineZoomWeeks: Boolean,
        val errorMessage: String?,
    )
}
