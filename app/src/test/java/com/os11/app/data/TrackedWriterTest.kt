package com.os11.app.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.os11.app.data.db.DatabaseInitializer
import com.os11.app.data.db.OS11Database
import com.os11.app.data.db.entities.AttachmentEntity
import com.os11.app.data.db.entities.ReminderEntity
import com.os11.app.data.db.entities.TaskEntity
import com.os11.app.data.sync.TrackedWriter
import com.os11.domain.id.DefaultIdGenerator
import com.os11.domain.id.DeterministicIds
import com.os11.domain.logic.sync.DefaultHlcClock
import com.os11.domain.logic.sync.Hlc
import com.os11.domain.time.TestClock
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
class TrackedWriterTest {

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
    @Suppress("LongMethod")
    fun create_edit_trash_restore_and_delete_task_lifecycle() = runBlocking {
        val workspaceId = DeterministicIds.defaultWorkspaceId()
        val taskId = UUID.randomUUID().toString()
        val task = TaskEntity(
            id = taskId,
            title = "Test tracked task",
            notes = "<p>Initial notes</p>",
            workspaceId = workspaceId,
            sortOrder = 1.0,
            createdAt = testClock.nowIso(),
            updatedAt = testClock.nowIso(),
        )

        var committedEvents = 0
        trackedWriter.registerListener { entity, id, _ ->
            if (entity == "tasks" && id == taskId) {
                committedEvents++
            }
        }

        // 1. CREATE TASK
        trackedWriter.writeTask(task)
        assertEquals(1, committedEvents)

        // Verify data row
        val saved = db.taskDao().getById(taskId)
        assertNotNull(saved)
        assertEquals("Test tracked task", saved?.title)

        // Verify sync_field_clock
        val titleClock = db.syncFieldClockDao().getClock("tasks", taskId, "title")
        assertNotNull(titleClock)
        assertEquals("device01", Hlc.parse(titleClock!!.hlc).deviceId)

        // Verify outbox (sync_queue)
        val outboxRows = db.syncQueueDao().getNextBatch(10)
        assertTrue(outboxRows.any { it.entity == "tasks" && it.entityId == taskId && it.operation == "upsert" })

        // Verify sync_local_state updated
        val localState = db.syncLocalStateDao().get()
        assertNotNull(localState)
        assertEquals(titleClock.hlc, localState?.lastHlc)

        // 2. EDIT TASK
        testClock.advanceBy(10_000L)
        val updatedTask = saved!!.copy(
            title = "Updated task title",
            priority = 3,
            updatedAt = testClock.nowIso(),
        )
        trackedWriter.writeTask(updatedTask)
        assertEquals(2, committedEvents)

        val updatedSaved = db.taskDao().getById(taskId)
        assertEquals("Updated task title", updatedSaved?.title)
        assertEquals(3, updatedSaved?.priority)

        // Verify task_history entry
        val historyEntries = db.taskHistoryDao().observeByTask(taskId)
        assertNotNull(historyEntries)

        // 3. TRASH TASK
        testClock.advanceBy(5_000L)
        val trashedTask = updatedSaved!!.copy(
            isTrashed = true,
            trashedAt = testClock.nowIso(),
            updatedAt = testClock.nowIso(),
        )
        trackedWriter.writeTask(trashedTask)
        assertEquals(3, committedEvents)

        val trashedSaved = db.taskDao().getById(taskId)
        assertTrue(trashedSaved!!.isTrashed)
        assertNotNull(trashedSaved.trashedAt)

        // 4. RESTORE TASK
        testClock.advanceBy(5_000L)
        val restoredTask = trashedSaved.copy(
            isTrashed = false,
            trashedAt = null,
            updatedAt = testClock.nowIso(),
        )
        trackedWriter.writeTask(restoredTask)
        assertEquals(4, committedEvents)

        val restoredSaved = db.taskDao().getById(taskId)
        assertFalse(restoredSaved!!.isTrashed)
        assertNull(restoredSaved.trashedAt)

        // 5. ATTACH REMINDER AND ATTACHMENT BEFORE PERMANENT DELETE TO TEST CASCADE TOMBSTONES
        val reminderId = UUID.randomUUID().toString()
        trackedWriter.writeReminder(
            ReminderEntity(
                id = reminderId,
                taskId = taskId,
                remindAt = "2026-10-15T09:00:00.000Z",
                createdAt = testClock.nowIso(),
            ),
        )

        val attachmentId = UUID.randomUUID().toString()
        trackedWriter.writeAttachment(
            AttachmentEntity(
                id = attachmentId,
                taskId = taskId,
                filename = "doc.pdf",
                originalName = "doc.pdf",
                mimeType = "application/pdf",
                sizeBytes = 1024L,
                createdAt = testClock.nowIso(),
            ),
        )

        // 6. PERMANENT DELETE (Tombstone + Cascade tombstones)
        testClock.advanceBy(5_000L)
        trackedWriter.deleteTask(taskId)
        assertEquals(5, committedEvents)

        // Physical row deleted
        assertNull(db.taskDao().getById(taskId))
        assertNull(db.reminderDao().getById(reminderId))
        assertNull(db.attachmentDao().getById(attachmentId))

        // Tombstones written for task and cascaded entities
        val taskTombstone = db.syncTombstoneDao().getTombstone("tasks", taskId)
        assertNotNull(taskTombstone)
        val reminderTombstone = db.syncTombstoneDao().getTombstone("reminders", reminderId)
        assertNotNull(reminderTombstone)
        val attachmentTombstone = db.syncTombstoneDao().getTombstone("attachments", attachmentId)
        assertNotNull(attachmentTombstone)

        // Outbox contains delete operations
        val allOutbox = db.syncQueueDao().getNextBatch(100)
        assertTrue(allOutbox.any { it.entity == "tasks" && it.entityId == taskId && it.operation == "delete" })
        assertTrue(allOutbox.any { it.entity == "reminders" && it.entityId == reminderId && it.operation == "delete" })
    }

    @Test
    fun writeSetting_with_allowlisted_keys_creates_sync_clock_and_outbox() = runBlocking {
        // Clear existing outbox from seed
        val existing = db.syncQueueDao().getNextBatch(100)
        if (existing.isNotEmpty()) {
            db.syncQueueDao().deleteSeqLessThanOrEqual(existing.maxOf { it.seq })
        }

        // Exactly the 7 allowlisted keys
        assertEquals(7, TrackedWriter.SYNCED_SETTINGS_ALLOWLIST.size)

        for (key in TrackedWriter.SYNCED_SETTINGS_ALLOWLIST) {
            val value = "\"test_value_$key\""
            trackedWriter.writeSetting(key, value)

            // 1. Data row written
            assertEquals(value, db.settingDao().getValue(key))

            // 2. Field clock written
            val clock = db.syncFieldClockDao().getClock("settings", key, "value")
            assertNotNull("Clock should exist for allowlisted key: $key", clock)

            // 3. Outbox row written
            val outbox = db.syncQueueDao().getNextBatch(100)
            assertTrue(
                "Outbox should have entry for allowlisted key: $key",
                outbox.any {
                    it.entity == "settings" && it.entityId == key && it.operation == "upsert"
                },
            )
        }
    }

    @Test
    fun writeSetting_with_device_local_keys_does_not_create_sync_clock_or_outbox() = runBlocking {
        // Clear existing outbox from seed
        val existing = db.syncQueueDao().getNextBatch(100)
        if (existing.isNotEmpty()) {
            db.syncQueueDao().deleteSeqLessThanOrEqual(existing.maxOf { it.seq })
        }

        val localKeys = listOf(
            "accent_color",
            "font_size",
            "density",
            "app_lock_enabled",
            "secure_screen",
            "haptics_enabled",
            "reduce_motion",
        )

        for (key in localKeys) {
            assertFalse(TrackedWriter.SYNCED_SETTINGS_ALLOWLIST.contains(key))
            val value = "\"local_value_$key\""
            trackedWriter.writeSetting(key, value)

            // 1. Data row written
            assertEquals(value, db.settingDao().getValue(key))

            // 2. Field clock NOT written
            val clock = db.syncFieldClockDao().getClock("settings", key, "value")
            assertNull("Clock must NOT exist for device-local key: $key", clock)

            // 3. Outbox row NOT written
            val outbox = db.syncQueueDao().getNextBatch(100)
            assertFalse(
                "Outbox must NOT have entry for device-local key: $key",
                outbox.any {
                    it.entity == "settings" && it.entityId == key
                },
            )
        }
    }
}
