# OS11 Android - Dependencies

> Every library here is free and open source, needs no account, no API key, and never phones home.
> No dependency is added without an entry in this file (ARCHITECTURE.md rule 6).
> The cost of a dependency is its size, its startup cost, and its upgrade risk. "It saves 20 lines" is not a reason.

---

## Policy

- **Free and open source only.** Apache-2.0, MIT, BSD, or equivalent. No paid tier, no hosted service, no API key.
- **No SDK that phones home.** No analytics, crash reporting, ads, remote config, or cloud backend.
- **Versions** are pinned in `gradle/libs.versions.toml`. This file does not state versions. Use the latest stable at the time the dependency is added.
- **Verify on the day it is added:** the licence, the date of the last release, and that it runs without Google Play Services. The licence column below is my understanding and is **not verified**. A library with no release in 12 months needs an ADR before it goes in.
- **Weight check:** anything adding more than 500 KB to the release build, or running code at startup, needs a one-line justification here.
- **Pending spike** means the library is a candidate, not approved. A short spike session decides, and the result updates this table.

---

## Approved: Runtime (`:app`)

| Library | Purpose | Licence (to verify) | Why this one |
|---|---|---|---|
| Kotlin stdlib, coroutines | Language, async | Apache-2.0 | Standard |
| kotlinx-serialization-json | Sync wire format, export/import JSON, settings values | Apache-2.0 | First-party Kotlin, no reflection, small |
| kotlinx-collections-immutable | Stable list parameters for Compose | Apache-2.0 | Keeps composables skippable (SYSTEM.md section 4) |
| Jetpack Compose (BOM) | UI | Apache-2.0 | The UI toolkit |
| Compose Material3 | Primitive layer for sheets, fields, switches, pickers, snackbars | Apache-2.0 | Behavior and accessibility semantics. Fully restyled from tokens |
| androidx.activity-compose, lifecycle-runtime-compose, lifecycle-viewmodel-compose | Activity, lifecycle-aware collection, ViewModel | Apache-2.0 | Required |
| androidx.navigation-compose | Single-activity navigation, type-safe routes | Apache-2.0 | First-party |
| Hilt (+ hilt-navigation-compose, hilt-work) | Dependency injection | Apache-2.0 | First-party, compile-time checked |
| Room (runtime, ktx, compiler via KSP) | Database access, migrations, schema export | Apache-2.0 | First-party. Entities only; FTS5 and triggers via migrations |
| androidx.sqlite:sqlite-bundled | Bundled SQLite driver | Apache-2.0 | Same SQLite version on every device, FTS5 guaranteed, matches desktop's SQLite behavior (ADR-A02) |
| androidx.paging (runtime, compose) | Paged lists for All Tasks, Completed, Trash, search | Apache-2.0 | First-party, works with Room |
| androidx.datastore-preferences | Device-local flags that are not task data | Apache-2.0 | Replaces SharedPreferences |
| androidx.work (WorkManager) | Daily backup, trash purge, opportunistic sync bursts | Apache-2.0 | The supported way to do deferrable work |
| androidx.core-splashscreen | Splash handoff | Apache-2.0 | Required for consistent splash on all API levels |
| androidx.biometric | Biometric app lock | Apache-2.0 | First-party |
| androidx.glance (appwidget) | Home-screen widgets in Compose style | Apache-2.0 | First-party |
| androidx.camera (core, camera2, lifecycle, view) | Camera preview for QR pairing | Apache-2.0 | First-party, no Play Services |
| ZXing core | Decode QR codes from camera frames | Apache-2.0 | Pure Java, on-device. Chosen over ML Kit to avoid a Google SDK (ADR-A08) |
| OkHttp | WebSocket client for LAN sync | Apache-2.0 | The only HTTP-family client in the app. Used by `LanWebSocketTransport` only |
| Coil (compose) | Attachment thumbnails | Apache-2.0 | Kotlin-first, small |
| Jsoup | Sanitize note HTML | MIT | The Android-side equivalent of DOMPurify. Notes are stored as sanitized HTML for parity with desktop |
| lib-recur (dmfs) | RFC 5545 RRULE parsing and expansion | Apache-2.0 | Pure Java, no Android dependency, usable from `:domain`. Parity with desktop's `rrule` is checked by shared vectors |
| androidx.profileinstaller | Installs baseline profiles | Apache-2.0 | Startup performance |

## Approved: `:domain` (pure Kotlin/JVM)

| Library | Purpose |
|---|---|
| Kotlin stdlib | |
| kotlinx-serialization-json | `Change` and `FieldWrite` JSON |
| lib-recur | Recurrence |

Nothing else. No coroutines in `:domain` unless an ADR says so. No Android, no Hilt, no Room.

## Approved: UI Extras (small, each justified)

| Library | Purpose | Notes |
|---|---|---|
| Reorderable (Compose, `sh.calvin.reorderable`) | Drag to reorder lazy lists | Pending spike. Confirm it handles drag-with-horizontal-offset for nesting, otherwise nesting is custom and this library does reorder only |
| compose-rich-editor | Edit note HTML | **Pending spike, high risk.** See Risks in the phases doc. Must round-trip the HTML subset desktop's editor produces without silently stripping content |
| Vico (compose) | Bar and line charts for Dashboard | Pending spike. If it fails the weight check, charts are drawn with Compose Canvas |
| Haze | Backdrop blur for glass surfaces | Pending spike. Optional. Without it, glass uses a translucent surface and border (ADR-A07) |

## Approved: Testing and Quality (not shipped)

| Library | Purpose |
|---|---|
| JUnit4 | Unit and instrumented tests |
| kotlinx-coroutines-test | Coroutine tests |
| Turbine | Flow assertions |
| MockK | Mocks |
| androidx.room-testing | Migration tests |
| androidx.compose.ui-test-junit4 | Compose UI tests |
| androidx.test (runner, ext) | Instrumented tests |
| Robolectric | Optional JVM tests that need Android classes |
| androidx.benchmark-macro-junit4 + Baseline Profile Gradle plugin | Startup, scroll, baseline profile generation |
| Konsist | Architecture tests (layer imports, token use, forbidden APIs) |
| LeakCanary | `debugImplementation` only |
| Spotless (ktlint) + detekt | Formatting and static analysis |

## Build Tooling

Android Gradle Plugin, Kotlin Gradle plugin, KSP, Gradle version catalog.

## Dev Tooling Outside the App (never shipped)

| Tool | Purpose |
|---|---|
| Node.js + `ws` | `tools/mock-desktop/`: a mock desktop sync server for building and testing the transport before the real one exists |
| OpenSSL | Generates the self-signed certificate for the mock server |

---

## Banned

| Banned | Why | Use instead |
|---|---|---|
| Firebase (any product) | Cloud dependency | Local DB, LAN sync |
| Google Play Services (location, auth, ML Kit, anything) | Google SDK, device-dependent, not available everywhere | Platform APIs, ZXing |
| Crashlytics, Sentry, any analytics or telemetry | Phones home | LeakCanary and StrictMode in debug; Play Console vitals if Play Store later (needs ADR) |
| Ads SDKs, remote config, A/B SDKs | Phones home | None |
| Retrofit, Ktor client, any other HTTP client | No HTTP APIs exist | OkHttp for the sync transport only |
| Glide, Picasso | Duplicate of Coil | Coil |
| RxJava, LiveData | Duplicate of Flow | Flow |
| Gson, Moshi, Jackson | Duplicate, reflection-based | kotlinx.serialization |
| Realm, SQLDelight, Exposed | Duplicate of Room | Room |
| Koin, manual Dagger without Hilt | Duplicate of Hilt | Hilt |
| kapt | Slower | KSP |
| EncryptedSharedPreferences / androidx.security-crypto | Deprecated | `SecretStore` over the Android Keystore |
| Fragments, XML layouts (beyond manifest and resource XML) | Compose only | Compose |
| Accompanist | Mostly folded into Compose | Compose APIs |
| Material You dynamic color | Own tokens | `Tokens.kt` |
| Any library needing an API key or account | Free and local only | |
| Any AI/LLM SDK | Hard rule 11 | |
| Android Auto Backup service | Cloud copy of user data | Explicit SAF export and backup |

---

## Adding a Dependency

1. Search `UTILITIES.md` and this file. Does something already do it?
2. Could 20 lines of Kotlin do it? Then write them.
3. Check licence, last release date, size, startup cost, Play Services dependence.
4. Add a row here with the justification.
5. If it is not a one-line change, write an ADR first.
6. Human review before it lands.
