package com.os11.app.service.dashboard

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.os11.app.data.db.DatabaseInitializer
import com.os11.app.data.db.OS11Database
import com.os11.app.data.repository.DashboardRepositoryImpl
import com.os11.app.data.seed.SeedGenerator
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DashboardPerformanceTest {

    private lateinit var db: OS11Database

    @Before
    fun setup() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = OS11Database.buildInMemory(context)
        DatabaseInitializer.seedDefaults(db)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun dashboard_aggregate_queries_on_5000_seeded_tasks_complete_under_300ms() = runBlocking {
        // Seed 5,000 tasks
        SeedGenerator.generateSeed(db, taskCount = 5000)

        val repo = DashboardRepositoryImpl(db)
        val today = "2026-10-11"
        val startIso = "2026-09-11T00:00:00.000Z"
        val endIso = "2026-10-11T23:59:59.999Z"

        val queryStartNs = System.nanoTime()

        // 1. Overview Stats query
        val overview = repo.observeOverviewStats(startIso, endIso, today).first()
        // 2. Priority Distribution query
        val priorities = repo.observePriorityDistribution(startIso, endIso).first()
        // 3. Daily Completion query
        val daily = repo.observeDailyCompletions(startIso, endIso).first()

        val queryElapsedMs = (System.nanoTime() - queryStartNs) / 1_000_000

        println("Dashboard queries on 5,000 tasks took $queryElapsedMs ms")

        assertTrue(
            "Dashboard queries across 5,000 tasks must load under 300 ms (took $queryElapsedMs ms)",
            queryElapsedMs < 300,
        )

        assertTrue("Total tasks should be 5,000", overview.totalTasks == 5000)
        assertTrue("Priority distribution should return items", priorities.isNotEmpty())
        assertTrue("Daily completions should return items", daily.isNotEmpty())
    }
}
