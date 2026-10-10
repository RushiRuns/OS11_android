package com.os11.app.service.tag

/**
 * Service-level DTO representing a tag, preserving Konsist UI boundaries.
 */
data class TagItem(
    val id: String,
    val name: String,
    val color: String? = null,
    val parentTagId: String? = null,
    val sortOrder: Double = 0.0,
    val taskCount: Int = 0,
) {
    val displayName: String
        get() = if (name.contains("/")) name.substringAfterLast("/") else name

    val parentPath: String?
        get() = if (name.contains("/")) name.substringBeforeLast("/") else null

    val depth: Int
        get() = name.count { it == '/' }
}
