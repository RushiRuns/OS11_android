package com.os11.app.ui.features.task

import com.os11.app.service.task.TaskItem
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Flattens hierarchical task trees into an indexed linear list for LazyColumn rendering.
 *
 * Implements PERFORMANCE.md mitigation #14:
 * Subtask tree flattening runs off the main thread on Dispatchers.Default.
 */
object TaskTreeFlattener {

    private class FlattenContext(
        val todayIso: String,
        val projectNames: Map<String, String>,
        val areaNames: Map<String, String>,
        val subtaskCounts: Map<String, Pair<Int, Int>>,
        val childrenMap: Map<String, List<TaskItem>>,
    )

    suspend fun flatten(
        tasks: List<TaskItem>,
        todayIso: String,
        projectNames: Map<String, String> = emptyMap(),
        areaNames: Map<String, String> = emptyMap(),
        subtaskCounts: Map<String, Pair<Int, Int>> = emptyMap(),
        dispatcher: CoroutineDispatcher = Dispatchers.Default,
    ): List<TaskUiModel> = withContext(dispatcher) {
        if (tasks.isEmpty()) return@withContext emptyList()

        val taskMap = tasks.associateBy { it.id }
        val childrenMap = buildChildrenMap(tasks, taskMap)
        val rootTasks = tasks.filter { it.parentTaskId == null || !taskMap.containsKey(it.parentTaskId) }

        val context = FlattenContext(
            todayIso = todayIso,
            projectNames = projectNames,
            areaNames = areaNames,
            subtaskCounts = subtaskCounts,
            childrenMap = childrenMap,
        )

        val flattened = mutableListOf<TaskUiModel>()
        for (root in rootTasks) {
            traverseTree(
                task = root,
                depth = 0,
                context = context,
                outList = flattened,
            )
        }
        flattened
    }

    private fun buildChildrenMap(tasks: List<TaskItem>, taskMap: Map<String, TaskItem>): Map<String, List<TaskItem>> {
        val childrenMap = mutableMapOf<String, MutableList<TaskItem>>()
        for (task in tasks) {
            val parentId = task.parentTaskId
            if (parentId != null && taskMap.containsKey(parentId)) {
                childrenMap.computeIfAbsent(parentId) { mutableListOf() }.add(task)
            }
        }
        return childrenMap
    }

    private fun traverseTree(task: TaskItem, depth: Int, context: FlattenContext, outList: MutableList<TaskUiModel>) {
        outList.add(toUiModel(task, depth, context))

        val children = context.childrenMap[task.id] ?: return
        for (child in children) {
            traverseTree(
                task = child,
                depth = depth + 1,
                context = context,
                outList = outList,
            )
        }
    }

    private fun toUiModel(task: TaskItem, depth: Int, context: FlattenContext): TaskUiModel {
        val counts = context.subtaskCounts[task.id]
        val progressString = if (counts != null && counts.second > 0) {
            "${counts.first}/${counts.second}"
        } else {
            null
        }

        val isOverdue = task.dueDate != null && task.dueDate < context.todayIso && !task.isCompleted

        return TaskUiModel(
            id = task.id,
            title = task.title,
            isCompleted = task.isCompleted,
            isStarred = task.isStarred,
            priority = task.priority,
            dueDate = task.dueDate,
            dueTime = task.dueTime,
            formattedDueDate = task.dueDate,
            isOverdue = isOverdue,
            projectName = task.projectId?.let { context.projectNames[it] },
            areaName = task.areaId?.let { context.areaNames[it] },
            subtaskProgress = progressString,
            hasNotes = !task.notes.isNullOrBlank(),
            tagDots = persistentListOf(),
            indentDepth = depth,
            parentTaskId = task.parentTaskId,
            workspaceId = task.workspaceId,
        )
    }
}
