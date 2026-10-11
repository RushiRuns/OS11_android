package com.os11.domain.logic.recurrence

import com.os11.domain.id.DeterministicIds
import org.dmfs.rfc5545.DateTime
import org.dmfs.rfc5545.recur.InvalidRecurrenceRuleException
import org.dmfs.rfc5545.recur.RecurrenceRule
import java.util.Locale

/**
 * Result of computing the next occurrence of a recurring task.
 */
data class NextOccurrenceResult(val nextDueDate: String, val occurrenceId: String)

/**
 * Pure Kotlin/JVM RecurrenceEngine over lib-recur (RFC 5545).
 *
 * Implements:
 * 1. Fixed basis: next occurrence is computed based on scheduled due date.
 * 2. After-completion basis: next occurrence is computed starting from the completion date.
 * 3. Skip occurrence: advances to next occurrence strictly after the current due date.
 * 4. Deterministic occurrence ID generation via RFC 4122 UUIDv5 (DeterministicIds).
 */
@Suppress("ReturnCount", "MagicNumber")
object RecurrenceEngine {

    private const val MAX_EXPANSION_ITERATIONS = 500

    fun getNextOccurrence(
        recurrenceRule: String,
        recurrenceBasis: String = "fixed",
        currentDueDate: String,
        completedDate: String? = null,
        sourceTaskId: String,
    ): NextOccurrenceResult? {
        val cleanRule = recurrenceRule.trim().removePrefix("RRULE:").trim()
        if (cleanRule.isEmpty()) return null

        val rrule = try {
            RecurrenceRule(cleanRule)
        } catch (_: InvalidRecurrenceRuleException) {
            return null
        } catch (_: IllegalArgumentException) {
            return null
        }

        val startDt = parseDate(currentDueDate) ?: return null
        val cutoffDt = if (recurrenceBasis.equals("after_completion", ignoreCase = true) && completedDate != null) {
            parseDate(completedDate) ?: startDt
        } else {
            startDt
        }

        val iterator = rrule.iterator(cutoffDt)
        var count = 0
        while (iterator.hasNext() && count < MAX_EXPANSION_ITERATIONS) {
            count++
            val candidate = iterator.nextDateTime()
            if (candidate.after(cutoffDt)) {
                val formattedDate = formatDate(candidate)
                val occurrenceId = DeterministicIds.recurringOccurrenceId(sourceTaskId, formattedDate)
                return NextOccurrenceResult(
                    nextDueDate = formattedDate,
                    occurrenceId = occurrenceId,
                )
            }
        }
        return null
    }

    /**
     * Advance occurrence without completing (skip occurrence).
     */
    fun skipOccurrence(recurrenceRule: String, currentDueDate: String): String? {
        val cleanRule = recurrenceRule.trim().removePrefix("RRULE:").trim()
        if (cleanRule.isEmpty()) return null

        val rrule = try {
            RecurrenceRule(cleanRule)
        } catch (_: Exception) {
            return null
        }

        val startDt = parseDate(currentDueDate) ?: return null
        val iterator = rrule.iterator(startDt)
        var count = 0
        while (iterator.hasNext() && count < MAX_EXPANSION_ITERATIONS) {
            count++
            val candidate = iterator.nextDateTime()
            if (candidate.after(startDt)) {
                return formatDate(candidate)
            }
        }
        return null
    }

    private fun parseDate(isoDate: String): DateTime? {
        val parts = isoDate.split("-")
        if (parts.size != 3) return null
        val year = parts[0].toIntOrNull() ?: return null
        val month = parts[1].toIntOrNull() ?: return null
        val day = parts[2].toIntOrNull() ?: return null
        // dmfs DateTime(year, month - 1, day) has 0-indexed month
        return DateTime(year, month - 1, day)
    }

    private fun formatDate(dt: DateTime): String {
        val year = dt.year
        val month = dt.month + 1
        val day = dt.dayOfMonth
        return String.format(Locale.US, "%04d-%02d-%02d", year, month, day)
    }
}
