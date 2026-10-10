# ADR-A09: Timers derived from timestamps

Status: Accepted (2026-10-10)

## Context
Pomodoro sessions and countdowns must stay correct when the screen is off, the process is killed, or the device sleeps. A coroutine that increments a counter every second drifts, wakes the CPU, and dies with the process.

## Decision
A running timer stores its start timestamp (and planned duration). Remaining time is computed from the stored start and the current time whenever the UI is visible. The end of a session is an exact alarm. The ongoing notification uses the system chronometer. No background coroutine ticks per second.

## Consequences
- Correct after process death and reboot (with boot re-arm).
- Needs the exact-alarm permission flow and a fallback when denied (Phase 11/12).
- Clock changes need handling: a time-change receiver reconciles armed alarms.
- The timer is device-local and is not synced live.
