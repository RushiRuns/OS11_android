package com.os11.app.service.project

import com.os11.app.data.db.entities.SectionEntity

/**
 * Service DTO for a Section, isolating UI components from Room entity models.
 */
data class SectionItem(
    val id: String,
    val projectId: String,
    val name: String,
    val sortOrder: Double = 0.0,
    val isCollapsed: Boolean = false,
)

fun SectionEntity.toItem() = SectionItem(
    id = id,
    projectId = projectId,
    name = name,
    sortOrder = sortOrder,
    isCollapsed = isCollapsed,
)
