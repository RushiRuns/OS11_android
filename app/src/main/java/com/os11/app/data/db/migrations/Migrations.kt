package com.os11.app.data.db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `version_history` (
                `id` TEXT NOT NULL PRIMARY KEY,
                `entity_type` TEXT NOT NULL,
                `entity_id` TEXT NOT NULL,
                `version_number` INTEGER NOT NULL,
                `snapshot_data` TEXT NOT NULL,
                `created_at` TEXT NOT NULL
            )
            """.trimIndent(),
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `idx_version_history_entity` ON `version_history` (`entity_type`, `entity_id`)",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `idx_version_history_created` ON `version_history` (`created_at`)",
        )
    }
}
