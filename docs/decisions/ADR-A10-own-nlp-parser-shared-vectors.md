# ADR-A10: Own NLP parser in Kotlin, parity by shared test vectors

Status: Accepted (2026-10-10)

## Context
Quick Add parses text like "call mom tomorrow 2pm #family !high every weekday". Desktop has a TypeScript parser. Using the same code on Android would force React Native (rejected, A01) or embedding a JS engine in a native app (extra runtime, startup cost, size). Cloud or AI-based parsing is forbidden. The same applies to recurrence and scheduling transitions.

## Decision
Port the parser to Kotlin in `:domain`. Parity is enforced by **shared JSON vectors**: input text, reference "now" and timezone, and expected structured output. The vectors are exported from the desktop test suite and live in one shared file both repos test against. Same approach for recurrence and the 27-row scheduling transition table.

## Consequences
- Two implementations to maintain. A change to parsing rules must update the vectors first, then both parsers.
- Where Kotlin cannot match desktop exactly (locale or date library differences), the divergence is recorded in ANDROID_VS_DESKTOP.md with the vector that shows it.
- Vectors must be obtained from desktop in Phase 0/7. If they cannot be exported, they are extracted from desktop tests by hand. This is risk R7 in the phases doc.
- Parse runs off the main thread, debounced, and cancels the previous parse (PERFORMANCE.md).
