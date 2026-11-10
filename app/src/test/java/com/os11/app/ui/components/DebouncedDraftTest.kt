package com.os11.app.ui.components

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DebouncedDraftTest {

    private val saved = mutableListOf<String>()

    private fun newDraft(scope: CoroutineScope) = DebouncedDraft<String>(scope, delayMs = DELAY_MS) { saved += it }

    // advanceTimeBy does not run tasks scheduled exactly at the target time, so step one millisecond past it.
    private fun TestScope.waitForTimer() = advanceTimeBy(DELAY_MS + 1)

    @Test
    fun `ten rapid updates save once with the newest value`() = runTest {
        val draft = newDraft(this)

        "abcdefghij".forEach { char ->
            draft.update("typed $char")
            advanceTimeBy(50L)
        }
        assertEquals("nothing is saved while typing continues", emptyList<String>(), saved)

        waitForTimer()
        assertEquals(listOf("typed j"), saved)
    }

    @Test
    fun `every update restarts the timer`() = runTest {
        val draft = newDraft(this)

        draft.update("first")
        advanceTimeBy(300L)
        draft.update("second")
        advanceTimeBy(300L) // 600 ms after the first update, but only 300 ms after the second
        assertEquals(emptyList<String>(), saved)

        advanceTimeBy(101L)
        assertEquals(listOf("second"), saved)
    }

    @Test
    fun `flush saves the pending value immediately and cancels the timer`() = runTest {
        val draft = newDraft(this)

        draft.update("unsaved")
        draft.flush()
        assertEquals(listOf("unsaved"), saved)

        advanceTimeBy(1_000L)
        assertEquals("the cancelled timer must not save a second time", listOf("unsaved"), saved)
    }

    @Test
    fun `flush with nothing pending saves nothing`() = runTest {
        val draft = newDraft(this)

        draft.flush()
        draft.update("a")
        waitForTimer()
        draft.flush() // already delivered by the timer

        assertEquals(listOf("a"), saved)
    }

    @Test
    fun `a new edit after a save is saved again`() = runTest {
        val draft = newDraft(this)

        draft.update("one")
        waitForTimer()
        draft.update("two")
        waitForTimer()

        assertEquals(listOf("one", "two"), saved)
    }

    private companion object {
        const val DELAY_MS = 400L
    }
}
