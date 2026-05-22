@file:Suppress("UnstableApiUsage")

import org.apache.commons.io.FileUtils
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.compose.desktop.application.tasks.AbstractJPackageTask
import java.net.URI
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// desktopApp — JVM application module for SimpMusic Desktop.
//
// Per JetBrains 2026 KMP guidance (AGP 9 + new default structure), the
// platform-app entry points live in dedicated modules separate from the
// shared KMP library. This module owns:
//
//   * the JVM main() entry
//   * compose.desktop.application packaging (jpackage path, still used
//     until Conveyor cutover lands in Task 14)
//   * VLC native bundling (vlc-setup)
//   * desktop-only UI (CustomTitleBar, MiniPlayerWindow, CrashDialog, etc.)
//
// composeApp remains a pure KMP library — its src/jvmMain only carries the
// expect/actual implementations the shared code needs on the JVM.
//
// Refs:
//   https://blog.jetbrains.com/kotlin/2026/05/new-kmp-default-structure/
//   https://github.com/HaroonBsf/kmp-conveyor-template
plugins {
    // Order matters: per the Bifrost reference (a known-good Conveyor 2.0
    // + KMP setup), `conveyor` must apply BEFORE `kotlin.multiplatform`.
    // Conveyor's task creation runs at apply time and only succeeds when
    // a `jar` task already exists — the compose plugin (applied first)
    // provides that, while kotlin.multiplatform would override it later
    // with `jvmJar` if applied before conveyor.
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.conveyor)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.multiplatform)
    // NOTE: `vlc.setup` lives in :composeApp (not here) because its eager
    // task iteration at apply time triggers Conveyor's writeConveyorConfig
    // creation, which then fails with "Task with name 'jar' not found" —
    // jvmJar isn't created until after the script body's `kotlin {}` block
    // runs. Plugin order tricks (vlc.setup last, Bifrost ordering) don't
    // help because vlc.setup's iteration force-realizes EVERY existing
    // task, including the lazily-registered Conveyor ones. Confirmed by
    // retry on 2026-05-21: same error reproduced. Run vlcSetup via
    // `./gradlew :composeApp:vlcSetup --no-configuration-cache`.
}

version = libs.versions.version.name.get().removeSuffix("-hf")

kotlin {
    // 21 matches :media-jvm-ui (requires 21+).
    jvmToolchain(21)

    // KMP jvm() target — Conveyor 2.0's writeConveyorConfig task looks up
    // `jvmRuntimeClasspath` (KMP convention), so we use the KMP plugin here
    // even though desktopApp only has a single JVM target. Pattern adapted
    // from https://github.com/zacharee/Bifrost desktop module.
    jvm {
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions {
                    jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
                }
            }
        }
    }

    sourceSets {
        val jvmMain by getting {
            dependencies {
                // Shared KMP library — pulls App.kt, expect/actual impls,
                // view-models, MiniPlayer state object, etc.
                implementation(project(":composeApp"))

                // Compose Desktop runtime for the current OS.
                implementation(compose.desktop.currentOs)

                // Swing dispatcher for kotlinx.coroutines on the JVM.
                implementation(libs.kotlinx.coroutinesSwing)

                // Sentry crash reporting (full builds only via BuildKonfig).
                implementation(libs.sentry.jvm)

                // System tray icon for desktop builds.
                implementation(libs.native.tray)

                // Media player JVM UI primitives consumed by jvmMain expect/actuals.
                implementation(projects.mediaJvmUi)

                // Commons-IO drives the custom AppImage packaging task below.
                implementation(libs.commons.io)
            }
        }
    }
}

// Workaround the Gradle "Cannot mutate configuration after observation" error
// hit when Conveyor 2.0's per-arch deps mix with VLC-setup / compose plugins
// that resolve runtimeClasspath at configuration time. Creating a sibling
// `desktopRuntimeClasspath` configuration shifts Conveyor's resolution off
// the primary jvmRuntimeClasspath, breaking the lock chain.
// Source: https://github.com/zacharee/Bifrost/blob/main/desktop/build.gradle.kts
project.configurations.create("desktopRuntimeClasspath") {
    extendsFrom(project.configurations.getByName("jvmRuntimeClasspath"))
}

// Conveyor per-arch artifacts. These configurations are created by the
// Conveyor plugin's `apply()`; we just feed them the right native compose
// binaries for cross-build from any host OS.
dependencies {
    linuxAarch64(libs.compose.linux.arm64)
    linuxAmd64(libs.compose.linux.x64)
    macAarch64(libs.compose.macos.arm64)
    macAmd64(libs.compose.macos.x64)
    windowsAarch64(libs.compose.windows.arm64)
    windowsAmd64(libs.compose.windows.x64)
}

// Append SimpMusic-specific keys to Conveyor's generated config file and
// — crucially — replace the auto-detected `app.inputs` classpath with
// ProGuard's shrunk jar directory so the packaged AppImage carries
// obfuscated + size-reduced bytecode instead of raw Gradle output.
// `dependsOn(proguardReleaseJars)` builds the shrunk jars first.
tasks.named<hydraulic.conveyor.gradle.WriteConveyorConfigTask>("writeConveyorConfig") {
    dependsOn(tasks.named("proguardReleaseJars"))
    val proguardJarsDir = layout.buildDirectory.dir("compose/tmp/main-release/proguard")
    doLast {
        destination.get().asFile.appendText(
            """
            |app.fsname = simpmusic
            |app.display-name = SimpMusic
            |app.rdns-name = com.maxrave.simpmusic
            |
            |// Override the Gradle-detected classpath with the ProGuard'd
            |// jar directory. Conveyor expands a directory entry to every
            |// file inside it — saves ~750 MB raw / ~100 MB compressed in
            |// the resulting AppImage by replacing 221 raw jars with the
            |// shrunk equivalents from compose.desktop's proguard task.
            |app.inputs = [
            |    "${proguardJarsDir.get().asFile.absolutePath}"
            |]
            """.trimMargin() + "\n",
        )
    }
}

// vlcSetup block disabled with the plugin above. VLC natives in
// vlc-natives/{linux,macos,windows}/ are already on disk from prior runs.
// TODO: replace with a simple Gradle download task that doesn't iterate
// tasks at apply time, so Conveyor + vlc-setup can coexist.

compose.desktop {
    application {
        mainClass = "com.maxrave.simpmusic.MainKt"
        jvmArgs += "--add-opens=java.base/java.nio=ALL-UNNAMED"

        nativeDistributions {
            appResourcesRootDir = rootDir.resolve("vlc-natives/")
            val listTarget = mutableListOf<TargetFormat>()
            if (org.gradle.internal.os.OperatingSystem
                    .current()
                    .isMacOsX
            ) {
                listTarget.addAll(
                    listOf(TargetFormat.Dmg, TargetFormat.Msi),
                )
            } else {
                listTarget.addAll(
                    listOf(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.AppImage),
                )
            }
            targetFormats(*listTarget.toTypedArray())
            modules("jdk.unsupported")
            packageName = "SimpMusic"
            macOS {
                val formatedDate =
                    Instant.now().let {
                        DateTimeFormatter
                            .ofPattern("yyyy.MM.dd")
                            .withZone(ZoneId.of("UTC"))
                            .format(it)
                    }
                includeAllModules = true
                packageVersion = formatedDate
                iconFile.set(rootDir.resolve("composeApp/icon/circle_app_icon.icns"))
                val macExtraPlistKeys =
                    """
                    <key>LSApplicationCategoryType</key>
                    <string>public.app-category.music</string>
                    <key>UIBackgroundModes</key>
                    <array>
                        <string>audio</string>
                        <string>fetch</string>
                        <string>processing</string>
                    </array>
                    <key>CFBundleURLTypes</key>
                    <array>
                        <dict>
                            <key>CFBundleTypeRole</key>
                            <string>Viewer</string>
                            <key>CFBundleURLName</key>
                            <string>com.maxrave.simpmusic.deeplink</string>
                            <key>CFBundleURLSchemes</key>
                            <array>
                                <string>simpmusic</string>
                            </array>
                        </dict>
                    </array>
                    """.trimIndent()
                infoPlist {
                    extraKeysRawXml = macExtraPlistKeys
                }
            }
            windows {
                includeAllModules = true
                packageVersion =
                    libs.versions.version.name
                        .get()
                        .removeSuffix("-hf")
                iconFile.set(rootDir.resolve("composeApp/icon/circle_app_icon.ico"))
            }
            linux {
                includeAllModules = true
                packageVersion =
                    libs.versions.version.name
                        .get()
                        .removeSuffix("-hf")
                iconFile.set(rootDir.resolve("composeApp/icon/circle_app_icon.png"))
            }
        }

        buildTypes.release.proguard {
            optimize.set(true)
            obfuscate.set(true)
            configurationFiles.from(rootDir.resolve("composeApp/proguard-desktop-rules.pro"))
        }
    }
}

afterEvaluate {
    tasks.withType<JavaExec> {
        jvmArgs("--add-opens", "java.desktop/sun.awt=ALL-UNNAMED")
        jvmArgs("--add-opens", "java.desktop/java.awt.peer=ALL-UNNAMED")
        jvmArgs("--add-opens", "java.base/java.nio=ALL-UNNAMED")

        // Pass bundled VLC natives path to the runtime for `./gradlew desktopApp:run`.
        val osSubDir =
            when {
                System.getProperty("os.name").contains("Mac") -> "macos"
                System.getProperty("os.name").contains("Win") -> "windows"
                else -> "linux"
            }
        val vlcNativesPath = rootDir.resolve("vlc-natives/$osSubDir").absolutePath
        systemProperty("vlc.bundled.path", vlcNativesPath)

        if (System.getProperty("os.name").contains("Mac")) {
            jvmArgs("--add-opens", "java.desktop/sun.awt=ALL-UNNAMED")
            jvmArgs("--add-opens", "java.desktop/sun.lwawt=ALL-UNNAMED")
            jvmArgs("--add-opens", "java.desktop/sun.lwawt.macosx=ALL-UNNAMED")
        }
    }

}

// ---------------------------------------------------------------------------
// packageConveyorAppImage — wraps Conveyor's `linux-app` directory tree
// (project root /output) into a single-file .AppImage. Conveyor 2.0 dropped
// native AppImage support (only .deb + .tar.gz remain), so we keep the
// "one-for-all-distros" .AppImage by piping its output through appimagetool
// — identical to the previous jpackage-based pipeline, just sourced from
// Conveyor instead.
//
// Usage: run AFTER `conveyor -Kapp.machines=linux.amd64.glibc make linux-app`
//        then `./gradlew :desktopApp:packageConveyorAppImage`
// ---------------------------------------------------------------------------
// Step 1 of the AppImage chain — invoke the external `conveyor` CLI to
// produce ./output (the relocatable linux-app directory tree).
// Conveyor 2.0 prompts once for a root-key passphrase on first run; press
// Enter to use no passphrase. Subsequent runs are fully non-interactive.
val conveyorMakeLinuxApp = tasks.register<Exec>("conveyorMakeLinuxApp") {
    group = "distribution"
    description = "Run `conveyor make linux-app` for Linux x86_64 (glibc)."
    dependsOn(":composeApp:vlcSetup")
    workingDir = rootDir
    commandLine(
        "conveyor",
        "--agree-to-license=1",
        "-Kapp.machines=linux.amd64.glibc",
        "make", "linux-app",
    )
    standardInput = System.`in`
}

tasks.register("packageConveyorAppImage") {
    group = "distribution"
    description = "Wrap Conveyor's linux-app output (./output) into a portable .AppImage."
    // Captures project references (rootDir, layout, libs catalog) inside
    // doLast — necessary for the per-build paths, incompatible with the
    // Gradle 9 configuration cache. This is a manual one-shot packaging
    // task invoked after `conveyor make linux-app`, not part of the
    // critical CI path, so opting out is acceptable.
    notCompatibleWithConfigurationCache(
        "Reads project/layout/libs from within doLast to compute appimage paths."
    )

    doLast {
        val appName = "SimpMusic"
        val conveyorOutput = rootDir.resolve("output")
        if (!conveyorOutput.exists()) {
            throw GradleException(
                "Conveyor output (./output) not found. Run `conveyor " +
                    "-Kapp.machines=linux.amd64.glibc make linux-app` first.",
            )
        }

        val appimagetool =
            layout.buildDirectory
                .dir("tmp")
                .get()
                .asFile
                .resolve("appimagetool-x86_64.AppImage")
        if (!appimagetool.exists()) {
            downloadFile(
                "https://github.com/AppImage/AppImageKit/releases/download/continuous/appimagetool-x86_64.AppImage",
                appimagetool,
            )
        }
        if (!appimagetool.canExecute()) {
            appimagetool.setExecutable(true)
        }

        val appDir =
            layout.buildDirectory
                .dir("appimage/conveyor/$appName.AppDir")
                .get()
                .asFile
        if (appDir.exists()) {
            appDir.deleteRecursively()
        }

        // Stage scaffold (icon source) + Conveyor binary tree.
        val appDirSrc = rootDir.resolve("composeApp/appimage")
        if (appDirSrc.exists()) {
            FileUtils.copyDirectory(appDirSrc, appDir)
        } else {
            appDir.mkdirs()
        }
        FileUtils.copyDirectory(conveyorOutput, appDir)

        // Ensure top-level PNG icon expected by appimagetool exists.
        val iconSrc = rootDir.resolve("composeApp/icon/circle_app_icon.png")
        val iconDst = appDir.resolve("simpmusic.png")
        if (!iconDst.exists() && iconSrc.exists()) {
            FileUtils.copyFile(iconSrc, iconDst)
        }

        val versionName = libs.versions.version.name.get()
        val desktopFile = appDir.resolve("simpmusic.desktop")
        desktopFile.writeText(
            """[Desktop Entry]
            |Type=Application
            |Version=1.0
            |Name=SimpMusic
            |Comment=SimpMusic v$versionName - FOSS YouTube Music Client
            |Exec=bin/simpmusic %u
            |Icon=simpmusic
            |Terminal=false
            |Categories=Audio;AudioVideo;
            |StartupWMClass=com-maxrave-simpmusic-MainKt
            |MimeType=x-scheme-handler/simpmusic;
            |
            """.trimMargin(),
        )

        val appRun = appDir.resolve("AppRun")
        appRun.writeText(
            """#!/bin/sh
            |
            |SELF=${'$'}(readlink -f "${'$'}0")
            |HERE=${'$'}{SELF%/*}
            |
            |# Install icon into XDG dirs so GNOME/KDE pick it up the first time.
            |ICON_DIR="${'$'}HOME/.local/share/icons/hicolor/256x256/apps"
            |if [ ! -f "${'$'}ICON_DIR/simpmusic.png" ] || [ "${'$'}HERE/simpmusic.png" -nt "${'$'}ICON_DIR/simpmusic.png" ]; then
            |    mkdir -p "${'$'}ICON_DIR"
            |    cp "${'$'}HERE/simpmusic.png" "${'$'}ICON_DIR/simpmusic.png"
            |    gtk-update-icon-cache -f -t "${'$'}HOME/.local/share/icons/hicolor" 2>/dev/null || true
            |fi
            |
            |# Install .desktop file with absolute Exec path to the AppImage.
            |DESKTOP_DIR="${'$'}HOME/.local/share/applications"
            |mkdir -p "${'$'}DESKTOP_DIR"
            |APPIMAGE_PATH="${'$'}{APPIMAGE:-${'$'}SELF}"
            |sed "s|Exec=bin/simpmusic|Exec=${'$'}APPIMAGE_PATH|" "${'$'}HERE/simpmusic.desktop" > "${'$'}DESKTOP_DIR/com-maxrave-simpmusic-MainKt.desktop"
            |update-desktop-database "${'$'}DESKTOP_DIR" 2>/dev/null || true
            |
            |cd "${'$'}HERE"
            |exec bin/simpmusic "${'$'}@"
            |
            """.trimMargin(),
        )
        appRun.setExecutable(true, false)

        // Conveyor's launcher lives at output/bin/simpmusic (lowercase).
        val appExecutable = appDir.resolve("bin/simpmusic")
        if (appExecutable.exists() && !appExecutable.canExecute()) {
            appExecutable.setExecutable(true)
        }

        val outputAppImage = appDir.parentFile.resolve("$appName-x86_64.AppImage")
        val process =
            ProcessBuilder(
                appimagetool.canonicalPath,
                "$appName.AppDir",
                outputAppImage.name,
            ).directory(appDir.parentFile)
                .apply { environment()["ARCH"] = "x86_64" }
                .inheritIO()
                .start()

        val exitCode = process.waitFor()
        if (exitCode != 0) {
            throw GradleException("appimagetool failed with exit code $exitCode")
        }
        println("[AppImage] Built: ${outputAppImage.absolutePath}")
    }
}

// End-to-end: vlcSetup → conveyor make linux-app → wrap as .AppImage.
// Single command for users: `./gradlew :desktopApp:buildLinuxAppImage --no-configuration-cache`
tasks.register("buildLinuxAppImage") {
    group = "distribution"
    description = "Full SimpMusic Desktop Linux AppImage build pipeline (vlcSetup → conveyor → AppImage)."
    dependsOn(conveyorMakeLinuxApp)
    finalizedBy("packageConveyorAppImage")
}

// macOS — Conveyor 2.0 ships .zip wrapping the .app bundle (no native
// .dmg target). Run on a macOS host for proper code signing; cross-build
// from Linux works but the app won't be signed.
//
// Run via: `./gradlew :desktopApp:buildMacZipAmd64 --no-configuration-cache`
val conveyorMakeMacZipAmd64 = tasks.register<Exec>("conveyorMakeMacZipAmd64") {
    group = "distribution"
    description = "Run `conveyor make unnotarized-mac-zip` for macOS Intel."
    dependsOn(":composeApp:vlcSetup")
    workingDir = rootDir
    commandLine(
        "conveyor",
        "--agree-to-license=1",
        "-Kapp.machines=mac.amd64",
        "make", "unnotarized-mac-zip",
    )
    standardInput = System.`in`
}

val conveyorMakeMacZipAarch64 = tasks.register<Exec>("conveyorMakeMacZipAarch64") {
    group = "distribution"
    description = "Run `conveyor make unnotarized-mac-zip` for macOS Apple Silicon."
    dependsOn(":composeApp:vlcSetup")
    workingDir = rootDir
    commandLine(
        "conveyor",
        "--agree-to-license=1",
        "-Kapp.machines=mac.aarch64",
        "make", "unnotarized-mac-zip",
    )
    standardInput = System.`in`
}

tasks.register("buildMacZipAmd64") {
    group = "distribution"
    description = "Full SimpMusic Desktop macOS Intel .zip pipeline (vlcSetup → conveyor)."
    dependsOn(conveyorMakeMacZipAmd64)
}

tasks.register("buildMacZipAarch64") {
    group = "distribution"
    description = "Full SimpMusic Desktop macOS Apple Silicon .zip pipeline (vlcSetup → conveyor)."
    dependsOn(conveyorMakeMacZipAarch64)
}

// Windows — Conveyor 2.0 ships .msix (modern Windows 10+ app package).
// NOTE: Unsigned .msix has rough UX (users must enable sideloading +
// install certificate). Recommended path long-term: code-sign with an
// EV cert OR switch to Inno Setup `.exe` wrap if signing budget unavailable.
val conveyorMakeWindowsMsix = tasks.register<Exec>("conveyorMakeWindowsMsix") {
    group = "distribution"
    description = "Run `conveyor make windows-msix` for Windows x86_64."
    dependsOn(":composeApp:vlcSetup")
    workingDir = rootDir
    commandLine(
        "conveyor",
        "--agree-to-license=1",
        "-Kapp.machines=windows.amd64",
        "make", "windows-msix",
    )
    standardInput = System.`in`
}

tasks.register("buildWindowsMsix") {
    group = "distribution"
    description = "Full SimpMusic Desktop Windows .msix pipeline (vlcSetup → conveyor)."
    dependsOn(conveyorMakeWindowsMsix)
}

tasks.withType<AbstractJPackageTask>().configureEach {
    notCompatibleWithConfigurationCache("Compose Desktop JPackage tasks are not yet compatible with configuration cache")
}

listOf("vlcExtract", "vlcFilterPlugins", "vlcSetup", "clean").forEach { taskName ->
    tasks.findByName(taskName)?.let {
        it.notCompatibleWithConfigurationCache("vlc-setup plugin tasks are not yet compatible with configuration cache")
    }
}

private fun downloadFile(
    url: String,
    destFile: java.io.File,
) {
    val destParent = destFile.parentFile
    destParent.mkdirs()

    if (destFile.exists()) {
        destFile.delete()
    }

    println("Download $url")
    URI(url).toURL().openStream().use { input ->
        destFile.outputStream().use { output ->
            input.copyTo(output)
        }
    }
    println("Download finish")
}
