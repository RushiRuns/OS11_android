package com.os11.app.service.search

import com.os11.app.data.db.entities.TaskEntity
import com.os11.app.data.repository.SearchRepository
import com.os11.app.di.IoDispatcher
import com.os11.app.service.settings.SettingsService
import com.os11.app.service.task.TaskItem
import com.os11.app.service.task.toTaskItem
import com.os11.domain.result.AppError
import com.os11.domain.result.AppResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@Suppress("TooGenericExceptionCaught")
class SearchService @Inject constructor(
    private val searchRepository: SearchRepository,
    private val settingsService: SettingsService,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    fun observeRecentSearches(): Flow<List<String>> = settingsService.observeAllSettings().map { settings ->
        val entity = settings.firstOrNull { it.key == RECENT_SEARCHES_KEY }
        if (entity != null && entity.value.isNotBlank()) {
            try {
                Json.decodeFromString<List<String>>(entity.value)
            } catch (_: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
    }

    suspend fun getRecentSearches(): List<String> = withContext(ioDispatcher) {
        val raw = settingsService.getSetting(RECENT_SEARCHES_KEY)
        if (!raw.isNullOrBlank()) {
            try {
                Json.decodeFromString<List<String>>(raw)
            } catch (_: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
    }

    suspend fun addRecentSearch(query: String): AppResult<Unit> = withContext(ioDispatcher) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return@withContext AppResult.ok(Unit)
        val current = getRecentSearches()
        val updated = (listOf(trimmed) + current.filterNot { it.equals(trimmed, ignoreCase = true) })
            .take(MAX_RECENT_SEARCHES)
        val json = Json.encodeToString(updated)
        settingsService.setSetting(RECENT_SEARCHES_KEY, json)
    }

    suspend fun removeRecentSearch(query: String): AppResult<Unit> = withContext(ioDispatcher) {
        val current = getRecentSearches()
        val updated = current.filterNot { it.equals(query, ignoreCase = true) }
        val json = Json.encodeToString(updated)
        settingsService.setSetting(RECENT_SEARCHES_KEY, json)
    }

    suspend fun clearRecentSearches(): AppResult<Unit> = withContext(ioDispatcher) {
        settingsService.setSetting(RECENT_SEARCHES_KEY, "[]")
    }

    suspend fun searchTasks(query: String, limit: Int = DEFAULT_SEARCH_LIMIT): AppResult<List<TaskEntity>> =
        withContext(ioDispatcher) {
            val trimmed = query.trim()
            if (trimmed.isEmpty()) {
                return@withContext AppResult.ok(emptyList())
            }

            try {
                val results = searchRepository.searchTasks(trimmed, limit)
                AppResult.ok(results)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppResult.err(AppError.Unexpected(e))
            }
        }

    suspend fun searchTaskItems(query: String, limit: Int = DEFAULT_SEARCH_LIMIT): AppResult<List<TaskItem>> =
        withContext(ioDispatcher) {
            val trimmed = query.trim()
            if (trimmed.isEmpty()) {
                return@withContext AppResult.ok(emptyList())
            }

            try {
                val results = searchRepository.searchTasks(trimmed, limit)
                AppResult.ok(results.map { it.toTaskItem() })
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppResult.err(AppError.Unexpected(e))
            }
        }

    companion object {
        const val DEFAULT_SEARCH_LIMIT = 50
        const val MAX_RECENT_SEARCHES = 20
        const val RECENT_SEARCHES_KEY = "recent_searches_json"
    }
}
