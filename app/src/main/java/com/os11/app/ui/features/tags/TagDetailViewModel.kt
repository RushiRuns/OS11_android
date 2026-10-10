package com.os11.app.ui.features.tags

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.os11.app.service.tag.TagItem
import com.os11.app.service.tag.TagService
import com.os11.app.service.task.TaskService
import com.os11.app.ui.features.task.TaskTreeFlattener
import com.os11.domain.result.AppResult
import com.os11.domain.time.Clock
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TagDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val tagService: TagService,
    private val taskService: TaskService,
    private val clock: Clock,
) : ViewModel() {

    val tagId: String = checkNotNull(savedStateHandle["tagId"])

    private val allTagsFlow = tagService.observeTagItems()
    private val tagTasksFlow = tagService.observeTasksByTag(tagId)

    val uiState: StateFlow<TagDetailUiState> = combine(
        allTagsFlow,
        tagTasksFlow,
    ) { allTags, tasks ->
        val currentTag = allTags.find { it.id == tagId } ?: TagItem(id = tagId, name = "tag")
        val todayIso = clock.today()
        val flattened = TaskTreeFlattener.flatten(
            tasks = tasks,
            todayIso = todayIso,
        )
        val completed = tasks.count { it.isCompleted }

        TagDetailUiState(
            tag = currentTag,
            tasks = flattened,
            completedCount = completed,
            totalCount = tasks.size,
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TagDetailUiState(isLoading = true),
    )

    fun setTaskCompleted(taskId: String, isCompleted: Boolean) {
        viewModelScope.launch {
            taskService.setCompleted(taskId, isCompleted)
        }
    }

    fun addTask(title: String) {
        if (title.isBlank()) return
        viewModelScope.launch {
            val result = taskService.createTask(title = title.trim(), workspaceId = "default")
            if (result is AppResult.Ok) {
                tagService.addTagToTask(result.value.id, tagId)
            }
        }
    }

    fun removeTagFromTask(taskId: String) {
        viewModelScope.launch {
            tagService.removeTagFromTask(taskId, tagId)
        }
    }
}
