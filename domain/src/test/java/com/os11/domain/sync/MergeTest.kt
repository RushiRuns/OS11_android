package com.os11.domain.sync

import com.os11.domain.logic.sync.Change
import com.os11.domain.logic.sync.FieldWrite
import com.os11.domain.logic.sync.Hlc
import com.os11.domain.logic.sync.Merge
import com.os11.domain.logic.sync.MergeDecision
import com.os11.domain.logic.sync.MergeGroups
import com.os11.domain.logic.sync.Operation
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MergeTest {

    private val json = Json { prettyPrint = false }

    @Test
    fun mergeField_idempotent() {
        val hlcA = Hlc.format(1000L, 1, "dev1").raw
        val decision = Merge.mergeField(hlcA, hlcA)
        assertEquals(MergeDecision.KEEP_LOCAL, decision)
    }

    @Test
    fun mergeField_convergence_properties() {
        val h1 = Hlc.format(1000L, 0, "dev1").raw
        val h2 = Hlc.format(2000L, 0, "dev2").raw
        val h3 = Hlc.format(3000L, 0, "dev3").raw

        val f1 = FieldWrite(JsonPrimitive("val1"), h1)
        val f2 = FieldWrite(JsonPrimitive("val2"), h2)
        val f3 = FieldWrite(JsonPrimitive("val3"), h3)

        // Commutativity: winning value between f1 and f2 is f2 regardless of order
        val map12 = Merge.mergeFields(mapOf("title" to f1), mapOf("title" to f2))
        val map21 = Merge.mergeFields(mapOf("title" to f2), mapOf("title" to f1))
        assertEquals(map12["title"]?.value, map21["title"]?.value)
        assertEquals(JsonPrimitive("val2"), map12["title"]?.value)

        // Associativity: merge(merge(f1, f2), f3) == merge(f1, merge(f2, f3))
        val step1 = Merge.mergeFields(mapOf("title" to f1), mapOf("title" to f2))
        val res123A = Merge.mergeFields(step1, mapOf("title" to f3))

        val step2 = Merge.mergeFields(mapOf("title" to f2), mapOf("title" to f3))
        val res123B = Merge.mergeFields(mapOf("title" to f1), step2)

        assertEquals(res123A["title"]?.value, res123B["title"]?.value)
        assertEquals(JsonPrimitive("val3"), res123A["title"]?.value)
    }

    @Test
    fun delete_and_resurrection_rules() {
        val oldClock = Hlc.format(1000L, 0, "dev1").raw
        val deleteClock = Hlc.format(2000L, 0, "dev1").raw
        val newerClock = Hlc.format(3000L, 0, "dev2").raw

        // Delete succeeds if local clock is older
        assertTrue(Merge.shouldApplyDelete(listOf(oldClock), deleteClock))
        // Delete fails if local clock is newer
        assertFalse(Merge.shouldApplyDelete(listOf(newerClock), deleteClock))

        // Resurrection: incoming edit newer than tombstone resurrects entity
        assertTrue(Merge.shouldResurrect(deleteClock, listOf(newerClock)))
        assertFalse(Merge.shouldResurrect(deleteClock, listOf(oldClock)))
    }

    @Test
    fun mergeGroups_mappings() {
        assertEquals("when", MergeGroups.getGroupName("tasks", "bucket"))
        assertEquals("when", MergeGroups.getGroupName("tasks", "due_date"))
        assertEquals("myday", MergeGroups.getGroupName("tasks", "scheduled_start_min"))
        assertEquals("status", MergeGroups.getGroupName("projects", "status"))

        // Independent field returns itself
        assertEquals("title", MergeGroups.getGroupName("tasks", "title"))
        assertEquals("notes", MergeGroups.getGroupName("tasks", "notes"))
    }

    @Test
    fun change_serialization_roundtrip() {
        val change = Change(
            id = "test-uuid-1",
            operation = Operation.UPSERT,
            entity = "tasks",
            entityId = "task-uuid-1",
            fields = mapOf(
                "title" to FieldWrite(JsonPrimitive("Buy milk"), "0000000001000-0000-dev10000"),
                "priority" to FieldWrite(JsonPrimitive(3), "0000000001000-0000-dev10000"),
            ),
        )

        val serialized = json.encodeToString(change)
        val deserialized = json.decodeFromString<Change>(serialized)

        assertEquals(change, deserialized)
    }
}
