package com.os11.app.data.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tasks",
    foreignKeys = [
        ForeignKey(
            entity = WorkspaceEntity::class,
            parentColumns = ["id"],
            childColumns = ["workspace_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = AreaEntity::class,
            parentColumns = ["id"],
            childColumns = ["area_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["project_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(
            entity = SectionEntity::class,
            parentColumns = ["id"],
            childColumns = ["section_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["parent_task_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(name = "idx_tasks_workspace", value = ["workspace_id"]),
        Index(name = "idx_tasks_area", value = ["area_id"]),
        Index(name = "idx_tasks_project", value = ["project_id", "sort_order"]),
        Index(name = "idx_tasks_section", value = ["section_id"]),
        Index(name = "idx_tasks_parent", value = ["parent_task_id"]),
        Index(name = "idx_tasks_due_date", value = ["due_date"]),
        Index(name = "idx_tasks_starred", value = ["is_starred"]),
        Index(name = "idx_tasks_completed", value = ["is_completed"]),
        Index(name = "idx_tasks_my_day", value = ["my_day_date"]),
        Index(name = "idx_tasks_trashed", value = ["is_trashed"]),
    ],
)
data class TaskEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "title")
    val title: String,
    @ColumnInfo(name = "notes")
    val notes: String? = null,
    @ColumnInfo(name = "workspace_id")
    val workspaceId: String,
    @ColumnInfo(name = "area_id")
    val areaId: String? = null,
    @ColumnInfo(name = "project_id")
    val projectId: String? = null,
    @ColumnInfo(name = "section_id")
    val sectionId: String? = null,
    @ColumnInfo(name = "parent_task_id")
    val parentTaskId: String? = null,
    @ColumnInfo(name = "due_date")
    val dueDate: String? = null,
    @ColumnInfo(name = "due_time")
    val dueTime: String? = null,
    @ColumnInfo(name = "all_day", defaultValue = "1")
    val allDay: Boolean = true,
    @ColumnInfo(name = "recurrence_rule")
    val recurrenceRule: String? = null,
    @ColumnInfo(name = "recurrence_basis")
    val recurrenceBasis: String? = null,
    @ColumnInfo(name = "priority", defaultValue = "0")
    val priority: Int = 0,
    @ColumnInfo(name = "is_starred", defaultValue = "0")
    val isStarred: Boolean = false,
    @ColumnInfo(name = "is_completed", defaultValue = "0")
    val isCompleted: Boolean = false,
    @ColumnInfo(name = "completed_at")
    val completedAt: String? = null,
    @ColumnInfo(name = "estimated_minutes")
    val estimatedMinutes: Int? = null,
    @ColumnInfo(name = "assignee_device_id")
    val assigneeDeviceId: String? = null,
    @ColumnInfo(name = "created_by_device", defaultValue = "local")
    val createdByDevice: String = "local",
    @ColumnInfo(name = "sort_order", defaultValue = "0")
    val sortOrder: Double = 0.0,
    @ColumnInfo(name = "my_day_date")
    val myDayDate: String? = null,
    @ColumnInfo(name = "pomodoro_count", defaultValue = "0")
    val pomodoroCount: Int = 0,
    @ColumnInfo(name = "scheduled_start_min")
    val scheduledStartMin: Int? = null,
    @ColumnInfo(name = "scheduled_duration_min")
    val scheduledDurationMin: Int? = null,
    @ColumnInfo(name = "bucket")
    val bucket: String? = null,
    @ColumnInfo(name = "waiting_on")
    val waitingOn: String? = null,
    @ColumnInfo(name = "waiting_since")
    val waitingSince: String? = null,
    @ColumnInfo(name = "follow_up_date")
    val followUpDate: String? = null,
    @ColumnInfo(name = "follow_up_notified_on")
    val followUpNotifiedOn: String? = null,
    @ColumnInfo(name = "reviewed_at")
    val reviewedAt: String? = null,
    @ColumnInfo(name = "is_trashed", defaultValue = "0")
    val isTrashed: Boolean = false,
    @ColumnInfo(name = "trashed_at")
    val trashedAt: String? = null,
    @ColumnInfo(name = "created_at")
    val createdAt: String,
    @ColumnInfo(name = "updated_at")
    val updatedAt: String,
)

@Entity(
    tableName = "task_dependencies",
    primaryKeys = ["task_id", "depends_on_id"],
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["task_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["depends_on_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(name = "idx_task_deps_target", value = ["depends_on_id"]),
    ],
)
data class TaskDependencyEntity(
    @ColumnInfo(name = "task_id")
    val taskId: String,
    @ColumnInfo(name = "depends_on_id")
    val dependsOnId: String,
)

@Entity(
    tableName = "milestones",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["project_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(name = "idx_milestones_project", value = ["project_id"]),
    ],
)
data class MilestoneEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "project_id")
    val projectId: String,
    @ColumnInfo(name = "title")
    val title: String,
    @ColumnInfo(name = "due_date")
    val dueDate: String,
    @ColumnInfo(name = "is_completed", defaultValue = "0")
    val isCompleted: Boolean = false,
    @ColumnInfo(name = "sort_order", defaultValue = "0")
    val sortOrder: Double = 0.0,
    @ColumnInfo(name = "created_at")
    val createdAt: String,
)

@Entity(
    tableName = "tags",
    foreignKeys = [
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["parent_tag_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index(name = "idx_tags_name", value = ["name"], unique = true),
        Index(name = "idx_tags_parent", value = ["parent_tag_id"]),
    ],
)
data class TagEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "color")
    val color: String? = null,
    @ColumnInfo(name = "parent_tag_id")
    val parentTagId: String? = null,
    @ColumnInfo(name = "sort_order", defaultValue = "0")
    val sortOrder: Double = 0.0,
    @ColumnInfo(name = "created_at")
    val createdAt: String,
)

@Entity(
    tableName = "task_tags",
    primaryKeys = ["task_id", "tag_id"],
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["task_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tag_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(name = "idx_task_tags_tag", value = ["tag_id"]),
    ],
)
data class TaskTagEntity(
    @ColumnInfo(name = "task_id")
    val taskId: String,
    @ColumnInfo(name = "tag_id")
    val tagId: String,
)
