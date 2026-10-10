package com.os11.domain.validation

import com.os11.domain.logic.validation.DependencyCycleCheck
import com.os11.domain.logic.validation.ParentCycleCheck
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CycleChecksTest {

    @Test
    fun dependency_self_dependency_creates_cycle() {
        assertTrue(DependencyCycleCheck.wouldCreateCycle(emptyList(), "A", "A"))
    }

    @Test
    fun dependency_direct_cycle_detected() {
        val edges = listOf("A" to "B")
        assertTrue(DependencyCycleCheck.wouldCreateCycle(edges, "B", "A"))
    }

    @Test
    fun dependency_transitive_cycle_detected() {
        val edges = listOf(
            "A" to "B",
            "B" to "C",
            "C" to "D",
        )
        assertTrue(DependencyCycleCheck.wouldCreateCycle(edges, "D", "A"))
        assertFalse(DependencyCycleCheck.wouldCreateCycle(edges, "A", "E"))
    }

    @Test
    fun dependency_valid_diamond_dag_allowed() {
        val edges = listOf(
            "A" to "B",
            "A" to "C",
            "B" to "D",
        )
        assertFalse(DependencyCycleCheck.wouldCreateCycle(edges, "C", "D"))
    }

    @Test
    fun parent_self_cycle_detected() {
        assertTrue(ParentCycleCheck.wouldCreateCycle("A", "A") { null })
    }

    @Test
    fun parent_transitive_cycle_detected() {
        // Tree: Root <- C <- B <- A
        val parents = mapOf(
            "A" to "B",
            "B" to "C",
            "C" to "Root",
        )
        // Trying to make A the parent of C (i.e. C.parent = A)
        assertTrue(ParentCycleCheck.wouldCreateCycle("C", "A") { parents[it] })
        // Moving A to Root or null is valid
        assertFalse(ParentCycleCheck.wouldCreateCycle("A", "Root") { parents[it] })
        assertFalse(ParentCycleCheck.wouldCreateCycle("A", null) { parents[it] })
    }
}
