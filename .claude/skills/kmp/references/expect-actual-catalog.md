# Complete expect/actual Catalog

All 24 expect declarations in Amethyst quartz module with rationale.

| # | Name | Type | Purpose | Why Abstracted | Files |
|---|------|------|---------|----------------|-------|
| 1 | AESCBC | class | AES CBC encryption | Platform crypto APIs differ | quartz/.../ciphers/AESCBC.kt |
| 2 | AESGCM | class | AES GCM encryption | Platform crypto APIs differ | quartz/.../ciphers/AESGCM.kt |
| 3 | DigestInstance | class | Hash digests (SHA256) | Platform implementations | quartz/.../diggest/DigestInstance.kt |
| 4 | MacInstance | class | MAC (HMAC) operations | Platform crypto APIs | quartz/.../mac/MacInstance.kt |
| 5 | Sha256 | object | SHA256 hashing | Platform-specific optimizations | quartz/.../sha256/Sha256.kt |
| 6 | LargeCache | object | Large object caching | Platform storage APIs differ | quartz/.../cache/LargeCache.kt |
| 7 | UriParser | object | URI parsing | Platform URL APIs differ | quartz/.../UriParser.kt |
| 8 | UrlEncoder | object | URL encoding | Platform encoding differs | quartz/.../UrlEncoder.kt |
| 9 | Urls | object | URL utilities | Platform URL handling | quartz/.../Urls.kt |
| 10 | Platform | functions | platform(), currentTimeSeconds() | Platform name & time APIs | quartz/.../Platform.kt |
| 11 | Rfc3986 | object | RFC 3986 URL normalization | Used in jvmAndroid | quartz/.../Rfc3986.kt |
| 12 | Secp256k1Instance | object | Bitcoin crypto (secp256k1) | Different libs per platform | quartz/.../Secp256k1Instance.kt |
| 13 | SecureRandom | object | Cryptographically secure random | Platform random APIs differ | quartz/.../SecureRandom.kt |
| 14 | StringExt | functions | String utilities | Platform string handling | quartz/.../StringExt.kt |
| 15 | UnicodeNormalizer | object | Unicode normalization | Platform text APIs | quartz/.../UnicodeNormalizer.kt |
| 16 | GZip | object | GZip compression | Platform compression APIs | quartz/.../GZip.kt |
| 17 | LibSodiumInstance | object | NaCl/libsodium (NIP-44 encryption) | Different libs per platform | quartz/.../LibSodiumInstance.kt |
| 18 | Log | object | Logging | Platform logging systems | quartz/.../Log.kt |
| 19 | BigDecimal | class | Arbitrary precision decimal | Not in Kotlin common stdlib | quartz/.../BigDecimal.kt |
| 20 | BitSet | class | Bit set data structure | Not in Kotlin common stdlib | quartz/.../BitSet.kt |
| 21 | ServerInfoParser | object | Server info parsing (NIP-96) | Platform JSON parsing | quartz/.../nip96.../ServerInfoParser.kt |
| 22 | EventHasherSerializer | object | Event hashing | Platform-specific optimizations | quartz/.../nip01Core.../EventHasherSerializer.kt |
| 23 | OptimizedJsonMapper | object | JSON mapping | Platform JSON libraries | quartz/.../nip01Core.../OptimizedJsonMapper.kt |
| 24 | Address | data class | Address data structure | Platform-specific string handling | quartz/.../nip01Core.../Address.kt |

## Pattern Analysis

### Objects (Singletons) - 19 total
Most common pattern for platform-specific singletons:
- Crypto: Secp256k1Instance, LibSodiumInstance, Sha256
- I/O: UriParser, UrlEncoder, GZip
- Utils: Log, Platform, SecureRandom

### Classes (Instantiable) - 4 total
For objects that need to maintain state:
- AESCBC, AESGCM (cipher state)
- DigestInstance, MacInstance (hash/MAC state)
- BigDecimal, BitSet (data structures)

### Functions - 2 total
Simple utilities:
- platform(), currentTimeSeconds()

## Why Abstracted Categories

### Crypto (8 items)
**Always abstract:** Security APIs fundamentally different across platforms
- Android: Android Keystore, secp256k1-android
- Desktop: JVM crypto, secp256k1-jvm
- iOS: Security framework, native crypto

### I/O & Platform Utils (7 items)
**Often abstract:** File systems, URLs, compression differ
- Platform storage APIs
- URL handling varies
- Compression libraries differ

### Data Structures (2 items)
**Abstract when missing:** Not available in Kotlin common stdlib
- BigDecimal, BitSet not in common

### JSON/Parsing (3 items)
**Platform-specific optimization:** Uses platform JSON libraries
- Android/Desktop: Jackson (via jvmAndroid)
- iOS: Native parsers

### Logging (1 item)
**Always abstract:** Platform logging systems differ
- Android: android.util.Log
- Desktop: println or logging framework
- iOS: NSLog or OSLog

## Actual Implementation Examples

### Simple Object Pattern

```kotlin
// commonMain
expect object Log {
    fun d(tag: String, message: String)
}

// androidMain
actual object Log {
    actual fun d(tag: String, message: String) {
        android.util.Log.d(tag, message)
    }
}

// jvmMain
actual object Log {
    actual fun d(tag: String, message: String) {
        println("[$tag] $message")
    }
}
```

### Complex Object with Dependencies

```kotlin
// commonMain
expect object Secp256k1Instance {
    fun signSchnorr(data: ByteArray, privKey: ByteArray): ByteArray
}

// androidMain - uses JNI bindings
actual object Secp256k1Instance {
    actual fun signSchnorr(data: ByteArray, privKey: ByteArray): ByteArray {
        return fr.acinq.secp256k1.Secp256k1.signSchnorr(data, privKey, null)
    }
}

// jvmMain - different JNI library
actual object Secp256k1Instance {
    actual fun signSchnorr(data: ByteArray, privKey: ByteArray): ByteArray {
        return fr.acinq.secp256k1.Secp256k1.signSchnorr(data, privKey, null)
    }
}

// iosMain - native iOS implementation
actual object Secp256k1Instance {
    actual fun signSchnorr(data: ByteArray, privKey: ByteArray): ByteArray {
        // Uses iOS Security framework or native lib
    }
}
```

### Class Pattern

```kotlin
// commonMain
expect class BigDecimal {
    constructor(value: String)
    fun add(other: BigDecimal): BigDecimal
    override fun toString(): String
}

// jvmAndroid (works on Android + Desktop)
actual typealias BigDecimal = java.math.BigDecimal

// iosMain
actual class BigDecimal {
    private val value: NSDecimalNumber
    actual constructor(value: String) {
        this.value = NSDecimalNumber(value)
    }
    // ... implementation
}
```

## Decision Patterns

Ask for each declaration:
1. **Used by 2+ platforms?** → YES (otherwise platform-specific)
2. **Pure Kotlin possible?** → NO (otherwise commonMain)
3. **Varies by platform?** → YES (expect/actual)
4. **JVM-only library?** → NO (otherwise jvmAndroid)
