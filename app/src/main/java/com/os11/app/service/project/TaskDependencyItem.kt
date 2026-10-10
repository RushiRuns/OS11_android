package com.os11.app.service.project

/**
 * Service DTO for a task dependency relationship.
 */
data class TaskDependencyItem(val taskId: String, val dependsOnId: String)
