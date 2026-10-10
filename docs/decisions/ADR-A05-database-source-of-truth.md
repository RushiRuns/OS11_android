# ADR-A05: Database as the single source of truth, no optimistic layer

Status: Accepted (2026-10-10)

## Context
Desktop uses optimistic updates in a renderer store because every write crosses an IPC boundary. On Android, the database is in-process and Room emits Flow updates after commit. With sync coming, a second copy of state in memory creates a second place where merge results can disagree.

## Decision
The UI observes database Flows through services and ViewModels. A write goes to the database and the UI updates from the resulting emission. There is no optimistic in-memory state layer. During a drag, only transient visual state exists; one tracked write happens on drop.

## Consequences
- One source of truth. Remote changes from sync appear through the same path as local ones.
- A write must be fast enough that waiting for the emission is imperceptible. The PERFORMANCE.md target (tracked write P95 under 20 ms) exists for this reason.
- If a measured case needs optimism, it needs an ADR and a reconcile rule.
