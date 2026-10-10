package com.os11.app.data.seed

import com.os11.app.data.db.OS11Database
import com.os11.app.data.db.entities.AreaEntity
import com.os11.app.data.db.entities.ProjectEntity
import com.os11.app.data.db.entities.ReminderEntity
import com.os11.app.data.db.entities.TagEntity
import com.os11.app.data.db.entities.TaskEntity
import com.os11.app.data.db.entities.TaskTagEntity
import com.os11.domain.id.DeterministicIds
import java.util.UUID

/**
 * High-volume realistic seed data generator for benchmark and stress testing.
 * Generates 1,000 or 5,000 tasks distributed across areas, projects, tags, subtasks, and reminders.
 */
internal object SeedGenerator {

    @Suppress("LongMethod", "CyclomaticComplexMethod", "MagicNumber")
    suspend fun generateSeed(db: OS11Database, taskCount: Int = 1000, nowIso: String = "2026-10-10T10:00:00.000Z") {
        val workspaceId = DeterministicIds.defaultWorkspaceId()

        // 1. Create 5 Areas
        val areas = (1..5).map { i ->
            AreaEntity(
                id = UUID.randomUUID().toString(),
                workspaceId = workspaceId,
                name = "Area $i",
                icon = "folder",
                color = "#4da3ff",
                sortOrder = i.toDouble(),
                createdAt = nowIso,
                updatedAt = nowIso,
            )
        }
        for (area in areas) {
            db.areaDao().upsert(area)
        }

        // 2. Create 15 Projects
        val projects = (1..15).map { i ->
            val area = areas[i % areas.size]
            ProjectEntity(
                id = UUID.randomUUID().toString(),
                workspaceId = workspaceId,
                areaId = area.id,
                name = "Project $i",
                status = if (i % 5 == 0) "archived" else "active",
                sortOrder = i.toDouble(),
                createdAt = nowIso,
                updatedAt = nowIso,
            )
        }
        for (project in projects) {
            db.projectDao().upsert(project)
        }

        // 3. Create 20 Tags
        val tags = (1..20).map { i ->
            TagEntity(
                id = UUID.randomUUID().toString(),
                name = "tag-$i",
                color = "#4da3ff",
                sortOrder = i.toDouble(),
                createdAt = nowIso,
            )
        }
        for (tag in tags) {
            db.tagDao().upsert(tag)
        }

        // 4. Batch generate Tasks in chunks of 500 for fast insertion
        val tasks = ArrayList<TaskEntity>(taskCount)
        val reminders = ArrayList<ReminderEntity>()
        val taskTags = ArrayList<TaskTagEntity>()

        for (i in 1..taskCount) {
            val id = UUID.randomUUID().toString()
            val isProjectTask = i % 3 != 0
            val project = if (isProjectTask) projects[i % projects.size] else null
            val area = if (isProjectTask) {
                project?.areaId?.let { aid -> areas.firstOrNull { it.id == aid } }
            } else {
                areas[i % areas.size]
            }

            val isCompleted = i % 4 == 0
            val isStarred = i % 7 == 0
            val isMyDay = i % 5 == 0
            val isSubtask = i > 10 && i % 8 == 0
            val parentId = if (isSubtask) tasks[i - 2].id else null

            val task = TaskEntity(
                id = id,
                title = "Task $i: Benchmark payload task item",
                notes = "<p>Detailed description and notes for task $i.</p>",
                workspaceId = workspaceId,
                areaId = area?.id,
                projectId = project?.id,
                parentTaskId = parentId,
                priority = i % 5,
                isStarred = isStarred,
                isCompleted = isCompleted,
                completedAt = if (isCompleted) nowIso else null,
                myDayDate = if (isMyDay) "2026-10-10" else null,
                dueDate = if (i % 6 == 0) "2026-10-15" else null,
                sortOrder = i.toDouble(),
                createdAt = nowIso,
                updatedAt = nowIso,
            )
            tasks.add(task)

            // Attach tags
            if (i % 2 == 0) {
                taskTags.add(TaskTagEntity(id, tags[i % tags.size].id))
            }

            // Attach reminder
            if (i % 10 == 0) {
                reminders.add(
                    ReminderEntity(
                        id = UUID.randomUUID().toString(),
                        taskId = id,
                        remindAt = "2026-10-15T09:00:00.000Z",
                        createdAt = nowIso,
                    ),
                )
            }
        }

        // Insert in batches
        val chunkSize = 500
        for (chunk in tasks.chunked(chunkSize)) {
            db.taskDao().upsertAll(chunk)
        }
        for (tt in taskTags) {
            db.taskTagDao().upsert(tt)
        }
        for (r in reminders) {
            db.reminderDao().upsert(r)
        }
    }
}
