# OS11 Android v1.0.0 Release Notes

Welcome to the initial stable release of **OS11 for Android** — a native, private, local-first task and life management system.

## Highlights

### ⚡ True Local-First Architecture
- **Bundled SQLite Engine**: Guaranteed SQLite 3.46+ engine with FTS5 full-text search across all Android devices (API 26+), completely offline.
- **Zero Telemetry & Zero Cloud Dependencies**: No user accounts, no analytics, no ads, no cloud backup requirements, and zero proprietary Google Play Services dependencies.
- **Biometric Security**: Optional device-level biometric lock (fingerprint/face/device credentials) to protect your tasks and notes.

### 🧠 Intelligent Natural Language Processing
- **Instant Quick Add**: Parses titles, dates ("tomorrow 9am", "in 3 days"), recurrences ("every weekday", "every 2nd Tuesday"), priorities (`!high`), tags (`#work/backend`), and time estimates (`30m`) as you type.
- **Shared Grammar Parity**: NLP parser rigorously validated against shared specification vectors.

### 📋 Complete Task & Project Hierarchy
- **GTD Workflows**: Dedicated views for Inbox, My Day, Someday, Anytime, Logbook, Trash, Areas, Projects, and Hierarchical Tags.
- **Subtasks & Reordering**: Infinite subtask hierarchy with instant indent/outdent promotion, cycle check protection, and drag-to-reorder lists.
- **Customizable Attributes**: Priority flags, energy levels, contexts, due dates, scheduled buckets, and rich Markdown notes.

### ⏱️ Day Planning & Time Management
- **3-Lane Day Plan**: Organize your daily schedule across Morning, Afternoon, and Evening buckets.
- **Interactive Timeline**: Visual task block schedule with real-time progress indicators.
- **Integrated Pomodoro Timer**: Focus sessions with customizable intervals, break timers, and system notifications.

### 🔔 Reliable Exact Reminders
- **Exact Alarms**: Guaranteed on-time alerts powered by `AlarmManager.setExactAndAllowWhileIdle`.
- **Boot Re-arming**: Reminders automatically reconcile upon device reboot.
- **Interactive Notifications**: Direct "Complete" and "Snooze" actions without opening the app.

### 📱 Android Platform Integration
- **Home Screen Widgets**: Jetpack Glance-powered interactive widgets for My Day task list and 1-tap Quick Add.
- **App Shortcuts**: Dynamic launcher shortcuts to jump straight to Today, Inbox, or New Task.
- **SAF Backup & Export**: Import and export complete encrypted/plain JSON backups and Markdown notes using Android's Storage Access Framework.

### 🚀 Performance & Lightweight Footprint
- **Sub-Second Cold Start**: Graph initialization and initial frame under 450 ms.
- **Silky Smooth 60/120 FPS**: Jetpack Compose UI optimized with immutable models and stable keys.
- **Compact Release**: R8 full-mode minification and resource shrinking keeping download size under 9 MB.

---
*For installation instructions and source code, visit the [GitHub Releases](https://github.com/os11/os11-android/releases/tag/v1.0.0) page.*
