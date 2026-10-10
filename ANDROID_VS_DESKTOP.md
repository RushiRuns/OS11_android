# OS11 Android vs Desktop - Parity Map

> Same product, same design language, same data. Different hands.
> Every desktop feature has a status here. If a feature is not in this file, it is not built until it is added here.

## Status Legend

| Status | Meaning |
|---|---|
| **Port** | Same feature, same behavior |
| **Adapt** | Same feature, different way to reach or operate it |
| **Replace** | Desktop mechanism has no phone equivalent. A different mechanism does the same job |
| **Skip** | Only makes sense on desktop. Not built |
| **Defer** | Wanted, but after v1.0.0 |

## Principles

1. **Data parity is absolute.** Same hierarchy, same fields, same rules. A task means the same thing on both sides.
2. **Behavior parity where the interaction survives.** Natural-language quick-add, smart lists, GTD buckets, recurrence, and reminders behave identically.
3. **Interaction parity is not a goal.** Hover, keyboard shortcuts, right-click, windows, and the tray do not exist on a phone. The job each one did is kept; the mechanism is not.
4. **Every gesture has a visible alternative.** Swipe and long-press are shortcuts, never the only path.
5. **Feel rules carry over.** Empty fields render nothing. Reversible actions have no confirmation, only Undo. Detail is one tap deeper, not on the default view.
6. **Shared logic is shared by test vectors, not code.** NLP, recurrence, scheduling transitions, and merge behavior are tested from the same JSON files on both platforms.

---

## 1. Shell and Navigation

| Desktop | Android | Status |
|---|---|---|
| Sidebar: Smart Lists, Areas, Projects | Bottom bar (My Day, Inbox, Planned, Browse). Browse holds the Area and Project tree plus remaining smart lists | Adapt |
| Sidebar profile block + glass dropdown (Dashboard, Goals, Pomodoro, Settings) | Avatar in the Browse top bar opens a glass sheet with the same four rows | Adapt |
| Core four smart lists always on (My Day, Important, Planned, Inbox) | My Day, Inbox, Planned in the bottom bar. Important in Browse, pinned at the top | Adapt |
| Optional smart lists (All Tasks, Completed), hidden by default | Same toggles in Settings, shown in Browse | Port |
| Anytime / Someday / Waiting For modules | Same modules, same toggles, shown in Browse when on | Port |
| Area "Tasks" item at top of each Area | Same: first row inside an Area in Browse | Port |
| Area Overview (loose tasks with inline add, project cards below) | Same layout in a scrolling screen. Quick Add is the FAB | Adapt |
| Detail panel on the right | Full-height bottom sheet over the current screen | Adapt |
| Resizable panes, sidebar position, density toggle in layout | Skip. Density stays as a setting | Skip |
| Custom titlebar, window controls, always-on-top, opacity | Skip | Skip |
| Pomodoro pill in the titlebar | Slim pill under the status bar while a session runs, tap opens Pomodoro | Replace |
| Splash screen, warm start, hide-on-close | Android SplashScreen API. Process lifecycle replaces warm start | Replace |
| Auto-updater | Skip. Updates arrive by whatever channel the APK is installed from | Skip |
| Launch at login | Skip | Skip |

---

## 2. Input Mapping

| Desktop | Android |
|---|---|
| Hover reveals layer 2 (date, tags, subtask count, actions) | No hover. The row shows a second line **only if** there is something to show. Actions live in swipe and long-press |
| Click row | Tap opens the detail sheet |
| Right-click context menu | Long-press opens an actions sheet with the same action set |
| Checkbox click | Tap checkbox. Spring + strikethrough + light haptic |
| Swipe | **Swipe right: complete. Swipe left: When sheet** (today, tomorrow, anytime, someday, pick a date). No swipe deletes |
| Keyboard shortcuts (all) | Skip. See section 3 for what replaces them |
| Vim mode | Skip |
| Drag to reorder | Port (long-press then drag) |
| Drag onto a task to nest | Adapt. Primary path: **Indent / Outdent** actions in the actions sheet and detail sheet. Drag with a horizontal offset is an enhancement, not the only way |
| Drag task to sidebar list | Replace: **Move to** picker in the actions sheet |
| Drag task to My Day in sidebar | Replace: swipe-left When sheet, or actions sheet |
| Drag file onto a task | Replace: Attach button in the detail sheet (system file picker) and the share target |
| Multi-select (hover checkbox, Ctrl/Shift click, Ctrl+A) | Adapt: long-press a row to enter selection mode, tap to add, "Select all" in the selection bar |
| Bulk action bar | Port as a bottom action bar in selection mode |
| Clipboard paste image | Skip in v1 |

### Replacements for keyboard-only things

| Desktop shortcut | Android mechanism |
|---|---|
| `Ctrl+N`, global quick-add | FAB, home-screen widget, Quick Settings tile, share target, app shortcut |
| `Ctrl+K` command palette, omnibar | Search screen with an Actions group (start Pomodoro, open Settings, jump to an Area) |
| `Ctrl+F` search | Search icon in the top bar |
| `Ctrl+Z` / `Ctrl+Y` | Snackbar Undo after each reversible action |
| `Ctrl+D`, `Ctrl+T`, `Ctrl+Shift+M` pickers | Rows in the actions sheet |
| `Ctrl+Shift+F` focus mode | Focus button in the detail sheet |
| `Ctrl+Shift+L` theme toggle | Skip (dark only) |
| Tab / Shift+Tab subtasks | Indent / Outdent |

---

## 3. Feature Map by Desktop Phase

### Core tasks (desktop Phase 5)

| Desktop feature | Status | Android note |
|---|---|---|
| Task row: checkbox + title; metadata on hover | Adapt | Metadata line shown only when present |
| Priority as 2.5 left border, critical pulses | Port | 2.5dp, alpha pulse, reduced-motion aware |
| Tags as dots on the row, chips in detail | Port | Shape variants keep it color-blind safe |
| Pomodoro count, attachment count | Port | Show only when greater than zero |
| Virtual scrolling | Port | LazyColumn with keys, paging for unbounded lists |
| Detail panel sections collapsed by default, empty sections render nothing | Port | Same rule in the sheet |
| Notes editor (TipTap, HTML) | Adapt | Spike and risk, see phases doc. Stored as sanitized HTML for parity |
| Quick add three entry points | Adapt | FAB, widget, share, tile, voice |
| Complete, recurring generates next | Port | Deterministic occurrence ID for sync |
| Trash with undo, 30-day purge | Port | Snackbar Undo; WorkManager purge |
| Inline title edit in list | Adapt | Edit title in the detail sheet |
| Star, duplicate | Port | Actions sheet |
| Subtasks, count "2/5" | Port | |
| Global undo/redo stack | Adapt | Undo only, via Snackbar, 100 entries |
| Sort and filter header with subtle icon | Adapt | Icon in top bar opens a sheet |

### Lists, smart lists, My Day (desktop Phase 6)

| Desktop feature | Status |
|---|---|
| Smart list queries | Port |
| Per-list backgrounds, background blur, animated backgrounds | Skip |
| List groups and list context menu | Replaced by Areas and Projects (new hierarchy) |
| My Day, suggestions panel, midnight rollover prompt | Port. Suggestions are a collapsible section at the bottom of My Day. Rollover is a bottom sheet on first open of the day |
| Planned: grouped timeline plus persistent mini-calendar | Adapt: collapsible week strip (expands to month) above tasks grouped by date |

### Quick add and search (desktop Phase 7)

| Desktop feature | Status | Note |
|---|---|---|
| NLP parser (dates, `!priority`, tags, recurrence, pomodoro marker) | Port | Own Kotlin parser, shared vectors |
| `@Area`, `/Project`, `#tag` syntax and context-aware defaults | Port | See section 4 |
| Live parse preview chip | Port | Chip row above the keyboard |
| Omnibar window, command palette | Replace | Search screen with Actions group |
| FTS5 search, results with snippet | Port | |
| Keyboard shortcuts, Vim mode | Skip | |

### Drag, menus, selection (desktop Phase 8)

Covered in section 2. Inline date picker: **Adapt** to a bottom sheet with quick options (Today, Tomorrow, Next Week, No Date), a calendar grid, a 15-minute time list, and a text field that accepts natural language.

### Tags, priority, notifications (desktop Phase 9)

| Desktop feature | Status | Note |
|---|---|---|
| Tag picker, nested tags, create inline | Port | Sheet |
| Tag view | Port | Under Browse |
| Tag manager, merge | Port | Settings |
| Auto-tag rules | Defer | |
| In-app notification center | Adapt | Screen reached from the Browse top bar. System notifications are the primary channel on Android |

### Projects (desktop Phase 10)

| Desktop feature | Status | Note |
|---|---|---|
| List view with sections | Port | |
| Board view | Adapt | Columns are sections. One column per page with a peek of the next, horizontal pager |
| Timeline view | Adapt | Scroll and zoom to read. Drag-to-reschedule is deferred. Dependencies and milestones shown |
| Calendar view | Adapt | Reuses the Planned calendar component |
| Table view | Skip | Renders as List. The stored `default_view = 'table'` is preserved, never overwritten |
| View switcher | Port | Chip row |
| Dependencies, milestones, progress ring | Port | |
| Activity feed, templates | Defer | |
| Project export | Adapt | CSV through the system share sheet. PDF and Markdown deferred |

### Scheduling, GTD, reminders (desktop Phase 11 and ADR-0010)

| Desktop feature | Status | Note |
|---|---|---|
| Recurrence picker, fixed vs after-completion | Port | |
| Reminders, multiple per task | Adapt | AlarmManager, next-reminder-only arming |
| Snooze actions on notifications | Port | Complete, Snooze 15m, 1h, tomorrow morning |
| Bucket / Waiting For / follow-up, 27-row transition table | Port | `TaskSchedulingService` single writer, shared vectors |
| Triage keys | Skip | Inbox rows use the When sheet; a full-screen triage mode is deferred |
| Calendar integration module | Skip | Not in OS11's scope (cloud) |

### Pomodoro (desktop Phase 12)

| Desktop feature | Status |
|---|---|
| Pomodoro view with ring, linked task, controls, session dots | Port |
| Session records, long break after N sessions | Port |
| Floating mini window | Replace with ongoing notification (chronometer) and the in-app pill |
| Tray countdown | Replace with the same notification |
| Distraction blocker (system DND) | Skip |
| Sound alert picker | Defer. Default notification sound and haptic |

Timer is derived from a stored start timestamp. The end is an exact alarm. A running timer is **not** synced live.

### Day Plan, Goals (desktop Phase 13, changed)

| Desktop feature | Status | Note |
|---|---|---|
| Daily Agenda (side panel on My Day, drag tasks into time blocks) | **Adapt as a separate page** | Spec in section 5 |
| Weekly Agenda | Already dropped on desktop | Not built |
| Goals with progress and streaks | Port | |
| Weekly review prompt | Defer | |
| Habit tracker module | Defer | |

### Dashboard (desktop Phase 14)

| Desktop feature | Status |
|---|---|
| Four stat cards | Port |
| Completion bar, distribution donut, trend, activity heatmap | Adapt: stacked vertically, date range chips |
| Project burndown | Defer |
| PDF export | Skip. CSV through share sheet |

### Attachments (desktop Phase 15)

| Desktop feature | Status | Note |
|---|---|---|
| File picker upload | Port | System file picker (SAF) |
| Drag file, clipboard paste | Skip | |
| Link attachments | Port | |
| Attachment strip, thumbnails, open | Adapt | Opens with an intent chooser |
| Attachment filename search | Defer | |

### Settings, modules, privacy (desktop Phase 16)

| Desktop feature | Status |
|---|---|
| Theme engine light/dark/auto, custom backgrounds | Skip (dark only) |
| Accent color | Port |
| Font size, density | Port |
| Module toggles and profile presets | Port |
| App lock | Adapt: biometric with PIN fallback, optional screen-capture protection |
| Notification settings (quiet hours, categories) | Adapt to channels |

### Data (desktop Phase 17)

| Desktop feature | Status | Note |
|---|---|---|
| JSON export | Port | Through the system file picker |
| CSV export | Port | |
| Markdown export | Defer | |
| Import OS11 JSON | Port | Lets the phone start from a desktop export before sync |
| Import Todoist / MS To Do / Notion | Skip | Import on desktop, then sync |
| Daily auto-backup | Adapt | WorkManager writing a ZIP to a folder the user picked |
| Version history | Defer | Table exists, no UI |

### Onboarding, accessibility, polish (desktop Phase 18)

| Desktop feature | Status |
|---|---|
| Onboarding (preset, name and emoji, quick-add demo) | Port, 3 steps. Shortcut cheat-sheet step dropped |
| Accessibility | Port, using TalkBack, font scale, contrast, reduced motion, 48dp targets |
| Focus mode | Adapt: full-screen current task and timer, optional keep-screen-on. Ambient sound deferred |
| Monthly review prompt | Defer |

### Desktop Phase 20 (Android companion, as originally planned)

| Planned in desktop Phase 20 | Status | Note |
|---|---|---|
| React Native app sharing TypeScript | Replace | Native Kotlin, see ADR-A01. Logic shared by test vectors, not code |
| Swipe left to complete, swipe right to move/star | **Deliberate change** | Android: swipe right completes, swipe left opens the When sheet. Move and star live in the actions sheet. Reason: right-swipe-to-complete is the common mobile convention and keeps "When" (the most frequent triage action) on the thumb side. Your call; it is one line in the gesture config if you want desktop's mapping |
| Location-based reminders (geofencing) | Skip | Needs Google Play Services location (ADR-A08). Revisit only with a new ADR |
| Voice input to NLP | Port | System recognizer intent, same parser |
| Home screen widget: today's tasks + Quick Add | Port | Phase 18 |
| Share sheet creates task with URL attached | Port | Phase 18 |
| Offline-first queue flushing on reconnect | Port | Outbox from day one; engine ships in the sync phases |

### Android-only additions

Home-screen widgets (My Day, Quick Add), Quick Settings tile, share target, app shortcuts, voice input via system recognizer, notification action buttons, biometric lock, haptics. Desktop has no equivalent for these.

---

## 4. Quick Add Spec

Opens as a bottom sheet with the keyboard up. Same parser as desktop.

**Syntax:** `@Area`, `/Project`, `#tag`, `!low|medium|high|critical`, the tomato emoji for a Pomodoro, natural dates ("tomorrow 2pm", "next Monday", "in 3 days", "end of month"), recurrence ("every weekday at 9am").

**Context-aware defaults:**

| Opened from | Task lands in |
|---|---|
| Inside a Project | That Project |
| Inside an Area | Loose task in that Area |
| Inbox, My Day, Planned, any smart list | Inbox |
| Widget, tile, share, app shortcut | Inbox |

**Behavior:**
- A chip row above the keyboard previews the parse (date, area, project, tags, priority). It is not interactive.
- Enter creates the task and **keeps the sheet open** for the next one. Swipe down or tap outside to finish.
- A mic button starts the system speech recognizer and puts the result through the same parser.
- Share target: shared text becomes the title (first line); a shared URL becomes a link attachment when attachments are on, otherwise goes into notes.

---

## 5. Day Plan Page

Replaces desktop's right-side panel. Separate page, reached from an icon opposite the "My Day" title.

**Layout**
- A vertical hour timeline, 80dp per hour, 15-minute snap. Starts at `day_starts_at`. A line marks the current time, and the page scrolls to it on open.
- A **bottom sheet** (peek height visible, drags up) lists My Day tasks that are not yet scheduled. My Day is the pool. There is no separate "Unscheduled" list.

**Placing a task**
1. Select one or more tasks in the bottom sheet (tap to select, checkmarks appear).
2. Tap **Add to plan**. Each selected task is placed on the timeline at the next free slot, using `estimated_minutes` as the duration when set, otherwise 30 minutes, rounded to 15.
3. Selected tasks leave the sheet and appear as blocks.

**Adjusting a block**
- Drag the block body to move it. Drag the top or bottom handle to resize. Both snap to 15 minutes.
- Duration is clamped to 15 to 480 minutes. Start is clamped to 00:00 to 23:59.
- Tap a block to open the task detail sheet. Long-press a block for **Remove from plan**, which sends it back to the sheet.
- Overlaps are allowed and drawn side by side in lanes. No hard block, no warning dialog.
- Writes `scheduled_start_min` and `scheduled_duration_min` together (both or neither) in one tracked write on drop.

**Rules carried from desktop**
- Both fields clear when a task leaves My Day and at day rollover.
- The page is behind the `agenda` module toggle.

**Not in v1:** load-balancing color header, morning summary notification.

---

## 6. Token Differences

| Desktop token | Android |
|---|---|
| Light mode, `data-theme`, system auto | Dropped. Dark only |
| `--sidebar-*`, `--detail-panel-width`, `--planned-calendar-width` | Dropped. No sidebar, sheet instead of panel, week strip instead of side calendar |
| `--titlebar-*` (height and window-control colors) | Dropped |
| `--z-*` scale | Dropped. Compose layers by composition order and platform sheets |
| `--shadow-*` | Replaced by elevation tokens. Glass menus use a translucent surface and border |
| `--task-height-*` 34 / 44 / 56 | 44 / 52 / 60dp, because of the 48dp touch target |
| `--checkbox-size` 20px | 20dp visual with a 48dp hit area |
| `--text-*` px | Same numbers in sp. System font scale applies on top. App font-size step is a further multiplier |
| `--ease-*`, `--motion-*` | Same values |
| `--font-body` Inter | Same. Bundled as a font resource |
| Accent derivation at runtime | Same, in `os11DarkColors(accent)` |
| Added | `minTouchTarget`, `bottomBarHeight`, `fabSize`, `dayPlan*`, `focusRing`, `scrim` |

Deviations that need your eye: task row heights and the font-size multipliers are my choices, not copied from desktop.

---

## 7. Settings Differences

| Setting | Android |
|---|---|
| `theme`, `font_family`, `sidebar_position`, `launch_at_login`, `vim_keybindings`, `triage_keys_enabled` | Not present |
| `haptics_enabled`, `secure_screen`, `sync_attachment_max_mb` | Android only |
| Synced across devices | `day_starts_at`, Pomodoro durations, `waiting_stale_days`, `follow_up_notify_time` |
| Everything else | Device-local |

Modules are device-local on purpose: turning Pomodoro off on the phone does not turn it off on the desktop.

---

## 8. Open Parity Items

Things I could not resolve from the docs I had.

1. Desktop's real post-migration schema (see the SCHEMA.md parity checklist).
2. The 27-row scheduling transition table and its tests. Needed before the scheduling phase.
3. Desktop NLP and recurrence test cases, exported as JSON vectors.
4. The final desktop shell gradient values (the Tokens.kt glow is a placeholder).
5. Desktop font-size steps in px for sm, md, lg, xl.
6. Whether `default_view = 'calendar'` is still a view on desktop (the Oct changes mention List, Board, Timeline, Table, with Calendar via Planned).
