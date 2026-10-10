package com.os11.app.service.backup

import com.os11.app.data.repository.TaskRepository
import com.os11.app.di.IoDispatcher
import com.os11.domain.result.AppError
import com.os11.domain.result.AppResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@Suppress("TooGenericExceptionCaught")
class ExportService @Inject constructor(
    private val taskRepository: TaskRepository,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    suspend fun exportWorkspaceToJson(workspaceId: String): AppResult<String> = withContext(ioDispatcher) {
        try {
            val tasks = taskRepository.observeAllSlim().first()
            val jsonObject = buildJsonObject {
                put("version", 1)
                put("workspaceId", workspaceId)
                put("taskCount", tasks.size)
            }
            AppResult.ok(Json.encodeToString(jsonObject))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppResult.err(AppError.Unexpected(e))
        }
    }
}
