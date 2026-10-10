# ADR-A03: Two Gradle modules, `:domain` and `:app`

Status: Accepted (2026-10-10)

## Context
Business rules (merge, clocks, NLP, recurrence, scheduling transitions, ordering) must be testable fast, must not depend on Android, and must be machine-checked as pure. A single module relies on discipline alone, and AI editors drift.

## Options considered
1. Single module with package conventions.
2. Two modules: pure Kotlin/JVM `:domain` and Android `:app`.
3. Many feature modules.

## Decision
Two modules. `:domain` depends only on Kotlin stdlib, kotlinx-serialization and lib-recur. It cannot import Android, Room, Hilt or coroutines. It does not read the clock or randomness; both are injected.

## Consequences
- The compiler enforces the purity rule. Domain tests run on the JVM in seconds.
- Shared test vectors run against `:domain` directly.
- Feature modules are not used. Build speed and complexity of many modules are not justified for a one-person project. Revisit only if build time becomes a measured problem, with a new ADR.
