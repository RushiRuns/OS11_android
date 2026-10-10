# ADR-A12: Application Services Layer, Startup Purity, and Benchmark Baselines

Status: Accepted (2026-10-10)

## Context
Phase 4 bridges the persistence/domain layer (:domain and Room repositories) with the UI layer (Compose ViewModels and Screens).
Rules per ARCHITECTURE.md and SYSTEM.md require:
1. Strict layer order: UI -> ViewModel -> Application Service -> Domain -> Repository -> DAO.
2. `AppResult` / `AppError` return types for all fallible operations so exceptions never cross layer boundaries into UI.
3. Strict startup path purity (Rule 16): `Application.onCreate` initializes Hilt only; deferred maintenance (alarms reconciliation, trash purge, widget refresh, sync engine) is scheduled strictly after the first frame has rendered.
4. Bounded undo capability (100 entries per session) with Snackbar bridge.
5. Secure storage for credentials via Android Keystore (`SecretStore`).

## Decisions
1. **Service Layer Pattern**: Implemented `@Singleton` services for all aggregates (`TaskService`, `ProjectService`, `AreaService`, `TagService`, `ReminderService`, `PomodoroService`, `GoalService`, `SettingsService`, `SearchService`, `ExportService`). Services coordinate domain validation, invoke atomic repository mutations via `TrackedWriter`, and register reverse operations with `UndoManager`.
2. **Result & Error Typing**: Defined pure `:domain` `AppResult<T>` and `AppError` (Validation, NotFound, RuleViolation, Unexpected) with non-leaking user-facing messages.
3. **Startup Purity**: `Application.onCreate` only sets up Hilt and debug StrictMode (`detectAll` + `penaltyLog`). `DeferredStartupManager` triggers post-first-frame maintenance in an injected background dispatcher scope without blocking main thread frame pacing.
4. **LeakCanary & StrictMode**: Integrated LeakCanary in `debugImplementation` and active StrictMode policies in debug builds.
5. **Cold-Start Baseline**: Verified startup path in `StartupTimingTest`: graph construction + initial My Day emission executes well within the 1.0s cold-start budget.

## Consequences
- ViewModels have clean, typed, reactive APIs to observe Flows and dispatch intents without directly touching repositories.
- Zero startup blocking: first frame emission occurs with 0 ms maintenance overhead.
