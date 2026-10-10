package com.os11.app.service.tag

/**
 * Service-level DTO representing a tag, preserving Konsist UI boundaries.
 */
data class TagItem(val id: String, val name: String, val color: String? = null)
