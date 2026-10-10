package com.os11.domain.result

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppResultTest {

    @Test
    fun `AppResult Ok exposes value and maps correctly`() {
        val ok = AppResult.Ok("hello")
        assertTrue(ok.isOk)
        assertFalse(ok.isErr)
        assertEquals("hello", ok.getOrNull())
        assertEquals("hello", ok.value)

        val mapped = ok.map { it.length }
        assertTrue(mapped is AppResult.Ok)
        assertEquals(5, (mapped as AppResult.Ok).value)

        val flatMapped = ok.flatMap { AppResult.Ok(it.uppercase()) }
        assertEquals("HELLO", (flatMapped as AppResult.Ok).value)
    }

    @Test
    fun `AppResult Err exposes error and short-circuits maps`() {
        val err = AppResult.Err(AppError.NotFound)
        assertFalse(err.isOk)
        assertTrue(err.isErr)
        assertNull(err.getOrNull())

        val mapped = err.map { "ignored" }
        assertTrue(mapped is AppResult.Err)
        assertEquals(AppError.NotFound, (mapped as AppResult.Err).error)
    }

    @Test
    fun `AppError userMessage formats readable messages`() {
        val validation = AppError.Validation("title", "cannot be blank")
        assertEquals("title: cannot be blank", validation.userMessage())
        assertEquals("title: cannot be blank", validation.toMessage())

        val notFound = AppError.NotFound
        assertEquals("Item not found", notFound.userMessage())

        val ruleViolation = AppError.RuleViolation("Cannot schedule past due task")
        assertEquals("Cannot schedule past due task", ruleViolation.userMessage())

        val unexpected = AppError.Unexpected(IllegalStateException("disk full"))
        assertEquals("disk full", unexpected.userMessage())
    }

    @Test
    fun `getOrElse returns fallback on error and value on success`() {
        val ok = AppResult.Ok(42)
        assertEquals(42, ok.getOrElse { 0 })

        val err = AppResult.Err(AppError.NotFound)
        assertEquals(0, err.getOrElse { 0 })
    }
}
