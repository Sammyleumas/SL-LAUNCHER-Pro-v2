# Phase 19: QA checklist

## Automated (runs on every push in CI)

| Suite | What it covers |
|---|---|
| JVM unit tests (`app`, `core:designsystem`) | Command parser (every brief example), home grid maths, floating panel sizing, slider maths, badges, window classes, island priorities, news text and dates, For You ranking, assistant bridge validation, backup parsing and validation, app categories, countdowns, clock timing, default-home logic |
| Compose UI tests (compiled in CI; run with `./gradlew connectedDebugAndroidTest`) | Search field, switch rows, chip selection, floating panel close, launcher setup screen |
| Android Lint | API levels, permissions, Compose correctness, locale handling |
| R8 release build | Shrinking and obfuscation succeed |

## Manual test pass (do on a real phone before each release)

Mark each item ✅, ❌ (with a bug note) or N/A.

### Launcher basics

- [ ] Install, open, complete onboarding; set as default home; press Home: SL opens
- [ ] Back on the home screen does nothing; Back closes drawer, search and folders
- [ ] Rotate to landscape and back; split-screen; nothing overlaps
- [ ] Reboot the phone: layout, widgets and settings survive

### Home screen

- [ ] Long-press and drag an app to a new cell; swap two apps; drop one app on another to make a folder
- [ ] Rename a folder; take an app out of a folder; remove a folder (its apps return to the grid)
- [ ] Move mode (item menu › Move) works with TalkBack
- [ ] Add a widget, including one that has a setup screen; resize it; remove it
- [ ] Pin a shortcut from Chrome ("Add to home screen"); open it
- [ ] Add and remove pages; change wallpaper
- [ ] Shrink the grid (Customize): no item is lost

### Drawer and search

- [ ] Swipe up opens the drawer; search "wha" finds WhatsApp; Enter opens the top result
- [ ] Categories, custom category, favourites, recent, hide and unhide, list view, alphabet index, sort
- [ ] App info, uninstall (system dialog), app shortcuts (default launcher only)
- [ ] Search: commands, contacts (after allowing), settings entries, web search, voice search, history

### SL assistant

- [ ] Mic permission asked on first tap; "open WhatsApp", "set a timer for 2 minutes", "play my music", "next song", "read the latest news"
- [ ] "Make my home screen minimal" asks to confirm; Undo in Customize restores the previous setup
- [ ] Interrupt while speaking; typing works with the mic denied

### Dynamic Island and notifications

- [ ] Without notification access: "Turn on Dynamic Island" card opens the right Settings screen
- [ ] Music from Spotify, Audiomack or Boomplay shows with working controls
- [ ] WhatsApp message: quick reply sends; an app without a reply action shows the explanation instead
- [ ] Incoming call shows the caller and the dialer's own Answer/Decline buttons
- [ ] Charging shows when plugged in; low battery shows at 15% or less
- [ ] Clock-app timer and SL focus timer show live countdowns
- [ ] Notification center: grouping, dismiss, clear all, mute app, hide content, history on and off

### News, music, productivity

- [ ] News loads; offline state shows when offline; save, share, listen, read full story
- [ ] Music: allow audio access, play, seek, shuffle, repeat, queue, playlist, favourite; lock-screen controls; unplugging headphones pauses
- [ ] Task with a reminder notifies at the due time; focus timer notifies at the end
- [ ] Calendar events show after allowing; Add event opens the calendar app

### Settings, privacy, backup

- [ ] Quick settings: flashlight toggles; brightness after granting; volume; panels open
- [ ] Device info values are plausible
- [ ] Export a backup, then clear app data, then import it: layout, notes and settings return; widgets need re-adding
- [ ] Theme, accent, font, icon pack (install any Nova/ADW pack) and profiles all apply

## Device matrix

Test at least the **bold** rows before release.

| Class | Example | Android | RAM | Result |
|---|---|---|---|---|
| **Entry-level** | Tecno Spark / Infinix Hot / Samsung Galaxy A0x | 11–13 (Go if possible) | 2–3 GB | |
| **Mid-range** | Samsung Galaxy A3x/A5x, Redmi Note | 13–15 | 4–6 GB | |
| High-end | Pixel 8+, Galaxy S2x | 15–16 | 8+ GB | |
| Oldest supported | Any phone | 8.0 / 8.1 | any | |
| Tablet or foldable | Any | 13+ | any | |

Watch for these OEM quirks:

- **MIUI and HyperOS:** the default-launcher dialog may be hidden (use the Settings fallback).
- **Transsion (Tecno, Infinix):** aggressive battery management can stop the notification listener. Check after a reboot.

## Accessibility pass (Phase 16)

- [ ] TalkBack: every control is announced with a name and role; island announces changes; drag items have Move alternatives
- [ ] Font size at maximum and display size at maximum: no clipped or overlapping text on home, drawer and settings
- [ ] "Remove animations" on: panels appear instantly; loaders are static
- [ ] Touch targets are at least 48dp (Accessibility Scanner app)
- [ ] Contrast: Accessibility Scanner reports no contrast issues in dark and light themes

## Performance measurements (Phase 15)

Run on the entry-level phone, release build:

```bash
adb shell am start -W -n com.sllauncher.app/.MainActivity               # cold start: record TotalTime
adb shell dumpsys gfxinfo com.sllauncher.app reset; (scroll the drawer 10 s); adb shell dumpsys gfxinfo com.sllauncher.app   # janky frames %
adb shell dumpsys meminfo com.sllauncher.app | head -20                   # TOTAL PSS
```

| Metric | Target (entry-level) | Measured |
|---|---|---|
| Cold start (TotalTime) | < 800 ms | |
| Janky frames while scrolling the drawer | < 5% | |
| Memory (TOTAL PSS) on home | < 120 MB | |
| Battery drain idle on home, 1 hour, screen off | ≈ 0% attributable to SL | |

These have **not been measured yet**. There was no physical device in the build environment.
