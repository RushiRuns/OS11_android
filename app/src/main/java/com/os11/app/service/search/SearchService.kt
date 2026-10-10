package com.os11.app.service.search

import com.os11.app.data.db.entities.TaskEntity
import com.os11.app.data.repository.SearchRepository
import com.os11.app.di.IoDispatcher
import com.os11.domain.result.AppError
import com.os11.domain.result.AppResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@Suppress("TooGenericExceptionCaught")
class SearchService @Inject constructor(
    private val searchRepository: SearchRepository,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    suspend fun searchTasks(query: String, limit: Int = 50): AppResult<List<TaskEntity>> = withContext(ioDispatcher) {
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
}
