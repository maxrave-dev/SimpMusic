@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.INT
import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING
import org.gradle.api.file.RelativePath
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.net.URI
import java.util.Properties

val isFullBuild: Boolean =
    try {
        extra["isFullBuild"] == "true"
    } catch (e: Exception) {
        false
    }

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.aboutlibraries.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.build.config)
    alias(libs.plugins.osdetector)
    alias(libs.plugins.packagedeps)
}

// composeApp uses the `android.kotlin.multiplatform.library` plugin, so with the
// default `generateResClass = auto` Compose skips generating the `Res` class
// (it treats a KMP *library* module as not owning the public resource class).
// Force it to `always` so `Res` is generated for this app module.
compose.resources {
    generateResClass = always
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xwhen-guards")
        freeCompilerArgs.add("-Xcontext-parameters")
        freeCompilerArgs.add("-Xmulti-dollar-interpolation")
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
    android {
        namespace = "com.maxrave.simpmusic.composeapp"
        compileSdk = 37
        minSdk = 26
        withJava()
        androidResources {
            enable = true
        }
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

//    listOf(
//        iosArm64(),
//        iosSimulatorArm64()
//    ).forEach { iosTarget ->
//        iosTarget.binaries.framework {
//            baseName = "ComposeApp"
//            isStatic = true
//        }
//    }

    jvm()

    sourceSets {
        dependencies {
            val composeBom = project.dependencies.platform(libs.compose.bom)
            val koinBom = project.dependencies.platform(libs.koin.bom)
            implementation(composeBom)
            implementation(koinBom)
            implementation(libs.commons.io)
        }
        androidMain.dependencies {
            api(project.dependencies.platform(libs.koin.bom))
            api(libs.koin.android)
            implementation(libs.koin.androidx.compose)

            implementation(libs.jetbrains.ui.tooling.preview)
            implementation(libs.constraintlayout.compose)

            api(libs.work.runtime.ktx)

            // Runtime
            api(libs.startup.runtime)

            api(projects.media3)
            api(projects.media3Ui)

            // Google Cast (gated: real SDK for full builds, no-op stub for FOSS builds)
            if (isFullBuild) {
                implementation(projects.cast)
            } else {
                implementation(projects.castEmpty)
            }
        }
        commonMain.dependencies {
            implementation(libs.runtime)
            implementation(libs.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.components.resources)
            implementation(libs.jetbrains.ui.tooling.preview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)

            // Compose
            implementation(libs.compose.material3.adaptive)
            implementation(libs.compose.material.ripple)
            implementation(libs.compose.material.icons.core)
            implementation(libs.compose.material.icons.extended)

            implementation(libs.ui.tooling.preview)

            // Other module
            api(projects.common)
            api(projects.domain)
            implementation(projects.data)

            // Navigation Compose
            implementation(libs.navigation.compose)

            // Kotlin Serialization
            implementation(libs.kotlinx.serialization.json)

            // Coil
            api(libs.coil.compose)
            api(libs.coil.network.okhttp)
            api(libs.kmpalette.core)
            api(libs.kmpalette.network)
            implementation(libs.materialkolor)
            implementation(libs.ktor.client.cio)

            // DataStore
            implementation(libs.datastore.preferences)

            // Lottie
            implementation(libs.compottie)
            implementation(libs.compottie.dot)
            implementation(libs.compottie.network)
            implementation(libs.compottie.resources)

            // Paging 3
            implementation(libs.androidx.paging.common)
            implementation(libs.paging.compose)

            implementation(libs.aboutlibraries)
            implementation(libs.aboutlibraries.compose.m3)

            // Koin
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)

            // Jetbrains Markdown
            api(libs.markdown)

            // Blur Haze
            implementation(libs.haze)
            implementation(libs.haze.material)

            api(libs.cmptoast)
            implementation(libs.file.picker)

            // Liquid glass
            implementation(libs.liquid.glass)
            implementation(libs.liquid.glass.shape)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jvmMain.dependencies {
            // Desktop app entry (main.kt), jpackage/Conveyor
            // packaging, and tray icon live in :desktopApp per the
            // JetBrains 2026 KMP default structure. This module keeps the
            // shared JVM UI + expect/actuals and their direct dependencies.
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
            implementation(libs.sentry.jvm)
            implementation(libs.native.tray)
            implementation(projects.mediaJvmUi)
        }
    }
}

// NOTE: compose.desktop{} application block, ProGuard config,
// linuxDebConfig{}, the custom AppImage tooling, and Conveyor packaging
// live in :desktopApp per the JetBrains 2026 KMP default structure.
//
// Native-library staging (libmpv) lives here in :composeApp — see the
// mpv-natives block below. The layout is per-arch so Conveyor bundles only
// the slice each per-machine installer actually needs.

fun downloadIfMissing(url: String, target: java.io.File, logPrefix: String = "mpv-multi") {
    if (target.exists() && target.length() > 0) {
        logger.lifecycle("[$logPrefix] Cached: ${target.name}")
        return
    }
    logger.lifecycle("[$logPrefix] Downloading $url")
    target.parentFile.mkdirs()
    // Use curl instead of Java's URL.openStream(): release downloads answer
    // with a redirect to a CDN, and Java's default HttpURLConnection redirect
    // handling is fragile — on a cross-protocol redirect or an HTML error
    // page, openStream() silently saves the error body as the target file,
    // which only surfaces as a corrupt-archive failure later. curl's `-L`
    // follows redirects robustly across protocols + mirrors, `--fail`
    // exits non-zero on HTTP errors instead of saving error bodies, and
    // `-o` writes atomically via tmp file. The downloaded artifact is
    // also size-verified (must match Content-Length).
    val curlExit = ProcessBuilder(
        "curl",
        "-fsSL",
        // `--retry` alone does NOT retry curl exit 56 (mid-transfer receive failure) — it only
        // retries HTTP 5xx/408/429 and connection errors. The get.videolan.org mirrors flake with
        // exit 56 mid-download, so `--retry-all-errors` is required to retry those too. Count/delay
        // bumped a bit for the occasionally-slow mirror.
        "--retry", "5",
        "--retry-delay", "5",
        "--retry-all-errors",
        "-o", target.absolutePath,
        url,
    ).inheritIO().start().waitFor()
    check(curlExit == 0 && target.exists() && target.length() > 0) {
        // Delete partial/empty file so the next run can retry cleanly.
        if (target.exists()) target.delete()
        "curl failed (exit $curlExit) downloading $url to $target"
    }
}

// ===========================================================================
// mpv natives (libmpv) — staged into mpv-natives/<os>-<arch>/ for packaging.
//
// mpv publishes no portable libmpv of its own and there is no Gradle plugin
// that fetches one, so each OS lifts libmpv out of a prebuilt artifact that
// ALREADY ships a relocatable dependency closure. That is what keeps this
// cheap: gathering ffmpeg / libplacebo / libass / luajit by hand and
// rewriting their install names is precisely the work these upstreams have
// already done and keep doing on every release.
//
//   Windows  shinchiro/mpv-winbuild-cmake `mpv-dev-<arch>.7z`
//            → libmpv-2.dll with ffmpeg linked in. Nothing to patch.
//   Linux    pkgforge-dev/mpv-AppImage
//            → libmpv.so.2 + closure under shared/lib with RPATH=$ORIGIN
//              (sharun), so nothing to patch there either.
//   macOS    Homebrew's mpv + dylibbundler, run on a macOS host.
//
// macOS is the odd one out and deliberately so. The obvious shortcut — lifting
// IINA.app/Contents/Frameworks straight out of IINA's .dmg — DOES NOT WORK,
// and fails in a way worth recording so nobody retries it. In IINA 1.4.4 the
// bundled pair is version-skewed:
//
//   nm -u  libmpv.2.dylib        → _pl_log_create_349   (libplacebo API 349)
//   nm -gU libplacebo.338.dylib  → _pl_log_create_338
//
// on BOTH slices of the fat binaries, with only one libplacebo in the bundle
// and the reference not weak ("(undefined) external ... (from libplacebo.338)").
// dlopen() of that libmpv fails outright — verified with RTLD_NOW *and*
// RTLD_LAZY — and libmpv has 136 undefined _pl_* symbols riding on it. Whatever
// makes IINA itself work, that closure is not self-sufficient, and MpvLibrary
// loads with RTLD_NOW by design, so there is no flag to hide behind.
//
// Homebrew resolves mpv and libplacebo as one dependency graph, so its closure
// is self-consistent by construction. dylibbundler then copies that closure and
// rewrites every dependency to @loader_path/... in one pass. The dylibs must be
// re-signed ad-hoc afterwards: mutating a Mach-O invalidates its signature and
// macOS refuses to load an invalidly-signed dylib (hard failure on Apple
// Silicon). Cost of this route: the macOS slice needs a macOS runner, one per
// architecture — it cannot be produced from the Linux runner that builds
// every other slice.
//
// Conveyor stages one slice per machine — see the mpv-natives inputs in
// conveyor.conf.
// ===========================================================================
val mpvCacheDir = layout.buildDirectory.dir("mpv-cache")

// Pinned upstream artifacts. Bump deliberately: MpvLibrary.kt hand-maps the
// libmpv struct layouts by raw offset, so a client-API MAJOR bump means the
// structs must be re-verified before these pins move. mpv 0.41.x is client
// API 2.5; MpvLibrary.kt was written against 2.2 and only guards the major.
// mpv's own tagged release — the source of the macOS slices.
val mpvVersion = "0.41.0"
val mpvWinBuildTag = "20260610"
val mpvWinBuildSuffix = "20260610-git-304426c"
// Percent-encoded because the release tag embeds an '@'. Kept as a literal rather than
// URLEncoder.encode(): in a build script `java` resolves to the JavaPluginExtension
// accessor, so `java.net.URLEncoder` is unresolvable in expression position.
val mpvAppImageTagEncoded = "v0.41.0%402026-07-01_1782914175"
val mpvAppImageVersion = "v0.41.0"
// Reads the AppImage's DwarFS payload. 0.15.6 handles DwarFS v2.5, which is what this
// AppImage carries; an older dwarfs reports "unsupported major version".
val dwarfsVersion = "0.15.6"

// Every extractor below finds the directory that actually holds the libmpv
// artifact and copies its whole sibling set, rather than hard-coding upstream
// tree shapes. Those layouts drift between releases; "the folder libmpv lives
// in" does not.
fun findDirContaining(root: java.io.File, namePredicate: (String) -> Boolean): java.io.File? =
    root.walkTopDown()
        .firstOrNull { it.isFile && namePredicate(it.name) }
        ?.parentFile

/** True when [tool] can be executed at all (i.e. it exists on PATH). */
fun toolAvailable(tool: String): Boolean =
    try {
        ProcessBuilder(tool)
            .redirectOutput(ProcessBuilder.Redirect.DISCARD)
            .redirectError(ProcessBuilder.Redirect.DISCARD)
            .start()
            .waitFor()
        true
    } catch (e: java.io.IOException) {
        false
    }

fun runChecked(vararg command: String) {
    val exit = ProcessBuilder(*command).inheritIO().start().waitFor()
    check(exit == 0) { "Command failed (exit $exit): ${command.joinToString(" ")}" }
}

// ---------------------------------------------------------------------------
// macOS
// ---------------------------------------------------------------------------

/**
 * Rewrite every `@executable_path/lib/<name>` load-command entry to [prefix]`<name>`.
 *
 * Patches bytes instead of shelling out to `install_name_tool`, which is what keeps the macOS
 * slice buildable on ANY host — the whole point, since every other slice comes off the same
 * Linux runner.
 *
 * Safe because the replacement is always SHORTER than what it replaces (`@executable_path/lib/`
 * is 21 chars; `@loader_path/lib/` is 17 and `@loader_path/` is 13), so the tail is NUL-padded:
 * a Mach-O dylib path is a C string inside a load command of fixed `cmdsize`, and it ends at the
 * first NUL. Growing a path would need the command resized, which this deliberately never does.
 *
 * @return how many entries were rewritten.
 */
fun rewriteExecutablePathRefs(file: java.io.File, prefix: String): Int {
    val needle = "@executable_path/lib/".toByteArray(Charsets.US_ASCII)
    val prefixBytes = prefix.toByteArray(Charsets.US_ASCII)
    check(prefixBytes.size <= needle.size) {
        "prefix '$prefix' is longer than '@executable_path/lib/' — paths can only be shortened in place"
    }
    val data = file.readBytes()
    var rewritten = 0
    var i = 0
    outer@ while (i <= data.size - needle.size) {
        for (j in needle.indices) {
            if (data[i + j] != needle[j]) {
                i++
                continue@outer
            }
        }
        // The entry runs from the match to its terminating NUL.
        var end = i + needle.size
        while (end < data.size && data[end] != 0.toByte()) end++
        val name = data.copyOfRange(i + needle.size, end)
        val replacement = prefixBytes + name
        check(replacement.size <= end - i)
        replacement.copyInto(data, i)
        for (k in i + replacement.size until end) data[k] = 0
        rewritten++
        i = end
    }
    if (rewritten > 0) file.writeBytes(data)
    return rewritten
}

/**
 * Re-sign every Mach-O under [dir] ad-hoc, recursively.
 *
 * Rewriting load commands invalidates the signature mpv's CI applied, and macOS refuses to load
 * an invalidly-signed Mach-O (a hard failure on Apple Silicon). `codesign` only exists on macOS,
 * so on a Linux runner this warns instead: Conveyor signs the macOS bundle it produces, which is
 * what makes the staged slice loadable in the shipped app. A locally staged slice built on Linux
 * and run directly, without going through Conveyor, would not load.
 */
fun codesignAdhoc(dir: java.io.File) {
    val machO = dir.walkTopDown().filter { it.isFile && (it.name.endsWith(".dylib") || it.extension.isEmpty()) }.toList()
    check(machO.isNotEmpty()) { "Nothing to sign in ${dir.absolutePath}" }

    // Patching load commands invalidates mpv's own signature and macOS refuses to load an
    // invalidly-signed Mach-O, so re-signing is mandatory. `codesign` is macOS-only, which is
    // fine: these tasks build the published archives and are run on a Mac, not in CI.
    check(toolAvailable("codesign")) {
        "codesign is required to re-sign the patched macOS slice — run this task on a Mac. " +
            "CI does not: it downloads the prebuilt archives instead."
    }
    logger.lifecycle("[mpv-multi] Ad-hoc signing ${machO.size} Mach-O files in ${dir.name}")
    machO.forEach { runChecked("codesign", "--force", "--sign", "-", it.absolutePath) }
}

/**
 * Stage one macOS slice straight out of mpv's own release build.
 *
 * mpv's macOS artifacts are app bundles that link libmpv STATICALLY into
 * `mpv.app/Contents/MacOS/mpv`, so there is no libmpv.dylib to copy — but that binary is a PIE
 * Mach-O exporting the whole client API (54 `_mpv_*` symbols, checked with `nm -gU`), and macOS
 * `dlopen()` accepts a PIE executable. Renaming it to `libmpv.dylib` is what lets JNA find it:
 * `Native.load("mpv")` maps to exactly that filename on macOS (NativeLibrary.mapSharedLibraryName).
 *
 * Its dependency closure ships alongside in `Contents/MacOS/lib/` already relocatable via
 * `@executable_path/lib/...`; only the anchor has to change, because the loading process here is
 * the JVM rather than mpv itself. Two different prefixes are needed: libmpv sits one level above
 * `lib/`, while the closure sits inside it.
 *
 * Do NOT swap this for IINA's .dmg. IINA 1.4.4 ships a libmpv that needs libplacebo API 349 next
 * to a libplacebo.338 exporting only 338, on both slices and not weakly referenced, so its libmpv
 * fails dlopen under RTLD_NOW *and* RTLD_LAZY.
 */
fun extractMacMpvSlice(assetArch: String, outputDir: java.io.File) {
    val cache = mpvCacheDir.get().asFile
    val zip = cache.resolve("mpv-$mpvVersion-$assetArch.zip")
    downloadIfMissing(
        "https://github.com/mpv-player/mpv/releases/download/v$mpvVersion/mpv-v$mpvVersion-$assetArch.zip",
        zip,
        logPrefix = "mpv-multi",
    )

    // The published .zip wraps a .tar.gz, so this unpacks twice. Both steps are plain Gradle file
    // operations — no 7z, no hdiutil, nothing host-specific.
    val stage = cache.resolve("mac-$assetArch-extract")
    stage.deleteRecursively()
    stage.mkdirs()
    project.copy {
        from(zipTree(zip))
        into(stage)
    }
    val innerTar = stage.walkTopDown().firstOrNull { it.isFile && it.name.endsWith(".tar.gz") }
        ?: error("No inner tarball inside ${zip.name}")
    project.copy {
        from(tarTree(resources.gzip(innerTar)))
        into(stage)
    }
    val macOsDir = stage.walkTopDown().firstOrNull {
        it.isDirectory && it.name == "MacOS" && it.parentFile?.name == "Contents"
    } ?: error("mpv.app/Contents/MacOS not found inside ${zip.name}")
    val binary = macOsDir.resolve("mpv")
    check(binary.isFile) { "mpv binary missing from ${macOsDir.absolutePath}" }

    outputDir.deleteRecursively()
    outputDir.mkdirs()
    project.copy {
        from(macOsDir.resolve("lib"))
        into(outputDir.resolve("lib"))
    }
    val staged = outputDir.resolve("libmpv.dylib")
    binary.copyTo(staged, overwrite = true)
    staged.setWritable(true)

    var rewritten = rewriteExecutablePathRefs(staged, "@loader_path/lib/")
    outputDir.resolve("lib").listFiles()?.filter { it.isFile && it.name.endsWith(".dylib") }?.forEach { dylib ->
        dylib.setWritable(true)
        rewritten += rewriteExecutablePathRefs(dylib, "@loader_path/")
    }
    logger.lifecycle("[mpv-multi] $assetArch: rewrote $rewritten install-name entries")
    codesignAdhoc(outputDir)
}

val mpvSetupMacArmCi by tasks.registering {
    group = "mpv-multi"
    description = "Cross-OS: populate mpv-natives/macos-arm64/ with libmpv + its dylib closure."
    val outputDir = rootDir.resolve("mpv-natives/macos-arm64/")
    outputs.dir(outputDir)
    doLast { extractMacMpvSlice("macos-15-arm", outputDir) }
}

val mpvSetupMacX64Ci by tasks.registering {
    group = "mpv-multi"
    description = "Cross-OS: populate mpv-natives/macos-x64/ with Intel libmpv + its dylib closure."
    val outputDir = rootDir.resolve("mpv-natives/macos-x64/")
    outputs.dir(outputDir)
    doLast { extractMacMpvSlice("macos-15-intel", outputDir) }
}

// ---------------------------------------------------------------------------
// Windows
// ---------------------------------------------------------------------------

fun extractWindowsMpvSlice(arch: String, outputDir: java.io.File) {
    // `7zz` (the official 7-Zip binary) is preferred over `7z`, which on many machines is p7zip
    // — a fork last released in 2017. shinchiro's aarch64 archive uses the ARM64 BCJ filter that
    // 7-Zip only gained in 21.07, so p7zip fails it with "Unsupported Method : libmpv-2.dll"
    // while extracting the x86_64 one just fine. The CI image installs 7-Zip 26.x for the same
    // reason.
    val sevenZip = listOf("7zz", "7z").firstOrNull(::toolAvailable)
        ?: error(
            "7-Zip is required to unpack shinchiro's .7z builds and must be 21.07 or newer. " +
                "macOS: `brew install sevenzip` (provides 7zz). Ubuntu: install the official " +
                "7-Zip build — distro p7zip is too old for the ARM64 archive.",
        )
    val cache = mpvCacheDir.get().asFile
    val archive = cache.resolve("mpv-dev-$arch-$mpvWinBuildSuffix.7z")
    downloadIfMissing(
        "https://github.com/shinchiro/mpv-winbuild-cmake/releases/download/" +
            "$mpvWinBuildTag/mpv-dev-$arch-$mpvWinBuildSuffix.7z",
        archive,
        logPrefix = "mpv-multi",
    )
    val extractDir = cache.resolve("mpv-dev-$arch-extract")
    extractDir.deleteRecursively()
    extractDir.mkdirs()
    // Output is left visible: when the extractor is too old it fails with "Unsupported Method",
    // and silencing that turns a one-line diagnosis into a bare non-zero exit code.
    runChecked(sevenZip, "x", "-y", "-o${extractDir.absolutePath}", archive.absolutePath)

    // The dev package is headers + import lib + the runtime DLL. Only the DLL
    // is shipped; JNA resolves it by name ("libmpv-2" is in MpvLibrary's
    // CANDIDATE_NAMES), and ffmpeg is linked into it, so it stands alone.
    val dllDir = findDirContaining(extractDir) { it.startsWith("libmpv") && it.endsWith(".dll") }
        ?: error("No libmpv*.dll inside ${archive.name}")
    outputDir.deleteRecursively()
    outputDir.mkdirs()
    project.copy {
        from(dllDir)
        into(outputDir)
        include("*.dll")
        includeEmptyDirs = false
    }
}

val mpvSetupWindowsX64Ci by tasks.registering {
    group = "mpv-multi"
    description = "Cross-OS: populate mpv-natives/windows-x64/ with libmpv-2.dll."
    val outputDir = rootDir.resolve("mpv-natives/windows-x64/")
    outputs.dir(outputDir)
    doLast { extractWindowsMpvSlice("x86_64", outputDir) }
}

val mpvSetupWindowsArmCi by tasks.registering {
    group = "mpv-multi"
    description = "Cross-OS: populate mpv-natives/windows-arm64/ with ARM64 libmpv-2.dll."
    val outputDir = rootDir.resolve("mpv-natives/windows-arm64/")
    outputs.dir(outputDir)
    doLast { extractWindowsMpvSlice("aarch64", outputDir) }
}

// ---------------------------------------------------------------------------
// Linux
// ---------------------------------------------------------------------------

/**
 * Offset of the payload appended after an ELF file, i.e. the end of the ELF proper.
 *
 * AppImages are an ELF runtime with a filesystem image concatenated onto it, and the image starts
 * exactly where the section-header table ends: `e_shoff + e_shnum * e_shentsize`.
 *
 * Do NOT try to find the payload by scanning for its magic instead. This runtime embeds the
 * strings `DWARFS_BLOCK_SIZE`, `DWARFS_CACHE_SIZE` and friends as environment-variable names, so
 * the first `DWARFS` hit lands ~1.1 MB before the real image and every extractor then reports
 * "unsupported major version".
 */
fun elfPayloadOffset(file: java.io.File): Long {
    val header = ByteArray(64)
    file.inputStream().use { check(it.read(header) == 64) { "${file.name} is too small to be an ELF" } }
    check(header[0] == 0x7f.toByte() && header[1] == 'E'.code.toByte()) { "${file.name} is not an ELF file" }
    // Hand-rolled little-endian reads rather than java.nio.ByteBuffer: inside a build script
    // `java` resolves to the JavaPluginExtension accessor, so java.* only works in type position.
    fun le(offset: Int, size: Int): Long {
        var value = 0L
        for (i in size - 1 downTo 0) value = (value shl 8) or (header[offset + i].toLong() and 0xff)
        return value
    }
    val shoff = le(0x28, 8)
    val shentsize = le(0x3a, 2)
    val shnum = le(0x3c, 2)
    return shoff + shentsize * shnum
}

/**
 * `DT_NEEDED` entries of an ELF file, i.e. the shared objects it links against directly.
 *
 * Enough of a parser to walk a dependency closure: section headers → `.dynamic` → `.dynstr`.
 * Returns empty for anything that isn't an ELF with section headers.
 */
fun elfNeeded(file: java.io.File): List<String> {
    val d = file.readBytes()
    if (d.size < 64 || d[0] != 0x7f.toByte() || d[1] != 'E'.code.toByte()) return emptyList()
    fun le(off: Int, size: Int): Long {
        var v = 0L
        for (i in size - 1 downTo 0) v = (v shl 8) or (d[off + i].toLong() and 0xff)
        return v
    }
    val shoff = le(0x28, 8).toInt()
    val shentsize = le(0x3a, 2).toInt()
    val shnum = le(0x3c, 2).toInt()
    val shstrndx = le(0x3e, 2).toInt()
    if (shnum == 0 || shoff == 0) return emptyList()
    fun sectionName(i: Int) = le(shoff + i * shentsize, 4).toInt()
    fun sectionOff(i: Int) = le(shoff + i * shentsize + 0x18, 8).toInt()
    fun sectionSize(i: Int) = le(shoff + i * shentsize + 0x20, 8).toInt()
    fun cstr(base: Int, offset: Int): String {
        var e = base + offset
        while (e < d.size && d[e] != 0.toByte()) e++
        return String(d, base + offset, e - (base + offset), Charsets.US_ASCII)
    }
    val shstrBase = sectionOff(shstrndx)
    var dynamicIdx = -1
    var dynstrIdx = -1
    for (i in 0 until shnum) {
        when (cstr(shstrBase, sectionName(i))) {
            ".dynamic" -> dynamicIdx = i
            ".dynstr" -> dynstrIdx = i
        }
    }
    if (dynamicIdx < 0 || dynstrIdx < 0) return emptyList()
    val dynOff = sectionOff(dynamicIdx)
    val strBase = sectionOff(dynstrIdx)
    val result = mutableListOf<String>()
    for (i in 0 until sectionSize(dynamicIdx) / 16) {
        val tag = le(dynOff + i * 16, 8)
        val value = le(dynOff + i * 16 + 8, 8).toInt()
        if (tag == 0L) break
        if (tag == 1L) result += cstr(strBase, value) // DT_NEEDED
    }
    return result
}

/**
 * Delete everything in `lib/` that [root] does not actually reach.
 *
 * sharun bundles whatever the mpv *player* needs — X11, wayland, pulse, GTK and more — which is
 * 350 shared objects / 373 MB. libmpv's own closure is 91 of them / 54 MB, and the rest would be
 * dead weight in every Linux installer.
 */
fun pruneUnreachableSharedObjects(root: java.io.File, libDir: java.io.File) {
    val present = libDir.listFiles()?.filter { it.isFile }?.associateBy { it.name }.orEmpty()
    val reachable = mutableSetOf<String>()
    val queue = ArrayDeque<java.io.File>()
    queue += root
    while (queue.isNotEmpty()) {
        elfNeeded(queue.removeFirst()).forEach { name ->
            if (reachable.add(name)) present[name]?.let { queue += it }
        }
    }
    var freed = 0L
    present.forEach { (name, file) ->
        if (name !in reachable) {
            freed += file.length()
            file.delete()
        }
    }
    logger.lifecycle(
        "[mpv-multi] Pruned ${present.size - reachable.size} unreachable shared objects " +
            "(${freed / 1048576} MB); kept ${reachable.size}",
    )
}

val mpvSetupLinuxCi by tasks.registering {
    group = "mpv-multi"
    description = "Cross-OS: populate mpv-natives/linux-x64/ with libmpv + its .so closure."
    val outputDir = rootDir.resolve("mpv-natives/linux-x64/")
    outputs.dir(outputDir)
    doLast {
        // patchelf is the one genuinely host-specific step. The binary carries NO
        // DT_RPATH/DT_RUNPATH at all — inside the AppImage a sharun wrapper sets
        // LD_LIBRARY_PATH instead — so an rpath must be added before it can be dlopen()ed
        // straight out of the staged folder.
        //
        // Skip rather than fail when it is missing: `mpvSetupAll` is normally run on a dev's
        // own machine to get the app running, and a hard failure there would take the macOS
        // and Windows slices down with it for a slice that machine cannot use anyway. CI runs
        // on Linux, where patchelf is one apt package away.
        if (!toolAvailable("patchelf")) {
            logger.warn(
                "[mpv-multi] Skipping the Linux slice: patchelf is not on PATH " +
                    "(`sudo apt-get install -y patchelf`, or `brew install patchelf` locally). " +
                    "The other slices are unaffected.",
            )
            return@doLast
        }
        val cache = mpvCacheDir.get().asFile
        val appImage = cache.resolve("mpv-$mpvAppImageVersion-x86_64.AppImage")
        downloadIfMissing(
            "https://github.com/pkgforge-dev/mpv-AppImage/releases/download/" +
                "$mpvAppImageTagEncoded/mpv-$mpvAppImageVersion-anylinux-x86_64.AppImage",
            appImage,
            logPrefix = "mpv-multi",
        )

        // The payload is DwarFS, not SquashFS, and this runtime does not implement the classic
        // `--appimage-extract` flag (no `--appimage-*` string appears anywhere in the binary), so
        // neither unsquashfs nor self-extraction works. dwarfsextract reads it directly, and its
        // upstream Linux build is a self-contained tarball — no apt package needed.
        // On the Linux runner, fetch upstream's self-contained build so no distro package is
        // needed. Anywhere else that binary cannot execute, so fall back to a dwarfsextract
        // already on PATH (`brew install dwarfs`) and skip the slice if there is none.
        val isLinuxHost = System.getProperty("os.name").lowercase().contains("linux")
        val dwarfsExtract: String =
            if (isLinuxHost) {
                val dwarfsTar = cache.resolve("dwarfs-$dwarfsVersion-Linux-x86_64.tar.xz")
                downloadIfMissing(
                    "https://github.com/mhx/dwarfs/releases/download/v$dwarfsVersion/" +
                        "dwarfs-$dwarfsVersion-Linux-x86_64.tar.xz",
                    dwarfsTar,
                    logPrefix = "mpv-multi",
                )
                val toolsDir = cache.resolve("dwarfs-tools")
                val binary =
                    toolsDir.walkTopDown().firstOrNull { it.isFile && it.name == "dwarfsextract" } ?: run {
                        toolsDir.deleteRecursively()
                        toolsDir.mkdirs()
                        runChecked("tar", "-xf", dwarfsTar.absolutePath, "-C", toolsDir.absolutePath)
                        toolsDir.walkTopDown().firstOrNull { it.isFile && it.name == "dwarfsextract" }
                            ?: error("dwarfsextract not found inside ${dwarfsTar.name}")
                    }
                binary.setExecutable(true)
                binary.absolutePath
            } else {
                if (!toolAvailable("dwarfsextract")) {
                    logger.warn(
                        "[mpv-multi] Skipping the Linux slice: dwarfsextract is not on PATH " +
                            "(`brew install dwarfs`). The other slices are unaffected.",
                    )
                    return@doLast
                }
                "dwarfsextract"
            }

        val extractDir = cache.resolve("mpv-appimage-extract")
        extractDir.deleteRecursively()
        extractDir.mkdirs()
        val offset = elfPayloadOffset(appImage)
        logger.lifecycle("[mpv-multi] DwarFS payload starts at offset $offset")
        runChecked(
            dwarfsExtract,
            "-i", appImage.absolutePath,
            "-O", offset.toString(),
            "-o", extractDir.absolutePath,
        )

        // sharun keeps the real binary in shared/bin and the closure in shared/lib; shared/bin/mpv
        // is the 24 MB PIE that statically links libmpv and exports the full client API (54
        // `mpv_*` dynamic symbols), while bin/mpv is only the ~230 KB sharun launcher.
        val sharedDir = extractDir.walkTopDown().firstOrNull {
            it.isDirectory && it.name == "shared" && it.resolve("bin/mpv").isFile
        } ?: error("shared/bin/mpv not found inside the extracted AppImage")

        outputDir.deleteRecursively()
        outputDir.mkdirs()
        project.copy {
            from(sharedDir.resolve("lib"))
            into(outputDir.resolve("lib"))
        }
        // Named libmpv.so.2 because that is one of MpvLibrary's CANDIDATE_NAMES; JNA passes a
        // versioned .so name through unchanged on Linux.
        val staged = outputDir.resolve("libmpv.so.2")
        sharedDir.resolve("bin/mpv").copyTo(staged, overwrite = true)
        staged.setWritable(true)
        staged.setExecutable(true)
        runChecked("patchelf", "--set-rpath", "\$ORIGIN/lib", staged.absolutePath)
        pruneUnreachableSharedObjects(staged, outputDir.resolve("lib"))
        logger.lifecycle(
            "[mpv-multi] linux-x64: staged libmpv.so.2 + " +
                "${outputDir.resolve("lib").listFiles()?.size ?: 0} shared objects",
        )
    }
}

// ===========================================================================
// Two entry points, deliberately split.
//
// Everything above turns upstream mpv builds into loadable native slices, and it needs a Mac
// (codesign) plus 7-Zip 21.07+, patchelf and dwarfsextract. Running that in CI would drag all
// of it onto the Ubuntu runner for artifacts that never change between commits.
//
// So it runs ONCE per mpv bump, on a Mac, via `mpvBundleAll` — which also packs the result into
// per-slice tarballs that get attached to a GitHub release. CI then calls `mpvSetupAll`, which
// only downloads and unpacks them: no toolchain, no host requirements, same shape as the old
// vlcSetupAll.
// ===========================================================================
// Kept in a repo of its own rather than SimpMusic's own releases: these archives are ~196 MB per
// mpv bump and would otherwise sit in the release list users browse for the app itself.
val mpvNativesRepo = "maxrave-dev/simpmusic-files"
val mpvNativesTag = "abc"
val mpvSlices = listOf("linux-x64", "macos-arm64", "macos-x64", "windows-x64", "windows-arm64")

val mpvBundleAll by tasks.registering {
    group = "mpv-bundle"
    description = "Mac only: build every native slice and pack them into build/mpv-dist/ for a GitHub release."
    dependsOn(
        mpvSetupLinuxCi,
        mpvSetupMacArmCi,
        mpvSetupMacX64Ci,
        mpvSetupWindowsX64Ci,
        mpvSetupWindowsArmCi,
    )
    val distDir = layout.buildDirectory.dir("mpv-dist")
    outputs.dir(distDir)
    doLast {
        val dist = distDir.get().asFile
        dist.deleteRecursively()
        dist.mkdirs()
        mpvSlices.forEach { slice ->
            val sliceDir = rootDir.resolve("mpv-natives/$slice")
            check(sliceDir.isDirectory && sliceDir.listFiles()?.isNotEmpty() == true) {
                "mpv-natives/$slice is missing or empty — cannot pack an incomplete set"
            }
            runChecked(
                "tar", "-czf", dist.resolve("mpv-natives-$slice.tar.gz").absolutePath,
                "-C", rootDir.resolve("mpv-natives").absolutePath, slice,
            )
        }
        logger.lifecycle("[mpv-bundle] Packed ${mpvSlices.size} slices into ${dist.absolutePath}")
        logger.lifecycle("[mpv-bundle] Publish with:")
        logger.lifecycle(
            "  gh release create $mpvNativesTag ${dist.absolutePath}/*.tar.gz " +
                "--repo $mpvNativesRepo --title \"Desktop natives (mpv $mpvVersion)\" --notes \"...\"",
        )
    }
}

val mpvSetupAll by tasks.registering {
    group = "mpv-multi"
    description = "Populate mpv-natives/ from the prebuilt release tarballs. Runs anywhere; this is what CI uses."
    val outputRoot = rootDir.resolve("mpv-natives")
    outputs.dir(outputRoot)
    doLast {
        val cache = mpvCacheDir.get().asFile
        mpvSlices.forEach { slice ->
            val archive = cache.resolve("mpv-natives-$slice-$mpvVersion.tar.gz")
            downloadIfMissing(
                "https://github.com/$mpvNativesRepo/releases/download/$mpvNativesTag/mpv-natives-$slice.tar.gz",
                archive,
                logPrefix = "mpv-multi",
            )
            val target = outputRoot.resolve(slice)
            target.deleteRecursively()
            outputRoot.mkdirs()
            runChecked("tar", "-xzf", archive.absolutePath, "-C", outputRoot.absolutePath)
            check(target.isDirectory) { "$slice missing after unpacking ${archive.name}" }
        }
        logger.lifecycle("[mpv-multi] Unpacked ${mpvSlices.size} native slices into mpv-natives/")
    }
}

buildkonfig {
    packageName = "com.maxrave.simpmusic"
    exposeObjectWithName = "BuildKonfig"
    defaultConfigs {
        val versionName =
            libs.versions.version.name
                .get()
        val versionCode =
            libs.versions.version.code
                .get()
                .toInt()
        buildConfigField(STRING, "versionName", versionName)
        buildConfigField(INT, "versionCode", "$versionCode")

        if (isFullBuild) {
            try {
                println("Full build detected, enabling Sentry DSN")
                val properties = Properties()
                properties.load(rootProject.file("local.properties").inputStream())
                buildConfigField(
                    STRING,
                    "sentryDsn",
                    properties.getProperty("SENTRY_DSN") ?: "",
                )
            } catch (e: Exception) {
                println("Failed to load SENTRY_DSN from local.properties: ${e.message}")
                buildConfigField(STRING, "sentryDsn", "")
            }
        } else {
            buildConfigField(STRING, "sentryDsn", "")
        }
    }
}

aboutLibraries {
    collect.configPath = file("../config")
    export {
        outputFile = file("src/commonMain/composeResources/files/aboutlibraries.json")
        prettyPrint = true
        excludeFields = listOf("generated")
    }
    library {
        // Enable the duplication mode, allows to merge, or link dependencies which relate
        duplicationMode = com.mikepenz.aboutlibraries.plugin.DuplicateMode.MERGE
        // Configure the duplication rule, to match "duplicates" with
        duplicationRule = com.mikepenz.aboutlibraries.plugin.DuplicateRule.SIMPLE
    }
}

// Wire BuildKonfig output as input to AGP ArtProfile prepare tasks.
// Required by Gradle 9 strict task dependency validation. BuildKonfig 0.21.0
// migrated to AGP 9.2.1 + Gradle 9.4.1 but doesn't auto-wire
// generateBuildKonfig output to AGP's prepare*ArtProfile tasks.
// Refs: moko-resources#421, AboutLibraries#936.
afterEvaluate {
    tasks.matching { it.name.startsWith("prepare") && it.name.endsWith("ArtProfile") }
        .configureEach {
            dependsOn("generateBuildKonfig")
        }
}

