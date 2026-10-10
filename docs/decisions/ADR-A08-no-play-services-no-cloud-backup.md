# ADR-A08: No Google Play Services, no cloud backup

Status: Accepted (2026-10-10)

## Context
OS11 is local-first with a hard rule of nothing that costs money or phones home. Google Play Services features (ML Kit, location, auth, Firebase) depend on Google's SDKs and, for some, quotas or accounts. Android Auto Backup uploads app data to the user's Google account.

## Decision
- No Google Play Services dependency of any kind. QR scanning uses CameraX + ZXing (on-device). Voice input uses the system recognizer intent, not an SDK.
- `allowBackup=false`. User backups are explicit exports via the Storage Access Framework.
- No analytics, crash reporting, ads or remote config. Debug builds use StrictMode and LeakCanary.

## Consequences
- Works on phones without Google services.
- Location-based reminders (geofencing) are not in scope; they would need Play Services location.
- No automatic crash reports. Stability relies on tests, a monkey session in release prep, and the user reporting issues.
- A user who gets a new phone must restore from their own export (or, later, from sync). This must be explained in onboarding and Settings.
- Changing any of this needs a new ADR.
