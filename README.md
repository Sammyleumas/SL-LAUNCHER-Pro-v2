# SL Launcher Pro (version 2)

A free Android launcher with a soft UI (neumorphism) design, a floating app drawer, the SL assistant, a Dynamic Island and built-in productivity tools. No ads, no account, no in-app purchases and no servers: everything runs on the phone.

Version 2 is a separate app from SL Launcher v1 (package `com.sllauncher.pro`), so both can be installed side by side.

**Download the latest test build:** https://github.com/Sammyleumas/SL-LAUNCHER-Pro-v2/releases/download/sl-launcher-pro-latest/SL-Launcher-Pro-test.apk

Status and what's included: [`docs/V2-STATUS.md`](docs/V2-STATUS.md).

## Build

```bash
./gradlew assembleDebug          # debug APK in app/build/outputs/apk/debug/
./gradlew testDebugUnitTest      # JVM unit tests
./gradlew lintDebug              # Android Lint
./gradlew bundleRelease          # signed Play bundle (needs the signing key, see docs/RELEASE.md)
```

Requirements: Android Studio (latest stable) or JDK 17+ with the Android SDK. The app runs on Android 8.0 and later.

Every push to `main` runs GitHub Actions: build, unit tests, lint and a release build, then publishes the test APK at the link above.

## Project layout

- `core/designsystem`: soft UI tokens (`theme/`), `softRaised` / `softInset` shadows and all shared components.
- `app`: the launcher. One package per feature: `home`, `drawer`, `search`, `assistant`, `island`, `notifications`, `news`, `music`, `productivity`, `gestures`, `widgets`, `settings`.
- `docs`: design system, permissions, privacy policy draft, QA checklist, release steps, store listing.
