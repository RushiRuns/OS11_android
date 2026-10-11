package com.os11.app.service.dashboard

/**
 * Service-level DTOs representing dashboard analytics, preserving Konsist UI boundaries.
 */
@Suppress("MagicNumber")
enum class DateRangeOption(val key: String, val label: String, val days: Int?) {
    SevenDays("7D", "7 Days", 7),
    ThirtyDays("30D", "30 Days", 30),
    NinetyDays("90D", "90 Days", 90),
    AllTime("ALL", "All Time", null),
}

data class DashboardOverviewStats(
    val totalTasks: Int,
    val completedTasks: Int,
    val pendingTasks: Int,
    val overdueTasks: Int,
    val completionRate: Float,
)

data class PriorityDistributionItem(val priority: Int, val label: String, val count: Int, val percentage: Float)

data class DailyTrendPoint(val date: String, val dayLabel: String, val count: Int)

data class HeatmapDay(val date: String, val count: Int, val level: Int, val dayOfWeek: Int)

data class DashboardData(
    val dateRange: DateRangeOption,
    val stats: DashboardOverviewStats,
    val priorities: List<PriorityDistributionItem>,
    val dailyTrend: List<DailyTrendPoint>,
    val heatmapDays: List<HeatmapDay>,
)
