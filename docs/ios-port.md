# iOS port

The `ios-support` branches in this repository and `core` contain the Apple Silicon
simulator port. Android and Desktop retain their existing playback engines. iOS
uses AVFoundation with the shared queue and playback handler.

## Build and run

Requirements: macOS, Xcode with an iOS simulator runtime, Python 3, and JDK 21.
Initialize `core` with `git submodule update --init --recursive` on a fresh clone.

```sh
python3 scripts/build-ios-framework.py
xcodebuild -project iosApp/SimpMusic.xcodeproj -scheme SimpMusic \
  -configuration Debug -sdk iphonesimulator \
  -destination 'platform=iOS Simulator,name=iPhone 17 Pro' \
  -derivedDataPath /tmp/SimpMusicIosBuild \
  SKIP_KOTLIN_BUILD=YES CODE_SIGNING_ALLOWED=NO build
xcrun simctl install booted /tmp/SimpMusicIosBuild/Build/Products/Debug-iphonesimulator/SimpMusic.app
xcrun simctl launch booted com.maxrave.simpmusic
```

Alternatively open `iosApp/SimpMusic.xcodeproj` and run the shared `SimpMusic`
scheme. Its build phase builds the appropriate Kotlin framework and copies the
Compose resources. `SKIP_KOTLIN_BUILD=YES` is for a framework already built for
the matching configuration and platform. Device builds need a signing team;
device signing and App Store distribution have not been validated.

`build-ios-framework.py` uses `JAVA_HOME`, or Android Studio's bundled JDK when
none is set. It temporarily removes the repository's daemon vendor restriction
and restores the original criteria in `finally`. Its lock serializes builds
started through that script. Do not run a separate wrapper script that changes
the same criteria concurrently.

The application Native targets remain opt-in with `-PenableIos=true`. Useful
standalone checks, passed as arguments to `build-ios-framework.py`, include:

```text
:data:compileKotlinIosSimulatorArm64
:composeApp:compileKotlinIosSimulatorArm64
:composeApp:linkDebugFrameworkIosSimulatorArm64
:data:compileKotlinJvm
:composeApp:jvmTest
:data:compileAndroidMain
:composeApp:compileAndroidMain
:ktorExt:iosSimulatorArm64Test
:spotify:iosSimulatorArm64Test
:kotlinYtmusicScraper:iosSimulatorArm64Test
```

## Platform implementation

- Startup initializes Koin, Room, DataStore, logging, view models, Coil's Darwin
  image loader, and Compose resources before rendering the shared application.
- AVPlayer handles AAC/HLS, seek, queue edits, shuffle/repeat, audio sessions,
  interruptions, and route changes. Now Playing metadata and remote commands use
  MediaPlayer. Startup reads settings without blocking the main thread, and a
  queue selected during startup takes precedence over the saved queue.
- The iOS stream request uses an Apple client returning direct AAC and HLS. Its
  parameters are documented in [yt-dlp's client definitions](https://github.com/yt-dlp/yt-dlp/blob/master/yt_dlp/extractor/youtube/_base.py).
  Android/Desktop retain their NewPipe extraction path. Service-side changes,
  region restrictions, private tracks, and account requirements can still affect
  which tracks are playable; the Native playback request is anonymous.
- Foreground NSURLSession downloads save AAC atomically in the application
  sandbox. Playback prefers a completed offline file. Clearing transient player
  cache does not remove downloaded tracks.
- WebKit provides login pages and cookie reads; platform file/photo pickers,
  artwork persistence, Photos export, sharing, clipboard, and external links use
  native APIs. Deep links are forwarded into shared navigation.
- Backup exports a standard ZIP containing the database and preferences, with
  optional downloaded AAC files. Restore stages an archive and applies it on the
  next full app launch before Room/DataStore open. It does not overwrite an open
  database. Close the app through the app switcher and reopen after staging.
- Shared file/log access uses Okio. Date, number, and bidirectional lyric layout
  no longer depend on JVM APIs. iOS HMAC and Spotify TOTP use CommonCrypto, with
  known-vector simulator tests.

## Deliberate limits

Device pairing/library sync, Cast, microphone song recognition, system EQ,
wallpaper colors, and the Android/Desktop audio effects are unavailable on iOS;
their normal settings entry points are hidden. The player supports normal speed,
volume, sleep fade, queue, and repeat controls, without the two-player crossfade
or DSP chain of the other engines. Downloads are foreground tasks, without a
background NSURLSession implementation. iOS cannot merge or tag separate video
and audio downloads with the JVM FFmpeg path.

Account sign-in completion, physical-device interruptions/AirPlay, App Store
packaging, and long-running background downloads require separate validation.

## Verified on 2026-11-14

- `data` Native and JVM compilation, Compose Native framework linking, Android
  compilation of data/Compose, and all 70 Compose JVM tests passed.
- Simulator tests passed: HMAC (2), Spotify TOTP (1), and scraper/locale (14).
- Xcode built and installed the app on iOS 26 simulators (iPhone 17 Pro and
  iPhone 16e). Home browsing, artwork, Settings, and the Google login page loaded.
  No account credentials were entered or sign-in completion claimed.
- The Debug host's real AVPlayer smoke test passed 13 checks: readiness,
  advancing playback clock, system Now Playing metadata, pause, seek, next and
  previous, duplicate-track identity, empty queues, and track metadata events.
- An online YouTube track played for over a minute. Its position also advanced
  from 07:56 to 12:10 while the app was backgrounded and the simulator locked.
  The simulator did not display the lock-screen media panel, so interactive
  lock-screen controls remain unverified despite successful metadata publication.

To repeat the player check with a booted simulator and the Debug app installed:

```sh
python3 scripts/verify-ios-playback.py --device SIMULATOR_UDID
```

It generates a quiet local WAV in the app's sandbox, drives the actual AVPlayer,
prints a PASS/FAIL report, and leaves normal launches unaffected. `--url` accepts
an optional remote audio URL. The report lives at
`Documents/SimpMusic/ios-playback-smoke.txt` inside that simulator app container.
The backup/restore round trip, Photos permission flow, video-mode visuals, and
physical-device audio interruptions have not received end-to-end validation.
