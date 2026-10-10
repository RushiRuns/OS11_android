package com.os11.app.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.os11.app.data.db.DatabaseInitializer
import com.os11.app.data.db.OS11Database
import com.os11.app.data.repository.CountsRepositoryImpl
import com.os11.app.data.seed.SeedGenerator
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SeedGeneratorTest {

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
    fun seed_generator_loads_1000_tasks_cleanly() = runBlocking {
        SeedGenerator.generateSeed(db, taskCount = 1000)

        val taskCount = db.taskDao().observeActiveTaskCount().first()
        // Some tasks are generated as completed
        assertTrue("Active task count should be > 500", taskCount > 500)

        val counts = CountsRepositoryImpl(db).observeBrowseCounts("2026-10-10").first()
        assertTrue("Completed tasks should exist", counts.completedCount > 0)
        assertTrue("My Day tasks should exist", counts.myDayCount > 0)
    }

    @Test
    fun seed_generator_loads_5000_tasks_cleanly() = runBlocking {
        val startNs = System.nanoTime()
        SeedGenerator.generateSeed(db, taskCount = 5000)
        val loadElapsedMs = (System.nanoTime() - startNs) / 1_000_000

        println("5,000 task seed generation and insertion took $loadElapsedMs ms")

        val queryStartNs = System.nanoTime()
        val counts = CountsRepositoryImpl(db).observeBrowseCounts("2026-10-10").first()
        val queryElapsedMs = (System.nanoTime() - queryStartNs) / 1_000_000

        println("Counts query across 5,000 tasks took $queryElapsedMs ms")
        assertTrue("Counts query on 5,000 tasks should be fast (< 200 ms)", queryElapsedMs < 200)

        val allSlim = db.taskDao().observeAllSlim().first()
        assertEquals(5000, allSlim.size)
    }
}
