package com.os11.app.service.backup

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.os11.app.data.db.DatabaseInitializer
import com.os11.app.data.db.OS11Database
import com.os11.app.data.db.entities.AreaEntity
import com.os11.app.data.db.entities.ProjectEntity
import com.os11.app.data.db.entities.TagEntity
import com.os11.app.data.db.entities.TaskEntity
import com.os11.app.data.sync.TrackedWriter
import com.os11.domain.id.DefaultIdGenerator
import com.os11.domain.id.DeterministicIds
import com.os11.domain.logic.sync.DefaultHlcClock
import com.os11.domain.result.AppResult
import com.os11.domain.time.TestClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream

@RunWith(RobolectricTestRunner::class)
@Suppress("LongMethod")
class BackupServiceTest {

    private lateinit var db1: OS11Database
    private lateinit var db2: OS11Database
    private lateinit var clock: TestClock
    private lateinit var hlcClock: DefaultHlcClock
    private lateinit var trackedWriter1: TrackedWriter
    private lateinit var trackedWriter2: TrackedWriter
    private lateinit var backupService1: BackupService
    private lateinit var backupService2: BackupService
    private val idGenerator = DefaultIdGenerator()

    @Before
    fun setup() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db1 = OS11Database.buildInMemory(context)
        db2 = OS11Database.buildInMemory(context)

        clock = TestClock(1_700_000_000_000L)
        hlcClock = DefaultHlcClock(clock, "device-test-1")

        trackedWriter1 = TrackedWriter(db1, hlcClock, clock, idGenerator)
        trackedWriter2 = TrackedWriter(db2, DefaultHlcClock(clock, "device-test-2"), clock, idGenerator)

        DatabaseInitializer.seedDefaults(db1, clock)
        DatabaseInitializer.seedDefaults(db2, clock)

        backupService1 = BackupService(
            db = db1,
            trackedWriter = trackedWriter1,
            clock = clock,
            idGenerator = idGenerator,
            ioDispatcher = Dispatchers.Unconfined,
        )

        backupService2 = BackupService(
            db = db2,
            trackedWriter = trackedWriter2,
            clock = clock,
            idGenerator = idGenerator,
            ioDispatcher = Dispatchers.Unconfined,
        )
    }

    @After
    fun tearDown() {
        db1.close()
        db2.close()
    }

    @Test
    fun round_trip_export_fresh_db_import_and_compare() = runBlocking {
        val defaultWs = DeterministicIds.defaultWorkspaceId()

        // 1. Populate DB1 with rich data via TrackedWriter
        val area = trackedWriter1.writeArea(
            AreaEntity(
                id = "area-work-1",
                workspaceId = defaultWs,
                name = "Engineering",
                sortOrder = 1.0,
                createdAt = clock.nowIso(),
                updatedAt = clock.nowIso(),
            ),
        )

        val project = trackedWriter1.writeProject(
            ProjectEntity(
                id = "project-app-1",
                workspaceId = defaultWs,
                areaId = area.id,
                name = "OS11 Android",
                status = "active",
                sortOrder = 1.0,
                defaultView = "list",
                createdAt = clock.nowIso(),
                updatedAt = clock.nowIso(),
            ),
        )

        val tag = trackedWriter1.writeTag(
            TagEntity(
                id = "tag-urgent-1",
                name = "Urgent",
                color = "#ff4d4d",
                sortOrder = 1.0,
                createdAt = clock.nowIso(),
            ),
        )

        val task1 = trackedWriter1.writeTask(
            TaskEntity(
                id = "task-101",
                title = "Ship Phase 17",
                notes = "Data portability & backup",
                workspaceId = defaultWs,
                areaId = area.id,
                projectId = project.id,
                priority = 3,
                isStarred = true,
                dueDate = "2026-10-15",
                bucket = "today",
                createdAt = clock.nowIso(),
                updatedAt = clock.nowIso(),
            ),
        )

        trackedWriter1.writeTaskTag(task1.id, tag.id)

        // 2. Export DB1 to JSON
        val exportResult = backupService1.exportToJson()
        assertTrue(exportResult.isOk)
        val jsonPayload = (exportResult as AppResult.Ok).value

        // 3. Import JSON into fresh DB2
        val importResult = backupService2.importFromJson(jsonPayload)
        assertTrue(importResult.isOk)
        val counts = (importResult as AppResult.Ok).value
        assertEquals(1, counts.tasksCount)
        assertEquals(1, counts.projectsCount)
        assertEquals(2, counts.areasCount)

        // 4. Verify DB2 contents match DB1
        val importedTask = db2.taskDao().getById("task-101")
        assertNotNull(importedTask)
        assertEquals("Ship Phase 17", importedTask?.title)
        assertEquals("Data portability & backup", importedTask?.notes)
        assertEquals(3, importedTask?.priority)
        assertEquals(true, importedTask?.isStarred)
        assertEquals(project.id, importedTask?.projectId)
        assertEquals(area.id, importedTask?.areaId)

        val importedProject = db2.projectDao().getById("project-app-1")
        assertNotNull(importedProject)
        assertEquals("OS11 Android", importedProject?.name)
        assertEquals(area.id, importedProject?.areaId)

        val importedArea = db2.areaDao().getById("area-work-1")
        assertNotNull(importedArea)
        assertEquals("Engineering", importedArea?.name)

        val importedTag = db2.tagDao().getById("tag-urgent-1")
        assertNotNull(importedTag)
        assertEquals("Urgent", importedTag?.name)

        // Verify task-tag association
        val taskTags = db2.taskTagDao().getTagsForTasks(listOf(task1.id))
        assertEquals(1, taskTags.size)
        assertEquals(tag.id, taskTags.first().tagId)

        // Verify outbox rows and sync field clocks were created on DB2 (not bypassed!)
        val outboxRows = db2.syncQueueDao().getNextBatch(50)
        assertTrue("Imported rows must create sync outbox queue entries", outboxRows.isNotEmpty())

        val taskClocks = db2.syncFieldClockDao().getClocksForEntity("tasks", task1.id)
        assertTrue("Imported tasks must have stamped sync field clocks", taskClocks.isNotEmpty())

        // Verify version history logged the events
        val historyEntries = db2.versionHistoryDao().getRecent(10)
        assertTrue(
            "Version history must record the import event",
            historyEntries.any { it.entityType == "backup_import" },
        )
    }

    @Test
    fun import_desktop_os11_json_normalizes_legacy_lists_and_list_groups() = runBlocking {
        // Desktop export with legacy list_groups, lists, and task.list_id
        val desktopJson = """
        {
            "version": 1,
            "exported_at": "2026-09-01T12:00:00Z",
            "source": "os11_desktop",
            "list_groups": [
                {
                    "id": "lg-work",
                    "name": "Work Group",
                    "sort_order": 1.0,
                    "created_at": "2026-09-01T10:00:00Z",
                    "updated_at": "2026-09-01T10:00:00Z"
                }
            ],
            "lists": [
                {
                    "id": "list-marketing",
                    "name": "Marketing Campaign",
                    "list_group_id": "lg-work",
                    "sort_order": 2.0,
                    "default_view": "board",
                    "created_at": "2026-09-01T10:05:00Z",
                    "updated_at": "2026-09-01T10:05:00Z"
                }
            ],
            "tasks": [
                {
                    "id": "task-desktop-1",
                    "title": "Design Banner",
                    "description": "Legacy notes from desktop",
                    "list_id": "list-marketing",
                    "priority": 2,
                    "is_completed": false,
                    "created_at": "2026-09-01T11:00:00Z",
                    "updated_at": "2026-09-01T11:00:00Z"
                }
            ]
        }
        """.trimIndent()

        val importResult = backupService2.importFromJson(desktopJson)
        assertTrue(importResult.isOk)

        // Verify list_groups normalized to areas
        val area = db2.areaDao().getById("lg-work")
        assertNotNull("Desktop list_groups must import as areas", area)
        assertEquals("Work Group", area?.name)

        // Verify lists normalized to projects with area_id mapped
        val project = db2.projectDao().getById("list-marketing")
        assertNotNull("Desktop lists must import as projects", project)
        assertEquals("Marketing Campaign", project?.name)
        assertEquals("lg-work", project?.areaId)
        assertEquals("board", project?.defaultView)

        // Verify tasks mapped list_id to project_id and description to notes
        val task = db2.taskDao().getById("task-desktop-1")
        assertNotNull(task)
        assertEquals("Design Banner", task?.title)
        assertEquals("Legacy notes from desktop", task?.notes)
        assertEquals("list-marketing", task?.projectId)
        assertEquals(2, task?.priority)
    }

    @Test
    fun csv_export_adheres_to_rfc_4180() = runBlocking {
        val defaultWs = DeterministicIds.defaultWorkspaceId()

        // Create tasks with commas, newlines, and double quotes
        trackedWriter1.writeTask(
            TaskEntity(
                id = "csv-task-1",
                title = "Meeting with \"Executive\" Team, Q3",
                notes = "Line 1\nLine 2 with, commas and \"quotes\"",
                workspaceId = defaultWs,
                priority = 1,
                dueDate = "2026-10-20",
                createdAt = clock.nowIso(),
                updatedAt = clock.nowIso(),
            ),
        )

        val csvResult = backupService1.exportToCsv()
        assertTrue(csvResult.isOk)
        val csv = (csvResult as AppResult.Ok).value

        val lines = csv.split("\r\n").filter { it.isNotBlank() }
        assertTrue("CSV must have header and rows", lines.isNotEmpty())
        val expectedHeader =
            "id,title,notes,priority,is_starred,is_completed,completed_at," +
                "due_date,due_time,bucket,project_id,area_id,tags,created_at,updated_at"
        assertEquals(expectedHeader, lines[0])

        // Verify RFC 4180 escaping
        assertTrue(
            "Title with quotes and comma must be escaped with doubled quotes",
            csv.contains("\"Meeting with \"\"Executive\"\" Team, Q3\""),
        )
        assertTrue(
            "Notes with quotes and newline must be escaped with doubled quotes",
            csv.contains("\"Line 1\nLine 2 with, commas and \"\"quotes\"\"\""),
        )
    }

    @Test
    fun createBackupZipBytes_produces_valid_zip_with_backup_and_metadata() = runBlocking {
        val zipResult = backupService1.createBackupZipBytes()
        assertTrue(zipResult.isOk)
        val zipBytes = (zipResult as AppResult.Ok).value
        assertTrue("ZIP bytes must not be empty", zipBytes.isNotEmpty())

        // Read ZIP stream
        val zipIn = ZipInputStream(ByteArrayInputStream(zipBytes))
        val entryNames = mutableListOf<String>()
        var entry = zipIn.nextEntry
        while (entry != null) {
            entryNames.add(entry.name)
            zipIn.closeEntry()
            entry = zipIn.nextEntry
        }

        assertTrue("ZIP archive must contain backup.json", entryNames.contains("backup.json"))
        assertTrue("ZIP archive must contain metadata.json", entryNames.contains("metadata.json"))
    }
}
