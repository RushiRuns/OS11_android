package com.os11.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.os11.app.data.db.entities.TaskEntity
import kotlinx.coroutines.flow.Flow

/**
 * Slim task projection excluding the heavy notes column for 60fps list rendering.
 */
data class SlimTask(
    val id: String,
    val title: String,
    val workspaceId: String,
    val areaId: String?,
    val projectId: String?,
    val sectionId: String?,
    val parentTaskId: String?,
    val dueDate: String?,
    val dueTime: String?,
    val allDay: Boolean,
    val recurrenceRule: String?,
    val recurrenceBasis: String?,
    val priority: Int,
    val isStarred: Boolean,
    val isCompleted: Boolean,
    val completedAt: String?,
    val estimatedMinutes: Int?,
    val sortOrder: Double,
    val myDayDate: String?,
    val pomodoroCount: Int,
    val scheduledStartMin: Int?,
    val scheduledDurationMin: Int?,
    val bucket: String?,
    val waitingOn: String?,
    val waitingSince: String?,
    val followUpDate: String?,
    val isTrashed: Boolean,
    val createdAt: String,
    val updatedAt: String,
)

@Dao
@Suppress("TooManyFunctions")
internal interface TaskDao {

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getById(id: String): TaskEntity?

    @Query("SELECT * FROM tasks WHERE id = :id")
    fun observeById(id: String): Flow<TaskEntity?>

    @Query(
        """
        SELECT id, title, workspace_id as workspaceId, area_id as areaId, project_id as projectId,
               section_id as sectionId, parent_task_id as parentTaskId, due_date as dueDate,
               due_time as dueTime, all_day as allDay, recurrence_rule as recurrenceRule,
               recurrence_basis as recurrenceBasis, priority, is_starred as isStarred,
               is_completed as isCompleted, completed_at as completedAt, estimated_minutes as estimatedMinutes,
               sort_order as sortOrder, my_day_date as myDayDate, pomodoro_count as pomodoroCount,
               scheduled_start_min as scheduledStartMin, scheduled_duration_min as scheduledDurationMin,
               bucket, waiting_on as waitingOn, waiting_since as waitingSince, follow_up_date as followUpDate,
               is_trashed as isTrashed, created_at as createdAt, updated_at as updatedAt
        FROM tasks
        WHERE is_trashed = 0
        ORDER BY sort_order ASC, id ASC
        """,
    )
    fun observeAllSlim(): Flow<List<SlimTask>>

    @Query(
        """
        SELECT id, title, workspace_id as workspaceId, area_id as areaId, project_id as projectId,
               section_id as sectionId, parent_task_id as parentTaskId, due_date as dueDate,
               due_time as dueTime, all_day as allDay, recurrence_rule as recurrenceRule,
               recurrence_basis as recurrenceBasis, priority, is_starred as isStarred,
               is_completed as isCompleted, completed_at as completedAt, estimated_minutes as estimatedMinutes,
               sort_order as sortOrder, my_day_date as myDayDate, pomodoro_count as pomodoroCount,
               scheduled_start_min as scheduledStartMin, scheduled_duration_min as scheduledDurationMin,
               bucket, waiting_on as waitingOn, waiting_since as waitingSince, follow_up_date as followUpDate,
               is_trashed as isTrashed, created_at as createdAt, updated_at as updatedAt
        FROM tasks
        WHERE area_id IS NULL AND project_id IS NULL AND parent_task_id IS NULL
          AND due_date IS NULL AND is_trashed = 0 AND is_completed = 0
        ORDER BY sort_order ASC, id ASC
        """,
    )
    fun observeInboxSlim(): Flow<List<SlimTask>>

    @Query(
        """
        SELECT id, title, workspace_id as workspaceId, area_id as areaId, project_id as projectId,
               section_id as sectionId, parent_task_id as parentTaskId, due_date as dueDate,
               due_time as dueTime, all_day as allDay, recurrence_rule as recurrenceRule,
               recurrence_basis as recurrenceBasis, priority, is_starred as isStarred,
               is_completed as isCompleted, completed_at as completedAt, estimated_minutes as estimatedMinutes,
               sort_order as sortOrder, my_day_date as myDayDate, pomodoro_count as pomodoroCount,
               scheduled_start_min as scheduledStartMin, scheduled_duration_min as scheduledDurationMin,
               bucket, waiting_on as waitingOn, waiting_since as waitingSince, follow_up_date as followUpDate,
               is_trashed as isTrashed, created_at as createdAt, updated_at as updatedAt
        FROM tasks
        WHERE project_id = :projectId AND is_trashed = 0
        ORDER BY sort_order ASC, id ASC
        """,
    )
    fun observeByProjectSlim(projectId: String): Flow<List<SlimTask>>

    @Query(
        """
        SELECT id, title, workspace_id as workspaceId, area_id as areaId, project_id as projectId,
               section_id as sectionId, parent_task_id as parentTaskId, due_date as dueDate,
               due_time as dueTime, all_day as allDay, recurrence_rule as recurrenceRule,
               recurrence_basis as recurrenceBasis, priority, is_starred as isStarred,
               is_completed as isCompleted, completed_at as completedAt, estimated_minutes as estimatedMinutes,
               sort_order as sortOrder, my_day_date as myDayDate, pomodoro_count as pomodoroCount,
               scheduled_start_min as scheduledStartMin, scheduled_duration_min as scheduledDurationMin,
               bucket, waiting_on as waitingOn, waiting_since as waitingSince, follow_up_date as followUpDate,
               is_trashed as isTrashed, created_at as createdAt, updated_at as updatedAt
        FROM tasks
        WHERE area_id = :areaId AND project_id IS NULL AND is_trashed = 0
        ORDER BY sort_order ASC, id ASC
        """,
    )
    fun observeByAreaSlim(areaId: String): Flow<List<SlimTask>>

    @Query(
        """
        SELECT id, title, workspace_id as workspaceId, area_id as areaId, project_id as projectId,
               section_id as sectionId, parent_task_id as parentTaskId, due_date as dueDate,
               due_time as dueTime, all_day as allDay, recurrence_rule as recurrenceRule,
               recurrence_basis as recurrenceBasis, priority, is_starred as isStarred,
               is_completed as isCompleted, completed_at as completedAt, estimated_minutes as estimatedMinutes,
               sort_order as sortOrder, my_day_date as myDayDate, pomodoro_count as pomodoroCount,
               scheduled_start_min as scheduledStartMin, scheduled_duration_min as scheduledDurationMin,
               bucket, waiting_on as waitingOn, waiting_since as waitingSince, follow_up_date as followUpDate,
               is_trashed as isTrashed, created_at as createdAt, updated_at as updatedAt
        FROM tasks
        WHERE parent_task_id = :parentId AND is_trashed = 0
        ORDER BY sort_order ASC, id ASC
        """,
    )
    fun observeSubtasksSlim(parentId: String): Flow<List<SlimTask>>

    @Query(
        """
        SELECT id, title, workspace_id as workspaceId, area_id as areaId, project_id as projectId,
               section_id as sectionId, parent_task_id as parentTaskId, due_date as dueDate,
               due_time as dueTime, all_day as allDay, recurrence_rule as recurrenceRule,
               recurrence_basis as recurrenceBasis, priority, is_starred as isStarred,
               is_completed as isCompleted, completed_at as completedAt, estimated_minutes as estimatedMinutes,
               sort_order as sortOrder, my_day_date as myDayDate, pomodoro_count as pomodoroCount,
               scheduled_start_min as scheduledStartMin, scheduled_duration_min as scheduledDurationMin,
               bucket, waiting_on as waitingOn, waiting_since as waitingSince, follow_up_date as followUpDate,
               is_trashed as isTrashed, created_at as createdAt, updated_at as updatedAt
        FROM tasks
        WHERE my_day_date = :date AND is_trashed = 0
        ORDER BY sort_order ASC, id ASC
        """,
    )
    fun observeMyDaySlim(date: String): Flow<List<SlimTask>>

    @Query(
        """
        SELECT id, title, workspace_id as workspaceId, area_id as areaId, project_id as projectId,
               section_id as sectionId, parent_task_id as parentTaskId, due_date as dueDate,
               due_time as dueTime, all_day as allDay, recurrence_rule as recurrenceRule,
               recurrence_basis as recurrenceBasis, priority, is_starred as isStarred,
               is_completed as isCompleted, completed_at as completedAt, estimated_minutes as estimatedMinutes,
               sort_order as sortOrder, my_day_date as myDayDate, pomodoro_count as pomodoroCount,
               scheduled_start_min as scheduledStartMin, scheduled_duration_min as scheduledDurationMin,
               bucket, waiting_on as waitingOn, waiting_since as waitingSince, follow_up_date as followUpDate,
               is_trashed as isTrashed, created_at as createdAt, updated_at as updatedAt
        FROM tasks
        WHERE is_trashed = 1
        ORDER BY trashed_at DESC, id ASC
        """,
    )
    fun observeTrashSlim(): Flow<List<SlimTask>>

    @Query("SELECT COUNT(*) FROM tasks WHERE is_trashed = 0 AND is_completed = 0")
    fun observeActiveTaskCount(): Flow<Int>

    @Query("SELECT * FROM tasks WHERE is_trashed = 1")
    suspend fun getTrashedTasks(): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE is_trashed = 1 AND trashed_at < :cutoffIso")
    suspend fun getTrashedTasksOlderThan(cutoffIso: String): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE parent_task_id = :parentId AND is_trashed = 0")
    suspend fun getSubtasks(parentId: String): List<TaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(task: TaskEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(tasks: List<TaskEntity>)

    @Update
    suspend fun update(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteById(id: String)
}
