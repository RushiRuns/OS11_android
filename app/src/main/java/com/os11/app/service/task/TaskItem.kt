package com.os11.app.service.task

import com.os11.app.data.db.dao.SlimTask
import com.os11.app.data.db.entities.TaskEntity

/**
 * Application service task model exposed to UI layers.
 * Decouples Compose UI from internal Room DAO/Entity classes per Konsist architectural rules.
 */
data class TaskItem(
    val id: String,
    val title: String,
    val notes: String? = null,
    val workspaceId: String,
    val areaId: String? = null,
    val projectId: String? = null,
    val sectionId: String? = null,
    val parentTaskId: String? = null,
    val dueDate: String? = null,
    val dueTime: String? = null,
    val allDay: Boolean = true,
    val priority: Int = 0,
    val isStarred: Boolean = false,
    val isCompleted: Boolean = false,
    val completedAt: String? = null,
    val sortOrder: Double = 0.0,
    val myDayDate: String? = null,
    val bucket: String? = null,
    val isTrashed: Boolean = false,
    val trashedAt: String? = null,
    val createdAt: String,
    val updatedAt: String,
)

fun SlimTask.toTaskItem(): TaskItem = TaskItem(
    id = id,
    title = title,
    notes = null,
    workspaceId = workspaceId,
    areaId = areaId,
    projectId = projectId,
    sectionId = sectionId,
    parentTaskId = parentTaskId,
    dueDate = dueDate,
    dueTime = dueTime,
    allDay = allDay,
    priority = priority,
    isStarred = isStarred,
    isCompleted = isCompleted,
    completedAt = completedAt,
    sortOrder = sortOrder,
    myDayDate = myDayDate,
    bucket = bucket,
    isTrashed = isTrashed,
    trashedAt = null,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun TaskEntity.toTaskItem(): TaskItem = TaskItem(
    id = id,
    title = title,
    notes = notes,
    workspaceId = workspaceId,
    areaId = areaId,
    projectId = projectId,
    sectionId = sectionId,
    parentTaskId = parentTaskId,
    dueDate = dueDate,
    dueTime = dueTime,
    allDay = allDay,
    priority = priority,
    isStarred = isStarred,
    isCompleted = isCompleted,
    completedAt = completedAt,
    sortOrder = sortOrder,
    myDayDate = myDayDate,
    bucket = bucket,
    isTrashed = isTrashed,
    trashedAt = trashedAt,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
