package com.os11.domain.ordering

import com.os11.domain.logic.ordering.FractionalIndex
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FractionalIndexTest {

    @Test
    fun atStart_generates_correct_order() {
        assertEquals(0.0, FractionalIndex.atStart(null), 1e-6)
        assertEquals(4.0, FractionalIndex.atStart(5.0), 1e-6)
        assertEquals(-2.0, FractionalIndex.atStart(-1.0), 1e-6)
    }

    @Test
    fun atEnd_generates_correct_order() {
        assertEquals(0.0, FractionalIndex.atEnd(null), 1e-6)
        assertEquals(6.0, FractionalIndex.atEnd(5.0), 1e-6)
        assertEquals(0.0, FractionalIndex.atEnd(-1.0), 1e-6)
    }

    @Test
    fun between_generates_midpoint() {
        assertEquals(1.5, FractionalIndex.between(1.0, 2.0), 1e-6)
        assertEquals(0.0, FractionalIndex.between(-1.0, 1.0), 1e-6)

        // Monotonic insertion
        val before = 1.0
        val after = 2.0
        val mid = FractionalIndex.between(before, after)
        assertTrue(before < mid)
        assertTrue(mid < after)

        val nextMid = FractionalIndex.between(before, mid)
        assertTrue(before < nextMid)
        assertTrue(nextMid < mid)
    }

    @Test
    fun between_handles_tight_gap_and_extremes() {
        val before = 1.0000000001
        val after = 1.0000000002
        val mid = FractionalIndex.between(before, after)
        assertTrue(mid > before)
        assertTrue(mid < after)

        // Equal or inverted bounds
        val invertedMid = FractionalIndex.between(5.0, 3.0)
        assertTrue(invertedMid > 5.0)
    }
}
