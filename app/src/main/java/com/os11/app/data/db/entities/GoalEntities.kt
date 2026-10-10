package com.os11.app.data.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "title")
    val title: String,
    @ColumnInfo(name = "description")
    val description: String? = null,
    @ColumnInfo(name = "goal_type")
    val goalType: String,
    @ColumnInfo(name = "target_date")
    val targetDate: String? = null,
    @ColumnInfo(name = "target_value", defaultValue = "100")
    val targetValue: Double = 100.0,
    @ColumnInfo(name = "current_value", defaultValue = "0")
    val currentValue: Double = 0.0,
    @ColumnInfo(name = "streak_count", defaultValue = "0")
    val streakCount: Int = 0,
    @ColumnInfo(name = "last_progress_at")
    val lastProgressAt: String? = null,
    @ColumnInfo(name = "created_at")
    val createdAt: String,
    @ColumnInfo(name = "updated_at")
    val updatedAt: String,
)

@Entity(
    tableName = "goal_links",
    primaryKeys = ["goal_id", "resource_type", "resource_id"],
    foreignKeys = [
        ForeignKey(
            entity = GoalEntity::class,
            parentColumns = ["id"],
            childColumns = ["goal_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class GoalLinkEntity(
    @ColumnInfo(name = "goal_id")
    val goalId: String,
    @ColumnInfo(name = "resource_type")
    val resourceType: String,
    @ColumnInfo(name = "resource_id")
    val resourceId: String,
)
