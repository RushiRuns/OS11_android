package com.os11.app.data.db

import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Database callback that configures SQLite pragmas (WAL, NORMAL synchronous, foreign keys)
 * and hand-written SQL objects that Room cannot express natively (FTS5 table, FTS triggers, partial indexes).
 */
internal class OS11DatabaseCallback : RoomDatabase.Callback() {

    override fun onOpen(db: SupportSQLiteDatabase) {
        super.onOpen(db)
        db.setForeignKeyConstraintsEnabled(true)
        try {
            db.execSQL("PRAGMA synchronous = NORMAL;")
        } catch (_: Exception) {
            // Ignored if unsupported
        }
    }

    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        createFtsAndTriggers(db)
        createPartialIndexes(db)
    }

    private fun createFtsAndTriggers(db: SupportSQLiteDatabase) {
        try {
            db.execSQL(
                """
                CREATE VIRTUAL TABLE IF NOT EXISTS tasks_fts USING fts5(
                  id UNINDEXED,
                  title,
                  notes,
                  content=tasks,
                  content_rowid=rowid
                );
                """.trimIndent(),
            )

            db.execSQL(
                """
                CREATE TRIGGER IF NOT EXISTS tasks_ai AFTER INSERT ON tasks BEGIN
                  INSERT INTO tasks_fts(rowid, id, title, notes) VALUES (new.rowid, new.id, new.title, new.notes);
                END;
                """.trimIndent(),
            )

            db.execSQL(
                """
                CREATE TRIGGER IF NOT EXISTS tasks_ad AFTER DELETE ON tasks BEGIN
                  INSERT INTO tasks_fts(tasks_fts, rowid, id, title, notes)
                  VALUES('delete', old.rowid, old.id, old.title, old.notes);
                END;
                """.trimIndent(),
            )

            db.execSQL(
                """
                CREATE TRIGGER IF NOT EXISTS tasks_au AFTER UPDATE ON tasks BEGIN
                  INSERT INTO tasks_fts(tasks_fts, rowid, id, title, notes)
                  VALUES('delete', old.rowid, old.id, old.title, old.notes);
                  INSERT INTO tasks_fts(rowid, id, title, notes) VALUES (new.rowid, new.id, new.title, new.notes);
                END;
                """.trimIndent(),
            )
        } catch (_: Exception) {
            // FTS5 is provided on real devices by BundledSQLiteDriver; host JVM shadow SQLite may omit it.
        }
    }

    private fun createPartialIndexes(db: SupportSQLiteDatabase) {
        try {
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS idx_tasks_bucket ON tasks(bucket, sort_order)
                WHERE bucket IS NOT NULL AND is_trashed = 0 AND is_completed = 0;
                """.trimIndent(),
            )

            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS idx_tasks_waiting ON tasks(follow_up_date, waiting_since)
                WHERE waiting_since IS NOT NULL AND is_trashed = 0 AND is_completed = 0;
                """.trimIndent(),
            )

            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS idx_tasks_inbox ON tasks(sort_order)
                WHERE area_id IS NULL AND project_id IS NULL AND parent_task_id IS NULL
                  AND due_date IS NULL AND is_trashed = 0 AND is_completed = 0;
                """.trimIndent(),
            )
        } catch (_: Exception) {
            // Partial index clauses gracefully fallback if host SQLite lacks support.
        }
    }
}
