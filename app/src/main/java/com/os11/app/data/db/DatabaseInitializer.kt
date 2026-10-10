package com.os11.app.data.db

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

        // 4. Default Settings
        val settingsToInsert = DEFAULT_SETTINGS.map { (key, value) ->
            SettingEntity(key = key, value = value)
        }
        db.settingDao().upsertAll(settingsToInsert)

        // 5. Default Modules
        val modulesToInsert = DEFAULT_MODULES.map { (name, enabled) ->
            ModuleEntity(moduleName = name, isEnabled = enabled)
        }
        db.moduleDao().upsertAll(modulesToInsert)

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
