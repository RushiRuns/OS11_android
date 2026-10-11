package com.os11.app.data

import androidx.sqlite.db.SupportSQLiteDatabase
import com.os11.app.data.db.migrations.MIGRATION_1_2
import io.mockk.mockk
import io.mockk.verify
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
            .takeIf { it.exists() } ?: File("app/schemas/com.os11.app.data.db.OS11Database/1.json")
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

    @Test
    fun migration_1_2_executes_ddl_for_version_history() {
        val mockDb = mockk<SupportSQLiteDatabase>(relaxed = true)
        MIGRATION_1_2.migrate(mockDb)

        verify {
            mockDb.execSQL(match { it.contains("CREATE TABLE IF NOT EXISTS `version_history`") })
            mockDb.execSQL(match { it.contains("CREATE INDEX IF NOT EXISTS `idx_version_history_entity`") })
            mockDb.execSQL(match { it.contains("CREATE INDEX IF NOT EXISTS `idx_version_history_created`") })
        }
    }

    @Test
    fun schema_version_2_json_is_valid_and_contains_all_tables_including_version_history() {
        val schemaFile = File("schemas/com.os11.app.data.db.OS11Database/2.json")
            .takeIf { it.exists() } ?: File("app/schemas/com.os11.app.data.db.OS11Database/2.json")
        assertTrue("Schema 2.json must exist in schemas directory", schemaFile.exists())

        val jsonString = schemaFile.readText()
        val jsonElement = Json.parseToJsonElement(jsonString)
        val databaseObject = jsonElement.jsonObject["database"]?.jsonObject
        assertNotNull(databaseObject)

        val version = databaseObject?.get("version")?.jsonPrimitive?.content
        assertEquals("2", version)

        val entities = databaseObject?.get("entities")?.jsonArray
        assertNotNull(entities)
        assertEquals("Schema version 2 must contain exactly 33 entities", 33, entities?.size)

        val tableNames = entities?.map { it.jsonObject["tableName"]?.jsonPrimitive?.content }
        assertTrue("Schema must contain table version_history", tableNames?.contains("version_history") == true)
    }
}
