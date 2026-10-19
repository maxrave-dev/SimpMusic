# CLAUDE.md - SimpMusic Project Guide for AI Agents

## 🌐 Language Rule

**Response language**: Always respond in **English**.

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
- **vivoAndroidApp/** builds the same app under `com.spotify.music` for vivo's Origin Island. It has no code of its own: its source set points at androidApp's sources, resources and manifest. Its `build.gradle.kts` mirrors androidApp's, so keep the two in step.

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
- **loginSync/**: the QR-paired transport that moves sign-ins from the Android app to the Desktop app (Ktor client on the phone, one-shot Ktor CIO server on the Desktop, AES-256-GCM). Android + JVM targets only; carries opaque bytes — the data layer decides what they mean
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
4. Never add `autoMirror = true`: in right-to-left languages only text flips, icons keep their shape (owner's rule, 2026-10-14). The six icons that had it — `ArrowForwardIos`, `QueueMusic`, `Sort`, `TrendingUp`, `VolumeUp`, `VolumeOff` — drew mirrored in Hebrew/Arabic/Persian until it was removed
5. Replace the closing `.build()` with `.buildSimpIcon()` (in `SimpIcons.kt`): it is `build()` plus the fit into Material's 20×20 live area described in the traps below. `SimpIconsFitTest` fails on a file that still ends with `.build()`

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
- **Glyphs that spill past Material's 20×20 live area are fitted into it (2026-10-18).** Material keeps a
  glyph inside the centred 20×20 square of the 24 box (2dp padding) and balances the glyphs inside it
  by keyline shapes of about equal area; 24 of our 111 Symbols spill into the padding anyway (`Lyrics`'
  note and `ThumbsUpDown` reach the box edge) and read bigger than everything beside them. `buildSimpIcon()`
  measures how far each glyph reaches from the centre along either axis and shrinks one that passes 10
  of 24 about the centre until it fits (`Lyrics`, `ThumbsUpDown` ×0.83, the other 21 ×0.86–0.996).
  A glyph inside the square is left as Google drew it, nothing is enlarged, and anything it cannot
  measure keeps the glyph as generated and logs under `SimpIcons`. `Groups` is the exception, named in
  `KEEP_AS_DRAWN` (2026-10-20): it spills only sideways, so fitting its 24 width also cut its height,
  which was never too tall, from 12 to 10 of 24, and it read shorter than every icon beside it.
  `SimpIconsFitTest` (jvmTest) fails if a file skips `buildSimpIcon()`, an icon outside `KEEP_AS_DRAWN`
  ends up outside the square, or one in it gets rebuilt. Fitting into a circle was
  tried first and dropped: judged by its one farthest point, `Lyrics` shrank to ×0.72 and read far
  smaller than `QueueMusic`. All 111 files match Google's generator at the axes above (audited the
  same day).
- **A small round button cuts a glyph that fills the live area.** `IconButton` clips its content to
  its `shape` (a circle by default); sized 24dp, that circle reaches 12dp from the centre, while a
  glyph filling the 20×20 square reaches 14.1 (`comment` 13.3). Classic's Comments button clips to
  28dp with `requiredSize(28.dp)` inside the same 24dp slot.
- **Icons in one row take one size.** Per-glyph tuning by eye (the Apple Music capsule at 23/24dp
  beside 22dp dock buttons, the title-row star at 32dp) is what made a row read big-small; only a
  deliberate hierarchy, such as Play over Previous/Next, gets a different size.

### 9. Prepare a Release (changelogs)

**Location**: `changelogs/<version>/`, one folder per version, holding `en-US.md` and `vi-VN.md`. The full procedure is the `simpmusic-release-changelog` skill in `.claude/skills/`.

- `fastlane/metadata/android/<locale>/changelogs/<versionCode>.txt` stays the short store text (F-Droid caps it at 500 characters) and is written by the owner. The full changelog lives in `changelogs/`.
- Sections are New / Improved / Fixed (vi: Mới / Cải thiện / Sửa lỗi) and list only shipped work. Every line links its issue or PR with a full URL, because GitHub does not autolink `#N` inside repository files.
- Shipped changes with no issue of their own are gathered into ONE "Polish for <version>" task on Project #2, and their lines link that task.
- Reconcile Project #2 before writing. Close issues fixed by shipped commits: post the "Fixed in <version> (<sha>)" comment FIRST, then set the milestone and Status Done. Done closes the issue by itself, so a later `gh issue close --comment` silently drops the comment.
- vi-VN addresses the reader as "bạn".

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
  - Custom title bar (macOS only; Windows and Linux keep the native title bar)
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

## ⚠️ Rules That Apply Everywhere

These traps are not tied to one area, so they stay here where every session sees them. The topic file named after each one (see **Topic Notes** below) explains it.

- **kotlin-lsp is not a compile gate.** It has no KMP support and answers "No diagnostics" for a broken `commonMain` file; the JetBrains MCP (`get_file_problems`) is the only check (`audio-effects.md`).
- **Two platforms, two player stacks.** Android runs `MediaServiceHandlerImpl` + `CrossfadeExoPlayerAdapter`, Desktop runs `JvmMediaPlayerHandlerImpl` + `MpvPlayerAdapter`. A playback change or guard usually belongs in both, and a crossfade guard goes on both of its trigger paths (`playback-engine.md`).
- **Anything both the UI and a service must name lives in `core/domain`.** `data` depends on the service modules with `implementation`, so `composeApp` cannot see their types (`lyrics.md`).
- **Logging.** Every `Logger.i/w/e` is written to disk, so diagnostics go to `Logger.d`. Never log a whole entity, response or list (log ids, counts and status codes), and never a `GoogleAccountEntity` or a googlevideo URL (`build-and-tooling.md`).
- **Settings flows re-run on every DataStore write.** `settingsDataStore.data.map {}` flows have no `distinctUntilChanged()`, and the playback position is saved every ~5 s, so a log or side effect inside a settings collector fires every few seconds (`build-and-tooling.md`).
- **Compose Resources understands `%1$s` and `%1$d` and nothing else.** No flags, no width, no `%%`, and a bare `%d` prints verbatim. Padding, rounding and units belong in Kotlin; a string resource only joins finished pieces (`analytics-wrapped.md`).
- **Never hardcode `Color.Black` as a page or bar colour; use `colorScheme.background`.** The reverse bites too: a Material default (`NavigationRail` defaults to `surface`) must not sit on a surface with a fixed colour (`ui-design.md`).
- **Text inside an M3 `Button`/`TextButton`/`FilledTonalButton` passes `color = LocalContentColor.current`.** `typo()` styles carry colours, so a bare `Text` keeps its body colour instead of the button's content colour (`analytics-wrapped.md`).
- **Right-to-left: only text flips.** Anything that shows which way time runs (seek bars, times, transport, artwork pagers) goes inside `LeftToRight { }`, and icons never get `autoMirror` (`ui-design.md`).
- **Layouts that differ by orientation gate on `screenInfo.wDP < screenInfo.hDP`, not on `getPlatform()`** (`ui-design.md`).

## 🗂️ Topic Notes (`.claude/rules/`)

What was learned while building each feature, including why the code is shaped the way it is and the traps that cost time, is kept by area in `.claude/rules/`. Each file lists the code it covers in its `paths:` frontmatter, and Claude Code loads it only when one of those files is read or edited, so this file stays small. To read about an area before touching its code, open the topic file directly.

| File | Covers |
|---|---|
| `playback-desktop.md` | The Desktop player: the libmpv binding, the bundled natives and how they are built, mpv threading, audio output, video frames and position smoothing |
| `playback-engine.md` | Both platforms' player handlers and adapters: crossfade rules, the sleep fade, queue and radio behaviour, live streams and published playback state |
| `audio-effects.md` | The ten-band equalizer, AutoEq import, the system equalizer option, and Delay and Reverb |
| `youtube-scraper.md` | The YouTube Music scraper: stream extraction (ciphers, itags, multi-language audio), `musicVideoType` and chart parsing |
| `lyrics.md` | Lyrics display and services: the Apple Music lyrics style, rich sync, the timing offset, the fullscreen lyrics page, romanization and the Japanese dictionary pack |
| `now-playing.md` | The Now Playing shell and its three styles (Classic, M3 Expressive, Apple Music), animated artwork and canvas, video frames and the comments sheet |
| `ui-design.md` | Shared UI design: the immersive headers, liquid glass, haze, the settings layout, the dark theme, right-to-left layout, the Desktop title bar, the Desktop sidebar, the Desktop frame governor and the icon migration |
| `analytics-wrapped.md` | The Analytics screen and its charts, the listening receipt, Wrapped and the AI taste card |
| `home-library.md` | Home sections, the Home backdrop and Create button, and the Browse page, the Library cards and auto playlists, Smart downloads, and the launch banners with their app links |
| `data-storage.md` | The Room database and its SQL traps, DataStore on Windows, and the Coil image cache |
| `integrations.md` | Google Cast, Last.fm scrobbling, Listen Together and Login sync |
| `build-and-tooling.md` | The Android app's identity (vivo build, official-build check, FOSS Sentry), Desktop packaging, Compose Hot Reload and the on-disk app log |

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
- **Deep link support**: `simpmusic://` and `simpmusic.org` URL schemes
- **Desktop Crash dialog**: Error reporting UI for desktop
- **Playback speed/pitch controls**: Redesigned UI with improved animations
- **VM environment detection**: Disable transparency and custom titlebar in VMs

Everything added since July 2026 lives in the topic notes in `.claude/rules/` (see **Topic Notes** above), filed by the area of code it is about.

## 🔄 CLAUDE.md Auto-Update Rule (MANDATORY)

After completing any of the following types of changes, the AI agent **MUST** update this CLAUDE.md file or the topic notes in `.claude/rules/`:

1. **Architecture changes**: Module additions/removals, dependency changes (e.g., library swaps like GStreamer → VLCJ), build system changes
2. **New major features**: New modules, new service integrations, new platform capabilities
3. **API/Technology migrations**: Swapping core libraries, changing data flow patterns
4. **Build/CI changes**: New build variants, changed packaging formats, CI workflow changes
5. **Module structure changes**: Adding/removing modules in settings.gradle.kts

**What to update**:
- Relevant sections in this document (Module Structure, Key Technologies, etc.)
- Add the changelog entry, with date/version context, to the topic file in `.claude/rules/` whose `paths` cover the code it is about, not to this file. If the change reaches code that file does not list yet, add those paths to its `paths:` list
- If no topic file fits, create one with a `paths:` list and add a row for it to **Topic Notes**
- Keep each `paths:` list to the files where its traps actually bite. Hub files such as `SharedViewModel.kt` and `App.kt` stay out of every list, or editing them would load every topic at once
- A trap that applies to code in every area goes in **Rules That Apply Everywhere** as one line, naming the topic file that explains it
- Split a topic file along its `paths` once it grows past about 40 KB
- Update "Last updated" date at the bottom

**What NOT to update for**:
- Bug fixes, minor UI tweaks, translation updates
- Simple refactoring within existing patterns
- Dependency version bumps without API changes

---

*This document helps AI Agents quickly understand the SimpMusic project. Update regularly when there are major changes to architecture or structure.*

**Last updated**: 2026-10-27
**Project version**: Check latest release on GitHub
**Maintained by**: maxrave-dev and contributors
