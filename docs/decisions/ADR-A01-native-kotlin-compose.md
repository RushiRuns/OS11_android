# ADR-A01: Native Kotlin and Jetpack Compose instead of React Native

Status: Accepted (2026-10-10)

## Context
Desktop OS11 phases doc (Phase 20) planned the Android app as React Native 0.74+, sharing TypeScript types and the NLP parser with the Electron app. The Android app is now a standalone offline product first, with sync later. The priorities are the same five North Stars: speed, convenience, modularity, feel. Feel and speed are judged on a phone, with lists, sheets, gestures, alarms, widgets and a notification-driven reminder system.

## Options considered
1. **React Native.** Shares TypeScript with desktop. But: bridge and JS runtime cost on startup and on long lists, native modules needed for alarms, widgets, Keystore, SAF and Quick Settings tiles anyway, and "feel" depends on third-party gesture and animation libraries.
2. **Flutter.** Good rendering, but a third language and a custom rendering stack; still needs platform channels for every Android-specific surface.
3. **Native Kotlin with Jetpack Compose.** First-class access to AlarmManager, Glance widgets, Keystore, WorkManager, NsdManager, SAF, TalkBack. One language end to end.

## Decision
Native Kotlin and Jetpack Compose.

## Consequences
- Types, NLP, recurrence and scheduling logic are **not shared as code** with desktop. They are re-implemented in Kotlin and kept equal by **shared JSON test vectors** (see A10). This is real duplicated effort and a drift risk. The vectors are the mitigation.
- The desktop phases doc Phase 20 "Android Companion App" section is superseded. The desktop repo should be told.
- All Android-only surfaces (widgets, tile, share target, exact alarms) need no bridge.
- The developer must be comfortable reviewing Kotlin. The AI editor writes most code; governance docs and architecture tests carry the discipline.
- Location-based reminders (geofencing) from the old plan are not carried over; they need Google Play Services (see A08).
