# Source Set Hierarchy in Amethyst

Visual guide to source set organization with concrete examples from the codebase.

## Hierarchy Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                       commonMain                            │
│  Pure Kotlin, no platform APIs                             │
│  Examples:                                                  │
│  - Nostr event parsing (TextNoteEvent, MetadataEvent)      │
│  - Business logic (data validation, crypto algorithms)     │
│  - Data models (@Immutable data classes)                   │
│  Dependencies: kotlin-stdlib, kotlinx-coroutines           │
└──────────────────────┬──────────────────────────────────────┘
                       │
          ┌────────────┴────────────┬───────────────┐
          │                         │               │
          ▼                         ▼               ▼
┌──────────────────┐    ┌───────────────────┐   ┌──────────────┐
│   jvmAndroid     │    │     iosMain       │   │  Future:     │
│  JVM libraries   │    │   iOS common      │   │  jsMain      │
│  Examples:       │    │   Examples:       │   │  wasmMain    │
│  - Jackson JSON  │    │   - Platform API  │   └──────────────┘
│  - OkHttp HTTP   │    │   - Actuals for   │
│  - url-detector  │    │     crypto/I/O    │
│  Dependencies:   │    │   Dependencies:   │
│  - Jackson       │    │   - Platform libs │
│  - OkHttp        │    └───────┬───────────┘
└────┬─────────┬───┘            │
     │         │                ├─→ iosX64Main (simulator Intel)
     │         │                ├─→ iosArm64Main (device ARM64)
     │         │                └─→ iosSimulatorArm64Main (Apple Silicon)
     ▼         ▼
┌──────────┐ ┌───────────┐
│android   │ │  jvmMain  │
│Main      │ │ (Desktop) │
│Examples: │ │ Examples: │
│- Activity│ │- Window   │
│- ViewModel│ │- MenuBar  │
│- Android │ │- Desktop  │
│  APIs    │ │  Compose  │
│Deps:     │ │ Deps:     │
│- secp256k│ │- secp256k │
│  1-android│ │  1-jvm    │
│- androidx│ │- Compose  │
│          │ │  Desktop  │
└──────────┘ └───────────┘
```

## Dependency Flow

```
Code in commonMain
  ↓ can use
Nothing (only Kotlin stdlib)

Code in jvmAndroid
  ↓ can use
commonMain + JVM libraries (Jackson, OkHttp)

Code in androidMain
  ↓ can use
commonMain + jvmAndroid + Android framework

Code in jvmMain
  ↓ can use
commonMain + jvmAndroid + JVM + Compose Desktop

Code in iosMain
  ↓ can use
commonMain + iOS platform APIs
```

## Real Examples from Amethyst

### commonMain - Pure Kotlin

**File:** `quartz/src/commonMain/.../TextNoteEvent.kt`

```kotlin
@Immutable
class TextNoteEvent(
    id: HexKey,
    pubKey: HexKey,
    createdAt: Long,
    tags: Array<Array<String>>,
    content: String,
    sig: HexKey,
) : BaseThreadedEvent(...) {
    // Pure Kotlin - works everywhere
    override fun indexableContent() = "Subject: " + subject() + "\n" + content
}
```

**Why commonMain:**
- Pure Kotlin code
- No platform APIs
- Data class with business logic
- Needed by all platforms

---

### jvmAndroid - JVM Libraries

**File:** `quartz/build.gradle.kts`

```kotlin
val jvmAndroid = create("jvmAndroid") {
    dependsOn(commonMain.get())

    dependencies {
        // Normalizes URLs
        api(libs.rfc3986.normalizer)

        // Performant Parser of JSONs into Events
        api(libs.jackson.module.kotlin)

        // Parses URLs from Text
        api(libs.url.detector)

        // Websockets API
        implementation(libs.okhttp)
        implementation(libs.okhttpCoroutines)
    }
}

jvmMain { dependsOn(jvmAndroid) }      // Desktop gets Jackson, OkHttp
androidMain { dependsOn(jvmAndroid) }  // Android gets Jackson, OkHttp
```

**Why jvmAndroid:**
- Jackson, OkHttp are JVM-only libraries
- Works on Android (JVM) and Desktop (JVM)
- Does NOT work on iOS (not JVM) or web (not JVM)

**Usage in code:**
```kotlin
// Can use Jackson in jvmAndroid source set
val mapper = ObjectMapper()
val event = mapper.readValue(json, Event::class.java)
```

---

### androidMain - Android Platform

**File:** `amethyst/src/main/.../MainActivity.kt`

```kotlin
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()  // Android API
        super.onCreate(savedInstanceState)

        setContent {  // Compose for Android
            AmethystTheme {
                val accountStateViewModel: AccountStateViewModel = viewModel()
                AccountScreen(accountStateViewModel)
            }
        }
    }
}
```

**Why androidMain:**
- AppCompatActivity is Android framework
- Activity lifecycle Android-specific
- AndroidX libraries (viewModel())

**Dependencies:**
```kotlin
androidMain {
    dependsOn(jvmAndroid)  // Gets Jackson, OkHttp
    dependencies {
        implementation(libs.androidx.core.ktx)
        api(libs.secp256k1.kmp.jni.android)  // Android crypto
    }
}
```

---

### jvmMain - Desktop Platform

**File:** `desktopApp/src/jvmMain/.../Main.kt`

```kotlin
fun main() = application {
    val windowState = rememberWindowState(
        width = 1200.dp,
        height = 800.dp
    )

    Window(  // Compose Desktop API
        onCloseRequest = ::exitApplication,
        state = windowState,
        title = "Amethyst"
    ) {
        MenuBar {  // Desktop-specific
            Menu("File") {
                Item("New Note", shortcut = KeyShortcut(Key.N, ctrl = true))
            }
        }
        NavigationRail { ... }  // Sidebar
    }
}
```

**Why jvmMain:**
- Window, MenuBar, NavigationRail are Compose Desktop
- Keyboard shortcuts desktop paradigm
- Different UX from Android (sidebar vs bottom nav)

**Dependencies:**
```kotlin
jvmMain {
    dependsOn(jvmAndroid)  // Gets Jackson, OkHttp
    dependencies {
        implementation(libs.secp256k1.kmp.jni.jvm)  // Desktop crypto
        implementation(compose.desktop.currentOs)
    }
}
```

---

### iosMain - iOS Platform

**File:** `quartz/build.gradle.kts`

```kotlin
iosMain {
    dependsOn(commonMain.get())
    dependencies {
        // iOS platform dependencies
    }
}

val iosX64Main by getting { dependsOn(iosMain.get()) }
val iosArm64Main by getting { dependsOn(iosMain.get()) }
val iosSimulatorArm64Main by getting { dependsOn(iosMain.get()) }
```

**Why iosMain:**
- iOS platform APIs
- Native crypto (Security framework)
- Different from Android/Desktop

**Architecture targets:**
- iosX64Main: Intel simulator
- iosArm64Main: Device (iPhone, iPad)
- iosSimulatorArm64Main: Apple Silicon simulator

---

## Build Order Matters

**CRITICAL:** jvmAndroid must be defined BEFORE androidMain and jvmMain:

```kotlin
// ✅ CORRECT ORDER
val jvmAndroid = create("jvmAndroid") { ... }
jvmMain { dependsOn(jvmAndroid) }
androidMain { dependsOn(jvmAndroid) }

// ❌ WRONG - Build error
androidMain { dependsOn(jvmAndroid) }  // jvmAndroid not defined yet!
val jvmAndroid = create("jvmAndroid") { ... }
```

See comment in quartz/build.gradle.kts:131:
```kotlin
// Must be defined before androidMain and jvmMain
val jvmAndroid = create("jvmAndroid") { ... }
```

## Choosing the Right Source Set

Decision flowchart:

```
Q: Where should this code go?

├─ Pure Kotlin? (no platform APIs)
│  └─ commonMain
│
├─ JVM library? (Jackson, OkHttp)
│  └─ jvmAndroid
│
├─ Android API? (Activity, Context)
│  └─ androidMain
│
├─ Desktop API? (Window, MenuBar)
│  └─ jvmMain
│
└─ iOS API? (platform.posix, Security)
   └─ iosMain
```

## Future: Web/wasm Source Sets

**Not yet implemented**, but structure would be:

```
commonMain
  ├─→ jsMain (JavaScript/Web)
  │    └─ JS-specific: DOM APIs, fetch
  │
  └─→ wasmMain (WebAssembly)
       └─ wasm-specific: limited APIs
```

**Constraints:**
- Cannot use jvmAndroid (Jackson, OkHttp)
- Cannot use platform.posix
- Must use pure Kotlin or web-compatible libs (ktor, kotlinx.serialization)

## Summary Table

| Source Set | Extends | Can Use | Example Code |
|------------|---------|---------|--------------|
| commonMain | - | Kotlin stdlib only | TextNoteEvent, business logic |
| jvmAndroid | commonMain | JVM libs (Jackson, OkHttp) | JSON parsing, HTTP |
| androidMain | jvmAndroid | Android framework | Activity, ViewModel |
| jvmMain | jvmAndroid | JVM + Compose Desktop | Window, MenuBar |
| iosMain | commonMain | iOS platform | Security framework |
| iosX64Main | iosMain | Simulator (Intel) | Architecture-specific |
| iosArm64Main | iosMain | Device (ARM64) | Architecture-specific |
| jsMain | commonMain | JS/DOM | Web (future) |
| wasmMain | commonMain | wasm APIs | WebAssembly (future) |
