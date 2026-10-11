package com.os11.app.data.repository

import com.os11.app.data.db.OS11Database
import com.os11.app.data.db.dao.DailyCountRaw
import com.os11.app.data.db.dao.DashboardOverviewRaw
import com.os11.app.data.db.dao.PriorityCountRaw
import kotlinx.coroutines.flow.Flow

interface DashboardRepository {
    fun observeOverviewStats(startDate: String?, endDate: String?, today: String): Flow<DashboardOverviewRaw>
    fun observePriorityDistribution(startDate: String?, endDate: String?): Flow<List<PriorityCountRaw>>
    fun observeDailyCompletions(startDate: String?, endDate: String?): Flow<List<DailyCountRaw>>
}

internal class DashboardRepositoryImpl(private val db: OS11Database) : DashboardRepository {
    override fun observeOverviewStats(
        startDate: String?,
        endDate: String?,
        today: String,
    ): Flow<DashboardOverviewRaw> = db.dashboardDao().observeOverviewStats(startDate, endDate, today)

    override fun observePriorityDistribution(startDate: String?, endDate: String?): Flow<List<PriorityCountRaw>> =
        db.dashboardDao().observePriorityDistribution(startDate, endDate)

    override fun observeDailyCompletions(startDate: String?, endDate: String?): Flow<List<DailyCountRaw>> =
        db.dashboardDao().observeDailyCompletions(startDate, endDate)
}
