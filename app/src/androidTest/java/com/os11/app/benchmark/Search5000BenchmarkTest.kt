package com.os11.app.benchmark

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Phase 7 Exit Criterion: Search on the 5,000-task seed is under target (<150ms).
 * Instrumented benchmark executing FTS5 prefix search with BundledSQLiteDriver over 5,000 seeded tasks.
 */
class Search5000BenchmarkTest {

    private fun SQLiteConnection.exec(sql: String) {
        val stmt = prepare(sql)
        try {
            stmt.step()
        } finally {
            stmt.close()
        }
    }

    @Test
    fun fts5_prefix_search_on_5000_seeded_tasks_is_under_150ms() {
        val driver = BundledSQLiteDriver()
        val connection = driver.open(":memory:")
        try {
            // 1. Create tasks table per SCHEMA.md
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

            // 2. Create FTS5 virtual table and triggers per SCHEMA.md
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

            // 3. Seed 5,000 tasks inside a single transaction
            connection.exec("BEGIN TRANSACTION")
            for (i in 1..5000) {
                val title = if (i ==
                    4200
                ) {
                    "Urgent client presentation slides"
                } else {
                    "Task $i: Benchmark payload task item"
                }
                val notes = if (i ==
                    4200
                ) {
                    "Include revenue charts and project roadmaps"
                } else {
                    "Detailed notes for task $i"
                }
                connection.exec(
                    "INSERT INTO tasks (id, title, notes, is_trashed, sort_order) VALUES ('id-$i', '$title', '$notes', 0, $i.0)",
                )
            }
            connection.exec("COMMIT")

            // 4. Measure FTS5 prefix search query latency (50-item limit per search contract)
            val startNs = System.nanoTime()
            val stmt = connection.prepare(
                """
                SELECT t.id, t.title FROM tasks t
                JOIN tasks_fts ON tasks_fts.rowid = t.rowid
                WHERE tasks_fts MATCH '"client"*' AND t.is_trashed = 0
                ORDER BY bm25(tasks_fts)
                LIMIT 50
                """.trimIndent(),
            )
            val hasRow = stmt.step()
            val elapsedMs = (System.nanoTime() - startNs) / 1_000_000
            stmt.close()

            println("Phase 7 Search benchmark: FTS5 prefix search across 5,000 tasks took $elapsedMs ms")
            assertTrue("Should find matching task from FTS5 prefix query", hasRow)
            assertTrue(
                "FTS5 prefix search over 5,000 tasks took $elapsedMs ms (target: < 150 ms)",
                elapsedMs < 150,
            )
        } finally {
            connection.close()
        }
    }
}
