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
- **media3/**: Media3 ExoPlayer integration (includes `CrossfadeExoPlayerAdapter` for DJ-style crossfade on Android)
- **media3-ui/**: Media3 UI components
- **media-jvm/**: JVM media playback (VLCJ - replaced GStreamer post-1.0.4)
- **media-jvm-ui/**: JVM media UI components

##### **core/service/**
Service modules:

- **kotlinYtmusicScraper/**: YouTube Music API scraper
- **spotify/**: Spotify Web API integration (Canvas, Lyrics)
- **aiService/**: AI features (OpenAI, Gemini integration)
- **lyricsService/**: Lyrics fetching (LRCLIB, SimpMusic Lyrics, BetterLyrics)
- **kizzy/**: Discord Rich Presence
- **ktorExt/**: Ktor extensions for networking

#### 4. **crashlytics/** & **crashlytics-empty/**
- **crashlytics/**: Full version with Sentry crash reporting
- **crashlytics-empty/**: FOSS version without tracking

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
- **VLCJ**: Audio playback (replaced GStreamer since post-1.0.4)
- VLC native libraries are bundled per platform via `vlc-setup` Gradle plugin

### Networking & APIs
- **Ktor Client**: HTTP client
- **Kotlin Serialization**: JSON parsing
- **YouTube Music hidden API**: Data source
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
- VLCJ (VlcPlayerAdapter) for Desktop
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
  - VLCJ: Audio playback (bundled via vlc-setup plugin)
- **Features**:
  - Deep link support (`simpmusic://` and `simpmusic.org`)
  - Mini Player window (always-on-top, resizable, draggable)
  - Crash dialog
  - Custom title bar (disabled in VM environments)
- **Limitations**:
  - No offline playback
  - No video playback

### External APIs
- YouTube Music: Hidden/unofficial API (may change anytime)
- Spotify: Requires login for lyrics
- OpenAI/Gemini: User must provide API key
- SponsorBlock: Public API
- LRCLIB: Public lyrics API

## 🎵 Media Playback Architecture

### Desktop Player (VLCJ - replaced GStreamer post-1.0.4)

**Location**: `core/media/media-jvm/src/main/java/com/simpmusic/media_jvm/VlcPlayerAdapter.kt`

- Uses **VLCJ** library for audio playback (GStreamer was removed)
- VLC native libraries bundled per platform via `vlc-setup` Gradle plugin in `composeApp/build.gradle.kts`
- Bundled natives stored in `vlc-natives/{linux,macos,windows}/`
- Supports crossfade transition with dual-player approach

#### Crossfade Transition (Desktop)
- Configurable duration: 1-15 seconds (default: 5 seconds)
- Audio-only: Crossfade is skipped for video playback
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
- [VLCJ](https://github.com/caprica/vlcj)

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
- **Deep link support**: `simpmusic://` and `simpmusic.org` URL schemes
- **Desktop Crash dialog**: Error reporting UI for desktop
- **Playback speed/pitch controls**: Redesigned UI with improved animations
- **VM environment detection**: Disable transparency and custom titlebar in VMs

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

**Last updated**: 2026-03-14
**Project version**: Check latest release on GitHub
**Maintained by**: maxrave-dev and contributors
