# OS11 Android - Performance

> Performance is a precondition, not a feature.
> These targets are rejection criteria. A change that breaks one is redesigned before it ships.

**Status of the numbers.** The targets below are my proposals, scaled from the desktop targets I could see (warm start, search under 150 ms, 60 fps interactions, RAM under 200 MB). I have no measurements from your phone. Phase 0 sets the **reference device** (your own phone) and records the baseline. After the first measurement, targets become ratchets: they can tighten, never loosen.

I did not have the desktop PERFORMANCE.md, only references to it. If it states targets that conflict with these, desktop wins on shared concepts (search time) and this file wins on Android-only ones (startup, jank, battery).

---

## Targets

| Metric | Target | How measured |
|---|---|---|
| Cold start to first task visible | < 1.0 s | Macrobenchmark `StartupTimingMetric` (release-like build, reference device) |
| Warm start (process alive) to interactive | < 300 ms | Macrobenchmark |
| Scroll, 1,000-task list | No dropped frames at the device refresh rate, P90 frame time within budget | Macrobenchmark `FrameTimingMetric` |
| Tap to visible response | < 100 ms | Manual + trace |
| Single tracked write (data + clocks + outbox), P95 | < 20 ms | Instrumented timing test |
| Search, 5,000 tasks | < 150 ms keypress to results | Instrumented timing test |
| NLP parse per keystroke, P95 | < 30 ms | JVM benchmark + trace |
| Idle memory | < 150 MB | Android Studio profiler, release build |
| Memory with 5,000 tasks loaded and scrolled | < 200 MB | Profiler |
| Release install size, per-ABI split | < 25 MB | Bundletool |
| Wakeups outside alarms and WorkManager | 0 | `dumpsys batterystats`, 24 h idle |
| Sync: apply 1,000 incoming changes | < 2 s | Instrumented test |
| Sync: first full sync, 5,000 tasks over LAN | < 15 s | Real device test |

Performance is judged on a **release-like build** (R8 on, baseline profile installed). A debug build of a Compose app is slow by design and is never used to judge speed.

---

## Measured Baselines

### Phase 4: Startup & Shell Baseline (Unit / JVM Timing Harness)

Recorded via `StartupTimingTest.kt` verifying Architecture Rule 16 startup budget constraints:

| Metric / Path | Budget | Measured Baseline | Status |
|---|---|---|---|
| Startup graph assembly (Services, Repositories, DB) | < 150 ms | ~35 ms | PASS |
| First My Day list emission (cold-start flow) | < 500 ms | ~110 ms | PASS |
| `Application.onCreate` I/O operations | 0 operations | 0 (pure Hilt + StrictMode) | PASS |
| Post-first-frame deferred init latency | Non-blocking | Asynchronous on `Dispatchers.IO` | PASS |

*Note: Macrobenchmark device run on physical reference device will be conducted in release configuration once UI screens are wired in Phase 6.*

---

## Mitigation Strategies

### Startup

**1. Startup path purity.** `Application.onCreate` initializes Hilt and nothing else. Sync, alarms, widgets, backup, trash purge, and attachment work start after the first frame. (ARCHITECTURE.md rule 16.)

**2. Splash handoff.** AndroidX SplashScreen holds until My Day has its first emission, with a hard cap, so the user never sees an empty list flash.

**3. Baseline Profile and Startup Profile** generated for the launch to My Day path and for scrolling the main list. Regenerated whenever the launch path changes. Without it, first run after install is noticeably slower.

**4. R8 full mode and resource shrinking** in release. No unused icons, no unused font weights.

**5. Lazy Hilt.** Heavy singletons (`SyncEngine`, `BackupService`, `AttachmentService`) are injected as `Lazy<T>` or `Provider<T>` where used, never eagerly in the startup graph.

### Database

**6. WAL, `synchronous = NORMAL`, sensible cache size**, set through the bundled driver's connection setup.

**7. Slim list projections.** List queries select only the columns a row shows. They never load `notes` (a potentially large HTML string). The detail sheet loads the full row.

**8. Partial indexes** that match the hot queries. The Inbox, bucket, and waiting indexes in SCHEMA.md exist so those smart lists never scan the task table.

**9. Counts are `COUNT` queries.** Badge counts never load rows. One query for all Browse badges, not one per row.

**10. No N+1.** Tags for the visible window of tasks come from one batched query (`IN` over visible IDs) or one join, not one query per row.

**11. Paging 3 for unbounded lists:** All Tasks, Completed, Trash, search results. Bounded smart lists (My Day, Inbox, Important) load directly.

**12. Tracked write overhead stays small.** Field clock upserts for one write go in one multi-row statement. The outbox payload carries only changed groups.

**13. Sync applies in batches.** One transaction per incoming batch. Room invalidation fires once per table at commit, so a 200-change batch causes one UI emission per affected table, not 200.

### UI

**14. Lazy lists done properly:** stable `key`, `contentType`, immutable UI models, no unstable `List` parameters. Subtask tree flattening runs on `Default`, not on the main thread.

**15. Read state late.** Scroll-driven and animation-driven values are read in `graphicsLayer { }`, `offset { }`, or `drawBehind { }`, not in composition. Verified with recomposition counts in Layout Inspector.

**16. Drag without database writes.** During a drag, only transient visual state changes. Exactly one tracked write happens on drop.

**17. Debounced, cancellable parsing.** Quick-add parsing is debounced (about 120 ms), runs off the main thread, and cancels the previous parse when a new keystroke arrives.

**18. FTS5 tuned for search-as-you-type:** prefix indexes on the title column, result limit of 50, snippets only for visible rows, query on `IO`.

**19. Images:** attachment thumbnails generated once on `IO`, stored small, loaded with Coil at the displayed size. No full-size bitmap in a list.

### Platform

**20. One alarm, not hundreds.** Android limits concurrent alarms per app. Reminders are scheduled by arming **only the next due reminder**. When it fires, all due reminders fire and the next one is armed. Reconciliation is one query.

**21. Widget updates are throttled** and read a slim read-only query. A burst of writes causes one widget refresh.

**22. Sync never holds the device awake.** No foreground service, no wake lock. Short sessions only (SYNC.md section 10).

**23. Timers use timestamps.** No per-second coroutine ticking a counter in the background. The UI computes remaining time from the stored start when visible; the end is an alarm.

---

## Debug-Only Tools

- **Seed generator** (debug build only): creates 1,000 or 5,000 tasks across areas, projects, tags, subtasks, and reminders, so every performance check runs against realistic data. Never in release.
- **StrictMode** (`detectAll`, `penaltyLog`) enabled in debug. Any violation is fixed in the session that caused it.
- **LeakCanary** in debug only.

---

## Pre-Ship Checklist

Run on the reference device, release-like build, before every release.

- [ ] Cold start under target (Macrobenchmark, 10 iterations, median and worst)
- [ ] Warm start under target
- [ ] Scroll benchmark on the 5,000-task seed: no regression versus the stored baseline
- [ ] Search under 150 ms on the 5,000-task seed
- [ ] Tracked write P95 under 20 ms
- [ ] Memory under target idle and with 5,000 tasks
- [ ] Release size under target
- [ ] 24 h idle battery: no unexpected wakeups
- [ ] Baseline Profile regenerated and verified present in the release build
- [ ] StrictMode clean in a full manual walkthrough
- [ ] No ANR or crash in a 30 minute monkey-style session
- [ ] Sync benchmark numbers hold (once sync is live)
- [ ] Dependency vulnerability scan clean
- [ ] Every new `Dependency` since the last release has a DEPENDENCIES.md entry
