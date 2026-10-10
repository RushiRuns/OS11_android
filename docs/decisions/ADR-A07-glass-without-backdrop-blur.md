# ADR-A07: Glass surfaces without backdrop blur

Status: Accepted, pending spike S-5 (2026-10-10)

## Context
Desktop uses glass-style menus with backdrop blur. Compose can blur its own content (`Modifier.blur`, API 31+) but has no general backdrop blur; the min SDK is 26. Third-party libraries (e.g. Haze) exist but add a dependency and a measurable per-frame cost over scrolling lists.

## Decision
Glass surfaces use a translucent surface token plus a border token. True backdrop blur is not assumed. Spike S-5 measures Haze on the reference device; it is adopted only if frame time stays inside the PERFORMANCE.md budget while a sheet is over a scrolling list, and only as an enhancement behind the same API (`OSGlassMenu`).

## Consequences
- Slightly less depth than desktop on API 26 to 30 and wherever blur is off.
- No per-frame blur cost by default.
- All glass usage goes through `OSGlassMenu` so the decision can change in one place.
