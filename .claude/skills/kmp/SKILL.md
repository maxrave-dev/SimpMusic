---
name: kotlin-multiplatform
description: |
  Platform abstraction decision-making for Amethyst KMP project. Guides when to abstract vs keep platform-specific,
  source set placement (commonMain, jvmAndroid, platform-specific), expect/actual patterns. Covers primary targets
  (Android, JVM/Desktop, iOS) with web/wasm future considerations. Integrates with gradle-expert for dependency issues.
  Triggers on: abstraction decisions ("should I share this?"), source set placement questions, expect/actual creation,
  build.gradle.kts work, incorrect placement detection, KMP dependency suggestions.
---

# Kotlin Multiplatform: Platform Abstraction Decisions

Expert guidance for KMP architecture in Amethyst - deciding what to share vs keep platform-specific.

## When to Use This Skill

Making platform abstraction decisions:
- "Should I create expect/actual or keep Android-only?"
- "Can I share this ViewModel logic?"
- "Where does this crypto/JSON/network implementation belong?"
- "This uses Android Context - can it be abstracted?"
- "Is this code in the wrong module?"
- Preparing for iOS/web/wasm targets
- Detecting incorrect placements

## Abstraction Decision Tree

**Central question:** "Should this code be reused across platforms?"

Follow this decision path (< 1 minute):

```
Q: Is it used by 2+ platforms?
├─ NO  → Keep platform-specific
│         Example: Android-only permission handling
│
└─ YES → Continue ↓

Q: Is it pure Kotlin (no platform APIs)?
├─ YES → commonMain
│         Example: Nostr event parsing, business rules
│
└─ NO  → Continue ↓

Q: Does it vary by platform or by JVM vs non-JVM?
├─ By platform (Android ≠ iOS ≠ Desktop)
│  → expect/actual
│  Example: Secp256k1Instance (uses different security APIs)
│
├─ By JVM (Android = Desktop ≠ iOS/web)
│  → jvmAndroid
│  Example: Jackson JSON parsing (JVM library)
│
└─ Complex/UI-related
   → Keep platform-specific
   Example: Navigation (Activity vs Window too different)

Final check:
Q: Maintenance cost of abstraction < duplication cost?
├─ YES → Proceed with abstraction
└─ NO  → Duplicate (simpler)
```

### Real Examples from Codebase

**Crypto → expect/actual:**
```kotlin
// commonMain - expect declaration
expect object Secp256k1Instance {
    fun signSchnorr(data: ByteArray, privKey: ByteArray): ByteArray
}

// androidMain - uses Android Keystore
// jvmMain - uses Desktop JVM crypto
// iosMain - uses iOS Security framework
```
**Why:** Each platform has different security APIs.

**JSON parsing → jvmAndroid:**
```kotlin
// quartz/build.gradle.kts
val jvmAndroid = create("jvmAndroid") {
    api(libs.jackson.module.kotlin)
}
```
**Why:** Jackson is JVM-only, works on Android + Desktop, not iOS/web.

**Navigation → platform-specific:**
- Android: `MainActivity` (Activity + Compose Navigation)
- Desktop: `Window` + sidebar + MenuBar
**Why:** UI paradigms fundamentally different.

## Mental Model: Source Sets as Dependency Graph

Think of source sets as a dependency graph, not folders.

```
┌─────────────────────────────────────────────┐
│ commonMain = Contract (pure Kotlin)         │
│ - Business logic, protocol, data models     │
│ - No platform APIs                          │
└────────────┬────────────────────────────────┘
             │
             ├──────────────────────┬────────────────────
             │                      │
             ▼                      ▼
   ┌───────────────────┐  ┌──────────────────┐
   │ jvmAndroid        │  │ iosMain          │
   │ JVM libs shared   │  │ iOS common       │
   │ - Jackson         │  │                  │
   │ - OkHttp          │  └────┬─────────────┘
   └───┬───────────┬───┘       │
       │           │            ├─→ iosX64Main
       ▼           ▼            ├─→ iosArm64Main
  ┌─────────┐ ┌──────────┐     └─→ iosSimulatorArm64Main
  │android  │ │jvmMain   │
  │Main     │ │(Desktop) │
  └─────────┘ └──────────┘

Future: jsMain, wasmMain
```

**Key insight:** jvmAndroid is NOT a platform - it's a shared JVM layer.

## The jvmAndroid Pattern

**Unique to Amethyst.** Shares JVM libraries between Android + Desktop.

### When to Use jvmAndroid

Use jvmAndroid when:
- ✅ JVM-specific libraries (Jackson, OkHttp, url-detector)
- ✅ Android implementation = Desktop implementation (same JVM)
- ✅ Library doesn't work on iOS/web

Do NOT use jvmAndroid for:
- ❌ Pure Kotlin code (use commonMain)
- ❌ Platform-specific APIs (use androidMain/jvmMain)
- ❌ Code that should work on all platforms

### Example from quartz/build.gradle.kts

```kotlin
// Must be defined BEFORE androidMain and jvmMain
val jvmAndroid = create("jvmAndroid") {
    dependsOn(commonMain.get())

    dependencies {
        api(libs.jackson.module.kotlin)  // JSON parsing - JVM only
        api(libs.url.detector)            // URL extraction - JVM only
        implementation(libs.okhttp)       // HTTP client - JVM only
    }
}

// Both depend on jvmAndroid
jvmMain { dependsOn(jvmAndroid) }
androidMain { dependsOn(jvmAndroid) }
```

**Why Jackson in jvmAndroid, not commonMain?**
- Jackson is JVM-specific library
- Works on Android (runs on JVM)
- Works on Desktop (runs on JVM)
- Does NOT work on iOS (not JVM) or web (not JVM)

**Web/wasm consideration:** For future web support, consider migrating from Jackson → kotlinx.serialization (see Target-Specific Guidance).

## What to Abstract vs Keep Platform-Specific

Quick decision guidelines based on codebase patterns:

### Always Abstract
- **Crypto** (Secp256k1, encryption, signing)
- **Core protocol logic** (Nostr events, NIPs)
- **Why:** Needed everywhere, platform security APIs vary

### Often Abstract
- **I/O operations** (file reading, caching)
- **Logging** (platform logging systems differ)
- **Serialization** (if using kotlinx.serialization)
- **Why:** Commonly reused, platform implementations available

### Sometimes Abstract
- **Business logic:** YES - state machines, data processing
- **ViewModels:** YES - state + business logic shareable (StateFlow/SharedFlow)
- **Screen layouts:** NO - platform-native (Window vs Activity)
- **Why:** ViewModels contain platform-agnostic state; Screens render differently per platform

### Rarely Abstract
- **Complex UI components** (composables with heavy platform dependencies)
- **Why:** Platform paradigms can differ significantly

### Never Abstract
- **Navigation** (Activity vs Window fundamentally different)
- **Permissions** (Android vs iOS APIs incompatible)
- **Platform UX patterns**
- **Why:** Too platform-specific, abstraction creates leaky APIs

### Evidence from shared-ui-analysis.md

| Component | Shared? | Rationale |
|-----------|---------|-----------|
| PubKeyFormatter, ZapFormatter | ✅ YES | Pure Kotlin, no platform APIs |
| TimeAgoFormatter | ⚠️ ABSTRACTED | Needs StringProvider for localized strings |
| ViewModels (state + logic) | ✅ YES | StateFlow/SharedFlow platform-agnostic, Compose Multiplatform lifecycle compatible |
| Screen layouts (Scaffold, nav) | ❌ NO | Window vs Activity, sidebar vs bottom nav fundamentally different |
| Image loading (Coil) | ⚠️ ABSTRACTED | Coil 3.x supports KMP, needs expect/actual wrapper |

## expect/actual Mechanics

**When to use:** Code needed by 2+ platforms, varies by platform.

### Pattern Categories from Codebase

**Objects (singletons):**
```kotlin
// 24 expect declarations found, common pattern:
expect object Secp256k1Instance { ... }
expect object Log { ... }
expect object LibSodiumInstance { ... }
```

**Classes (instantiable):**
```kotlin
expect class AESCBC { ... }
expect class DigestInstance { ... }
```

**Functions (utilities):**
```kotlin
expect fun platform(): String
expect fun currentTimeSeconds(): Long
```

**See** [references/expect-actual-catalog.md](references/expect-actual-catalog.md) for complete catalog with rationale.

## Target-Specific Guidance

### Android, JVM (Desktop), iOS - Current Primary Targets

**Status:** Mature patterns, stable APIs

**Android (androidMain):**
- Uses Android framework (Activity, Context, etc.)
- secp256k1-kmp-jni-android for crypto
- AndroidX libraries

**Desktop JVM (jvmMain):**
- Uses Compose Desktop (Window, MenuBar, etc.)
- secp256k1-kmp-jni-jvm for crypto
- Pure JVM libraries

**iOS (iosMain):**
- Active development, framework configured
- Architecture targets: iosX64Main, iosArm64Main, iosSimulatorArm64Main
- Platform APIs via platform.posix, Security framework

### Web, wasm - Future Targets

**Status:** Not yet implemented, consider for future-proofing

**Constraints to know:**
- ❌ No platform.posix (file I/O different)
- ❌ No JVM libraries (Jackson, OkHttp won't work)
- ❌ Different async model (JS event loop vs threads)

**Future-proofing tips:**
1. Prefer pure Kotlin in commonMain
2. Use kotlinx.* libraries:
   - kotlinx.serialization instead of Jackson
   - ktor instead of OkHttp (ktor supports web)
   - kotlinx.datetime instead of custom date handling
3. Avoid platform.posix for file operations
4. Test abstractions work without JVM assumptions

**Example migration path:**
```kotlin
// Current: jvmAndroid (JVM-only)
api(libs.jackson.module.kotlin)

// Future: commonMain (all platforms)
api(libs.kotlinx.serialization.json)
```

## Integration: When to Invoke Other Skills

### Invoke gradle-expert

Trigger gradle-expert skill when encountering:
- Dependency conflicts (e.g., secp256k1-android vs secp256k1-jvm version mismatch)
- Build errors related to source sets
- Version catalog issues (libs.versions.toml)
- "Duplicate class" errors
- Performance/build time issues

**Example trigger:**
```
Error: Duplicate class found: fr.acinq.secp256k1.Secp256k1
```
→ Invoke gradle-expert for dependency conflict resolution.

### Flags to Raise

**Platform code in commonMain:**
```kotlin
// ❌ INCORRECT - Android API in commonMain
expect fun getContext(): Context  // Context is Android-only!
```
→ Flag: "Android API in commonMain won't compile on other platforms"

**Duplicated business logic:**
```kotlin
// ❌ INCORRECT - Same logic in both
// androidMain/.../CryptoUtils.kt
fun validateSignature(...) { ... }

// jvmMain/.../CryptoUtils.kt
fun validateSignature(...) { ... }  // Duplicated!
```
→ Flag: "Business logic duplicated, should be in commonMain or expect/actual"

**Reinventing wheel - suggest KMP alternatives:**
- Custom date/time → kotlinx.datetime
- OkHttp → ktor (supports web)
- Jackson → kotlinx.serialization
- Custom UUID → kotlinx.uuid (when stable)

## Common Pitfalls

### 1. Over-Abstraction
**Problem:** Creating expect/actual for UI components
```kotlin
// ❌ BAD
expect fun NavigationComponent(...)
```
**Why:** Navigation paradigms too different (Activity vs Window)
**Fix:** Keep platform-specific, accept duplication

### 2. Under-Sharing
**Problem:** Duplicating business logic across platforms
```kotlin
// ❌ BAD - duplicated in androidMain and jvmMain
fun parseNostrEvent(json: String): Event { ... }
```
**Why:** Bug fixes need to be applied twice, tests duplicated
**Fix:** Move to commonMain (pure Kotlin) or create expect/actual

### 3. Leaky Abstractions
**Problem:** Platform code in commonMain
```kotlin
// commonMain - ❌ BAD
import android.content.Context  // Won't compile on iOS!
```
**Fix:** Use expect/actual or dependency injection

### 4. Premature Abstraction
**Problem:** Creating expect/actual before second platform needs it
```kotlin
// ❌ BAD - only used on Android currently
expect fun showNotification(...)
```
**Why:** Wrong abstraction boundaries, wasted effort
**Fix:** Wait until iOS actually needs it, then abstract

### 5. Wrong Source Set
**Problem:** JVM libraries in commonMain
```kotlin
// commonMain - ❌ BAD
import com.fasterxml.jackson.databind.ObjectMapper
```
**Why:** Jackson won't compile on iOS/web
**Fix:** Move to jvmAndroid or migrate to kotlinx.serialization

## Quick Reference

| Code Type | Recommended Location | Reason |
|-----------|---------------------|--------|
| Pure Kotlin business logic | commonMain | Works everywhere |
| Nostr protocol, NIPs | commonMain | Core logic, no platform APIs |
| JVM libs (Jackson, OkHttp) | jvmAndroid | Android + Desktop only |
| Crypto (varies by platform) | expect in commonMain, actual in platforms | Different security APIs per platform |
| I/O, logging | expect in commonMain, actual in platforms | Platform implementations differ |
| State (business logic) | commonMain or commons/jvmAndroid | Reusable StateFlow patterns |
| **ViewModels** | **commons/commonMain/viewmodels/** | **StateFlow/SharedFlow + logic shareable, Compose MP lifecycle compatible** |
| UI formatters (pure) | commons/commonMain | Reusable, no dependencies |
| UI components (simple) | commons/commonMain | Cards, buttons, dialogs |
| **Screen layouts** | **Platform-specific** | **Window vs Activity, sidebar vs bottom nav** |
| Navigation | Platform-specific only | Activity vs Window too different |
| Permissions | Platform-specific only | APIs incompatible |
| Platform UX (menus, etc.) | Platform-specific only | Native feel required |

## See Also

- [references/abstraction-examples.md](references/abstraction-examples.md) - Good/bad abstraction examples with rationale
- [references/source-set-hierarchy.md](references/source-set-hierarchy.md) - Visual hierarchy with Amethyst examples
- [references/expect-actual-catalog.md](references/expect-actual-catalog.md) - All 24 expect/actual pairs with "why abstracted"
- [references/target-compatibility.md](references/target-compatibility.md) - Platform constraints and future-proofing

## Scripts

- `scripts/validate-kmp-structure.sh` - Detect incorrect placements, validate source sets
- `scripts/suggest-kmp-dependency.sh` - Suggest KMP library alternatives (ktor, kotlinx.serialization, etc.)
