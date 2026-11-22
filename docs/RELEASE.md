# Phase 20: release

## Test builds (available now)

Every push to the development branch publishes the newest test build:

**https://github.com/Sammyleumas/ES-EL-LAUNCHER-/releases/tag/sl-launcher-latest** → `SL-Launcher-test.apk`

It is a **debug build**. It installs as "SL Launcher" next to any Play version (package `com.sllauncher.app.debug`) and includes the "SL Catalogue" design app. Do not distribute it to the public.

## 1. Create your upload key (once, on your computer)

```bash
keytool -genkeypair -v -keystore sl-upload.jks -alias sl-upload -keyalg RSA -keysize 4096 -validity 10000
```

Keep `sl-upload.jks` and its passwords somewhere safe and **outside the repository**. Losing them means you can't update the app unless Play App Signing resets the key.

## 2a. Sign locally

Create `keystore.properties` in the project root. It is ignored by git.

```properties
storeFile=/full/path/to/sl-upload.jks
storePassword=...
keyAlias=sl-upload
keyPassword=...
```

Then run `./gradlew bundleRelease`. The output is `app/build/outputs/bundle/release/app-release.aab`.

## 2b. Or sign in CI

In GitHub, open **Settings › Secrets and variables › Actions** and add:

| Secret | Value |
|---|---|
| `SL_KEYSTORE_BASE64` | Output of `base64 -w0 sl-upload.jks` |
| `SL_KEYSTORE_PASSWORD` | Keystore password |
| `SL_KEY_ALIAS` | `sl-upload` |
| `SL_KEY_PASSWORD` | Key password |

The next CI run produces a signed `app-release.aab` (in the `sl-launcher-release` artifact) and adds `SL-Launcher-release-signed.apk` to the test release.

## 3. Versions

- `versionCode` is the CI run number (`-PversionCode`). Locally it defaults to 1.
- `versionName` lives in `app/build.gradle.kts`. Bump it for each store release (1.0.0, 1.0.1, 1.1.0…).

## 4. Before the first Play upload

- [ ] Confirm the package name `com.sllauncher.app`. It can never change afterwards.
- [ ] Final launcher icon and feature graphic (the current icon is a placeholder mark).
- [ ] Re-check Google Play's current target API requirement (`targetSdk` is 36 today).
- [ ] News licensing settled (see `NEWS-SOURCES.md`).
- [ ] Privacy policy published at a public URL (draft in `PRIVACY_POLICY.md`).
- [ ] Data safety form: no data collected or shared. Network use is news only. Data is processed on the device.
- [ ] Permissions declarations. Notification access and contacts need clear in-app explanations (already in the app).
- [ ] Content rating questionnaire.
- [ ] Store listing text and screenshots (see `STORE_LISTING.md`).
- [ ] Manual QA pass on at least one low-end and one mid-range phone (see `QA_CHECKLIST.md`).
- [ ] Internal testing track → closed testing → production. New personal developer accounts must run a closed test with testers for 14 days before production.

## 5. After launch

- Watch Android vitals (crashes, ANRs, start-up time) in Play Console.
- Keep `mapping.txt` from each release (uploaded as a CI artifact) to read crash stack traces.

## Release notes (1.0.0, draft)

> SL Launcher 1.0: a bold, fast home screen.
>
> - Floating app drawer with categories, favourites and search
> - SL assistant: tap to talk or type, for apps, music, timers and more
> - Dynamic Island for music, calls, messages, timers and charging
> - News, music, notes, tasks and a focus timer built in
> - Themes, icon packs and switchable profiles
> - No ads, no account, your data stays on your phone
