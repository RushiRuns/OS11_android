package com.os11.app.data.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "local_identity")
data class LocalIdentityEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "display_name")
    val displayName: String,
    @ColumnInfo(name = "avatar_emoji")
    val avatarEmoji: String? = null,
    @ColumnInfo(name = "created_at")
    val createdAt: String,
)

@Entity(tableName = "workspaces")
data class WorkspaceEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "created_at")
    val createdAt: String,
    @ColumnInfo(name = "updated_at")
    val updatedAt: String,
)

@Entity(
    tableName = "areas",
    foreignKeys = [
        ForeignKey(
            entity = WorkspaceEntity::class,
            parentColumns = ["id"],
            childColumns = ["workspace_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(name = "idx_areas_workspace", value = ["workspace_id", "sort_order"]),
    ],
)
data class AreaEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "workspace_id")
    val workspaceId: String,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "icon")
    val icon: String? = null,
    @ColumnInfo(name = "color")
    val color: String? = null,
    @ColumnInfo(name = "sort_order", defaultValue = "0")
    val sortOrder: Double = 0.0,
    @ColumnInfo(name = "is_default", defaultValue = "0")
    val isDefault: Boolean = false,
    @ColumnInfo(name = "created_at")
    val createdAt: String,
    @ColumnInfo(name = "updated_at")
    val updatedAt: String,
)

@Entity(
    tableName = "projects",
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
    ],
    indices = [
        Index(name = "idx_projects_workspace", value = ["workspace_id"]),
        Index(name = "idx_projects_area", value = ["area_id", "sort_order"]),
        Index(name = "idx_projects_status", value = ["status"]),
        Index(name = "idx_projects_someday", value = ["is_someday"]),
    ],
)
data class ProjectEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "workspace_id")
    val workspaceId: String,
    @ColumnInfo(name = "area_id")
    val areaId: String? = null,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "description")
    val description: String? = null,
    @ColumnInfo(name = "color")
    val color: String? = null,
    @ColumnInfo(name = "icon")
    val icon: String? = null,
    @ColumnInfo(name = "status", defaultValue = "active")
    val status: String = "active",
    @ColumnInfo(name = "due_date")
    val dueDate: String? = null,
    @ColumnInfo(name = "default_view", defaultValue = "list")
    val defaultView: String = "list",
    @ColumnInfo(name = "is_someday", defaultValue = "0")
    val isSomeday: Boolean = false,
    @ColumnInfo(name = "reviewed_at")
    val reviewedAt: String? = null,
    @ColumnInfo(name = "sort_order", defaultValue = "0")
    val sortOrder: Double = 0.0,
    @ColumnInfo(name = "created_at")
    val createdAt: String,
    @ColumnInfo(name = "updated_at")
    val updatedAt: String,
)

@Entity(
    tableName = "sections",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["project_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(name = "idx_sections_project_id", value = ["project_id"]),
    ],
)
data class SectionEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "project_id")
    val projectId: String,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "sort_order", defaultValue = "0")
    val sortOrder: Double = 0.0,
    @ColumnInfo(name = "is_collapsed", defaultValue = "0")
    val isCollapsed: Boolean = false,
    @ColumnInfo(name = "created_at")
    val createdAt: String,
    @ColumnInfo(name = "updated_at")
    val updatedAt: String,
)
