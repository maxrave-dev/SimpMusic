# Abstraction Examples from Amethyst Codebase

Real examples of abstraction decisions with rationale.

## Good Abstractions (Why They Work)

### 1. Secp256k1Instance - Crypto Signing

**Location:** expect in commonMain, actual in androidMain/jvmMain/iosMain

**Code:**
```kotlin
// quartz/src/commonMain/.../Secp256k1Instance.kt
expect object Secp256k1Instance {
    fun signSchnorr(data: ByteArray, privKey: ByteArray): ByteArray
    fun verifySchnorr(signature: ByteArray, hash: ByteArray, pubKey: ByteArray): Boolean
}
```

**Why abstracted:**
- Used by all platforms (Android, Desktop, iOS)
- Security APIs fundamentally different:
  - Android: secp256k1-kmp-jni-android (Android Keystore integration)
  - Desktop: secp256k1-kmp-jni-jvm (pure JVM crypto)
  - iOS: Native Security framework
- Core protocol requirement (Nostr signatures)

**Decision rationale:** Always abstract crypto - varies by platform security APIs, critical for all platforms.

---

### 2. Log - Platform Logging

**Location:** expect object in commonMain

**Code:**
```kotlin
// quartz/src/commonMain/.../Log.kt
expect object Log {
    fun d(tag: String, message: String)
    fun w(tag: String, message: String, throwable: Throwable?)
    fun e(tag: String, message: String, throwable: Throwable?)
}
```

**Why abstracted:**
- Used throughout quartz module (protocol library)
- Logging systems differ:
  - Android: android.util.Log
  - Desktop: println or logging framework
  - iOS: NSLog or OSLog
- Simple interface, easy to implement

**Decision rationale:** Often abstract logging - platform systems differ, widely used, simple interface.

---

### 3. Platform Utils - Time & Platform Name

**Location:** expect functions in commonMain

**Code:**
```kotlin
// quartz/src/commonMain/.../Platform.kt
expect fun platform(): String
expect fun currentTimeSeconds(): Long
```

**Why abstracted:**
- Used by Nostr event creation (timestamps)
- Platform name for debugging
- Simple utilities, clear platform boundary

**Decision rationale:** Platform utilities are good abstraction candidates - simple, useful everywhere.

---

### 4. Jackson JSON (jvmAndroid Pattern)

**Location:** jvmAndroid source set

**Code:**
```kotlin
// quartz/build.gradle.kts
val jvmAndroid = create("jvmAndroid") {
    api(libs.jackson.module.kotlin)  // JVM-only library
}
```

**Why jvmAndroid (not commonMain):**
- Jackson is JVM-specific library
- Works on Android (JVM) + Desktop (JVM)
- Does NOT work on iOS (not JVM) or web (not JVM)
- Performance-critical JSON parsing

**Decision rationale:** Use jvmAndroid for JVM libraries shared between Android and Desktop.

**Future consideration:** For web support, migrate to kotlinx.serialization (works on all platforms).

---

## Bad/Over-Abstractions (Why They Failed)

### 1. Navigation Abstraction (Avoided)

**What COULD have been done:**
```kotlin
// ❌ Over-abstraction - DON'T DO THIS
expect interface Navigator {
    fun navigate(route: String)
    fun popBackStack()
}
```

**Why NOT abstracted:**
- Navigation paradigms fundamentally different:
  - Android: Activity + Compose Navigation + back stack
  - Desktop: Window + screen state + no back stack concept
- Complex APIs don't map well
- Creates leaky abstraction

**Actual approach:** Keep platform-specific
- Android: `INav` interface + Compose Navigation
- Desktop: Simple screen enum + state

**Decision rationale:** Never abstract navigation - platforms too different, abstraction would be leaky.

---

### 2. String Resources (Abstraction Planned)

**Current state:** Platform-specific (over-duplication)

**Problem:**
```kotlin
// Android uses R.string.*
Text(stringResource(R.string.post_not_found))

// Desktop uses hardcoded strings
Text("Post not found")
```

**Why NOT yet abstracted:** Waiting for second platform to fully implement UI, then will create StringProvider interface.

**Planned abstraction:**
```kotlin
// commonMain
interface StringProvider {
    fun get(key: String): String
}

// androidMain
class AndroidStringProvider(context: Context): StringProvider { ... }

// jvmMain
class DesktopStringProvider: StringProvider { ... }
```

**Lesson:** Don't abstract prematurely - wait until second platform needs it, then create proper abstraction.

---

## Platform-Specific Code (Why NOT Abstracted)

### 1. MainActivity (Android Activity)

**Location:** amethyst/src/main/.../MainActivity.kt

**Code:**
```kotlin
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        setContent {
            AmethystTheme {
                AccountScreen(accountStateViewModel)
            }
        }
    }
}
```

**Why platform-specific:**
- AppCompatActivity is Android framework
- Activity lifecycle unique to Android
- enableEdgeToEdge() is Android-specific API
- No equivalent on Desktop (uses Window)

**Decision rationale:** Android Activity is platform-specific by nature.

---

### 2. Desktop Window & MenuBar

**Location:** desktopApp/src/jvmMain/.../Main.kt

**Code:**
```kotlin
fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Amethyst"
    ) {
        MenuBar {
            Menu("File") {
                Item("New Note", onClick = { ... }, shortcut = KeyShortcut(Key.N, ctrl = true))
                Item("Quit", onClick = ::exitApplication)
            }
        }
        NavigationRail { ... }  // Sidebar navigation
    }
}
```

**Why platform-specific:**
- Window, MenuBar, NavigationRail are Compose Desktop APIs
- Keyboard shortcuts (Ctrl+N) are desktop paradigm
- Sidebar navigation vs Android bottom nav
- No equivalent on Android

**Decision rationale:** Desktop UX patterns are platform-specific by nature.

---

### 3. AccountViewModel (Android ViewModel)

**Location:** amethyst/.../AccountStateViewModel.kt

**Partially abstracted:**
- Business logic → IAccountState interface (can be shared)
- UI state + lifecycle → AndroidX ViewModel (Android-only)

**Why not fully abstracted:**
- AndroidX ViewModel lifecycle tied to Android
- Desktop doesn't need ViewModel (simpler state management)
- SavedStateHandle is Android-specific

**Decision rationale:** Extract business logic to interface, keep UI state platform-specific.

---

## Migration Examples (Android → Shared)

### Example 1: PubKeyFormatter (Pure Kotlin)

**Before:**
```kotlin
// amethyst/ui/note/PubKeyFormatter.kt
fun String.toDisplayHexKey(): String {
    return "${take(8)}:${takeLast(8)}"
}
```

**After:**
```kotlin
// commons/commonMain/formatters/PubKeyFormatter.kt
fun String.toDisplayHexKey(): String {
    return "${take(8)}:${takeLast(8)}"
}

// Both apps use it
import com.vitorpamplona.amethyst.commons.formatters.toDisplayHexKey
```

**Why successful:**
- Pure Kotlin, no platform dependencies
- Widely reused
- Simple utility function

---

### Example 2: TimeAgoFormatter (Requires Abstraction)

**Problem:**
```kotlin
// Uses Android R.string.*
fun timeAgo(timestamp: Long): String {
    return context.getString(R.string.x_minutes_ago, minutes)
}
```

**Solution:** Abstract string resources
```kotlin
// commonMain
fun timeAgo(timestamp: Long, stringProvider: StringProvider): String {
    return stringProvider.get("x_minutes_ago", minutes)
}

// androidMain
stringProvider = AndroidStringProvider(context)

// jvmMain
stringProvider = DesktopStringProvider()
```

**Why successful:** Clear platform boundary (string resources), useful on both platforms.

---

## Decision Pattern Summary

| Pattern | Abstract? | Why |
|---------|-----------|-----|
| Pure Kotlin utilities | ✅ YES | No platform dependency, easy |
| Crypto APIs | ✅ YES (expect/actual) | Platform security APIs differ |
| JVM libraries | ⚠️ jvmAndroid | Works on Android+Desktop only |
| UI components (simple) | ✅ YES | Composables work cross-platform |
| UI components (complex) | ❌ NO | Platform dependencies |
| Navigation | ❌ NO | Paradigms too different |
| ViewModels | ⚠️ PARTIAL | Business logic yes, UI state no |
| String resources | ⚠️ PLANNED | Needs abstraction layer |
