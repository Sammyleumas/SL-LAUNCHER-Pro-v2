# SL assistant bridge

How your existing SL assistant app can control SL Launcher. The SL app's source was not available while this was built, so this is the launcher side of the contract. The SL app needs the small client shown below.

## Security

- **Service:** `com.sllauncher.app.assistant.AssistantBridgeService` (exported).
- **Required permission:** `com.sllauncher.permission.EXECUTE_LAUNCHER_ACTIONS`, protection level `signature`.
- **Who can call it:** only an app **signed with the same key** as SL Launcher. Sign the SL app with the same upload/release key, and declare `<uses-permission android:name="com.sllauncher.permission.EXECUTE_LAUNCHER_ACTIONS"/>` in its manifest.
- **Validation:** every parameter is checked (types, ranges, lengths) before anything runs. Unknown actions are rejected.
- **Confirmation:** actions that change a lot (profile switch) or send something on the user's behalf (message reply) run only when `confirmed=true` is passed. That means the SL app must ask the user first.

## Protocol (Android Messenger)

1. Bind to the service: `Intent().setClassName("com.sllauncher.app", "com.sllauncher.app.assistant.AssistantBridgeService")`.
2. Send a `Message` with:
   - `what = 1`
   - `data = Bundle { "action": String, "params": Bundle<String, String> }`
   - `replyTo` = your Messenger
3. Receive a `Message` with:
   - `what = 2`
   - `data = Bundle { "status": "success" | "needs_confirmation" | "needs_permission" | "failure", "message": String }`

`message` is a user-facing sentence you can show or speak.

## Actions

| Action | Parameters | Notes |
|---|---|---|
| `open_app` | `name` (≤ 80) | Best match by label |
| `open_drawer`, `show_favorites`, `show_notifications` | none | |
| `open_settings` | `panel`: wifi, bluetooth, internet, airplane, display, battery, sound, hotspot, device | Opens Android's panel |
| `set_timer` | `seconds` (1–86400), optional `label` | Handled by the clock app |
| `set_alarm` | `hour` (0–23), `minute` (0–59) | |
| `play_music`, `pause_music`, `next_track`, `previous_track` | none | Any player with a media session |
| `read_news`, `read_current` | none | Returns text to speak |
| `change_wallpaper`, `open_camera` | none | |
| `apply_profile` | `name`, `confirmed` | Minimal, Work, Study, Gaming, Personal or a saved profile |
| `web_search` | `query` (≤ 200) | |
| `create_note` | `text` (≤ 2000) | |
| `add_task` | `text` (≤ 200) | |
| `flashlight` | `on`: true / false | |
| `reply` | `text` (≤ 500), `confirmed` | Latest replyable message notification |
| `text_command` | `text` (≤ 300) | Free text through the launcher's command parser |

## Kotlin interface (inside the launcher)

```kotlin
interface LauncherActionBridge {
    suspend fun execute(action: String, parameters: Map<String, String>): ActionResult
}
```

## What is needed from the SL app

1. **Its source code**, or answers to these: its language or framework, its speech and TTS engines, its AI provider, and how it runs commands today.
2. Whether it should **replace** the launcher's built-in voice UI or **call into** the launcher (this bridge).
3. Both apps must be **signed with the same key** for the bridge to accept calls.
