# Permissions

SL asks for a runtime permission only when the user turns on or first uses the feature that needs it, and the feature explains why first. Saying no never breaks the rest of the launcher.

| Permission | Type | When it is asked | What it is used for | If refused |
|---|---|---|---|---|
| Default home app (role) | User choice | Onboarding or the home banner | Being the home screen; reading app shortcuts | SL works as a normal app; Home opens the old launcher |
| Notification access | Special access (Settings) | Notification center or island "Turn on" card | Island, notification center, quick replies, music controls, badges | Those features show a setup card; everything else works |
| `RECORD_AUDIO` | Runtime | First tap on the SL voice button | Speech recognition while the user talks to SL | Type to SL instead |
| `READ_CONTACTS` | Runtime | Enabling contact search or quick contacts | Contact search, starred contacts | Contacts are not searched |
| `READ_MEDIA_AUDIO` (13+) / `READ_EXTERNAL_STORAGE` (≤12) | Runtime | Opening the music library or enabling music search | Listing and playing local songs | Other apps' playback can still be controlled |
| `READ_CALENDAR` | Runtime | "Allow calendar access" in Productivity | Today's events in the overview | No events shown |
| `POST_NOTIFICATIONS` (13+) | Runtime | Setting a reminder or starting a focus timer | Reminder and timer alerts | Timers still run; no alert |
| `WRITE_SETTINGS` | Special access (Settings) | "Allow" in Quick settings | Brightness slider only | Brightness opens display settings |
| `INTERNET`, `ACCESS_NETWORK_STATE` | Normal | Never asked | News headlines and images; offline detection | n/a |
| `SET_ALARM` | Normal | Never asked | Handing timers and alarms to the clock app | n/a |
| `REQUEST_DELETE_PACKAGES` | Normal | Never asked | Opening Android's own uninstall confirmation | n/a |
| `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | Normal | Never asked | Keeping local music playing with a media notification | n/a |
| `BLUETOOTH` (Android 11 and older) | Normal | Never asked | Bluetooth on/off state in Device info | n/a |
| Accessibility service "SL Launcher Pro gestures" | Special access (Settings › Accessibility) | Settings › Gestures and focus, or the first double-tap | Only two global actions: lock the screen and open the notification shade. It subscribes to no events and cannot read the screen | Double-tap shows how to turn it on; swipe down opens SL's notification center instead |
| `PACKAGE_USAGE_STATS` | Special access (Usage access) | "Allow usage access" in Productivity › Screen time | Today's per-app screen time, shown on the phone only | Screen time shows a setup card |
| `ACCESS_NOTIFICATION_POLICY` | Special access (Do Not Disturb access) | Turning on "Do Not Disturb while focusing" | Priority-only Do Not Disturb during a focus session, restored afterwards | Focus sessions don't change Do Not Disturb |
| `SET_WALLPAPER` | Normal | Never asked | Applying SL's built-in soft wallpapers | n/a |

## Package visibility

SL declares `<queries>` for launcher activities, icon packs, the default dock apps, speech and text-to-speech services, equalizers and web links. It does **not** request `QUERY_ALL_PACKAGES`.

## What SL never does

- Its accessibility service performs only "lock screen" and "open notification shade". It has no event types, cannot retrieve window content and is off until the user turns it on. Google Play requires the listing and an in-app notice to explain this use (both are drafted).
- It never draws over other apps (`SYSTEM_ALERT_WINDOW`). The Dynamic Island lives inside the launcher.
- It never answers or ends calls itself. It only fires the calling app's own notification actions.
- It never toggles Wi-Fi, Bluetooth, airplane mode or hotspot. It opens Android's panels.
- It never listens in the background. There is no wake word.

## Signature permission it defines

`com.sllauncher.permission.EXECUTE_LAUNCHER_ACTIONS` (protection level `signature`) guards the SL assistant bridge. Only apps signed with the same key can hold it. See `ASSISTANT-BRIDGE.md`.
