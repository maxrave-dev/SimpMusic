# AGENT.md - SimpMusic Project Guide for AI Agents

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
- **media3/**: Media3 ExoPlayer integration
- **media3-ui/**: Media3 UI components
- **media-jvm/**: JVM media playback (GStreamer)
- **media-jvm-ui/**: JVM media UI components

##### **core/service/**
Service modules:

- **kotlinYtmusicScraper/**: YouTube Music API scraper
- **spotify/**: Spotify Web API integration (Canvas, Lyrics)
- **aiService/**: AI features (OpenAI, Gemini integration)
- **lyricsService/**: Lyrics fetching (LRCLIB, SimpMusic Lyrics)
- **kizzy/**: Discord Rich Presence
- **ktorExt/**: Ktor extensions for networking

#### 4. **MediaServiceCore/**
- Core media service logic
- Playback management
- Queue handling

#### 5. **crashlytics/** & **crashlytics-empty/**
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
- **GStreamer**: Audio playback (required)
- **yt-dlp**: Streaming URL extraction (required)

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
**Location**: `core/media/media3/` or `MediaServiceCore/`
- Media3 for Android
- GStreamer wrapper for Desktop
- Queue management
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
- **Linux**: `.deb`, `.rpm`, `.AppImage`

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
  - GStreamer: Audio playback
  - yt-dlp: Stream URL extraction
- **Limitations**:
  - No offline playback
  - No video playback
  - Buggy on some Linux distributions

### External APIs
- YouTube Music: Hidden/unofficial API (may change anytime)
- Spotify: Requires login for lyrics
- OpenAI/Gemini: User must provide API key
- SponsorBlock: Public API
- LRCLIB: Public lyrics API

## 🎵 Media Playback Architecture

### Desktop Player (GStreamer)

#### Crossfade Transition
The Desktop player supports **crossfade transition** for smooth track changes:

**Location**: `core/media/media-jvm/src/main/java/com/simpmusic/media_jvm/GstreamerPlayerAdapter.kt`

**Key Features**:
- Dual-player approach: Uses two GStreamer players simultaneously during crossfade
- Configurable duration: 1-15 seconds (default: 5 seconds)
- Desktop-only: Not available on Android (uses Media3/ExoPlayer)
- Audio-only: Crossfade is skipped for video playback

**How it works**:
1. **Detection**: Position tracking detects when `crossfadeDurationMs` remains before track end
2. **Trigger**: Loads next track into secondary player with volume = 0
3. **Animation**: 50-step smooth transition
   - Fade out current player: volume → 0
   - Fade in secondary player: 0 → target volume
4. **Metadata Update**: Track info (title, artist, thumbnail) updates **immediately** when crossfade starts
5. **Finalize**: Swap players, cleanup, trigger next precache

**Settings**:
- Enable/disable toggle in Desktop Player settings
- Duration selector: 1s, 2s, 3s, 5s, 8s, 10s, 12s, 15s
- Settings persisted via DataStore

**Edge Cases Handled**:
- Video playback → skip crossfade (audio only)
- Repeat one mode → no crossfade
- Manual skip → cancel crossfade gracefully
- Last track → no crossfade
- Precache miss → load on-demand
- Crossfade disabled → fallback to normal transition

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
- [GStreamer](https://gstreamer.freedesktop.org/documentation/)

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

---

*This document helps AI Agents quickly understand the SimpMusic project. Update regularly when there are major changes to architecture or structure.*

**Last updated**: 2026-02-04
**Project version**: Check latest release on GitHub
**Maintained by**: maxrave-dev and contributors
