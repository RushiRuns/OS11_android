# OS11 Android - Project Breakdown into Phases and Tasks

> **Governance first.** Same rule as desktop: every governing document is written and validated before production code.
> **Local first.** v1.0.0 is standalone and offline. No cloud, no account, no AI/LLM, nothing that costs money to run.
> **Sync-ready from day one.** Sync metadata (field clocks, outbox, tombstones, stable IDs) is built into the write path in Phase 3. The sync engine itself ships after v1.0.0 (Phases S1 to S3).

**Numbering.** These phases are Android's own. "Desktop Phase N" always means the desktop phases doc. This replaces desktop Phase 20's "Android Companion App (React Native)" section. Reasons are in ARCHITECTURE.md (A01) and SYNC.md section 12.

**The five North Stars carry over:** Insane Speed, Extreme Convenience, Modular, FEEL UI, and Keyboard Native, which becomes **Thumb Native** on a phone: every frequent action reachable one-handed, no action reachable only by a hidden gesture.

## Estimates: read this first

- Every week range below is **my estimate with no Android velocity data behind it**. Desktop took 24 weeks for a comparable scope. I assumed Android takes longer per feature because of the platform learning curve, Compose performance tuning, and device testing.
- Total to v1.0.0: **about 41 weeks**. Sync after that: about **9 weeks**.
- After Phases 3 and 5 are done, replace these estimates with measured pace. Update this file then.
- Any phase can take 1.5x. Phase 1 (spikes) exists to find out early which parts will.
- Assumes one person, working with an AI code editor, part of the week. If you are full-time on it, ranges shrink; if you are not, they grow.

---

## PHASE 0: Governance and Foundation (Weeks 1-2)

### Governance documents (written; validate before the first code session)

- [x] Read and validate `ARCHITECTURE.md` (stack, layers, 24 rejection rules)
- [x] Read and validate `SYSTEM.md` (conventions, NEVER DO list)
- [x] Read and validate `SCHEMA.md` (Android schema) and **run its Parity Checklist against the desktop repo's real post-migration schema**
- [x] Read and validate `SYNC.md` and decide the 15 desktop-side changes (section 12); open the desktop ADR for them
- [x] Read and validate `PERFORMANCE.md`, `DEPENDENCIES.md`, `ANDROID_VS_DESKTOP.md`
- [x] Review `Tokens.kt` against the real desktop `tokens.css` (the PARITY placeholders: shell glow, font-step values)
- [x] `DONE.md`, `UTILITIES.md`, `CHANGELOG_INTERNAL.md` exist (empty registries)

### ADRs (`docs/decisions/`)

- [x] A01 native Kotlin/Compose (and why not React Native)
- [x] A02 Room with bundled SQLite and FTS5
- [x] A03 two Gradle modules
- [x] A04 Material3 as a primitive layer only
- [x] A05 database as source of truth, no optimistic layer
- [x] A06 sync metadata from day one
- [x] A07 glass surfaces without backdrop blur
- [x] A08 no Google Play Services, no cloud backup
- [x] A09 timers derived from timestamps
- [x] A10 own NLP parser with shared test vectors

### Project setup

- [x] Create the Android Studio project: `:app` and `:domain` modules, version catalog, KSP, Hilt
- [x] Package structure per ARCHITECTURE.md
- [x] Spotless + detekt + Konsist architecture test skeleton (fails on forbidden imports from the first commit)
- [x] `allowBackup=false`, manifest permissions exactly as listed in ARCHITECTURE.md, no others
- [x] CI-equivalent local script: `./gradlew check` runs lint, detekt, unit tests, Konsist
- [x] Git init, first commit: governance docs + skeleton
- [x] Antigravity project setup: confirm how it loads project rules. **I could not verify this.** If it does not read rule files automatically, every session starts with the template in SYSTEM.md section 9

### Reference device

- [ ] Record your phone model, Android version, RAM, refresh rate in PERFORMANCE.md. All targets are judged on this device

**Done when:** a clean build runs an empty app, `./gradlew check` is green, and every doc has been read by you at least once.

---

## PHASE 1: Spikes and Risk Burn-Down (Week 3)

Short, throwaway-or-keep experiments. Each ends with a written result in DEPENDENCIES.md or an ADR. This phase exists because three of the unknowns can reshape later phases.

- [x] **S-1 Bundled SQLite + FTS5 + Room:** create the tasks table, the FTS5 virtual table and its triggers via a Room migration, run a prefix search over 5,000 seeded rows. Confirm Room validates the schema without knowing about FTS. Confirm migration tests run
- [x] **S-2 Rich text note editor (highest risk):** evaluate `compose-rich-editor`. Load HTML the desktop TipTap editor produces (headings, bold/italic, lists, checklists, links, code, blockquote). Edit, save, reload. Pass only if nothing is silently dropped. If it fails, fallback options: (a) restricted Markdown-ish editing with HTML conversion, (b) a custom minimal editor over `AnnotatedString`, (c) WebView with a bundled editor (breaks several rules, needs an ADR). Decision before Phase 5
- [x] **S-3 Reorder and nesting:** evaluate `Reorderable`. Can it reorder in a LazyColumn with 1,000 rows without dropped frames, and can nesting by horizontal offset be built on it? If not, nesting is Indent/Outdent only in v1 (already the primary path)
- [x] **S-4 Charts:** Vico vs drawing with Canvas. Measure size added to the release build. Pick one
- [x] **S-5 Glass:** Haze vs translucent surface plus border. Measure frame time while a sheet is over a scrolling list on the reference device. Pick one
- [x] **S-6 Konsist:** prove a test fails on a UI file importing a DAO, a raw `Color(0x...)`, or `MaterialTheme.` in a feature package
- [x] **S-7 Baseline Profile:** generate one for a blank list screen. Prove the toolchain works before there is anything to protect
- [x] **S-8 Exact alarm + boot:** schedule an exact alarm for 1 minute out, reboot the phone, confirm it re-arms. Learn your device vendor's battery-killer behavior now, not in Phase 11

**Done when:** each spike has a result paragraph and a keep/drop decision. Update ADRs and the phases below if a spike changes scope.

---

## PHASE 2: Design System and Shell (Weeks 4-5)

- [x] Bundle Inter as font resources; wire `os11Type`
- [x] Finalize `Tokens.kt`; compile it; resolve the PARITY placeholders against desktop `tokens.css`
- [x] `OS11Theme { }` wired at the root; verify no `MaterialTheme.` reads in feature code
- [x] Base components (each in `ui/components/`, each registered in UTILITIES.md):
  - [x] `OSButton`, `OSIconButton` (48dp hit area)
  - [x] `OSCheckbox` (20dp visual, 48dp target, spring + strikethrough + light haptic, reduced-motion aware)
  - [x] `OSTextField`, `OSSearchField`
  - [x] `OSSheet` (modal bottom sheet, restyled; the sheet used by Quick Add, detail, pickers)
  - [x] `OSGlassMenu` (profile menu surface)
  - [x] `OSChip`, `OSTagDot`, `OSPriorityBar`
  - [x] `OSSnackbar` host with Undo action
  - [x] `OSEmptyState`, `OSProgressRing`, `OSProgressBar`
  - [x] `OSDatePickerSheet`, `OSTimeList`
  - [x] `OSSwitch`, `OSSegmented`
- [x] App shell: edge-to-edge, insets handled once, bottom bar (My Day, Inbox, Planned, Browse), top bar, profile avatar menu
- [x] Navigation graph with type-safe routes and empty placeholder screens
- [x] Splash via `core-splashscreen`
- [x] Debug-only component gallery screen (every component, every state) used for visual review
- [x] Reduced-motion helper; the 4 permitted animation sites implemented as shared modifiers

**Done when:** the shell runs with placeholder screens, the gallery shows every component, the architecture test confirms zero hardcoded values.

---

## PHASE 3: Database, TrackedWriter, Repositories, Domain (Weeks 6-8)

The largest foundation phase. Sync readiness is built here or never cheaply.

### Database

- [x] Room entities and DAOs for every table in SCHEMA.md, **including** sync tables, local-identity, Phase 2 stubs
- [x] Bundled SQLite driver configured: WAL, `synchronous = NORMAL`, foreign keys on
- [x] Migration 1 as hand-written SQL where Room cannot express it: partial indexes, FTS5 table, FTS triggers, constraints
- [x] Exported Room schema committed; migration test harness
- [x] Seeds: default workspace and default area with deterministic UUIDv5 IDs; settings defaults per the Android seed table; modules defaults

### Domain (`:domain`, pure Kotlin)

- [x] `Hlc` (parse, format, compare, tick, receive) with property tests
- [x] `Clock`, `IdGenerator` (random UUID, UUIDv5) interfaces
- [x] Task validation, fractional indexing (`between`, `atStart`, `atEnd`, extremes)
- [x] Dependency cycle check, parent-cycle check
- [x] Merge groups table and `FieldWrite` / `Change` types and serialization
- [x] Merge function `mergeField(local, incoming)` with convergence property tests (commutative, associative, idempotent)

### Write path

- [x] `TrackedWriter`: one Room transaction = data row + field clocks + outbox row + history row; post-commit side-effect hook
- [x] Every repository write goes through it. Konsist test: no DAO write is callable outside `TrackedWriter`
- [x] Outbox is written even though nothing drains it yet; pruning rule for an unpaired device (outbox must not grow forever; define the cap and compaction in SYNC.md if not already clear)
- [x] Tombstone writes, cascade tombstones, resurrection check

### Repositories

- [x] One repository per aggregate: workspace, area, project, section, task, dependency, milestone, tag, reminder, attachment, comment, pomodoro, goal, notification, history, settings, module
- [x] Slim list projections (no `notes` column in list queries)
- [x] Batched tag loading for visible task windows
- [x] Search repository (FTS5), counts repository (single query for all Browse badges)

### Tests

- [x] Repository tests on in-memory Room
- [x] Migration test from schema version 1 (and the framework for later versions)
- [x] Seed generator (debug only): 1,000 and 5,000 tasks across areas, projects, tags, subtasks, reminders
- [x] Timing test: tracked write P95 against the target

**Done when:** a task can be created, edited, trashed and restored through `TrackedWriter` in tests, with the right clocks and outbox rows, and the 5,000-task seed loads.

---

## PHASE 4: Application Services and Startup (Week 9)

- [x] Hilt modules; `Application.onCreate` initializes Hilt only
- [x] Service layer: `TaskService`, `ProjectService`, `AreaService`, `TagService`, `ReminderService`, `PomodoroService`, `GoalService`, `SettingsService`, `SearchService`, `ExportService` shells
- [x] `AppResult` / `AppError` used for every fallible service call
- [x] Undo stack (service-level, 100 entries) and Snackbar bridge
- [x] Post-first-frame initializer: schedules the deferred work (alarm reconciliation, trash purge check, widget refresh) after first frame
- [x] `SecretStore` over Android Keystore (unused until sync pairing, but written now so the interface is stable)
- [x] StrictMode enabled in debug, LeakCanary in debug
- [x] Macrobenchmark module; baseline of cold start on the empty shell recorded

**Done when:** startup path is audited against rule 16, and a cold-start baseline is in PERFORMANCE.md.

---

## PHASE 5: Core Task Management (Weeks 10-12)

- [x] Task row (44/52/60dp by density): checkbox, title, second line only when there is something to show, priority bar, tag dots
- [x] Task list with `LazyColumn`: keys, content types, immutable UI models, subtask tree flattened on `Default`
- [x] Task detail bottom sheet: title, When, tags, notes, subtasks, reminders; sections collapsed by default; empty sections render nothing
- [x] Notes editor per the S-2 decision; HTML sanitized with Jsoup on save
- [x] Task CRUD through services: create, edit, complete, uncomplete, star, duplicate, trash, restore, permanent delete
- [x] Subtasks: add, complete, count "2/5", promote, Indent / Outdent
- [x] Trash screen with 30-day purge (WorkManager)
- [x] Undo via Snackbar for complete, trash, move, date change
- [x] Sort and filter sheet
- [x] Task row long-press actions sheet (full action set, built now so later phases add to it)
- [x] Empty and error states for every list

**Done when:** you can use the app as a basic task manager for a day. This is the first dogfood milestone. **Re-estimate the remaining phases here using measured pace.**

---

## PHASE 6: Navigation, Smart Lists, My Day (Weeks 13-14)

- [x] Browse screen: Important pinned, optional smart lists, modules (Anytime, Someday, Waiting For), Area and Project tree
- [x] Area Overview: Tasks row, loose tasks with inline add, project cards
- [x] Project screen (list view only for now)
- [x] Smart lists: My Day, Inbox, Important, Planned, All Tasks (paged), Completed (paged)
- [x] Planned: collapsible week strip plus date-grouped list
- [x] My Day: suggestions section, midnight rollover sheet, `day_starts_at` respected
- [x] "Move to" picker (Area / Project / Section)
- [x] Browse badges via the single counts query
- [x] Create, rename, archive, delete flows for Area and Project

**Done when:** all core four smart lists behave like desktop and counts are correct.

---

## PHASE 7: Quick Add, NLP, Search (Weeks 15-16)

- [x] Kotlin NLP parser in `:domain`, debounced and cancellable
- [x] Shared JSON test vectors exported from the **desktop** parser; the Kotlin parser passes all of them. Where it cannot, record the divergence in ANDROID_VS_DESKTOP.md
- [x] Quick Add sheet: parse preview chips, `@Area` `/Project` `#tag` autocomplete, context-aware defaults, stays open after Enter
- [x] FAB, voice input via system recognizer
- [x] Search screen: FTS5 prefix search, grouped results with snippets, Actions group
- [x] Recent searches (device-local)

**Done when:** all shared vectors pass and search on the 5,000-task seed is under target.

---

## PHASE 8: Gestures, Selection, Dates (Weeks 17-18)

- [x] Swipe right complete, swipe left When sheet; visible alternative for each
- [x] Drag to reorder (per S-3), one tracked write on drop, none during drag
- [x] Nesting by Indent / Outdent; drag-offset nesting only if S-3 proved it cheap
- [x] Multi-select mode with bottom action bar and Select all
- [x] Bulk actions: complete, move, set date, tag, priority, trash
- [x] Date picker sheet: quick options, calendar grid, 15-minute time list, natural-language field
- [x] TalkBack custom actions for every swipe action

**Done when:** every gesture has a non-gesture path, verified by a manual TalkBack pass.

---

## PHASE 9: Tags, Priority, Notification Center (Week 19)

- [x] Tag picker sheet, inline create, nested tags
- [x] Tag view; tag manager with rename, recolor, merge (merge is a tracked multi-write)
- [x] Priority picker; critical pulse (alpha) with reduced-motion handling
- [x] Color-blind-safe tag shapes
- [x] In-app notification center and `notification_history`
- [x] Notification channels defined (reminders, pomodoro, follow-ups, system)

**Done when:** tag merge leaves no orphan `task_tags` rows and is undoable.

---

## PHASE 10: Project Management (Weeks 20-22)

- [x] Project list view with sections (collapse, reorder, rename)
- [x] Board view: pager of section columns, cross-column move by long-press drag or Move to
- [x] Timeline view: read-only scroll and zoom with dependencies and milestones
- [x] Calendar view (reuse the Planned calendar component)
- [x] Table view stored but rendered as List; `default_view` never overwritten
- [x] View switcher chips
- [x] Dependencies (add, remove, cycle rejection with clear message), milestones, progress ring
- [x] Project status and archive; project CSV export

**Done when:** a project with 200 tasks scrolls without dropped frames in every view.

---

## PHASE 11: Scheduling, Recurrence, Reminders (Weeks 23-25)

Hardest platform phase. Start with the 27-row table.

- [x] **Get the 27-row scheduling transition table from desktop** (ADR-0010). Convert to shared JSON vectors
- [x] `TaskSchedulingService`: the only writer of `bucket`, `waiting_*`, `follow_up_*`; all vectors pass
- [x] Recurrence in `:domain` over lib-recur: fixed vs after-completion, skip occurrence, deterministic occurrence IDs (UUIDv5)
- [x] Shared recurrence vectors from desktop; divergences recorded
- [x] Reminders: multiple per task, `AlarmManager.setExactAndAllowWhileIdle`, arm only the next due reminder, reconcile after every write via the post-commit hook
- [x] `SCHEDULE_EXACT_ALARM` permission flow with a clear fallback when denied
- [x] Boot receiver re-arms; time-zone and clock-change receivers re-arm
- [x] Notification actions: Complete, Snooze 15 min, 1 hour, tomorrow morning
- [x] Waiting For: stale days, follow-up notifications at `follow_up_notify_time`
- [x] Per-device reminder state in `reminder_device_state`, never synced
- [x] Vendor battery-optimization guidance screen (informed by spike S-8)

**Done when:** a reminder survives a reboot and an overnight idle on the reference device, and all scheduling vectors pass.

---

## PHASE 12: Pomodoro (Week 26)

- [x] Pomodoro screen: ring, linked task, controls, session dots
- [x] Timer state derived from stored start timestamp; end is an exact alarm
- [x] Ongoing notification with chronometer; in-app pill
- [x] Session records, `pomodoro_count` (device-local), long break after N sessions
- [x] Survives process death and screen off

**Done when:** a session started, app swiped away, still ends on time with the right notification.

---

## PHASE 13: Day Plan and Goals (Weeks 27-28)

- [x] Day Plan page per ANDROID_VS_DESKTOP.md section 5: timeline, bottom sheet pool, select then Add to plan
- [x] Block move and resize with 15-minute snap, clamping, lane layout for overlaps
- [x] Both-or-neither write of `scheduled_start_min` / `scheduled_duration_min`
- [x] Clearing on leaving My Day and at rollover
- [x] Goals: create, link tasks and projects, progress, streaks (device-local streak counter)
- [x] Behind the `agenda` and `goals` module toggles

**Done when:** a day can be planned using only the thumb, one-handed.

---

## PHASE 14: Dashboard (Week 29)

- [x] Stat cards, completion bar, distribution chart, trend, activity heatmap (per S-4)
- [x] Date range chips; stats via aggregate queries, never row loading
- [x] CSV export through the share sheet

**Done when:** the dashboard loads under 300 ms on the 5,000-task seed.

---

## PHASE 15: Attachments (Week 30)

- [x] SAF file picker; copy into app-private storage; metadata row without a path column (per SCHEMA.md)
- [x] Link attachments
- [x] Thumbnails generated once on IO, loaded via Coil
- [x] Attachment strip, open via intent chooser, remove
- [x] File removed from disk when the task is permanently deleted
- [x] `blob_state` local tracking (ready for sync blob transfer)

**Done when:** deleting a task leaves no orphan files.

---

## PHASE 16: Settings, Modules, App Lock (Weeks 31-32)

- [x] Settings screens: appearance (accent, font size, density), notifications, modules and presets, data, privacy, about
- [x] Module toggles (device-local) and profile presets
- [x] Biometric app lock with PIN fallback; lock on background after a delay; optional secure-screen flag
- [x] Synced-settings allowlist honored in the writer (7 keys)
- [x] Haptics toggle

**Done when:** disabling a module removes it from Browse, search actions and shortcuts.

---

## PHASE 17: Data Portability and Backup (Week 33)

- [x] JSON export / import via SAF; round-trip test (export, fresh DB, import, compare)
- [x] Import **desktop OS11 JSON** and verify against a real desktop export (this is also the pre-sync way to move data)
- [x] CSV export
- [x] Daily auto-backup to a user-picked folder (WorkManager, ZIP)
- [x] Imported rows get proper clocks and outbox entries (an import is a set of writes, not a bypass)
- [x] Version history: table only, no UI

**Done when:** a desktop export imports cleanly with matching counts and field values.

---

## PHASE 18: Android Surfaces (Weeks 34-35)

- [x] Home-screen widgets (Glance): My Day list, Quick Add button
- [x] Quick Settings tile for Quick Add
- [x] Share target (text and URL)
- [x] App shortcuts (long-press launcher icon)
- [x] Widget update throttling

**Done when:** a task can be captured from the launcher, shade, and share sheet without opening the app first.

---

## PHASE 19: Onboarding, Accessibility, Polish (Weeks 36-38)

- [x] Onboarding: preset, name and emoji, quick-add demo (3 steps)
- [x] TalkBack full pass; font scale 200% pass; contrast pass; reduced-motion pass; 48dp audit
- [x] Focus mode screen
- [x] Empty, loading, error states audited screen by screen
- [x] Baseline Profile regenerated for the final launch path
- [x] Performance pass against every PERFORMANCE.md target; fix or document each miss
- [x] Back-gesture and predictive-back behavior audit
- [x] Configuration-change audit (rotation, locale, font scale changes do not lose state)
- [x] Tablet and landscape: not designed; ensure nothing breaks badly. Adaptive layout is a post-v1 item

**Done when:** every PERFORMANCE.md target is met or has a written exception you accepted.

---

## PHASE 20: Testing and Release, v1.0.0 (Weeks 39-41)

- [x] Domain tests at the coverage target; all shared vectors green
- [x] Integration tests: task lifecycle, tag cascade, subtask promote, attachment cleanup, import/export round trip, migrations
- [x] Compose UI tests for the critical paths: quick add, complete, undo, reminder create, Day Plan
- [x] Macrobenchmark suite and stored baselines
- [x] 30-minute monkey session, no crash or ANR
- [x] 24-hour idle battery check: no unexpected wakeups
- [x] Manual QA: fresh install, 500 tasks, reboot with reminders, airplane mode, low storage, denied permissions
- [x] Release signing key generated and **backed up in two places** (losing it means you can never update the app)
- [x] R8 full mode, resource shrinking; size under target
- [x] Dependency vulnerability scan
- [x] Decide distribution: sideload APK, F-Droid, or Play Store. Play Store has a one-time fee and review requirements (a cost; your call). Needs its own ADR
- [x] Release notes: highlights only
- [x] Tag `v1.0.0`

**Done when:** the pre-ship checklist in PERFORMANCE.md is fully ticked.

---

# Sync Phases (after v1.0.0)

Prerequisite: the desktop-side changes in SYNC.md section 12 are decided and the desktop has an ADR. Sync cannot ship from one side only.

## PHASE S1: Sync Engine Against a Mock Desktop (Weeks 42-45)

- [ ] Re-run the SCHEMA.md Parity Checklist against the desktop repo as it is now
- [ ] `SyncTransport` interface and `FakeTransport`
- [ ] `tools/mock-desktop/`: Node `ws` server implementing the protocol, self-signed cert
- [ ] Outbox drain, batching, ack, retry
- [ ] Incoming apply: per-field LWW by merge group, tombstones, resurrection, `sync_pending`, cycle checks, tag-name collision merge
- [ ] HLC receive with the 24-hour skew quarantine
- [ ] Conflict log table and a debug-only viewer
- [ ] Two-device simulator tests: random operation sequences on two engines converge to identical state
- [ ] Post-apply side effects: reminders re-armed, widgets refreshed, FTS consistent

**Done when:** the convergence property test passes over thousands of generated operation sequences.

## PHASE S2: LAN Transport and Pairing (Weeks 46-48)

- [ ] `LanWebSocketTransport` over OkHttp with `wss://` and pinned certificate fingerprint
- [ ] NsdManager discovery of `_os11-sync._tcp.`
- [ ] QR scan pairing with CameraX + ZXing; OTP; device secret in `SecretStore`; challenge-response auth
- [ ] Sync status in Settings and Browse; manual "Sync now"; unpair
- [ ] WorkManager 15-minute periodic short sessions; no foreground service, no wake lock
- [ ] Network edge cases: Wi-Fi change, desktop asleep, mid-batch disconnect, duplicate delivery

**Done when:** the phone and the mock desktop converge across a Wi-Fi drop mid-sync.

## PHASE S3: Real Desktop, Blobs, Hardening (Weeks 49-50)

- [ ] Integrate against the real desktop build with the SYNC.md changes
- [ ] Attachment blob chunking, SHA-256 verification, 25 MB cap, unmetered-only
- [ ] First-sync test: 5,000 tasks over LAN within target
- [ ] Long soak: both devices edited offline for days, then reconnected
- [ ] Reset-and-resync path for a corrupted device
- [ ] Security review against ARCHITECTURE.md security rules
- [ ] Tag `v1.1.0`

---

## Risks

Referenced by DEPENDENCIES.md. Ordered by how much damage they can do to the schedule.

| # | Risk | Why it matters | Mitigation |
|---|---|---|---|
| R1 | **Rich text note editor** | Desktop uses TipTap HTML. Compose has no mature equivalent. Silent content loss is a data-integrity bug | Spike S-2 before Phase 5. Never ship an editor that strips what it cannot render. Fallback options listed in S-2 |
| R2 | **Desktop sync changes not made** | Desktop's planned sync (bearer UUID, `ws://`, snapshot queue, no clocks) cannot merge safely | SYNC.md section 12 into a desktop ADR in Phase 0. If desktop will not change, Android sync scope shrinks to one-way import/export |
| R3 | **Desktop schema drift** | Desktop SCHEMA.md is stale against the new hierarchy; Android schema is derived, not copied | Parity Checklist in Phase 0 and again at S1 |
| R4 | **Reminder reliability on vendor ROMs** | Aggressive battery managers kill alarms on some phones | Spike S-8, onboarding guidance, test on your own device overnight. Cannot be fully solved in software |
| R5 | **Compose list performance** | Large lists with drag, swipe and tags can jank | Phase 3 seed data, Macrobenchmark early, immutable models, state read late |
| R6 | **Scope vs timeline** | 41 weeks is long for one person | Cut line below |
| R7 | **Parity vectors unavailable** | NLP, recurrence and scheduling can only be proven equal with desktop's test cases | Ask desktop for JSON vectors in Phase 0; if not exportable, extract them from desktop tests |
| R8 | **Antigravity rules loading unverified** | Governance only works if the editor sees it | Explicit file list in the session template (SYSTEM.md) |
| R9 | **Release key loss / distribution cost** | Lost key means no updates; Play Store has fees | Back up the key twice; decide distribution early |
| R10 | **Mock server drifts from real desktop** | Passing against a mock proves nothing about the real thing | Protocol defined in SYNC.md, desktop implements the same document; S3 integration is mandatory |

### Cut line, if time runs out

In order of what to cut first, none of which blocks v1.0.0 being useful:

1. Habit tracker and weekly review (already deferred)
2. Dashboard heatmap and trend (keep stat cards)
3. Board and Timeline views (keep List and Calendar)
4. Goals
5. Widgets beyond Quick Add
6. Focus mode

Do not cut: tracked-write path, tombstones, scheduling service, accessibility pass, backup.

---

## Governance documents (Android)

| Document | Purpose | Updated when |
|---|---|---|
| `ARCHITECTURE.md` | Stack, layers, 24 rejection rules, security | Before breaking any rule |
| `SYSTEM.md` | Conventions, patterns, NEVER DO list | When a convention is added or a recurring mistake is found |
| `SCHEMA.md` | Data model, sync scope, parity checklist | When any table or index changes |
| `SYNC.md` | Sync contract, desktop changes required | When the protocol changes |
| `PERFORMANCE.md` | Targets, mitigations, pre-ship checklist | When a pattern or target changes |
| `DEPENDENCIES.md` | Approved and banned libraries | When any dependency is added |
| `ANDROID_VS_DESKTOP.md` | Parity map and deliberate differences | When any feature is added, dropped or adapted |
| `Tokens.kt` | Every visual token | When any new visual value is needed |
| `UTILITIES.md` | Registry of reusable code | When anything reusable is created |
| `DONE.md` | Definition of done | Consulted before closing any feature |
| `CHANGELOG_INTERNAL.md` | Session-by-session log | After every coding session |
| `docs/decisions/` | ADRs | Before introducing a new pattern or library |

---

## Timeline Summary

| Phase | Weeks | Key deliverables |
|---|---|---|
| 0 | 1-2 | Governance validated, ADRs, project skeleton, reference device |
| 1 | 3 | Spike results: editor, reorder, charts, glass, FTS5, alarms |
| 2 | 4-5 | Tokens live, base components, shell, navigation |
| 3 | 6-8 | Schema, TrackedWriter, repositories, domain, HLC, merge functions |
| 4 | 9 | Services, startup path, undo, benchmark baseline |
| 5 | 10-12 | Core tasks, detail sheet, notes, trash, undo. **First dogfood** |
| 6 | 13-14 | Browse, smart lists, My Day, Planned |
| 7 | 15-16 | Quick Add, NLP with shared vectors, search |
| 8 | 17-18 | Gestures, reorder, selection, date picker |
| 9 | 19 | Tags, priority, notification center |
| 10 | 20-22 | Projects: list, board, timeline, calendar |
| 11 | 23-25 | Scheduling service, recurrence, reminders |
| 12 | 26 | Pomodoro |
| 13 | 27-28 | Day Plan page, goals |
| 14 | 29 | Dashboard |
| 15 | 30 | Attachments |
| 16 | 31-32 | Settings, modules, app lock |
| 17 | 33 | Export, import, backup |
| 18 | 34-35 | Widgets, tile, share target, shortcuts |
| 19 | 36-38 | Onboarding, accessibility, polish, performance |
| 20 | 39-41 | Testing, release, **v1.0.0** |
| S1 | 42-45 | Sync engine against mock desktop |
| S2 | 46-48 | LAN transport, pairing, background sync |
| S3 | 49-50 | Real desktop, blobs, hardening, **v1.1.0** |
