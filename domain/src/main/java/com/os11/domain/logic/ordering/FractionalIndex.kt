package com.os11.domain.logic.ordering

/**
 * Fractional indexing helper for floating-point sort_order columns.
 *
 * Provides deterministic position generation:
 * - atStart: prepends before first item
 * - atEnd: appends after last item
 * - between: generates midpoint between two neighbors
 */
object FractionalIndex {
    const val STEP = 1.0
    const val MIN_GAP = 1e-9

    fun atStart(first: Double?): Double {
        if (first == null) return 0.0
        return first - STEP
    }

    fun atEnd(last: Double?): Double {
        if (last == null) return 0.0
        return last + STEP
    }

    fun between(before: Double, after: Double): Double = if (before >= after) {
        before + (STEP / 2.0)
    } else {
        val gap = after - before
        before + (gap / 2.0)
    }

    fun isClose(a: Double, b: Double): Boolean = kotlin.math.abs(a - b) < MIN_GAP
}
