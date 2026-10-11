package com.os11.app.service.backup

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OS11BackupJson(
    val version: Int = 1,
    @SerialName("exported_at") val exportedAt: String = "",
    val source: String = "os11",
    val workspaces: List<WorkspaceExportDto> = emptyList(),
    val areas: List<AreaExportDto> = emptyList(),
    val projects: List<ProjectExportDto> = emptyList(),
    val sections: List<SectionExportDto> = emptyList(),
    val tasks: List<TaskExportDto> = emptyList(),
    val tags: List<TagExportDto> = emptyList(),
    @SerialName("task_tags") val taskTags: List<TaskTagExportDto> = emptyList(),
    @SerialName("task_dependencies") val taskDependencies: List<TaskDependencyExportDto> = emptyList(),
    val milestones: List<MilestoneExportDto> = emptyList(),
    val reminders: List<ReminderExportDto> = emptyList(),
    @SerialName("pomodoro_sessions") val pomodoroSessions: List<PomodoroSessionExportDto> = emptyList(),
    val goals: List<GoalExportDto> = emptyList(),
    @SerialName("goal_links") val goalLinks: List<GoalLinkExportDto> = emptyList(),
    val settings: Map<String, String> = emptyMap(),

    // Legacy Desktop OS11 schema compatibility
    @SerialName("list_groups") val legacyListGroups: List<AreaExportDto> = emptyList(),
    @SerialName("lists") val legacyLists: List<LegacyListExportDto> = emptyList(),
)

@Serializable
data class WorkspaceExportDto(
    val id: String,
    val name: String,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)

@Serializable
data class AreaExportDto(
    val id: String,
    val name: String,
    @SerialName("workspace_id") val workspaceId: String? = null,
    @SerialName("sort_order") val sortOrder: Double = 0.0,
    @SerialName("is_default") val isDefault: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)

@Serializable
data class ProjectExportDto(
    val id: String,
    val name: String,
    @SerialName("workspace_id") val workspaceId: String? = null,
    @SerialName("area_id") val areaId: String? = null,
    val status: String = "active",
    @SerialName("sort_order") val sortOrder: Double = 0.0,
    @SerialName("default_view") val defaultView: String = "list",
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)

@Serializable
data class LegacyListExportDto(
    val id: String,
    val name: String,
    @SerialName("list_group_id") val listGroupId: String? = null,
    val color: String? = null,
    val icon: String? = null,
    @SerialName("sort_order") val sortOrder: Double = 0.0,
    @SerialName("default_view") val defaultView: String = "list",
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)

@Serializable
data class SectionExportDto(
    val id: String,
    @SerialName("project_id") val projectId: String,
    val name: String,
    @SerialName("sort_order") val sortOrder: Double = 0.0,
    @SerialName("is_collapsed") val isCollapsed: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)

@Serializable
data class TaskExportDto(
    val id: String,
    val title: String,
    val notes: String = "",
    @SerialName("description") val legacyDescription: String? = null,
    @SerialName("workspace_id") val workspaceId: String? = null,
    @SerialName("area_id") val areaId: String? = null,
    @SerialName("project_id") val projectId: String? = null,
    @SerialName("list_id") val legacyListId: String? = null,
    @SerialName("section_id") val sectionId: String? = null,
    @SerialName("parent_task_id") val parentTaskId: String? = null,
    @SerialName("due_date") val dueDate: String? = null,
    @SerialName("due_time") val dueTime: String? = null,
    @SerialName("all_day") val allDay: Boolean = true,
    @SerialName("recurrence_rule") val recurrenceRule: String? = null,
    @SerialName("recurrence_basis") val recurrenceBasis: String = "due_date",
    val priority: Int = 0,
    @SerialName("is_starred") val isStarred: Boolean = false,
    @SerialName("is_completed") val isCompleted: Boolean = false,
    @SerialName("completed_at") val completedAt: String? = null,
    @SerialName("estimated_minutes") val estimatedMinutes: Int? = null,
    @SerialName("sort_order") val sortOrder: Double = 0.0,
    @SerialName("my_day_date") val myDayDate: String? = null,
    @SerialName("pomodoro_count") val pomodoroCount: Int = 0,
    @SerialName("scheduled_start_min") val scheduledStartMin: Int? = null,
    @SerialName("scheduled_duration_min") val scheduledDurationMin: Int? = null,
    val bucket: String = "inbox",
    @SerialName("waiting_on") val waitingOn: String? = null,
    @SerialName("waiting_since") val waitingSince: String? = null,
    @SerialName("follow_up_date") val followUpDate: String? = null,
    @SerialName("reviewed_at") val reviewedAt: String? = null,
    @SerialName("is_trashed") val isTrashed: Boolean = false,
    @SerialName("trashed_at") val trashedAt: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)

@Serializable
data class TagExportDto(
    val id: String,
    val name: String,
    val color: String = "#808080",
    @SerialName("sort_order") val sortOrder: Double = 0.0,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)

@Serializable
data class TaskTagExportDto(@SerialName("task_id") val taskId: String, @SerialName("tag_id") val tagId: String)

@Serializable
data class TaskDependencyExportDto(
    @SerialName("task_id") val taskId: String,
    @SerialName("depends_on_id") val dependsOnId: String,
)

@Serializable
data class MilestoneExportDto(
    val id: String,
    @SerialName("project_id") val projectId: String,
    val title: String,
    @SerialName("due_date") val dueDate: String? = null,
    @SerialName("is_completed") val isCompleted: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)

@Serializable
data class ReminderExportDto(
    val id: String,
    @SerialName("task_id") val taskId: String,
    @SerialName("remind_at") val remindAt: String,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)

@Serializable
data class PomodoroSessionExportDto(
    val id: String,
    @SerialName("task_id") val taskId: String? = null,
    val type: String = "focus",
    @SerialName("duration_seconds") val durationSeconds: Int = 1500,
    @SerialName("started_at") val startedAt: String,
    @SerialName("ended_at") val endedAt: String? = null,
    @SerialName("completed_at") val completedAt: String? = null,
    @SerialName("was_completed") val wasCompleted: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
data class GoalExportDto(
    val id: String,
    val title: String,
    val description: String? = null,
    @SerialName("goal_type") val goalType: String = "numeric",
    @SerialName("target_date") val targetDate: String? = null,
    @SerialName("target_value") val targetValue: Double = 100.0,
    @SerialName("current_value") val currentValue: Double = 0.0,
    val status: String = "in_progress",
    @SerialName("streak_count") val streakCount: Int = 0,
    @SerialName("last_progress_at") val lastProgressAt: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)

@Serializable
data class GoalLinkExportDto(
    @SerialName("goal_id") val goalId: String,
    @SerialName("entity_type") val entityType: String = "",
    @SerialName("resource_type") val resourceType: String = "",
    @SerialName("entity_id") val entityId: String = "",
    @SerialName("resource_id") val resourceId: String = "",
)

@Serializable
data class BackupMetadata(
    @SerialName("backup_version") val backupVersion: Int = 1,
    @SerialName("app_version") val appVersion: String = "1.0.0",
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("task_count") val taskCount: Int = 0,
    @SerialName("project_count") val projectCount: Int = 0,
    @SerialName("area_count") val areaCount: Int = 0,
)

data class ImportResult(
    val areasCount: Int,
    val projectsCount: Int,
    val tasksCount: Int,
    val tagsCount: Int,
    val sectionsCount: Int = 0,
    val remindersCount: Int = 0,
    val milestonesCount: Int = 0,
)
