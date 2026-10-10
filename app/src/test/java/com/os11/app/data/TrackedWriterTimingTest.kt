package com.os11.app.data

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

@RunWith(RobolectricTestRunner::class)
class TrackedWriterTimingTest {

    private lateinit var db: OS11Database
    private lateinit var testClock: TestClock
    private lateinit var hlcClock: DefaultHlcClock
    private lateinit var trackedWriter: TrackedWriter

    @Before
    fun setup() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = OS11Database.buildInMemory(context)
        testClock = TestClock(1_000_000L)
        hlcClock = DefaultHlcClock(testClock, "device01")
        trackedWriter = TrackedWriter(db, hlcClock, testClock, DefaultIdGenerator())
        DatabaseInitializer.seedDefaults(db, testClock)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun tracked_write_p95_latency_is_under_budget() = runBlocking {
        val workspaceId = DeterministicIds.defaultWorkspaceId()
        val latencies = mutableListOf<Long>()
        val iterations = 100

        // Warm up
        for (i in 1..10) {
            val warmTask = TaskEntity(
                id = UUID.randomUUID().toString(),
                title = "Warmup $i",
                workspaceId = workspaceId,
                createdAt = testClock.nowIso(),
                updatedAt = testClock.nowIso(),
            )
            trackedWriter.writeTask(warmTask)
        }

        // Measure
        for (i in 1..iterations) {
            val task = TaskEntity(
                id = UUID.randomUUID().toString(),
                title = "Bench task $i",
                notes = "<p>Notes for task $i</p>",
                workspaceId = workspaceId,
                priority = i % 5,
                createdAt = testClock.nowIso(),
                updatedAt = testClock.nowIso(),
            )

            val start = System.nanoTime()
            trackedWriter.writeTask(task)
            val elapsedMs = (System.nanoTime() - start) / 1_000_000
            latencies.add(elapsedMs)
        }

        latencies.sort()
        val p95Index = (iterations * 0.95).toInt()
        val p95Latency = latencies[p95Index]
        val medianLatency = latencies[iterations / 2]

        println("Tracked write timing: median=${medianLatency}ms, P95=${p95Latency}ms")
        assertTrue("Tracked write P95 must be < 50ms, was ${p95Latency}ms", p95Latency < 50)
    }
}
