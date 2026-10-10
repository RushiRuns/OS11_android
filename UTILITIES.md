# OS11 Android - Utilities Registry

> Check here BEFORE writing any component, helper, or domain function.
> If it is not here, it does not exist yet. If it is here, use it; do not write a second one.
> Updated at the end of every session that creates something reusable (DONE.md section 8).

**Status: empty.** Nothing is built yet. The tables below show the shape and the planned entries from the phases doc. A planned entry is a promise of where something will live, not code that exists. Mark an entry **Built** only when it is merged and tested.

**Row format:** `Name | Location | One-line purpose | Phase | Status`

---

## Do NOT Create a Duplicate Of

Each of these exists exactly once. A second copy is a bug.

| Item | The one place |
|---|---|
| Writing to synced tables | `TrackedWriter` |
| Applying remote changes | `ApplyRemoteWriter` |
| Scheduling fields (`bucket`, `waiting_*`, `follow_up_*`) | `TaskSchedulingService` |
| Time source | `Clock` (injected) |
| ID generation, UUIDv5 | `IdGenerator` (injected) |
| Hybrid logical clock | `Hlc` in `:domain` |
| Field merge | `mergeField` in `:domain` |
| Fractional ordering | `FractionalIndex` in `:domain` |
| NLP parsing | `NlpParser` in `:domain` |
| Recurrence expansion | `RecurrenceEngine` in `:domain` |
| Tokens (colors, spacing, radius, type, motion) | `Tokens.kt` |
| Haptics | `OS11Haptic` helper |
| Reduced-motion check | `rememberReducedMotion()` |
| Reorderable list behavior | one shared `ReorderableTaskList` |
| Task row | `TaskRow` |
| Bottom sheet container | `OSSheet` |
| Undo and Snackbar bridge | `UndoManager` |
| Note HTML sanitizer | `NoteSanitizer` |
| Secret storage | `SecretStore` |
| Error mapping to user messages | `AppError.toMessage()` |

---

## Domain Functions (`:domain`)

| Name | Location | Purpose | Phase | Status |
|---|---|---|---|---|
| `Clock`, `SystemClock`, `TestClock` | `domain/time/Clock.kt` | Deterministic time abstractions for domain and tests | 3 | Built |
| `Hlc`, `DefaultHlcClock` | `domain/logic/sync/Hlc.kt` | Parse, format, compare, tick, receive hybrid clocks (<13 wall>-<4 counter>-<8 dev>) | 3 | Built |
| `mergeField`, `MergeResult` | `domain/logic/sync/Merge.kt` | Per-field last-write-wins by merge group with tombstone resurrection rules | 3 | Built |
| `MergeGroups` | `domain/logic/sync/MergeGroups.kt` | Field-to-group table from SYNC.md section 5 | 3 | Built |
| `Change`, `FieldWrite`, `Operation` | `domain/logic/sync/Change.kt` | Wire and outbox types, serializable via kotlinx.serialization | 3 | Built |
| `FractionalIndex` | `domain/logic/ordering/FractionalIndex.kt` | Fractional indexing `between`, `atStart`, `atEnd` with 53-bit mantissa precision safety | 3 | Built |
| `TaskValidation` | `domain/logic/validation/TaskValidation.kt` | Title, dates, bucket/due_date conflict, bounds, and priority rules | 3 | Built |
| `DependencyCycleCheck` | `domain/logic/validation/CycleChecks.kt` | Direct and transitive task dependency cycle detection (DFS) | 3 | Built |
| `ParentCycleCheck` | `domain/logic/validation/CycleChecks.kt` | Subtask nesting cycle detection (DFS) | 3 | Built |
| `NlpParser` | `domain/nlp/NlpParser.kt` | Quick Add text to structured fields | 7 | Planned |
| `RecurrenceEngine` | `domain/recurrence/RecurrenceEngine.kt` | Next occurrence, fixed and after-completion | 11 | Planned |
| `SchedulingTransitions` | `domain/scheduling/SchedulingTransitions.kt` | The 27-row bucket/waiting transition table | 11 | Planned |
| `DayPlanLayout` | `domain/dayplan/DayPlanLayout.kt` | Snap, clamp, lane assignment for overlapping blocks | 13 | Planned |
| `DeterministicIds` | `domain/id/IdGenerator.kt` | RFC 4122 UUIDv5 for default workspace/area and recurring occurrences | 3 | Built |
| `AppResult`, `AppError` | `domain/result/AppResult.kt` | Typed Result error model for all fallible operations | 4 | Built |

## Application Services (`:app`)

| Name | Location | Purpose | Phase | Status |
|---|---|---|---|---|
| `TrackedWriter` | `data/sync/TrackedWriter.kt` | Room transaction = data row + field clocks + outbox + history + hooks + cascade tombstones | 3 | Built |
| `DatabaseInitializer` | `data/db/DatabaseInitializer.kt` | Deterministic seed generator for default workspace, area, settings, modules | 3 | Built |
| `SeedGenerator` | `data/seed/SeedGenerator.kt` | Benchmark dataset generator for 1,000 and 5,000 tasks | 3 | Built |
| `ApplyRemoteWriter` | `data/sync/ApplyRemoteWriter.kt` | Remote changes through merge | S1 | Planned |
| `TaskService` | `service/task/TaskService.kt` | Task aggregate application use cases with undo tokens | 4 | Built |
| `ProjectService` | `service/project/ProjectService.kt` | Project lifecycle and section use cases | 4 | Built |
| `AreaService` | `service/area/AreaService.kt` | Area lifecycle use cases | 4 | Built |
| `TagService` | `service/tag/TagService.kt` | Tag management use cases | 4 | Built |
| `ReminderService` | `service/reminder/ReminderService.kt` | Reminder scheduling and alarm reconciliation | 4 | Built |
| `PomodoroService` | `service/pomodoro/PomodoroService.kt` | Pomodoro session tracking use cases | 4 | Built |
| `GoalService` | `service/goal/GoalService.kt` | Goal tracking use cases | 4 | Built |
| `SettingsService` | `service/settings/SettingsService.kt` | App preferences use cases | 4 | Built |
| `SearchService` | `service/search/SearchService.kt` | FTS5 task search use cases | 4 | Built |
| `ExportService` | `service/export/ExportService.kt` | JSON/CSV export orchestration | 4 | Built |
| `TaskSchedulingService` | `service/task/` | Sole writer of scheduling fields | 11 | Planned |
| `ReminderScheduler` | `service/reminder/` | Arms next due alarm; reconciles after writes | 11 | Planned |
| `UndoManager` | `service/undo/UndoManager.kt` | Service-level 100-entry undo stack and Snackbar bridge | 4 | Built |
| `ImportService` | `service/data/` | JSON/CSV import round trip | 17 | Planned |
| `BackupService` | `service/data/` | Daily ZIP backup | 17 | Planned |
| `NoteSanitizer` | `service/note/NoteSanitizer.kt` | Jsoup-based HTML sanitizer preserving TipTap attributes | 5 | Built |
| `TaskTreeFlattener` | `ui/features/task/TaskTreeFlattener.kt` | Tree flattener on Dispatchers.Default for LazyColumn | 5 | Built |
| `SecretStore` | `platform/security/SecretStore.kt` | Keystore-backed AES-256-GCM secret storage | 4 | Built |
| `DeferredStartupManager` | `platform/startup/DeferredStartupManager.kt` | Post-first-frame deferred background initializations | 4 | Built |
| `AlarmScheduler` | `platform/alarms/AlarmScheduler.kt` | Exact alarm manager wrapper for single next-alarm arming | 1 | Built |

## Repository Methods

Record query methods here as they are written, grouped by repository, so a new query is not written when one exists.

| Repository | Method | Returns | Phase | Status |
|---|---|---|---|---|
| `TaskRepository` | `observeById(id)` | `Flow<TaskEntity?>` | 3 | Built |
| `TaskRepository` | `getById(id)` | `TaskEntity?` | 3 | Built |
| `TaskRepository` | `observeAllSlim()` | `Flow<List<SlimTask>>` | 3 | Built |
| `TaskRepository` | `observeInboxSlim()` | `Flow<List<SlimTask>>` | 3 | Built |
| `TaskRepository` | `observeByProjectSlim(projectId)` | `Flow<List<SlimTask>>` | 3 | Built |
| `TaskRepository` | `observeByAreaSlim(areaId)` | `Flow<List<SlimTask>>` | 3 | Built |
| `TaskRepository` | `observeSubtasksSlim(parentId)` | `Flow<List<SlimTask>>` | 3 | Built |
| `TaskRepository` | `observeMyDaySlim(date)` | `Flow<List<SlimTask>>` | 3 | Built |
| `TaskRepository` | `observeTrashSlim()` | `Flow<List<SlimTask>>` | 3 | Built |
| `TaskRepository` | `observeActiveTaskCount()` | `Flow<Int>` | 3 | Built |
| `TaskRepository` | `saveTask(task)` | `TaskEntity` | 3 | Built |
| `TaskRepository` | `trashTask(id)` | `Unit` | 3 | Built |
| `TaskRepository` | `restoreTask(id)` | `Unit` | 3 | Built |
| `TaskRepository` | `deletePermanently(id)` | `Unit` | 3 | Built |
| `WorkspaceRepository` | `observeAll()`, `getById(id)`, `save(workspace)` | Workspace queries | 3 | Built |
| `AreaRepository` | `observeAll()`, `getById(id)`, `save(area)`, `delete(id)` | Area queries | 3 | Built |
| `ProjectRepository` | `observeAll()`, `observeByArea(areaId)`, `getById(id)`, `save(project)`, `delete(id)` | Project queries | 3 | Built |
| `SectionRepository` | `observeByProject(projectId)`, `getById(id)`, `save(section)`, `delete(id)` | Section queries | 3 | Built |
| `TagRepository` | `observeAll()`, `getById(id)`, `getByName(name)`, `observeTagsForTask(taskId)`, `getTagsForTasks(taskIds)`, `observeTagsForTasks(taskIds)`, `save(tag)`, `delete(id)`, `addTagToTask(taskId, tagId)`, `removeTagFromTask(taskId, tagId)` | Tag queries & batch loading | 3 | Built |
| `DependencyRepository` | `getDependenciesForTask(taskId)`, `getAllDependencyPairs()`, `addDependency(...)`, `removeDependency(...)` | Dependency queries | 3 | Built |
| `MilestoneRepository` | `observeByProject(projectId)`, `getById(id)`, `save(milestone)`, `delete(id)` | Milestone queries | 3 | Built |
| `ReminderRepository` | `observeByTask(taskId)`, `getById(id)`, `save(reminder)`, `delete(id)` | Reminder queries | 3 | Built |
| `AttachmentRepository` | `observeByTask(taskId)`, `getById(id)`, `save(attachment)`, `delete(id)` | Attachment queries | 3 | Built |
| `CommentRepository` | `observeByTask(taskId)`, `save(comment)` | Comment queries | 3 | Built |
| `PomodoroRepository` | `observeAll()`, `observeByTask(taskId)`, `save(session)` | Pomodoro queries | 3 | Built |
| `GoalRepository` | `observeAll()`, `getById(id)`, `save(goal)`, `delete(id)` | Goal queries | 3 | Built |
| `SettingsRepository` | `observeAll()`, `getByKey(key)`, `set(key, value)`, `delete(key)` | Setting queries | 3 | Built |
| `ModuleRepository` | `observeAll()`, `observeEnabled()`, `setModuleEnabled(id, enabled)` | Module queries | 3 | Built |
| `NotificationRepository`| `observeRecent()`, `recordNotification(history)` | Notification queries | 3 | Built |
| `HistoryRepository` | `observeByTask(taskId)`, `purgeOlderThan(cutoffIso)` | Task history queries | 3 | Built |
| `SearchRepository` | `searchTasks(query, limit)` | FTS5 ranked tasks | 3 | Built |
| `CountsRepository` | `observeBrowseCounts(today)` | Single-query browse badge counts | 3 | Built |

## Shared Composables (`ui/components/`)

| Name | Purpose | Phase | Status |
|---|---|---|---|
| `OSButton`, `OSIconButton` | Buttons with 48dp hit area | 2 | Built |
| `OSCheckbox` | 20dp visual, 48dp target, spring, strikethrough, haptic | 2 | Built |
| `OSTextField`, `OSSearchField` | Text input | 2 | Built |
| `OSSheet` | Modal bottom sheet container | 2 | Built |
| `OSGlassMenu` | Translucent menu surface | 2 | Built |
| `OSChip`, `OSTagDot`, `OSPriorityBar` | Tags and priority | 2 | Built |
| `OSSnackbarHost`, `OSSnackbar` | Snackbar with Undo | 2 | Built |
| `OSEmptyState` | Empty state | 2 | Built |
| `OSProgressRing`, `OSProgressBar` | Progress indicators via Canvas | 2 | Built |
| `OSDatePickerSheet`, `OSTimeList` | Date and time selection | 2 | Built |
| `OSSwitch`, `OSSegmented` | Settings and view controls | 2 | Built |
| `OS11AppShell` | Edge-to-edge shell, top bar, bottom bar, FAB | 2 | Built |
| `ComponentGalleryScreen` | Visual component audit harness | 2 | Built |
| `TaskRow` | The one task row | 5 | Built |
| `TaskList` | LazyColumn with stable keys and content types | 5 | Built |
| `TaskDetailSheet` | Progressive disclosure task detail modal | 5 | Built |
| `TaskActionsSheet` | Long-press action set | 5 | Built |
| `TaskSortFilterSheet` | Task sort and filter bottom sheet | 5 | Built |
| `NoteEditor` | Rich text markdown/HTML note editor | 5 | Built |
| `ReorderableTaskList` | Shared reorder behavior | 8 | Planned |
| `WeekStrip` | Collapsible week/month calendar | 6 | Planned |
| `ParsePreviewChips` | Quick Add parse preview | 7 | Planned |

## Modifiers and Compose Helpers

| Name | Purpose | Phase | Status |
|---|---|---|---|
| `Modifier.os11MainPaneBackground` | Main pane surface | 2 | Built (compiled in `Tokens.kt`) |
| `rememberReducedMotion()` | Reads the system animation scale | 2 | Built (in `ui/util/MotionUtil.kt`) |
| `OS11AnimationSpecs` | The 4 permitted animation sites and shared specs | 2 | Built (in `ui/theme/Motion.kt`) |

## Platform Helpers (`platform/`)

| Name | Purpose | Phase | Status |
|---|---|---|---|
| `TrashPurgeWorker` | Daily 30-day trash purge background worker | 5 | Built |
| `AlarmGateway` | Wraps `AlarmManager` for testability | 11 | Planned |
| `NotificationChannels` | Channel definitions | 9 | Planned |
| `HapticsGateway` | Wraps `Vibrator`/`HapticFeedback` | 2 | Planned |
| `FilePicker` | SAF contracts | 15 | Planned |

---

## Rules for this file

1. Add the entry in the same commit as the code.
2. Entries describe what exists, not what is hoped for. Planned items stay marked Planned.
3. When something is renamed or moved, fix its row in the same commit.
4. If two entries do nearly the same thing, merge them and update every caller. That is a refactor session.
