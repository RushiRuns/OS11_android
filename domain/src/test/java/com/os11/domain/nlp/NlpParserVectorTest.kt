package com.os11.domain.nlp

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

@Serializable
data class NlpTestVector(
    val id: String,
    val input: String,
    val referenceDate: String,
    val referenceTime: String,
    val expectedTitle: String,
    val expectedDueDate: String? = null,
    val expectedDueTime: String? = null,
    val expectedPriority: Int = 0,
    val expectedArea: String? = null,
    val expectedProject: String? = null,
    val expectedTags: List<String> = emptyList(),
    val expectedRecurrence: String? = null,
    val expectedPomodoros: Int? = null,
)

class NlpParserVectorTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun testAllSharedVectorsPass() {
        val stream = javaClass.classLoader.getResourceAsStream("vectors/nlp_vectors.json")
            ?: error("vectors/nlp_vectors.json not found in test resources")
        val content = stream.bufferedReader().use { it.readText() }
        val vectors = json.decodeFromString<List<NlpTestVector>>(content)

        assertTrue("Expected at least 10 vectors", vectors.size >= 10)

        for (vector in vectors) {
            val refDate = LocalDate.parse(vector.referenceDate)
            val refTime = LocalTime.parse(vector.referenceTime)

            val result = NlpParser.parse(
                input = vector.input,
                referenceDate = refDate,
                referenceTime = refTime,
            )

            assertEquals("[${vector.id}] Title mismatch", vector.expectedTitle, result.title)
            assertEquals("[${vector.id}] DueDate mismatch", vector.expectedDueDate, result.dueDate)
            assertEquals("[${vector.id}] DueTime mismatch", vector.expectedDueTime, result.dueTime)
            assertEquals("[${vector.id}] Priority mismatch", vector.expectedPriority, result.priority)
            assertEquals("[${vector.id}] Area mismatch", vector.expectedArea, result.areaName)
            assertEquals("[${vector.id}] Project mismatch", vector.expectedProject, result.projectName)
            assertEquals("[${vector.id}] Tags mismatch", vector.expectedTags, result.tags)
            assertEquals("[${vector.id}] Recurrence mismatch", vector.expectedRecurrence, result.recurrenceRule)
            assertEquals("[${vector.id}] Pomodoro mismatch", vector.expectedPomodoros, result.estimatedPomodoros)
        }
    }

    @Test
    fun testParseLatencyUnderBudget() {
        val input = "Prepare keynote speech tomorrow at 4pm @Personal /Speaking #conference !high every week 🍅"
        val refDate = LocalDate.of(2026, 10, 10)
        val refTime = LocalTime.of(10, 0)

        // Warm up
        repeat(100) {
            NlpParser.parse(input, refDate, refTime)
        }

        // Measure 1,000 runs
        val start = System.nanoTime()
        val iterations = 1_000
        repeat(iterations) {
            NlpParser.parse(input, refDate, refTime)
        }
        val elapsedMs = (System.nanoTime() - start) / 1_000_000.0
        val avgMs = elapsedMs / iterations

        // Performance requirement: NLP parse P95 < 30ms. Pure Kotlin is typically < 0.1ms.
        assertTrue("Average parse time ($avgMs ms) should be well under 30ms", avgMs < 1.0)
    }
}
