package com.os11.app.service.project

import com.os11.app.data.db.entities.MilestoneEntity

/**
 * Service DTO for a Milestone, isolating UI components from Room entity models.
 */
data class MilestoneItem(
    val id: String,
    val projectId: String,
    val title: String,
    val dueDate: String,
    val isCompleted: Boolean = false,
    val sortOrder: Double = 0.0,
)

fun MilestoneEntity.toItem() = MilestoneItem(
    id = id,
    projectId = projectId,
    title = title,
    dueDate = dueDate,
    isCompleted = isCompleted,
    sortOrder = sortOrder,
)
