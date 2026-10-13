package com.os11.app.data.db

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.execSQL

/**
 * Migration 1 -> 2: Adds the missing sync metadata columns to support bidirectional syncing.
 * Note: Workspace/area seeding was never defined in migrations. It is done by OS11DatabaseCallback.
 *
 * With BundledSQLiteDriver, Room calls migrate(SQLiteConnection). The default implementation of that
 * overload throws NotImplementedError for a real driver connection, so it has to be overridden here.
 * The SupportSQLiteDatabase overload is kept for builders that run without a driver.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE sync_queue ADD COLUMN retry_count INTEGER NOT NULL DEFAULT 0")
        connection.execSQL("ALTER TABLE sync_queue ADD COLUMN last_attempt_at TEXT")
    }

    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE sync_queue ADD COLUMN retry_count INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE sync_queue ADD COLUMN last_attempt_at TEXT")
    }
}
