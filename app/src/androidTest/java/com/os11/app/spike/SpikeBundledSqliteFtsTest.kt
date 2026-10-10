package com.os11.app.spike

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Spike S-1: Bundled SQLite + FTS5 Verification.
 *
 * Verifies:
 * 1. Bundled SQLite driver (androidx.sqlite:sqlite-bundled) operates consistently.
 * 2. FTS5 virtual table and sync triggers work per SCHEMA.md.
 * 3. Prefix search over 5,000 seeded tasks finishes well under the 150 ms target.
 */
class SpikeBundledSqliteFtsTest {

    private fun SQLiteConnection.exec(sql: String) {
        val stmt = prepare(sql)
        try {
            stmt.step()
        } finally {
            stmt.close()
        }
    }

    @Test
    fun testBundledSqliteFts5PrefixSearch() {
        val driver = BundledSQLiteDriver()
        val connection = driver.open(":memory:")
        try {
            // 1. Create tasks table
            connection.exec(
                """
                CREATE TABLE tasks (
                    id TEXT PRIMARY KEY,
                    title TEXT NOT NULL,
                    notes TEXT,
                    is_trashed INTEGER NOT NULL DEFAULT 0,
                    sort_order REAL NOT NULL DEFAULT 0
                )
                """.trimIndent(),
            )

            // 2. Create FTS5 virtual table and triggers (per SCHEMA.md Full-Text Search)
            connection.exec(
                """
                CREATE VIRTUAL TABLE tasks_fts USING fts5(
                    id UNINDEXED,
                    title,
                    notes,
                    content=tasks,
                    content_rowid=rowid
                )
                """.trimIndent(),
            )

            connection.exec(
                """
                CREATE TRIGGER tasks_ai AFTER INSERT ON tasks BEGIN
                    INSERT INTO tasks_fts(rowid, id, title, notes) VALUES (new.rowid, new.id, new.title, new.notes);
                END
                """.trimIndent(),
            )

            // 3. Seed 5,000 rows inside a single transaction
            connection.exec("BEGIN TRANSACTION")
            for (i in 1..5000) {
                val title = if (i == 4200) "Urgent buy groceries for dinner" else "Regular task number $i"
                val notes = if (i == 4200) "Remember to buy milk and bread" else "Notes for task $i"
                connection.exec(
                    "INSERT INTO tasks (id, title, notes, is_trashed, sort_order) VALUES ('id-$i', '$title', '$notes', 0, $i.0)",
                )
            }
            connection.exec("COMMIT")

            // 4. Measure prefix search latency
            val startTime = System.nanoTime()
            val stmt = connection.prepare(
                "SELECT t.id, t.title FROM tasks_fts f JOIN tasks t ON f.rowid = t.rowid WHERE tasks_fts MATCH 'grocer*' AND t.is_trashed = 0",
            )
            val hasRow = stmt.step()
            val elapsedMs = (System.nanoTime() - startTime) / 1_000_000
            stmt.close()

            assertTrue("Should find matching task from FTS5 prefix match", hasRow)
            println("Spike S-1 Result: FTS5 prefix search across 5,000 seeded tasks took $elapsedMs ms")
            assertTrue("Prefix search over 5,000 rows took $elapsedMs ms (must be < 150 ms)", elapsedMs < 150)
        } finally {
            connection.close()
        }
    }
}
