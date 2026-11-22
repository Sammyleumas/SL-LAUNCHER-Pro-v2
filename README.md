# SL Launcher

An intelligent, customizable Android launcher with a neo-brutalist design, a floating app drawer, the SL voice assistant, and a Dynamic Island-style live activity system.

**Status:** Phase 1, project foundation. See [`docs/PHASE-0-DISCOVERY.md`](docs/PHASE-0-DISCOVERY.md) for the architecture, Android constraints and roadmap.

## What works today

- Installs as a real Android launcher (HOME intent filter) and can be selected as the default Home app.
- Detects whether SL is the default home app and walks the user through selecting it: the system dialog on Android 10+, or Settings as a fallback.
- Home screen over the system wallpaper with a live, locale-aware clock and date.
- Pressing Home returns to the home screen; Back on the home screen is ignored (standard launcher behaviour).

## Requirements

- **Android Studio** (latest stable), which bundles a suitable JDK and the Android SDK. Use it for emulators and device installs; VS Code is fine for editing.
- JDK 17 or newer if building from the command line.
- An Android 8.0+ device or emulator.

## Build and run

```bash
# macOS / Linux
./gradlew assembleDebug          # build the debug APK
./gradlew testDebugUnitTest      # JVM unit tests
./gradlew installDebug           # install on a connected device (USB debugging on)
./gradlew connectedDebugAndroidTest   # UI tests on a device/emulator

# Windows
gradlew.bat assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

**Without a local setup:** every push runs GitHub Actions (`Android CI`). Open the run on GitHub, download the `sl-launcher-debug-apk` artifact, unzip it and install the APK on your phone.

## Make SL Launcher your home app

1. Open **SL Launcher** from your app list.
2. Tap **Set up**, then **Set as default home app**, and confirm in the system dialog.
3. If no dialog appears (some phones hide it), tap **Open Android home settings** and choose SL Launcher.
4. Press Home: SL Launcher now opens.

To switch back: Android **Settings › Apps › Default apps › Home app**.

## Project structure

```
app/                 Launcher activity, navigation, DI, home + setup screens
core/designsystem/   SL colour, type, spacing tokens and components
docs/                Phase reports and architecture
gradle/libs.versions.toml   All dependency versions (verified)
```
