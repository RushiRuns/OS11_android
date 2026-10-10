package com.os11.app.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class MigrationTest {

    @Test
    fun schema_version_1_json_is_valid_and_contains_all_tables() {
        val schemaFile = File("schemas/com.os11.app.data.db.OS11Database/1.json")
        assertTrue("Schema 1.json must exist in schemas directory", schemaFile.exists())

        val jsonString = schemaFile.readText()
        val jsonElement = Json.parseToJsonElement(jsonString)
        val databaseObject = jsonElement.jsonObject["database"]?.jsonObject
        assertNotNull(databaseObject)

        val version = databaseObject?.get("version")?.jsonPrimitive?.content
        assertEquals("1", version)

        val entities = databaseObject?.get("entities")?.jsonArray
        assertNotNull(entities)
        assertEquals("Schema version 1 must contain exactly 32 entities", 32, entities?.size)

        val tableNames = entities?.map { it.jsonObject["tableName"]?.jsonPrimitive?.content }
        val requiredTables = setOf(
            "local_identity", "workspaces", "areas", "projects", "sections",
            "tasks", "task_dependencies", "milestones", "tags", "task_tags",
            "reminders", "reminder_device_state", "attachments", "comments",
            "comment_reactions", "pomodoro_sessions", "goals", "goal_links",
            "notification_history", "task_history", "settings", "modules",
            "users", "devices", "collaboration_members", "sync_queue",
            "sync_field_clock", "sync_tombstone", "sync_local_state",
            "sync_peer_state", "sync_pending", "sync_conflict_log",
        )

        for (table in requiredTables) {
            assertTrue("Schema must contain table $table", tableNames?.contains(table) == true)
        }
    }
}
