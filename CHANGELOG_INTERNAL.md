# OS11 Android - Internal Changelog

> One entry per coding session, written before committing.
> Format: `[date] - [what was built] - [what changed architecturally]`
> "Architecturally" means: a new layer, a new table, a new pattern, a rule bent, a dependency added. If nothing architectural changed, write "none".
> If a performance number was measured, record it here with the device and build type.

---

[2026-10-10] - Governance set drafted: ARCHITECTURE, SYSTEM, SCHEMA, SYNC, PERFORMANCE, DEPENDENCIES, ANDROID_VS_DESKTOP, phases doc, DONE, UTILITIES, ADRs A01-A10, Tokens.kt (uncompiled) - Initial architecture: native Kotlin/Compose, two modules, TrackedWriter sync-ready write path, LAN sync designed but not built. No code compiled yet.
[2026-10-10] - Phase 0 completed: two-module setup (:domain pure Kotlin/JVM and :app Android/Compose), package layout per ARCHITECTURE.md, Hilt, Room, Compose, Spotless (ktlint), Detekt, and Konsist architecture test skeleton. Tokens.kt compiled in com.os11.app.ui.theme. Manifest allowBackup=false with strictly approved permissions. ./gradlew check and assembleDebug green. - Architectural changes: Established :domain and :app module boundaries, Konsist boundary test suite, single Activity Compose host.
