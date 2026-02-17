# SimpMusic-WearOS

WearOS-focused fork of SimpMusic, with standalone watch playback and a Wear-native UI built with Wear Compose Material 3.

## What This Repo Is
- A fork specialized for WearOS development.
- Not a mirror of upstream app priorities.
- Optimized for real on-watch usage (Galaxy Watch class devices), including standalone Wi-Fi playback.

## Current Wear Status
- Dedicated `wearApp` module.
- Wear screens: Discover, Home, Library, Playlist (local + YouTube), Song Details, Now Playing, Queue, Accounts, Login.
- Phone-assisted login bridge is implemented for devices where watch-side WebView is unavailable/unreliable.
- Playback pipeline has Wear-specific reliability hardening and resolver fallback tuning.
- Tested on: Galaxy Watch4 Classic, WearOS 5

## Recent Progress (Feb 2026)
- Migrated this fork onto `upstream/main` lineage and consolidated the fork to a single default branch: `main`.
- Added proper Wear-native rotary + visible list scroll UX using `rotaryScrollable` and `ScrollIndicator`.
- Expanded Library parity with new Wear screens/routes:
  - Liked songs
  - Recently played
  - Followed artists
  - Liked albums
  - Followed releases (artist singles/albums)
- Replaced oversized quick actions with compact chip-style actions better suited to round Wear displays.
- Revalidated repeatedly with:
  - `:wearApp:lintDebug`
  - `:wearApp:assembleDebug`
  - `:wearApp:testDebugUnitTest`
- Installed and verified on real watch hardware over wireless ADB.

## Architecture Notes
- Reuses shared core/data/media/service modules from the original app where practical.
- Adds Wear-specific app shell, navigation, auth bridge, network binding, and UX flows.
- Uses Wear Compose Material 3 components (not phone-style UI).

## Build

### Debug APK (Watch)
```bash
./gradlew_ws :wearApp:assembleDebug
```

APK output:
```text
wearApp/build/outputs/apk/debug/wearApp-debug.apk
```

### Install on Watch
```bash
adb devices -l
adb -s <watch-adb-id> install -r -t wearApp/build/outputs/apk/debug/wearApp-debug.apk
```

Optional reset app data:
```bash
adb -s <watch-adb-id> shell pm clear com.maxrave.simpmusic.dev
```

## Login on Wear
- Watch login can be initiated from the Wear app.
- Phone handles the Web-based account flow.
- Session/cookie is returned to the watch through Wear Data Layer.

## Known Constraints
- YouTube backend behavior changes over time and can break stream extraction paths.
- WearOS networking is less stable than phone networking (Bluetooth proxy, Wi-Fi handoffs, radio wakeups).
- Some heavy phone features are still being ported/tuned for watch hardware limits.

## Roadmap
- Path A: UI/UX stabilization and polish.
- Path B: feature parity pass (search, richer queue actions, improved startup session handling).
- Path C: larger standalone features (downloads/offline, deeper library workflows).

## Upstream
- Upstream project: `maxrave-dev/SimpMusic`
- This fork intentionally diverges in implementation details and priorities for WearOS.
