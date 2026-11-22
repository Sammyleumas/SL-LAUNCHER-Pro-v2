# SL Launcher: project status (phases 0–20)

**Download the test build:** https://github.com/Sammyleumas/ES-EL-LAUNCHER-/releases/download/sl-launcher-latest/SL-Launcher-test.apk
(release page: https://github.com/Sammyleumas/ES-EL-LAUNCHER-/releases/tag/sl-launcher-latest)

Every push to `ccr-6d920359-jg1086` rebuilds and replaces this file. It is a **debug build** for testing on Android 8.0 or later. It is not a Play Store release.

## Verification

Each CI run does all of the following, and all of it passes:

1. Builds the debug app.
2. Runs the JVM unit tests for `app` and `core:designsystem`.
3. Compiles the instrumented UI tests.
4. Runs Android Lint.
5. Builds the release app with R8.

**Not verified:**

- **Physical devices:** the app has not been run on any phone. The build environment has no device or emulator, so nothing in this report comes from on-device testing.
- **Instrumented UI tests:** these compile in CI but have not been run.
- **Performance:** not measured.

Status in the table below means:

- **Done:** the code is written, compiles and passes lint and its unit tests. It still needs a manual device pass using `QA_CHECKLIST.md`.
- **Partial:** part of the feature is built; the Notes column says which part.
- **Blocked:** it needs something only you can provide.

## Phases

| # | Phase | Status | Notes |
|---|---|---|---|
| 0 | Discovery | Done | `PHASE-0-DISCOVERY.md` |
| 1 | Project setup | Done | Kotlin, Compose, Hilt, CI. `PHASE-1-REPORT.md` |
| 2 | Design system | Done | Neo-brutalist tokens and components. `DESIGN-SYSTEM.md` |
| 3 | Onboarding and home | Done | Default-home setup, paged grid, dock, folders, drag and move mode, widgets, pinned shortcuts, wallpaper |
| 4 | Floating drawer | Done | Search, categories, favourites, recents, hidden apps, list or grid, A–Z index, work profile |
| 5 | Search and commands | Done | Apps, contacts, settings, commands, web, voice. The command parser is on-device and regex-based |
| 6 | SL assistant | Partial | Assistant built into the launcher: tap to talk or type, speech output, confirmation before risky changes. **Integration with your separate SL app is a contract only**: a signature-protected Messenger service (`ASSISTANT-BRIDGE.md`). That app's source was not available |
| 7 | Dynamic Island | Done | Music, calls, messages with quick reply, timers, downloads, charging, low battery. Uses the notification listener, so it is limited to what apps put in their notifications |
| 8 | News | Partial | RSS reader, offline cache, save, share, listen, background refresh. **You must confirm publisher licensing before any commercial use.** The Lifestyle category has no source (`NEWS-SOURCES.md`) |
| 9 | Music | Done | Plays audio files stored on the phone (Media3): queue, shuffle, repeat, playlists, favourites, lock-screen controls. Streaming apps appear through the island |
| 10 | Notifications | Done | Center with grouping, mute, hidden content, optional history |
| 11 | Quick settings and device info | Done | Flashlight, brightness (needs permission), volume, system panels, battery, storage, memory |
| 12 | Productivity | Done | Notes, tasks with reminders, focus and Pomodoro timer, countdowns, calendar view (read-only) |
| 13 | Customization | Done | Grid, icons, dock, themes, accent colours, fonts, icon packs, profiles, undo |
| 14 | Privacy and backup | Done | Privacy dashboard, file export and import. Widgets must be re-added after a restore, because Android limits widget restore |
| 15 | Performance | Partial | Low-performance mode, lazy lists, icon cache, R8, StrictMode in debug builds. **Not measured on a device yet** (see the targets in `QA_CHECKLIST.md`) |
| 16 | Accessibility | Partial | Content descriptions, 48dp touch targets, contrast-checked colours, reduce-motion setting, Move as an alternative to drag. **TalkBack pass not done yet** |
| 17 | Monetization | Blocked | Entitlement layer and premium screen exist. Every user is on Free until Play Billing is added, which needs your Play Console account and products (`BACKEND-AND-MONETIZATION.md`) |
| 18 | Backend | Not needed for version 1 | Decision recorded: every core feature runs on the phone. AI features, sync and the marketplace need a backend later |
| 19 | QA | Partial | Automated tests pass in CI. The manual device checklist and device matrix are written but not yet carried out |
| 20 | Release | Partial | Test builds are published automatically. Release signing is ready but needs your key (`RELEASE.md`). Store listing drafted (`STORE_LISTING.md`) |

## What you need to do

1. **Install the test APK** on a real phone and work through `QA_CHECKLIST.md`. Report anything broken.
2. **Signing key:** create an upload key and add the four GitHub secrets (`RELEASE.md` §2b).
3. **Package name:** confirm `com.sllauncher.app`. It cannot change after the first Play upload.
4. **Icon:** supply the final launcher icon and feature graphic. The current icon is a placeholder.
5. **News:** settle licensing with the publishers, or swap the feeds.
6. **SL app:** share its source, or implement the bridge contract in it.
7. **Play Console:**
   - Create the developer account and the products, then Play Billing can be added.
   - Publish the privacy policy (draft in `PRIVACY_POLICY.md`).
   - Run the 14-day closed test.
