package com.os11.app.data.sync

import androidx.room.withTransaction
import com.os11.app.data.db.OS11Database
import com.os11.app.data.db.entities.AreaEntity
import com.os11.app.data.db.entities.AttachmentEntity
import com.os11.app.data.db.entities.CommentEntity
import com.os11.app.data.db.entities.GoalEntity
import com.os11.app.data.db.entities.GoalLinkEntity
import com.os11.app.data.db.entities.LocalIdentityEntity
import com.os11.app.data.db.entities.MilestoneEntity
import com.os11.app.data.db.entities.ModuleEntity
import com.os11.app.data.db.entities.NotificationHistoryEntity
import com.os11.app.data.db.entities.PomodoroSessionEntity
import com.os11.app.data.db.entities.ProjectEntity
import com.os11.app.data.db.entities.ReminderDeviceStateEntity
import com.os11.app.data.db.entities.ReminderEntity
import com.os11.app.data.db.entities.SectionEntity
import com.os11.app.data.db.entities.SettingEntity
import com.os11.app.data.db.entities.SyncFieldClockEntity
import com.os11.app.data.db.entities.SyncLocalStateEntity
import com.os11.app.data.db.entities.SyncQueueEntity
import com.os11.app.data.db.entities.SyncTombstoneEntity
import com.os11.app.data.db.entities.TagEntity
import com.os11.app.data.db.entities.TaskDependencyEntity
import com.os11.app.data.db.entities.TaskEntity
import com.os11.app.data.db.entities.TaskHistoryEntity
import com.os11.app.data.db.entities.TaskTagEntity
import com.os11.app.data.db.entities.WorkspaceEntity
import com.os11.domain.id.DefaultIdGenerator
import com.os11.domain.id.IdGenerator
import com.os11.domain.logic.sync.FieldWrite
import com.os11.domain.logic.sync.HlcClock
import com.os11.domain.logic.sync.MergeGroups
import com.os11.domain.logic.sync.Operation
import com.os11.domain.time.Clock
import com.os11.domain.time.SystemClock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Side-effect listener invoked after a tracked write transaction successfully commits.
 */
fun interface TrackedWriteListener {
    fun onWriteCommitted(entity: String, entityId: String, operation: Operation)
}

/**
 * The single gateway for local writes to synced tables.
 *
 * Guarantees that inside one database transaction:
 * 1. Data row is written
 * 2. Per-field clocks are stamped in sync_field_clock
 * 3. Outbox row is appended to sync_queue
 * 4. sync_local_state is updated
 * 5. If task, change diff is appended to task_history
 * 6. Post-commit hooks are fired
 */
@Suppress("TooManyFunctions", "LongParameterList")
class TrackedWriter(
    private val db: OS11Database,
    private val hlcClock: HlcClock,
    private val clock: Clock = SystemClock(),
    private val idGenerator: IdGenerator = DefaultIdGenerator(),
) {
    private val json = Json { prettyPrint = false }
    private val listeners = CopyOnWriteArrayList<TrackedWriteListener>()

    fun registerListener(listener: TrackedWriteListener) {
        listeners.add(listener)
    }

    fun unregisterListener(listener: TrackedWriteListener) {
        listeners.remove(listener)
    }

    private fun notifyCommitted(entity: String, entityId: String, operation: Operation) {
        for (listener in listeners) {
            listener.onWriteCommitted(entity, entityId, operation)
        }
    }

    companion object {
        const val MAX_OUTBOX_CAP = 5000
        private val TASK_SYNCED_FIELDS = listOf(
            "title", "notes", "workspace_id", "area_id", "project_id", "section_id", "parent_task_id",
            "due_date", "due_time", "all_day", "recurrence_rule", "recurrence_basis", "priority",
            "is_starred", "is_completed", "completed_at", "estimated_minutes", "sort_order", "my_day_date",
            "scheduled_start_min", "scheduled_duration_min", "bucket", "waiting_on", "waiting_since",
            "follow_up_date", "reviewed_at", "is_trashed", "trashed_at",
        )

        val SYNCED_SETTINGS_ALLOWLIST: Set<String> = setOf(
            "day_starts_at",
            "pomodoro_work_minutes",
            "pomodoro_break_minutes",
            "pomodoro_long_break_minutes",
            "pomodoro_sessions_before_long_break",
            "waiting_stale_days",
            "follow_up_notify_time",
        )
    }

    // --- Task Writes ---

    suspend fun writeTask(task: TaskEntity): TaskEntity {
        val (entityId, op) = db.withTransaction {
            val existing = db.taskDao().getById(task.id)
            val changedFields = computeTaskChangedFields(existing, task)
            val tickHlc = hlcClock.tick()

            db.taskDao().upsert(task)

            // Update clocks & build payload
            val payload = mutableMapOf<String, FieldWrite>()
            val clocks = mutableListOf<SyncFieldClockEntity>()

            for (field in changedFields) {
                val value = extractTaskFieldValue(task, field)
                payload[field] = FieldWrite(value, tickHlc.raw)
                clocks.add(SyncFieldClockEntity("tasks", task.id, field, tickHlc.raw))
            }

            if (clocks.isNotEmpty()) {
                db.syncFieldClockDao().upsertAll(clocks)
                enqueueOutbox("tasks", task.id, Operation.UPSERT, json.encodeToString(payload))
            }

            db.syncLocalStateDao().set(SyncLocalStateEntity(id = 1, lastHlc = tickHlc.raw))

            if (existing != null && changedFields.isNotEmpty()) {
                val diffJson = json.encodeToString(
                    changedFields.associateWith { field ->
                        mapOf(
                            "from" to extractTaskFieldValue(existing, field).toString(),
                            "to" to extractTaskFieldValue(task, field).toString(),
                        )
                    },
                )
                db.taskHistoryDao().upsert(
                    TaskHistoryEntity(
                        id = idGenerator.randomId(),
                        taskId = task.id,
                        changedFields = diffJson,
                        changedAt = clock.nowIso(),
                    ),
                )
            }

            pruneOutboxIfNeeded()
            task.id to Operation.UPSERT
        }
        notifyCommitted("tasks", entityId, op)
        return task
    }

    suspend fun deleteTask(taskId: String) {
        val (entityId, op) = db.withTransaction {
            val tickHlc = hlcClock.tick()
            val nowIso = clock.nowIso()

            // Cascade reminders
            val reminders = db.reminderDao().getByTask(taskId)
            for (reminder in reminders) {
                db.syncTombstoneDao().upsert(SyncTombstoneEntity("reminders", reminder.id, tickHlc.raw, nowIso))
                enqueueOutbox("reminders", reminder.id, Operation.DELETE, "{}")
                db.reminderDao().deleteById(reminder.id)
            }

            // Cascade attachments
            val attachments = db.attachmentDao().getByTask(taskId)
            for (att in attachments) {
                db.syncTombstoneDao().upsert(SyncTombstoneEntity("attachments", att.id, tickHlc.raw, nowIso))
                enqueueOutbox("attachments", att.id, Operation.DELETE, "{}")
                db.attachmentDao().deleteById(att.id)
            }

            // Task tombstone
            db.syncTombstoneDao().upsert(SyncTombstoneEntity("tasks", taskId, tickHlc.raw, nowIso))
            enqueueOutbox("tasks", taskId, Operation.DELETE, "{}")
            db.taskDao().deleteById(taskId)
            db.syncLocalStateDao().set(SyncLocalStateEntity(id = 1, lastHlc = tickHlc.raw))

            pruneOutboxIfNeeded()
            taskId to Operation.DELETE
        }
        notifyCommitted("tasks", entityId, op)
    }

    // --- Workspace, Area, Project, Section Writes ---

    suspend fun writeWorkspace(workspace: WorkspaceEntity): WorkspaceEntity {
        db.withTransaction {
            val tickHlc = hlcClock.tick()
            db.workspaceDao().upsert(workspace)
            val payload = mapOf("name" to FieldWrite(JsonPrimitive(workspace.name), tickHlc.raw))
            db.syncFieldClockDao().upsert(SyncFieldClockEntity("workspaces", workspace.id, "name", tickHlc.raw))
            enqueueOutbox("workspaces", workspace.id, Operation.UPSERT, json.encodeToString(payload))
            db.syncLocalStateDao().set(SyncLocalStateEntity(id = 1, lastHlc = tickHlc.raw))
            pruneOutboxIfNeeded()
        }
        notifyCommitted("workspaces", workspace.id, Operation.UPSERT)
        return workspace
    }

    suspend fun writeArea(area: AreaEntity): AreaEntity {
        db.withTransaction {
            val tickHlc = hlcClock.tick()
            db.areaDao().upsert(area)
            val payload = mapOf(
                "name" to FieldWrite(JsonPrimitive(area.name), tickHlc.raw),
                "workspace_id" to FieldWrite(JsonPrimitive(area.workspaceId), tickHlc.raw),
                "sort_order" to FieldWrite(JsonPrimitive(area.sortOrder), tickHlc.raw),
            )
            val clocks = payload.keys.map { field ->
                SyncFieldClockEntity("areas", area.id, field, tickHlc.raw)
            }
            db.syncFieldClockDao().upsertAll(clocks)
            enqueueOutbox("areas", area.id, Operation.UPSERT, json.encodeToString(payload))
            db.syncLocalStateDao().set(SyncLocalStateEntity(id = 1, lastHlc = tickHlc.raw))
            pruneOutboxIfNeeded()
        }
        notifyCommitted("areas", area.id, Operation.UPSERT)
        return area
    }

    suspend fun deleteArea(areaId: String) {
        db.withTransaction {
            val tickHlc = hlcClock.tick()
            val nowIso = clock.nowIso()
            db.syncTombstoneDao().upsert(SyncTombstoneEntity("areas", areaId, tickHlc.raw, nowIso))
            enqueueOutbox("areas", areaId, Operation.DELETE, "{}")
            db.areaDao().deleteById(areaId)
            db.syncLocalStateDao().set(SyncLocalStateEntity(id = 1, lastHlc = tickHlc.raw))
            pruneOutboxIfNeeded()
        }
        notifyCommitted("areas", areaId, Operation.DELETE)
    }

    suspend fun writeProject(project: ProjectEntity): ProjectEntity {
        db.withTransaction {
            val tickHlc = hlcClock.tick()
            db.projectDao().upsert(project)
            val payload = mapOf(
                "name" to FieldWrite(JsonPrimitive(project.name), tickHlc.raw),
                "workspace_id" to FieldWrite(JsonPrimitive(project.workspaceId), tickHlc.raw),
                "status" to FieldWrite(JsonPrimitive(project.status), tickHlc.raw),
                "sort_order" to FieldWrite(JsonPrimitive(project.sortOrder), tickHlc.raw),
                "default_view" to FieldWrite(JsonPrimitive(project.defaultView), tickHlc.raw),
            )
            val clocks = payload.keys.map { field ->
                SyncFieldClockEntity("projects", project.id, field, tickHlc.raw)
            }
            db.syncFieldClockDao().upsertAll(clocks)
            enqueueOutbox("projects", project.id, Operation.UPSERT, json.encodeToString(payload))
            db.syncLocalStateDao().set(SyncLocalStateEntity(id = 1, lastHlc = tickHlc.raw))
            pruneOutboxIfNeeded()
        }
        notifyCommitted("projects", project.id, Operation.UPSERT)
        return project
    }

    suspend fun deleteProject(projectId: String) {
        db.withTransaction {
            val tickHlc = hlcClock.tick()
            val nowIso = clock.nowIso()
            db.syncTombstoneDao().upsert(SyncTombstoneEntity("projects", projectId, tickHlc.raw, nowIso))
            enqueueOutbox("projects", projectId, Operation.DELETE, "{}")
            db.projectDao().deleteById(projectId)
            db.syncLocalStateDao().set(SyncLocalStateEntity(id = 1, lastHlc = tickHlc.raw))
            pruneOutboxIfNeeded()
        }
        notifyCommitted("projects", projectId, Operation.DELETE)
    }

    suspend fun writeSection(section: SectionEntity): SectionEntity {
        db.withTransaction {
            val tickHlc = hlcClock.tick()
            db.sectionDao().upsert(section)
            val payload = mapOf(
                "name" to FieldWrite(JsonPrimitive(section.name), tickHlc.raw),
                "project_id" to FieldWrite(JsonPrimitive(section.projectId), tickHlc.raw),
                "sort_order" to FieldWrite(JsonPrimitive(section.sortOrder), tickHlc.raw),
            )
            val clocks = payload.keys.map { field ->
                SyncFieldClockEntity("sections", section.id, field, tickHlc.raw)
            }
            db.syncFieldClockDao().upsertAll(clocks)
            enqueueOutbox("sections", section.id, Operation.UPSERT, json.encodeToString(payload))
            db.syncLocalStateDao().set(SyncLocalStateEntity(id = 1, lastHlc = tickHlc.raw))
            pruneOutboxIfNeeded()
        }
        notifyCommitted("sections", section.id, Operation.UPSERT)
        return section
    }

    suspend fun deleteSection(sectionId: String) {
        db.withTransaction {
            val tickHlc = hlcClock.tick()
            val nowIso = clock.nowIso()
            db.syncTombstoneDao().upsert(SyncTombstoneEntity("sections", sectionId, tickHlc.raw, nowIso))
            enqueueOutbox("sections", sectionId, Operation.DELETE, "{}")
            db.sectionDao().deleteById(sectionId)
            db.syncLocalStateDao().set(SyncLocalStateEntity(id = 1, lastHlc = tickHlc.raw))
            pruneOutboxIfNeeded()
        }
        notifyCommitted("sections", sectionId, Operation.DELETE)
    }

    // --- Tags and Dependencies ---

    suspend fun writeTag(tag: TagEntity): TagEntity {
        db.withTransaction {
            val tickHlc = hlcClock.tick()
            db.tagDao().upsert(tag)
            val payload = mapOf(
                "name" to FieldWrite(JsonPrimitive(tag.name), tickHlc.raw),
                "sort_order" to FieldWrite(JsonPrimitive(tag.sortOrder), tickHlc.raw),
            )
            val clocks = payload.keys.map { field ->
                SyncFieldClockEntity("tags", tag.id, field, tickHlc.raw)
            }
            db.syncFieldClockDao().upsertAll(clocks)
            enqueueOutbox("tags", tag.id, Operation.UPSERT, json.encodeToString(payload))
            db.syncLocalStateDao().set(SyncLocalStateEntity(id = 1, lastHlc = tickHlc.raw))
            pruneOutboxIfNeeded()
        }
        notifyCommitted("tags", tag.id, Operation.UPSERT)
        return tag
    }

    suspend fun deleteTag(tagId: String) {
        db.withTransaction {
            val tickHlc = hlcClock.tick()
            val nowIso = clock.nowIso()
            db.syncTombstoneDao().upsert(SyncTombstoneEntity("tags", tagId, tickHlc.raw, nowIso))
            enqueueOutbox("tags", tagId, Operation.DELETE, "{}")
            db.tagDao().deleteById(tagId)
            db.syncLocalStateDao().set(SyncLocalStateEntity(id = 1, lastHlc = tickHlc.raw))
            pruneOutboxIfNeeded()
        }
        notifyCommitted("tags", tagId, Operation.DELETE)
    }

    suspend fun writeTaskTag(taskId: String, tagId: String) {
        val compositeKey = "$taskId|$tagId"
        db.withTransaction {
            val tickHlc = hlcClock.tick()
            db.taskTagDao().upsert(TaskTagEntity(taskId, tagId))
            enqueueOutbox("task_tags", compositeKey, Operation.UPSERT, "{}")
            db.syncLocalStateDao().set(SyncLocalStateEntity(id = 1, lastHlc = tickHlc.raw))
            pruneOutboxIfNeeded()
        }
        notifyCommitted("task_tags", compositeKey, Operation.UPSERT)
    }

    suspend fun deleteTaskTag(taskId: String, tagId: String) {
        val compositeKey = "$taskId|$tagId"
        db.withTransaction {
            val tickHlc = hlcClock.tick()
            val nowIso = clock.nowIso()
            db.syncTombstoneDao().upsert(SyncTombstoneEntity("task_tags", compositeKey, tickHlc.raw, nowIso))
            enqueueOutbox("task_tags", compositeKey, Operation.DELETE, "{}")
            db.taskTagDao().delete(taskId, tagId)
            db.syncLocalStateDao().set(SyncLocalStateEntity(id = 1, lastHlc = tickHlc.raw))
            pruneOutboxIfNeeded()
        }
        notifyCommitted("task_tags", compositeKey, Operation.DELETE)
    }

    suspend fun writeDependency(taskId: String, dependsOnId: String) {
        val compositeKey = "$taskId|$dependsOnId"
        db.withTransaction {
            val tickHlc = hlcClock.tick()
            db.taskDependencyDao().upsert(TaskDependencyEntity(taskId, dependsOnId))
            enqueueOutbox("task_dependencies", compositeKey, Operation.UPSERT, "{}")
            db.syncLocalStateDao().set(SyncLocalStateEntity(id = 1, lastHlc = tickHlc.raw))
            pruneOutboxIfNeeded()
        }
        notifyCommitted("task_dependencies", compositeKey, Operation.UPSERT)
    }

    suspend fun deleteDependency(taskId: String, dependsOnId: String) {
        val compositeKey = "$taskId|$dependsOnId"
        db.withTransaction {
            val tickHlc = hlcClock.tick()
            val nowIso = clock.nowIso()
            db.syncTombstoneDao().upsert(SyncTombstoneEntity("task_dependencies", compositeKey, tickHlc.raw, nowIso))
            enqueueOutbox("task_dependencies", compositeKey, Operation.DELETE, "{}")
            db.taskDependencyDao().delete(taskId, dependsOnId)
            db.syncLocalStateDao().set(SyncLocalStateEntity(id = 1, lastHlc = tickHlc.raw))
            pruneOutboxIfNeeded()
        }
        notifyCommitted("task_dependencies", compositeKey, Operation.DELETE)
    }

    suspend fun writeMilestone(milestone: MilestoneEntity): MilestoneEntity {
        db.withTransaction {
            val tickHlc = hlcClock.tick()
            db.milestoneDao().upsert(milestone)
            val payload = mapOf(
                "title" to FieldWrite(JsonPrimitive(milestone.title), tickHlc.raw),
                "due_date" to FieldWrite(JsonPrimitive(milestone.dueDate), tickHlc.raw),
                "is_completed" to FieldWrite(JsonPrimitive(milestone.isCompleted), tickHlc.raw),
            )
            val clocks = payload.keys.map { field ->
                SyncFieldClockEntity("milestones", milestone.id, field, tickHlc.raw)
            }
            db.syncFieldClockDao().upsertAll(clocks)
            enqueueOutbox("milestones", milestone.id, Operation.UPSERT, json.encodeToString(payload))
            db.syncLocalStateDao().set(SyncLocalStateEntity(id = 1, lastHlc = tickHlc.raw))
            pruneOutboxIfNeeded()
        }
        notifyCommitted("milestones", milestone.id, Operation.UPSERT)
        return milestone
    }

    suspend fun deleteMilestone(milestoneId: String) {
        db.withTransaction {
            val tickHlc = hlcClock.tick()
            val nowIso = clock.nowIso()
            db.syncTombstoneDao().upsert(SyncTombstoneEntity("milestones", milestoneId, tickHlc.raw, nowIso))
            enqueueOutbox("milestones", milestoneId, Operation.DELETE, "{}")
            db.milestoneDao().deleteById(milestoneId)
            db.syncLocalStateDao().set(SyncLocalStateEntity(id = 1, lastHlc = tickHlc.raw))
            pruneOutboxIfNeeded()
        }
        notifyCommitted("milestones", milestoneId, Operation.DELETE)
    }

    // --- Reminders ---

    suspend fun writeReminder(reminder: ReminderEntity): ReminderEntity {
        db.withTransaction {
            val tickHlc = hlcClock.tick()
            db.reminderDao().upsert(reminder)
            val payload = mapOf(
                "task_id" to FieldWrite(JsonPrimitive(reminder.taskId), tickHlc.raw),
                "remind_at" to FieldWrite(JsonPrimitive(reminder.remindAt), tickHlc.raw),
            )
            val clocks = payload.keys.map { field ->
                SyncFieldClockEntity("reminders", reminder.id, field, tickHlc.raw)
            }
            db.syncFieldClockDao().upsertAll(clocks)
            enqueueOutbox("reminders", reminder.id, Operation.UPSERT, json.encodeToString(payload))
            db.syncLocalStateDao().set(SyncLocalStateEntity(id = 1, lastHlc = tickHlc.raw))
            pruneOutboxIfNeeded()
        }
        notifyCommitted("reminders", reminder.id, Operation.UPSERT)
        return reminder
    }

    suspend fun deleteReminder(reminderId: String) {
        db.withTransaction {
            val tickHlc = hlcClock.tick()
            val nowIso = clock.nowIso()
            db.syncTombstoneDao().upsert(SyncTombstoneEntity("reminders", reminderId, tickHlc.raw, nowIso))
            enqueueOutbox("reminders", reminderId, Operation.DELETE, "{}")
            db.reminderDao().deleteById(reminderId)
            db.syncLocalStateDao().set(SyncLocalStateEntity(id = 1, lastHlc = tickHlc.raw))
            pruneOutboxIfNeeded()
        }
        notifyCommitted("reminders", reminderId, Operation.DELETE)
    }

    // --- Attachments & Comments & Pomodoro ---

    suspend fun writeAttachment(attachment: AttachmentEntity): AttachmentEntity {
        db.withTransaction {
            val tickHlc = hlcClock.tick()
            db.attachmentDao().upsert(attachment)
            val payload = buildMap {
                put("task_id", FieldWrite(JsonPrimitive(attachment.taskId), tickHlc.raw))
                put("filename", FieldWrite(JsonPrimitive(attachment.filename), tickHlc.raw))
                put("original_name", FieldWrite(JsonPrimitive(attachment.originalName), tickHlc.raw))
                put("mime_type", FieldWrite(JsonPrimitive(attachment.mimeType), tickHlc.raw))
                put("size_bytes", FieldWrite(JsonPrimitive(attachment.sizeBytes), tickHlc.raw))
                put("is_link", FieldWrite(JsonPrimitive(attachment.isLink), tickHlc.raw))
                attachment.linkUrl?.let { put("link_url", FieldWrite(JsonPrimitive(it), tickHlc.raw)) }
            }
            val clocks = payload.keys.map { field ->
                SyncFieldClockEntity("attachments", attachment.id, field, tickHlc.raw)
            }
            db.syncFieldClockDao().upsertAll(clocks)
            enqueueOutbox("attachments", attachment.id, Operation.UPSERT, json.encodeToString(payload))
            db.syncLocalStateDao().set(SyncLocalStateEntity(id = 1, lastHlc = tickHlc.raw))
            pruneOutboxIfNeeded()
        }
        notifyCommitted("attachments", attachment.id, Operation.UPSERT)
        return attachment
    }

    suspend fun deleteAttachment(attachmentId: String) {
        db.withTransaction {
            val tickHlc = hlcClock.tick()
            val nowIso = clock.nowIso()
            db.syncTombstoneDao().upsert(SyncTombstoneEntity("attachments", attachmentId, tickHlc.raw, nowIso))
            enqueueOutbox("attachments", attachmentId, Operation.DELETE, "{}")
            db.attachmentDao().deleteById(attachmentId)
            db.syncLocalStateDao().set(SyncLocalStateEntity(id = 1, lastHlc = tickHlc.raw))
            pruneOutboxIfNeeded()
        }
        notifyCommitted("attachments", attachmentId, Operation.DELETE)
    }

    suspend fun writeComment(comment: CommentEntity): CommentEntity {
        db.withTransaction {
            val tickHlc = hlcClock.tick()
            db.commentDao().upsert(comment)
            val payload = mapOf(
                "task_id" to FieldWrite(JsonPrimitive(comment.taskId), tickHlc.raw),
                "body" to FieldWrite(JsonPrimitive(comment.body), tickHlc.raw),
                "author_name" to FieldWrite(JsonPrimitive(comment.authorName), tickHlc.raw),
            )
            val clocks = payload.keys.map { field ->
                SyncFieldClockEntity("comments", comment.id, field, tickHlc.raw)
            }
            db.syncFieldClockDao().upsertAll(clocks)
            enqueueOutbox("comments", comment.id, Operation.UPSERT, json.encodeToString(payload))
            db.syncLocalStateDao().set(SyncLocalStateEntity(id = 1, lastHlc = tickHlc.raw))
            pruneOutboxIfNeeded()
        }
        notifyCommitted("comments", comment.id, Operation.UPSERT)
        return comment
    }

    suspend fun writePomodoroSession(session: PomodoroSessionEntity): PomodoroSessionEntity {
        db.withTransaction {
            val tickHlc = hlcClock.tick()
            db.pomodoroSessionDao().upsert(session)
            val payload = mapOf(
                "type" to FieldWrite(JsonPrimitive(session.type), tickHlc.raw),
                "duration_seconds" to FieldWrite(JsonPrimitive(session.durationSeconds), tickHlc.raw),
                "started_at" to FieldWrite(JsonPrimitive(session.startedAt), tickHlc.raw),
            )
            val clocks = payload.keys.map { field ->
                SyncFieldClockEntity("pomodoro_sessions", session.id, field, tickHlc.raw)
            }
            db.syncFieldClockDao().upsertAll(clocks)
            enqueueOutbox("pomodoro_sessions", session.id, Operation.UPSERT, json.encodeToString(payload))
            db.syncLocalStateDao().set(SyncLocalStateEntity(id = 1, lastHlc = tickHlc.raw))
            pruneOutboxIfNeeded()
        }
        notifyCommitted("pomodoro_sessions", session.id, Operation.UPSERT)
        return session
    }

    // --- Goals ---

    suspend fun writeGoal(goal: GoalEntity): GoalEntity {
        db.withTransaction {
            val tickHlc = hlcClock.tick()
            db.goalDao().upsert(goal)
            val payload = mapOf(
                "title" to FieldWrite(JsonPrimitive(goal.title), tickHlc.raw),
                "goal_type" to FieldWrite(JsonPrimitive(goal.goalType), tickHlc.raw),
                "current_value" to FieldWrite(JsonPrimitive(goal.currentValue), tickHlc.raw),
                "target_value" to FieldWrite(JsonPrimitive(goal.targetValue), tickHlc.raw),
            )
            val clocks = payload.keys.map { field ->
                SyncFieldClockEntity("goals", goal.id, field, tickHlc.raw)
            }
            db.syncFieldClockDao().upsertAll(clocks)
            enqueueOutbox("goals", goal.id, Operation.UPSERT, json.encodeToString(payload))
            db.syncLocalStateDao().set(SyncLocalStateEntity(id = 1, lastHlc = tickHlc.raw))
            pruneOutboxIfNeeded()
        }
        notifyCommitted("goals", goal.id, Operation.UPSERT)
        return goal
    }

    suspend fun deleteGoal(goalId: String) {
        db.withTransaction {
            val tickHlc = hlcClock.tick()
            val nowIso = clock.nowIso()
            db.syncTombstoneDao().upsert(SyncTombstoneEntity("goals", goalId, tickHlc.raw, nowIso))
            enqueueOutbox("goals", goalId, Operation.DELETE, "{}")
            db.goalDao().deleteById(goalId)
            db.syncLocalStateDao().set(SyncLocalStateEntity(id = 1, lastHlc = tickHlc.raw))
            pruneOutboxIfNeeded()
        }
        notifyCommitted("goals", goalId, Operation.DELETE)
    }

    suspend fun writeGoalLink(link: GoalLinkEntity): GoalLinkEntity {
        db.withTransaction {
            db.goalLinkDao().upsert(link)
        }
        return link
    }

    suspend fun deleteGoalLink(goalId: String, type: String, resourceId: String) {
        db.withTransaction {
            db.goalLinkDao().delete(goalId, type, resourceId)
        }
    }

    suspend fun writeSetting(key: String, value: String) {
        val isSynced = SYNCED_SETTINGS_ALLOWLIST.contains(key)
        if (isSynced) {
            db.withTransaction {
                val tickHlc = hlcClock.tick()
                db.settingDao().upsert(SettingEntity(key, value))
                val payload = mapOf("value" to FieldWrite(JsonPrimitive(value), tickHlc.raw))
                db.syncFieldClockDao().upsert(SyncFieldClockEntity("settings", key, "value", tickHlc.raw))
                enqueueOutbox("settings", key, Operation.UPSERT, json.encodeToString(payload))
                db.syncLocalStateDao().set(SyncLocalStateEntity(id = 1, lastHlc = tickHlc.raw))
                pruneOutboxIfNeeded()
            }
            notifyCommitted("settings", key, Operation.UPSERT)
        } else {
            db.withTransaction {
                db.settingDao().upsert(SettingEntity(key, value))
            }
        }
    }

    suspend fun writeNotification(item: NotificationHistoryEntity) {
        db.notificationHistoryDao().upsert(item)
    }

    suspend fun markNotificationRead(id: String, readAt: String) {
        db.notificationHistoryDao().markAsRead(id, readAt)
    }

    suspend fun markAllNotificationsRead(readAt: String) {
        db.notificationHistoryDao().markAllAsRead(readAt)
    }

    suspend fun deleteNotification(id: String) {
        db.notificationHistoryDao().deleteById(id)
    }

    suspend fun clearNotifications() {
        db.notificationHistoryDao().clearAll()
    }

    suspend fun purgeNotifications(cutoffIso: String) {
        db.notificationHistoryDao().purgeOlderThan(cutoffIso)
    }

    suspend fun writeModule(module: ModuleEntity) {
        db.moduleDao().upsert(module)
    }

    suspend fun writeReminderDeviceState(state: ReminderDeviceStateEntity) {
        db.reminderDeviceStateDao().upsert(state)
    }

    suspend fun incrementTaskPomodoroCount(taskId: String) {
        db.withTransaction {
            db.taskDao().incrementPomodoroCount(taskId)
        }
    }

    suspend fun updateTaskPomodoroCount(taskId: String, count: Int) {
        db.withTransaction {
            db.taskDao().updatePomodoroCount(taskId, count)
        }
    }

    suspend fun purgeTaskHistory(cutoffIso: String) {
        db.taskHistoryDao().purgeOlderThan(cutoffIso)
    }

    suspend fun writeLocalIdentity(identity: LocalIdentityEntity): LocalIdentityEntity {
        db.withTransaction {
            db.localIdentityDao().upsert(identity)
        }
        return identity
    }

    // --- Private helpers ---

    private suspend fun enqueueOutbox(entity: String, entityId: String, operation: Operation, payload: String) {
        db.syncQueueDao().enqueue(
            SyncQueueEntity(
                id = idGenerator.randomId(),
                operation = if (operation == Operation.UPSERT) "upsert" else "delete",
                entity = entity,
                entityId = entityId,
                payload = payload,
                createdAt = clock.nowIso(),
            ),
        )
    }

    private suspend fun pruneOutboxIfNeeded() {
        val count = db.syncQueueDao().count()
        if (count > MAX_OUTBOX_CAP) {
            db.syncQueueDao().pruneOldest(MAX_OUTBOX_CAP)
        }
    }

    private fun computeTaskChangedFields(old: TaskEntity?, new: TaskEntity): Set<String> {
        if (old == null) {
            // New task: all defined fields are changed
            val allFields = mutableSetOf<String>()
            for (f in TASK_SYNCED_FIELDS) {
                if (extractTaskFieldValue(new, f) !is JsonNull) {
                    allFields.addAll(MergeGroups.getGroupFields("tasks", f))
                }
            }
            return allFields
        }

        val changed = mutableSetOf<String>()
        for (f in TASK_SYNCED_FIELDS) {
            val oldVal = extractTaskFieldValue(old, f)
            val newVal = extractTaskFieldValue(new, f)
            if (oldVal != newVal) {
                changed.addAll(MergeGroups.getGroupFields("tasks", f))
            }
        }
        return changed
    }

    @Suppress("CyclomaticComplexMethod")
    private fun extractTaskFieldValue(task: TaskEntity, field: String): JsonElement = when (field) {
        "title" -> JsonPrimitive(task.title)
        "notes" -> task.notes?.let { JsonPrimitive(it) } ?: JsonNull
        "workspace_id" -> JsonPrimitive(task.workspaceId)
        "area_id" -> task.areaId?.let { JsonPrimitive(it) } ?: JsonNull
        "project_id" -> task.projectId?.let { JsonPrimitive(it) } ?: JsonNull
        "section_id" -> task.sectionId?.let { JsonPrimitive(it) } ?: JsonNull
        "parent_task_id" -> task.parentTaskId?.let { JsonPrimitive(it) } ?: JsonNull
        "due_date" -> task.dueDate?.let { JsonPrimitive(it) } ?: JsonNull
        "due_time" -> task.dueTime?.let { JsonPrimitive(it) } ?: JsonNull
        "all_day" -> JsonPrimitive(task.allDay)
        "recurrence_rule" -> task.recurrenceRule?.let { JsonPrimitive(it) } ?: JsonNull
        "recurrence_basis" -> task.recurrenceBasis?.let { JsonPrimitive(it) } ?: JsonNull
        "priority" -> JsonPrimitive(task.priority)
        "is_starred" -> JsonPrimitive(task.isStarred)
        "is_completed" -> JsonPrimitive(task.isCompleted)
        "completed_at" -> task.completedAt?.let { JsonPrimitive(it) } ?: JsonNull
        "estimated_minutes" -> task.estimatedMinutes?.let { JsonPrimitive(it) } ?: JsonNull
        "sort_order" -> JsonPrimitive(task.sortOrder)
        "my_day_date" -> task.myDayDate?.let { JsonPrimitive(it) } ?: JsonNull
        "scheduled_start_min" -> task.scheduledStartMin?.let { JsonPrimitive(it) } ?: JsonNull
        "scheduled_duration_min" -> task.scheduledDurationMin?.let { JsonPrimitive(it) } ?: JsonNull
        "bucket" -> task.bucket?.let { JsonPrimitive(it) } ?: JsonNull
        "waiting_on" -> task.waitingOn?.let { JsonPrimitive(it) } ?: JsonNull
        "waiting_since" -> task.waitingSince?.let { JsonPrimitive(it) } ?: JsonNull
        "follow_up_date" -> task.followUpDate?.let { JsonPrimitive(it) } ?: JsonNull
        "reviewed_at" -> task.reviewedAt?.let { JsonPrimitive(it) } ?: JsonNull
        "is_trashed" -> JsonPrimitive(task.isTrashed)
        "trashed_at" -> task.trashedAt?.let { JsonPrimitive(it) } ?: JsonNull
        else -> JsonNull
    }
}
