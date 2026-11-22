# SL Launcher Pro v2: status

**Download the test build:** https://github.com/Sammyleumas/SL-LAUNCHER-Pro-v2/releases/download/sl-launcher-pro-latest/SL-Launcher-Pro-test.apk

This is a debug build for Android 8.0 or later. It installs next to SL Launcher v1, which stays unchanged.

## Verification

On every push, CI builds the app, runs the unit tests, runs Android Lint and makes a release build with R8. All of these pass.

Not verified: **the app has not been run on a phone or emulator.** The build environment has none. Use `QA_CHECKLIST.md` for the device pass, and also cover the v2 items listed under "Device testing still needed" below.

## Separate from v1

- **Package:** `com.sllauncher.pro`, so v2 installs next to v1. Each app keeps its own data.
- **Signature permission and app name:** both are v2's own ("SL Launcher Pro").
- **Releases:** v2 has its own repository, CI and download link.
- **Backups:** v1 backup files can be imported into v2, because the format is the same.

## Everything is free

There is no Premium tier, no locked feature, no in-app purchase and no ads. The v1 entitlement code, Premium screen and accent and profile limits were removed.

## Design: soft UI (neumorphism)

- **Palettes:** light soft and dark soft, plus Auto, which follows the phone's dark mode.
- **Accents:** five, all free.
- **Font:** Nunito.
- **Depth:** raised and pressed soft surfaces on every component.
- **Accessibility:** outlines, strong text contrast and visible pressed and selected states (see `DESIGN-SYSTEM.md`).
- **Low-performance mode:** turns the blurred shadows into cheap flat shading.
- **Icons:** a shape setting (system, circle, squircle or square) and optional soft icon plates.
- **Wallpapers:** five built-in soft wallpapers, with an optional daily change.
- **Launcher icon and system bars:** the icon is restyled for the soft look, and the system bars match the theme.

## Features

| Area | Status | Notes |
|---|---|---|
| Home screen, dock, folders, widgets, shortcuts | Done (v1, restyled) | |
| Gestures | Done (new) | Double-tap and swipe-down actions are configurable. Locking the screen and opening the notification shade use an accessibility service that the user turns on. It performs only those two actions. Lock needs Android 9+ |
| Floating drawer | Done | New "Suggested for now" row learns from your own app launches on the phone, by time of day |
| Search and commands | Done (v1) | |
| SL assistant | Done (v1) | The bridge to your separate SL app is still a contract only (`ASSISTANT-BRIDGE.md`) |
| Dynamic Island and notifications | Done (v1, restyled) | |
| Lock screen | Partial (new) | The focus timer shows a live countdown on the lock screen, and music controls appear there. A shortcut opens the lock-screen notification privacy settings. **SL cannot replace the secure lock screen**, because Android doesn't allow it. The "Glance screen" was not built: Android 10+ blocks apps from opening a screen over the lock screen except for calls and alarms |
| News | Done | New: add your own RSS feeds under "My feeds". Licensing of the built-in publishers still needs confirming |
| Music | Done | New: sleep timer (15, 30 or 60 minutes) |
| Productivity | Done | New: habits with streaks, repeating tasks (daily, weekly or weekdays) and screen time (needs usage access) |
| Focus mode | Done (new) | Optional Do Not Disturb (priority only) while focusing. Social, games and media apps ask "open anyway?" |
| SL widget | Done (new) | "SL Today" shows the clock, the date, the next task and the focus timer |
| Customization | Done | All accents and unlimited profiles are free. Icon shapes, soft plates and wallpapers |
| Privacy and backup | Done | Android Auto Backup now restores the home layout, notes, tasks and habits to a new phone through the user's own Google backup. File export and import still work. Reset app suggestions |
| Backend | None, by decision | Everything runs on the phone |

## Not built in v2

| Item | Why |
|---|---|
| On-device AI: news summaries, smart replies, "brief me" | Needs Google's Gemini Nano (ML Kit GenAI), which runs only on some recent phones. Its library versions couldn't be checked from the build environment. The plan is to add it behind a "supported phones only" check |
| "Hey SL" wake word | Always-on listening needs a wake-word model and a persistent service. That drains battery and is a Play policy risk. Tap-to-talk stays |
| Glance screen over the lock screen | Blocked by Android's background-activity rules (see "Lock screen" above) |
| Import a layout from other launchers | Other launchers don't share their layouts. File backup and Auto Backup cover moving SL between phones |
| Theme files to share | Not built. Profiles and backups cover saving a setup |
| Tablet and foldable layouts | The floating drawer and panels already size themselves to the window. Dedicated two-pane layouts were not built |
| More app languages | Not built. Tell me which languages you need |
| Creating calendar events inside SL | Still opens your calendar app's "new event" screen. That avoids asking for calendar write access |

## Device testing still needed

Do the `QA_CHECKLIST.md` pass, plus:

- Soft shadows look right on Android 8 (flat) and on Android 9+ (blurred). Scrolling stays smooth on a low-end phone, and low-performance mode helps.
- Double-tap locks after turning on the gesture helper. Swipe down opens the shade.
- Start a focus session: you see the lock-screen countdown, Do Not Disturb turns on and then off again, and a social app asks first.
- Habits, repeating tasks and screen time (after granting usage access).
- The SL Today widget updates when a task changes. Soft wallpapers apply.
- Auto Backup restore on a second phone, or with `adb shell bmgr`.

## What you need to do

1. **Install and test.** Install the APK and work through the checklist above. Send me what breaks, with your phone model and Android version.
2. **Signing key.** Create one and add the four GitHub secrets to this repository (`RELEASE.md`).
3. **Package name and icon.** Confirm the package name `com.sllauncher.pro`, and send the final icon.
4. **Google Play declarations.** The accessibility service needs a clear in-app disclosure (already shown) and a Play Console declaration. Usage access and Do Not Disturb access also need clear explanations, which are in the app.
5. **News licensing.** Settle licensing for the built-in news publishers.
