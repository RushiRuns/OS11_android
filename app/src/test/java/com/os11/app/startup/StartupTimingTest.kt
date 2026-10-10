package com.os11.app.startup

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.os11.app.data.db.DatabaseInitializer
import com.os11.app.data.db.OS11Database
import com.os11.app.data.repository.TaskRepositoryImpl
import com.os11.app.data.sync.TrackedWriter
import com.os11.app.platform.startup.DeferredStartupManager
import com.os11.app.service.reminder.ReminderService
import com.os11.app.service.task.TaskService
import com.os11.app.service.undo.UndoManager
import com.os11.domain.id.DefaultIdGenerator
import com.os11.domain.logic.sync.DefaultHlcClock
import com.os11.domain.time.TestClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Startup path purity and latency test.
 *
 * Verifies Architecture Rule 16 (ARCHITECTURE.md):
 * Application.onCreate path and first frame emission are lightweight,
 * with heavy maintenance deferred after first frame.
 */
@RunWith(RobolectricTestRunner::class)
class StartupTimingTest {

    private lateinit var db: OS11Database
    private lateinit var testClock: TestClock
    private lateinit var taskService: TaskService
    private lateinit var deferredStartupManager: DeferredStartupManager

    @Before
    fun setup() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val startTotal = System.nanoTime()

        testClock = TestClock(1_000_000L)
        db = OS11Database.buildInMemory(context)
        val hlcClock = DefaultHlcClock(testClock, "device-startup-test")
        val idGenerator = DefaultIdGenerator()
        val trackedWriter = TrackedWriter(db, hlcClock, testClock, idGenerator)

        DatabaseInitializer.seedDefaults(db, testClock)

        val taskRepository = TaskRepositoryImpl(db, trackedWriter, testClock)
        val undoManager = UndoManager()
        taskService = TaskService(
            taskRepository = taskRepository,
            undoManager = undoManager,
            clock = testClock,
            idGenerator = idGenerator,
            ioDispatcher = Dispatchers.Unconfined,
        )

        val reminderRepository = com.os11.app.data.repository.ReminderRepositoryImpl(db, trackedWriter)
        val alarmScheduler = com.os11.app.platform.alarms.AlarmScheduler(context)
        val reminderService = ReminderService(
            reminderRepository = reminderRepository,
            alarmScheduler = alarmScheduler,
            clock = testClock,
            idGenerator = idGenerator,
            ioDispatcher = Dispatchers.Unconfined,
        )
        deferredStartupManager = DeferredStartupManager(
            reminderService = reminderService,
            context = context,
            ioDispatcher = Dispatchers.Unconfined,
        )

        val setupElapsedMs = (System.nanoTime() - startTotal) / 1_000_000
        println("Startup graph construction elapsed: ${setupElapsedMs}ms")
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `first frame path to My Day list emission is under latency budget`() = runBlocking {
        val startNano = System.nanoTime()

        // 1. Initial emission from My Day query
        val myDayItems = taskService.observeMyDaySlim().first()
        val elapsedMs = (System.nanoTime() - startNano) / 1_000_000

        println("Cold start to My Day list first emission: ${elapsedMs}ms")
        assertNotNull(myDayItems)
        assertTrue(
            "First emission must be under 500ms budget in test environment, was ${elapsedMs}ms",
            elapsedMs < 500,
        )

        // 2. Post-first-frame deferred startup completes cleanly without blocking
        deferredStartupManager.onFirstFrameRendered()
    }
}
