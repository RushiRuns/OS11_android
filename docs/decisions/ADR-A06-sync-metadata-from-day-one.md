# ADR-A06: Sync metadata built into the write path from day one

Status: Accepted (2026-10-10)

## Context
Sync will be built after v1.0.0. Retrofitting change tracking into a shipped app means migrating real user data and touching every write. Desktop's planned sync (snapshot queue, no per-field clocks) cannot merge concurrent edits safely.

## Decision
From the first write, every synced change goes through `TrackedWriter`, which in one Room transaction writes: the data row, per-field hybrid logical clocks (`sync_field_clock`), an outbox row (`sync_queue`), and tombstones for deletes. Entities use stable UUIDs; default rows and recurrence occurrences use deterministic UUIDv5. Per-device state is excluded from synced tables. The merge function exists in `:domain` with convergence property tests before sync ships.

## Consequences
- Every write costs a little more (clock rows and an outbox row). Budgeted in PERFORMANCE.md.
- The outbox grows while no desktop is paired. A compaction/cap rule is a Phase 3 task.
- Desktop must adopt compatible rules (HLC, tombstones, TLS pinning); SYNC.md section 12 lists them. If desktop will not, Android sync shrinks to import/export.
- The sync engine is built last, against a mock desktop, with no change to the write path.
