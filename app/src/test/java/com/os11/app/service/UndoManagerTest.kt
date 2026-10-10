package com.os11.app.service

import com.os11.app.service.undo.UndoManager
import com.os11.domain.result.AppError
import com.os11.domain.result.AppResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class UndoManagerTest {

    private lateinit var undoManager: UndoManager

    @Before
    fun setup() {
        undoManager = UndoManager()
    }

    @Test
    fun `registerUndo adds token and triggers undo action on execute`() = runBlocking {
        var actionExecuted = false
        val token = undoManager.registerUndo("Completed task") {
            actionExecuted = true
            AppResult.ok(Unit)
        }

        assertEquals("Completed task", token.actionDescription)
        assertEquals(1, undoManager.count())

        val result = undoManager.undo(token.id)
        assertTrue(result.isOk)
        assertTrue(actionExecuted)
        assertEquals(0, undoManager.count())
    }

    @Test
    fun `undoLatest executes and pops most recent entry`() = runBlocking {
        var firstReverted = false
        var secondReverted = false

        undoManager.registerUndo("Action 1") {
            firstReverted = true
            AppResult.ok(Unit)
        }
        undoManager.registerUndo("Action 2") {
            secondReverted = true
            AppResult.ok(Unit)
        }

        assertEquals(2, undoManager.count())

        val result = undoManager.undoLatest()
        assertTrue(result.isOk)
        assertTrue(secondReverted)
        assertFalse(firstReverted)
        assertEquals(1, undoManager.count())
    }

    @Test
    fun `stack caps at 100 entries by dropping oldest`() = runBlocking {
        for (i in 1..105) {
            undoManager.registerUndo("Action $i") { AppResult.ok(Unit) }
        }

        assertEquals(100, undoManager.count())
    }

    @Test
    fun `undoing nonexistent token returns NotFound error`() = runBlocking {
        val result = undoManager.undo("nonexistent-id")
        assertTrue(result.isErr)
        assertEquals(AppError.NotFound, (result as AppResult.Err).error)
    }
}
