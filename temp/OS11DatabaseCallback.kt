package com.os11.app.data.db

import androidx.room.RoomDatabase
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.execSQL

/**
 * Database callback that configures SQLite pragmas (foreign keys, NORMAL synchronous), seeds default rows,
 * and creates hand-written SQL objects that Room cannot express natively (FTS5 table, FTS triggers,
 * partial indexes).
 *
 * The database is built with BundledSQLiteDriver. When a driver is set, Room only calls the
 * SQLiteConnection overloads below. The SupportSQLiteDatabase overloads are kept for builders that run
 * without a driver (for example the default in-memory test builder). Everything here is idempotent, so it
 * does no harm if both paths end up running.
 */
internal class OS11DatabaseCallback : RoomDatabase.Callback() {

    // Driver path (BundledSQLiteDriver). No super call on purpose: the default implementation forwards to
    // the SupportSQLiteDatabase overloads, which would run everything a second time.

    override fun onOpen(connection: SQLiteConnection) {
        connection.execSQL("PRAGMA foreign_keys = ON;")
        try {
            connection.execSQL("PRAGMA synchronous = NORMAL;")
        } catch (_: Exception) {
            // Ignored if unsupported
        }
        DatabaseInitializer.seedDefaults(connection)
    }

    override fun onCreate(connection: SQLiteConnection) {
        createFtsAndTriggers { connection.execSQL(it) }
        createPartialIndexes { connection.execSQL(it) }
        DatabaseInitializer.seedDefaults(connection)
    }

    // Legacy SupportSQLite path

    override fun onOpen(db: SupportSQLiteDatabase) {
        super.onOpen(db)
        db.setForeignKeyConstraintsEnabled(true)
        try {
            db.execSQL("PRAGMA synchronous = NORMAL;")
        } catch (_: Exception) {
            // Ignored if unsupported
        }
        DatabaseInitializer.seedDefaults(db)
    }

    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        createFtsAndTriggers { db.execSQL(it) }
        createPartialIndexes { db.execSQL(it) }
        DatabaseInitializer.seedDefaults(db)
    }

    private fun createFtsAndTriggers(exec: (String) -> Unit) {
        try {
            FTS_STATEMENTS.forEach(exec)
        } catch (_: Exception) {
            // FTS5 is provided on real devices by BundledSQLiteDriver; host JVM shadow SQLite may omit it.
        }
    }

    private fun createPartialIndexes(exec: (String) -> Unit) {
        try {
            INDEX_STATEMENTS.forEach(exec)
        } catch (_: Exception) {
            // Partial index clauses gracefully fallback if host SQLite lacks support.
        }
    }

    private companion object {
        val FTS_STATEMENTS = listOf(
            """
            CREATE VIRTUAL TABLE IF NOT EXISTS tasks_fts USING fts5(
              id UNINDEXED,
              title,
              notes,
              content=tasks,
              content_rowid=rowid
            );
            """.trimIndent(),
            """
            CREATE TRIGGER IF NOT EXISTS tasks_ai AFTER INSERT ON tasks BEGIN
              INSERT INTO tasks_fts(rowid, id, title, notes) VALUES (new.rowid, new.id, new.title, new.notes);
            END;
            """.trimIndent(),
            """
            CREATE TRIGGER IF NOT EXISTS tasks_ad AFTER DELETE ON tasks BEGIN
              INSERT INTO tasks_fts(tasks_fts, rowid, id, title, notes)
              VALUES('delete', old.rowid, old.id, old.title, old.notes);
            END;
            """.trimIndent(),
            """
            CREATE TRIGGER IF NOT EXISTS tasks_au AFTER UPDATE ON tasks BEGIN
              INSERT INTO tasks_fts(tasks_fts, rowid, id, title, notes)
              VALUES('delete', old.rowid, old.id, old.title, old.notes);
              INSERT INTO tasks_fts(rowid, id, title, notes) VALUES (new.rowid, new.id, new.title, new.notes);
            END;
            """.trimIndent(),
        )

        val INDEX_STATEMENTS = listOf(
            """
            CREATE INDEX IF NOT EXISTS idx_tasks_bucket ON tasks(bucket, sort_order)
            WHERE bucket IS NOT NULL AND is_trashed = 0 AND is_completed = 0;
            """.trimIndent(),
            """
            CREATE INDEX IF NOT EXISTS idx_tasks_waiting ON tasks(follow_up_date, waiting_since)
            WHERE waiting_since IS NOT NULL AND is_trashed = 0 AND is_completed = 0;
            """.trimIndent(),
            """
            CREATE INDEX IF NOT EXISTS idx_tasks_inbox ON tasks(sort_order)
            WHERE area_id IS NULL AND project_id IS NULL AND parent_task_id IS NULL
              AND due_date IS NULL AND is_trashed = 0 AND is_completed = 0;
            """.trimIndent(),
        )
    }
}
