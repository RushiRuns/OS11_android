# ADR-A04: Material3 as a primitive layer only, no dynamic color

Status: Accepted (2026-10-10)

## Context
OS11 has its own design system (tokens, glass surfaces, priority borders, specific motion). Compose Material3 provides sheets, text fields, switches, pickers and accessibility semantics that are expensive to rebuild correctly. It also brings its own look and, by default, dynamic color.

## Decision
Use Material3 components only as behavioral primitives. Every visual value comes from `Tokens.kt`. `OS11Theme` configures MaterialTheme internally so primitives inherit the right values, but **feature code never reads `MaterialTheme.*`**. Dynamic color (Material You) is not used.

## Consequences
- We keep correct focus, semantics and IME handling without rebuilding them.
- The app looks like OS11, not like a stock Material app, and matches desktop.
- Konsist test fails any feature file that references `MaterialTheme.`.
- Material3 version upgrades can change primitive behavior; each upgrade is checked against the component gallery.
- If a primitive cannot be restyled enough, we replace it with our own component, registered in UTILITIES.md.
