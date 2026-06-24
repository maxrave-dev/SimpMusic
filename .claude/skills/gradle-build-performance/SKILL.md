---
name: gradle-build-performance
description: Debug and optimize Android/Gradle build performance. Use when builds are slow, investigating CI/CD performance, analyzing build scans, or identifying compilation bottlenecks.
---

# Gradle Build Performance

## When to Use

- Build times are slow (clean or incremental)
- Investigating build performance regressions
- Analyzing Gradle Build Scans
- Identifying configuration vs execution bottlenecks
- Optimizing CI/CD build times
- Enabling Gradle Configuration Cache
- Reducing unnecessary recompilation
- Debugging kapt/KSP annotation processing

## Example Prompts

- "My builds are slow, how can I speed them up?"
- "How do I analyze a Gradle build scan?"
- "Why is configuration taking so long?"
- "Why does my project always recompile everything?"
- "How do I enable configuration cache?"
- "Why is kapt so slow?"

---

## Workflow

1. **Measure Baseline** — Clean build + incremental build times
2. **Generate Build Scan** — `./gradlew assembleDebug --scan`
3. **Identify Phase** — Configuration? Execution? Dependency resolution?
4. **Apply ONE optimization** — Don't batch changes
5. **Measure Improvement** — Compare against baseline
6. **Verify in Build Scan** — Visual confirmation

---

## Quick Diagnostics

### Generate Build Scan

```bash
./gradlew assembleDebug --scan
```

### Profile Build Locally

```bash
./gradlew assembleDebug --profile
# Opens report in build/reports/profile/
```

### Build Timing Summary

```bash
./gradlew assembleDebug --info | grep -E "^\:.*"
# Or view in Android Studio: Build > Analyze APK Build
```

---

## Build Phases

| Phase | What Happens | Common Issues |
|-------|--------------|---------------|
| **Initialization** | `settings.gradle.kts` evaluated | Too many `include()` statements |
| **Configuration** | All `build.gradle.kts` files evaluated | Expensive plugins, eager task creation |
| **Execution** | Tasks run based on inputs/outputs | Cache misses, non-incremental tasks |

### Identify the Bottleneck

```
Build scan → Performance → Build timeline
```

- **Long configuration phase**: Focus on plugin and buildscript optimization
- **Long execution phase**: Focus on task caching and parallelization
- **Dependency resolution slow**: Focus on repository configuration

---

## 12 Optimization Patterns

### 1. Enable Configuration Cache

Caches configuration phase across builds (AGP 8.0+):

```properties
# gradle.properties
org.gradle.configuration-cache=true
org.gradle.configuration-cache.problems=warn
```

### 2. Enable Build Cache

Reuses task outputs across builds and machines:

```properties
# gradle.properties
org.gradle.caching=true
```

### 3. Enable Parallel Execution

Build independent modules simultaneously:

```properties
# gradle.properties
org.gradle.parallel=true
```

### 4. Increase JVM Heap

Allocate more memory for large projects:

```properties
# gradle.properties
org.gradle.jvmargs=-Xmx4g -XX:+UseParallelGC
```

### 5. Use Non-Transitive R Classes

Reduces R class size and compilation (AGP 8.0+ default):

```properties
# gradle.properties
android.nonTransitiveRClass=true
```

### 6. Migrate kapt to KSP

KSP is 2x faster than kapt for Kotlin:

```kotlin
// Before (slow)
kapt("com.google.dagger:hilt-compiler:2.51.1")

// After (fast)
ksp("com.google.dagger:hilt-compiler:2.51.1")
```

### 7. Avoid Dynamic Dependencies

Pin dependency versions:

```kotlin
// BAD: Forces resolution every build
implementation("com.example:lib:+")
implementation("com.example:lib:1.0.+")

// GOOD: Fixed version
implementation("com.example:lib:1.2.3")
```

### 8. Optimize Repository Order

Put most-used repositories first:

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        google()      // First: Android dependencies
        mavenCentral() // Second: Most libraries
        // Third-party repos last
    }
}
```

### 9. Use includeBuild for Local Modules

Composite builds are faster than `project()` for large monorepos:

```kotlin
// settings.gradle.kts
includeBuild("shared-library") {
    dependencySubstitution {
        substitute(module("com.example:shared")).using(project(":"))
    }
}
```

### 10. Enable Incremental Annotation Processing

```properties
# gradle.properties
kapt.incremental.apt=true
kapt.use.worker.api=true
```

### 11. Avoid Configuration-Time I/O

Don't read files or make network calls during configuration:

```kotlin
// BAD: Runs during configuration
val version = file("version.txt").readText()

// GOOD: Defer to execution
val version = providers.fileContents(file("version.txt")).asText
```

### 12. Use Lazy Task Configuration

Avoid `create()`, use `register()`:

```kotlin
// BAD: Eagerly configured
tasks.create("myTask") { ... }

// GOOD: Lazily configured
tasks.register("myTask") { ... }
```

---

## Common Bottleneck Analysis

### Slow Configuration Phase

**Symptoms**: Build scan shows long "Configuring build" time

**Causes & Fixes**:
| Cause | Fix |
|-------|-----|
| Eager task creation | Use `tasks.register()` instead of `tasks.create()` |
| buildSrc with many dependencies | Migrate to Convention Plugins with `includeBuild` |
| File I/O in build scripts | Use `providers.fileContents()` |
| Network calls in plugins | Cache results or use offline mode |

### Slow Compilation

**Symptoms**: `:app:compileDebugKotlin` takes too long

**Causes & Fixes**:
| Cause | Fix |
|-------|-----|
| Non-incremental changes | Avoid `build.gradle.kts` changes that invalidate cache |
| Large modules | Break into smaller feature modules |
| Excessive kapt usage | Migrate to KSP |
| Kotlin compiler memory | Increase `kotlin.daemon.jvmargs` |

### Cache Misses

**Symptoms**: Tasks always rerun despite no changes

**Causes & Fixes**:
| Cause | Fix |
|-------|-----|
| Unstable task inputs | Use `@PathSensitive`, `@NormalizeLineEndings` |
| Absolute paths in outputs | Use relative paths |
| Missing `@CacheableTask` | Add annotation to custom tasks |
| Different JDK versions | Standardize JDK across environments |

---

## CI/CD Optimizations

### Remote Build Cache

```kotlin
// settings.gradle.kts
buildCache {
    local { isEnabled = true }
    remote<HttpBuildCache> {
        url = uri("https://cache.example.com/")
        isPush = System.getenv("CI") == "true"
        credentials {
            username = System.getenv("CACHE_USER")
            password = System.getenv("CACHE_PASS")
        }
    }
}
```

### Gradle Enterprise / Develocity

For advanced build analytics:

```kotlin
// settings.gradle.kts
plugins {
    id("com.gradle.develocity") version "3.17"
}

develocity {
    buildScan {
        termsOfUseUrl.set("https://gradle.com/help/legal-terms-of-use")
        termsOfUseAgree.set("yes")
        publishing.onlyIf { System.getenv("CI") != null }
    }
}
```

### Skip Unnecessary Tasks in CI

```bash
# Skip tests for UI-only changes
./gradlew assembleDebug -x test -x lint

# Only run affected module tests
./gradlew :feature:login:test
```

---

## Android Studio Settings

### File → Settings → Build → Gradle

- **Gradle JDK**: Match your project's JDK
- **Build and run using**: Gradle (not IntelliJ)
- **Run tests using**: Gradle

### File → Settings → Build → Compiler

- **Compile independent modules in parallel**: ✅ Enabled
- **Configure on demand**: ❌ Disabled (deprecated)

---

## Verification Checklist

After optimizations, verify:

- [ ] Configuration cache enabled and working
- [ ] Build cache hit rate > 80% (check build scan)
- [ ] No dynamic dependency versions
- [ ] KSP used instead of kapt where possible
- [ ] Parallel execution enabled
- [ ] JVM memory tuned appropriately
- [ ] CI remote cache configured
- [ ] No configuration-time I/O

---

## References

- [Optimize Build Speed](https://developer.android.com/build/optimize-your-build)
- [Gradle Configuration Cache](https://docs.gradle.org/current/userguide/configuration_cache.html)
- [Gradle Build Cache](https://docs.gradle.org/current/userguide/build_cache.html)
- [Migrate from kapt to KSP](https://developer.android.com/build/migrate-to-ksp)
- [Gradle Build Scans](https://scans.gradle.com/)
