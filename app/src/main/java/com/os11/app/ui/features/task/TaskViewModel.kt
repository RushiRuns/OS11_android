package com.os11.app.ui.features.task

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.os11.app.service.tag.TagItem
import com.os11.app.service.tag.TagService
import com.os11.app.service.task.TaskItem
import com.os11.app.service.task.TaskService
import com.os11.domain.result.AppResult
import com.os11.domain.time.Clock
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel orchestrating core task management for Inbox and task views.
 * Strictly adheres to clean architecture: depends only on application services.
 */
@HiltViewModel
@Suppress("TooManyFunctions")
class TaskViewModel @Inject constructor(
    private val taskService: TaskService,
    private val tagService: TagService,
    private val clock: Clock,
) : ViewModel() {

    private val _sortOption = MutableStateFlow(TaskSortOption.Manual)
    val sortOption: StateFlow<TaskSortOption> = _sortOption.asStateFlow()

    private val _filterOption = MutableStateFlow(TaskFilterOption.All)
    val filterOption: StateFlow<TaskFilterOption> = _filterOption.asStateFlow()

    private val _selectedDetailTask = MutableStateFlow<TaskItem?>(null)
    val selectedDetailTask: StateFlow<TaskItem?> = _selectedDetailTask.asStateFlow()

    private val _detailSubtasks = MutableStateFlow<ImmutableList<TaskUiModel>>(persistentListOf())
    val detailSubtasks: StateFlow<ImmutableList<TaskUiModel>> = _detailSubtasks.asStateFlow()

    private val _detailTags = MutableStateFlow<ImmutableList<TagItem>>(persistentListOf())
    val detailTags: StateFlow<ImmutableList<TagItem>> = _detailTags.asStateFlow()

    val allTags: StateFlow<ImmutableList<TagItem>> = tagService.observeTagItems()
        .map { it.toImmutableList() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = persistentListOf(),
        )

    private var detailTagsJob: Job? = null

    private val _selectedActionsTask = MutableStateFlow<TaskUiModel?>(null)
    val selectedActionsTask: StateFlow<TaskUiModel?> = _selectedActionsTask.asStateFlow()

    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode: StateFlow<Boolean> = _isSelectionMode.asStateFlow()

    private val _selectedTaskIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedTaskIds: StateFlow<Set<String>> = _selectedTaskIds.asStateFlow()

    val inboxTasks: StateFlow<ImmutableList<TaskUiModel>> = combine(
        taskService.observeInboxTasks(),
        _sortOption,
        _filterOption,
    ) { rawTasks, sort, filter ->
        val filtered = filterTasks(rawTasks, filter)
        val sorted = sortTasks(filtered, sort)
        TaskTreeFlattener.flatten(
            tasks = sorted,
            todayIso = clock.today(),
        ).toImmutableList()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = persistentListOf(),
    )

    fun setSort(sort: TaskSortOption) {
        _sortOption.value = sort
    }

    fun setFilter(filter: TaskFilterOption) {
        _filterOption.value = filter
    }

    fun createTask(title: String, workspaceId: String = "default") {
        if (title.isBlank()) return
        viewModelScope.launch {
            taskService.createTask(
                title = title.trim(),
                workspaceId = workspaceId,
            )
        }
    }

    fun toggleCompleted(taskId: String, completed: Boolean) {
        viewModelScope.launch {
            taskService.setCompleted(taskId, completed)
        }
    }

    fun toggleStarred(taskId: String, current: Boolean) {
        viewModelScope.launch {
            taskService.setStarred(taskId, !current)
        }
    }

    fun openDetail(taskId: String) {
        viewModelScope.launch {
            val item = taskService.getTaskItemById(taskId)
            _selectedDetailTask.value = item
            loadSubtasksForDetail(taskId)
            loadTagsForDetail(taskId)
        }
    }

    fun closeDetail() {
        detailTagsJob?.cancel()
        detailTagsJob = null
        _selectedDetailTask.value = null
        _detailSubtasks.value = persistentListOf()
        _detailTags.value = persistentListOf()
    }

    private fun loadTagsForDetail(taskId: String) {
        detailTagsJob?.cancel()
        detailTagsJob = viewModelScope.launch {
            tagService.observeTagsForTask(taskId).collect { entities ->
                _detailTags.value = entities.map { entity ->
                    TagItem(
                        id = entity.id,
                        name = entity.name,
                        color = entity.color,
                        parentTagId = entity.parentTagId,
                        sortOrder = entity.sortOrder,
                    )
                }.toImmutableList()
            }
        }
    }

    private fun loadSubtasksForDetail(parentId: String) {
        viewModelScope.launch {
            taskService.observeSubtasks(parentId).collect { subtasks ->
                val flattened = TaskTreeFlattener.flatten(
                    tasks = subtasks,
                    todayIso = clock.today(),
                )
                _detailSubtasks.value = flattened.toImmutableList()
            }
        }
    }

    fun updateTitle(taskId: String, title: String) {
        viewModelScope.launch {
            taskService.editTask(id = taskId, title = title)
            val current = _selectedDetailTask.value
            if (current != null && current.id == taskId) {
                _selectedDetailTask.value = current.copy(title = title)
            }
        }
    }

    fun updateNotes(taskId: String, notes: String) {
        viewModelScope.launch {
            taskService.editTask(id = taskId, notes = notes)
            val current = _selectedDetailTask.value
            if (current != null && current.id == taskId) {
                _selectedDetailTask.value = current.copy(notes = notes)
            }
        }
    }

    fun updateDueDate(taskId: String, dueDate: String?) {
        viewModelScope.launch {
            taskService.updateDueDate(id = taskId, dueDate = dueDate)
            val current = _selectedDetailTask.value
            if (current != null && current.id == taskId) {
                _selectedDetailTask.value = current.copy(dueDate = dueDate)
            }
        }
    }

    fun updatePriority(taskId: String, priority: Int) {
        viewModelScope.launch {
            taskService.editTask(id = taskId, priority = priority)
            val current = _selectedDetailTask.value
            if (current != null && current.id == taskId) {
                _selectedDetailTask.value = current.copy(priority = priority)
            }
        }
    }

    fun addSubtask(parentId: String, title: String) {
        viewModelScope.launch {
            taskService.addSubtask(parentId, title)
            loadSubtasksForDetail(parentId)
        }
    }

    fun openActions(task: TaskUiModel) {
        _selectedActionsTask.value = task
    }

    fun closeActions() {
        _selectedActionsTask.value = null
    }

    fun duplicateTask(taskId: String) {
        viewModelScope.launch {
            taskService.duplicateTask(taskId)
        }
    }

    fun indentSubtask(taskId: String, newParentId: String) {
        viewModelScope.launch {
            taskService.indentSubtask(taskId, newParentId)
        }
    }

    fun outdentSubtask(taskId: String) {
        viewModelScope.launch {
            taskService.outdentSubtask(taskId)
        }
    }

    fun promoteSubtask(taskId: String) {
        viewModelScope.launch {
            taskService.promoteSubtask(taskId)
        }
    }

    fun trashTask(taskId: String) {
        viewModelScope.launch {
            taskService.trashTask(taskId)
            if (_selectedDetailTask.value?.id == taskId) {
                closeDetail()
            }
        }
    }

    fun enterSelectionMode(initialTaskId: String? = null) {
        _isSelectionMode.value = true
        _selectedTaskIds.value = if (initialTaskId != null) setOf(initialTaskId) else emptySet()
    }

    fun toggleSelect(taskId: String) {
        val current = _selectedTaskIds.value
        val updated = if (current.contains(taskId)) current - taskId else current + taskId
        _selectedTaskIds.value = updated
        if (updated.isNotEmpty()) {
            _isSelectionMode.value = true
        }
    }

    fun selectAll(allIds: List<String>) {
        _selectedTaskIds.value = allIds.toSet()
        _isSelectionMode.value = true
    }

    fun toggleSelectAll(allIds: List<String>) {
        if (_selectedTaskIds.value.size >= allIds.size) {
            _selectedTaskIds.value = emptySet()
        } else {
            _selectedTaskIds.value = allIds.toSet()
            _isSelectionMode.value = true
        }
    }

    fun clearSelection() {
        _selectedTaskIds.value = emptySet()
        _isSelectionMode.value = false
    }

    fun bulkComplete(completed: Boolean) {
        val ids = _selectedTaskIds.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            taskService.bulkComplete(ids, completed)
            clearSelection()
        }
    }

    fun bulkTrash() {
        val ids = _selectedTaskIds.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            taskService.bulkTrash(ids)
            clearSelection()
        }
    }

    fun bulkMove(areaId: String?, projectId: String?, sectionId: String? = null) {
        val ids = _selectedTaskIds.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            taskService.bulkMove(ids, areaId, projectId, sectionId)
            clearSelection()
        }
    }

    fun bulkSetDueDate(dueDate: String?, dueTime: String? = null) {
        val ids = _selectedTaskIds.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            taskService.bulkSetDueDate(ids, dueDate, dueTime)
            clearSelection()
        }
    }

    fun bulkSetPriority(priority: Int) {
        val ids = _selectedTaskIds.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            taskService.bulkSetPriority(ids, priority)
            clearSelection()
        }
    }

    fun addTagToTask(taskId: String, tagId: String) {
        viewModelScope.launch {
            tagService.addTagToTask(taskId, tagId)
        }
    }

    fun removeTagFromTask(taskId: String, tagId: String) {
        viewModelScope.launch {
            tagService.removeTagFromTask(taskId, tagId)
        }
    }

    fun createTagAndAddToTask(taskId: String, name: String, parentId: String?) {
        viewModelScope.launch {
            val res = tagService.getOrCreateTag(name, parentTagId = parentId)
            if (res is AppResult.Ok) {
                tagService.addTagToTask(taskId, res.value.id)
            }
        }
    }

    fun bulkAddTag(tagId: String) {
        val ids = _selectedTaskIds.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            tagService.bulkAddTag(ids, tagId)
            clearSelection()
        }
    }

    fun reorderTask(taskId: String, newSortOrder: Double) {
        viewModelScope.launch {
            taskService.reorderTask(taskId, newSortOrder)
        }
    }

    private fun filterTasks(tasks: List<TaskItem>, filter: TaskFilterOption): List<TaskItem> = when (filter) {
        TaskFilterOption.All -> tasks
        TaskFilterOption.ActiveOnly -> tasks.filter { !it.isCompleted }
        TaskFilterOption.CompletedOnly -> tasks.filter { it.isCompleted }
        TaskFilterOption.HighPriorityOnly -> tasks.filter { it.priority >= 3 }
        TaskFilterOption.HasDueDateOnly -> tasks.filter { !it.dueDate.isNullOrBlank() }
    }

    private fun sortTasks(tasks: List<TaskItem>, sort: TaskSortOption): List<TaskItem> = when (sort) {
        TaskSortOption.Manual -> tasks.sortedBy { it.sortOrder }
        TaskSortOption.DueDate -> tasks.sortedWith(
            compareBy<TaskItem> { it.dueDate == null }.thenBy { it.dueDate },
        )
        TaskSortOption.Priority -> tasks.sortedByDescending { it.priority }
        TaskSortOption.Title -> tasks.sortedBy { it.title.lowercase() }
        TaskSortOption.CreatedAt -> tasks.sortedByDescending { it.createdAt }
    }
}
