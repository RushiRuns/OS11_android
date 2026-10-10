package com.os11.app.service.undo

import com.os11.domain.result.AppError
import com.os11.domain.result.AppResult
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Token returned to callers when an undoable operation is registered.
 */
data class UndoToken(
    val id: String = UUID.randomUUID().toString(),
    val actionDescription: String,
    val timestampMs: Long = System.currentTimeMillis(),
)

internal data class UndoEntry(val token: UndoToken, val undoAction: suspend () -> AppResult<Unit>)

/**
 * Service-level undo stack and Snackbar bridge.
 *
 * Rules per ARCHITECTURE.md and SYSTEM.md:
 * - Keeps up to 100 entries per session.
 * - Undo is itself a normal tracked write (so it syncs).
 * - Dropped oldest when stack exceeds 100 entries.
 */
@Singleton
class UndoManager @Inject constructor() {

    private val mutex = Mutex()
    private val stack = ArrayDeque<UndoEntry>()

    private val _undoEvents = MutableSharedFlow<UndoToken>(extraBufferCapacity = 1)
    val undoEvents: SharedFlow<UndoToken> = _undoEvents.asSharedFlow()

    suspend fun registerUndo(description: String, undoAction: suspend () -> AppResult<Unit>): UndoToken =
        mutex.withLock {
            val token = UndoToken(actionDescription = description)
            if (stack.size >= MAX_STACK_SIZE) {
                stack.removeFirst()
            }
            stack.addLast(UndoEntry(token, undoAction))
            _undoEvents.tryEmit(token)
            token
        }

    suspend fun undo(tokenId: String): AppResult<Unit> {
        val entry = mutex.withLock {
            val index = stack.indexOfLast { it.token.id == tokenId }
            if (index == -1) null else stack.removeAt(index)
        } ?: return AppResult.err(AppError.NotFound)

        return entry.undoAction()
    }

    suspend fun undoLatest(): AppResult<Unit> {
        val entry = mutex.withLock {
            if (stack.isEmpty()) null else stack.removeLast()
        } ?: return AppResult.err(AppError.NotFound)

        return entry.undoAction()
    }

    suspend fun count(): Int = mutex.withLock { stack.size }

    suspend fun clear() = mutex.withLock { stack.clear() }

    companion object {
        const val MAX_STACK_SIZE = 100
        const val UNDO_TIMEOUT_SECONDS = 5
    }
}
