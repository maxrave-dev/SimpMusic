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
    alias(libs.plugins.vlc.setup)
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
            // Desktop app entry (main.kt), VLC setup, jpackage/Conveyor
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
// vlcSetup{} stays in :composeApp — moving it to :desktopApp fails
// because vlc.setup eagerly iterates tasks at apply time, which force-
// realizes Conveyor's lazily-registered writeConveyorConfig task before
// kotlin.multiplatform has created jvmJar → "Task with name 'jar' not
// found" (reproduced 2026-05-21 with vlc.setup placed last in the
// plugins block — plugin order does not help). composeApp has no
// Conveyor so the conflict cannot occur here.
//
// Run `./gradlew :composeApp:vlcSetup --no-configuration-cache` to
// populate vlc-natives/{linux-x64,macos-<hostArch>,windows-x64}/. Layout
// is per-arch so Conveyor can bundle the right native slice into each
// per-machine installer (universal Mac dylibs almost doubled artifact
// size pre-split — see commit message for context).
val hostMacArchDir = if (System.getProperty("os.arch").lowercase().contains("aarch64")) {
    "macos-arm64"
} else {
    "macos-x64"
}
val hostWinArchDir = if (System.getProperty("os.arch").lowercase().contains("aarch64")) {
    "windows-arm64"
} else {
    "windows-x64"
}
vlcSetup {
    vlcVersion = libs.versions.vlc.get()
    shouldCompressVlcFiles = false
    shouldIncludeAllVlcFiles = true
    pathToCopyVlcLinuxFilesTo   = rootDir.resolve("vlc-natives/linux-x64/")
    pathToCopyVlcMacosFilesTo   = rootDir.resolve("vlc-natives/$hostMacArchDir/")
    pathToCopyVlcWindowsFilesTo = rootDir.resolve("vlc-natives/$hostWinArchDir/")
}

// Flatten vlc-natives/<arch>/vlc/plugins → vlc-natives/<arch>/plugins after
// vlc-setup copies files. The plugin ships a nested vlc/ subdir for VLC's
// own path resolution, but Conveyor then copies both the nested tree AND
// extracts the .so files flat at the parent → 2× duplication (~348 MB
// extra) in the packaged AppImage. Flat layout keeps Conveyor lean while
// VLCJ still resolves libs via DefaultVlcDiscoverer.
tasks.named("vlcSetup").configure {
    doLast {
        listOf("linux-x64", hostMacArchDir, hostWinArchDir).forEach { archDir ->
            val root = rootDir.resolve("vlc-natives/$archDir")
            val nested = root.resolve("vlc")
            if (nested.isDirectory) {
                nested.listFiles()?.forEach { child ->
                    val target = root.resolve(child.name)
                    if (target.exists()) target.deleteRecursively()
                    child.renameTo(target)
                }
                nested.deleteRecursively()
            }
        }
    }
}

// ============================================================================
// Cross-OS VLC natives downloader (single-runner CI)
// ============================================================================
// The `vlc-setup` plugin above only registers `vlcSetup` for the HOST OS
// (LinuxTasksConfigure / MacTasksConfigure / WindowsTasksConfigure each
// gate on `getCurrentOs() == OS.X`). To package multi-OS artifacts from a
// single CI runner (Linux), we replicate the plugin's download + filter +
// copy logic for the OTHER two OSes here.
//
// Resulting layout matches the upstream plugin so VLCJ's
// DefaultVlcDiscoverer keeps working unchanged.
//
// Local dev: keep using `./gradlew :composeApp:vlcSetup` (host-OS only).
// Cross-OS CI: use `./gradlew :composeApp:vlcSetupAll`.
//
// Mac DMG extraction needs the 7z tool:
//   Ubuntu CI:   sudo apt-get install -y p7zip-full
//   macOS local: brew install p7zip
val vlcCacheDir = layout.buildDirectory.dir("vlc-cache")

fun downloadIfMissing(url: String, target: java.io.File) {
    if (target.exists() && target.length() > 0) {
        logger.lifecycle("[vlc-multi] Cached: ${target.name}")
        return
    }
    logger.lifecycle("[vlc-multi] Downloading $url")
    target.parentFile.mkdirs()
    // Use curl instead of Java's URL.openStream() because get.videolan.org
    // returns a 302 redirect to a random mirror per request, and Java's
    // default HttpURLConnection redirect handling is fragile — if the
    // mirror lands on a cross-protocol redirect or returns an HTML error
    // page, openStream() silently saves the HTML response as the target
    // file, producing a "Cannot expand ZIP" downstream. curl's `-L`
    // follows redirects robustly across protocols + mirrors, `--fail`
    // exits non-zero on HTTP errors instead of saving error bodies, and
    // `-o` writes atomically via tmp file. The downloaded artifact is
    // also size-verified (must match Content-Length).
    val curlExit = ProcessBuilder(
        "curl",
        "-fsSL",
        "--retry", "3",
        "--retry-delay", "2",
        "-o", target.absolutePath,
        url,
    ).inheritIO().start().waitFor()
    check(curlExit == 0 && target.exists() && target.length() > 0) {
        // Delete partial/empty file so the next run can retry cleanly.
        if (target.exists()) target.delete()
        "curl failed (exit $curlExit) downloading $url to $target"
    }
}

val vlcSetupLinuxCi by tasks.registering {
    group = "vlc-multi"
    description = "Cross-OS: populate vlc-natives/linux-x64/ with .so files."
    val outputDir = rootDir.resolve("vlc-natives/linux-x64/")
    outputs.dir(outputDir)
    doLast {
        // Pinned upstream — Linux artifact is a custom Maven package whose
        // version is independent of the desktop VLC release.
        val linuxVersion = "3.0.20-2"
        val cache = vlcCacheDir.get().asFile
        val jar = cache.resolve("vlc-plugins-linux-$linuxVersion.jar")
        downloadIfMissing(
            "https://repo1.maven.org/maven2/ir/mahozad/vlc-plugins-linux/$linuxVersion/vlc-plugins-linux-$linuxVersion.jar",
            jar,
        )
        outputDir.walk().filter { it.extension == "so" }.forEach { it.delete() }
        project.copy {
            from(zipTree(jar))
            into(outputDir)
            // Ship the full VLC plugin set (matches the v1.2.1 release).
            // A curated subset based on upstream vlc-setup defaults turned
            // out to be insufficient for SimpMusic — YT Music streaming
            // depends on HTTP/HTTPS access + MP4/WebM demuxers that the
            // upstream music-app preset doesn't cover. `**/` is needed
            // because include() evaluates against the original jar paths
            // (which include the `vlc-plugins-linux-<ver>/` top-level dir)
            // before the eachFile drop(1) transformation kicks in.
            include("**/*.so", "**/*.so.*")
            // Strip the top-level dir inside the jar (matches upstream plugin).
            eachFile {
                if (relativePath.segments.size > 1) {
                    relativePath = RelativePath(true, *relativePath.segments.drop(1).toTypedArray())
                }
            }
            includeEmptyDirs = false
        }
        // Flatten vlc-natives/linux-x64/vlc/* → vlc-natives/linux-x64/* (same
        // as the host-OS flatten task above) so Conveyor doesn't duplicate
        // plugins.
        val nested = outputDir.resolve("vlc")
        if (nested.isDirectory) {
            nested.listFiles()?.forEach { child ->
                val target = outputDir.resolve(child.name)
                if (target.exists()) target.deleteRecursively()
                child.renameTo(target)
            }
            nested.deleteRecursively()
        }
    }
}

// Shared Mac DMG download + extraction logic. Each per-arch task calls
// this with its slice's DMG suffix ("arm64" or "intel64") and output
// folder. We pull per-arch DMGs (48-55 MB each) instead of the universal
// DMG (84.9 MB = arm64 + intel64 fat binary), so each per-machine zip
// only ships its own slice — saves ~25-40 MB per user download.
fun extractMacVlcSlice(
    archSuffix: String,
    outputDir: java.io.File,
) {
    val macVersion = libs.versions.vlc.get()
    val cache = vlcCacheDir.get().asFile
    val dmg = cache.resolve("vlc-$macVersion-$archSuffix.dmg")
    downloadIfMissing(
        "https://get.videolan.org/vlc/$macVersion/macosx/vlc-$macVersion-$archSuffix.dmg",
        dmg,
    )
    outputDir.walk().filter { it.extension == "dylib" }.forEach { it.delete() }
    outputDir.mkdirs()
    // Pick the extractor that's native to the host:
    //   • macOS  → hdiutil (built-in, no install required for local dev)
    //   • Linux/Windows CI → 7z (cross-platform HFS+ support, needs
    //     p7zip-full / official 7-Zip 23+ installed on the runner)
    // Both paths drop a directory containing the VLC.app payload at
    // `macOsDir`, ready for the curated copy step below.
    val isMacHost = System.getProperty("os.name").lowercase().contains("mac")
    val macOsDir: java.io.File
    val cleanupMount: (() -> Unit)?
    if (isMacHost) {
        val mountPoint = cache.resolve("vlc-mount-$macVersion-$archSuffix")
        mountPoint.deleteRecursively()
        mountPoint.mkdirs()
        val attachExit = ProcessBuilder(
            "hdiutil", "attach",
            "-mountpoint", mountPoint.absolutePath,
            "-nobrowse", "-quiet",
            dmg.absolutePath,
        ).inheritIO().start().waitFor()
        check(attachExit == 0) {
            "hdiutil attach failed with exit code $attachExit for $dmg"
        }
        macOsDir = mountPoint.resolve("VLC.app/Contents/MacOS")
        cleanupMount = {
            ProcessBuilder("hdiutil", "detach", "-quiet", mountPoint.absolutePath)
                .inheritIO().start().waitFor()
        }
    } else {
        val extractDir = cache.resolve("vlc-mac-$macVersion-$archSuffix-extract")
        extractDir.deleteRecursively()
        extractDir.mkdirs()
        // 7z returns exit code 2 because the DMG contains a "VLC media
        // player/Applications → /Applications" drag-to-install symlink
        // that 7z refuses to extract (dangerous absolute link). The
        // VLC.app payload extracts fine, so we verify by directory
        // presence below rather than trusting the exit code.
        val sevenZipExit = ProcessBuilder(
            "7z", "x", "-y", "-bso0", "-bsp0",
            "-o${extractDir.absolutePath}",
            dmg.absolutePath,
        ).inheritIO().start().waitFor()
        macOsDir = extractDir.walkTopDown()
            .firstOrNull {
                it.isDirectory &&
                    it.name == "MacOS" &&
                    it.parentFile?.name == "Contents" &&
                    it.parentFile?.parentFile?.name == "VLC.app"
            }
            ?: error(
                "VLC.app/Contents/MacOS/ not found inside extracted DMG at $extractDir " +
                        "(7z exit code $sevenZipExit)",
            )
        cleanupMount = null
    }
    check(macOsDir.isDirectory) {
        "VLC.app/Contents/MacOS not found at ${macOsDir.absolutePath}"
    }
    try {
        project.copy {
            from(macOsDir)
            into(outputDir)
            // Ship the full VLC plugin set (matches v1.2.1 release).
            // Curated music-app preset from upstream vlc-setup didn't
            // include HTTP/HTTPS access + MP4/WebM demuxers needed for
            // YT Music streaming.
            include("lib/libvlc.dylib", "lib/libvlccore.dylib", "plugins/**")
            // Flatten lib/ → root (matches upstream Mac VlcSetupTask).
            eachFile {
                if (relativePath.segments.firstOrNull() == "lib") {
                    relativePath = RelativePath(true, *relativePath.segments.drop(1).toTypedArray())
                }
            }
            includeEmptyDirs = false
        }
    } finally {
        cleanupMount?.invoke()
    }
}

val vlcSetupMacArmCi by tasks.registering {
    group = "vlc-multi"
    description = "Cross-OS: populate vlc-natives/macos-arm64/ with Apple Silicon .dylib files."
    val outputDir = rootDir.resolve("vlc-natives/macos-arm64/")
    outputs.dir(outputDir)
    doLast { extractMacVlcSlice("arm64", outputDir) }
}

val vlcSetupMacX64Ci by tasks.registering {
    group = "vlc-multi"
    description = "Cross-OS: populate vlc-natives/macos-x64/ with Intel .dylib files."
    val outputDir = rootDir.resolve("vlc-natives/macos-x64/")
    outputs.dir(outputDir)
    doLast { extractMacVlcSlice("intel64", outputDir) }
}

// Shared Windows VLC zip extraction. VideoLAN ships separate per-arch
// zips (win64/ for x64, winarm64/ for ARM64) — we mirror that layout
// in vlc-natives/ so Conveyor bundles the right slice per msix.
fun extractWindowsVlcSlice(
    archSuffix: String,
    outputDir: java.io.File,
) {
    val winVersion = libs.versions.vlc.get()
    val cache = vlcCacheDir.get().asFile
    // VideoLAN URL layout for Windows:
    //   x64: /vlc/<ver>/win64/vlc-<ver>-win64.zip
    //   arm: /vlc/<ver>/winarm64/vlc-<ver>-winarm64.zip
    // Both zips share the same internal tree shape, so once downloaded
    // the rest of the pipeline is identical.
    val subDir = if (archSuffix == "winarm64") "winarm64" else "win64"
    val zip = cache.resolve("vlc-$winVersion-$archSuffix.zip")
    downloadIfMissing(
        "https://get.videolan.org/vlc/$winVersion/$subDir/vlc-$winVersion-$archSuffix.zip",
        zip,
    )
    outputDir.walk().filter { it.extension == "dll" }.forEach { it.delete() }
    project.copy {
        from(zipTree(zip))
        into(outputDir)
        // Ship the full VLC plugin set (matches v1.2.1 release). The
        // music-app preset from upstream vlc-setup turned out to be
        // missing HTTP/HTTPS access + MP4/WebM demuxers needed for YT
        // Music streaming. `**/` is required because include() runs
        // against the original `vlc-<ver>/...` paths inside the zip
        // before the eachFile drop(1) transformation.
        include("**/*.dll")
        // Strip top-level `vlc-<ver>/` prefix dir.
        eachFile {
            if (relativePath.segments.size > 1) {
                relativePath = RelativePath(true, *relativePath.segments.drop(1).toTypedArray())
            }
        }
        includeEmptyDirs = false
    }
}

val vlcSetupWindowsX64Ci by tasks.registering {
    group = "vlc-multi"
    description = "Cross-OS: populate vlc-natives/windows-x64/ with .dll files."
    val outputDir = rootDir.resolve("vlc-natives/windows-x64/")
    outputs.dir(outputDir)
    doLast { extractWindowsVlcSlice("win64", outputDir) }
}

val vlcSetupWindowsArmCi by tasks.registering {
    group = "vlc-multi"
    description = "Cross-OS: populate vlc-natives/windows-arm64/ with ARM64 .dll files."
    val outputDir = rootDir.resolve("vlc-natives/windows-arm64/")
    outputs.dir(outputDir)
    doLast { extractWindowsVlcSlice("winarm64", outputDir) }
}

val vlcSetupAll by tasks.registering {
    group = "vlc-multi"
    description = "Cross-OS: populate vlc-natives/{linux-x64,macos-arm64,macos-x64,windows-x64,windows-arm64}/ from any host. Use in CI."
    dependsOn(
        vlcSetupLinuxCi,
        vlcSetupMacArmCi,
        vlcSetupMacX64Ci,
        vlcSetupWindowsX64Ci,
        vlcSetupWindowsArmCi,
    )
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

