package com.os11.app.ui.features.tags

import com.os11.app.service.tag.TagItem
import com.os11.app.ui.features.task.TaskUiModel

data class TagDetailUiState(
    val tag: TagItem? = null,
    val tasks: List<TaskUiModel> = emptyList(),
    val completedCount: Int = 0,
    val totalCount: Int = 0,
    val isLoading: Boolean = false,
)
