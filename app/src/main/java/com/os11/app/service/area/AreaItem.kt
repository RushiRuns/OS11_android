package com.os11.app.service.area

/**
 * Service DTO for an Area, isolating UI components from Room entity models.
 */
data class AreaItem(
    val id: String,
    val workspaceId: String,
    val name: String,
    val icon: String? = null,
    val color: String? = null,
    val sortOrder: Double = 0.0,
    val isDefault: Boolean = false,
)
