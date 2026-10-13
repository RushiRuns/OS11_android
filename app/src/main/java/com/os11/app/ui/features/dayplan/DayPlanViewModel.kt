package com.os11.app.ui.features.dayplan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.os11.app.service.settings.SettingsService
import com.os11.app.service.task.TaskItem
import com.os11.app.service.task.TaskService
import com.os11.domain.time.Clock
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val MINUTES_PER_HOUR = 60
private const val SNAP_MINUTES = 15
private const val SNAP_ROUNDING_OFFSET = 7
private const val MIN_DURATION_MINUTES = 15
private const val MAX_DURATION_MINUTES = 480
private const val MAX_DAY_MINUTES = 1440
private const val SUBSCRIPTION_TIMEOUT_MS = 5000L

@HiltViewModel
@Suppress("TooManyFunctions")
class DayPlanViewModel @Inject constructor(
    private val taskService: TaskService,
    private val settingsService: SettingsService,
    private val clock: Clock,
) : ViewModel() {

    private val selectedUnscheduledIds = MutableStateFlow<Set<String>>(emptySet())
    private val selectedBlockForAction = MutableStateFlow<DayPlanBlock?>(null)
    private val isSheetExpanded = MutableStateFlow(false)
    private val logicalToday = MutableStateFlow(clock.today())
    private val dayStartMin = MutableStateFlow(DayPlanUiState.DEFAULT_DAY_START_MIN)

    init {
        viewModelScope.launch {
            logicalToday.value = settingsService.calculateLogicalToday()
            val rawStart = settingsService.getDayStartsAt()
            dayStartMin.value = parseTimeToMinutes(rawStart)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val dataFlow = logicalToday.flatMapLatest { today ->
        combine(
            taskService.observeMyDayTasks(today),
            dayStartMin,
            settingsService.observeModuleEnabled("agenda"),
        ) { tasks, startMin, isAgendaOn ->
            Triple(tasks, startMin, isAgendaOn)
        }
    }

    private val controlFlow = combine(
        selectedUnscheduledIds,
        selectedBlockForAction,
        isSheetExpanded,
    ) { selectedIds, actionBlock, sheetExpanded ->
        Triple(selectedIds, actionBlock, sheetExpanded)
    }

    val uiState: StateFlow<DayPlanUiState> = combine(
        dataFlow,
        controlFlow,
    ) { (tasks, startMin, isAgendaOn), (selectedIds, actionBlock, sheetExpanded) ->
        buildUiState(
            today = logicalToday.value,
            startMin = startMin,
            tasks = tasks,
            selectedIds = selectedIds,
            actionBlock = actionBlock,
            sheetExpanded = sheetExpanded,
            isAgendaOn = isAgendaOn,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS),
        initialValue = DayPlanUiState(isLoading = true),
    )

    private fun buildUiState(
        today: String,
        startMin: Int,
        tasks: List<TaskItem>,
        selectedIds: Set<String>,
        actionBlock: DayPlanBlock?,
        sheetExpanded: Boolean,
        isAgendaOn: Boolean,
    ): DayPlanUiState {
        val scheduledRaw = tasks.filter { it.scheduledStartMin != null && it.scheduledDurationMin != null }
            .map { task ->
                DayPlanBlock(
                    task = task,
                    startMin = task.scheduledStartMin ?: 0,
                    durationMin = task.scheduledDurationMin ?: SNAP_MINUTES,
                )
            }
        val layoutBlocks = DayPlanLaneCalculator.computeLanes(scheduledRaw)
        val unscheduled = tasks.filter { it.scheduledStartMin == null || it.scheduledDurationMin == null }

        return DayPlanUiState(
            date = today,
            dayStartMin = startMin,
            scheduledBlocks = layoutBlocks,
            unscheduledTasks = unscheduled,
            selectedUnscheduledIds = selectedIds,
            isAgendaEnabled = isAgendaOn,
            isSheetExpanded = sheetExpanded,
            selectedBlockForAction = actionBlock,
            isLoading = false,
        )
    }

    fun toggleSelectUnscheduled(taskId: String) {
        val current = selectedUnscheduledIds.value
        selectedUnscheduledIds.value = if (taskId in current) current - taskId else current + taskId
    }

    fun selectAllUnscheduled() {
        val allIds = uiState.value.unscheduledTasks.map { it.id }.toSet()
        selectedUnscheduledIds.value = allIds
    }

    fun clearSelectedUnscheduled() {
        selectedUnscheduledIds.value = emptySet()
    }

    fun setSheetExpanded(expanded: Boolean) {
        isSheetExpanded.value = expanded
    }

    fun selectBlockForAction(block: DayPlanBlock?) {
        selectedBlockForAction.value = block
    }

    fun addSelectedToPlan() {
        val selected = selectedUnscheduledIds.value.toList()
        if (selected.isEmpty()) return

        viewModelScope.launch {
            taskService.addTasksToDayPlan(selected, dayStartMin.value)
            selectedUnscheduledIds.value = emptySet()
            isSheetExpanded.value = false
        }
    }

    fun moveBlock(taskId: String, rawNewStartMin: Int) {
        val block = uiState.value.scheduledBlocks.firstOrNull { it.task.id == taskId } ?: return
        val snappedStart = snapToFifteen(rawNewStartMin).coerceIn(0, MAX_DAY_MINUTES - block.durationMin)
        viewModelScope.launch {
            taskService.scheduleTimeBlock(taskId, snappedStart, block.durationMin)
        }
    }

    fun resizeBlock(taskId: String, rawNewDurationMin: Int, rawNewStartMin: Int? = null) {
        val block = uiState.value.scheduledBlocks.firstOrNull { it.task.id == taskId } ?: return
        val snappedDuration = snapToFifteen(rawNewDurationMin)
            .coerceIn(MIN_DURATION_MINUTES, MAX_DURATION_MINUTES)
        val startMin = if (rawNewStartMin != null) {
            snapToFifteen(rawNewStartMin).coerceIn(0, MAX_DAY_MINUTES - snappedDuration)
        } else {
            block.startMin
        }

        viewModelScope.launch {
            taskService.scheduleTimeBlock(taskId, startMin, snappedDuration)
        }
    }

    fun quickAdjustTime(taskId: String, deltaMinutes: Int) {
        val block = uiState.value.scheduledBlocks.firstOrNull { it.task.id == taskId } ?: return
        val newStart = (block.startMin + deltaMinutes).coerceIn(0, MAX_DAY_MINUTES - block.durationMin)
        viewModelScope.launch {
            taskService.scheduleTimeBlock(taskId, newStart, block.durationMin)
        }
    }

    fun quickAdjustDuration(taskId: String, deltaMinutes: Int) {
        val block = uiState.value.scheduledBlocks.firstOrNull { it.task.id == taskId } ?: return
        val newDuration = (block.durationMin + deltaMinutes)
            .coerceIn(MIN_DURATION_MINUTES, MAX_DURATION_MINUTES)
        viewModelScope.launch {
            taskService.scheduleTimeBlock(taskId, block.startMin, newDuration)
        }
    }

    fun removeFromPlan(taskId: String) {
        viewModelScope.launch {
            taskService.removeScheduledTimeBlock(taskId)
            if (selectedBlockForAction.value?.task?.id == taskId) {
                selectedBlockForAction.value = null
            }
        }
    }

    private fun snapToFifteen(minutes: Int): Int = ((minutes + SNAP_ROUNDING_OFFSET) / SNAP_MINUTES) * SNAP_MINUTES

    private fun parseTimeToMinutes(timeStr: String): Int {
        val parts = timeStr.split(":")
        if (parts.size >= 2) {
            val h = parts[0].toIntOrNull() ?: 0
            val m = parts[1].toIntOrNull() ?: 0
            return (h * MINUTES_PER_HOUR + m).coerceIn(0, MAX_DAY_MINUTES - 1)
        }
        return DayPlanUiState.DEFAULT_DAY_START_MIN
    }
}
