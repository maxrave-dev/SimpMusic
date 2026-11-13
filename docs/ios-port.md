# iOS port — initial groundwork

This branch is a work in progress, not a runnable iOS release. The existing
Android/Desktop app remains the reference implementation. No IPA or Xcode host
application is provided yet.

## Compile the shared module for iOS

Initialize the pinned core submodule, then opt in to the Native targets on macOS:

```sh
git submodule update --init --recursive
./gradlew -PenableIos=true :composeApp:compileKotlinIosSimulatorArm64
```

The opt-in registers `iosArm64` (device) and `iosSimulatorArm64` (Apple Silicon
simulator). Both declare a static `ComposeApp` framework. Ordinary builds leave
these new composeApp targets disabled while the port is incomplete; existing
iOS targets in core are unchanged.

Once the dependency and source blockers below are resolved, link with:

```sh
./gradlew -PenableIos=true :composeApp:linkDebugFrameworkIosSimulatorArm64
./gradlew -PenableIos=true :composeApp:linkDebugFrameworkIosArm64
```

These are development commands, **not currently verified successful builds**.
The future Xcode host can use `:composeApp:embedAndSignAppleFrameworkForXcode`
with the same property from an Xcode build phase, following
[Kotlin's direct integration guide](https://kotlinlang.org/docs/multiplatform-direct-integration.html).
That task requires Xcode's build environment and is not a standalone CLI build.

## Implemented groundwork

- Shared clocks and UUID generation use Kotlin APIs instead of Java APIs.
- Desktop OS name lookup is behind `expect`/`actual`; common code no longer
  references `System.getProperty`.
- Playback time labels use common Kotlin arithmetic, preserving total minutes,
  zero padding, and truncation of milliseconds. The UI still localizes negative
  (unknown) durations. Boundary tests live in `commonTest`.
- iOS clipboard writes are dispatched to the main thread when necessary.
- iOS download paths use `Documents/Downloads` in the application sandbox and
  report directory creation failures instead of returning an unusable path.
- Commons IO and Coil's OkHttp backend are scoped to Android/JVM. Shared image
  cache code depends explicitly on Coil's cross-platform network core.

## Remaining blockers, in implementation order

1. **Resolve and compile the full dependency graph.** Registering a Native
   target does not make every dependency Native-compatible. In particular,
   `core/service/loginSync` declares Android/JVM only and uses Java sockets and
   Java cryptography. Audit transitive dependencies before adding iOS targets.
2. **Finish common-code portability.** Remaining examples include Java ZIP and
   file APIs in `AllExt` and `SharedViewModel`, Java bidirectional text handling
   in lyrics, Java random-access files in app logs, and Java date formatting in
   settings. The scraper also imports Java hashing/time APIs in common code.
   This list is not an exhaustive compiler diagnostic.
3. **Complete iOS platform implementations.** Most composeApp `expect` APIs
   still lack iOS `actual`s: login/cookies, file and photo pickers, sharing,
   player view, permissions, settings, and others. Network image loading needs
   an iOS backend and ImageLoader setup; network-core alone does not fetch images.
4. **Implement the player.** `core/data/.../ExpectMediaHandler.ios.kt` still
   throws `TODO("Not yet implemented")`. Implement the media handler using
   AVFoundation and verify stream resolution: the existing iOS extractor
   returns an empty NewPipe result and no live HLS URL. Then add audio-session
   handling, background playback, interruption handling, and lock-screen controls.
5. **Add the application host.** Once dependencies initialize successfully,
   add a Kotlin `ComposeUIViewController` entry, an Xcode application, and
   startup dependency injection. Verify browsing and playback on a simulator
   before configuring device signing. Avoid a host that starts directly into
   the current unimplemented media handler.

Core is a separate Git submodule. Changes there need their own feature commits
and a corresponding parent-repository gitlink commit. This initial groundwork
does not change the pinned core revision. Keep both repositories local until
publishing is explicitly requested.

## Validation

- An isolated JVM check compiled the actual `PlaybackDuration.kt` source with
  the locally cached Kotlin 2.2.20 compiler. All 10,008 boundary/random cases
  matched the previous Java formatting; negative input was rejected. This is
  a focused compatibility check, not a full project or Native build.
- An isolated JVM check compiled the common `Platform.kt` with its desktop
  actual, checked all OS labels and platform selection, and checked the Kotlin
  clock and UUID APIs. The older 2.2.20 test compiler required an ExperimentalTime
  opt-in in the temporary harness; the project's pinned Kotlin version is unchanged.
- On 2026-11-13, the simulator compile attempt timed out after four minutes
  during Gradle dependency resolution, before Kotlin source compilation. An
  offline diagnostic then failed in root-project configuration because
  `io.sentry:sentry-android-gradle-plugin:6.24.0` was not cached. This is not a
  Native compiler result. The check used the locally installed JetBrains JDK 21
  temporarily; the repository's Microsoft JDK selection was restored afterward.
- Full Gradle/Native validation and device testing are still required. The
  clipboard and sandbox adapters have not been executed on an iOS device.
