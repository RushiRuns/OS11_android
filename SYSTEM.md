# OS11 Android - SYSTEM.md

> Conventions, patterns, and "never do X" rules for every line of code in this repo.
> ARCHITECTURE.md decides the structure: layers, modules, who may call whom.
> This file decides the habits: how things are named, shaped, written, tested, and committed.
> If the two ever disagree, ARCHITECTURE.md wins and this file gets fixed.

Every Antigravity session starts with this file and ARCHITECTURE.md in context. I have not verified how Antigravity loads project rules automatically, so the session template at the bottom names both files explicitly and works regardless of that mechanism.

---

## 1. Naming

| Thing | Convention | Example |
|---|---|---|
| Screen composable | `<Feature>Screen` | `MyDayScreen` |
| ViewModel | `<Feature>ViewModel` | `MyDayViewModel` |
| UI state | `<Feature>UiState` (sealed or data class, immutable) | `MyDayUiState` |
| User intents | `<Feature>Event` (sealed) | `MyDayEvent.TaskChecked` |
| One-shot effects | `<Feature>Effect` (sealed) | `MyDayEffect.ShowUndo` |
| Reusable component | Noun, no "Custom" or "My" prefix | `TaskRow`, `PriorityBorder` |
| Application service | `<Domain>Service` | `TaskService`, `ReminderService` |
| Repository interface | `<Aggregate>Repository` | `TaskRepository` |
| Repository impl | `Room<Aggregate>Repository` | `RoomTaskRepository` |
| DAO | `<Aggregate>Dao` (internal) | `TaskDao` |
| Room entity | `<Aggregate>Entity` | `TaskEntity` |
| Domain model | Plain noun | `Task`, `Area`, `Project` |
| UI model (only when the domain model is wrong for display) | `<Thing>UiModel` | `TaskRowUiModel` |
| Domain function file | `<Topic>.kt` with top-level pure functions | `Recurrence.kt` |
| Token | PascalCase property in a token class | `OS11Theme.colors.accentMuted` |
| Test | Backtick sentence | `` `completing a recurring task creates next occurrence once` `` |
| Package | lowercase, no underscores | `service.scheduling` |
| DB table and column | snake_case, identical to desktop SCHEMA.md | `scheduled_start_min` |
| Wire (sync) field | snake_case, identical to DB column | `follow_up_date` |
| Branch | `phase-N/<feature>` | `phase-4/task-detail-sheet` |

Domain concepts use the same words as desktop. Area, Project, Task, Subtask, Inbox, My Day, Planned, Anytime, Someday, Waiting For. Do not rename them for Android.

---

## 2. Kotlin Rules

- Immutable by default. `val` over `var`. `data class` with `val` fields for models. Copy to change.
- No `!!`. Use `requireNotNull(x) { "reason" }` when a null is a bug, or handle the null.
- No platform types leaking out of Java APIs without an explicit nullability decision at the boundary.
- No `lateinit` except for Hilt/Android framework injection points.
- Sealed types for closed sets (events, errors, UI state). `when` on them is exhaustive with no `else`.
- Enums map to DB strings through an explicit `dbValue`, never through `name` or `ordinal`. Renaming an enum constant must not corrupt data or break sync.
- Extension functions are fine for mapping (`TaskEntity.toDomain()`). They live next to the thing they map, not in a global `Utils.kt`.
- A function that does more than one thing gets split. A file over about 400 lines gets split.
- Comments explain why, never what. No commented-out code.
- No wildcard imports.
- Explicit visibility. Default to `internal` or `private`. `public` is a decision.

---

## 3. Feature Shape

One folder per feature under `ui/features/<feature>/`:

```
myday/
  MyDayScreen.kt        - stateless composable: (state, onEvent) -> UI
  MyDayRoute.kt         - wires ViewModel to Screen, collects state and effects
  MyDayViewModel.kt
  MyDayUiState.kt       - state, events, effects in one file
  components/           - pieces used only by this feature
```

The `Route` / `Screen` split is mandatory. `Screen` takes plain parameters, so it can be previewed and tested without Hilt or a ViewModel.

### ViewModel pattern

```kotlin
@HiltViewModel
class MyDayViewModel @Inject constructor(
    private val tasks: TaskService,
) : ViewModel() {

    val state: StateFlow<MyDayUiState> = tasks.observeMyDay()
        .map<_, MyDayUiState> { MyDayUiState.Content(it.map(Task::toRowUiModel)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MyDayUiState.Loading)

    private val _effects = Channel<MyDayEffect>(Channel.BUFFERED)
    val effects: Flow<MyDayEffect> = _effects.receiveAsFlow()

    fun onEvent(event: MyDayEvent) {
        when (event) {
            is MyDayEvent.TaskChecked -> viewModelScope.launch {
                when (val r = tasks.setCompleted(event.id, event.checked)) {
                    is AppResult.Ok -> _effects.send(MyDayEffect.ShowUndo(r.value))
                    is AppResult.Err -> _effects.send(MyDayEffect.ShowError(r.error.userMessage()))
                }
            }
        }
    }
}
```

Rules:
- One `StateFlow<UiState>` per screen. UI state is a snapshot. Never expose `MutableStateFlow`.
- One-shot things (snackbars, navigation, haptics) go through the effects channel. Never encode them as a boolean in state that someone must remember to reset.
- Reads come from Flows exposed by application services. A ViewModel never holds a repository, DAO, or DB reference.
- Writes go through `onEvent` into a service call. The composable never calls a service.
- No business rules in the ViewModel. If it has an `if` about what a task may do, that `if` belongs in a service or in `:domain`.

### Service pattern

```kotlin
@Singleton
class TaskService @Inject constructor(
    private val tasks: TaskRepository,
    private val clock: Clock,
    private val ids: IdGenerator,
    private val undo: UndoManager,
    private val reminders: ReminderScheduler,
) {
    fun observeMyDay(): Flow<List<Task>> = tasks.observeMyDay(clock.today())

    suspend fun setCompleted(id: String, done: Boolean): AppResult<UndoToken> { ... }
}
```

A service method: validate through `:domain`, call the repository inside one tracked write, then schedule side effects after commit, then return an `AppResult`.

### Errors

```kotlin
sealed interface AppResult<out T> {
    data class Ok<T>(val value: T) : AppResult<T>
    data class Err(val error: AppError) : AppResult<Nothing>
}

sealed interface AppError {
    data class Validation(val field: String, val reason: String) : AppError
    data object NotFound : AppError
    data class RuleViolation(val rule: String) : AppError   // e.g. scheduling R7
    data class Unexpected(val cause: Throwable) : AppError
}
```

- Repositories and services return `AppResult` for expected failures. They throw only for programmer errors.
- `CancellationException` is always rethrown. Never swallow it in a `catch (e: Exception)`.
- A catch block logs (without user text) and converts. It never silently ignores.
- The UI shows a short human message from `userMessage()`. No stack traces, no raw exception text.

---

## 4. Compose Rules

- **State hoisting.** Components are stateless. State and event lambdas come from the caller.
- **Stable inputs.** Parameters are primitives, immutable data classes, or `ImmutableList`/`ImmutableMap` (kotlinx.collections.immutable). Never a `List<T>` of a mutable thing. This keeps skipping effective.
- **Lazy lists.** Always `key = { it.id }` and `contentType`. Never index-based keys for data that can reorder.
- **Read state late.** If a value is only needed in layout or draw (scroll offset, animation progress), read it inside a `Modifier.graphicsLayer { }` / `drawBehind { }` / `offset { }` lambda, not in composition.
- **`derivedStateOf`** for values computed from fast-changing state (scroll position to "show button").
- **`remember`** anything that allocates or computes. `rememberSaveable` for input that must survive process death.
- **Side effects.** `LaunchedEffect` is for reacting to a key (collecting effects, scrolling to an item). It is never where a business action starts.
- **Collect with lifecycle.** `collectAsStateWithLifecycle()`. Never plain `collectAsState()` for ViewModel flows.
- **Previews.** Every component and every Screen has `@Preview` with fake data, in the dark theme. Previews use `OS11Theme`. A component without a preview is not done.
- **Tokens only.** `OS11Theme.spacing.s4`, `OS11Theme.colors.textSecondary`, `OS11Theme.radius.md`. No literal `16.dp`, `Color(0xFF...)`, `14.sp`, `RoundedCornerShape(8.dp)`.
- **Semantics.** `Modifier.semantics` or built-in roles on every interactive element. Checkboxes are `Role.Checkbox` with a state description. Rows merge descendants into one TalkBack stop with a sensible label.
- **Touch targets.** `Modifier.minimumInteractiveComponentSize()` or an explicit 48dp hit area on anything tappable.
- **Insets.** Each screen consumes insets once, at the right level. Never double-pad.
- **No nested scrolling surprises.** A scrollable inside a scrollable needs an explicit bounded height.
- **Gestures.** Swipe, long-press, and drag go through `Modifier.pointerInput` or the approved reorder library, with a visible non-gesture alternative for every gesture action (overflow menu or long-press sheet). Gestures are never the only path to an action.

---

## 5. Time, IDs, Ordering

- **Time.** Domain and services take a `Clock` (an interface in `:domain`). Production uses system time. Tests use a fixed clock. No `Instant.now()`, `LocalDate.now()`, or `System.currentTimeMillis()` outside the `Clock` implementation.
- **"Today".** Always computed through the `day_starts_at` setting, in one function (`Clock.today()`). Never `LocalDate.now()` in feature code.
- **Storage format.** ISO 8601 strings, UTC with `Z` for instants, `YYYY-MM-DD` for date-only, `HH:mm:ss` for time-only. Same as desktop.
- **IDs.** UUID v4 strings from `IdGenerator`. Seeded defaults and recurring-occurrence rows use deterministic IDs (UUID v5) from `:domain/ids`. Never `UUID.randomUUID()` directly.
- **Ordering.** `sort_order` is a REAL with fractional indexing from `:domain/ordering`, ties broken by `id`. Never renumber a whole list on a single move. A rebalance (when the gap gets too small) is its own service operation and writes through `TrackedWriter` like any other change.

---

## 6. Database and Repository Conventions

- Entities mirror SCHEMA.md exactly: column names, nullability, defaults.
- DAOs are `internal`. They contain `@Query` and nothing else. No logic.
- Repositories map entity to domain model, never expose entities, and expose `Flow` for reads and `suspend` for writes.
- Reads that feed lists return paged or bounded data. No `SELECT *` of an unbounded table into memory.
- Counts shown in the UI (badges) come from a `COUNT` query, not from loading rows.
- Search goes through the FTS5 table. The query text is escaped by one function, `FtsQuery.escape`, and nowhere else.
- Multi-row changes (move task with subtasks, delete project) run in one transaction.
- A cascade that removes synced rows must also write their tombstones in the same transaction (see SYNC.md).

---

## 7. Touching a Synced Field

Before adding or changing any column on a synced table:

1. Decide its sync scope in SCHEMA.md: `synced`, `device-local`, or `derived`.
2. If `synced`, add it to the entity field list that `TrackedWriter` diffs, and add a merge test vector.
3. If it changes the wire format, update SYNC.md and note the desktop change needed.
4. Write the migration and its test.

A write path example:

```kotlin
// Inside RoomTaskRepository. The only way to write a synced task.
override suspend fun update(id: String, patch: TaskPatch): AppResult<Task> =
    tracked.update(TaskEntity::class, id) { current ->
        current.applyPatch(patch)          // pure
    }
```

`TrackedWriter` does the diff, ticks the HLC per changed field, writes field clocks and the outbox row, all in one transaction. A repository that calls a DAO write directly is a rejection.

---

## 8. Testing

- **Domain** (`:domain`): JVM unit tests, table-driven. Target 100% of branches for validation, recurrence, NLP, scheduling transitions, ordering, merge.
- **Shared vectors.** NLP, recurrence, scheduling transitions, and merge behavior are tested from JSON files in `domain/src/test/resources/vectors/`. The desktop repo consumes the same files. A vector that passes on one platform and fails on the other is a parity bug and blocks release.
- **Repositories:** Room tests against an in-memory database with the bundled driver. Cover every query, every cascade, every tombstone.
- **Services:** unit tests with fake repositories, fixed `Clock`, fixed `IdGenerator`. Happy path and each error path.
- **ViewModels:** tests with Turbine on `state` and `effects`.
- **Compose:** UI tests for each Screen's empty state, populated state, and one interaction. Semantics are asserted (content descriptions, roles), not just text.
- **Migrations:** `MigrationTestHelper` from every previous version to current, with data.
- **Sync engine:** two in-memory devices plus a fake transport; randomized interleavings of edits, deletes, reorders, and recurring completions must converge to identical state.
- **Architecture tests:** a test that scans imports and fails on: composable importing repository/DAO/service, DAO called outside `data/`, `MaterialTheme.` outside `ui/theme/`, literal `dp`/`sp`/`Color(` outside `ui/theme/`, `Dispatchers.` outside `di/`, `GlobalScope`, `runBlocking` in main sources.
- **Performance:** Macrobenchmark for cold start and for scrolling a 1,000-task list. Run before every release (see PERFORMANCE.md).
- No flaky tests. A flaky test is fixed or deleted the same day.

---

## 9. Git and Session Conventions

- One session = one feature = one commit. The session goal is one sentence.
- Commit message: `Phase N: <feature> - <one line>`.
- Update before committing: CHANGELOG_INTERNAL.md, UTILITIES.md (if anything reusable was created), DONE.md checklist walked.
- A session never touches files outside its declared scope. If the work needs another file, stop and ask.
- No drive-by refactors. A refactor is its own session with its own scope.
- If a rule in ARCHITECTURE.md or SYSTEM.md blocks the straightforward solution, stop and say so. Do not route around it.
- Dependencies, migrations, and SCHEMA.md changes need human review before they land.

### Session template (copy for every Antigravity session)

```
# Session Goal
[One sentence. One thing only.]

# Read First
- ARCHITECTURE.md - section: [layer touched]
- SYSTEM.md - sections: [which apply]
- SCHEMA.md - tables: [read/written]
- SYNC.md - [only if a synced field or write path is touched]
- PERFORMANCE.md - [only if a list, startup path, or heavy work is touched]
- Tokens.kt - [if building any UI]
- UTILITIES.md - check BEFORE creating any component, helper, or domain function
- DEPENDENCIES.md - [only if a new library is being considered]

# Scope
- Create: [exact paths]
- Modify: [exact paths]
- Do NOT touch: [exact paths]

# Definition of Done
- [ ] Per DONE.md, every line
- [ ] Architecture test suite passes
- [ ] Tests written and passing
- [ ] CHANGELOG_INTERNAL.md and UTILITIES.md updated
- [ ] Committed with the required message format

# North Star Check
- [ ] Speed: first frame, scroll, and interaction targets still hold
- [ ] Convenience: any friction the user should not have to deal with?
- [ ] Modular: should this sit behind a module toggle?
- [ ] Feel: does this add anything to the default view that belongs one tap deeper?
- [ ] Parity: does it match desktop behavior, or is the difference listed in ANDROID_VS_DESKTOP.md?
```

---

## 10. NEVER DO

### Architecture
- Never call a repository, DAO, or service from a composable.
- Never hold a repository in a ViewModel.
- Never put an `if` about task rules in a ViewModel or composable.
- Never import `android.*` or `androidx.*` into `:domain`.
- Never write a synced table except through `TrackedWriter` or `ApplyRemoteWriter`.
- Never let the sync engine read live entity tables to decide what to send.
- Never write a scheduling field (`bucket`, `waiting_*`, `follow_up_*`, `reviewed_at`, `due_*`, `all_day`, `recurrence_rule`) outside `TaskSchedulingService`.
- Never add a Gradle module without an ADR.

### Data
- Never use `fallbackToDestructiveMigration`.
- Never change a column without a migration, a migration test, and the exported schema JSON.
- Never store a date as epoch millis, a locale string, or anything but the ISO formats above.
- Never use `ordinal` or enum `name` as the stored value.
- Never hard-delete a synced row without writing its tombstone in the same transaction.
- Never renumber an entire list's `sort_order` for one move.
- Never store `local_path` as an absolute path that another device would need. Derive it from id and filename.
- Never `SELECT *` an unbounded table into memory.
- Never build SQL by string concatenation.
- Never store task text, tokens, or notes in logs, crash reports, or notification channels names.

### Concurrency and time
- Never `GlobalScope`. Never `runBlocking` in production code.
- Never swallow `CancellationException`.
- Never block the main thread with I/O, parsing, or sorting a large list.
- Never read the clock or generate an ID or random value inside `:domain`.
- Never call `LocalDate.now()` or `System.currentTimeMillis()` outside the `Clock` implementation.
- Never drive a timer from a tick counter. Timers derive from a stored start timestamp.

### UI
- Never hardcode a color, dp, sp, radius, elevation, or duration.
- Never read `MaterialTheme.colorScheme`, `MaterialTheme.typography`, or use dynamic color.
- Never add an animation site beyond the four permitted ones without an ADR.
- Never animate by recomposing a whole list each frame.
- Never make a gesture the only way to do something.
- Never ship an interactive element under 48dp or without semantics.
- Never use color alone to carry meaning (priority, tags, overdue all have a second signal).
- Never show a modal confirmation for a reversible action. Do it and offer Undo.
- Never add a visible element to a default view for something that can live one tap deeper.
- Never render an empty field. No due date means no date chip at all.
- Never use `collectAsState()` on ViewModel flows.
- Never use index as a lazy list key for reorderable data.

### Platform and privacy
- Never add a permission that is not in the list in ARCHITECTURE.md.
- Never add Firebase, Google Play Services, analytics, crash reporting, ads, or any SDK that phones home.
- Never add an HTTP client or any network call outside the sync transport.
- Never set `allowBackup` to true or add cloud backup rules.
- Never export a component without validating every intent extra.
- Never request camera permission before the user taps Scan.
- Never show task content in a widget or notification while app lock is on and the device is locked.
- Never store a secret outside `SecretStore`.

### Dependencies and process
- Never add a dependency that is paid, closed-source, requires an account, or is not listed in DEPENDENCIES.md.
- Never add a library to avoid writing 20 lines of code.
- Never make a change outside the declared session scope.
- Never mark a task done without the DONE.md checklist.
- Never refactor while building a feature.
- Never route around a blocked rule. Stop and ask.
- Never edit SCHEMA.md or a migration file without human review.
- Never write copy that sounds like marketing. Plain words. No exclamation marks in UI text.
