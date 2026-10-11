package com.os11.app.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

data class DashboardOverviewRaw(
    val totalTasks: Int,
    val completedTasks: Int,
    val pendingTasks: Int,
    val overdueTasks: Int,
)

data class PriorityCountRaw(val priority: Int, val count: Int)

data class DailyCountRaw(val date: String, val count: Int)

@Dao
internal interface DashboardDao {

    @Query(
        """
        SELECT
            COUNT(*) AS totalTasks,
            SUM(CASE WHEN is_completed = 1 THEN 1 ELSE 0 END) AS completedTasks,
            SUM(CASE WHEN is_completed = 0 THEN 1 ELSE 0 END) AS pendingTasks,
            SUM(CASE WHEN is_completed = 0 AND due_date IS NOT NULL AND due_date < :today THEN 1 ELSE 0 END) AS overdueTasks
        FROM tasks
        WHERE is_trashed = 0
          AND (:startDate IS NULL OR created_at >= :startDate OR (completed_at IS NOT NULL AND completed_at >= :startDate))
          AND (:endDate IS NULL OR created_at <= :endDate OR (completed_at IS NOT NULL AND completed_at <= :endDate))
        """,
    )
    fun observeOverviewStats(startDate: String?, endDate: String?, today: String): Flow<DashboardOverviewRaw>

    @Query(
        """
        SELECT
            priority,
            COUNT(*) AS count
        FROM tasks
        WHERE is_trashed = 0
          AND (:startDate IS NULL OR created_at >= :startDate OR (completed_at IS NOT NULL AND completed_at >= :startDate))
          AND (:endDate IS NULL OR created_at <= :endDate OR (completed_at IS NOT NULL AND completed_at <= :endDate))
        GROUP BY priority
        ORDER BY priority DESC
        """,
    )
    fun observePriorityDistribution(startDate: String?, endDate: String?): Flow<List<PriorityCountRaw>>

    @Query(
        """
        SELECT
            substr(completed_at, 1, 10) AS date,
            COUNT(*) AS count
        FROM tasks
        WHERE is_trashed = 0
          AND is_completed = 1
          AND completed_at IS NOT NULL
          AND (:startDate IS NULL OR completed_at >= :startDate)
          AND (:endDate IS NULL OR completed_at <= :endDate)
        GROUP BY substr(completed_at, 1, 10)
        ORDER BY date ASC
        """,
    )
    fun observeDailyCompletions(startDate: String?, endDate: String?): Flow<List<DailyCountRaw>>
}
