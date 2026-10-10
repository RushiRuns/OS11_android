# OS11 Android - Architecture Rules

> This file is law, not suggestion.
> Every prompt to Antigravity (or any AI tool) must include or reference the relevant section here, plus SYSTEM.md.
> A feature that violates these rules does not ship. It goes back for a redesign.
>
> This file answers: what are the layers, who may talk to whom, what lives where.
> SYSTEM.md answers: how code is written day to day, and what is never done.

---

## Build Phase

**Android v1.0.0 (current target): standalone, fully offline, sync-ready.**
- Native Android. Kotlin. Jetpack Compose.
- All data is local. No cloud. No accounts. No AI/LLM. Nothing that costs money to run.
- The app is built so that sync with the desktop app is a transport problem only. Every write already produces sync metadata (field clocks, outbox rows, tombstones) from day one.
- The sync engine is built and tested against a fake transport and a local mock desktop server before the real desktop server exists.

**Sync go-live: LAN sync with the desktop app.**
- Matches the desktop ARCHITECTURE.md Phase 2 design: WebSocket over the local network, mDNS discovery, QR pairing, device UUID token.
- Still no cloud, no auth service, no relay operated by us.
- The exact protocol lives in SYNC.md. Changes this forces on the desktop app are listed there too.

Nothing in the Android codebase introduces a dependency on a cloud provider, Firebase, Google Play Services, an analytics or crash SDK, an ad SDK, or an AI API.

---

## Stack (locked)

| Concern | Choice |
|---|---|
| Language | Kotlin (latest stable at scaffold time) |
| UI | Jetpack Compose, Material3 used as an unstyled primitive layer only (see UI section) |
| Pattern | MVVM + Repository, unidirectional data flow |
| DI | Hilt |
| Database | SQLite via Room, with the bundled SQLite driver (see ADR-A02) |
| Async | Kotlin coroutines + Flow |
| Preferences | DataStore (device-local settings only; synced settings live in the `settings` table) |
| Background work | WorkManager + AlarmManager |
| Serialization | kotlinx.serialization |
| Min SDK | 26 |
| Target SDK | Current Play-required level at build time |
| Orientation | Phone portrait first. Layout code must not assume portrait (use window size classes) so adaptive layouts can be added later without a rewrite |
| Theme | Dark only. Own tokens. No Material You dynamic color. Ever |
| Editor | Antigravity AI code editor |

Application ID placeholder: `com.os11.app`. Change it before the first install you care about. It cannot change after a Play upload.

---

## Gradle Modules

Two modules. Not more. More modules slow the AI editor down and add build overhead with no benefit at this size.

```
:domain   Pure Kotlin/JVM module. No Android dependency is physically possible.
:app      Everything else (UI, application services, data, platform integration).
```

`:domain` holds: models, validation, recurrence, NLP parsing, scheduling transition table, fractional indexing, HLC and merge logic, dependency-cycle check, deterministic ID generation. Domain tests run on the JVM in seconds.

If an Android import is needed in `:domain`, the design is wrong. Move the Android part to `:app` behind an interface defined in `:domain`.

---

## Layer Order

Strict. No layer may be skipped. No direction may be reversed.

```
UI (Compose screens + ViewModels)
  |  observes Flow, sends intents
Application Services (@Singleton classes in :app, one per domain)
  |
Domain (pure functions in :domain - no I/O, no Android, no clock reads)
  |
Repositories (all database and file access lives here - nowhere else)
  |
Room DAOs (internal) -> SQLite / Filesystem
```

**What this means in practice:**

- A composable never touches a repository, DAO, application service, or domain logic function. It receives state and emits events through its ViewModel. Pure data classes from `domain.model` may be read by composables. Nothing else from `:domain`.
- A ViewModel calls application services for writes and observes Flows exposed by application services for reads. It never holds a repository. It contains no business rules.
- An application service may call other services. It never reaches into another service's repository.
- Repositories do not call other repositories. Cross-aggregate work happens in the service that owns the use case.
- Domain functions are pure: input in, output out. No `System.currentTimeMillis()`, no `LocalDate.now()`, no random. Time and randomness come in as parameters (`Clock`, `IdGenerator`).
- DAOs are `internal` to the repository package. Nothing outside `data/` can name a DAO.

---

## Package Layout (`:app`)

```
com.os11.app
  OS11Application.kt       - Hilt entry. Does almost nothing (see Startup)
  MainActivity.kt          - Single activity, Compose host
  di/                      - Hilt modules
  data/
    db/                    - Room database, entities, DAOs (internal), converters, migrations
    repository/            - One repository per aggregate. Only writers of the DB
    sync/                  - TrackedWriter, ApplyRemoteWriter, outbox, SyncEngine, transports
    files/                 - Attachment storage, SAF export/import/backup writers
    secrets/               - Keystore-backed SecretStore
  service/                 - Application services, one package per domain
    task/ scheduling/ area/ project/ tag/ reminder/ notification/
    pomodoro/ goal/ attachment/ settings/ nlp/ search/ backup/ lock/ sync/
  platform/                - Android-only integration, behind interfaces
    alarms/ notifications/ widgets/ shortcuts/ tiles/ share/ haptics/ biometrics/
  ui/
    theme/                 - Tokens.kt, OS11Theme, typography, motion
    components/            - Dumb reusable composables (props in, events out)
    navigation/            - NavHost, routes
    features/              - Screen + ViewModel + UiState per feature
      inbox/ myday/ dayplan/ planned/ browse/ area/ project/ task/ tags/
      quickadd/ search/ pomodoro/ goals/ dashboard/ settings/ onboarding/ sync/
    util/                  - Compose-only helpers
```

```
:domain
  model/                   - Task, Area, Project, Tag, etc. (immutable data classes)
  logic/
    validation/ recurrence/ nlp/ scheduling/ ordering/ sync/ ids/
  time/                    - Clock abstraction, ISO helpers
```

---

## Data Flow and State

- **The database is the single source of truth.** Screens observe `Flow`s that originate in Room. There is no second in-memory store of tasks.
- ViewModels expose one `StateFlow<UiState>` per screen, built from service Flows (which wrap repository Flows) with `stateIn(WhileSubscribed)`. They do not copy entities into their own mutable collections.
- Local writes are fast enough (target: under 16 ms) that the UI does not need an optimistic layer. The Flow re-emits from the DB. For gesture feedback that must not wait (checkbox spring, drag), the composable holds transient visual state only.
- No duplicated state. If a task lives in the DB, no ViewModel keeps a parallel list that can disagree with it.
- UI-local state (sheet open, text field content, scroll position) stays in `remember` / `rememberSaveable` or the screen's ViewModel `UiState`.
- Configuration changes and process death: `SavedStateHandle` for draft input and navigation arguments. Content always reloads from Room.

### Write path

```
user action -> ViewModel intent -> application service
  -> validate (domain) -> repository.write via TrackedWriter
       one Room transaction:
         data row write + field clocks + outbox entry (+ tombstone if delete)
  -> Flow re-emits -> UI updates
  -> side effects scheduled after commit (reminder alarms, widget refresh)
```

Never reversed. Never parallelized on the same record. Side effects (alarms, widget updates, notifications) are scheduled only after the transaction commits.

### Undo

Destructive actions show a Snackbar with Undo (5 seconds). An `UndoManager` in the service layer keeps up to 100 entries per session. Undo is itself a normal tracked write, so it syncs. Autosave and undo never run concurrently on the same record.

---

## Threading

- Main thread: composition, input, animation. No database, no file, no network, no JSON parsing, no regex-heavy parsing.
- Room runs on its own query executors. Repositories expose `suspend` and `Flow` only. No blocking calls.
- Dispatchers are injected (`@IoDispatcher`, `@DefaultDispatcher`). No hardcoded `Dispatchers.IO` in feature code.
- CPU-heavy work (tree flattening of 1000+ tasks, NLP on long input, import, stats aggregation, export) runs on `Default`/`IO`, never on `Main`.
- No `GlobalScope`. No `runBlocking` outside tests. Scope is `viewModelScope`, a service scope owned by Hilt, or WorkManager.
- Functions that cross async boundaries pass immutable values. No shared mutable objects.

---

## Startup

The first frame must not wait for anything optional.

- `Application.onCreate` does: Hilt init, nothing else. No sync, no alarm rescheduling, no migration of large tables, no attachment scan, no stats.
- Use the AndroidX SplashScreen API. The splash holds only until the first task list has its first emission, with a hard cap.
- Room migrations run when the database is first opened, before the first query. Keep migrations small and tested.
- Deferred after first frame: reminder alarm reconciliation, sync engine start, widget refresh, backup check, trash purge.
- Baseline Profile and Startup Profile are generated for the critical path (launch to My Day visible).

---

## Sync Architecture (summary)

Full detail in SYNC.md. The rules that matter for the architecture:

1. **Only `TrackedWriter` writes synced tables locally.** Only `ApplyRemoteWriter` writes remote changes in. Both are the only code allowed to call DAO write methods.
2. Every synced field write produces a **hybrid logical clock (HLC)** value stored in `sync_field_clock`. Conflict resolution is per-field last-write-wins on HLC, with a conflict history log.
3. Local writes append to `sync_queue` (the outbox). The sync engine drains the outbox. It never reads live task tables to decide what to send.
4. Deletes are tombstones in `sync_tombstone`. Rows that are only trashed (`is_trashed`) sync as ordinary field changes.
5. Remote changes never overwrite local state without passing through the merge function in `:domain`.
6. The transport is an interface (`SyncTransport`). The engine does not know whether it is talking to a fake, a mock server, or the real desktop.
7. Settings, modules, notification history, and per-device reminder state are device-local unless SYNC.md lists them as synced.

---

## Platform Integration

Each of these is a thin adapter in `platform/` behind an interface, called from services. Services hold the logic. Adapters hold the Android API calls.

| Capability | Mechanism | Notes |
|---|---|---|
| Reminders | `AlarmManager.setExactAndAllowWhileIdle` when exact alarm access is granted, inexact fallback otherwise | Reconciled on boot, time change, timezone change, app update, and app start |
| Notifications | Notification channels per category (due, reminder, pomodoro, agenda, goal, streak) | `POST_NOTIFICATIONS` runtime permission on API 33+. Actions: Complete, Snooze 15m, Snooze 1h, Snooze tomorrow morning |
| Pomodoro timer | Persisted start timestamp + duration, exact alarm for phase end, ongoing notification using chronometer | The timer is derived from timestamps, never from a tick counter. No foreground service in v1 |
| Quick capture | Home-screen widget (Glance), Quick Settings tile, share target (`ACTION_SEND` text), app shortcuts | All route into the same quick-add service path |
| Voice input | Platform `RecognizerIntent` | Result goes into the normal NLP parser. No speech SDK |
| Widgets | Jetpack Glance: My Day list, Quick Add button | Widget reads via a read-only repository query. Never shows data when app lock is on |
| Export / import / backup | Storage Access Framework | No storage permissions requested. Daily backup via WorkManager to a user-picked folder URI |
| Biometrics | `androidx.biometric` | PIN fallback |
| Haptics | `LocalHapticFeedback` + a small wrapper | Light on complete, drag start, timer end. Setting `haptics_enabled` is device-local |

### Permissions (complete list - anything else is a rejection)

`POST_NOTIFICATIONS`, `SCHEDULE_EXACT_ALARM`, `RECEIVE_BOOT_COMPLETED`, `VIBRATE`, `USE_BIOMETRIC`, `INTERNET`, `ACCESS_NETWORK_STATE`, `ACCESS_WIFI_STATE`, `CAMERA` (QR pairing only, requested at the moment of scanning).

`INTERNET` exists only for the sync transport talking to paired peers on the local network. No other HTTP client exists in the app.

---

## UI Architecture

### Primitive layer, not a design system

Desktop uses Radix as headless primitives and styles everything with tokens. Android does the same with Material3:

- Material3 components may be used for behavior and accessibility semantics (sheets, text fields, switches, snackbars, pickers).
- Their look is fully overridden from `Tokens.kt`. Feature code never reads `MaterialTheme.colorScheme`, `MaterialTheme.typography`, or `dynamicColorScheme`. It reads `OS11Theme.colors`, `OS11Theme.spacing`, and so on.
- If a Material3 component cannot be fully restyled with tokens, build the visual part ourselves and keep only the semantics.

### Tokens

- `ui/theme/Tokens.kt` is the Android port of desktop `tokens.css`. Same names, same values, in Kotlin. Parity table and deliberate differences are in ANDROID_VS_DESKTOP.md.
- If a token does not exist for something, add it to `Tokens.kt` first, then use it.
- Dark only. The token structure is a `CompositionLocal` so a second palette could be added later without touching feature code.

### Navigation

- Bottom bar (4 items): My Day, Inbox, Planned, Browse. Browse holds the Area/Project tree, remaining smart lists (Important, Anytime, Someday, Waiting For, All Tasks, Completed), Tags, Trash.
- Profile menu (avatar in the Browse top bar): Dashboard, Goals, Pomodoro, Settings. Glass-style sheet.
- Compose Navigation with type-safe routes. One `NavHost`. Deep links only for widgets, notifications, and share.
- Detail view for a task is a full-height bottom sheet over the current screen (the Android form of the desktop right-side panel).
- Quick Add is a bottom sheet with the keyboard open. Opens from a FAB on list screens, widget, tile, and share.
- Day Plan is its own page, opened from an icon opposite the "My Day" title.

### Animation rules

Compose animation is cheap to write and easy to abuse. Same discipline as desktop's four-site rule.

| # | Feature | Permitted motion |
|---|---|---|
| 1 | Checkbox completion | Spring overshoot + strikethrough |
| 2 | Task detail sheet open/close | Spring slide up |
| 3 | List reorder / nesting drag | Item displacement and drop settle |
| 4 | Quick-add sheet appear/dismiss | Scale + fade with the sheet |

Everything else uses `tween` with `MotionTokens` durations and easings (`fast` 120, `default` 180, `slow` 280). Animate with `graphicsLayer { }` lambdas or `animate*AsState` read inside layout/draw phases. No animation that recomposes a whole list per frame.

**Reduced motion:** respect the system animator duration scale (0 means off) and the app setting `reduce_motion`. Every animated component reads `LocalReduceMotion`.

### Component styling rules

1. Every composable uses only token references. No hardcoded color, dp, sp, radius, elevation, or duration in feature or component code. Ever.
2. Priority is a 2.5dp left border only. Never a badge, icon, or separate element. Critical pulses (alpha only, reduced-motion aware).
3. Tag display has three forms: **Dot** (18dp circle) resting on task rows, **Chip** (pill with label) in detail view and filters, **Project dot** (10dp circle + text) in the Browse tree only. Color is never the only signal (shape variants from desktop carry over).
4. Star uses `starColor`. Never red, never accent.
5. Overdue date chips use `danger` for text and its muted tint for the background. No one-off colors.
6. Every interactive element has a touch target of at least 48dp, even when the visual is smaller.
7. Every interactive element has a content description or semantics role. Lists expose collection semantics.
8. Text respects the system font scale. No fixed-height text containers.
9. Glass surfaces (profile menu, popovers) use a translucent surface token plus border. True backdrop blur is not assumed (see ADR-A07).

### Layout and insets

- Edge-to-edge. Every screen handles system bar and IME insets.
- Layout is driven by `WindowSizeClass`, not by orientation checks. v1 ships only the compact layout, but no code may assume compact.

---

## Security Rules

```
android:allowBackup="false" and no cloud backup rules. Device data does not
leave the device except through explicit user export and LAN sync.

All Activities, Services, Receivers, Providers are exported="false" unless
they are the launcher, the share target, or a widget/tile entry point, and
those validate every incoming intent extra.

All user input is validated at the service layer before it reaches the DB
or filesystem. ViewModels never trust deep-link or intent extras.

Room queries use bound parameters. No string concatenation into SQL. The
FTS query builder escapes user text.

Secrets (sync pairing token, sync private material, PIN hash and salt) are
wrapped with an Android Keystore key (AES-GCM) via SecretStore. Never stored
in plain DataStore, plain SQLite, or logs.

App lock: biometric prompt with PIN fallback. PIN stored as salted hash.
Optional FLAG_SECURE hides content from recents and screenshots.

Files: attachments live in app-internal storage only. External access goes
through SAF URIs the user picked. No broad storage permission.

Network: the only network code is the sync transport. Sync connections use
TLS with a certificate pinned from the pairing QR (see SYNC.md). No cleartext
exception is added to the network security config for sync.

Logging: no task titles, notes, tokens, or personal text in logs in release
builds.

Dependency audit (OSV scan or Gradle dependency check) runs before every
release. No dependency with a known high or critical vulnerability ships.
```

---

## Schema and Migration Rules

- Every schema change is a Room migration with a test, and the exported schema JSON is committed.
- `exportSchema = true`. Schema JSON files live in `app/schemas/` and are reviewed in every diff.
- Migrations run forward only. No destructive fallback (`fallbackToDestructiveMigration` is banned).
- FTS5 virtual table and its triggers are created in a migration, not declared as a Room entity. Startup self-test asserts FTS5 is available.
- IDs are UUID v4 strings. Dates are ISO 8601 strings. Booleans are INTEGER 0/1. Same as desktop, because sync payloads carry these as-is.
- Any change to a column that syncs needs a parity note in SCHEMA.md and, if it changes the wire format, an update to SYNC.md.
- No AI tool touches migration files or SCHEMA.md without human review of the migration path.

---

## Hard Rules - The Rejection List

If a proposed implementation violates any of these, it is rejected and redesigned before code is written.

 1. Composables do not import repositories, DAOs, application services, or `domain.logic`.
 2. All persistence goes through the repository layer. DAOs are `internal` to `data/`. No `@Query` outside `data/db/dao`.
 3. All dates are stored as ISO 8601 strings (`YYYY-MM-DDTHH:mm:ss.sssZ`, or `YYYY-MM-DD` for date-only fields). `java.time` types in code. Conversion only in converters.
 4. Services return typed results (`AppResult`). Exceptions do not cross a layer boundary into UI. ViewModels map failures to UI state or effects.
 5. State lives in one place per domain. The DB is truth. No ViewModel mirrors a list that the DB can answer.
 6. No new dependency without a written justification in DEPENDENCIES.md. Free and open-source only.
 7. No new architectural pattern without a decision record in `docs/decisions/ADR-A*.md`.
 8. Every schema change requires a migration, a migration test, and the exported schema JSON.
 9. No feature ships without at least one test.
10. UI contains zero business logic. Validation, computation, and data transformation live in services or `:domain`.
11. No cloud provider SDK, Firebase, Google Play Services, analytics, crash reporting, ad SDK, external auth SDK, or AI/LLM client without a formal ADR and explicit founder sign-off. Nothing that needs a paid account or paid tier to function.
12. No hardcoded color, dp, sp, radius, elevation, or duration in any composable. Every visual value comes from `Tokens.kt`.
13. Spring/overshoot motion exists at exactly the 4 permitted sites. Any new animation site requires an ADR.
14. Material3 is a primitive layer only. No feature code reads `MaterialTheme.*` values. No dynamic color.
15. **Single writer for scheduling.** Only `TaskSchedulingService` may write the guarded scheduling fields (`bucket`, `waiting_on`, `waiting_since`, `follow_up_date`, `follow_up_notified_on`, `reviewed_at`) and the date group fields (`due_date`, `due_time`, `all_day`, `recurrence_rule`). An architecture test enforces it.
16. **Startup path purity.** `Application.onCreate` and the first-frame path may not initialize sync, attachments, dashboard, timeline, pomodoro, goals, or backup. Those start lazily or after first frame.
17. **Sync writer rule.** Synced tables are written only through `TrackedWriter` (local) and `ApplyRemoteWriter` (remote). No synced field is added without a sync scope decision in SCHEMA.md.
18. **Sync read rule.** The sync engine reads the outbox and tombstones. It never scans live entity tables to decide what to send.
19. No `GlobalScope`, no `runBlocking` in production code, no blocking I/O on the main thread, no hardcoded dispatchers in feature code.
20. No `Clock`, `now()`, or randomness read inside `:domain`. Time and IDs are injected.
21. Every interactive element has a 48dp minimum touch target and accessibility semantics.
22. No network code outside the sync transport. `INTERNET` is used for nothing else.
23. `allowBackup` stays false. No cloud copy of user data, ever, without an ADR.
24. Synced entities with natural-key uniqueness (tags by name) must use the collision-merge rule in SYNC.md. Default-seeded entities use deterministic IDs.

---

## Decision Records

Significant decisions live in `docs/decisions/` as `ADR-A00N-title.md`. Format:

```
ADR-A01: Native Kotlin and Compose instead of React Native
Status: Accepted
Context: ...
Decision: ...
Consequences: ...
```

When a prompt would introduce a new pattern, library, or structural change: write the ADR first, then build.

Initial set (written in Phase 0): A01 native Kotlin/Compose, A02 Room with bundled SQLite and FTS5, A03 two Gradle modules, A04 Material3 as primitive layer, A05 DB-as-source-of-truth (no optimistic layer), A06 sync metadata from day one (HLC, field clocks, tombstones), A07 glass surfaces without backdrop blur, A08 no Google Play Services and no cloud backup, A09 timers from timestamps, A10 own NLP parser with shared test vectors.
