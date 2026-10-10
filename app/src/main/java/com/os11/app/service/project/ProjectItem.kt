package com.os11.app.service.project

/**
 * Service DTO for a Project, isolating UI components from Room entity models.
 */
data class ProjectItem(
    val id: String,
    val workspaceId: String,
    val areaId: String? = null,
    val name: String,
    val description: String? = null,
    val color: String? = null,
    val icon: String? = null,
    val status: String = "active",
    val dueDate: String? = null,
    val defaultView: String = "list",
    val isSomeday: Boolean = false,
    val sortOrder: Double = 0.0,
)
