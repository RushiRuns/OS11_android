package com.os11.domain.recurrence

import com.os11.domain.id.DeterministicIds
import com.os11.domain.logic.recurrence.RecurrenceEngine
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.io.InputStreamReader

@Serializable
data class RecurrenceVector(
    val id: String,
    val rrule: String,
    val basis: String,
    val currentDueDate: String,
    val completedDate: String,
    val sourceTaskId: String,
    val expectedNextDueDate: String,
)

class RecurrenceEngineTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `all shared recurrence vectors pass`() {
        val stream = javaClass.classLoader?.getResourceAsStream("vectors/recurrence_vectors.json")
            ?: error("vectors/recurrence_vectors.json not found")
        val content = InputStreamReader(stream).use { it.readText() }
        val vectors = json.decodeFromString<List<RecurrenceVector>>(content)

        for (vector in vectors) {
            val result = RecurrenceEngine.getNextOccurrence(
                recurrenceRule = vector.rrule,
                recurrenceBasis = vector.basis,
                currentDueDate = vector.currentDueDate,
                completedDate = vector.completedDate,
                sourceTaskId = vector.sourceTaskId,
            )

            assertNotNull("[${vector.id}] Result should not be null", result)
            assertEquals("[${vector.id}] Next due date mismatch", vector.expectedNextDueDate, result!!.nextDueDate)

            val expectedId = DeterministicIds.recurringOccurrenceId(vector.sourceTaskId, vector.expectedNextDueDate)
            assertEquals("[${vector.id}] Occurrence ID must be deterministic", expectedId, result.occurrenceId)
        }
    }

    @Test
    fun `skip occurrence advances to next occurrence strictly after current due date`() {
        val next = RecurrenceEngine.skipOccurrence(
            recurrenceRule = "FREQ=DAILY;INTERVAL=3",
            currentDueDate = "2026-10-10",
        )
        assertEquals("2026-10-13", next)
    }

    @Test
    fun `deterministic occurrence IDs are stable and identical for same task and date`() {
        val id1 = DeterministicIds.recurringOccurrenceId("task-100", "2026-10-25")
        val id2 = DeterministicIds.recurringOccurrenceId("task-100", "2026-10-25")
        val id3 = DeterministicIds.recurringOccurrenceId("task-100", "2026-10-26")

        assertEquals(id1, id2)
        assert(id1 != id3)
    }
}
