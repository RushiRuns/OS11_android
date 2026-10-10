package com.os11.domain.logic.sync

import com.os11.domain.time.Clock
import java.util.Locale

/**
 * Hybrid Logical Clock (HLC).
 *
 * Format: `<wallMs 13 digits zero-padded>-<counter 4 hex>-<device 8 hex>`
 * Example: `1760086800000-0003-9f3c21ab`
 * Lexicographical comparison matches chronological order.
 */
@JvmInline
value class Hlc(val raw: String) : Comparable<Hlc> {
    override fun compareTo(other: Hlc): Int = raw.compareTo(other.raw)

    val wallMs: Long
        get() = raw.substring(0, WALL_END_INDEX).toLong()

    val counter: Int
        get() = raw.substring(COUNTER_START_INDEX, COUNTER_END_INDEX).toInt(HEX_RADIX)

    val deviceId: String
        get() = raw.substring(DEVICE_START_INDEX)

    companion object {
        const val RAW_LENGTH = 27
        const val WALL_END_INDEX = 13
        const val COUNTER_START_INDEX = 14
        const val COUNTER_END_INDEX = 18
        const val DEVICE_START_INDEX = 19
        const val HEX_RADIX = 16
        const val DEVICE_HEX_LENGTH = 8
        const val COUNTER_HEX_MASK = 0xFFFF

        const val SEED_RAW = "0000000000000-0000-00000000"
        val SEED = Hlc(SEED_RAW)

        const val MAX_SKEW_MS = 24L * 60 * 60 * 1000 // 24 hours

        fun format(wallMs: Long, counter: Int, deviceId: String): Hlc {
            val normalizedDevice = deviceId.replace("-", "").lowercase(Locale.US)
                .take(DEVICE_HEX_LENGTH).padStart(DEVICE_HEX_LENGTH, '0')
            val raw = String.format(Locale.US, "%013d-%04x-%s", wallMs, counter and COUNTER_HEX_MASK, normalizedDevice)
            return Hlc(raw)
        }

        fun parse(raw: String): Hlc {
            require(raw.length == RAW_LENGTH && raw[WALL_END_INDEX] == '-' && raw[COUNTER_END_INDEX] == '-') {
                "Invalid HLC format: '$raw'. Expected '<13-digits>-<4-hex>-<8-hex>'."
            }
            return Hlc(raw)
        }

        fun isSkewed(remote: Hlc, nowMs: Long): Boolean = remote.wallMs > nowMs + MAX_SKEW_MS
    }
}

class ClockSkewException(message: String) : Exception(message)

interface HlcClock {
    fun tick(): Hlc
    fun receive(remote: Hlc): Hlc
    fun current(): Hlc
}

/**
 * Standard implementation of HlcClock maintaining the latest HLC state.
 */
class DefaultHlcClock(private val clock: Clock, val deviceId: String, initialHlc: Hlc = Hlc.SEED) : HlcClock {
    private var last: Hlc = initialHlc

    @Synchronized
    override fun tick(): Hlc {
        val now = clock.wallMs()
        val (wall, counter) = if (now > last.wallMs) {
            now to 0
        } else {
            last.wallMs to (last.counter + 1)
        }
        val next = Hlc.format(wall, counter, deviceId)
        last = next
        return next
    }

    @Synchronized
    override fun receive(remote: Hlc): Hlc {
        val now = clock.wallMs()
        if (Hlc.isSkewed(remote, now)) {
            throw ClockSkewException(
                "Remote HLC clock skew exceeds 24h threshold: remote wall=${remote.wallMs}, now=$now",
            )
        }

        val wall = maxOf(last.wallMs, remote.wallMs, now)
        val counter = when {
            wall == last.wallMs && wall == remote.wallMs -> maxOf(last.counter, remote.counter) + 1
            wall == last.wallMs -> last.counter + 1
            wall == remote.wallMs -> remote.counter + 1
            else -> 0
        }
        val next = Hlc.format(wall, counter, deviceId)
        last = next
        return next
    }

    @Synchronized
    override fun current(): Hlc = last
}
