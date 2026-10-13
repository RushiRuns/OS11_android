package com.os11.app.data.db

import android.content.Context
import android.util.Log
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.immediateTransaction
import androidx.room.useWriterConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.os11.app.data.db.entities.TaskEntity
import com.os11.app.data.db.migrations.MIGRATION_1_2
import com.os11.domain.id.DeterministicIds
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class DriverTransactionTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var dbName: String
    private var db: OS11Database? = null

    companion object {
        private const val TAG = "DriverTxTest"
    }

    @Before
    fun setUp() {
        dbName = "driver_tx_test_${UUID.randomUUID()}.db"
        deleteDbFiles(dbName)
        db = createDb(dbName)
    }

    @After
    fun tearDown() {
        try {
            db?.close()
        } catch (_: Exception) {
        }
        deleteDbFiles(dbName)
    }

    private fun createDb(name: String): OS11Database {
        return Room.databaseBuilder(context, OS11Database::class.java, name)
            .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
            .setDriver(BundledSQLiteDriver())
            .addCallback(OS11DatabaseCallback())
            .addMigrations(MIGRATION_1_2)
            .build()
    }

    private fun deleteDbFiles(name: String) {
        context.deleteDatabase(name)
        val dbFile = context.getDatabasePath(name)
        if (dbFile.exists()) dbFile.delete()
        val walFile = File(dbFile.path + "-wal")
        if (walFile.exists()) walFile.delete()
        val shmFile = File(dbFile.path + "-shm")
        if (shmFile.exists()) shmFile.delete()
    }

    /**
     * Copy of inTransaction helper from TrackedWriter.
     */
    private suspend fun <R> inTransaction(database: OS11Database, block: suspend () -> R): R =
        database.useWriterConnection { transactor ->
            transactor.immediateTransaction { block() }
        }

    private fun createTask(
        id: String,
        workspaceId: String = DeterministicIds.defaultWorkspaceId(),
        title: String = "Test Task",
    ): TaskEntity {
        return TaskEntity(
            id = id,
            title = title,
            workspaceId = workspaceId,
            createdAt = "2026-10-13T00:00:00.000Z",
            updatedAt = "2026-10-13T00:00:00.000Z",
        )
    }

    @Test
    fun fkPragmaWriter() = runBlocking {
        val database = checkNotNull(db)
        val fkPragma = database.useWriterConnection { transactor ->
            transactor.usePrepared("PRAGMA foreign_keys;") { stmt ->
                if (stmt.step()) {
                    stmt.getLong(0)
                } else {
                    -1L
                }
            }
        }
        Log.i(TAG, "fkPragmaWriter: foreign_keys = $fkPragma")
        assertEquals(1L, fkPragma)
    }

    @Test
    fun fkPragmaInsideTransaction() = runBlocking {
        val database = checkNotNull(db)
        val fkPragma = inTransaction(database) {
            database.useWriterConnection { transactor ->
                transactor.usePrepared("PRAGMA foreign_keys;") { stmt ->
                    if (stmt.step()) {
                        stmt.getLong(0)
                    } else {
                        -1L
                    }
                }
            }
        }
        Log.i(TAG, "fkPragmaInsideTransaction: foreign_keys = $fkPragma")
        assertEquals(1L, fkPragma)
    }

    @Test
    fun fkEnforced() = runBlocking {
        val database = checkNotNull(db)
        val invalidTask = createTask(
            id = "task-fk-invalid-${UUID.randomUUID()}",
            workspaceId = "nonexistent-workspace-id",
        )

        var thrownException: Throwable? = null
        try {
            inTransaction(database) {
                database.taskDao().upsert(invalidTask)
            }
        } catch (e: Throwable) {
            thrownException = e
        }

        assertNotNull("Expected foreign key constraint violation to throw", thrownException)
        val message = thrownException?.message ?: ""
        Log.i(TAG, "fkEnforced: exception caught = $message")
        assertTrue(
            "Expected foreign key constraint exception message, got: $message",
            message.contains("FOREIGN KEY", ignoreCase = true) ||
                message.contains("constraint", ignoreCase = true),
        )
    }

    @Test
    fun rollback() = runBlocking {
        val database = checkNotNull(db)
        val taskId = "task-rollback-${UUID.randomUUID()}"
        val task = createTask(id = taskId)

        try {
            inTransaction(database) {
                database.taskDao().upsert(task)
                throw RuntimeException("Forced rollback")
            }
        } catch (_: RuntimeException) {
        }

        val found = database.taskDao().getById(taskId)
        Log.i(TAG, "rollback: found after rollback = $found")
        assertNull(found)
    }

    @Test
    fun commit() = runBlocking {
        val database = checkNotNull(db)
        val taskId = "task-commit-${UUID.randomUUID()}"
        val task = createTask(id = taskId)

        inTransaction(database) {
            database.taskDao().upsert(task)
        }

        val found = database.taskDao().getById(taskId)
        Log.i(TAG, "commit: found after commit = $found")
        assertNotNull(found)
        assertEquals(taskId, found?.id)
    }

    @Test
    fun nested() = runBlocking {
        val database = checkNotNull(db)
        val taskId = "task-nested-${UUID.randomUUID()}"
        val task = createTask(id = taskId)

        try {
            inTransaction(database) {
                inTransaction(database) {
                    database.taskDao().upsert(task)
                }
                throw RuntimeException("Forced outer rollback")
            }
        } catch (_: RuntimeException) {
        }

        val found = database.taskDao().getById(taskId)
        Log.i(TAG, "nested: found after outer rollback = $found")
        assertNull(found)
    }

    @Test
    fun flowInvalidation() = runBlocking {
        val database = checkNotNull(db)
        val taskId = "task-flow-${UUID.randomUUID()}"
        val task = createTask(id = taskId)

        val flow = database.taskDao().observeById(taskId)

        // Asynchronously collect the first 2 emissions
        val emissionsDeferred = async {
            withTimeout(2000L) {
                flow.take(2).toList()
            }
        }

        // Write through helper to trigger 2nd emission
        inTransaction(database) {
            database.taskDao().upsert(task)
        }

        val emissions = emissionsDeferred.await()
        Log.i(TAG, "flowInvalidation: collected ${emissions.size} emissions: $emissions")
        assertEquals(2, emissions.size)
        assertNull(emissions[0])
        assertEquals(taskId, emissions[1]?.id)
    }

    @Test
    fun reopen() = runBlocking {
        val initialDb = checkNotNull(db)

        // Read initial counts seeded by callback
        val initialWorkspaceCount = getRowCount(initialDb, "workspaces")
        val initialAreaCount = getRowCount(initialDb, "areas")
        val initialSettingCount = getRowCount(initialDb, "settings")
        val initialModuleCount = getRowCount(initialDb, "modules")
        val initialIdentityCount = getRowCount(initialDb, "local_identity")
        val initialSyncStateCount = getRowCount(initialDb, "sync_local_state")

        Log.i(
            TAG,
            "reopen (before close): workspaces=$initialWorkspaceCount, areas=$initialAreaCount, " +
                "settings=$initialSettingCount, modules=$initialModuleCount, " +
                "local_identity=$initialIdentityCount, sync_local_state=$initialSyncStateCount",
        )

        initialDb.close()

        // Rebuild database with the exact same name
        val reopenedDb = createDb(dbName)
        db = reopenedDb

        val reopenedWorkspaceCount = getRowCount(reopenedDb, "workspaces")
        val reopenedAreaCount = getRowCount(reopenedDb, "areas")
        val reopenedSettingCount = getRowCount(reopenedDb, "settings")
        val reopenedModuleCount = getRowCount(reopenedDb, "modules")
        val reopenedIdentityCount = getRowCount(reopenedDb, "local_identity")
        val reopenedSyncStateCount = getRowCount(reopenedDb, "sync_local_state")

        Log.i(
            TAG,
            "reopen (after rebuild): workspaces=$reopenedWorkspaceCount, areas=$reopenedAreaCount, " +
                "settings=$reopenedSettingCount, modules=$reopenedModuleCount, " +
                "local_identity=$reopenedIdentityCount, sync_local_state=$reopenedSyncStateCount",
        )

        assertEquals(initialWorkspaceCount, reopenedWorkspaceCount)
        assertEquals(initialAreaCount, reopenedAreaCount)
        assertEquals(initialSettingCount, reopenedSettingCount)
        assertEquals(initialModuleCount, reopenedModuleCount)
        assertEquals(initialIdentityCount, reopenedIdentityCount)
        assertEquals(initialSyncStateCount, reopenedSyncStateCount)
    }

    @Test
    fun fts() = runBlocking {
        val database = checkNotNull(db)

        val expectedObjects = listOf(
            "tasks_fts",
            "tasks_ai",
            "tasks_ad",
            "tasks_au",
            "idx_tasks_bucket",
            "idx_tasks_waiting",
            "idx_tasks_inbox",
        )

        for (name in expectedObjects) {
            val exists = database.useWriterConnection { transactor ->
                transactor.usePrepared("SELECT 1 FROM sqlite_master WHERE name = ?;") { stmt ->
                    stmt.bindText(1, name)
                    stmt.step()
                }
            }
            Log.i(TAG, "fts: object '$name' exists = $exists")
            assertTrue("Expected sqlite_master object '$name' to exist", exists)
        }

        val taskId = "task-fts-${UUID.randomUUID()}"
        val uniqueTitle = "UniqueFtsSearchTitle_${UUID.randomUUID()}"
        val task = createTask(id = taskId, title = uniqueTitle)

        inTransaction(database) {
            database.taskDao().upsert(task)
        }

        val matchFound = database.useWriterConnection { transactor ->
            transactor.usePrepared(
                "SELECT t.id, t.title FROM tasks_fts f JOIN tasks t ON f.rowid = t.rowid WHERE tasks_fts MATCH ?;",
            ) { stmt ->
                stmt.bindText(1, uniqueTitle)
                if (stmt.step()) {
                    val foundId = stmt.getText(0)
                    val foundTitle = stmt.getText(1)
                    Log.i(TAG, "fts: matched task id=$foundId, title=$foundTitle")
                    foundId == taskId
                } else {
                    false
                }
            }
        }

        Log.i(TAG, "fts: matchFound = $matchFound")
        assertTrue("Expected FTS MATCH on title '$uniqueTitle' to return task", matchFound)
    }

    private suspend fun getRowCount(database: OS11Database, tableName: String): Long {
        return database.useWriterConnection { transactor ->
            transactor.usePrepared("SELECT COUNT(*) FROM $tableName;") { stmt ->
                if (stmt.step()) stmt.getLong(0) else 0L
            }
        }
    }
}
