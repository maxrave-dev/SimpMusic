# Target Compatibility Guide

Current targets (Android, JVM/Desktop, iOS) and future targets (web, wasm) with constraints.

## Current Primary Targets

### Android (androidMain)

**Status:** ✅ Mature, production-ready

**Runtime:** JVM (Dalvik/ART)

**Available:**
- Android framework (Activity, Context, Intent, etc.)
- AndroidX libraries (ViewModel, Navigation, etc.)
- JVM libraries via jvmAndroid (Jackson, OkHttp)
- Platform-specific crypto: secp256k1-kmp-jni-android

**Constraints:**
- Mobile UX paradigms (bottom navigation, vertical scroll)
- Touch-first interaction
- Limited screen space
- Battery/performance constraints

**Example code:**
```kotlin
// androidMain
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Android-specific lifecycle
    }
}
```

---

### JVM / Desktop (jvmMain)

**Status:** ✅ Active development, functional

**Runtime:** JVM

**Available:**
- Pure JVM libraries
- JVM libraries via jvmAndroid (Jackson, OkHttp)
- Compose Desktop (Window, MenuBar, etc.)
- Platform-specific crypto: secp256k1-kmp-jni-jvm

**Constraints:**
- Desktop UX paradigms (sidebar, menus, keyboard shortcuts)
- Keyboard + mouse interaction
- Larger screen space
- Different navigation patterns (no back stack)

**Example code:**
```kotlin
// jvmMain
fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Amethyst"
    ) {
        MenuBar { ... }  // Desktop-specific
        NavigationRail { ... }  // Sidebar
    }
}
```

---

### iOS (iosMain + architecture targets)

**Status:** ⚠️ In development, framework configured

**Runtime:** Native iOS

**Source sets:**
- iosMain (common iOS code)
- iosX64Main (Intel simulator)
- iosArm64Main (device - iPhone/iPad)
- iosSimulatorArm64Main (Apple Silicon simulator)

**Available:**
- iOS platform APIs (platform.posix, Foundation, etc.)
- Native crypto (Security framework)
- SwiftUI integration (via KMP framework)

**NOT available:**
- JVM libraries (Jackson, OkHttp)
- jvmAndroid source set
- JVM-specific APIs

**Constraints:**
- Mobile UX (similar to Android)
- Swift/Objective-C interop
- XCFramework distribution
- CocoaPods integration

**Example code:**
```kotlin
// iosMain
actual object Secp256k1Instance {
    actual fun signSchnorr(...): ByteArray {
        // Use iOS Security framework
    }
}
```

**XCFramework setup:**
```kotlin
// quartz/build.gradle.kts
kotlin {
    listOf(iosX64(), iosArm64(), iosSimulatorArm64())
        .forEach { target ->
            target.binaries.framework {
                baseName = "quartz-kmpKit"
                isStatic = true
            }
        }
}
```

---

## Future Targets

### Web / JavaScript (jsMain)

**Status:** ❌ Not implemented, consider for future

**Runtime:** JavaScript (browser or Node.js)

**Available:**
- Kotlin/JS stdlib
- JS/DOM APIs
- kotlinx.* libraries (serialization, coroutines, datetime)
- ktor-client (HTTP)

**NOT available:**
- ❌ JVM libraries (Jackson, OkHttp)
- ❌ jvmAndroid source set
- ❌ platform.posix (no file system access like native)
- ❌ Blocking APIs (different async model - JS event loop)

**Constraints:**
- Single-threaded event loop
- No blocking calls
- Different async patterns (Promises, async/await)
- Browser security (CORS, no file system)

**Migration path from current code:**

| Current (jvmAndroid) | Web-compatible alternative |
|---------------------|---------------------------|
| Jackson JSON | kotlinx.serialization |
| OkHttp HTTP | ktor-client |
| java.math.BigDecimal | Kotlin BigDecimal (coming) |
| Blocking I/O | Suspending functions |

**Example migration:**
```kotlin
// Current: jvmAndroid
val mapper = ObjectMapper()
val event = mapper.readValue(json, Event::class.java)

// Future: commonMain (works on web)
val json = Json { ignoreUnknownKeys = true }
val event = json.decodeFromString<Event>(jsonString)
```

---

### WebAssembly (wasmMain)

**Status:** ❌ Not implemented, experimental Kotlin/Wasm

**Runtime:** WebAssembly

**Available:**
- Kotlin/Wasm stdlib
- Limited kotlinx.* libraries
- wasm-specific APIs

**NOT available:**
- ❌ JVM libraries
- ❌ Full platform.posix
- ❌ Many kotlinx libraries (limited wasm support)

**Constraints:**
- Even more limited than JS
- Experimental Kotlin support
- Limited library ecosystem

**Recommendation:** Focus on web (jsMain) first, wasm later.

---

## Cross-Target Compatibility Matrix

| Feature | Android | JVM/Desktop | iOS | Web (JS) | wasm |
|---------|---------|-------------|-----|----------|------|
| Pure Kotlin | ✅ | ✅ | ✅ | ✅ | ✅ |
| kotlinx.coroutines | ✅ | ✅ | ✅ | ✅ | ⚠️ |
| kotlinx.serialization | ✅ | ✅ | ✅ | ✅ | ⚠️ |
| kotlinx.datetime | ✅ | ✅ | ✅ | ✅ | ⚠️ |
| ktor-client | ✅ | ✅ | ✅ | ✅ | ❌ |
| Jackson JSON | ✅ (jvmAndroid) | ✅ (jvmAndroid) | ❌ | ❌ | ❌ |
| OkHttp | ✅ (jvmAndroid) | ✅ (jvmAndroid) | ❌ | ❌ | ❌ |
| platform.posix | ❌ | ❌ | ✅ | ❌ | ⚠️ |
| Compose Multiplatform | ✅ | ✅ | ⚠️ (experimental) | ⚠️ (experimental) | ❌ |

Legend:
- ✅ Full support
- ⚠️ Limited/experimental
- ❌ Not available

---

## Future-Proofing Recommendations

### For Web Compatibility

**DO:**
- ✅ Use kotlinx.serialization instead of Jackson
- ✅ Use ktor-client instead of OkHttp
- ✅ Use kotlinx.datetime instead of java.time
- ✅ Use suspending functions (non-blocking)
- ✅ Keep business logic in commonMain

**DON'T:**
- ❌ Put JVM libraries in commonMain
- ❌ Use platform.posix for critical features
- ❌ Use blocking I/O
- ❌ Depend on threading (use coroutines)

**Example:**
```kotlin
// ❌ NOT web-compatible
// jvmAndroid
fun parseJson(json: String): Event {
    val mapper = ObjectMapper()  // Jackson - JVM only
    return mapper.readValue(json, Event::class.java)
}

// ✅ Web-compatible
// commonMain
@Serializable
data class Event(...)

fun parseJson(json: String): Event {
    return Json.decodeFromString<Event>(json)  // Works everywhere
}
```

### Current Migration Priorities

**High priority:** (Needed for web)
1. Migrate Jackson → kotlinx.serialization
2. Migrate OkHttp → ktor-client
3. Move business logic to commonMain

**Medium priority:** (Nice to have)
1. Abstract date/time handling → kotlinx.datetime
2. Remove platform.posix usage where possible
3. Use suspending functions over blocking

**Low priority:** (Future optimization)
1. wasm-specific optimizations
2. Platform-specific performance tuning

---

## Platform-Specific Patterns

### Android vs iOS Differences

| Aspect | Android | iOS |
|--------|---------|-----|
| **Activity/ViewController** | Activity | UIViewController |
| **Navigation** | Compose Navigation | UINavigationController |
| **Lifecycle** | onCreate, onResume, etc. | viewDidLoad, viewWillAppear |
| **Permissions** | Runtime permissions | Info.plist + runtime |
| **Crypto** | secp256k1-android | Security framework |
| **Storage** | Room, SharedPreferences | Core Data, UserDefaults |

### Desktop vs Mobile Differences

| Aspect | Desktop | Mobile |
|--------|---------|--------|
| **Navigation** | Sidebar | Bottom nav |
| **Input** | Keyboard + mouse | Touch |
| **Screen** | Large, landscape | Small, portrait |
| **Windows** | Multi-window | Single app |
| **Shortcuts** | Keyboard shortcuts (Ctrl+N) | None |
| **Menus** | MenuBar | Bottom sheets |

---

## Testing Strategy

### Per-Target Testing

**Android:**
- Unit tests: androidTest
- Instrumented: androidInstrumentedTest
- Device/emulator testing

**Desktop:**
- Unit tests: jvmTest
- Manual desktop app testing

**iOS:**
- Unit tests: iosTest (iosX64Test, iosArm64Test, etc.)
- Simulator/device testing

**Web (future):**
- Unit tests: jsTest
- Browser testing (Selenium, Playwright)

### Shared Testing

**commonTest:**
- Business logic tests
- Pure Kotlin code
- Works on all platforms

```kotlin
// commonTest
class EventParsingTest {
    @Test
    fun parseTextNoteEvent() {
        // Tests run on all platforms
    }
}
```

---

## Summary

**Current Focus:** Android, JVM/Desktop, iOS (active development)

**Future Considerations:** Web (requires migration from Jackson/OkHttp)

**Key Decision:** Prefer kotlinx.* libraries over JVM-specific libs for future web compatibility.
