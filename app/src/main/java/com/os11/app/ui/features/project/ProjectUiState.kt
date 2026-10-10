package com.os11.app.ui.features.project

import com.os11.app.service.project.MilestoneItem
import com.os11.app.service.project.ProjectItem
import com.os11.app.service.project.SectionItem
import com.os11.app.service.project.TaskDependencyItem
import com.os11.app.ui.features.task.TaskUiModel
import java.time.LocalDate

data class ProjectUiState(
    val project: ProjectItem? = null,
    val tasks: List<TaskUiModel> = emptyList(),
    val sections: List<SectionItem> = emptyList(),
    val milestones: List<MilestoneItem> = emptyList(),
    val dependencies: List<TaskDependencyItem> = emptyList(),
    val selectedView: String = "list",
    val selectedDate: LocalDate? = null,
    val referenceDate: LocalDate = LocalDate.now(),
    val datesWithTasks: Set<String> = emptySet(),
    val timelineZoomWeeks: Boolean = false,
    val completedCount: Int = 0,
    val totalCount: Int = 0,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)
