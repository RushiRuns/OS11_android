# ADR-A07: Glass surfaces without backdrop blur

Status: Accepted (2026-10-10)

## Context
Desktop uses glass-style menus with backdrop blur. Compose can blur its own content (`Modifier.blur`, API 31+) but has no general backdrop blur; the min SDK is 26. Third-party libraries (e.g. Haze) exist but add a dependency and a measurable per-frame cost over scrolling lists.

## Decision
Glass surfaces use a translucent surface token plus a border token (`surfaceGlass` and `borderGlass` in `Tokens.kt`). True backdrop blur is not assumed. Spike S-5 evaluated Haze and measured measurable frame time degradation (~8-14 ms/frame) over scrolling lists on 120Hz displays, plus lack of native API 26-30 support. Haze is dropped; translucent surface plus border is accepted as the permanent baseline.

## Consequences
- Clean, consistent rendering across all API levels (26 to 36).
- Zero per-frame blur cost; steady 60/120 fps during sheet transitions and list scrolls.
- All glass usage goes through `OSGlassMenu` backed by theme tokens.
