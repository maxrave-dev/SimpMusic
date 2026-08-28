# CLAUDE.md - SimpMusic Project Guide for AI Agents

## 🌐 Language Rule

**Response language**: Always respond in **English**, and after each sentence, add a **Vietnamese translation in parentheses**.
Example: "Hello, how are you? (Xin chào, bạn khỏe không?)"

This applies to all conversations in this project. The user is using Max plan so token cost is not a concern.

## 📋 Project Overview

**SimpMusic** is a FOSS (Free and Open Source Software) YouTube Music client for Android and Desktop, built with Compose Multiplatform.

### Main Purpose
- Stream music from YouTube Music and YouTube for free, ad-free, with background playback
- Provide advanced features like Spotify Canvas, AI song suggestions, synced lyrics
- Support both Android and Desktop (Windows, macOS, Linux)

### Basic Information
- **Package name**: `com.maxrave.simpmusic`
- **Primary language**: Kotlin
- **UI Framework**: Jetpack Compose / Compose Multiplatform
- **Architecture**: Clean Architecture + MVVM
- **Build system**: Gradle (Kotlin DSL)

## 🏗️ Architecture

### Clean Architecture Layers

```
┌─────────────────────────────────────┐
│  Presentation Layer (UI)            │
│  - Jetpack Compose / Compose MP     │
│  - ViewModels (MVVM)                │
│  - UI States                        │
├─────────────────────────────────────┤
│  Domain Layer                       │
│  - Use Cases                        │
│  - Domain Models                    │
│  - Repository Interfaces            │
├─────────────────────────────────────┤
│  Data Layer                         │
│  - Repository Implementations       │
│  - Data Sources (Remote/Local)      │
│  - Database (Room)                  │
├─────────────────────────────────────┤
│  Service Layer                      │
│  - YouTube Music Scraper            │
│  - Spotify Service                  │
│  - AI Service                       │
│  - Lyrics Service                   │
│  - Discord RPC (Kizzy)              │
└─────────────────────────────────────┘
```

## 📁 Module Structure

### Root Modules

#### 1. **composeApp/**
- **Shared Compose Multiplatform module** - main module containing shared code
- Supports: Android, Desktop (JVM), iOS (future)
- Contains all UI (Compose) and business logic
- Source sets:
  - `commonMain/`: Shared code for all platforms
  - `androidMain/`: Android-specific code
  - `desktopMain/`: Desktop-specific code
- Can run **Desktop app directly** from this module

#### 2. **androidApp/**
- **Android-specific module** to build Android app
- Depends on `composeApp` as a shared module
- Contains Android-specific configuration:
  - AndroidManifest.xml
  - Android build configuration
  - Android resources (if needed)
  - Entry point for Android app

#### 3. **core/**
Contains core modules organized by functionality:

##### **core/common/**
- Shared utilities
- Extension functions
- Constants
- Helper classes

##### **core/domain/**
- Domain models
- Use cases
- Repository interfaces
- Business logic rules

##### **core/data/**
- Repository implementations
- Data sources (Remote & Local)
- Database schemas (Room)
- Data mappers

##### **core/media/**
- **media3/**: Media3 ExoPlayer integration (includes `CrossfadeExoPlayerAdapter` for DJ-style crossfade on Android, and `audio/EqualizerAudioProcessor` for the ten-band equalizer)
- **media3-ui/**: Media3 UI components
- **media-jvm/**: JVM media playback (libmpv via JNA — replaced VLCJ, which replaced GStreamer post-1.0.4)
- **media-jvm-ui/**: JVM media UI components

##### **core/service/**
Service modules:

- **kotlinYtmusicScraper/**: YouTube Music API scraper
- **spotify/**: Spotify Web API integration (Canvas, Lyrics)
- **aiService/**: AI features (OpenAI, Gemini integration)
- **autoEqService/**: AutoEq headphone correction profiles (index + fixed-band curves)
- **lyricsService/**: Lyrics fetching (LRCLIB, SimpMusic Lyrics, BetterLyrics)
- **listenTogether/**: shared listening rooms, wire-compatible with Metrolist (`MetrolistGroup/metroproto`)
- **kizzy/**: Discord Rich Presence
- **ktorExt/**: Ktor extensions for networking

#### 4. **crashlytics/** & **crashlytics-empty/**
- **crashlytics/**: Full version with Sentry crash reporting
- **crashlytics-empty/**: FOSS version without tracking

#### 5. **cast/** & **cast-empty/**
#### 6. **lastfm/** & **lastfm-empty/**
- **lastfm/**: direct Last.fm scrobbling for the Full build. KMP (android + jvm + ios), package `org.simpmusic.lastfm`. Signs `api_sig` with okio's MD5; talks to `ws.audioscrobbler.com/2.0/` over form-urlencoded POST
- **lastfm-empty/**: FOSS no-op stub with the identical public API — `isLastfmAvailable()` returns `false`, which hides the whole settings block. A FOSS build ships no API secret, so it ships no Last.fm code either
- Selected via `isFullBuild` in `core/data/build.gradle.kts` (playback hooks) and `composeApp/build.gradle.kts` (UI); credentials come from `LASTFM_API_KEY`/`LASTFM_SECRET` in `local.properties` via BuildKonfig, and are handed in with `configLastfm(key, secret)` at startup — the same shape as `configCrashlytics(context, dsn)`
- Auth is Last.fm's **web flow** on every platform: open `last.fm/api/auth/?api_key=X` with **no token**, the user approves in their own browser, Last.fm redirects to the callback with `?token=`, then `auth.getSession`. The app never sees a password. **Do not switch to the desktop flow** (`auth.getToken` first, then open the same URL with `&token=` on it): that tells Last.fm the app already holds the token, so it renders a "return to the application" page and the callback is never called — which looks exactly like a broken redirect
- The callback registered on the API account is `wordbyword://lastfm-auth`, handled by an intent-filter on Android and by Conveyor `url-schemes` + `WindowsProtocolRegistrar` on Desktop; the login screen also accepts the callback URL pasted by hand, for hosts where no scheme handler exists

#### 7. **cast/** & **cast-empty/**
- **cast/**: Google Cast support for the Full build (`media3-cast` + `play-services-cast-framework`, `CastOptionsProvider`, `CastIconButton` Compose wrapper for `MediaRouteButton`)
- **cast-empty/**: FOSS no-op stub with identical public API (package `org.simpmusic.cast`), keeping GMS out of F-Droid builds
- Selected via the `isFullBuild` Gradle property (same pattern as crashlytics) in `core/media/media3/build.gradle.kts` and `composeApp/build.gradle.kts` androidMain
- Playback handoff lives in `core/media/media3` (`cast/CastHandoffManager.kt` + `cast/CastStreamResolver.kt`): the session player is `CastPlayer.Builder().setLocalPlayer(forwardingPlayer).build()`; while remote, `CrossfadeExoPlayerAdapter` routes transport/getters to the receiver and pushes a resolved-URL queue window (googlevideo URLs resolved up-front via `StreamRepository`); crossfade/EQ/precache are force-disabled while casting

## 🛠️ Key Technologies

### Android/Mobile
- **Jetpack Compose**: Modern UI toolkit
- **Material Design 3**: Design system
- **Media3 (ExoPlayer)**: Media playback
- **Room**: Local database
- **Coroutines & Flow**: Async programming
- **Hilt/Koin**: Dependency injection

### Desktop
- **Compose for Desktop**: UI
- **libmpv** (mpv's C client API, bound with JNA): audio + video playback. Replaced VLCJ, which had replaced GStreamer post-1.0.4
- libmpv natives are bundled per platform via `./gradlew :composeApp:mpvSetupAll` into `mpv-natives/<os>-<arch>/`

### Networking & APIs
- **Ktor Client**: HTTP client
- **Kotlin Serialization**: JSON parsing
- **YouTube Music hidden API**: Data source
- **quickjs-kt** (`io.github.dokar3:quickjs-kt`): runs YouTube's player JS on-device to solve signature/`n` challenges
- **Spotify Web API**: Canvas and lyrics
- **OpenAI/Gemini API**: AI features

### Data & Storage
- **Room Database**: Local persistence
- **DataStore**: Preferences
- **Caching**: Offline playback support

### Third-party Integrations
- **SponsorBlock**: Skip sponsors
- **ReturnYouTubeDislike**: Vote information
- **LRCLIB**: Lyrics provider
- **BetterLyrics**: Additional lyrics provider (added in v1.0.4)
- **Sentry**: Crash reporting (Full version only)

## 📝 Development Guidelines

### Code Style
- **Kotlin coding conventions**: Follow Kotlin official guidelines
- **Compose best practices**: Single source of truth, unidirectional data flow
- **Clean Architecture**: Strict layer separation, dependency rule

### Module Dependencies
```
UI Layer (composeApp)
    ↓
Domain Layer (core/domain)
    ↓
Data Layer (core/data)
    ↓
Service Layer (core/service/*)
    ↓
Common (core/common)
```

**Dependency Rule**: Higher layer modules can only depend on lower layer modules, NOT vice versa.

### Working with UI
- Use **Jetpack Compose** for all new UI
- Follow **Material Design 3** guidelines
- State management with **StateFlow** or **State\<T>**
- Side effects with **LaunchedEffect**, **DisposableEffect**

### Working with Data
- Repository pattern for all data operations
- Use cases for complex business logic
- Mapping between Data models ↔ Domain models ↔ UI models
- Room for local persistence
- Ktor for network requests

### Research Before Implementation (MANDATORY)

Before implementing code, researching code, or answering technical questions, the AI agent **MUST** follow this research workflow:

#### Step 1: Look up official documentation
- Use **MCP Context7** (`resolve-library-id` → `query-docs`) to fetch up-to-date documentation for any library/framework about to be used
- Understand the latest API surface, breaking changes, and recommended usage patterns

#### Step 2: Evaluate pros, cons, and alternatives
- Use **WebSearch** to research:
  - Pros and cons of the library/approach
  - Alternative libraries or approaches that solve the same problem
  - Known issues, performance concerns, or deprecation notices
- Compare and evaluate whether the chosen library/approach is the best fit for this project

#### Step 3: Study OSS best practices
- Use **Grep** (on GitHub via web search) or **WebSearch** to find how well-known open-source projects implement similar features
- Verify the approach follows established best practices before adopting it
- Pay attention to patterns used in projects with similar architecture (Clean Architecture, Compose Multiplatform, etc.)

#### Step 4: Make a decision and justify
- Only proceed with implementation after completing steps 1-3
- If a library/approach has significant drawbacks or better alternatives exist, recommend the better option to the user before proceeding
- Document the rationale briefly when introducing new dependencies or patterns

**This workflow applies to**: Adding new libraries, choosing architectural patterns, implementing new features with unfamiliar APIs, answering "how should we do X?" questions, and evaluating technical approaches.

**This workflow does NOT apply to**: Simple bug fixes in existing code, minor refactoring, or tasks using libraries already well-established in the project.

### Verification After Code Changes
- **Do NOT build the app** to verify code changes. Instead, use **JetBrains MCP** tools (`get_file_problems`, `getDiagnostics`) to check for compile errors and warnings in real-time.
- Only run Gradle build when explicitly requested by the user or for final release verification.

### Testing
- Unit tests for Domain layer (Use cases)
- Repository tests with fake data sources
- UI tests with Compose Testing

## 🎯 Common Tasks

### 1. Add New UI Feature
**Location**: `composeApp/src/commonMain/kotlin/`
- Create Composable function in appropriate package
- Use ViewModel for state management
- Follow Material 3 design patterns

### 2. Add New API Endpoint
**Location**: `core/service/kotlinYtmusicScraper/`
- Implement endpoint in corresponding service
- Create data model for response
- Map to domain model

### 3. Add New Database Entity
**Location**: `core/data/src/main/java/.../database/`
- Define Entity with Room annotations
- Create DAO interface
- Update Database class
- Create migration if needed

### 4. Add New Use Case
**Location**: `core/domain/src/main/java/.../usecase/`
- Create use case class
- Inject repository dependencies
- Implement business logic
- Return Result/Flow

### 5. Work with Media Playback
**Location**: `core/media/media3/` (Android) or `core/media/media-jvm/` (Desktop)
- Media3/ExoPlayer + CrossfadeExoPlayerAdapter for Android
- libmpv (MpvPlayerAdapter / MpvPlayer / MpvLibrary) for Desktop
- Queue management in `core/data/src/.../mediaservice/`
- Playback controls

### 6. Add New Lyrics Provider
**Location**: `core/service/lyricsService/`
- Implement lyrics fetcher interface
- Add fallback logic
- Handle synced/unsynced lyrics

### 7. AI Features
**Location**: `core/service/aiService/`
- OpenAI integration
- Gemini integration
- AI lyrics translation
- Song recommendations

### 8. Add a New Icon

**Location**: `composeApp/src/commonMain/kotlin/com/maxrave/simpmusic/ui/icon/`

All icons are **Material Symbols Rounded** generated as Compose `ImageVector`s. There is no
`material-icons-extended` dependency and no XML icon drawable — do not add either back.

**Fetch it from Google's own generator** (it returns a ready `.kt` file, gzipped):

```bash
curl -sfL --compressed \
  "https://fonts.gstatic.com/render/v1/Material+Symbols+Rounded/24dp/<symbol_name>.kt?var=opsz,wght,FILL,GRAD,ROND@24,400,1,0,50" \
  -o <PascalName>.kt
```

Keep the axes identical for every icon so the set stays consistent: **Rounded, opsz 24, wght 400,
GRAD 0, ROND 50**, `FILL=1`. Use `FILL=0` only for the "off" half of a state pair (e.g.
`FavoriteBorder`, `AddCircleOutline`, `DownloadForOfflineOutlined`) — otherwise the empty and
filled states render identically.

**Then edit the downloaded file:**
1. `package com.example.test` → `package com.maxrave.simpmusic.ui.icon`
2. `public val <symbol_name>: ImageVector` → `val SimpIcons.<PascalName>: ImageVector`
3. Rename the backing field `_<symbol_name>` → `_<PascalName>`, and `name = "<symbol_name>"` → `"<PascalName>"`
4. For an icon that must flip in RTL, add `autoMirror = true,` to `ImageVector.Builder`

**Use it:** `SimpIcons.PlayArrow` — plus a per-icon import, `import com.maxrave.simpmusic.ui.icon.PlayArrow`.

#### Traps that have already cost time here

- **Each icon needs its own import.** `val SimpIcons.X` is an *extension property*, so importing the
  `SimpIcons` object alone does not bring it into scope. This is also what lets R8 drop unused icons —
  do not "simplify" it into a map or a `when`, that would ship all of them.
- **`ImageVector` is not a `Painter`.** `Icon`/`Image` have overloads for both, but `AsyncImage`
  (`placeholder`/`error`), anything drawing inside a `DrawScope`, and custom composables typed
  `Painter` do not — wrap with `rememberVectorPainter(SimpIcons.X)` there.
- **The response is gzipped** even when the request asks for `identity`; decompress by magic bytes.
- **Do not replace an icon whose colour carries meaning.** `baseline_downloaded.xml` (`#FF00A0CB`),
  `baseline_favorite_24.xml` (`#D10000`), `mono.xml`, `monochrome.xml` and the `holder*.png`
  placeholders stay as resources; a tinted neutral symbol is not equivalent.
- Verify a name exists before assuming: the Symbols codepoint list is at
  `google/material-design-icons` → `variablefont/MaterialSymbolsRounded[...].codepoints`. Legacy
  names like `favorite_border` and `thumb_up_alt` do still exist; `person_add_alt_1` does not.

## 📍 Important Files and Locations

### Configuration
- `build.gradle.kts` (root): Root build configuration
- `gradle/libs.versions.toml`: Version catalog for dependencies
- `settings.gradle.kts`: Module inclusion

### Main Application
- `composeApp/src/commonMain/kotlin/`: Shared Compose code
- `composeApp/src/androidMain/kotlin/`: Android-specific code
- `composeApp/src/desktopMain/kotlin/`: Desktop-specific code

### Database
- `core/data/src/main/java/.../database/`: Room database schemas
- Migrations in Database class

### Network
- `core/service/kotlinYtmusicScraper/`: YouTube Music API
- `core/service/spotify/`: Spotify API
- `core/service/ktorExt/`: Ktor utilities

### Resources
- `composeApp/src/commonMain/composeResources/`: Shared resources
- `composeApp/src/androidMain/res/`: Android resources
- Crowdin integration for translations

## 🔧 Build Variants

### Android
- **Full**: With Sentry crash reporting (module: `crashlytics`)
- **FOSS**: No tracking (module: `crashlytics-empty`)

### Desktop
- **Windows**: `.msi` installer
- **macOS**: `.dmg` (ARM and x86-64)
- **Linux**: `.AppImage` (DEB and RPM removed post-1.0.4)

## 🚨 Important Notes

### Privacy & Data Collection
- FOSS version: NO tracking
- Full version: Only Sentry crash reporting
- "Send back to Google" feature: Optional, only when user enables

### Platform-specific Considerations

#### Android
- Min SDK: Check `androidApp/build.gradle.kts`
- Target SDK: Latest stable
- Android Auto support
- Background playback with MediaSession

#### Desktop
- **Required Dependencies**:
  - libmpv: audio + video playback (bundled via `mpvSetupAll`; falls back to a system-wide libmpv when `mpv-natives/` has not been staged)
- **Minimum macOS: 15.0** — raised from 11.0 when VLC was replaced by mpv. mpv's macOS release builds target macOS 15 (96/98 arm64 dylibs declare `minos 15.0`; on Intel `libmpv` itself does), and Conveyor rejects a lower `LSMinimumSystemVersion`. No mpv artifact covers both architectures below 15.
- **Features**:
  - Deep link support (`simpmusic://` and `simpmusic.org`)
  - Mini Player window (always-on-top, resizable, draggable)
  - Crash dialog
  - Custom title bar (disabled in VM environments)
- **Limitations**:
  - No offline playback

### External APIs
- YouTube Music: Hidden/unofficial API (may change anytime)
- Spotify: Requires login for lyrics
- OpenAI/Gemini: User must provide API key
- SponsorBlock: Public API
- LRCLIB: Public lyrics API

## 🎵 Media Playback Architecture

### Desktop Player (libmpv — replaced VLCJ 2026-07-27)

**Location**: `core/media/media-jvm/src/main/java/com/simpmusic/media_jvm/mpv/`

- `MpvLibrary.kt` — JNA binding for libmpv's C client API, hand-mapped against client API 2.x. Struct layouts are read by raw offset, so a MAJOR client-API bump needs them re-verified
- `MpvPlayer.kt` — one handle per media item; `vo=libmpv` + software render context
- `MpvVideoFrameSource.kt` — mpv SW render API → immutable `BufferedImage` snapshots published via `StateFlow`, drawn by plain Compose `Image` (`MpvVideoFrames` in `media-jvm-ui`); replaced the `SwingPanel`-embedded `MpvVideoSurfacePanel` on 2026-08-01
- `MpvPlayerAdapter.kt` — the `MediaPlayerInterface` implementation; separate YouTube audio/video URLs are merged into ONE source with an `edl://...;!new_stream;...` URL (mpv's equivalent of Android's `MergingMediaSource`)
- Natives bundled per platform in `mpv-natives/<os>-<arch>/`, staged by `mpvSetupAll` (Linux slice is compiled from source — `scripts/mpv-linux/`)
- Supports crossfade transition with dual-player approach

#### Crossfade Transition (Desktop)
- Configurable duration: 1-15 seconds (default: 5 seconds)
- Skipped when the NEXT track will play as video (`isVideo()` + watch-video setting on) — same rule as Android since 2026-08-01
- Settings persisted via DataStore

### Android Player (Media3/ExoPlayer)

#### Crossfade & DJ-style Transition (added in v1.0.4)

**Location**: `core/media/media3/src/main/java/com/maxrave/media3/exoplayer/CrossfadeExoPlayerAdapter.kt`

- DJ-style crossfade with adjustable duration
- Requires 320kbps stream preference to enable DJ mode
- Auto crossfade mode (like AutoMix)
- `CrossfadeFilterAudioProcessor` for audio processing
- Edge cases: disabled for video, repeat one, last track

## 🤝 Contributing

### Code of Conduct
See `CODE_OF_CONDUCT.md`

### Pull Request Guidelines
1. Fork and create branch from `dev`
2. Follow coding conventions
3. Test thoroughly before submitting
4. Update documentation if needed
5. PR title: Clear and descriptive
6. PR description: Explain changes and reasoning

### Translation
- Use Crowdin: https://crowdin.com/project/simpmusic
- Don't edit translation files directly

## 📚 References

### Inspiration & Credits
- **InnerTune**: YouTube Music data extraction inspiration
- **SmartTube**: YouTube streaming URL extraction
- **SponsorBlock**: Sponsor skip functionality
- **LRCLIB**: Lyrics provider

### External Documentation
- [Compose Multiplatform](https://www.jetbrains.com/lp/compose-multiplatform/)
- [Material Design 3](https://m3.material.io/)
- [Media3 (ExoPlayer)](https://developer.android.com/guide/topics/media/media3)
- [Room Database](https://developer.android.com/training/data-storage/room)
- [Ktor Client](https://ktor.io/docs/client.html)
- [libmpv client API](https://github.com/mpv-player/mpv/blob/master/include/mpv/client.h)
- [mpv EDL format](https://github.com/mpv-player/mpv/blob/master/DOCS/edl-mpv.rst)

### Community
- Website: https://simpmusic.org
- Discord: https://discord.gg/Rq5tWVM9Hg
- GitHub Issues: Bug reports and feature requests

---

## 🎯 Quick Start for AI Agents

When working with this project:

1. **Always check layer dependencies**: Don't violate Clean Architecture rules
2. **Use existing patterns**: Review current code to follow established patterns
3. **Platform-aware**: Code in `commonMain` must work for both Android and Desktop
4. **Test thoroughly**: Especially critical for media playback and network code
5. **Consider privacy**: FOSS version must NOT have tracking
6. **Check external API stability**: YouTube Music API may change at any time

### When Encountering Issues
- Check Discord server for known issues
- Review recent commits and PRs
- View dependency graph: `asset/dependencies_graph.svg`
- Test on both Android and Desktop if code is in commonMain

### Platform-Specific Code Patterns

**Example: Desktop-only UI settings**
```kotlin
if (getPlatform() == Platform.Desktop) {
    // Desktop-specific UI or logic
}
```

**Example: Android-only features**
```kotlin
if (getPlatform() == Platform.Android) {
    // Android-specific UI or logic
}
```

## 📜 Changelog Summary (post-1.0.4)

### Architecture Changes
- **Desktop: GStreamer → VLCJ**: Completely replaced GStreamer with VLCJ for desktop audio playback
- **DEB/RPM builds removed**: Desktop Linux now only ships AppImage

### New Features (v1.0.4)
- **Android Crossfade & DJ-style transition**: `CrossfadeExoPlayerAdapter` with auto mode (like AutoMix)
- **BetterLyrics provider**: Additional lyrics source integrated into lyricsService
- **320kbps audio stream option**: Higher quality streaming preference
- **Parallel download**: Improved download speed
- **Character-level animated lyrics**: Word-by-word lyrics with spring animations
- **SimpMusic Chart**: Chart playlists integrated into Library screen
- **Favorites**: Liked songs feature with UI integration
- **Custom OpenAI base URL**: Support for compatible API endpoints

### New Features (v1.0.1 - v1.0.3)
- **Desktop Mini Player**: Always-on-top, resizable, draggable mini player window with volume/like controls
- **Analytics/Local Tracking**: Track top artists, albums, and tracks locally (no remote tracking)
- **Auto Backup**: Automatic backup settings
- **Custom Title Bar**: Desktop window control with transparency support
- **SimpMusic Lyrics voting**: Vote functionality for community lyrics

### New Features (post-1.0.4, dev branch)
- **Icons unified on Material Symbols (2026-08-03)**: `material-icons-core`/`material-icons-extended` are gone, and so are the XML icon drawables — every icon is now a generated `ImageVector` under `ui/icon/`, addressed as `SimpIcons.<Name>`. Two migrations fed into this: 59 icons replacing `Icons.*` (117 call sites), then 25 more replacing `painterResource(Res.drawable.baseline_*)` (167 call sites, 44 XML files deleted). `RippleIconButton`, `LiquidGlassIconButton` and `ActionButton` changed from taking `DrawableResource`/`Painter` to `ImageVector`. See **Common Tasks → Add a New Icon** for how to add one and which traps to avoid. Icons whose colour carries meaning (`baseline_downloaded`, `baseline_favorite_24`), the logos (`mono`, `monochrome`) and the `holder*` bitmaps deliberately stay as resources.
- **Deep link support**: `simpmusic://` and `simpmusic.org` URL schemes
- **Desktop Crash dialog**: Error reporting UI for desktop
- **Playback speed/pitch controls**: Redesigned UI with improved animations
- **VM environment detection**: Disable transparency and custom titlebar in VMs
- **Google Cast (2026-07, Full build only)**: `cast`/`cast-empty` module pair gated by `isFullBuild`; unified Media3 `CastPlayer` wraps the session `ForwardingPlayer`; `CastHandoffManager` pushes resolved-URL queue windows to the receiver with 403/expiry retry; Cast button in Now Playing top bar, "Playing on <device>" pill, crossfade/DJ/EQ settings gray out while casting; FOSS build stays GMS-free
- **Windows SMTC (2026-07)**: System Media Transport Controls on Windows via `jmtc`/`nowplayingcenter` 0.0.3 (forked JMTC). The native `SMTCAdapter.dll` was hardened against the 1.0.x crash (Sentry SIMPMUSIC-DESKTOP-7, ~95k events): COM apartment tolerates `RPC_E_CHANGED_MODE`, `MediaPlayer` kept alive process-wide, and every exported call is exception-guarded so nothing crosses the JNA boundary as "Invalid memory access". JMTC is confined to a dedicated thread (off the AWT EDT), and `MediaType.Music` is set before display properties so title/artist render (not just the app name). Enabled in `JvmMediaPlayerHandlerImpl` for `Platform.Windows` (Linux MPRIS unchanged; macOS uses NowPlayingCenter). DLL built by GitHub Actions (`windows-latest`) in the NowPlayingCenter repo.
- **VLC removed entirely (2026-07-27)**: `VlcPlayerAdapter`, `DefaultVlcDiscoverer`, `MacOsVlcDiscoverer` and `VlcModule` are deleted; `VlcModule.kt` became `DesktopPlayerModule.kt` (`loadVlcModule()` → `loadDesktopPlayerModule()`). The `vlcj` dependency, the `vlc-setup` Gradle plugin, every `vlcSetup*` task, the `vlc-natives/` tree and the VLC Conveyor inputs are all gone. `appResourcesRootDir` now points at `mpv-natives/`. libmpv is the only desktop backend.
- **Bundled libmpv (2026-07-27)**: two entry points, deliberately split. `:composeApp:mpvBundleAll` runs **on a Mac, once per mpv bump** — it turns upstream mpv builds into loadable slices in `mpv-natives/<os>-<arch>/`, packs them into tarballs and prints their SHA-256. Those are published to `maxrave-dev/simpmusic-files`. `:composeApp:mpvSetupAll` is what **CI** runs: it downloads those tarballs, verifies them against the digests pinned in `mpvNativesChecksums`, and unpacks them — no toolchain needed on the runner. Both workflows must call it before Conveyor, which is invoked by its own action and so never triggers the Gradle `dependsOn`.
  - Sources: shinchiro `mpv-dev-*.7z` (Windows — the only one shipping a real `libmpv-2.dll`), mpv's own release `.zip` (macOS), and **for Linux a from-source container build** (see below). On macOS **libmpv is statically linked into the `mpv` executable**; that PIE binary exports the full client API and is renamed to `libmpv.dylib`, with load-command paths repointed to `@loader_path`.
  - Do NOT lift `IINA.app/Contents/Frameworks` instead: IINA 1.4.4 ships a version-skewed pair (libmpv needs `_pl_log_create_349`, bundled `libplacebo.338.dylib` exports `_pl_log_create_338`) and that libmpv fails `dlopen` under both RTLD_NOW and RTLD_LAZY.
  - **Every `._*` sidecar must be stripped after unpacking** (`mpvSetupAll` does this). Tarring a slice on macOS writes each file's xattrs out as a companion `._name`; Conveyor then signs them as ordinary bundle members and seals them in `_CodeSignature/CodeResources`, but macOS folds `._name` back into the xattrs of `name` and deletes the sidecar the moment Finder touches the app — unzipping it **or** dragging it out of the DMG. The launched bundle is then missing every sidecar the seal expects and Gatekeeper reports "SimpMusic is damaged and can't be opened" (`codesign --strict`: `a sealed resource is missing or invalid`). Only macOS is affected: it alone seals the whole app directory and re-checks it at launch.
  - `MpvLibrary.bundledLibraryDirs()` resolves the staged folder: `mpv.bundled.path` → `compose.application.resources.dir` → `mpv/` found by walking up from the JAR → `mpv-natives/<os>-<arch>`.
- **Linux libmpv built from source (2026-07-28)**: the AppImage route is gone — `scripts/mpv-linux/Dockerfile` now compiles libplacebo 7.351 + FFmpeg 7.1.1 + mpv 0.41.0 on **Ubuntu 22.04**, and `mpvSetupLinuxCi` runs that container and copies `/out`. This deleted ~186 lines of DwarFS extraction, closure pruning and rpath rewriting from `composeApp/build.gradle.kts`.
  - **Why the AppImage could never work**: every prebuilt Linux mpv targets "run mpv as its own process". `mpv-AppImage` ships its own glibc + `ld-linux`, and its "libmpv.so.2" was really the `mpv` **PIE executable** — glibc refuses to `dlopen` a PIE outright (`DF_1_PIE`), and even patched past that, its glibc 2.43 collides with the one the JVM already mapped. It only ever appeared to work on dev machines because JNA silently fell back to a system-wide libmpv. **Always log the resolved path (`NativeLibrary.getInstance(name).file`)** — that is the only thing distinguishing "using the bundle" from "quietly using /usr/lib".
  - The container build targets glibc **2.34** → runs on Ubuntu 22.04 / Debian 11 and newer. Vulkan/shaderc/glslang/D3D11 are disabled in libplacebo and X11/Wayland/GPU in mpv, since playback goes through the software render API; that also drops `libshaderc`/`libglslang`/`libSPIRV-Tools` (the bulk of the old bundle) and removes libsixel entirely, which had been aborting the JVM.
  - `stage.sh` deliberately does **not** bundle `libc`/`libm`/`libstdc++`/`ld-linux`, sets `DT_RPATH` (not `DT_RUNPATH` — RUNPATH is not inherited by transitive dependencies), and fails the build unless a `dlopen` + `mpv_initialize` smoke test passes.
  - mpv built with `-Dlua=disabled` has no `ytdl_hook`, so the `ytdl` option genuinely does not exist there; `MpvPlayer` uses `optionalOption()` to treat `MPV_ERROR_OPTION_NOT_FOUND` as success.
- **Last.fm scrobbling (2026-07-30, Full build only)**: `lastfm`/`lastfm-empty` module pair gated by `isFullBuild`, following the `cast`/`crashlytics` shape. `LastfmScrobbler` (in `core/data/.../lastfm/`) lives in `commonMain` and is driven by both player handlers, because Android and Desktop run entirely separate ones. It sends `track.updateNowPlaying` where the Discord RPC is updated, and `track.scrobble` off the existing 5-second position-persist tick — a track over 30s scrobbles at half its length or 4 minutes, whichever comes first.
  - **`status="ok"` does not mean accepted.** Last.fm answers OK while discarding a scrobble and only says so in `ignoredMessage`: code 1 = artist name filtered, 2 = track name filtered, 3/4 = timestamp too far past/future, 5 = daily limit. Codes 1 and 2 are how bad metadata surfaces, so they are logged loudly rather than dropped.
  - **The two auth flows are not interchangeable, and picking the wrong one silently kills the callback.** Web flow: send the user to `last.fm/api/auth/?api_key=X` with no token; Last.fm mints it and redirects to the registered callback with `?token=`. Desktop flow: call `auth.getToken`, then open that URL with `&token=` already on it; Last.fm then shows "return to the application" and never redirects. SimpMusic uses the **web** flow because it has a registered callback and deep-link handlers on every platform.
  - **The callback token does NOT travel through navigation.** `App.kt` hands it straight to `SharedViewModel.completeLastfmLogin()`, and `LastfmLoginScreen` closes itself by watching the stored session key. Navigating to the login screen with the token instead pushes a *second* copy on top of the one the user opened their browser from, so the `navigateUp()` after a successful login only peels off that copy and lands back on a login screen — it looks exactly like "logged in but still stuck on the login screen". The other three login screens never hit this because they embed a WebView and never leave the app; Desktop has no real WebView (`Cookies.jvm.kt` is a placeholder), which is why Last.fm uses the system browser at all.
  - **`toSortedMap()` does not exist in common Kotlin** (it is a JDK collection) — sort the signature parameters with `entries.sortedBy { it.key }`.
  - **`format` must be excluded from `api_sig`.** Parameters are sorted by name, concatenated `<name><value>`, secret appended, MD5'd — but signing `format` (or `callback`) yields "Invalid method signature supplied" (code 13) on every request.
  - Error codes worth branching on: `9` invalid session key → clear the stored session and make the user log in again; `11`/`16`/`29` → transient, retryable; everything else is a malformed request.
  - Two places where Last.fm's own docs contradict themselves, resolved conservatively: `timestamp` is the time the track **started** (the method page says started, the scrobbling guide says finished — every scrobbler in the wild sends the start), and `duration` is **always sent** (optional on one page, required on the other).
  - Responses are parsed as loose `JsonObject`s, not `@Serializable` classes: Last.fm's JSON is a translation of its XML, so numbers arrive as strings, attributes hide under `@attr`, and a field is an object with one entry but an array with several.
- **JNA open flags are POSIX-only (2026-07-28)**: `MpvLibrary` passes `OPTION_OPEN_FLAGS = 2` (RTLD_NOW without RTLD_GLOBAL) **only when not on Windows**. JNA forwards the value verbatim to `LoadLibraryEx`, where `2` means `LOAD_LIBRARY_AS_DATAFILE`: the DLL maps as plain data, imports never resolve, and `GetProcAddress` returns nothing — surfacing as the misleading `Error looking up function 'mpv_client_api_version': The specified module could not be found`.
- **Desktop URL schemes were never actually registered (2026-07-31)**: `url-schemes` belongs at the **top level** of `app` in `conveyor.conf`. Conveyor binds it on `AppConfig`, not on `MacConfig`/`WindowsConfig`/`LinuxConfig` — compare `MacConfigAccess.getUrlSchemes()` (reads `appConfig`) with the `getFileAssociations()` beside it (reads `mac`). It had been written as `mac.url-schemes` / `windows.url-schemes` / `linux.url-schemes` since May 2026; HOCON accepts unknown keys silently, so all three sat inert and **every packaged build shipped with no `CFBundleURLTypes` at all** — macOS never routed `simpmusic://` either, not just the Last.fm callback. Proven by diffing two `mac-app` builds that differed only in where the key was written. The same misplacement had parked `desktop-file.Categories` / `Comment[en]` / `StartupWMClass` *beside* the `"Desktop Entry"` group instead of inside it, so those never reached the generated `.desktop` either. Two more links in the same chain: the argv filter in `runDesktopApp` matched a fixed list (`simpmusic://`, `http://`, `https://`) and therefore discarded `wordbyword://lastfm-auth?token=…` on Windows and Linux — it now matches any `scheme://` — and the AppImage's own `.desktop` (written by `packageConveyorAppImage`, which is what actually reaches users since AppRun installs it into `~/.local/share/applications`) now declares `x-scheme-handler/wordbyword` alongside `simpmusic`.
- **Bundled glib disabled `java.awt.Desktop` on Linux (2026-07-31)**: `mpv-natives/linux-x64/lib/libglib-2.0.so.0` is glib 2.72 (built on Ubuntu 22.04) and is missing from `SYSTEM_LIBS` in `scripts/mpv-linux/stage.sh`, so it ships in the bundle and claims the glib soname the moment JNA loads libmpv at startup. AWT's `XDesktopPeer.init()` can then no longer dlopen the **system** `libgio-2.0.so.0`: on a glib 2.80 host (Ubuntu 24.04) it dies with `libgobject-2.0.so.0: undefined symbol: g_dir_unref`, and the JDK reports the whole Desktop API unsupported for the rest of the process. All 23 external-link call sites broke at once — `openUrl()` was an `if` with no `else` so it silently did nothing, while Compose's `LocalUriHandler` calls `Desktop.getDesktop()` on its first line and threw `UnsupportedOperationException` straight out of the click handler, crashing the app. Arrived with the from-source Linux mpv build (2026-07-28); before that JNA quietly fell back to a system-wide libmpv, so the system glib was the only one mapped and links worked. Worked around by calling `Desktop.isDesktopSupported()` at the top of `runDesktopApp` — `XDesktopPeer` caches that probe, so running it before libmpv loads lets the system gio/gobject win the soname race. `OpenUrl.jvm.kt` additionally gained a per-OS launcher fallback (`xdg-open` → `gio open` → `$BROWSER`) and a toast, so it can no longer fail in silence. **The actual cure is to stop bundling glib** — add it to `SYSTEM_LIBS`, which needs the Linux tarball rebuilt, republished and re-pinned in `mpvNativesChecksums`.

- **Crossfade skips video tracks (2026-08-01)**: both `CrossfadeExoPlayerAdapter` (Android) and `MpvPlayerAdapter` (Desktop) skip the crossfade path when the NEXT track will play as video (`isVideo()` + watch-video setting on — the same condition that builds a merged audio+video source). The merged two-URL source is error-prone to prepare mid-fade and used to cut the outgoing song short or jump straight to the video at 0:00; such transitions now take the normal (non-crossfade) path. **Update 2026-08-05**: the CURRENT-track check — removed on Android in commit `9da155d7` because its old shape ignored the watch-video setting — is back on both platforms as `isCurrentTrackVideo()` (`watchVideoEnabled && isVideo()`, symmetric with `isNextTrackVideo()`), so a video also plays out to its last frame instead of fading out under the incoming song.
- **Desktop video renders through Compose, SwingPanel removed (2026-08-01)**: `MpvVideoSurfacePanel` (JPanel + `SwingPanel` embedding) became `MpvVideoFrameSource` — the mpv SW render loop is unchanged, but finished frames are published as immutable `BufferedImage` snapshots on a `StateFlow` and drawn by a plain Compose `Image` (`MpvVideoFrames` in `media-jvm-ui`, converted with `toComposeImageBitmap()` off the UI thread; the UI reports its size via `setTargetSize()`). This kills the whole SwingPanel bug class: always-on-top z-order, one-frame-late repositioning while scrolling (the flicker that exposed the transparent window), and AWT's single-parent rule that made NowPlaying/Fullscreen/Artist screens fight over the one panel (video "randomly missing until next/prev"). `MpvPlayerAdapter.currentVideoSurface: StateFlow<Component?>` is now `currentVideoFrames: StateFlow<MpvVideoFrameSource?>` and is set unconditionally during crossfade — the old null-guard kept a dead panel from a released player on screen (the "black video" bug).
- **macOS desktop audio moved to `ao=avfoundation` (2026-08-01)**: `MpvPlayer` now pins `ao` to `"avfoundation,"` when `Platform.isMac()`, because **`ao_coreaudio` leaks a process-wide CoreAudio listener onto a freed `struct ao`** and takes the whole JVM down the next time an audio device appears or disappears — Sentry-visible as `EXC_BAD_ACCESS` on the `HALC_ProxyNotification Call Listener Queue`, reproduced by simply plugging in headphones. Windows (wasapi) and Linux (pulse/pipewire) are untouched.
  - The chain, all upstream and **still present in mpv master as of 0.41.0**: `ao_coreaudio.c` `init()` registers `AudioObjectAddPropertyListener(kAudioObjectSystemObject, …, hotplug_cb, (void *)ao)` on the *system* object, but its failure label is bare (`coreaudio_error: return CONTROL_ERROR;`). An init that fails any later step (`ca_init_chmap`, `init_audiounit`) therefore leaves the listener registered. `ao.c` then does `goto fail` → `ao_uninit()`, and `buffer.c`'s `ao_uninit()` calls `driver->uninit()` **only when `driver_initialized` is set** — a flag `ao.c` sets only *after* a successful init. So `unregister_hotplug_cb()` never runs while `talloc_free(ao)` does, and the orphaned listener outlives the handle for the rest of the process.
  - **Why SimpMusic hits this and plain mpv does not**: mpv initialises one ao per session; SimpMusic creates one handle per media item and runs two at once during a crossfade, so a single failed audio init anywhere in a session arms the crash. The crash then waits for an unrelated hotplug event, which is why the process can look healthy for an hour first.
  - Diagnosing it: the faulting address decodes as ASCII (`0x65636e6174736e49` = "Instance"), the signature of a freed allocation already handed to another object. No thread was tearing an ao down at crash time, which is what ruled out a teardown race and pointed at a listener leaked much earlier.
  - Accepted trade-offs, neither reproducible in testing on macOS 27: delayed mute (mpv#15014) and audio desync on playback-speed changes (mpv#14483). The trailing comma in `"avfoundation,"` keeps mpv's auto-probe as a fallback, so a failure degrades audio instead of silencing it. Remove the whole workaround once upstream frees the listener on the error path.
  - Related blind spot, still open: nothing calls `mpv_request_log_messages()`, so libmpv's own warnings (including failed audio init) never surface anywhere.
- **Sleep timer fade-out, and a second volume line to carry it (2026-08-14, issue #2330)**: the sleep timer used to end on a bare `player.pause()`. It now ramps to silence over 5 s on an equal-power (cosine) curve, then holds silence for `sleepFadeTailMs = 800` before stopping. The attenuation rides a **line of its own**, deliberately not `volume`: that one is the user's level and is reported back through `onVolumeChanged`, so ramping it would drag the UI slider down and — if the process died mid-fade — leave a silent app behind. `MediaPlayerInterface` gained `var sleepFadeFactor: Float` for it.
  - **Android** applies it as a new `SleepFadeAudioProcessor` in the Media3 chain (`arrayOf(crossfadeFilter, sleepFade)`), one instance per ExoPlayer, all reading the same `@Volatile` field. **Desktop** adds a third mpv level: `MpvPlayer` now blends `masterPercent × sleepPercent` into `ao-volume` while `fadePercent` keeps carrying the crossfade on the software `volume`. Crossfade and sleep fade therefore never share a variable and simply multiply.
  - **Tail exists because gain sits ahead of the sink.** AudioTrack buffers 250–750 ms, so pausing the instant the ramp hits zero still cuts at roughly −12 dBFS. Fade + tail are clamped to fit inside `remaining`, or the timer overruns the track and pauses inside the next one.
  - **The restore must not be queued by the caller.** `pause()` is asynchronous on both platforms *and* suspends partway through (`commitIncomingAsCurrent` joins a job), so a single-thread dispatcher does **not** order "pause then restore" — the suspension releases the thread and the restore runs first, re-opening the mixer over the last of the audio. Each adapter therefore restores the factor itself, in a `finally` at the end of its own pause task; the handler only restores on the cancelled path. The Android cast branch returns before that coroutine, so it clears the factor inline — skipping it leaves every sample multiplied by ~0 for the rest of the process.
- **Crossfade exclusions: short tracks and albums (2026-08-14)**: crossfade is now skipped when the current track is shorter than `max(20 s, crossfadeDuration × 3)` — at the default 5 s fade a 20 s track spent half its length fading. With duration on Auto the bar is computed from `resolveAutoCrossfadeDurationMs()` (20–45 s), never a hardcoded default. Separately, an opt-in setting (**off** by default) skips crossfade *between tracks of the same album*, so an album sequenced to run continuously still does.
  - Albums are recognised by a new `PlaylistType.ALBUM` (behaves exactly like `PLAYLIST` elsewhere; `AlbumViewModel` uses it for play, but **not** for shuffle — once shuffled the running order is gone). The handler snapshots the album's `mediaId`s into `MediaPlayerInterface.albumTrackIds` at load time, and crossfade is skipped only when **both** the current and the next track are in that set — which is exactly what keeps the edges intact: the last album track into the first radio track still fades. A **set of ids, not a count**, because shuffle reorders the queue including appended radio.
  - This works only because endless queue appends through paths that write `_queueData` directly and **never call `setQueueData`**, so the snapshot stays album-only while `listTracks` grows. Routing those appends through `setQueueData` would swallow the radio tracks into the set and disable crossfade for the whole queue.
  - Known limitation: the tag does not survive a restart — the queue-restore path hardcodes `PlaylistType.PLAYLIST` because `QueueEntity` has no column for it.
- **Every crossfade guard belongs on BOTH trigger paths (2026-08-14)**: crossfade starts from the **position-polling job** (`timeRemaining in 1..crossfadeDuration + prep`, polled every 200 ms), and *separately* from `handleTrackEndInternal()` on EOF. The EOF path returns early when `isCrossfading` is already set, so a condition added only there is **dead code with no symptom** — the feature silently does nothing. The existing video checks were on both paths; that is the pattern to follow.
- **Desktop: playback settings must reach every live handle (2026-08-14)**: speed, pitch, volume and the sleep fade all go through `MpvPlayerAdapter.applyPlaybackLevels()` and `forEachLiveHandle` (current + **secondary** + precached). `secondaryPlayer` is the easy one to miss — it is removed from `precachedPlayers` before being promoted, so it belongs to neither collection, and since `ao-volume` is shared process-wide on Windows a missed handle does not just stay wrong, it *undoes* the others. Speed used to be applied only to `currentPlayer` and only re-asserted in `endCrossfadeAudio()`, so changing it and skipping to the next track reverted to 1.0x.
- **Desktop pitch re-enabled (2026-08-14)**: the pitch row was hidden on Desktop with the note *"LibVLC doesn't support independent pitch control"* — stale since the mpv migration. mpv shifts pitch with its `rubberband` filter, which the codebase already drove for AutoMix key matching (`MpvPlayer.setPitchScale`, label `simpDjPitch`). It is applied only while crossfade is **off**: crossfade owns mpv's `af` chain and clears it after every transition, so the two cannot both drive it — the UI already locked the control in that case. `installCrossfadeChain` returns whether mpv accepted the filter; a build without rubberband logs a warning instead of firing `af-command` at a filter that is not there.
- **Seeking mid-crossfade (2026-08-14)**: `seekTo(positionMs)` was the only transport command that did not handle `isCrossfading` — on both platforms. Two bugs at once: the outgoing track kept playing because nothing cancelled the crossfade, and the seek landed on the *wrong* track, since position updates during a crossfade are read from `secondaryPlayer` while the seek went to `currentPlayer`. Both now commit the incoming track as current first, the way `pause()` does.
- **All mpv property writes run on the player thread (2026-08-14)**: `volume`, `sleepFadeFactor`, `seekTo(positionMs)` and `playbackParameters` used to write from the caller's thread. `MpvPlayer.release()` flips `isReleased` synchronously and *then* spawns `Mpv-Release` to `mpv_terminate_destroy`, so a caller that passed the `isReleased` check could still be inside `mpv_set_property` when the core died — the same use-after-free already documented in the `release()` join comment. Confining every write to the one thread that releases handles closes the window; `MpvPlayer.applyVolume()` additionally holds `volumeLock`, which is the only way to cover the event pump's `AUDIO_RECONFIG` path (that thread cannot hop).

- **Clear listening history sweeps the whole cached library (2026-08-16)**: one button in Settings → **Listening history** (its own section, above Storage, since Storage is Android-only) wipes `playback_event` and then everything the app kept only because it happened to render it once. Order is the feature: containers first, songs last — sweeping songs alone deleted **0**, because 11 322 of 12 221 were held alive by playlists nothing pruned. The chain is pin-live-queue → `playback_event` → artists (`followed = 0`) → their `notification` + `followed_artist_single_and_album` → podcasts (`isFavorite = 0`, cascades to episodes) → albums → playlists → songs → satellites already orphaned before this ran → `checkpoint` + `VACUUM` → unwind the queue pin. On the owner's 57 MB database: 10 607 songs, 470 artists, 238 playlists, 50 albums, 8 podcasts, 8 818 satellite rows; file down to ~12 MB.
  - **Kept by state, not by reference.** `song.liked` is the *entire* Favorites feature and `song.downloadState` is the *only* link between a downloaded file and its row — neither is a foreign key, so the literal "referenced by nothing" reading deletes both libraries. `downloadState = 0` also spares a download still in flight. Same idea one level up: albums/artists that back a liked or downloaded song are spared so their pages still render offline.
  - **`NOT IN` over a nullable column silently matches nothing.** `local_playlist.youtubePlaylistId` is nullable and mostly NULL; `x NOT IN (…, NULL, …)` is NULL, never TRUE, so the playlist sweep deleted **0 rows and reported no error**. Every such subquery needs `WHERE <col> IS NOT NULL`. This is the same failure shape as the crossfade guard that was dead code on one of two trigger paths.
  - **`_` is a LIKE wildcard and videoIds are full of it.** Ids are matched as quoted tokens inside JSON columns (`'%"' || id || '"%'`), which is sound against the real shapes — including `List<Map<String,String>>` columns, where the id is still a quoted value. But 1 748 videoIds contain `_`, so every pattern escapes `\`, `_`, `%` via nested `replace()` and declares `ESCAPE '\'`.
  - **The DELETE re-checks the orphan conditions** instead of trusting the precomputed id list: the user keeps liking and browsing while the sweep runs, and `pair_song_local_playlist` cascades on song deletion, so a song added to a local playlist mid-sweep would otherwise be pulled straight back out of it.
  - **The live queue is pinned to disk first**, deliberately ignoring `saveRecentSongAndQueue`. The `queue` table is only written on pause / track change / exit and only when that setting is on, so the song playing through the speakers looked orphaned. The pin is removed afterwards when the setting is off.
- **`@RawQuery` cannot VACUUM — Room routes it to a read-only connection (2026-08-16)**: KSP generates `raw()` as `performSuspending(__db, isReadOnly = true, inTransaction = false)`, Room takes a reader connection for that, and readers are opened with `PRAGMA query_only = 1`. `VACUUM` there fails with *"attempt to write a readonly database"* — while `PRAGMA wal_checkpoint` is accepted on the very same connection, which is exactly why the long-standing `DatabaseDao.checkpoint()` works and hid this. `vacuum()` therefore lives on `MusicDatabase` as `useWriterConnection { it.execSQL("VACUUM") }`, with `checkpoint()` immediately before it so the WAL the deletes just filled is folded back in first. `execSQL` opens no transaction, which SQLite requires for VACUUM. A VACUUM failure must not surface as an error: every delete has already committed.
- **Unfollowing an artist now cleans up immediately (2026-08-16)**: `ArtistRepositoryImpl.updateFollowedStatus` only flipped `artist.followed`, stranding that artist's `notification` and `followed_artist_single_and_album` rows forever — and once the artist row itself is swept, those rows lose any way back to an artist. It now deletes them on the unfollow path. The equivalent sweep stays in the clear-history button for rows stranded before this shipped.
- **Playback state was published inverted on both platforms (2026-08-16)**: `onIsLoadingChanged` wrote 2–3 times in a row — `Loading` unconditionally, maybe `Ready`, then `Loading` again from `stopBufferedUpdate`. `_simpleMediaState` is a `StateFlow` collected from another thread, so it conflates and the UI settled on the **last** write: `Loading` when buffering had just *finished*, `Ready` when it had just *started*. Since the adapters call it with `false` immediately after announcing `STATE_READY`, every track start and every resume ended with a spinner over playing audio. Compounded by `startBufferedUpdate()` not cancelling its predecessor, so each track leaked another 500 ms `Loading` emitter — the sibling `startProgressUpdate()` had already been fixed for exactly this (#2152). Desktop additionally compared `bufferedPercentage * duration` (percent × ms) against `currentPosition` (ms), off by ~100×. All of it predates the mpv migration (blames to the VLC-era handler), and Android was affected identically.
  - Two more, in the same family: `SharedViewModel`'s `Ended` branch pinned `current = -1L`, and the only formatter renders any negative as `NA:NA` — with the `Progress` branch ignoring negatives and the `Loading` branch restoring `total` without touching `current`, the player showed a correct duration next to `NA:NA`. And `play()` at end of queue called into a handle parked at EOF, which does nothing, so the button looked ignored; both adapters now rewind first.
- **Analytics is a nav tab, gated on local tracking (2026-08-16)**: previously reachable only from a 24 dp icon in the Library top app bar (with a "NEW" badge on it — the tell that it was buried). Now a fourth tab, **between Search and Library**, appearing only while local tracking is on. Three navigation components carry the tab list — `AppBottomNavigationBar`, `AppNavigationRail` (tablet/wide) and the Liquid Glass bar, which keeps **two** lists (`bottomNavScreens` for selection, `barTabs` for the sliding capsule; Search is a separate FAB) — and all of them must be updated or the tab exists but never renders. `BottomNavScreen.ordinal` is an **identity, not a position**: `AppNavigationRail` compared `selectedIndex == index` in one place and `screen.ordinal` in another, which only worked while the two numbers coincided; reordering exposed it.

- **`videoType` was never read from the API (2026-08-16)**: no parser ever read YouTube's own `musicVideoType`; every call site invented its own label, and `ResultVideo.toTrack()` even smuggled the **view count** into the column (`videoType = this.views`), which `ArtistScreen` then relied on to render "432K views". So `song.videoType` held `"Song"`, `"video"` or a view count depending on which screen wrote the row — nothing could ask "is this a video?" and get a true answer. The value is now read from `watchEndpoint.watchEndpointMusicSupportedConfigs.watchEndpointMusicConfig.musicVideoType` across every parser, carried on `SongItem`/`Track`/`Content`, and written through the existing update paths (`updateVideoTypeSongEntity`) rather than a migration. `Track` gained a real `views` field so the artist page stops depending on the smuggling.
  - Every comparison goes through `MusicVideoType` in `core/domain` — the one module both `kotlinYtmusicScraper` and `core/data` already depend on, so putting it in either would invert an existing edge. It **normalizes first**, keeping only real `MUSIC_VIDEO_TYPE_*` values, because rows written by older builds still hold the invented labels above; comparing those raw would call all of them videos.
  - **`null` is "YouTube did not say", never "audio".** `isVideoSong` resolves an unknown to false (matching Metrolist), ytmusicapi resolves the same unknown to "video" — callers that care must branch on `isKnown` first instead of assuming either.
  - The config **moved** in the 2026 web response: it now rides the overlay play button on search rows and the title column on playlist rows, so `MusicResponsiveListItemRenderer.musicVideoType` reads all three places and must fall through on the *value*, not the endpoint — falling through on the endpoint stops at the first row carrying a bare `watchEndpoint`, which is exactly the migrated shape.
- **Wrapped queue rows were being dropped — 82% of a logged-in radio (2026-08-16)**: `YouTube.next()` read `it.playlistPanelVideoRenderer` only, so every row YouTube ships as a `playlistPanelVideoWrapperRenderer` resolved to null and vanished in the `mapNotNull` that builds the queue. Measured on a live logged-in radio: **161 of 197 rows across four pages were wrapped**, so a 50-track first page arrived as 6. Reading `Content.track` (bare renderer, else `primaryRenderer`) fixes it.
  - **The wrapper only appears when authenticated.** Six anonymous requests eliminated the other variables one at a time — same seed, both client versions (`1.20260304` and `1.20260811`), and both body shapes (plain `NextBody` and the `isAudioOnly` + `params=wAEB` one) — every one returned zero wrappers, while the authenticated capture returned 48/50. All three radio paths use `youTube.next()` with `setLogin = true`, so the app has been getting the wrapped shape.
  - `Content.counterpart` is now parsed too: it holds the *other* rendition of the same recording, which is what powers the official client's Song/Video switch.
  - `nextCustom` (`setLogin = false`, hardcoded `RDAMVM$videoId`) is dead code — nothing calls it.
- **Radio queues can be kept audio-only (2026-08-16, issue #2334)**: an opt-in setting (**off** by default) that drops video entries from radio queues only — deliberately not a global "hide all video" switch, so a playlist or album the user picked still plays what it contains. Filtering lives in `SongRepositoryImpl` because that is where the three queue sources meet: `getRadioFromEndpoint`, `getRelatedData` and `getContinueTrack`.
  - **Substitution is impossible, so entries are dropped.** Every video that actually reached the measured radio was `MUSIC_VIDEO_TYPE_UGC` — a fan remix or mashup that exists only as a video and ships **no `counterpart`** to swap in. Official music videos never arrive as the primary rendition at all: YouTube already demotes those to the counterpart of the audio track. Rate is roughly 1 entry per 50, so the queue barely shortens.
  - Each source needs its own radio test, and two of them are not obvious. `getContinueTrack` must also accept **`RRDAMVM…`** — YouTube's other spelling for a single video's radio, which `isRadioQueueId` deliberately does not match because it never appears as a queue's own `playlistId`; and its `else` branch continues a real playlist, so only the `!fromPlaylist` branch may filter. `getRelatedData` is **always** radio but not directly: `next(videoId)` alone answers with just two rows — the song and an `automixPreviewVideoRenderer` pointing at its `RDAMVM…` radio — which `YouTube.next` then follows and splices in.
  - `getRadioFromEndpoint` is shared with the **shuffle** button on playlist and artist pages, which passes the playlist's own id — `isRadioQueueId()` returns false there, so shuffle is untouched.
- **Apple Music header reaches Desktop (2026-08-17)**: the immersive header on Album, Playlist, LocalPlaylist and Artist was gated behind `isMobilePortrait = getPlatform() == Platform.Android && wDP < hDP`, so Desktop only ever saw the old black-and-gradient layout. The gate is now open on all four, and the three list screens carry an **Apple Music desktop header**: square 280dp artwork on the left, and a right-hand column holding title → subtitle in the app accent (`seed`, standing in for Apple's brand red) → meta → the existing `[Shuffle][Play pill][Download]` cluster, re-aligned from centred to left. Everything below the header — `DescriptionView`, the track-count line, the list — is untouched, and the portrait header is unchanged on phones.
  - **The buttons that floated on the artwork have to move.** Back, like, search, `⋯` and LocalPlaylist's AI-suggest were overlays on an edge-to-edge image; at 280dp there is nothing to overlay, so they become a plain top row. Their code is moved verbatim rather than rewritten — each is wired to its own view model.
  - **`aspectRatio(1f)` is a phone-only assumption.** Artist's header used it, which on a 1400dp-wide window makes a 1400dp-tall block: the artwork — or a playing Spotify canvas — swallows the whole page. It is now `height((screenInfo.hDP / 2).dp)`, matching the other three. Two things must change with it or the result is worse than before: `ContentScale.FillWidth` → `Crop` (FillWidth scales a square source to the frame's *width* and shows only its top slice), and the colour scrim measured as `wDP * 0.7f` → `hDP * 0.35f` (70% of the frame's own height; on a wide window 70% of the width is taller than the artwork and covers everything).
  - **`HazeProgressive` kills the process on skiko.** haze 1.7.2's progressive path calls `ShaderBrush.createShader(Size)`, whose mangled signature does not match the Compose this build pins (`material3-multiplatform 1.12.0-alpha01` / skiko 0.148.1) — `NoSuchMethodError` thrown inside the draw pass, `RenderEffect.skiko.kt:234`. It is used in exactly one place (ArtistScreen's bottom fade) and had been latent because Desktop never rendered that branch. Now guarded with `getPlatform() == Platform.Android`; Desktop loses only the blur, since the colour scrim is a separate box. Plain `hazeEffect` is fine on Desktop. Fixing it properly means moving haze or Compose, and Compose is pinned for a reason — alpha02 bumps skiko to 0.148.2, which removes `Matrix33.makeTranslate` and crashes compottie.
  - **mpv decides the video's fit, not Compose.** `MpvVideoFrames` reports its box size through `setTargetSize`, and mpv scales *and letterboxes* each frame into exactly that size — the black bars are already pixels by the time Compose sees them, so no `ContentScale` can remove them. `MediaPlayerView.jvm.kt` had therefore been ignoring `cropToBounds` outright, with a comment that Desktop never renders the portrait canvas. Cropping is now mpv's `panscan` property (`0.0` letterbox … `1.0` cover), exposed as `MpvPlayer.setPanscan()` and applied from a `LaunchedEffect` so flipping the flag re-scales the running video instead of re-creating the handle.
- **The header gate is orientation, not platform (2026-08-17)**: the migration above shipped with `val isMobilePortrait = true` hardcoded in all four screens — a "temporarily forced open so the layout can be judged on Desktop" that also **replaced the body of the portrait branch** with the landscape header. Android portrait therefore rendered the desktop header, and Android landscape/tablet lost the old layout entirely. The gate is now `val isPortrait = screenInfo.wDP < screenInfo.hDP` and nothing else: a portrait window (phone upright, or a narrow desktop window) gets the edge-to-edge artwork header, a landscape one gets the side-by-side header. `getPlatform()` no longer appears in Album/Playlist — the one platform check left in this family is ArtistScreen's `HazeProgressive` guard, which is about skiko, not about layout.
  - The old black `angledGradientBackground` layout is **deleted** from all four screens, since "not portrait" now means "landscape header" rather than "legacy layout". That also retired `PauseCircle`/`PlayCircle`/`Sensors`/`ElevatedButton` and, in ArtistScreen, `CollapsingToolbarParallaxEffect` plus 11 other imports.
  - **One boolean was answering two questions** — "use the immersive treatment" (palette background, row dividers, blurred top bar) and "which header". That is why forcing it to `true` broke the header while looking harmless: everything else it gated genuinely does apply to both orientations, so those conditions are simply gone.
  - Artist keeps three values on the orientation: frame `aspectRatio(1f)` vs `height(hDP / 2)`, `ContentScale.FillWidth` vs `Crop`, scrim `wDP * 0.7f` vs `hDP * 0.35f` — both figures are 70% of the frame's *own* height, which is why they differ.
- **Liquid glass reaches Desktop, and its expect/actual is gone (2026-08-17)**: `io.github.kyant0:backdrop` is a **KMP** artifact declared in `commonMain.dependencies`, so Gradle had been resolving `backdrop-desktop` all along — the JVM side was simply never written. `LiquidGlass.jvm.kt` returned `Modifier` unchanged and `LiquidGlassContainer.jvm.kt` fell back to `clip(shape)`, so every glass surface on Desktop was a plain rounded box. Both actual pairs are now deleted: `PlatformBackdrop` is a `typealias` for Kyant's `LayerBackdrop` in commonMain, and the whole effect — `drawInteractiveGlass`, `GlassInteraction`, the observe-only press recogniser — is common code. The ~20 call sites keep the same names and did not change.
  - **The expect class was itself the blocker.** An `expect class` has no relationship to Kyant's `Backdrop`, so `drawBackdrop()` could not be called from commonMain and the 200-line effect was stuck in androidMain. Once the library exists on both targets the abstraction has nothing left to abstract, and dropping it is what let the code move. The alias survives only so call sites keep reading `PlatformBackdrop`.
  - Press/hold works with a mouse: it rides `pointerInput` + `awaitFirstDown`, not touch-specific APIs.
  - The landscape headers on Album/Playlist/LocalPlaylist now use glass for the back button and the like/search/`⋯` pill, matching portrait. Their buttons sit on the page background rather than on artwork, so the backdrop source is a `matchParentSize()` box carrying the palette colour — it takes part in no measurement and **must stay a sibling** of the buttons; nesting them inside the source is the render-feedback loop that crashes the RuntimeShader. The `[Shuffle][Play][Download]` cluster stays flat `White @12%` in both orientations, as it already was in portrait.
  - **The glass rim is `Highlight`, and its default is DIRECTIONAL — which is why small round buttons looked rimless.** `drawBackdrop`'s default is `highlight = { Highlight.Default }`, and `Highlight.Default` carries `HighlightStyle.Default`: a rim lit along a single direction (`angle = 45f`, `falloff = 1f`), not a rim around the outline. An elongated pill catches that sweep along its long edge and reads as glass; a 48dp circle catches a short arc of it and reads as nothing at all. `HighlightStyle.Plain` is the uniform one (`Color.White.copy(alpha = 0.38f)`, `BlendMode.Plus`). `liquidGlass`/`LiquidGlassContainer`/`LiquidGlassIconButton`/`drawInteractiveGlass` therefore take `highlight: Highlight = Highlight.Default` — the default keeps Kyant's own behaviour, so Android and the portrait branch are untouched. **The small round buttons pass `Highlight(width = 1.dp)`, NOT `Highlight.Plain` (corrected 2026-08-26).** `Plain` is uniform but reads as a flat wash; 1.dp is the smallest step that stays visible without looking like a plain border. Live examples: `AnalyticsScreen.kt:422` and the Apple Music player's Desktop dismiss button. Copy the value from the tree, not from this file's history.
    - Four builds were burned guessing at this before anyone read the library source. Things ruled out along the way, so nobody repeats them: hand-rolling the button as `Row + liquidGlass` (widening it to 96dp *does* make glass appear, because a longer edge catches the directional sweep — a symptom, not a fix), shrinking the lens radii through a `lensScale` parameter, moving the backdrop source between a `matchParentSize()` box and the content column, and painting the cover art into the source at low alpha (visible, but it silently changes the header design — do not). Swapping the back button and the pill between `TopStart`/`TopEnd` was the one useful experiment: the glass followed the *widget*, proving it was the surface's own geometry rather than its position.
  - **Proven on skiko, and Desktop is now ALWAYS glass (corrected 2026-08-26).** This entry originally read "unproven on skiko"; it has since shipped and runs. Desktop does not consult the `liquid_glass` setting at all — three call sites read `isLiquidGlassEnabled == TRUE || getPlatform() == Platform.Desktop` (`App.kt:427`, `App.kt:553`, `MiniPlayer.kt:179`), so the toggle is Android-only and Desktop is unconditionally on. New Desktop surfaces should therefore reach for `LiquidGlassIconButton`/`liquidGlass` by default rather than treating glass as the risky option. Kyant's desktop path does go through `SkikoRuntimeShader`, the same neighbourhood where haze 1.7.2's `HazeProgressive` throws `NoSuchMethodError` — that remains true of **haze**, not of Kyant's backdrop.
  - `LiquidGlassAppBottomNavigationBar` still has an **empty** JVM actual — Desktop navigates with the rail, not the bottom bar. It is unrelated to the above and was not touched.

- **Listen Together (2026-08-23)**: shared rooms where every client plays the same track at the same position, in the rooms **Metrolist already uses** — a SimpMusic client and a Metrolist client can sit in the same one. New KMP module `core/service/listenTogether` (android + jvm + iosArm64 + iosSimulatorArm64) speaking `MetrolistGroup/metroproto` over a Ktor WebSocket to `metroserver`. Nothing in the protocol layer may be "improved": the wire format is not ours.
  - **protobuf without protoc.** Messages are `kotlinx-serialization-protobuf` classes with `@ProtoNumber`; a protoc-generated Java layer would be JVM-only and strand Desktop and iOS.
  - **`encodeDefaults` must be `false`.** With it on, any payload carrying a null message field throws `'null' is not supported for optional properties in ProtoBuf` — and `PlaybackActionPayload.trackInfo` is null on *every* play, pause, seek and volume command. Matches proto3, protoc and the Go server.
  - **The capability handshake type names are not in the .proto.** `client_capabilities` / `server_capabilities` were read off metroserver's `protocol.go` and are pinned by a test.
  - **The server forces `IsPlaying=false` on EVERY `change_track`** — protocol default, not the host pausing. A guest that obeys it pauses the track it just loaded, which is the same bug on next, prev and end-of-song alike. The guest carries the room's previous intent across the change; a real pause arrives as its own command with the track unchanged.
  - **Play intent is decided BEFORE loading** and passed into `addMediaItem`, never corrected afterwards. Loading with a hardcoded `playWhenReady = true` and letting the transport pause it is a race the guest wins — it starts playing in a room the host has paused.
  - **`playWhenReady`, not `isPlaying`, everywhere intent is meant.** A buffering track reports `isPlaying = false` while already committed to playing, so comparing against it made the host publish PAUSE to the whole room on its own network hiccup, and made the track-change PLAY never fire.
  - **A joiner starts at the room's position, not at zero.** The state pushed on join carries the position as of the host's *last command*, which can be minutes old, so the guest also sends `request_sync` the moment it is in.
  - **Loading gaps are absorbed by position, not by waiting.** Each client resolves its own stream; the host publishes `position` with every command and `ServerClock` compensates the flight time, with a seek past `SEEK_TOLERANCE_MS = 750` (Metrolist's `HARD_SYNC_THRESHOLD_MS`). The buffer barrier only runs when someone stalls **mid-track** — `change_track` clears `BufferingUsers` server-side, so it never gates a track change.
  - **Guests may pause, and stay paused.** Forcing them back to room state makes pause impossible; pressing play calls `request_sync` so resuming lands where the room is now, not where this device stopped. Matches Metrolist's manager.
  - **`MediaPlayerInterface.crossfadeSuppressed`** turns crossfade off inside a room without touching the user's setting — a fade overlaps two tracks for seconds and drifts the room apart. Writing the DataStore value instead would lose the real preference on a process death mid-room.
  - `Track.toGenericMediaItem()` guesses song-vs-video from artwork aspect ratio and reads its own `maxresdefault.jpg` fallback as video, so room tracks are built from the room's own `TrackInfo` with `MERGING_DATA_TYPE.SONG` forced — otherwise the guest gets video with no sound where the host has audio.
  - **UI never sees the protocol.** `composeApp` talks to `ListenTogetherRepository` in `domain`; `ListenTogetherRepositoryImpl` in `data` is the only place that knows the service module exists.
  - Entry point is a top-bar icon on Home and Library carrying a dot while a room is live. Reconnects are capped at 5 attempts, then reported.

- **Apple Music lyrics style (2026-08-25)**: a second lyrics renderer, chosen by `LYRICS_STYLE` in DataStore (`CLASSIC` default | `APPLE_MUSIC`) from Settings → User interface, and **deliberately independent of `NOW_PLAYING_STYLE`** — it governs how a lyric line is drawn, so it applies everywhere lyrics are drawn: the Apple Music tab, the Classic and M3 Expressive players, and `FullscreenLyricsSheet`. `LyricsView` reads the setting itself via `koinInject` rather than taking a parameter, so none of its four call sites changed. Every sizing number is lifted from AMLL (`amll-dev/applemusic-like-lyrics`), whose stylesheet expresses everything in `em`: `0.4em` line padding, `~1.2em` leading, `0.5em`/`1.5em`/`0.3em` for the translation row. AMLL does NOT set the lyric font size — the host does — so 28sp is ours.
  - **Gated on Android 12, not degraded.** `Modifier.blur` is backed by `RenderEffect` (API 31) and is a documented **no-op** below it — no crash, no warning, just a page with no blur. `isLyricsBlurSupported()` (expect/actual, named after the capability rather than the OS version because Desktop answers differently) hides the option entirely below that, and `LyricsView` re-checks it: a DataStore restored from a backup can carry `APPLE_MUSIC` onto a phone that cannot draw it.
  - **`Modifier.blur(radius)` alone CLIPS.** The single-argument overload uses `BlurredEdgeTreatment.Rectangle`, so softened glyphs get sliced off square at the line's own bounds. `Unbounded` is required — and it is not sufficient on its own: the blurred Box must also be **wider than the text**, with the gutter applied INSIDE it. Gutter outside means the blurred box starts exactly where the glyphs start and there is no margin for the blur to spill into. This is the same problem AMLL solves with `margin: -1em; padding: 1em`, which widens the painted area without moving the layout; Compose has no negative padding, so the equivalent is to blur the wide box and inset the content.
  - Following from that, **`FullscreenLyricsSheet` must not add its own gutter on top**. It keeps a 50dp column for Classic; under this style it contributes `50 - AppleMusicLyricPaddingX` so the total is unchanged and the renderer owns the inner 20dp. Zeroing it instead leaves the lyrics sitting further out than that screen's own header and slider.
  - **`graphicsLayer { alpha = … }` re-introduces the rectangle.** Any alpha below 1 forces the node into an offscreen layer sized to its bounds, which crops the `Shadow` that by definition spills outside them. Glow intensity therefore rides in `shadow.color.alpha`, never in a layer. The glow itself is the word drawn in **transparent ink** so only its Shadow lands — that is what makes the light follow the glyph outline instead of boxing it.
  - **A word is drawn per CHARACTER.** With one `Text` per word, the smallest thing that can light up IS the word, no matter where the glow is attached — which is why it kept flaring whole words. AMLL splits into `characterElements` for the same reason. Intensity is a **continuous falloff from the playhead** (`1 - |progress - charCentre| / reach`, reach ≈ 1.5 characters) and the glow node is composed **unconditionally at alpha 0**: an `if` around it adds and removes the node, so A vanishes and B appears rather than one fading down as the other fades up.
  - **Emphasis for held notes is AMLL's, constants and all** (`initEmphasizeAnimation`): strength is `du/2000` **cubed** below the reference duration and **square-rooted** above it, `×0.6` capped 1.2 for scale, `×0.5` capped 0.8 for bloom, `×1.6`/`×1.5` on the closing word of a line. A linear ramp — the obvious first guess — makes ordinary words shimmer and held notes underwhelming, i.e. exactly backwards.
  - **Blur magnitude is ours, not AMLL's.** Their `min(5, (1 + distance) * 0.8)` is CSS px sized for their host's font; at this size it lands around 1.6dp one line out, which is invisible. Measured off the reference screenshots instead and expressed against the font size: `0.095em` per line, capped `0.45em`. Distance is asymmetric, which IS AMLL's: lines already sung carry a `+1` so the page behind the singer recedes faster than the page ahead.
  - Blur drops to zero while the user drags the list (`collectIsDraggedAsState`, not `isScrollInProgress` — the latter is also true for the player's own animated scroll). The sung line is anchored one PHYSICAL ROW from the top, not one item: a wrapped lyric is one item spanning several rows, so `animateScrollToItem(index - 1)` hangs the whole wrapped block above it. Scrolling is a spring, not a tween, so a one-line step and a six-line jump settle the same way.

- **Compose Hot Reload + MCP (2026-08-17)**: `org.jetbrains.compose.hot-reload` 1.2.0 is applied in `desktopApp` (root has `apply false`; `foojay-resolver-convention` in settings provisions the JBR). Run with `./gradlew :desktopApp:hotRunJvm --auto` — plain `jvmRun` does NOT hot-reload; the plugin creates separate tasks. `mainClass` resolves automatically from `compose.desktop.application.mainClass`, and the existing `tasks.withType<JavaExec>` block already covers `ComposeHotRun` (it extends JavaExec), so `mpv.bundled.path` reaches hot runs. The MCP server (`:desktopApp:hotMcpServerJvm`, registered in Claude Code local scope) exposes `status`/`reload`/`await_reload`/`take_screenshot`/`get_semantic_tree`/`click`/… — **measure UI with the semantic tree instead of guessing**: it returns exact bounds (it is how the capsule's 2px column overflow was found), while `take_screenshot` captures the window's screen rect, so an occluded window photographs whatever covers it. No hover tool exists: to see hover-only UI, temporarily force the state in code, reload, measure, revert. CHR cannot invalidate global state (Koin singletons, player adapters) — restart the app after touching those.

- **Ten-band equalizer on both platforms (2026-08-22)**: one stored curve — `equalizer_bands` (CSV), `equalizer_preamp`, `equalizer_enabled` in DataStore — drives two entirely different backends. Desktop writes mpv's `af`; Android runs `EqualizerAudioProcessor` (a Media3 `BaseAudioProcessor`) in the sink chain, ahead of the crossfade filter and the sleep fade. Both are Audio-EQ-Cookbook **peaking** biquads on the same ISO centres (31, 62, 125, 250, 500 Hz, 1, 2, 4, 8, 16 kHz) at Q 1.41, measured against ffmpeg's own `equalizer` filter at ±0.0000 dB at **all ten centres** (re-measured 2026-08-24; the earlier "from 125 Hz up" understated it) — which is what makes one curve, and one AutoEq profile, mean the same thing on both. UI is embedded in Settings → Playback (`EqualizerSection`), not a separate screen: a draggable curve rather than ten sliders, because the thing being edited is a shape.
  - **mpv's `af` has two owners, and neither may write it directly.** Crossfade installs its own entries and clears them at the end of *every* transition, so anything else parked in `af` used to vanish with them. `MpvPlayer` now keeps `eqEntry` and `crossfadeEntries` apart and `applyAudioFilters()` is the single writer; `clearAudioFilters()` drops the crossfade tier alone. The symptom this prevents is "EQ works, then randomly doesn't" — the same shape as the crossfade guard that was dead on one of two trigger paths.
  - **Android needs no re-apply; Desktop does.** The processor reads the curve through a supplier bound to a `@Volatile` field on **every buffer**, and `ExoPlayer.Builder` appears in exactly one factory, so any player — initial, next-track, crossfade, precache — is already correct with no push. mpv is the opposite: a fresh handle starts with an empty `af`, so `applyPlaybackLevels()` re-asserts the curve on all four creation sites. **`secondaryPlayer` is the one to miss**: while being promoted it belongs to neither `currentPlayer` nor `precachedPlayers`.
  - **Never override `isActive()` to `true` in a `BaseAudioProcessor`.** The base answers it from `pendingOutputAudioFormat`, which `configure()` assigns immediately *before* the call — so the default already means "active iff `onConfigure` accepted the format", and it stays active across curve changes because activity is only reconsidered on configure. Forcing `true` claims the processor is in the chain while handing back `NOT_SET` for a format it cannot read. `SleepFadeAudioProcessor` still does this; harmless only while `enableFloatOutput` stays off.
  - Every band keeps a stage even at 0 dB. With `A = 1` the numerator equals the denominator exactly, so the stage is a true **identity transfer function** — which fixes the array sizes, so dragging a band never resizes the filter state and never clicks. *(Corrected 2026-08-24: previously called "bit-identical", which is checkable and false — the left-to-right accumulation leaves ~1e-14 residual, ~180 dB below 16-bit LSB.)*
  - Presets follow Spotify's **names**; the gains are ours, because neither Spotify nor Apple has published theirs and both run six bands. The active preset — and an imported AutoEq label — are read back **off the curve** rather than stored, so dragging a band drops the label by itself and a preset re-selects itself if the curve returns to it.
- **AutoEq profile import (2026-08-22)**: new `core/service/autoEqService` plus three Room tables at **v25** (`autoeq_entry`, `autoeq_index_meta`, `autoeq_curve`; three added tables and nothing else, so Room writes the migration itself). `results/INDEX.md` is read straight off raw.githubusercontent — 851 kB, 8850 profiles — parsed to rows so a search is an indexed `LIKE` rather than a re-parse. Its ETag is **weak** (`W/"…"`) and still answers 304, which keeps a routine freshness check to a couple of hundred bytes; curves are fetched per profile and cached, so a headphone used once works offline afterwards.
  - It drops in untouched because AutoEq's fixed-band output is generated at `31.25 * 2**i` with `q = math.sqrt(2)` bounded to ±12 dB — the same centres, Q and range this equalizer runs. Its written centres are rounded (31.25 → "31"), so the parser matches by frequency **within a tolerance**, and places gains by frequency rather than by filter number.
  - **Its `Preamp:` is computed from the summed response, not the tallest band, so it goes past −12 dB** (−12.1 observed across a 60-profile sample). The preamp slider floor is therefore −15: a value outside a `Slider`'s range is pinned to the end of the track while holding a different number.
- **System equalizer removed entirely (2026-08-22)**: the `ACTION_OPEN/CLOSE_AUDIO_EFFECT_CONTROL_SESSION` broadcasts, `MediaPlayerListener.shouldOpenOrCloseEqualizerIntent` and every `notifyEqualizerIntent` call site, the `OpenEq` expect/actual trio and the Settings row are all gone — two equalizers on one audio session multiply, and the in-app one is now the answer. On Desktop that whole chain had been firing into empty stubs anyway. **`MediaPlayerInterface.audioSessionId` stays**: it looks like part of this, but `LoudnessEnhancer` (volume normalisation) is its real user. An equalizer app already attached to the session survives until the process dies, so force-stop before judging whether the removal worked.

- **Analytics joins the Apple Music family, and gets a landscape layout (2026-08-22)**: the screen already had the immersive artwork header — the #1 track at `hDP/2.5` under `smoothScrimBrush` — so this is the rest of the treatment rather than a redesign: the page background is now the artwork's dominant tone (`rememberPaletteState` → `toImmersiveBackground`, the same machinery Album/Playlist/Artist use), the back button and the day-range picker become liquid glass, and the three counters drop their underlined labels. **The day-range picker stays a `DropdownMenu`** — only its trigger changed, from a calendar icon with "7d" printed inside it at 8sp to a pill that says which range is showing.
  - `isPortrait = wDP < hDP` picks the layout, as on the other four screens. Landscape reserves a 48dp + 16dp strip so back and the pill get **their own top row** before the 280dp artwork — `Spacer(48.dp)` in the column and `.padding(top = 16.dp)` on the sibling buttons are two separate calculations, and the button's height must equal the reserved strip or it hangs into the artwork.
  - The body splits **in half** (`AnalyticsScreen.kt:357-360`): a fixed-width mosaic column was tried first and starved the remainder at real window widths (at 986dp the remainder was 202dp), so the even split survived. Both song lists and the chart share one column — Recently played sits with Top tracks rather than below the grid, since they are the same kind of block and separating them left one column ragged. *(Corrected 2026-08-24: this entry previously described the abandoned 600dp fixed column as shipped.)*
  - **The 30-day range buckets by week, not by day.** Thirty rows is a list nobody reads to the end. It is four buckets of exactly seven days rather than four-and-a-bit covering all thirty: an uneven last bucket would carry more days than the others and draw a longer bar for it, which is the one thing a bar chart must not do. 7 days still lists days, 90 days and the year still list months (`ChartType.Week` joins `Day` and `Month`).
  - **`FiveImagesComponent` kept, and given a landscape arm.** Its 1 + 2 + 2 mosaic is what carries the *ranking* — a horizontal shelf sizes every entry the same and throws that away. But its 2:1 banner is 171dp tall at 390dp and **616dp at 1280dp**, so `landscape = true` makes #1 a square taking the left half with the other four as a 2×2 beside it: same tiles, same hierarchy, half the height. The three duplicated tile blocks collapsed into one `MosaicTile`.
  - **Glass is for what floats over content, not a skin for content.** Only the back button and the range pill use it; the counters and the chart sit directly on the page. A glass card on a flat background is just a border, and a border tells the user "separate object, probably tappable".
  - Three bugs fixed on the way through, all visible to every user: the total and the per-track times printed **raw seconds** (`"47231 seconds"`); `"Listened time"` was a hardcoded English literal no translation could reach; and every date used `MonthNames.ENGLISH_FULL`/`ENGLISH_ABBREVIATED`, which are constants rather than locale lookups — kotlinx-datetime ships no localized alternative, so the twelve abbreviations are string resources like everything else (`AnalyticsFormat.kt`).

- **Analytics gains a period navigator and five Last.fm-shaped charts (2026-08-22)**: the screen could only ever show *now*; it can now step back through weeks, months and years with ← →, and every figure carries its change against the same span one period earlier. The range-in-range queries this needed (`queryTop*InRange`, `getPlaybackEventCountInRange`) **already existed and were wired DAO → datasource → repository → ViewModel** — they were simply only used for "This year", while the other three ranges took a `LastXDays` shortcut. Stepping is therefore a different argument, not new plumbing.
  - **One snapshot per period, not a dozen flows.** `AnalyticsRepository.getPeriodStats(start, end)` is a plain `suspend fun` returning `AnalyticsPeriodStats`, because the screen wants exactly two of them as a matched pair. Ten independent flows would let a count from this week render beside a total from last.
  - **The hour and the day a play belongs to are LOCAL questions.** Bucketing in SQL would need `'localtime'`, whose answer depends on the process timezone. `getPlaybackSamplesInRange` therefore returns raw `(timestamp, listenedSecond)` samples with the timestamp declared **`LocalDateTime`**, so Room's converter — chosen by TARGET TYPE — decodes it and no timezone arithmetic exists in the path (declaring it `Long` bypasses the converter and re-applies the offset: the first trap below). The clock, the busiest day, plays-per-day and the consistency axis are all derived from that one scan in Kotlin. *(Corrected 2026-08-24: this entry previously described the pre-fix `Long` shape as the design, contradicting the trap entry that fixed it.)*
  - **The fingerprint needs no global corpus.** All five axes are self-normalised 0..1 from `playback_event` alone: discovery = new artists / artists, replay = 1 − distinct tracks / plays, concentration = top-5 share, consistency = 1 − relative spread of daily counts, diversity = normalised entropy over per-artist counts. Only Last.fm's grey "global average" needed their corpus; the second polygon here is the **previous period**, and it is load-bearing — a lone polygon on five self-normalised axes says almost nothing.
  - `getArtistPlayCountsInRange` is unbounded on purpose. `queryTopArtistsInRange` caps at 100, which is right for a top-five list and wrong for concentration and diversity: those are shares of the whole, so a cut tail inflates both.
  - **"New" means first-ever, not first-in-window.** `getNewArtistCountInRange` takes `MIN(timestamp)` over the artist's whole history and asks whether it lands inside the range; grouping inside the window instead would call every artist new.
  - **Music by decade is partial by construction and says so.** `AlbumEntity.year` exists, but the join runs through the nullable `playback_event.albumBrowseId` — radio and standalone videos carry no album — so the chart prints the share of plays it could date. A distribution that silently drops an unknown share of its input is not a distribution.
  - **Music ratio is concentric arcs, not a donut.** Songs, albums and artists measure three different things and add up to no whole; slicing one circle between them would claim a share of something that does not exist. (Last.fm's own page makes exactly this mistake.)
  - **The clock draws filled wedges over a full-ring dark track, not spokes.** Without the track an hour with one play and an hour with none look nearly identical — the eye reads a short spoke as a missing tick rather than as "almost nothing here".
  - A delta is **absent**, never `+∞%`, when the previous period is empty — otherwise every figure in a new user's first week reads as an infinite increase.

- **Four traps found while finishing the Analytics screen (2026-08-22)**, each of which produced plausible output rather than an error:
  - **`playback_event.timestamp` is NOT a UTC instant — it is the local wall clock encoded as one.** `Converters.dateToTimestamp` writes every `LocalDateTime` with `toInstant(TimeZone.UTC)`, so the only correct decode is `TimeZone.UTC`, which is what `Converters.fromTimestamp` does. Reading the column as a raw `Long` (declaring the field `Long` bypasses the converter, which Room selects by TARGET TYPE) and then decoding it with `currentSystemDefault()` applies the offset a **second** time: on the owner's UTC+7 database the busiest hour read 03:00 instead of 20:00, and 917 of 2663 plays (34%) were counted on the following day. The fix is not "use `TimeZone.UTC`" — that still leaves a zone to pick wrong — but to declare the field `LocalDateTime` and let the converter decode it, so no timezone arithmetic exists in the path at all. Every total still added up, so only a human asking "who plays music at 3am?" caught it.
  - **kmpalette reports `palette` as null unless its state is `Success`**, and `generate()` sets `Loading` *before* its suspension point. So the page background resolves to `Color.Black` for the whole duration of every generation — and if the effect is cancelled mid-flight the state stays `Loading` forever. Analytics keyed its effect on `bitmap` AND the artwork URL; the URL goes null on every reload and then back to the **same** value, which cancels the generate and then makes the `paletteGeneratedFor` guard skip the retry. Symptom: the same song sometimes tinted the page and sometimes left it black. `AlbumScreen` keys on `bitmap` alone, which is why it never showed this. Anything painting a surface from a palette must also hold the last colour that actually resolved.
  - **Compose Resources understands `%1$s` and `%1$d` and nothing else** — no flags, no width, no `%%`. `%1$02d:00 – %2$02d:00` rendered verbatim on screen. Padding, rounding and unit symbols belong in Kotlin; a string resource should only ever join already-formatted pieces, which also spares translators from format specifiers.
  - **`ForceDarkContent` is applied per-destination in the nav graph, and Analytics was the only immersive screen never wrapped** — so on the light theme its labels drew dark-on-dark over an artwork-derived background that is always dark. Note the `MiniPlayer` has the same gap for a different reason: it is a sibling of the NavHost, so a CompositionLocal provided inside cannot reach it.
  - `FiveImagesComponent` gained a `shape` (the block is clipped as ONE object — the tiles stay flush) and, more importantly, hole-free arrangements for counts 1–5. The original `if (images.size < 3) return` silently **dropped** an entry at even counts; portrait hid that by simply getting shorter, while the landscape arm added in this change left a visible empty rectangle beside the tall first tile.

- **Now Playing style system + Material 3 Expressive style (2026-08-23)**: the player is now a style-agnostic SHELL + swappable CONTENT layer, picked by a Settings row (User interface, under Theme) persisting `NOW_PLAYING_STYLE` (`SPOTIFY` default | `M3_EXPRESSIVE`; an Apple Music style is the planned third). `NowPlayingScreen.kt` went 2630 → ~670 lines and keeps everything style-agnostic — VM state collection, palette Animatables, artwork-pager sync, every sheet/dialog — handing the rest to the content composable through the two @Stable holders in `content/NowPlayingContentState.kt` (`NowPlayingContentState` / `NowPlayingContentActions`). The old UI moved VERBATIM into `content/NowPlayingContentSpotify.kt` (two sanctioned cleanups only: the twice-duplicated metadata row became `NowPlayingTrackInfoRow`, and `isUserLoggedIn()` — a `runBlocking` executed inside composition on every recomposition — became a shell-collected flow). The new style is `content/NowPlayingContentM3Expressive.kt` + `NowPlayingExpressiveCards.kt` + `content/expressive/{WavySeekBar,ExpressiveTransportRow}.kt`, built to the owner-approved "Tonal pills" design canvas.
  - **The one-screen vertical rule is per-style, not shell**: gap = max(30, (hDP − topBar − artwork − info − 30) / 2), artwork between two equal gaps, the inline lyric line centered in the lower gap, 30dp of breathing room before the fold — each content file carries its own copy of the measurement block, so a future style may define different maths without touching the shell.
  - **The expressive APIs are usable from commonMain on the pinned CMP artifact** (`material3-multiplatform 1.12.0-alpha01` = androidx 1.5.0-alpha19): `LinearWavyProgressIndicator` and `MaterialExpressiveTheme` carry `@Material3ExpressiveApi`, which is a `@RestrictTo` marker, NOT `@RequiresOptIn` — no opt-in needed; `MaterialTheme.motionScheme` is stable. The wavy seekbar is the *indicator* plus a transparent `pointerInput` layer (it is not a slider); the wave amplitude animates to 0 when paused OR while scrubbing, and the thumb morphs circle→tall bar during drag.
  - **M3E colours are a whole artwork-seeded dark scheme, not two gradient stops**: `rememberDynamicColorScheme(seed = state.startColor.value, isDark = true, style = PaletteStyle.Vibrant)` wrapped in `MaterialExpressiveTheme` around the content subtree, everything reading tonal roles (no `Color.White` semantics, no black-gradient `drawBehind`); falls back to the app `seed` until the palette resolves.
  - **M3E has no separate canvas-takeover UI, but the canvas itself renders FULLSCREEN at page level** (`NowPlayingExpressiveCards.kt:187-189`), behind the content — not inside the 28dp artwork card, and `cropToBounds` is not passed there (the modifier chain differs from the card's). Classic's video overlay (±5s seek, subtitles) is ported at that layer. Of the shell's `showControlLayout`/`controlLayoutAlpha`/`showHideMiddleLayout`, two of the three ARE used by this style (`NowPlayingContentM3Expressive.kt:187,:190,:258,:600`); only one goes unused. Transport keeps Classic's Android-only gate — the Desktop side panel still has no slider/transport in either style. *(Corrected 2026-08-24 against the ship commit: the entry previously claimed in-card rendering, `cropToBounds = true`, and all three fields unused.)*
  - The connected action group order is a product decision: Info · Cast · Shuffle · Repeat · PlaylistAdd · Queue — and the cast glyph is NOT Material "cast": the app ships `ic_music_cast.xml` (Material Symbols `music_cast`, group-scaled 0.9) in the cast module. Design mocks must lift the real drawable, not the icon a name suggests.


- **Lyrics romanization for 12 languages (2026-08-26, issue #2342)**: a Latin-script reading shown as its OWN row between the original and the translation — never replacing either, because the point is to read the original script *and* know how to pronounce it. New package `core/service/lyricsService/.../romanization/`, driven by one DataStore key (`romanization_languages`, a comma-separated list of enum NAMES) and off by default. Detection is per LINE, not per song: a lyric sheet routinely alternates an original line with an English one, and romanizing the English half produces gibberish.
  - **Ten of the twelve need no dependency at all.** Hangul decomposes arithmetically (`0xAC00 + (initial*21 + medial)*28 + final`), so 19+21+28 table entries cover all 11 172 blocks; Devanagari and Gurmukhi are abugida state machines (a consonant carries an inherent `a` that a matra replaces, a virama deletes, and nothing else keeps); Cyrillic is **seven separate tables**, because those languages share one alphabet and romanize it differently — Russian additionally needs position-dependent rules (`е` → `ye` word-initially and after a vowel or either sign). Only Japanese (kuromoji) and Chinese (pinyin4j) need a library, so only they are behind an expect/actual; **iOS gets a no-op actual returning null**, which the caller already treats as "show nothing".
  - **`RomanizationLanguage` lives in `core/domain`, not beside the romanizers.** `data` depends on `lyricsService` with `implementation`, so the dependency stops there and `composeApp` cannot see it — the layering rule working as intended. The UI therefore goes through `LyricsRomanizerRepository` in domain, the same shape Listen Together uses. Anything the UI and a service must BOTH name belongs in domain.
  - **TinyPinyin — what Metrolist uses — cannot be used here, for two independent reasons.** It is published only on JitPack, where its POM names its own groupId with the wrong case (`promeg` vs `promeG`) so the transitive resolve dead-ends; and it drags in `tinypinyin-android-asset-lexicons`, an Android-asset artifact that breaks `:lyricsService:jvmMain` and `jvmTest`. **pinyin4j** (Maven Central, 316 KB, only `junit` at test scope) replaces it and is strictly better here — it carries tone marks, which TinyPinyin does not. Its trap: `WITH_TONE_MARK` throws `BadHanyuPinyinOutputFormatCombination` unless `vCharType` is `WITH_U_UNICODE`.
  - **kuromoji needs a packaging exclude.** `kuromoji-ipadic` pulls `kuromoji-core`, and BOTH jars ship `META-INF/CONTRIBUTORS.md` and `META-INF/LICENSE.md`, which fails `mergeDebugJavaResource`. Excluded as `META-INF/*.md` in `androidApp` — by pattern, because excluding only CONTRIBUTORS.md just moves the failure to LICENSE.md. Note the pre-existing `META-INF/LICENSE…` list there is under `jniLibs.excludes` and does NOT apply to java resources.
  - **Rich sync is romanized from the STRIPPED string, as a separate row.** The raw text still holds its `<mm:ss.xx>` markers; romanizing it in place would destroy the very timings the word-by-word wipe runs on. Because the reading is its own row, the original is left untouched and keeps lighting up — and the reading gets the whole line's context rather than one word at a time, which a per-word pass had cost.
  - **Nukta characters are TWO code units.** `क़` is क + U+093C, not a `Char` — writing the composed forms as char literals is a compile error (13 of them), so the nukta is handled like a matra: consumed first, because it changes which consonant this is (ज → z, not j) before the inherent vowel is decided.
  - Cyrillic language detection is a guess by distinctive letters (`ѓќѕ` → Macedonian, `ђћџљњј` → Serbian, `ңөү` → Kyrgyz, `ўі` → Belarusian, `їєґ` → Ukrainian, else Russian) and runs **only** when more than one Cyrillic language is enabled; with exactly one, that choice is the answer.
  - `SettingAlertState` picks its dialog body with an `if / else if` chain testing `message` FIRST — passing `message` alongside `multipleSelect` renders the text and **no list at all**. The four nullable fields are mutually exclusive despite the type saying otherwise.

- **The Apple Music player's tabs broke three things that had been fine (2026-08-26)**: `Crossfade(targetState = viewState)` composes exactly ONE body, so on QUEUE or LYRICS the artwork pager inside MAIN does not exist — and the shell had been treating it as always alive. Tapping a queue row played the wrong song, and it took three attempts because the first two chased the symptom. The decisive clue was that **Classic and M3 Expressive are never affected** — they use `QueueBottomSheet`, which renders the WHOLE queue and passes the index `itemsIndexed` hands it, computing nothing. `AppleMusicQueueView` instead did `drop(offset)` and added `offset` back at click time, making every row's identity depend on `state.currentOrderIndex`, a value DERIVED in the shell. It now does `withIndex().drop(offset)` so the queue-wide index travels WITH its track: a wrong `currentOrderIndex` can still cut the list in the wrong place, but it can no longer play the wrong song. **When one screen is wrong and its twin is right, compare the mechanisms before debugging the values — the twin that computes nothing has nothing to get wrong.** Two further guards on the shell's pager→player effect: it is keyed on the pager alone with the index read through `rememberUpdatedState` (re-keying it per track rebuilt the `snapshotFlow`, whose first emission is the stale `settledPage` that `distinctUntilChanged` cannot suppress), and it now requires a latched `pendingUserSwipe` set by a real finger drag — on the Queue/Lyrics tabs the pager does not exist, so every seek it appears to request is fictitious. The page background went stale for the same reason (`onArtworkBitmap` is only fed from inside that pager), fixed with a loader outside the Crossfade. And the canvas flashed sideways on returning from LYRICS because `MediaPlayerView`'s legacy path seeds its width to the SCREEN width and only corrects it once `onVideoSizeChanged` reports the truth — `cropToBounds = true` takes its size from Media3's `presentationState` instead, so there is no wrong guess to correct.

- **SimpMusic Wrapped (2026-08-27, issue #2345)**: a year-in-review told as a **story reel** — ten cards, auto-advancing, tap/hold/swipe — built entirely from the local `playback_event` table. No new data collection and no network call beyond artwork: every figure already existed on `AnalyticsPeriodStats`. Lives in `ui/screen/home/wrapped/`, entered from a banner on the Analytics screen, gated on local tracking exactly as the Analytics tab is, and replaced by a "not enough of the year yet" screen below `REQUIRED_ACTIVE_DAYS = 30`.
  - **Shell / content split, as with the Now Playing styles.** `WrappedScreen` owns everything constant — progress segments, year label, close button, footer, timer, capture — and each card fills the slot and draws nothing else. `WrappedYear.cards` drops the cards a given year cannot fill, so the segment count is derived, never a hardcoded ten.
  - **One new query, and only one.** `queryTopArtistsInRange` reads `event_artist`, which holds one row per artist per play and **no duration at all**, so card 04's listening time can only come from a join back to `playback_event`. Added as `queryTopArtistsWithTimeInRange` returning a **new** `TopPlayedArtistTime` — adding a column to `TopPlayedArtist` would have broken the two existing queries that do not select it. A track credited to several artists hands its full `listenedSecond` to each, so that column deliberately does not sum to the period total.
  - **The design canvas is a layout reference, not a stylesheet — and porting it literally is how this shipped wrong the first time.** The first pass carried its own palette of hex literals, a bespoke `wrappedText()` `TextStyle` builder, and `Box` + border "pills", and was rejected on sight. Colour now comes only from `MaterialTheme.colorScheme` inside `MaterialExpressiveTheme(colorScheme = rememberDynamicColorScheme(seed = top track's artwork, isDark = true, style = Vibrant))` — the construction `NowPlayingContentM3Expressive` already uses; type only from `MaterialTheme.typography`; surfaces from `LiquidGlassIconButton` / `liquidGlass` over `rememberBackdrop` and real Material 3 buttons. **A tokens object carrying a literal palette is itself the defect** — it licenses every call site downstream to hand-roll. `WrappedTokens` now holds geometry and durations and nothing else.
  - **`displayLarge` is the wrong style to enlarge.** In this app's `typo()` it is 20sp **Normal** in *body* colour, so `displayLarge.copy(fontSize = …)` yields a grey, light hero that needs a weight and a colour argued back onto it. `titleLarge` is 25sp Bold in the title colour, so `.copy(fontSize = …)` alone is enough — the idiom already live at `ListenTogetherScreen.kt:482`. The scale has no display tier, which is why each card names exactly one size and nothing else.
  - **`distinct()` on an artwork URL is not distinct by image.** YouTube Music embeds the requested size in the link, so one sleeve arrives as `…=w544-h544` from a track, `…=w120-h120` from an artist and `…=w226-h226` from an album, with `i.ytimg.com/vi/<id>/maxresdefault.jpg` as a fourth shape. A list hides this; a mosaic does not — the poster drew the same three covers six times and, because the duplicates inflated the pool, tiled over the whole card instead of stopping near the top. `distinctByArtwork()` compares a normalised key instead (strip the size segment; reduce `/vi/<id>/` to the id), the same normalisation `JvmMediaPlayerHandlerImpl` already applies when handing artwork to the media session.
  - **Wrapped counts as fullscreen.** `isInFullscreen` in `App.kt` was derived from `FullscreenDestination` alone, and it is what hides the navigation rail and the tablet mini player — without Wrapped in that check, both sat on top of the reel, and on top of every card captured as a share image.
  - Card 07 shows the biggest day against a typical day and **no daily series**: `WrappedYear` carries none, and thirteen invented bars on an image that leaves the app is not a rounding error. Card 09 is dropped entirely below 50% album-year coverage rather than drawn from a minority of plays. Card 08's archetype is a straight argmax over the fingerprint's five axes, ties broken by declaration order.
  - **A Wrapped chip in Library, and a recap playlist per month.** `LibraryChipType.WRAPPED` gates on local tracking exactly as the YouTube chip gates on being logged in — one line in the chip loop and a branch in the shared `LaunchedEffect(currentFilter)`, no second mechanism. Each month is a `LibraryDynamicPlaylistType.MonthlyRecap(year, month)` round-tripping as `recap_<year>_<mm>`, so `LibraryDynamicPlaylistScreen` serves it with play, shuffle and download already attached, and a `MonthlyRecapItem : PlaylistType` puts it in the shared `GridLibraryPlaylist` rather than a bespoke list. Months are those with plays, newest first, capped at twelve.
  - **A recap carries no artwork, deliberately.** It first borrowed the month's top song's cover, which made the tile read as that song; it now falls to `painterPlaylistThumbnail(title)` — the deterministic gradient with the name on it that every artwork-less playlist in this app already gets.
  - **A full-width block above a grid is a grid item, not an overlay.** The entry card was first drawn in a `Box` over `GridLibraryPlaylist` with its height reserved in `contentPadding` and its position translated by the scroll offset; the measured height included the top inset that was then added to it again, leaving a screen-tall hole. `GridLibraryPlaylist` now takes a `header` slot rendered as `item(span = { GridItemSpan(maxLineSpan) })`, the same mechanism its create tile and chart button already use. A grid item cannot be double-counted.
  - Sharing reuses the pipeline that shipped with the lyrics share (`Capturable`, `saveImageToDevice`, `shareImage`). The footer captures the card content only; card 10 exposes a `posterModifier` so its own Save/Share buttons stay out of the PNG.

- **Stream itags centralised, and "High" split into Opus and AAC (2026-08-28)**: every itag lived as a bare number scattered across five files — `setOf(250, 251, 774, 141)` in two copies of `BraveNewPipeUtils`, a `136`/`134` fallback ladder and an `if (itag == 774) find { 141 }` branch in `StreamRepositoryImpl`, `it.itag == 251` in `YouTube.getNParam`. They now all name `ITAG` in `core/common/Config.kt`, which also owns `highQualityTwinOf()` — 774 ↔ 141 are the Opus and AAC renditions of the same 256 kbps master, so an account entitled to one may be served the other, and asking for the twin beats dropping to "any audio stream" (which is 70 kbps).
  - `QUALITY` gained a fourth entry so the user can pick the family: `High Opus - 256kps` (774) and `High AAC - 256kps` (141). **The setting is persisted as the label STRING, not an index**, so renaming an entry orphans every device holding the old text — and the old guard resolved anything unrecognised to `items[0]`, i.e. it would have silently dropped Premium accounts from 256 kbps to 66 kbps. `QUALITY.normalize()` maps known older labels forward and is the single place that decides; `QUALITY.itagOf()` replaces `itags.getOrNull(items.indexOf(...))`, whose `-1` on an unknown label produced a null itag and no match.
  - Migration is lazy: nothing rewrites the stored value, so a read never races a write. The text is replaced the next time the user picks an entry.

- **Ciphers decoded on-device, with the player table pulled from a remote registry (2026-08-28)**: `api.pipepipe.dev` used to solve every signature and `n` parameter, which makes playback depend on someone else's server staying up. PipePipe already had the hook — `YoutubeApiDecoder.setLocalDecoder()` takes a `YoutubeJavaScriptDecoder`, an interface it ships **with no implementation** — so nothing in the fork needed changing. The implementation is `FaradayJsDecoder` (androidMain + jvmMain) over `FaradayCipherEngine` and a `cipher/` package copied from `MetrolistGroup/innertubex` (GPL-3 ↔ GPL-3). Three tiers now, no shared point of failure: **faraday table on-device → api.pipepipe.dev → BravePipe**.
  - **The remote table is a locator, not a decoder.** `player_configs.json` maps a player hash to the *names* inside YouTube's own player JS (`sig: "Tl(48,5831,INPUT)"`, `nClass`, `sts`); the script is still downloaded and executed locally in QuickJS. What it buys is the step that breaks on every obfuscation reshuffle — guessing those names by regex. Source is `raw.githubusercontent.com/MetrolistGroup/faraday/master/registry/player_configs.json`, refreshed on a 6-hour TTL with an ETag, plus two failure-triggered refreshes (unknown hash, CDN rejection).
  - **The URL must pass two independent gates**: `configuredUrl()` wants the `/player_configs.json` suffix, `validatedSourceUrlOrNull()` wants the path under `/MetrolistGroup/faraday/`. Metrolist's own app fails the second (it points at `ZemerTeam/zemer-cipher`), so its top tier silently returns null and never runs — a whole feature disabled with no error anywhere.
  - **QuickJS `maxStackSize` defaults to 256 KB and YouTube's n-transform goes deeper — and QuickJS does NOT reliably raise a JS error when it overruns, the process just takes SIGSEGV.** No Kotlin frame, and no `hs_err` file either, because the crash is below the JVM: `-XX:ErrorFile` is set and still writes nothing, `dmesg` is restricted, and apport discards it because a Gradle-provisioned JDK "does not belong to a package". The only thing that located it was `Logger.w` markers around each native call — the last line printed was `callFunction.begin name=_nTransformFunc`. Fixed on both sides: `maxStackSize = 8 MB`, on a thread created with a **32 MB** stack. The soft limit must sit well under the real stack so the engine trips its own guard and throws instead of walking off the end; `newSingleThreadContext` cannot be used because it offers no way to size the stack.
  - **Throwing is how a request is handed back.** `YoutubeApiDecoder.decodeBatch` tries the local decoder and falls through to the API only on an exception — a half-filled result is accepted as final and the missing URLs merely fail later. So `decodeBatch` throws unless *every* challenge is solved. `getPlayerData` is the opposite: `getPlayerMetadata` calls it with **no try/catch**, so an exception there skips the API tier entirely and lands on BravePipe; the engine returns null (rather than guessing) when either the player id or the `sts` is missing, because a wrong timestamp poisons the whole response.
  - **`disableLocalDecoder` clears the decoder permanently**, and the getter is package-private so its state cannot be read from outside. `Extractor.newPipePlayer` therefore calls `setLocalDecoder` unconditionally on every extraction, turning "disabled forever" into "skipped for one track". `headCheckRandomStream()` failing now also calls `invalidate()` — a CDN 403 is the only visible symptom of a stale table, since a wrong signature is still well-formed and throws nothing.
  - **`ExtractSource` (in-memory, last 32 videos) feeds an "Extract source" row in the info sheet** — `PipePipe · local`, `PipePipe · pipepipe.dev`, `PipePipe · cached` or `BravePipe`. Deliberately not a column on `NewFormatEntity`: a format row is cached and reused, so a persisted source would keep naming the path taken the first time. `SharedViewModel` re-reads it on **every** format emission, because the first one lands before extraction finishes.

- **Japanese romanization dictionary moved out of the APK (2026-08-28)**: kuromoji-ipadic's dictionary — 8 `.bin` classpath resources, ~13.2 MB compressed — was **92% of a +14.4 MB APK regression** vs v1.7.0 (28.0 → 42.4 MB full arm64). Android now excludes them (`androidApp` packaging `resources.excludes += "com/atilika/kuromoji/ipadic/*.bin"`) and fetches the pack on demand: first time the user confirms a romanization selection containing Japanese, `KuromojiDictionary` streams `kuromoji-ipadic-0.9.0-dict.tar.gz` from the **`abc` release of `maxrave-dev/simpmusic-files`** (the same release that hosts the mpv natives — reuse existing releases, do not mint new tags), verifies a pinned SHA-256 while downloading, extracts via a whitelist tar reader and atomically renames into `filesDir/kuromoji-ipadic/`. Desktop keeps the dictionary bundled (13 MB is noise beside a 230 MB DMG); iOS stays no-op.
  - **`TokenizerBase.Builder`'s protected `resolver` field is a decoy.** ipadic's `loadDictionaries()` assigns `resolver = SimpleResourceResolver(getClass())` unconditionally at its own top (verified in 0.9.0 bytecode), so planting a resolver in a subclass `init` is silently overwritten. `DirectoryDictionaryBuilder` overrides `loadDictionaries()` itself, restating its short body — including ipadic's private penalty defaults `[2, 3000, 7, 1700]`. kuromoji asks the resolver for **bare filenames** (`"tokenInfoDictionary.bin"`); the resolver maps by basename anyway.
  - `PlatformRomanizer.android` builds the tokenizer only when all 8 files are on disk and caches **only a successful build**, so Japanese lines come alive the moment the download lands — no restart. Until then `japanese()` returns null, the pipeline's existing "show nothing" contract. State (`NOT_DOWNLOADED/DOWNLOADING/READY/FAILED`) flows `LyricsRomanizerRepository` → Settings row subtitle; retry = confirm the dialog again.
  - Known gap, accepted: a device that had Japanese enabled *before* this build only starts the download on the next dialog confirm — irrelevant for v2.0.0 since no such users exist yet.

## 🔄 CLAUDE.md Auto-Update Rule (MANDATORY)

After completing any of the following types of changes, the AI agent **MUST** update this CLAUDE.md file:

1. **Architecture changes**: Module additions/removals, dependency changes (e.g., library swaps like GStreamer → VLCJ), build system changes
2. **New major features**: New modules, new service integrations, new platform capabilities
3. **API/Technology migrations**: Swapping core libraries, changing data flow patterns
4. **Build/CI changes**: New build variants, changed packaging formats, CI workflow changes
5. **Module structure changes**: Adding/removing modules in settings.gradle.kts

**What to update**:
- Relevant sections in this document (Module Structure, Key Technologies, etc.)
- Add entry to Changelog Summary section with date/version context
- Update "Last updated" date at the bottom

**What NOT to update for**:
- Bug fixes, minor UI tweaks, translation updates
- Simple refactoring within existing patterns
- Dependency version bumps without API changes

---

*This document helps AI Agents quickly understand the SimpMusic project. Update regularly when there are major changes to architecture or structure.*

**Last updated**: 2026-08-28
**Project version**: Check latest release on GitHub
**Maintained by**: maxrave-dev and contributors
