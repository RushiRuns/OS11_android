package com.os11.domain.scheduling

import com.os11.domain.logic.scheduling.BucketType
import com.os11.domain.logic.scheduling.ScheduleState
import com.os11.domain.logic.scheduling.SchedulingAction
import com.os11.domain.logic.scheduling.SchedulingTransitions
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.InputStreamReader

@Serializable
data class SchedulingVector(
    val id: String,
    val initialBucket: String? = null,
    val initialDueDate: String? = null,
    val initialWaitingOn: String? = null,
    val initialFollowUpDate: String? = null,
    val actionType: String,
    val targetDate: String? = null,
    val expectedBucket: String? = null,
    val expectedDueDate: String? = null,
    val expectedWaitingOn: String? = null,
    val expectedFollowUpDate: String? = null,
    val expectedValid: Boolean,
)

class SchedulingTransitionsTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `all 27 scheduling transition vectors pass`() {
        val stream = javaClass.classLoader?.getResourceAsStream("vectors/scheduling_vectors.json")
            ?: error("vectors/scheduling_vectors.json not found")
        val content = InputStreamReader(stream).use { it.readText() }
        val vectors = json.decodeFromString<List<SchedulingVector>>(content)

        assertEquals("Must contain exactly 27 transition vectors", 27, vectors.size)

        for (vector in vectors) {
            val initialState = ScheduleState(
                bucket = vector.initialBucket,
                dueDate = vector.initialDueDate,
                waitingOn = vector.initialWaitingOn,
                followUpDate = vector.initialFollowUpDate,
            )

            val action: SchedulingAction = when (vector.actionType) {
                "SCHEDULE_DUE_DATE" -> SchedulingAction.ScheduleDueDate(
                    dueDate = vector.targetDate ?: error("Target date required for SCHEDULE_DUE_DATE"),
                )
                "MOVE_TO_BUCKET_ANYTIME" -> SchedulingAction.MoveToBucket(BucketType.ANYTIME)
                "MOVE_TO_BUCKET_SOMEDAY" -> SchedulingAction.MoveToBucket(BucketType.SOMEDAY)
                "CLEAR_TEMPORAL" -> SchedulingAction.ClearTemporalSchedule
                else -> error("Unknown actionType: ${vector.actionType}")
            }

            val result = SchedulingTransitions.transition(
                current = initialState,
                action = action,
                referenceDate = "2026-10-11",
                referenceTimestamp = "2026-10-11T10:00:00.000Z",
            )

            assertEquals("[${vector.id}] Validity mismatch", vector.expectedValid, result.isSuccess)
            assertEquals("[${vector.id}] Bucket mismatch", vector.expectedBucket, result.state.bucket)
            assertEquals("[${vector.id}] DueDate mismatch", vector.expectedDueDate, result.state.dueDate)
            assertEquals("[${vector.id}] WaitingOn mismatch", vector.expectedWaitingOn, result.state.waitingOn)
            assertEquals("[${vector.id}] FollowUpDate mismatch", vector.expectedFollowUpDate, result.state.followUpDate)
        }
    }

    @Test
    fun `waiting_on exceeding 120 chars fails validation`() {
        val longString = "A".repeat(121)
        val initial = ScheduleState()
        val result = SchedulingTransitions.transition(
            current = initial,
            action = SchedulingAction.SetWaiting(waitingOn = longString),
            referenceDate = "2026-10-11",
            referenceTimestamp = "2026-10-11T10:00:00.000Z",
        )
        assertTrue("Must fail on waiting_on > 120 chars", !result.isSuccess)
    }

    @Test
    fun `clear waiting removes waiting_since and follow_up`() {
        val initial = ScheduleState(
            waitingOn = "Sam",
            waitingSince = "2026-10-01",
            followUpDate = "2026-10-15",
            followUpNotifiedOn = "2026-10-15",
        )
        val result = SchedulingTransitions.transition(
            current = initial,
            action = SchedulingAction.ClearWaiting,
            referenceDate = "2026-10-11",
            referenceTimestamp = "2026-10-11T10:00:00.000Z",
        )
        assertTrue(result.isSuccess)
        assertEquals(null, result.state.waitingOn)
        assertEquals(null, result.state.waitingSince)
        assertEquals(null, result.state.followUpDate)
        assertEquals(null, result.state.followUpNotifiedOn)
    }
}
