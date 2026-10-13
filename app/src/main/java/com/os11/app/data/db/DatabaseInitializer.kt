package com.os11.app.data.db

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteStatement
import androidx.sqlite.db.SupportSQLiteDatabase
import com.os11.app.data.db.entities.AreaEntity
import com.os11.app.data.db.entities.LocalIdentityEntity
import com.os11.app.data.db.entities.ModuleEntity
import com.os11.app.data.db.entities.SettingEntity
import com.os11.app.data.db.entities.SyncLocalStateEntity
import com.os11.app.data.db.entities.WorkspaceEntity
import com.os11.domain.id.DefaultIdGenerator
import com.os11.domain.id.DeterministicIds
import com.os11.domain.logic.sync.Hlc
import com.os11.domain.time.Clock
import com.os11.domain.time.SystemClock

/**
 * Deterministic database seeder for initial app launch.
 * Seeds default workspace, default area, settings defaults, and module toggles per SCHEMA.md.
 *
 * There are three entry points and they all describe the same rows:
 *  - seedDefaults(SQLiteConnection): the path Room uses when a SQLiteDriver (BundledSQLiteDriver) is set.
 *    With a driver, Room never calls the SupportSQLiteDatabase callback overloads, which is why the old
 *    seeding never ran.
 *  - seedDefaults(SupportSQLiteDatabase): legacy path, still used by in-memory builders without a driver.
 *  - seedDefaults(OS11Database): DAO based, for tests and manual repair.
 *
 * Every statement is idempotent (INSERT OR IGNORE, or guarded), so running on every open is safe and also
 * repairs installs that were created while seeding was not running.
 */
internal object DatabaseInitializer {

    private const val SEED_TIMESTAMP = "1970-01-01T00:00:00.000Z"

    val DEFAULT_SETTINGS: Map<String, String> = mapOf(
        "accent_color" to "\"#4da3ff\"",
        "font_size" to "\"md\"",
        "density" to "\"comfortable\"",
        "day_starts_at" to "\"00:00\"",
        "pomodoro_work_minutes" to "25",
        "pomodoro_break_minutes" to "5",
        "pomodoro_long_break_minutes" to "15",
        "pomodoro_sessions_before_long_break" to "4",
        "active_profile_preset" to "\"custom\"",
        "app_lock_enabled" to "false",
        "reduce_motion" to "false",
        "task_card_style" to "\"default\"",
        "waiting_stale_days" to "7",
        "follow_up_notify_time" to "\"09:00\"",
        "show_someday_projects_in_browse" to "false",
        "show_stalled_marker" to "true",
        "follow_ups" to "true",
        "haptics_enabled" to "true",
        "secure_screen" to "false",
        "sync_attachment_max_mb" to "25",
        "onboarding_completed" to "false",
    )

    val DEFAULT_MODULES: Map<String, Boolean> = mapOf(
        "my_day" to true,
        "project_management" to true,
        "pomodoro" to true,
        "agenda" to true,
        "goals_habits" to true,
        "dashboard" to true,
        "file_attachments" to true,
        "nlp_parsing" to true,
        "anytime" to false,
        "someday" to false,
        "waiting_for" to false,
        "habit_tracker" to false,
        "collaboration" to false,
        "companion_sync" to false,
    )

    private class SeedStatement(val sql: String, val args: List<Any>)

    private fun seedStatements(clock: Clock): List<SeedStatement> {
        val workspaceId = DeterministicIds.defaultWorkspaceId()
        val areaId = DeterministicIds.defaultAreaId()
        val statements = mutableListOf<SeedStatement>()

        // 1. Local identity, only when the table is empty
        statements += SeedStatement(
            "INSERT INTO local_identity (id, display_name, avatar_emoji, created_at) " +
                "SELECT ?, ?, ?, ? WHERE NOT EXISTS (SELECT 1 FROM local_identity)",
            listOf(DefaultIdGenerator().randomId(), "My Android", "📱", clock.nowIso()),
        )

        // 2. Default workspace (tasks.workspace_id has a foreign key to this row)
        statements += SeedStatement(
            "INSERT OR IGNORE INTO workspaces (id, name, created_at, updated_at) VALUES (?, ?, ?, ?)",
            listOf(workspaceId, "Personal", SEED_TIMESTAMP, SEED_TIMESTAMP),
        )

        // 3. Default area
        statements += SeedStatement(
            "INSERT OR IGNORE INTO areas " +
                "(id, workspace_id, name, icon, color, sort_order, is_default, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
            listOf(areaId, workspaceId, "Personal", "folder", "#4da3ff", 0.0, 1, SEED_TIMESTAMP, SEED_TIMESTAMP),
        )

        // 4. Default settings, never overwriting a value the user already changed
        for ((key, value) in DEFAULT_SETTINGS) {
            statements += SeedStatement(
                "INSERT OR IGNORE INTO settings (key, value) VALUES (?, ?)",
                listOf(key, value),
            )
        }

        // 5. Default modules, same rule
        for ((name, enabled) in DEFAULT_MODULES) {
            statements += SeedStatement(
                "INSERT OR IGNORE INTO modules (module_name, is_enabled) VALUES (?, ?)",
                listOf(name, if (enabled) 1 else 0),
            )
        }

        // 6. Sync local state
        statements += SeedStatement(
            "INSERT OR IGNORE INTO sync_local_state (id, last_hlc) VALUES (?, ?)",
            listOf(1L, Hlc.SEED_RAW),
        )

        return statements
    }

    private fun SQLiteStatement.bindAll(args: List<Any>) {
        args.forEachIndexed { i, arg ->
            val index = i + 1
            when (arg) {
                is String -> bindText(index, arg)
                is Long -> bindLong(index, arg)
                is Int -> bindLong(index, arg.toLong())
                is Double -> bindDouble(index, arg)
                else -> error("Unsupported seed argument type: ${arg::class}")
            }
        }
    }

    /** Driver path. Called from OS11DatabaseCallback.onCreate/onOpen(SQLiteConnection). */
    fun seedDefaults(connection: SQLiteConnection, clock: Clock = SystemClock()) {
        for (seed in seedStatements(clock)) {
            connection.prepare(seed.sql).use { stmt ->
                stmt.bindAll(seed.args)
                stmt.step()
            }
        }
    }

    /** Legacy SupportSQLite path. */
    fun seedDefaults(db: SupportSQLiteDatabase, clock: Clock = SystemClock()) {
        for (seed in seedStatements(clock)) {
            db.execSQL(seed.sql, seed.args.toTypedArray())
        }
    }

    /** DAO path. Inserts only what is missing, so it never resets user settings or module toggles. */
    suspend fun seedDefaults(db: OS11Database, clock: Clock = SystemClock()) {
        val workspaceId = DeterministicIds.defaultWorkspaceId()
        val areaId = DeterministicIds.defaultAreaId()

        // 1. Local Identity
        val existingIdentity = db.localIdentityDao().get()
        if (existingIdentity == null) {
            val id = DefaultIdGenerator().randomId()
            db.localIdentityDao().upsert(
                LocalIdentityEntity(
                    id = id,
                    displayName = "My Android",
                    avatarEmoji = "📱",
                    createdAt = clock.nowIso(),
                ),
            )
        }

        // 2. Default Workspace
        if (db.workspaceDao().getById(workspaceId) == null) {
            db.workspaceDao().upsert(
                WorkspaceEntity(
                    id = workspaceId,
                    name = "Personal",
                    createdAt = SEED_TIMESTAMP,
                    updatedAt = SEED_TIMESTAMP,
                ),
            )
        }

        // 3. Default Area
        if (db.areaDao().getById(areaId) == null) {
            db.areaDao().upsert(
                AreaEntity(
                    id = areaId,
                    workspaceId = workspaceId,
                    name = "Personal",
                    icon = "folder",
                    color = "#4da3ff",
                    sortOrder = 0.0,
                    isDefault = true,
                    createdAt = SEED_TIMESTAMP,
                    updatedAt = SEED_TIMESTAMP,
                ),
            )
        }

        // 4. Default Settings (missing keys only)
        for ((key, value) in DEFAULT_SETTINGS) {
            if (db.settingDao().getValue(key) == null) {
                db.settingDao().upsert(SettingEntity(key = key, value = value))
            }
        }

        // 5. Default Modules (missing rows only)
        for ((name, enabled) in DEFAULT_MODULES) {
            if (db.moduleDao().isEnabled(name) == null) {
                db.moduleDao().upsert(ModuleEntity(moduleName = name, isEnabled = enabled))
            }
        }

        // 6. Sync Local State
        if (db.syncLocalStateDao().get() == null) {
            db.syncLocalStateDao().set(
                SyncLocalStateEntity(
                    id = 1,
                    lastHlc = Hlc.SEED_RAW,
                ),
            )
        }
    }
}
