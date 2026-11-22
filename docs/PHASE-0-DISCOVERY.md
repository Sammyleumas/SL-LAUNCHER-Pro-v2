# Phase 0: Discovery, environment and architecture

_Date: 2026-11-16_

## 1. Environment report

| Item | Finding |
|---|---|
| Repository | `Sammyleumas/ES-EL-LAUNCHER-` was **empty** (no commits, no files). New project created from scratch. |
| Existing SL assistant source | **Not available.** Nothing in this repository. Integration contract is defined in §6; the SL source is needed before Phase 6. |
| Cloud build container | JDK 21 and Gradle 8.14 installed; **no Android SDK**. The network policy blocks `dl.google.com` / `maven.google.com` and `downloads.gradle.org`, so Android code **cannot be compiled in this container**. |
| Build verification | Done on **GitHub Actions** (`.github/workflows/android.yml`), where the Android SDK is pre-installed. |
| Your machine | Not inspected (cloud session). Install Android Studio for emulator and device work; VS Code is fine for editing. |
| Physical test device | Unknown. Please give model, Android version and RAM (see "User action"). |

## 2. Verified toolchain (checked 2026-11-16)

| Component | Version | Source |
|---|---|---|
| Gradle | 9.6.0 | AGP 9.5 minimum/default |
| Android Gradle Plugin | 9.5.1 | developer.android.com AGP release notes |
| Kotlin (+ Compose compiler) | 2.4.21 | Maven Central |
| KSP | 2.3.14 | Maven Central |
| Hilt / Dagger | 2.60.1 | Maven Central |
| Compose BOM | 2026.08.00 (Compose 1.12) | Android Developers blog |
| AndroidX core / activity / lifecycle / navigation / hilt | 1.19.1 / 1.13.0 / 2.11.0 / 2.10.2 / 1.4.0 | AndroidX versions table |
| JDK | 17 (minimum for AGP 9.5) | AGP compatibility table |

AGP 9 has **built-in Kotlin support**, so the `org.jetbrains.kotlin.android` plugin is no longer applied.

## 3. SDK levels

- **minSdk 26 (Android 8.0).** Native `java.time`, adaptive icons, notification channels and pinned shortcuts. Covers almost all active devices, including Android Go phones.
- **compileSdk 37** (required by Compose 1.12 and Navigation 2.10 libraries; compile-time only).
- **targetSdk 36.** Meets current Google Play target requirements without opting into API 37 runtime changes yet. Re-check before release (Phase 20).

## 4. Android launcher constraints (what is and isn't possible)

| Capability | Status | Notes |
|---|---|---|
| Being the Home app | ✅ Supported | HOME intent filter, plus the user's choice via `RoleManager.ROLE_HOME` (Android 10+) or Settings. An app can never make itself the default silently. |
| Listing and launching apps | ✅ Supported | `LauncherApps` plus a `<queries>` entry for `MAIN/LAUNCHER`. No `QUERY_ALL_PACKAGES` needed. |
| App shortcuts (long-press) | ⚠️ Default launcher only | `LauncherApps.getShortcuts` works only while SL holds the HOME role. |
| Widgets | ✅ Supported | `AppWidgetHost`. Binding needs a one-time user confirmation (`ACTION_APPWIDGET_BIND`). |
| Uninstall / app info | ✅ Supported | Through system dialogs only (`ACTION_DELETE`, app details settings). |
| Notifications, Dynamic Island content | ⚠️ Needs permission | `NotificationListenerService`, which the user must enable in Settings. |
| Quick reply | ⚠️ Compatible apps only | Only for notifications that expose a `RemoteInput` action. |
| Media controls | ⚠️ Needs permission | `MediaSessionManager.getActiveSessions` requires notification-listener access. |
| Call answer/decline | ⛔ Restricted | Needs the dialer role or `ANSWER_PHONE_CALLS`, which Play strictly limits. Plan: display only, unless SL becomes a full dialer. |
| Island floating over other apps | ⛔ Restricted | `SYSTEM_ALERT_WINDOW` is heavily restricted and Play may reject it. The island will live **inside the launcher** and use Android's own notifications elsewhere. |
| Toggling Wi-Fi/Bluetooth directly | ⛔ Restricted (API 29+) | Use `Settings.Panel` and system screens instead. |
| Wake word | ⚠️ Battery and policy heavy | Needs a foreground service with microphone type, a visible notification, and an on-device model. Deferred; tap-to-talk first. |
| AccessibilityService | ⛔ Won't use | Using it for non-accessibility purposes breaks Play policy. |

## 5. Architecture

- **Pattern:** MVVM with unidirectional data flow. Composables render immutable `UiState`; ViewModels expose `StateFlow`; repositories own data.
- **DI:** Hilt. **Async:** Coroutines and Flow. **Persistence:** Room (apps, categories, notes) and DataStore (settings).
- **Modules are added only when a phase needs them** (no empty modules):

```
app/                      Activity, navigation, DI wiring, phase-1 screens
core/designsystem/        SL tokens + components            (Phase 1–2)
core/database, datastore  Room + DataStore                  (Phase 3–4)
core/model, core/common   Shared models / utilities          (Phase 3–4)
feature/home, appdrawer, search, assistant, island, news, music, ...
integration/notificationlistener, mediasession, assistantbridge
```

A `build-logic` convention-plugin module will be added once there are 3+ Android library modules, to remove duplicated Gradle config.

## 6. SL assistant integration contract (draft)

Until the SL source is available, the launcher side will expose:

```kotlin
interface LauncherActionBridge {
    suspend fun execute(action: String, parameters: Map<String, String>): ActionResult
}
sealed interface ActionResult {
    data class Success(val message: String? = null) : ActionResult
    data class NeedsConfirmation(val prompt: String, val token: String) : ActionResult
    data class NeedsPermission(val permission: String) : ActionResult
    data class Failure(val reason: String) : ActionResult
}
```

Transport options depend on how SL is built: in-process module (Kotlin), bound service with a signature-protected permission (separate APK), or HTTPS backend. **Information needed:** SL's language/framework, speech-to-text and text-to-speech engines, AI provider, and how it currently executes commands.

## 7. Risks

1. No local compilation in the cloud container, so every build depends on CI. Mitigation: CI on every push; Android Studio locally.
2. AGP 9 is a recent major version. Some third-party Gradle plugins may lag. Mitigation: keep the plugin set minimal.
3. OEM launcher restrictions (Xiaomi, Oppo, Tecno, Infinix are common in Nigeria) can affect the role dialog, background limits and gestures. Mitigation: Settings fallback and device testing.
4. Licensing for news and music content: legitimate APIs/RSS only (Phase 8–9).

## 8. Roadmap

| Phase | Milestone | Depends on |
|---|---|---|
| 0 | Discovery (this document) | – |
| 1 | Project initialization, HOME role, navigation, DI, CI | 0 |
| 2 | Design system + component catalogue | 1 |
| 3 | Onboarding + real home screen (dock, pages, widgets, wallpaper) | 2 |
| 4 | Floating app drawer (Grid Bold) | 2, 3 |
| 5 | Unified search + command router | 4 |
| 6 | SL assistant bridge | 5, **SL source** |
| 7 | Dynamic Island / live activities | 3, notification access |
| 8 | News center | backend decision (18) |
| 9 | Music center | 7 (media sessions) |
| 10 | Notification center | 7 |
| 11 | Quick settings + device info | 2 |
| 12 | Productivity center | 3 |
| 13 | Customization studio + profiles | 2–4 |
| 14 | Privacy, security, backup | 13 |
| 15 | Performance on low-end devices | all features |
| 16 | Accessibility + responsiveness | all features |
| 17 | Monetization (Play Billing) | 13, 18 |
| 18 | Backend (only what is truly needed) | 8, 14, 17 |
| 19 | QA | all |
| 20 | Release | 19 |
