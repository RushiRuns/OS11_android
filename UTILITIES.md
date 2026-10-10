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
| `Hlc` | `domain/sync/Hlc.kt` | Parse, format, compare, tick, receive hybrid clocks | 3 | Planned |
| `mergeField` | `domain/sync/Merge.kt` | Per-field last-write-wins by merge group | 3 | Planned |
| `MergeGroups` | `domain/sync/MergeGroups.kt` | Field-to-group table from SYNC.md | 3 | Planned |
| `Change`, `FieldWrite` | `domain/sync/Change.kt` | Wire and outbox types, serializable | 3 | Planned |
| `FractionalIndex` | `domain/ordering/FractionalIndex.kt` | `between`, `atStart`, `atEnd` | 3 | Planned |
| `TaskValidation` | `domain/task/TaskValidation.kt` | Title, dates, estimate rules | 3 | Planned |
| `DependencyCheck` | `domain/task/DependencyCheck.kt` | Direct and transitive cycle detection | 3 | Planned |
| `ParentCycleCheck` | `domain/task/ParentCycleCheck.kt` | Subtask nesting cycle detection | 3 | Planned |
| `NlpParser` | `domain/nlp/NlpParser.kt` | Quick Add text to structured fields | 7 | Planned |
| `RecurrenceEngine` | `domain/recurrence/RecurrenceEngine.kt` | Next occurrence, fixed and after-completion | 11 | Planned |
| `SchedulingTransitions` | `domain/scheduling/SchedulingTransitions.kt` | The 27-row bucket/waiting transition table | 11 | Planned |
| `DayPlanLayout` | `domain/dayplan/DayPlanLayout.kt` | Snap, clamp, lane assignment for overlapping blocks | 13 | Planned |
| `DeterministicIds` | `domain/id/DeterministicIds.kt` | UUIDv5 for default workspace/area and occurrences | 3 | Planned |

## Application Services (`:app`)

| Name | Location | Purpose | Phase | Status |
|---|---|---|---|---|
| `TrackedWriter` | `data/sync/TrackedWriter.kt` | Data + field clocks + outbox in one transaction | 3 | Planned |
| `ApplyRemoteWriter` | `data/sync/ApplyRemoteWriter.kt` | Remote changes through merge | S1 | Planned |
| `TaskService` | `service/task/` | Task use cases | 4-5 | Planned |
| `TaskSchedulingService` | `service/task/` | Sole writer of scheduling fields | 11 | Planned |
| `ReminderScheduler` | `service/reminder/` | Arms next due alarm; reconciles after writes | 11 | Planned |
| `UndoManager` | `service/undo/` | Undo stack and Snackbar bridge | 4 | Planned |
| `ExportService`, `ImportService` | `service/data/` | JSON/CSV round trip | 17 | Planned |
| `BackupService` | `service/data/` | Daily ZIP backup | 17 | Planned |
| `NoteSanitizer` | `service/note/` | Jsoup-based HTML sanitizer | 5 | Planned |
| `SecretStore` | `platform/security/` | Keystore-backed secret storage | 4 | Planned |

## Repository Methods

Record query methods here as they are written, grouped by repository, so a new query is not written when one exists.

| Repository | Method | Returns | Phase | Status |
|---|---|---|---|---|
| (none yet) | | | | |

## Shared Composables (`ui/components/`)

| Name | Purpose | Phase | Status |
|---|---|---|---|
| `OSButton`, `OSIconButton` | Buttons with 48dp hit area | 2 | Planned |
| `OSCheckbox` | 20dp visual, 48dp target, spring, strikethrough, haptic | 2 | Planned |
| `OSTextField`, `OSSearchField` | Text input | 2 | Planned |
| `OSSheet` | Modal bottom sheet container | 2 | Planned |
| `OSGlassMenu` | Translucent menu surface | 2 | Planned |
| `OSChip`, `OSTagDot`, `OSPriorityBar` | Tags and priority | 2 | Planned |
| `OSSnackbarHost` | Snackbar with Undo | 2 | Planned |
| `OSEmptyState` | Empty state | 2 | Planned |
| `OSProgressRing`, `OSProgressBar` | Progress | 2 | Planned |
| `OSDatePickerSheet`, `OSTimeList` | Date and time selection | 2 | Planned |
| `OSSwitch`, `OSSegmented` | Settings controls | 2 | Planned |
| `TaskRow` | The one task row | 5 | Planned |
| `TaskActionsSheet` | Long-press action set | 5 | Planned |
| `ReorderableTaskList` | Shared reorder behavior | 8 | Planned |
| `WeekStrip` | Collapsible week/month calendar | 6 | Planned |
| `ParsePreviewChips` | Quick Add parse preview | 7 | Planned |

## Modifiers and Compose Helpers

| Name | Purpose | Phase | Status |
|---|---|---|---|
| `Modifier.os11MainPaneBackground` | Main pane surface | 2 | Built (uncompiled, in `Tokens.kt`) |
| `Modifier.minTouchTarget` | Enforces 48dp hit area | 2 | Planned |
| `rememberReducedMotion()` | Reads the system animation scale | 2 | Planned |
| `Modifier.pressScale` / `springIn` / `critPulse` | The permitted animation sites | 2 | Planned |

## Platform Helpers (`platform/`)

| Name | Purpose | Phase | Status |
|---|---|---|---|
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
