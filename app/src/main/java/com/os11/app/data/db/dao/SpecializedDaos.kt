package com.os11.app.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.RawQuery
import androidx.sqlite.db.SupportSQLiteQuery
import com.os11.app.data.db.entities.TaskEntity
import kotlinx.coroutines.flow.Flow

data class BrowseCounts(
    val inboxCount: Int,
    val myDayCount: Int,
    val plannedCount: Int,
    val importantCount: Int,
    val anytimeCount: Int,
    val somedayCount: Int,
    val waitingForCount: Int,
    val completedCount: Int,
    val trashCount: Int,
)

@Dao
internal interface CountsDao {

    @Query(
        """
        SELECT
            COALESCE(SUM(CASE WHEN area_id IS NULL AND project_id IS NULL AND parent_task_id IS NULL
                          AND due_date IS NULL AND is_trashed = 0 AND is_completed = 0 THEN 1 ELSE 0 END), 0) as inboxCount,
            COALESCE(SUM(CASE WHEN my_day_date = :today AND is_trashed = 0 AND is_completed = 0 THEN 1 ELSE 0 END), 0) as myDayCount,
            COALESCE(SUM(CASE WHEN due_date IS NOT NULL AND is_trashed = 0 AND is_completed = 0 THEN 1 ELSE 0 END), 0) as plannedCount,
            COALESCE(SUM(CASE WHEN is_starred = 1 AND is_trashed = 0 AND is_completed = 0 THEN 1 ELSE 0 END), 0) as importantCount,
            COALESCE(SUM(CASE WHEN bucket = 'anytime' AND is_trashed = 0 AND is_completed = 0 THEN 1 ELSE 0 END), 0) as anytimeCount,
            COALESCE(SUM(CASE WHEN bucket = 'someday' AND is_trashed = 0 AND is_completed = 0 THEN 1 ELSE 0 END), 0) as somedayCount,
            COALESCE(SUM(CASE WHEN waiting_since IS NOT NULL AND is_trashed = 0 AND is_completed = 0 THEN 1 ELSE 0 END), 0) as waitingForCount,
            COALESCE(SUM(CASE WHEN is_completed = 1 AND is_trashed = 0 THEN 1 ELSE 0 END), 0) as completedCount,
            COALESCE(SUM(CASE WHEN is_trashed = 1 THEN 1 ELSE 0 END), 0) as trashCount
        FROM tasks
        """,
    )
    fun observeBrowseCounts(today: String): Flow<BrowseCounts>
}

@Dao
internal interface SearchDao {
    @RawQuery(observedEntities = [TaskEntity::class])
    suspend fun searchTasks(query: SupportSQLiteQuery): List<TaskEntity>
}
