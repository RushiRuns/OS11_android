package com.os11.domain.time

import java.time.Instant

/**
 * Clock abstraction for testable time across pure domain and services.
 */
interface Clock {
    fun wallMs(): Long
    fun nowInstant(): Instant = Instant.ofEpochMilli(wallMs())
    fun nowIso(): String = nowInstant().toString()
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
