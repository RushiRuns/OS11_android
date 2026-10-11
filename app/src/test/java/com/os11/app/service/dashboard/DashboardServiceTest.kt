package com.os11.app.service.dashboard

import com.os11.app.data.db.dao.DailyCountRaw
import com.os11.app.data.db.dao.DashboardOverviewRaw
import com.os11.app.data.db.dao.PriorityCountRaw
import com.os11.app.data.repository.DashboardRepository
import com.os11.domain.result.AppResult
import com.os11.domain.time.Clock
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DashboardServiceTest {

    private val dashboardRepository: DashboardRepository = mockk()
    private val clock: Clock = mockk()
    private lateinit var dashboardService: DashboardService

    @Before
    fun setup() {
        every { clock.today() } returns "2026-10-11"
        every { clock.nowIso() } returns "2026-10-11T11:00:00.000Z"
        dashboardService = DashboardService(
            dashboardRepository = dashboardRepository,
            clock = clock,
            ioDispatcher = Dispatchers.Unconfined,
        )
    }

    @Test
    fun `observeDashboardData calculates overview stats and completion rate`() = runTest {
        val overview = DashboardOverviewRaw(
            totalTasks = 20,
            completedTasks = 15,
            pendingTasks = 5,
            overdueTasks = 2,
        )
        val priorities = listOf(
            PriorityCountRaw(priority = 4, count = 2),
            PriorityCountRaw(priority = 3, count = 4),
            PriorityCountRaw(priority = 2, count = 8),
            PriorityCountRaw(priority = 1, count = 4),
            PriorityCountRaw(priority = 0, count = 2),
        )
        val daily = listOf(
            DailyCountRaw(date = "2026-10-10", count = 3),
            DailyCountRaw(date = "2026-10-11", count = 5),
        )

        every { dashboardRepository.observeOverviewStats(any(), any(), any()) } returns flowOf(overview)
        every { dashboardRepository.observePriorityDistribution(any(), any()) } returns flowOf(priorities)
        every { dashboardRepository.observeDailyCompletions(any(), any()) } returns flowOf(daily)

        val data = dashboardService.observeDashboardData(DateRangeOption.SevenDays).first()

        assertEquals(20, data.stats.totalTasks)
        assertEquals(15, data.stats.completedTasks)
        assertEquals(5, data.stats.pendingTasks)
        assertEquals(2, data.stats.overdueTasks)
        assertEquals(75.0f, data.stats.completionRate, 0.01f)

        // Check Priorities breakdown
        val crit = data.priorities.first { it.priority == 4 }
        assertEquals("Critical", crit.label)
        assertEquals(2, crit.count)
        assertEquals(10.0f, crit.percentage, 0.01f)

        // Check 7 days trend points count
        assertEquals(7, data.dailyTrend.size)
        val todayPoint = data.dailyTrend.last()
        assertEquals("2026-10-11", todayPoint.date)
        assertEquals(5, todayPoint.count)

        // Check Heatmap has 84 days (12 weeks * 7 days)
        assertEquals(84, data.heatmapDays.size)
    }

    @Test
    fun `observeDashboardData handles zero tasks gracefully without NaN`() = runTest {
        val overview = DashboardOverviewRaw(
            totalTasks = 0,
            completedTasks = 0,
            pendingTasks = 0,
            overdueTasks = 0,
        )
        every { dashboardRepository.observeOverviewStats(any(), any(), any()) } returns flowOf(overview)
        every { dashboardRepository.observePriorityDistribution(any(), any()) } returns flowOf(emptyList())
        every { dashboardRepository.observeDailyCompletions(any(), any()) } returns flowOf(emptyList())

        val data = dashboardService.observeDashboardData(DateRangeOption.ThirtyDays).first()

        assertEquals(0, data.stats.totalTasks)
        assertEquals(0f, data.stats.completionRate, 0.01f)
        assertEquals(30, data.dailyTrend.size)
        assertEquals(84, data.heatmapDays.size)
    }

    @Test
    fun `exportDashboardToCsv returns valid formatted CSV`() = runTest {
        val overview = DashboardOverviewRaw(
            totalTasks = 10,
            completedTasks = 8,
            pendingTasks = 2,
            overdueTasks = 1,
        )
        every { dashboardRepository.observeOverviewStats(any(), any(), any()) } returns flowOf(overview)
        every { dashboardRepository.observePriorityDistribution(any(), any()) } returns flowOf(emptyList())
        every { dashboardRepository.observeDailyCompletions(any(), any()) } returns flowOf(emptyList())

        val result = dashboardService.exportDashboardToCsv(DateRangeOption.SevenDays)

        assertTrue(result is AppResult.Ok)
        val csv = (result as AppResult.Ok).value

        assertTrue(csv.contains("Total Tasks,10"))
        assertTrue(csv.contains("Completed Tasks,8"))
        assertTrue(csv.contains("Completion Rate,80.0%"))
        assertTrue(csv.contains("Priority,Count,Percentage"))
        assertTrue(csv.contains("Date,Day,Completed Tasks"))
    }
}
