# Phase 1 completion report

**PHASE:** 1, Project initialization (Phase 0 discovery also complete: see `PHASE-0-DISCOVERY.md`).

**OBJECTIVE:** A building Android project that is a real launcher, with HOME intent filters, default-launcher setup, dependency injection, theme, navigation, tests and CI.

**IMPLEMENTED**
- Gradle 9.6 wrapper, verified version catalog, `app` and `core:designsystem` modules.
- Launcher activity with a `HOME` + `DEFAULT` intent filter, single instance, system wallpaper behind the window, excluded from Recents.
- Default-home detection: `RoleManager` on Android 10+, intent resolution on Android 8–9. Re-checked on every resume.
- Setup flow: system role dialog, a Settings fallback when the dialog is missing or declined, and an error message when a phone has no default-apps screen.
- Pressing Home returns to the home screen. Back on the home screen is ignored while SL is the default.
- Live clock and date, aligned to the minute, following the 12/24-hour setting and the system language.
- Hilt DI, type-safe Navigation Compose routes.
- SL design tokens (colour, type, spacing, shapes) and `SLButton`.
- Release build type with R8 shrinking.

**FILES CREATED:** every file in the repository (it started empty).

**FILES MODIFIED:** none that existed before.

**DEPENDENCIES:** AGP 9.5.1, Kotlin 2.4.21, KSP 2.3.14, Hilt 2.60.1, Compose BOM 2026.08.00, AndroidX core 1.19.1, activity 1.13.0, lifecycle 2.11.0, navigation 2.10.2, hilt 1.4.0, kotlinx-serialization 1.13.0 (type-safe routes), coroutines 1.11.0. Test: JUnit 4.13.2, coroutines-test, AndroidX test ext 1.3.0, Espresso 3.7.0, Compose UI test.

**BUILD STATUS:** Successful, GitHub Actions run #5 (`assembleDebug`, `testDebugUnitTest`, `assembleDebugAndroidTest`, `lintDebug`, `assembleRelease`).

Fixed along the way:
1. `compileSdk` raised to 37, which current AndroidX AARs require. `targetSdk` stays 36.
2. Unit tests collected `StateFlow`s on a deferred dispatcher.
3. Lint `NewApi`: `RoleManager` calls are now guarded at each call site.
4. Lint `NonObservableLocale`: the clock now reads `LocalLocale`, so it updates when the language changes.

**TEST STATUS**
- 9/9 JVM unit tests pass.
- 3 Compose UI tests compile; they need a device or emulator to run.
- Not yet run on a physical device.

**KNOWN ISSUES**
- No device testing yet.
- Placeholder launcher icon (final branding is Phase 20).
- System sans-serif until Phase 2 bundles a typeface.
- `applicationId` `com.sllauncher.app` needs your confirmation before any Play upload.

**NEXT PHASE:** Phase 2, design system and component catalogue.

**USER ACTION**
1. Install the debug APK from the latest green CI run (`sl-launcher-debug-apk` artifact) and set SL Launcher as your home app.
2. Send your test phone's model, Android version and RAM.
3. Share the SL assistant source and tell me its stack.
4. Confirm the package name.
5. Approve Phase 1.
