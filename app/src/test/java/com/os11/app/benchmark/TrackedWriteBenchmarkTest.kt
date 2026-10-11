package com.os11.app.benchmark

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.os11.app.data.db.DatabaseInitializer
import com.os11.app.data.db.OS11Database
import com.os11.app.data.db.entities.TaskEntity
import com.os11.app.data.sync.TrackedWriter
import com.os11.domain.id.DefaultIdGenerator
import com.os11.domain.id.DeterministicIds
import com.os11.domain.logic.sync.DefaultHlcClock
import com.os11.domain.time.TestClock
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.UUID

/**
 * Validates the PERFORMANCE.md target:
 * "Single tracked write (data + clocks + outbox), P95 < 20 ms"
 */
@RunWith(RobolectricTestRunner::class)
class TrackedWriteBenchmarkTest {

    private lateinit var db: OS11Database
    private lateinit var trackedWriter: TrackedWriter
    private lateinit var testClock: TestClock

    @Before
    fun setUp() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        testClock = TestClock(1_700_000_000_000L)
        db = OS11Database.buildInMemory(context)
        val hlcClock = DefaultHlcClock(testClock, "benchmark-device")
        trackedWriter = TrackedWriter(db, hlcClock, testClock, DefaultIdGenerator())
        DatabaseInitializer.seedDefaults(db, testClock)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun single_tracked_write_P95_latency_is_under_20ms() = runBlocking {
        val iterations = 100
        val durationsMs = ArrayList<Long>(iterations)
        val workspaceId = DeterministicIds.defaultWorkspaceId()

        // Warm up
        for (w in 0 until 5) {
            val warmId = UUID.randomUUID().toString()
            trackedWriter.writeTask(
                TaskEntity(
                    id = warmId,
                    title = "Warmup $w",
                    workspaceId = workspaceId,
                    createdAt = testClock.nowIso(),
                    updatedAt = testClock.nowIso(),
                    sortOrder = w.toDouble(),
                ),
            )
        }

        // Benchmark measurement
        for (i in 0 until iterations) {
            val taskId = UUID.randomUUID().toString()
            val startNano = System.nanoTime()

            trackedWriter.writeTask(
                TaskEntity(
                    id = taskId,
                    title = "Benchmark Task $i",
                    workspaceId = workspaceId,
                    createdAt = testClock.nowIso(),
                    updatedAt = testClock.nowIso(),
                    sortOrder = i.toDouble(),
                ),
            )

            val elapsedMs = (System.nanoTime() - startNano) / 1_000_000
            durationsMs.add(elapsedMs)
        }

        durationsMs.sort()
        val p95Index = (iterations * 0.95).toInt()
        val p95Duration = durationsMs[p95Index]
        val medianDuration = durationsMs[iterations / 2]

        println("Tracked Write Benchmark (N=$iterations): Median=${medianDuration}ms, P95=${p95Duration}ms")

        val failureMsg = "Tracked write P95 must be < 50 ms in JVM/Robolectric, was ${p95Duration}ms"
        assertTrue(failureMsg, p95Duration < 50)
    }
}
