# OS11 Android - Data Schema

> The data model is treated as sacred.
> No AI tool modifies this file or a migration without a human reviewing the migration path.
> Every change here gets a Room migration, a migration test, and the exported schema JSON in `app/schemas/`.
> All dates are ISO 8601 strings. All IDs are UUID v4 strings. All booleans are INTEGER 0/1.

---

## Read This First: Parity Status

This is the **target Android schema**. It follows the Workspace > Area > Project > Task > Subtask hierarchy and the migration decisions made for desktop (Lists become Projects, List Groups become Areas, Inbox is `area_id NULL` and `project_id NULL`).

The desktop SCHEMA.md I was given is **stale against that hierarchy**. It still has `lists` and `list_groups`, `tasks.list_id NOT NULL`, and `tasks`/`projects` reference an `areas` table that the file never defines. So this schema is written from the hierarchy decisions, not copied from that file.

**Before Phase 2 coding starts**, run the Parity Checklist at the bottom of this file against the desktop repo's real post-migration schema. Anything that differs gets fixed here first. Sync correctness depends on table names and column names being identical on both sides.

---

## Sync Scope Legend

Every table and every column has a sync scope. This is what keeps two devices from fighting over things that should be local.

| Scope | Meaning |
|---|---|
| `synced` | Travels in sync payloads. Has a per-field HLC in `sync_field_clock` |
| `local` | Never leaves this device |
| `derived` | Recomputed locally from synced data. Never sent. Never conflicts |
| `meta` | Sync bookkeeping. Written by the sync layer only |

Columns on a synced table that are not marked are `synced`. `created_at` and `updated_at` are `synced` as display metadata. They are **not** used for conflict resolution (HLC is).

Some synced columns merge **together as a group** (for example `bucket` with `due_date`) so that dependent fields never end up from different devices. The group table is in SYNC.md section 5. Adding a synced column means deciding its group or confirming it stands alone.

---

## Schema Version

Current version: `2` (Room database version, tracked by Room).
Schema JSON: `app/schemas/`
Migrations: `app/src/main/java/com/os11/app/data/db/migrations/`

---

## Table Index

| Table | Purpose | Sync |
|---|---|---|
| `local_identity` | This device's identity | local |
| `workspaces` | Tenancy boundary (one invisible default for solo use) | synced |
| `areas` | Ongoing, non-completing contexts (Personal, Work, Health) | synced |
| `projects` | The single container type (replaces Lists and old Projects) | synced |
| `sections` | Named groups inside a project | synced |
| `tasks` | Core task records, subtasks via `parent_task_id` | synced (some columns derived/local) |
| `task_dependencies` | Dependency links | synced |
| `milestones` | Project timeline markers | synced |
| `tags` | Named colored labels (nestable) | synced (natural-key merge) |
| `task_tags` | Task to tag join | synced |
| `reminders` | Time alerts per task | synced (alert definition only) |
| `reminder_device_state` | Per-device fired/snoozed state | local |
| `attachments` | File metadata | synced (no path); blob transfer separate |
| `comments` | Comment threads | synced |
| `comment_reactions` | Emoji reactions | synced |
| `pomodoro_sessions` | Focus sessions | synced |
| `goals` | Objectives | synced (streak derived) |
| `goal_links` | Goal to task/project join | synced |
| `notification_history` | In-app notification log | local |
| `task_history` | Field-change history for a task | local |
| `settings` | Key-value preferences | local, except an allowlist |
| `modules` | Feature toggles | local |
| `users` | Known collaborators (Phase 2 stub) | synced |
| `devices` | Trusted paired peers | local |
| `collaboration_members` | Permission grants (Phase 2 stub) | synced |
| `sync_queue` | Outbox | meta |
| `sync_field_clock` | Per-field HLC | meta |
| `sync_tombstone` | Deletes | meta |
| `sync_local_state` | One row: latest HLC issued by this device | meta |
| `sync_peer_state` | Per-peer cursors | meta |
| `sync_pending` | Remote changes waiting on a missing parent | meta |
| `sync_conflict_log` | Conflict history shown in task detail | meta |
| `tasks_fts` | FTS5 search index | derived |
| `version_history` | System and backup snapshot history (table only, no UI) | local |

Not in v1: `calendar_integration` data, `task_history` UI, Todoist/Notion/MS To Do importers. Tables for them are not created until their phase.

---

## Deterministic IDs

Seeded rows must have the same ID on every device, or the first sync creates duplicates.

```
OS11_NAMESPACE = UUID v5 namespace constant, fixed in :domain/ids and in desktop

default workspace   = uuidv5(OS11_NAMESPACE, "workspace:default")
default area        = uuidv5(OS11_NAMESPACE, "area:default")
recurring occurrence= uuidv5(OS11_NAMESPACE, "occurrence:" + sourceTaskId + ":" + occurrenceDate)
```

All other IDs are random UUID v4. The namespace constant and the three derivations must be byte-identical on desktop. They are part of the parity checklist.

---

## Tables

### `local_identity` (local)

```sql
CREATE TABLE local_identity (
  id           TEXT PRIMARY KEY,   -- UUID v4, generated once on first launch
  display_name TEXT NOT NULL,
  avatar_emoji TEXT,
  created_at   TEXT NOT NULL
);
```

Exactly one row. This `id` is the device ID used in HLCs, `created_by_device`, and pairing.

---

### `workspaces`

```sql
CREATE TABLE workspaces (
  id         TEXT PRIMARY KEY,     -- default row uses the deterministic ID
  name       TEXT NOT NULL,
  created_at TEXT NOT NULL,
  updated_at TEXT NOT NULL
);
```

One default row, never shown to a solo user. Surfaces only for team use (Phase 2).

---

### `areas`

```sql
CREATE TABLE areas (
  id           TEXT PRIMARY KEY,
  workspace_id TEXT NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
  name         TEXT NOT NULL,
  icon         TEXT,
  color        TEXT,
  sort_order   REAL NOT NULL DEFAULT 0,
  is_default   INTEGER NOT NULL DEFAULT 0,
  created_at   TEXT NOT NULL,
  updated_at   TEXT NOT NULL
);

CREATE INDEX idx_areas_workspace ON areas(workspace_id, sort_order);
```

Rules (enforced in services, not by SQL):
- The default Area (deterministic ID) can be renamed, never deleted.
- Deleting an Area that still has projects or loose tasks is blocked. The user moves or deletes the contents first.

---

### `projects`

```sql
CREATE TABLE projects (
  id           TEXT PRIMARY KEY,
  workspace_id TEXT NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
  area_id      TEXT REFERENCES areas(id) ON DELETE SET NULL,
  name         TEXT NOT NULL,
  description  TEXT,
  color        TEXT,
  icon         TEXT,
  status       TEXT NOT NULL DEFAULT 'active',   -- 'active' | 'archived' | 'completed'
  due_date     TEXT,
  default_view TEXT NOT NULL DEFAULT 'list',     -- 'list' | 'board' | 'timeline' | 'calendar' | 'table'
  is_someday   INTEGER NOT NULL DEFAULT 0 CHECK (is_someday IN (0, 1)),
  reviewed_at  TEXT,
  sort_order   REAL NOT NULL DEFAULT 0,
  created_at   TEXT NOT NULL,
  updated_at   TEXT NOT NULL
);

CREATE INDEX idx_projects_area    ON projects(area_id, sort_order);
CREATE INDEX idx_projects_status  ON projects(status);
CREATE INDEX idx_projects_someday ON projects(is_someday) WHERE is_someday = 1;
```

`default_view` may hold `calendar` or `table` because desktop writes them. Android renders unsupported values as `list` and **preserves the stored value** (never writes `list` over `table` just because it cannot show a table).

---

### `sections`

```sql
CREATE TABLE sections (
  id           TEXT PRIMARY KEY,
  project_id   TEXT NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
  name         TEXT NOT NULL,
  sort_order   REAL NOT NULL DEFAULT 0,
  is_collapsed INTEGER NOT NULL DEFAULT 0,   -- device-local in practice, see note
  created_at   TEXT NOT NULL,
  updated_at   TEXT NOT NULL
);

CREATE INDEX idx_sections_project_id ON sections(project_id);
```

`is_collapsed` is **local**: a section collapsed on the phone should not collapse on the desktop. It stays in this table (desktop parity) but is excluded from sync field lists.

---

### `tasks`

```sql
CREATE TABLE tasks (
  id                     TEXT PRIMARY KEY,
  title                  TEXT NOT NULL,
  notes                  TEXT,                       -- sanitized HTML (same as desktop)
  workspace_id           TEXT NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
  area_id                TEXT REFERENCES areas(id) ON DELETE SET NULL,
  project_id             TEXT REFERENCES projects(id) ON DELETE SET NULL,
  section_id             TEXT REFERENCES sections(id) ON DELETE SET NULL,
  parent_task_id         TEXT REFERENCES tasks(id) ON DELETE CASCADE,
  due_date               TEXT,                       -- YYYY-MM-DD
  due_time               TEXT,                       -- HH:mm:ss, null = all-day
  all_day                INTEGER NOT NULL DEFAULT 1,
  recurrence_rule        TEXT,                       -- RFC 5545 RRULE
  recurrence_basis       TEXT,                       -- 'fixed' | 'after_completion'
  priority               INTEGER NOT NULL DEFAULT 0, -- 0 None 1 Low 2 Medium 3 High 4 Critical
  is_starred             INTEGER NOT NULL DEFAULT 0,
  is_completed           INTEGER NOT NULL DEFAULT 0,
  completed_at           TEXT,
  estimated_minutes      INTEGER,
  assignee_device_id     TEXT,                       -- Phase 2
  created_by_device      TEXT NOT NULL DEFAULT 'local',
  sort_order             REAL NOT NULL DEFAULT 0,
  my_day_date            TEXT,                       -- YYYY-MM-DD
  pomodoro_count         INTEGER NOT NULL DEFAULT 0, -- DERIVED, not synced
  scheduled_start_min    INTEGER CHECK (scheduled_start_min IS NULL OR (scheduled_start_min >= 0 AND scheduled_start_min < 1440)),
  scheduled_duration_min INTEGER CHECK (scheduled_duration_min IS NULL OR (scheduled_duration_min >= 15 AND scheduled_duration_min <= 480)),
  bucket                 TEXT CHECK (bucket IN ('anytime', 'someday')),
  waiting_on             TEXT,                       -- max 120 chars
  waiting_since          TEXT,
  follow_up_date         TEXT,
  follow_up_notified_on  TEXT,                       -- LOCAL, not synced
  reviewed_at            TEXT,
  is_trashed             INTEGER NOT NULL DEFAULT 0,
  trashed_at             TEXT,
  created_at             TEXT NOT NULL,
  updated_at             TEXT NOT NULL
);

CREATE INDEX idx_tasks_area        ON tasks(area_id);
CREATE INDEX idx_tasks_project     ON tasks(project_id, sort_order);
CREATE INDEX idx_tasks_parent      ON tasks(parent_task_id);
CREATE INDEX idx_tasks_due_date    ON tasks(due_date);
CREATE INDEX idx_tasks_starred     ON tasks(is_starred);
CREATE INDEX idx_tasks_completed   ON tasks(is_completed);
CREATE INDEX idx_tasks_my_day      ON tasks(my_day_date);
CREATE INDEX idx_tasks_trashed     ON tasks(is_trashed);
CREATE INDEX idx_tasks_bucket      ON tasks(bucket, sort_order)
  WHERE bucket IS NOT NULL AND is_trashed = 0 AND is_completed = 0;
CREATE INDEX idx_tasks_waiting     ON tasks(follow_up_date, waiting_since)
  WHERE waiting_since IS NOT NULL AND is_trashed = 0 AND is_completed = 0;
CREATE INDEX idx_tasks_inbox       ON tasks(sort_order)
  WHERE area_id IS NULL AND project_id IS NULL AND parent_task_id IS NULL
    AND due_date IS NULL AND is_trashed = 0 AND is_completed = 0;
```

**Sync scope exceptions on `tasks`:**
- `pomodoro_count`: `derived`. Recomputed from `pomodoro_sessions`. A counter that two devices both increment cannot merge, so it is never sent.
- `follow_up_notified_on`: `local`. Each device notifies for itself.
- Everything else: `synced`.

**Rules (enforced in services and `:domain`):**
- Loose task in an Area: `area_id` set, `project_id` null. Task inside a Project: both set (`area_id` is the project's area, kept consistent by the service).
- Inbox task: `area_id` null, `project_id` null. Inbox index above also requires no parent and no due date.
- `bucket` and `due_date` are mutually exclusive (ADR-0010 on desktop, rules R1 to R3). The transition table is ported to `:domain/logic/scheduling` and is the only thing allowed to decide it.
- `scheduled_start_min` and `scheduled_duration_min` are both set or both null. Cleared when removed from My Day or on day rollover.
- `parent_task_id` supports unlimited nesting; depth is enforced in domain logic.
- Tie-break for ordering is `(sort_order, id)`.

---

### `task_dependencies`

```sql
CREATE TABLE task_dependencies (
  task_id       TEXT NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
  depends_on_id TEXT NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
  PRIMARY KEY (task_id, depends_on_id)
);
```

Sync entity key: `task_id|depends_on_id`. Cycle check in `:domain` before any insert. Two devices can each add a link that together form a cycle; the apply step re-runs the check and drops the later link into `sync_conflict_log`.

---

### `milestones`

```sql
CREATE TABLE milestones (
  id           TEXT PRIMARY KEY,
  project_id   TEXT NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
  title        TEXT NOT NULL,
  due_date     TEXT NOT NULL,
  is_completed INTEGER NOT NULL DEFAULT 0,
  sort_order   REAL NOT NULL DEFAULT 0,
  created_at   TEXT NOT NULL
);

CREATE INDEX idx_milestones_project ON milestones(project_id);
```

---

### `tags`

```sql
CREATE TABLE tags (
  id            TEXT PRIMARY KEY,
  name          TEXT NOT NULL UNIQUE,     -- full path for nested: 'work/client/Acme'
  color         TEXT,
  parent_tag_id TEXT REFERENCES tags(id) ON DELETE SET NULL,
  sort_order    REAL NOT NULL DEFAULT 0,
  created_at    TEXT NOT NULL
);
```

`name` is a natural key. Two devices creating "work" offline produce two rows with different IDs. The apply step merges them (SYNC.md, Natural-Key Collisions): lower ID wins, `task_tags` are repointed, the loser is tombstoned.

---

### `task_tags`

```sql
CREATE TABLE task_tags (
  task_id TEXT NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
  tag_id  TEXT NOT NULL REFERENCES tags(id) ON DELETE CASCADE,
  PRIMARY KEY (task_id, tag_id)
);

CREATE INDEX idx_task_tags_tag ON task_tags(tag_id);
```

Sync entity key: `task_id|tag_id`. Add is an insert, remove is a tombstone. Re-adding after a remove is a newer insert that beats the tombstone.

---

### `reminders`

```sql
CREATE TABLE reminders (
  id         TEXT PRIMARY KEY,
  task_id    TEXT NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
  remind_at  TEXT NOT NULL,       -- ISO 8601 datetime
  created_at TEXT NOT NULL
);

CREATE INDEX idx_reminders_task ON reminders(task_id);
CREATE INDEX idx_reminders_at   ON reminders(remind_at);
```

**Deviation from desktop:** desktop's `reminders` row also carries `is_triggered` and `snoozed_until`. Those are per-device facts (the desktop firing a reminder says nothing about whether the phone buzzed). On Android they live in `reminder_device_state` and are **never synced**. Desktop must stop syncing those two columns. See SYNC.md desktop-side changes.

---

### `reminder_device_state` (local)

```sql
CREATE TABLE reminder_device_state (
  reminder_id   TEXT PRIMARY KEY REFERENCES reminders(id) ON DELETE CASCADE,
  is_triggered  INTEGER NOT NULL DEFAULT 0,
  snoozed_until TEXT
);

CREATE INDEX idx_rds_pending ON reminder_device_state(is_triggered) WHERE is_triggered = 0;
```

A reminder with no state row is pending. Alarm reconciliation = pending reminders joined to this table.

---

### `attachments`

```sql
CREATE TABLE attachments (
  id            TEXT PRIMARY KEY,
  task_id       TEXT NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
  filename      TEXT NOT NULL,        -- sanitized name on disk
  original_name TEXT NOT NULL,
  mime_type     TEXT NOT NULL,
  size_bytes    INTEGER NOT NULL,
  is_link       INTEGER NOT NULL DEFAULT 0,
  link_url      TEXT,                 -- when is_link = 1
  blob_state    TEXT NOT NULL DEFAULT 'present',  -- LOCAL: 'present' | 'missing' | 'pending'
  created_at    TEXT NOT NULL
);

CREATE INDEX idx_attachments_task ON attachments(task_id);
```

**Deviation from desktop:** desktop stores `local_path` as an absolute path. That cannot sync. Android has no stored path. The file lives at `filesDir/attachments/<task_id>/<id>.<ext>`, derived from the row. `blob_state` is `local`.

Metadata syncs first. The file bytes transfer separately (SYNC.md, Attachment Blobs). A row whose blob has not arrived shows as "not downloaded" and never as an error.

---

### `comments` and `comment_reactions`

```sql
CREATE TABLE comments (
  id                TEXT PRIMARY KEY,
  task_id           TEXT NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
  author_device_id  TEXT NOT NULL DEFAULT 'local',
  author_name       TEXT NOT NULL,
  body              TEXT NOT NULL,
  parent_comment_id TEXT REFERENCES comments(id) ON DELETE CASCADE,
  is_resolved       INTEGER NOT NULL DEFAULT 0,
  created_at        TEXT NOT NULL,
  updated_at        TEXT NOT NULL
);
CREATE INDEX idx_comments_task ON comments(task_id);

CREATE TABLE comment_reactions (
  comment_id        TEXT NOT NULL REFERENCES comments(id) ON DELETE CASCADE,
  reactor_device_id TEXT NOT NULL DEFAULT 'local',
  emoji             TEXT NOT NULL,
  PRIMARY KEY (comment_id, reactor_device_id, emoji)
);
```

Phase 2 UI. Tables exist from day one for parity.

`author_device_id = 'local'` is a problem for sync: `'local'` means a different device on each side. At first sync, rows written with `'local'` are rewritten to the writing device's `local_identity.id` before being sent. Desktop does the same. (Same applies to `tasks.created_by_device` and `reactor_device_id`.)

---

### `pomodoro_sessions`

```sql
CREATE TABLE pomodoro_sessions (
  id               TEXT PRIMARY KEY,
  task_id          TEXT REFERENCES tasks(id) ON DELETE SET NULL,
  type             TEXT NOT NULL,      -- 'work' | 'short_break' | 'long_break'
  duration_seconds INTEGER NOT NULL,
  started_at       TEXT NOT NULL,
  ended_at         TEXT,
  was_completed    INTEGER NOT NULL DEFAULT 0,
  created_at       TEXT NOT NULL
);
CREATE INDEX idx_pomodoro_task    ON pomodoro_sessions(task_id);
CREATE INDEX idx_pomodoro_started ON pomodoro_sessions(started_at);
```

An in-progress session is **not** live-synced as a ticking row. The live timer state (current phase, start timestamp) is device-local. Only the completed or ended session row is written, and that row syncs.

---

### `goals` and `goal_links`

```sql
CREATE TABLE goals (
  id               TEXT PRIMARY KEY,
  title            TEXT NOT NULL,
  description      TEXT,
  goal_type        TEXT NOT NULL,       -- 'habit' | 'milestone' | 'outcome'
  target_date      TEXT,
  target_value     REAL DEFAULT 100,
  current_value    REAL NOT NULL DEFAULT 0,
  streak_count     INTEGER NOT NULL DEFAULT 0,   -- DERIVED, not synced
  last_progress_at TEXT,
  created_at       TEXT NOT NULL,
  updated_at       TEXT NOT NULL
);

CREATE TABLE goal_links (
  goal_id       TEXT NOT NULL REFERENCES goals(id) ON DELETE CASCADE,
  resource_type TEXT NOT NULL,          -- 'task' | 'project'
  resource_id   TEXT NOT NULL,
  PRIMARY KEY (goal_id, resource_type, resource_id)
);
```

`streak_count` is recomputed from completions. Sync entity key for `goal_links`: `goal_id|resource_type|resource_id`.

---

### `notification_history` and `task_history` (local)

```sql
CREATE TABLE notification_history (
  id         TEXT PRIMARY KEY,
  type       TEXT NOT NULL,   -- 'due' | 'reminder' | 'pomodoro' | 'collaboration' | 'agenda' | 'goal' | 'streak'
  task_id    TEXT REFERENCES tasks(id) ON DELETE CASCADE,
  title      TEXT NOT NULL,
  body       TEXT NOT NULL,
  created_at TEXT NOT NULL,
  read_at    TEXT
);
CREATE INDEX idx_notif_created ON notification_history(created_at);

CREATE TABLE task_history (
  id             TEXT PRIMARY KEY,
  task_id        TEXT NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
  changed_fields TEXT NOT NULL,     -- JSON { field: { from, to } }
  changed_at     TEXT NOT NULL
);
CREATE INDEX idx_task_history_task ON task_history(task_id);
```

Both device-local. `task_history` has no UI in v1 and is purged after 30 days. It is created now so the table exists when the UI lands. `sync_conflict_log` is the synced-world equivalent that users actually see.

---

### `version_history` (local, table only, no UI)

```sql
CREATE TABLE version_history (
  id             TEXT PRIMARY KEY,
  entity_type    TEXT NOT NULL,
  entity_id      TEXT NOT NULL,
  version_number INTEGER NOT NULL,
  snapshot_data  TEXT NOT NULL,
  created_at     TEXT NOT NULL
);
CREATE INDEX idx_version_history_entity ON version_history(entity_type, entity_id);
CREATE INDEX idx_version_history_created ON version_history(created_at);
```

Stores snapshot history for system and backup operations (JSON export, JSON import, CSV export, automated daily backups). Table only, no UI.

---

### `settings` (local, with an allowlist)

```sql
CREATE TABLE settings (
  key   TEXT PRIMARY KEY,
  value TEXT NOT NULL      -- JSON-encoded
);
```

**Synced allowlist** (the only keys that travel):

`day_starts_at`, `pomodoro_work_minutes`, `pomodoro_break_minutes`, `pomodoro_long_break_minutes`, `pomodoro_sessions_before_long_break`, `waiting_stale_days`, `follow_up_notify_time`

Everything else is device-local. Settings sync as `settings` entities keyed by `key`, one field (`value`).

**Seed values that apply on Android:**

| Key | Default | Notes |
|---|---|---|
| `accent_color` | `"#4da3ff"` | Dark accent from desktop tokens. Desktop's seed `#0078d4` is the old light accent |
| `font_size` | `"md"` | `sm` `md` `lg` `xl` multiplier on top of system font scale |
| `density` | `"comfortable"` | `compact` `comfortable` `cozy` |
| `day_starts_at` | `"00:00"` | synced |
| `pomodoro_work_minutes` | `25` | synced |
| `pomodoro_break_minutes` | `5` | synced |
| `pomodoro_long_break_minutes` | `15` | synced |
| `pomodoro_sessions_before_long_break` | `4` | synced |
| `active_profile_preset` | `"custom"` | |
| `app_lock_enabled` | `false` | |
| `reduce_motion` | `false` | |
| `task_card_style` | `"default"` | |
| `waiting_stale_days` | `7` | synced |
| `follow_up_notify_time` | `"09:00"` | synced |
| `show_someday_projects_in_browse` | `false` | Android name for `show_someday_projects_in_sidebar` |
| `show_stalled_marker` | `true` | |
| `follow_ups` | `true` | |
| `haptics_enabled` | `true` | Android only |
| `secure_screen` | `false` | Android only (FLAG_SECURE) |
| `sync_attachment_max_mb` | `25` | Android only |

**Desktop settings Android does not have:** `theme` (dark only), `font_family` (Inter only), `sidebar_position`, `launch_at_login`, `vim_keybindings`, `triage_keys_enabled` (keyboard triage). They are never created on Android and never sent.

---

### `modules` (local)

```sql
CREATE TABLE modules (
  module_name TEXT PRIMARY KEY,
  is_enabled  INTEGER NOT NULL DEFAULT 1
);
```

Modules gate UI, not data, so they are device-local. Disabling Pomodoro on the phone does not disable it on the desktop.

| module_name | Default |
|---|---|
| `my_day` | 1 |
| `project_management` | 1 |
| `pomodoro` | 1 |
| `agenda` | 1 |
| `goals_habits` | 1 |
| `dashboard` | 1 |
| `file_attachments` | 1 |
| `nlp_parsing` | 1 |
| `anytime` | 0 |
| `someday` | 0 |
| `waiting_for` | 0 |
| `habit_tracker` | 0 |
| `collaboration` | 0 (Phase 2) |
| `companion_sync` | 0 until the sync phase ships, then 1 |

Not present on Android: `vim_keybindings`, `animated_backgrounds`.

---

### Phase 2 stubs

```sql
CREATE TABLE users (
  id           TEXT PRIMARY KEY,
  display_name TEXT NOT NULL,
  avatar_emoji TEXT,
  last_seen_at TEXT,
  created_at   TEXT NOT NULL
);

CREATE TABLE devices (
  id             TEXT PRIMARY KEY,   -- remote device's local_identity.id
  device_name    TEXT NOT NULL,
  device_type    TEXT NOT NULL,      -- 'android' | 'desktop'
  last_connected TEXT,
  is_trusted     INTEGER NOT NULL DEFAULT 1,
  created_at     TEXT NOT NULL
  -- pairing token and pinned certificate fingerprint live in SecretStore, keyed by this id
);

CREATE TABLE collaboration_members (
  id            TEXT PRIMARY KEY,
  resource_type TEXT NOT NULL,       -- 'area' | 'project'
  resource_id   TEXT NOT NULL,
  device_id     TEXT NOT NULL REFERENCES devices(id) ON DELETE CASCADE,
  permission    TEXT NOT NULL DEFAULT 'editor',
  invited_at    TEXT NOT NULL,
  accepted_at   TEXT
);
CREATE UNIQUE INDEX idx_collab_device_resource ON collaboration_members(resource_id, device_id);
```

`devices` is local: each device keeps its own list of who it trusts. Note: desktop's `collaboration_members.resource_type` says `'list' | 'project'`. With the new hierarchy there are no lists, so desktop's value set needs to change. Listed in the parity checklist.

Desktop's `devices.pairing_token` column is not created on Android. The token is a secret and lives only in `SecretStore`.

---

## Sync Tables (meta)

Full semantics in SYNC.md. Shapes here.

```sql
-- Outbox. Appended in the same transaction as the data write.
CREATE TABLE sync_queue (
  seq              INTEGER PRIMARY KEY AUTOINCREMENT,   -- local drain order
  id               TEXT NOT NULL UNIQUE,                -- UUID v4, idempotency key on the wire
  operation        TEXT NOT NULL,                       -- 'upsert' | 'delete'
  entity           TEXT NOT NULL,                       -- table name
  entity_id        TEXT NOT NULL,                       -- row id or composite key 'a|b'
  payload          TEXT NOT NULL,                       -- JSON { field: { value, hlc } }
  target_device_id TEXT,                                -- null = all trusted peers
  created_at       TEXT NOT NULL,
  retry_count      INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX idx_sync_queue_created ON sync_queue(created_at);

-- One row per synced field that has ever been written.
CREATE TABLE sync_field_clock (
  entity    TEXT NOT NULL,
  entity_id TEXT NOT NULL,
  field     TEXT NOT NULL,
  hlc       TEXT NOT NULL,
  PRIMARY KEY (entity, entity_id, field)
);

-- Deletes.
CREATE TABLE sync_tombstone (
  entity     TEXT NOT NULL,
  entity_id  TEXT NOT NULL,
  hlc        TEXT NOT NULL,
  deleted_at TEXT NOT NULL,
  PRIMARY KEY (entity, entity_id)
);

-- One row. Updated in the same transaction as every write that ticks the clock.
CREATE TABLE sync_local_state (
  id       INTEGER PRIMARY KEY CHECK (id = 1),
  last_hlc TEXT NOT NULL
);

-- Per-peer cursors.
CREATE TABLE sync_peer_state (
  peer_device_id   TEXT PRIMARY KEY REFERENCES devices(id) ON DELETE CASCADE,
  last_sent_seq    INTEGER NOT NULL DEFAULT 0,
  last_received_hlc TEXT,
  last_synced_at   TEXT
);

-- Remote changes whose parent row does not exist yet.
CREATE TABLE sync_pending (
  id         TEXT PRIMARY KEY,
  entity     TEXT NOT NULL,
  entity_id  TEXT NOT NULL,
  payload    TEXT NOT NULL,
  waiting_for_entity    TEXT NOT NULL,
  waiting_for_entity_id TEXT NOT NULL,
  received_at TEXT NOT NULL
);

-- What the user sees under "conflict history" in the task detail sheet.
CREATE TABLE sync_conflict_log (
  id            TEXT PRIMARY KEY,
  entity        TEXT NOT NULL,
  entity_id     TEXT NOT NULL,
  field         TEXT NOT NULL,
  losing_value  TEXT,
  winning_value TEXT,
  losing_device_id TEXT,
  occurred_at   TEXT NOT NULL
);
CREATE INDEX idx_conflict_entity ON sync_conflict_log(entity, entity_id);
```

**Deviation from desktop:** desktop's `sync_queue` stores a `JSON snapshot of changed fields` with no clock, and uses `'insert' | 'update' | 'delete'`. Android needs a per-field HLC in the payload and a `seq` for drain order, and merges insert/update into `upsert` because under per-field merge they are the same operation. Desktop's table needs the same shape. Listed in SYNC.md.

---

## Full-Text Search

FTS5 virtual table, created in a migration and **not** declared as a Room entity. Kept in sync by triggers on `tasks` INSERT, UPDATE, DELETE. Searched through `FtsQuery.escape`. Startup self-test asserts FTS5 exists (ADR-A02).

```sql
CREATE VIRTUAL TABLE tasks_fts USING fts5(
  id UNINDEXED,
  title,
  notes,
  content=tasks,
  content_rowid=rowid
);
```

Trashed tasks are excluded at query time (join on `tasks.is_trashed = 0`), not by index removal, so Restore does not need a reindex.

---

## Relationships at a Glance

```
local_identity (1 row - this device)

workspaces (1 default, invisible for solo)
  +-- areas
  |     +-- projects
  |     |     +-- sections
  |     |     +-- milestones
  |     |     +-- tasks (via project_id)
  |     +-- tasks (loose, via area_id with project_id NULL)
  +-- tasks (Inbox: area_id NULL and project_id NULL)

tasks
  +-- tasks (subtasks via parent_task_id)
  +-- reminders  -> reminder_device_state (local)
  +-- attachments
  +-- comments -> comment_reactions
  +-- pomodoro_sessions
  +-- tags (via task_tags)
  +-- depends on tasks (via task_dependencies)

goals -> tasks or projects (via goal_links)

Smart lists (My Day, Important, Planned, Anytime, Someday, Waiting For,
All Tasks, Completed) are computed queries. They have no rows.

sync: sync_queue, sync_field_clock, sync_tombstone, sync_peer_state,
      sync_pending, sync_conflict_log
```

---

## Parity Checklist (run before Phase 2 coding)

Run against the desktop repo's actual post-migration schema. Every line is yes/no. Any "no" is fixed here or on desktop before code.

- [ ] Table names and column names identical for every `synced` table
- [ ] `lists` and `list_groups` are gone on desktop; `tasks.list_id` is gone
- [ ] `workspace_id` exists on `areas`, `projects`, `tasks` on desktop (or this file is changed to match where desktop put it)
- [ ] `areas` table definition matches (columns, `is_default`)
- [ ] `OS11_NAMESPACE` constant and the three deterministic ID derivations match byte for byte
- [ ] `reminders.is_triggered` and `snoozed_until` are excluded from desktop sync
- [ ] `attachments.local_path` is excluded from desktop sync; desktop derives the path from id
- [ ] `tasks.pomodoro_count`, `goals.streak_count`, `tasks.follow_up_notified_on`, `sections.is_collapsed` excluded from desktop sync
- [ ] `'local'` device ID placeholder is rewritten to the real device ID before first send on desktop
- [ ] `collaboration_members.resource_type` value set updated for the new hierarchy
- [ ] Desktop `sync_queue` reshaped to match (seq, id, upsert/delete, per-field HLC payload)
- [ ] Desktop adds `sync_field_clock`, `sync_tombstone`, `sync_peer_state`, `sync_pending`, `sync_conflict_log`
- [ ] Desktop tag-name collision merge implemented the same way
- [ ] Recurring completion on desktop uses the deterministic occurrence ID
- [ ] Settings sync allowlist matches
- [ ] `projects.default_view` value set matches; unknown values are preserved, not overwritten
- [ ] Desktop `task_history` and `notification_history` confirmed device-local
