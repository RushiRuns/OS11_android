package com.os11.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** How long typing must pause before a draft is saved. */
const val DRAFT_SAVE_DELAY_MS = 400L

/**
 * Holds the newest unsaved value of something being typed and saves it once, after [delayMs] without a new
 * value, instead of on every keystroke. Each [update] restarts the timer. [flush] saves immediately, which
 * callers use when the editor goes away so a pending edit is never lost.
 *
 * Not thread-safe: use it from one thread (the main thread in the UI).
 */
class DebouncedDraft<T : Any>(
    private val scope: CoroutineScope,
    private val delayMs: Long,
    private val onSave: (T) -> Unit,
) {
    private var job: Job? = null
    private var pending: T? = null

    /** Records [value] as the newest draft and (re)starts the save timer. */
    fun update(value: T) {
        pending = value
        job?.cancel()
        job = scope.launch {
            delay(delayMs)
            deliver()
        }
    }

    /** Saves the pending draft now, if there is one, and cancels the timer. */
    fun flush() {
        job?.cancel()
        job = null
        deliver()
    }

    private fun deliver() {
        val value = pending ?: return
        pending = null
        onSave(value)
    }
}

/**
 * Remembers a [DebouncedDraft] for the current composition and flushes it when the composable leaves the
 * screen. [onSave] may change between recompositions; the latest one is always used.
 */
@Composable
fun <T : Any> rememberDebouncedDraft(delayMs: Long = DRAFT_SAVE_DELAY_MS, onSave: (T) -> Unit): DebouncedDraft<T> {
    val latestOnSave by rememberUpdatedState(onSave)
    val scope = rememberCoroutineScope()
    val draft = remember(scope, delayMs) { DebouncedDraft<T>(scope, delayMs) { latestOnSave(it) } }
    DisposableEffect(draft) { onDispose { draft.flush() } }
    return draft
}
