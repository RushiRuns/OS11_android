# ADR-A02: Room with the bundled SQLite driver, FTS5 through migrations

Status: Accepted (2026-10-10)

## Context
Desktop uses SQLite with an FTS5 virtual table kept in sync by triggers. Android ships a system SQLite whose version and compiled options vary by OS version and vendor, so FTS5 and some SQL features are not guaranteed. Room provides compile-time-checked DAOs and migrations, but only models FTS3/FTS4 through annotations, not FTS5.

## Options considered
1. **Room on the system SQLite.** Simplest. FTS5 availability and SQL behavior vary by device.
2. **Room on `androidx.sqlite:sqlite-bundled`.** The same SQLite on every device, FTS5 included, behavior matches desktop's.
3. **SQLDelight or raw SQLite.** Duplicate of Room's job; more to maintain.

## Decision
Room with the bundled SQLite driver. Plain tables are Room entities. Anything Room cannot express (FTS5 virtual table, its triggers, partial indexes, CHECK constraints) is created in **hand-written migration SQL** and tested by migration tests.

## Consequences
- Search behaves the same on every phone and like desktop.
- App size grows by the bundled library (to be measured; the PERFORMANCE.md size target accounts for it).
- Room's schema validation does not know about FTS tables; the migration test is the guard.
- Spike S-1 (Phase 1) confirms the setup before anything depends on it.
