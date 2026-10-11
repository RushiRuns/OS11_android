# ADR-A13: Distribution Strategy for OS11 Android

Status: Accepted (2026-10-11)

## Context
OS11 Android is a privacy-first, local-first personal productivity and task management app. As established in ADR-A08, the app includes zero proprietary Google Play Services dependencies, zero cloud analytics, zero remote tracking, and strictly local storage.

When releasing v1.0.0, we must decide on distribution channels:
1. **Direct GitHub Releases (Sideload APK)**: Immediate availability, zero cost, reproducible builds, full control over signing and release timing, but requires users to enable sideloading and manually check/update or use Obtainium.
2. **F-Droid (FLOSS Directory)**: The premier repository for free and open-source Android software. Perfect philosophical and architectural fit for OS11. Zero cost, provides automatic updates, trusted by privacy-conscious users. Review cycle can take days to weeks for initial inclusion in the main repository, though IzzyOnDroid repository provides rapid distribution.
3. **Google Play Store**: Broadest consumer reach and seamless automatic updates, but requires a $25 developer registration fee, mandatory identity verification, ongoing Google policy reviews, and potential friction with local filesystem export/SAF features.

## Decision
We adopt a **multi-tier FLOSS distribution strategy**:

1. **Tier 1 — GitHub Releases (Primary at v1.0.0)**:
   - Every release tag produces signed release APKs (both universal APK and per-ABI splits for `arm64-v8a`, `armeabi-v7a`, `x86_64`).
   - Releases include SHA-256 checksums and release notes.
   - Compatible with APK update managers such as Obtainium.

2. **Tier 2 — F-Droid & IzzyOnDroid**:
   - Submit recipe to F-Droid and IzzyOnDroid repository immediately following v1.0.0 tag.
   - Built with pure open-source toolchains, validating our adherence to ADR-A08 (no proprietary closed blobs).

3. **Tier 3 — Google Play Store (Deferred)**:
   - Google Play Store submission is deferred post-v1.0.0. Once sync is hardened in v1.1.0, Play Store distribution may be evaluated as an opt-in channel for non-technical users if community demand warrants the developer registration overhead. The codebase will remain 100% agnostic to Google Play Services.

## Consequences
- **Zero Financial Cost**: Launching v1.0.0 incurs $0 in platform fees.
- **Privacy Guarantee**: Users can install and run OS11 on de-Googled devices (GrapheneOS, CalyxOS, LineageOS) with zero friction.
- **Release Agility**: Updates can be shipped immediately via GitHub Releases without waiting for app store review gates.
- **Documentation**: Release notes and onboarding must clearly direct users to GitHub Releases and F-Droid.
