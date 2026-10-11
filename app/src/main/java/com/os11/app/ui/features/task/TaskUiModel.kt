package com.os11.app.ui.features.task

import androidx.compose.runtime.Immutable
import com.os11.app.ui.components.OSTagShape
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
data class TagDotUiModel(
    val id: String,
    val name: String,
    val colorHex: String?,
    val shape: OSTagShape = OSTagShape.fromTag(name, colorHex),
)

@Immutable
data class TaskUiModel(
    val id: String,
    val title: String,
    val isCompleted: Boolean = false,
    val isStarred: Boolean = false,
    val priority: Int = 0,
    val dueDate: String? = null,
    val dueTime: String? = null,
    val formattedDueDate: String? = null,
    val isOverdue: Boolean = false,
    val projectName: String? = null,
    val areaName: String? = null,
    val subtaskProgress: String? = null,
    val hasNotes: Boolean = false,
    val tagDots: ImmutableList<TagDotUiModel> = persistentListOf(),
    val indentDepth: Int = 0,
    val parentTaskId: String? = null,
    val sectionId: String? = null,
    val workspaceId: String = "",
    val sortOrder: Double = 0.0,
    val recurrenceRule: String? = null,
    val bucket: String? = null,
    val waitingOn: String? = null,
    val followUpDate: String? = null,
    val estimatedMinutes: Int? = null,
    val scheduledStartMin: Int? = null,
    val scheduledDurationMin: Int? = null,
) {
    val hasSecondLine: Boolean
        get() = formattedDueDate != null ||
            projectName != null ||
            areaName != null ||
            subtaskProgress != null ||
            hasNotes ||
            tagDots.isNotEmpty()
}

enum class TaskSortOption {
    Manual,
    DueDate,
    Priority,
    Title,
    CreatedAt,
}

enum class TaskFilterOption {
    All,
    ActiveOnly,
    CompletedOnly,
    HighPriorityOnly,
    HasDueDateOnly,
}
