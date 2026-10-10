# OS11 Android - Definition of Done

> Consulted before closing any feature, and walked line by line at the end of every session.
> A feature with an unticked line is not done. It is "in progress", and it is reported that way.
> "Looks right on the emulator" is not a line on this list.

---

## 1. Behavior

- [ ] Happy path works on the **reference device**, not only the emulator
- [ ] Empty state exists, uses `OSEmptyState`, and is correct
- [ ] Error state exists and is reachable (force it once to prove it)
- [ ] Loading state exists where data is not instant, and does not flash for fast loads
- [ ] Every fallible service call returns `AppResult`; no thrown exception reaches the UI
- [ ] Reversible actions have **no confirmation dialog**, only a Snackbar Undo
- [ ] Irreversible actions (permanent delete, unpair, wipe) have an explicit confirmation naming what is lost
- [ ] State survives rotation, process death, and a font scale change (the three common Android state-loss events)
- [ ] Back gesture does the expected thing (closes sheet, then screen; never exits mid-edit with data lost)
- [ ] No feature depends on a gesture alone; the visible alternative exists

## 2. Architecture

- [ ] Layer order respected: UI -> ViewModel -> service -> domain -> repository -> DAO
- [ ] No business logic in a composable or ViewModel (validation, computation, transformation live in a service or `:domain`)
- [ ] ViewModel reads service Flows and never holds a repository
- [ ] Every write to a synced table goes through `TrackedWriter`
- [ ] If the feature adds or changes a synced field: it has a merge group in SYNC.md and a row in the SCHEMA.md scope legend
- [ ] If the feature writes `bucket`, `waiting_*` or `follow_up_*`: it goes through `TaskSchedulingService` only
- [ ] No `Clock`, `now()` or randomness read in `:domain`; time and IDs are injected
- [ ] Side effects (alarms, widgets, notifications) are scheduled **after** commit, never inside the transaction
- [ ] No network code outside the sync transport
- [ ] Architecture test suite (Konsist) passes
- [ ] ARCHITECTURE.md and SYSTEM.md rules not violated. If one had to be bent, the session stopped and said so

## 3. UI and Design

- [ ] No hardcoded color, dp, sp, radius, elevation, or duration; tokens only
- [ ] No `MaterialTheme.*` read in feature code
- [ ] Every interactive element has a 48dp touch target and a content description or role
- [ ] Layout holds at 200% font scale with no clipped or overlapping text
- [ ] Insets handled once at the shell; the screen does not add its own system-bar padding
- [ ] Reduced motion handled for every animation; any new animation site has an ADR
- [ ] Spring/overshoot motion only at the permitted sites
- [ ] Metadata that is empty renders nothing (no "No date", no empty chips)
- [ ] Priority is a left border only; star uses the star token
- [ ] TalkBack: the screen reads in a sensible order and every gesture has a custom action
- [ ] Checked against the component gallery; no one-off look

## 4. Performance

- [ ] Any list that can grow without bound is a `LazyColumn` with stable `key` and `contentType`, or paged
- [ ] List projections do not load `notes` or other large columns
- [ ] No N+1 queries
- [ ] No database write during drag or scroll; one tracked write on drop
- [ ] Scroll-driven and animation-driven state is read late (`graphicsLayer`, `offset {}`, `drawBehind`), not in composition
- [ ] Nothing new runs in `Application.onCreate` or before the first frame
- [ ] No background loop or per-second ticking when the screen is off
- [ ] If a list, startup path or write path was touched: measured on a **release-like build** against PERFORMANCE.md; number recorded in the changelog

## 5. Data and Sync Readiness

- [ ] Schema change has a Room migration, a migration test, and a SCHEMA.md update in the same commit
- [ ] New table or column has a sync scope (synced / local / derived / meta) stated
- [ ] Deletes of synced entities write tombstones, with cascade tombstones where the entity has children
- [ ] Per-device state is not placed in a synced table
- [ ] Imports and bulk operations go through the tracked write path; no bypass

## 6. Security and Privacy

- [ ] No new permission. If one is truly needed, it is justified in ARCHITECTURE.md first
- [ ] No secret, token, or key in code, logs, or DataStore; secrets use `SecretStore`
- [ ] Notes HTML is sanitized before storage
- [ ] Logs contain no task titles, notes, or personal data in release builds
- [ ] No new dependency without a DEPENDENCIES.md entry and review

## 7. Tests

- [ ] At least one test written for the feature
- [ ] Domain logic: unit tests, including the edge and empty cases
- [ ] Anything covered by shared vectors (NLP, recurrence, scheduling, merge): the vectors pass, and any new case was added to the shared file
- [ ] Anything touching a migration: migration test passes
- [ ] Anything touching sync write paths: convergence test still passes
- [ ] The full `./gradlew check` is green

## 8. Documentation and Hygiene

- [ ] UTILITIES.md updated if anything reusable was created
- [ ] Existing utilities checked **before** writing new ones (no duplicate of anything in UTILITIES.md)
- [ ] ANDROID_VS_DESKTOP.md updated if the feature differs from desktop
- [ ] CHANGELOG_INTERNAL.md entry added
- [ ] New pattern or library has an ADR
- [ ] No dead code, no commented-out blocks, no TODO without an owner and a reason
- [ ] Scope respected: no files touched outside the session's declared scope
- [ ] Committed with the format `Phase N: <feature> - <one line>`

---

## When a line cannot be ticked

Say which line, why, and what it blocks. Do not tick it and do not delete it. A feature can ship with a documented exception that **you** accepted; it cannot ship with a silent one.
