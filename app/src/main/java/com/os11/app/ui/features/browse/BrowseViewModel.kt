package com.os11.app.ui.features.browse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.os11.app.service.area.AreaItem
import com.os11.app.service.area.AreaService
import com.os11.app.service.project.ProjectItem
import com.os11.app.service.project.ProjectService
import com.os11.app.service.settings.SettingsService
import com.os11.app.service.tag.TagItem
import com.os11.app.service.tag.TagService
import com.os11.app.service.task.BrowseCountsItem
import com.os11.app.service.task.TaskItem
import com.os11.app.service.task.TaskService
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

data class AreaWithProjects(val area: AreaItem, val projects: List<ProjectItem>, val taskCount: Int)

data class BrowseUiState(
    val counts: BrowseCountsItem = BrowseCountsItem(),
    val areasWithProjects: List<AreaWithProjects> = emptyList(),
    val looseProjects: List<ProjectItem> = emptyList(),
    val tags: List<TagItem> = emptyList(),
    val projectTaskCounts: Map<String, Int> = emptyMap(),
    val enabledModules: Set<String> = emptySet(),
    val showCreateAreaDialog: Boolean = false,
    val showCreateProjectDialog: Boolean = false,
    val showCreateTagDialog: Boolean = false,
    val isLoading: Boolean = false,
)

@HiltViewModel
class BrowseViewModel @Inject constructor(
    private val taskService: TaskService,
    private val areaService: AreaService,
    private val projectService: ProjectService,
    private val tagService: TagService,
    private val settingsService: SettingsService,
    private val clock: Clock,
) : ViewModel() {

    private val workspaceId = DeterministicIds.defaultWorkspaceId()

    private val _showCreateAreaDialog = MutableStateFlow(false)
    val showCreateAreaDialog: StateFlow<Boolean> = _showCreateAreaDialog.asStateFlow()

    private val _showCreateProjectDialog = MutableStateFlow(false)
    val showCreateProjectDialog: StateFlow<Boolean> = _showCreateProjectDialog.asStateFlow()

    private val _showCreateTagDialog = MutableStateFlow(false)
    val showCreateTagDialog: StateFlow<Boolean> = _showCreateTagDialog.asStateFlow()

    private data class BrowseData(
        val counts: BrowseCountsItem,
        val areas: List<AreaItem>,
        val projects: List<ProjectItem>,
        val allTasks: List<TaskItem>,
        val tags: List<TagItem>,
        val enabledModules: Set<String>,
    )

    private val areasFlow = areaService.observeAreaItems(workspaceId)
    private val projectsFlow = projectService.observeProjectItems(workspaceId)
    private val allTasksFlow = taskService.observeAllTasks()
    private val countsFlow = taskService.observeBrowseCounts(clock.today())
    private val tagsFlow = tagService.observeTagItems()
    private val enabledModulesFlow = settingsService.observeEnabledModuleNames()

    private val browseDataFlow = combine(
        combine(countsFlow, areasFlow, projectsFlow) { counts, areas, projects -> Triple(counts, areas, projects) },
        combine(allTasksFlow, tagsFlow, enabledModulesFlow) { tasks, tags, modules -> Triple(tasks, tags, modules) },
    ) { first, second ->
        BrowseData(
            counts = first.first,
            areas = first.second,
            projects = first.third,
            allTasks = second.first,
            tags = second.second,
            enabledModules = second.third,
        )
    }

    val uiState: StateFlow<BrowseUiState> = combine(
        browseDataFlow,
        _showCreateAreaDialog,
        _showCreateProjectDialog,
        _showCreateTagDialog,
    ) { data, showAreaDlg, showProjDlg, showTagDlg ->
        val activeTasks = data.allTasks.filter { !it.isCompleted && !it.isTrashed }

        val areaTaskCounts = mutableMapOf<String, Int>()
        val projectTaskCounts = mutableMapOf<String, Int>()

        for (task in activeTasks) {
            task.areaId?.let { areaId ->
                areaTaskCounts[areaId] = (areaTaskCounts[areaId] ?: 0) + 1
            }
            task.projectId?.let { projectId ->
                projectTaskCounts[projectId] = (projectTaskCounts[projectId] ?: 0) + 1
            }
        }

        val projectsByArea = data.projects.groupBy { it.areaId }

        val areasWithProjects = data.areas.map { area ->
            AreaWithProjects(
                area = area,
                projects = projectsByArea[area.id] ?: emptyList(),
                taskCount = areaTaskCounts[area.id] ?: 0,
            )
        }

        val looseProjects = projectsByArea[null] ?: emptyList()

        BrowseUiState(
            counts = data.counts,
            areasWithProjects = areasWithProjects.toImmutableList(),
            looseProjects = looseProjects.toImmutableList(),
            tags = data.tags.toImmutableList(),
            projectTaskCounts = projectTaskCounts,
            enabledModules = data.enabledModules,
            showCreateAreaDialog = showAreaDlg,
            showCreateProjectDialog = showProjDlg,
            showCreateTagDialog = showTagDlg,
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = BrowseUiState(isLoading = true),
    )

    fun openCreateAreaDialog() {
        _showCreateAreaDialog.value = true
    }

    fun closeCreateAreaDialog() {
        _showCreateAreaDialog.value = false
    }

    fun openCreateProjectDialog() {
        _showCreateProjectDialog.value = true
    }

    fun closeCreateProjectDialog() {
        _showCreateProjectDialog.value = false
    }

    fun openCreateTagDialog() {
        _showCreateTagDialog.value = true
    }

    fun closeCreateTagDialog() {
        _showCreateTagDialog.value = false
    }

    fun createArea(name: String, color: String? = null) {
        if (name.isBlank()) return
        viewModelScope.launch {
            areaService.createArea(
                name = name.trim(),
                workspaceId = workspaceId,
                color = color,
            )
            _showCreateAreaDialog.value = false
        }
    }

    fun createProject(name: String, areaId: String? = null, color: String? = null) {
        if (name.isBlank()) return
        viewModelScope.launch {
            projectService.createProject(
                name = name.trim(),
                workspaceId = workspaceId,
                areaId = areaId,
                color = color,
            )
            _showCreateProjectDialog.value = false
        }
    }

    fun createTag(name: String, color: String? = null, parentTagId: String? = null) {
        if (name.isBlank()) return
        viewModelScope.launch {
            tagService.getOrCreateTag(
                name = name.trim(),
                color = color,
                parentTagId = parentTagId,
            )
            _showCreateTagDialog.value = false
        }
    }
}
