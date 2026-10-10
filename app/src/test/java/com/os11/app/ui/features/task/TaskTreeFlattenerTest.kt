package com.os11.app.ui.features.task

import com.os11.app.service.task.TaskItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskTreeFlattenerTest {

    @Test
    fun `flatten orders roots and nested subtasks with correct indent depths`() = runBlocking {
        val root1 = TaskItem(
            id = "root-1",
            title = "Root 1",
            workspaceId = "ws-1",
            createdAt = "2026-10-10T10:00:00Z",
            updatedAt = "2026-10-10T10:00:00Z",
        )
        val child1 = TaskItem(
            id = "child-1",
            title = "Child 1",
            workspaceId = "ws-1",
            parentTaskId = "root-1",
            createdAt = "2026-10-10T10:01:00Z",
            updatedAt = "2026-10-10T10:01:00Z",
        )
        val grandchild1 = TaskItem(
            id = "grandchild-1",
            title = "Grandchild 1",
            workspaceId = "ws-1",
            parentTaskId = "child-1",
            createdAt = "2026-10-10T10:02:00Z",
            updatedAt = "2026-10-10T10:02:00Z",
        )
        val root2 = TaskItem(
            id = "root-2",
            title = "Root 2",
            workspaceId = "ws-1",
            createdAt = "2026-10-10T10:03:00Z",
            updatedAt = "2026-10-10T10:03:00Z",
        )

        val flattened = TaskTreeFlattener.flatten(
            tasks = listOf(root1, child1, grandchild1, root2),
            todayIso = "2026-10-10",
            dispatcher = Dispatchers.Unconfined,
        )

        assertEquals(4, flattened.size)
        // DFS order: root1, child1, grandchild1, root2
        assertEquals("root-1", flattened[0].id)
        assertEquals(0, flattened[0].indentDepth)

        assertEquals("child-1", flattened[1].id)
        assertEquals(1, flattened[1].indentDepth)

        assertEquals("grandchild-1", flattened[2].id)
        assertEquals(2, flattened[2].indentDepth)

        assertEquals("root-2", flattened[3].id)
        assertEquals(0, flattened[3].indentDepth)
    }

    @Test
    fun `flatten flags overdue tasks accurately`() = runBlocking {
        val overdueTask = TaskItem(
            id = "overdue-1",
            title = "Old task",
            workspaceId = "ws-1",
            dueDate = "2026-10-01",
            isCompleted = false,
            createdAt = "2026-10-01T10:00:00Z",
            updatedAt = "2026-10-01T10:00:00Z",
        )
        val completedPastTask = TaskItem(
            id = "done-1",
            title = "Done past task",
            workspaceId = "ws-1",
            dueDate = "2026-10-01",
            isCompleted = true,
            createdAt = "2026-10-01T10:00:00Z",
            updatedAt = "2026-10-01T10:00:00Z",
        )

        val flattened = TaskTreeFlattener.flatten(
            tasks = listOf(overdueTask, completedPastTask),
            todayIso = "2026-10-10",
            dispatcher = Dispatchers.Unconfined,
        )

        assertTrue(flattened[0].isOverdue)
        assertFalse(flattened[1].isOverdue)
    }
}
