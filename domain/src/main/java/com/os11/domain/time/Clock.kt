package com.os11.domain.time

import java.time.Instant

/**
 * Clock abstraction for testable time across pure domain and services.
 */
interface Clock {
    fun wallMs(): Long
    fun nowInstant(): Instant = Instant.ofEpochMilli(wallMs())
    fun nowIso(): String = nowInstant().toString()

    fun today(dayStartsAt: String = "04:00"): String {
        val instant = nowInstant()
        val zone = java.time.ZoneOffset.UTC
        val zdt = instant.atZone(zone)
        val parts = dayStartsAt.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: DEFAULT_DAY_START_HOUR
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
        val cutoff = zdt.toLocalDate().atTime(hour, minute).atZone(zone)
        val effectiveDate = if (zdt.isBefore(cutoff)) zdt.toLocalDate().minusDays(1) else zdt.toLocalDate()
        return effectiveDate.toString()
    }

    companion object {
        const val DEFAULT_DAY_START_HOUR = 4
    }
}

/**
 * Production clock backed by System.currentTimeMillis().
 */
class SystemClock : Clock {
    override fun wallMs(): Long = System.currentTimeMillis()
}

/**
 * Controllable clock for unit and property testing.
 */
class TestClock(private var currentMs: Long = 0L) : Clock {
    override fun wallMs(): Long = currentMs

    fun advanceBy(deltaMs: Long) {
        currentMs += deltaMs
    }

    fun set(millis: Long) {
        currentMs = millis
    }
}
