package com.os11.domain.id

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class DeterministicIdsTest {

    @Test
    fun default_workspace_and_area_are_deterministic() {
        val wsId1 = DeterministicIds.defaultWorkspaceId()
        val wsId2 = DeterministicIds.defaultWorkspaceId()
        assertEquals(wsId1, wsId2)

        val areaId1 = DeterministicIds.defaultAreaId()
        val areaId2 = DeterministicIds.defaultAreaId()
        assertEquals(areaId1, areaId2)

        assertNotEquals(wsId1, areaId1)

        // Verify UUID RFC 4122 v5 format
        val wsUuid = UUID.fromString(wsId1)
        assertEquals(5, wsUuid.version())
        assertEquals(2, wsUuid.variant())
    }

    @Test
    fun recurring_occurrence_id_is_deterministic() {
        val id1 = DeterministicIds.recurringOccurrenceId("task-123", "2026-10-15")
        val id2 = DeterministicIds.recurringOccurrenceId("task-123", "2026-10-15")
        val id3 = DeterministicIds.recurringOccurrenceId("task-123", "2026-10-16")

        assertEquals(id1, id2)
        assertNotEquals(id1, id3)
    }

    @Test
    fun random_id_generator_produces_valid_uuid_v4() {
        val generator = DefaultIdGenerator()
        val id = generator.randomId()
        val uuid = UUID.fromString(id)
        assertEquals(4, uuid.version())
        assertTrue(id.isNotBlank())
    }
}
