package com.os11.app.ui.features.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.os11.app.service.goal.GoalItem
import com.os11.app.service.goal.GoalService
import com.os11.app.service.project.ProjectItem
import com.os11.app.service.project.ProjectService
import com.os11.app.service.settings.SettingsService
import com.os11.app.service.task.TaskItem
import com.os11.app.service.task.TaskService
import com.os11.domain.id.DeterministicIds
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val SUBSCRIPTION_TIMEOUT_MS = 5000L

data class GoalsUiState(
    val goals: List<GoalItem> = emptyList(),
    val isGoalsEnabled: Boolean = true,
    val selectedGoalForDetail: GoalItem? = null,
    val isCreateSheetOpen: Boolean = false,
    val availableTasks: List<TaskItem> = emptyList(),
    val availableProjects: List<ProjectItem> = emptyList(),
    val isLoading: Boolean = false,
)

@HiltViewModel
@Suppress("TooManyFunctions")
class GoalsViewModel @Inject constructor(
    private val goalService: GoalService,
    private val taskService: TaskService,
    private val projectService: ProjectService,
    private val settingsService: SettingsService,
) : ViewModel() {

    private val workspaceId = DeterministicIds.defaultWorkspaceId()
    private val selectedGoalId = MutableStateFlow<String?>(null)
    private val isCreateSheetOpen = MutableStateFlow(false)

    private data class GoalsData(
        val goals: List<GoalItem>,
        val isEnabled: Boolean,
        val tasks: List<TaskItem>,
        val projects: List<ProjectItem>,
    )

    private val dataFlow = combine(
        goalService.observeGoalItems(),
        settingsService.observeModuleEnabled("goals"),
        taskService.observeAllTasks(),
        projectService.observeProjectItems(workspaceId),
    ) { goals, isEnabled, tasks, projects ->
        GoalsData(goals, isEnabled, tasks, projects)
    }

    val uiState: StateFlow<GoalsUiState> = combine(
        dataFlow,
        selectedGoalId,
        isCreateSheetOpen,
    ) { data, selId, createOpen ->
        val selectedGoal = data.goals.firstOrNull { it.id == selId }
        GoalsUiState(
            goals = data.goals,
            isGoalsEnabled = data.isEnabled,
            selectedGoalForDetail = selectedGoal,
            isCreateSheetOpen = createOpen,
            availableTasks = data.tasks.filter { !it.isCompleted },
            availableProjects = data.projects,
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS),
        initialValue = GoalsUiState(isLoading = true),
    )

    fun openCreateSheet() {
        isCreateSheetOpen.value = true
    }

    fun closeCreateSheet() {
        isCreateSheetOpen.value = false
    }

    fun selectGoal(goal: GoalItem?) {
        selectedGoalId.value = goal?.id
    }

    fun createGoal(
        title: String,
        description: String = "",
        goalType: String = GoalService.DEFAULT_GOAL_TYPE,
        targetDate: String? = null,
        targetValue: Double = GoalItem.DEFAULT_TARGET_VALUE,
    ) {
        viewModelScope.launch {
            goalService.createGoal(
                title = title,
                description = description,
                goalType = goalType,
                targetDate = targetDate,
                targetValue = targetValue,
            )
            isCreateSheetOpen.value = false
        }
    }

    fun updateGoal(
        id: String,
        title: String,
        description: String? = null,
        targetDate: String? = null,
        targetValue: Double? = null,
        currentValue: Double? = null,
    ) {
        viewModelScope.launch {
            goalService.updateGoal(
                id = id,
                title = title,
                description = description,
                targetDate = targetDate,
                targetValue = targetValue,
                currentValue = currentValue,
            )
        }
    }

    fun recordProgress(goalId: String, incrementValue: Double = GoalService.DEFAULT_INCREMENT) {
        viewModelScope.launch {
            goalService.recordProgress(goalId, incrementValue)
        }
    }

    fun linkTask(goalId: String, taskId: String) {
        viewModelScope.launch {
            goalService.linkTask(goalId, taskId)
        }
    }

    fun unlinkTask(goalId: String, taskId: String) {
        viewModelScope.launch {
            goalService.unlinkTask(goalId, taskId)
        }
    }

    fun linkProject(goalId: String, projectId: String) {
        viewModelScope.launch {
            goalService.linkProject(goalId, projectId)
        }
    }

    fun unlinkProject(goalId: String, projectId: String) {
        viewModelScope.launch {
            goalService.unlinkProject(goalId, projectId)
        }
    }

    fun deleteGoal(goalId: String) {
        viewModelScope.launch {
            goalService.deleteGoal(goalId)
            if (selectedGoalId.value == goalId) {
                selectedGoalId.value = null
            }
        }
    }

    fun toggleTaskCompleted(taskId: String, completed: Boolean) {
        viewModelScope.launch {
            taskService.setCompleted(taskId, completed)
        }
    }
}
