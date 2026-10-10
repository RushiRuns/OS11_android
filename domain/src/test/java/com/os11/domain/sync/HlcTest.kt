package com.os11.domain.sync

import com.os11.domain.logic.sync.ClockSkewException
import com.os11.domain.logic.sync.DefaultHlcClock
import com.os11.domain.logic.sync.Hlc
import com.os11.domain.time.TestClock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HlcTest {

    @Test
    fun parse_and_format_roundtrip() {
        val original = Hlc.format(1760086800000L, 3, "9f3c21ab")
        assertEquals("1760086800000-0003-9f3c21ab", original.raw)
        assertEquals(1760086800000L, original.wallMs)
        assertEquals(3, original.counter)
        assertEquals("9f3c21ab", original.deviceId)

        val parsed = Hlc.parse(original.raw)
        assertEquals(original, parsed)
    }

    @Test
    fun compareTo_orders_chronologically() {
        val hlc1 = Hlc.format(1000L, 0, "device01")
        val hlc2 = Hlc.format(1000L, 1, "device01")
        val hlc3 = Hlc.format(2000L, 0, "device01")
        val hlc4 = Hlc.format(2000L, 0, "device02")

        assertTrue(hlc1 < hlc2)
        assertTrue(hlc2 < hlc3)
        assertTrue(hlc3 < hlc4)
    }

    @Test
    fun local_event_ticks_wall_or_increments_counter() {
        val clock = TestClock(1000L)
        val hlcClock = DefaultHlcClock(clock, "dev1")

        val h1 = hlcClock.tick()
        assertEquals(1000L, h1.wallMs)
        assertEquals(0, h1.counter)

        // Same time ticks counter
        val h2 = hlcClock.tick()
        assertEquals(1000L, h2.wallMs)
        assertEquals(1, h2.counter)

        // Advancing wall resets counter
        clock.advanceBy(500L)
        val h3 = hlcClock.tick()
        assertEquals(1500L, h3.wallMs)
        assertEquals(0, h3.counter)
    }

    @Test
    fun receive_folds_remote_hlc_correctly() {
        val clock = TestClock(1000L)
        val hlcClock = DefaultHlcClock(clock, "dev1")
        hlcClock.tick() // 1000L, 0, dev1

        // Receive remote with higher wall
        val remote1 = Hlc.format(2000L, 5, "dev2")
        val next1 = hlcClock.receive(remote1)
        assertEquals(2000L, next1.wallMs)
        assertEquals(6, next1.counter)

        // Receive remote with same wall and higher counter
        val remote2 = Hlc.format(2000L, 10, "dev3")
        val next2 = hlcClock.receive(remote2)
        assertEquals(2000L, next2.wallMs)
        assertEquals(11, next2.counter)
    }

    @Test(expected = ClockSkewException::class)
    fun receive_rejects_excessive_clock_skew() {
        val clock = TestClock(1000L)
        val hlcClock = DefaultHlcClock(clock, "dev1")

        // 25 hours ahead
        val skewedWall = 1000L + 25L * 60 * 60 * 1000
        val skewedRemote = Hlc.format(skewedWall, 0, "dev2")
        hlcClock.receive(skewedRemote)
    }

    @Test
    fun seed_constant_has_lowest_ordering() {
        val realHlc = Hlc.format(100L, 0, "dev1")
        assertTrue(Hlc.SEED < realHlc)
        assertFalse(Hlc.isSkewed(realHlc, 1000L))
    }
}
