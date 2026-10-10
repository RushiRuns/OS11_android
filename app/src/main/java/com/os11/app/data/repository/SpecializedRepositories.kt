package com.os11.app.data.repository

import androidx.sqlite.db.SimpleSQLiteQuery
import com.os11.app.data.db.OS11Database
import com.os11.app.data.db.dao.BrowseCounts
import com.os11.app.data.db.entities.TaskEntity
import kotlinx.coroutines.flow.Flow
import java.util.Locale

interface SearchRepository {
    suspend fun searchTasks(query: String, limit: Int = 100): List<TaskEntity>
}

internal class SearchRepositoryImpl(private val db: OS11Database) : SearchRepository {

    override suspend fun searchTasks(query: String, limit: Int): List<TaskEntity> {
        val trimmed = query.trim()
        val escapedQuery = if (trimmed.isEmpty()) "" else escapeFts5Query(trimmed)
        if (escapedQuery.isEmpty()) return emptyList()

        val sql = """
            SELECT t.* FROM tasks t
            JOIN tasks_fts ON tasks_fts.rowid = t.rowid
            WHERE tasks_fts MATCH ? AND t.is_trashed = 0
            ORDER BY bm25(tasks_fts)
            LIMIT ?
        """.trimIndent()

        return try {
            val rawQuery = SimpleSQLiteQuery(sql, arrayOf<Any>(escapedQuery, limit))
            db.searchDao().searchTasks(rawQuery)
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun escapeFts5Query(input: String): String {
        val tokens = input.split("\\s+".toRegex())
            .map { it.replace("\"", "").replace("'", "").replace("*", "") }
            .filter { it.isNotBlank() }

        if (tokens.isEmpty()) return ""

        return tokens.joinToString(" ") { token ->
            "\"${token.lowercase(Locale.US)}\"*"
        }
    }
}

interface CountsRepository {
    fun observeBrowseCounts(today: String): Flow<BrowseCounts>
}

internal class CountsRepositoryImpl(private val db: OS11Database) : CountsRepository {
    override fun observeBrowseCounts(today: String): Flow<BrowseCounts> = db.countsDao().observeBrowseCounts(today)
}
