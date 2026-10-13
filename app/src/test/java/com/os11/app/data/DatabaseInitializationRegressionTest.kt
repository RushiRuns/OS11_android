package com.os11.app.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.os11.app.data.db.OS11Database
import com.os11.app.data.repository.CountsRepositoryImpl
import com.os11.app.data.repository.TaskRepositoryImpl
import com.os11.app.data.sync.TrackedWriter
import com.os11.app.service.task.TaskService
import com.os11.app.service.undo.UndoManager
import com.os11.domain.id.DefaultIdGenerator
import com.os11.domain.id.DeterministicIds
import com.os11.domain.logic.sync.DefaultHlcClock
import com.os11.domain.result.AppResult
import com.os11.domain.time.TestClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Regression test for clean-install bug where the database was not automatically
 * seeded on creation, leading to foreign key violation on task creation and missing defaults.
 */
@RunWith(RobolectricTestRunner::class)
class DatabaseInitializationRegressionTest {

    private lateinit var db: OS11Database
    private lateinit var taskService: TaskService
    private lateinit var countsRepository: CountsRepositoryImpl

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        // Build database purely via OS11Database.buildInMemory
        // without manually calling DatabaseInitializer.seedDefaults
        db = OS11Database.buildInMemory(context)

        val testClock = TestClock(1_000_000L)
        val hlcClock = DefaultHlcClock(testClock, "device-test")
        val idGenerator = DefaultIdGenerator()
        val trackedWriter = TrackedWriter(db, hlcClock, testClock, idGenerator)
        val taskRepository = TaskRepositoryImpl(db, trackedWriter, testClock)
        countsRepository = CountsRepositoryImpl(db)
        taskService = TaskService(
            taskRepository = taskRepository,
            countsRepository = countsRepository,
            undoManager = UndoManager(),
            clock = testClock,
            idGenerator = idGenerator,
            ioDispatcher = Dispatchers.Unconfined,
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun database_is_automatically_seeded_with_default_workspace_on_clean_launch() = runBlocking {
        // Query workspace directly - must be seeded automatically without manual intervention
        val defaultWorkspace = db.workspaceDao().getById(DeterministicIds.defaultWorkspaceId())
        assertNotNull("Default workspace must be auto-seeded on database creation", defaultWorkspace)
        assertEquals("Personal", defaultWorkspace?.name)

        val defaultArea = db.areaDao().getById(DeterministicIds.defaultAreaId())
        assertNotNull("Default area must be auto-seeded on database creation", defaultArea)

        val isMyDayEnabled = db.moduleDao().isEnabled("my_day")
        assertEquals(true, isMyDayEnabled)

        val onboardingSetting = db.settingDao().getValue("onboarding_completed")
        assertNotNull("Default settings must be auto-seeded", onboardingSetting)
    }

    @Test
    fun creating_task_on_clean_install_succeeds_without_foreign_key_violation() = runBlocking {
        // Creating a task on a fresh database using the default workspace must succeed
        val result = taskService.createTask(
            title = "First User Task",
            workspaceId = DeterministicIds.defaultWorkspaceId(),
        )

        assertTrue(
            "Task creation must succeed on fresh install rather than failing with foreign key violation",
            result is AppResult.Ok,
        )
    }

    @Test
    fun browse_counts_on_empty_tasks_table_evaluates_cleanly_without_null_crash() = runBlocking {
        val counts = countsRepository.observeBrowseCounts("2026-10-12").first()
        assertEquals(0, counts.inboxCount)
        assertEquals(0, counts.myDayCount)
        assertEquals(0, counts.plannedCount)
        assertEquals(0, counts.completedCount)
        assertEquals(0, counts.trashCount)
    }
}
