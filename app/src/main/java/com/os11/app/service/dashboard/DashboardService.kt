package com.os11.app.service.dashboard

import com.os11.app.data.repository.DashboardRepository
import com.os11.app.di.IoDispatcher
import com.os11.domain.result.AppError
import com.os11.domain.result.AppResult
import com.os11.domain.time.Clock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@Suppress("LongMethod", "TooGenericExceptionCaught", "MagicNumber")
class DashboardService @Inject constructor(
    private val dashboardRepository: DashboardRepository,
    private val clock: Clock,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {

    fun observeDashboardData(range: DateRangeOption, today: String = clock.today()): Flow<DashboardData> {
        val todayDate = parseLocalDate(today)
        val endDateIso = "${today}T23:59:59.999Z"

        val startDateIso = range.days?.let { days ->
            val start = todayDate.minusDays((days - 1).toLong())
            "${start}T00:00:00.000Z"
        }

        return combine(
            dashboardRepository.observeOverviewStats(startDateIso, endDateIso, today),
            dashboardRepository.observePriorityDistribution(startDateIso, endDateIso),
            dashboardRepository.observeDailyCompletions(startDateIso, endDateIso),
        ) { overviewRaw, priorityRaws, dailyRaws ->
            val total = overviewRaw.totalTasks
            val completed = overviewRaw.completedTasks
            val rate = if (total > 0) (completed.toFloat() / total) * 100f else 0f

            val overviewStats = DashboardOverviewStats(
                totalTasks = total,
                completedTasks = completed,
                pendingTasks = overviewRaw.pendingTasks,
                overdueTasks = overviewRaw.overdueTasks,
                completionRate = rate,
            )

            val priorityCounts = priorityRaws.associate { it.priority to it.count }
            val priorities = listOf(
                4 to "Critical",
                3 to "High",
                2 to "Medium",
                1 to "Low",
                0 to "None",
            ).map { (pVal, pLabel) ->
                val pCount = priorityCounts[pVal] ?: 0
                val pPercent = if (total > 0) (pCount.toFloat() / total) * 100f else 0f
                PriorityDistributionItem(
                    priority = pVal,
                    label = pLabel,
                    count = pCount,
                    percentage = pPercent,
                )
            }

            val dailyCountsMap = dailyRaws.associate { it.date to it.count }

            val trendDaysCount = when (range) {
                DateRangeOption.SevenDays -> 7
                DateRangeOption.ThirtyDays -> 30
                DateRangeOption.NinetyDays -> 90
                DateRangeOption.AllTime -> 30
            }

            val trendPoints = (trendDaysCount - 1 downTo 0).map { daysAgo ->
                val d = todayDate.minusDays(daysAgo.toLong())
                val dStr = d.toString()
                val count = dailyCountsMap[dStr] ?: 0
                val label = if (trendDaysCount <= 7) {
                    d.dayOfWeek.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
                } else {
                    "${d.monthValue}/${d.dayOfMonth}"
                }
                DailyTrendPoint(date = dStr, dayLabel = label, count = count)
            }

            val heatmapDays = buildHeatmapDays(todayDate, dailyCountsMap)

            DashboardData(
                dateRange = range,
                stats = overviewStats,
                priorities = priorities,
                dailyTrend = trendPoints,
                heatmapDays = heatmapDays,
            )
        }
    }

    suspend fun exportDashboardToCsv(range: DateRangeOption, today: String = clock.today()): AppResult<String> =
        withContext(ioDispatcher) {
            try {
                val data = observeDashboardData(range, today).first()
                val nowIso = clock.nowIso()

                val builder = StringBuilder()
                builder.append("Metric,Value\n")
                builder.append("Date Range,").append(escapeCsv(range.label)).append("\n")
                builder.append("Exported At,").append(escapeCsv(nowIso)).append("\n")
                builder.append("Total Tasks,").append(data.stats.totalTasks).append("\n")
                builder.append("Completed Tasks,").append(data.stats.completedTasks).append("\n")
                builder.append("Pending Tasks,").append(data.stats.pendingTasks).append("\n")
                builder.append("Overdue Tasks,").append(data.stats.overdueTasks).append("\n")
                builder.append(
                    "Completion Rate,",
                ).append(String.format(Locale.US, "%.1f%%", data.stats.completionRate)).append("\n\n")

                builder.append("Priority,Count,Percentage\n")
                for (p in data.priorities) {
                    val row = listOf(
                        p.label,
                        p.count.toString(),
                        String.format(Locale.US, "%.1f%%", p.percentage),
                    ).joinToString(",") { escapeCsv(it) }
                    builder.append(row).append("\n")
                }
                builder.append("\n")

                builder.append("Date,Day,Completed Tasks\n")
                for (point in data.dailyTrend) {
                    val row = listOf(
                        point.date,
                        point.dayLabel,
                        point.count.toString(),
                    ).joinToString(",") { escapeCsv(it) }
                    builder.append(row).append("\n")
                }

                AppResult.ok(builder.toString())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppResult.err(AppError.Unexpected(e))
            }
        }

    private fun buildHeatmapDays(todayDate: LocalDate, dailyCountsMap: Map<String, Int>): List<HeatmapDay> {
        val totalDays = HEATMAP_WEEKS * DAYS_IN_WEEK
        val currentDayOfWeek = todayDate.dayOfWeek.value // 1 (Mon) .. 7 (Sun)
        val endOfGridWeek = todayDate.plusDays((DAYS_IN_WEEK - currentDayOfWeek).toLong())
        val startOfGrid = endOfGridWeek.minusDays((totalDays - 1).toLong())

        val result = ArrayList<HeatmapDay>(totalDays)
        var cursor = startOfGrid
        repeat(totalDays) {
            val dStr = cursor.toString()
            val count = if (cursor.isAfter(todayDate)) 0 else (dailyCountsMap[dStr] ?: 0)
            val level = when {
                cursor.isAfter(todayDate) -> 0
                count == 0 -> 0
                count == 1 -> 1
                count in 2..3 -> 2
                count in 4..6 -> 3
                else -> 4
            }
            result.add(
                HeatmapDay(
                    date = dStr,
                    count = count,
                    level = level,
                    dayOfWeek = cursor.dayOfWeek.value,
                ),
            )
            cursor = cursor.plusDays(1)
        }
        return result
    }

    private fun parseLocalDate(dateString: String): LocalDate = try {
        LocalDate.parse(dateString)
    } catch (_: Exception) {
        LocalDate.now()
    }

    private fun escapeCsv(value: String): String {
        val escaped = value.replace("\"", "\"\"")
        return "\"$escaped\""
    }

    companion object {
        const val HEATMAP_WEEKS = 12
        const val DAYS_IN_WEEK = 7
    }
}
