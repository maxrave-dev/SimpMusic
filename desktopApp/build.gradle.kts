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
    alias(libs.plugins.jetbrains.kotlin.jvm)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.vlc.setup)
    // Provides the `linuxDebConfig {}` DSL used below (sets startupWMClass).
    alias(libs.plugins.packagedeps)
}

version = libs.versions.version.name.get().removeSuffix("-hf")

kotlin {
    // 21 matches :media-jvm-ui (requires 21+).
    jvmToolchain(21)
}

dependencies {
    // Shared KMP library — pulls all commonMain + jvmMain code (App.kt,
    // expect/actual impls, view-models, the MiniPlayer state object, etc.).
    implementation(project(":composeApp"))

    // Compose Desktop runtime for the current OS — required by Window APIs
    // and AWT integration used by the migrated desktop entry / window code.
    implementation(compose.desktop.currentOs)

    // Swing dispatcher for kotlinx.coroutines on the JVM.
    implementation(libs.kotlinx.coroutinesSwing)

    // Sentry crash reporting (full builds only — see BuildKonfig wiring).
    implementation(libs.sentry.jvm)

    // System tray icon for desktop builds.
    implementation(libs.native.tray)

    // Media player JVM UI primitives consumed by jvmMain expect/actuals.
    // Already on the runtime classpath via composeApp, declared again here
    // for the desktop UI components that reference it directly.
    implementation(projects.mediaJvmUi)

    // Commons-IO drives the custom AppImage packaging task below.
    implementation(libs.commons.io)
}

// NOTE: Hydraulic Conveyor wiring was attempted on branch
// feat/conveyor-packaging but blocked by a Gradle config-resolution
// timing conflict between Conveyor 1.12's `implementation.extendsFrom(<arch>)`
// chain and an earlier plugin (suspected vlc-setup or compose.multiplatform)
// resolving `runtimeClasspath` at configuration time. The KMP-structure
// migration to a dedicated :desktopApp module (per JetBrains 2026
// guidance) was kept because it unlocked jpackage runs from a standard
// kotlin("jvm") entry. Conveyor remains a separate follow-up.

// VLC Setup — bundles VLC native libraries so users don't need to install
// VLC. Output is consumed by `compose.desktop.nativeDistributions.appResourcesRootDir`
// and by the in-source DefaultVlcDiscoverer at runtime.
vlcSetup {
    vlcVersion = libs.versions.vlc.get()
    shouldCompressVlcFiles = false
    shouldIncludeAllVlcFiles = true
    pathToCopyVlcLinuxFilesTo = rootDir.resolve("vlc-natives/linux/")
    pathToCopyVlcMacosFilesTo = rootDir.resolve("vlc-natives/macos/")
    pathToCopyVlcWindowsFilesTo = rootDir.resolve("vlc-natives/windows/")
}

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

linuxDebConfig {
    startupWMClass.set("java-lang-Thread")
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

    fun packAppImage(isRelease: Boolean) {
        val appName = "SimpMusic"
        val appDirSrc = rootDir.resolve("composeApp/appimage")
        val packageOutput =
            if (isRelease) {
                layout.buildDirectory
                    .dir("compose/binaries/main-release/app/$appName")
                    .get()
                    .asFile
            } else {
                layout.buildDirectory
                    .dir("compose/binaries/main/app/$appName")
                    .get()
                    .asFile
            }
        if (!appDirSrc.exists() || !packageOutput.exists()) {
            return
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
            if (isRelease) {
                layout.buildDirectory
                    .dir("appimage/main-release/$appName.AppDir")
                    .get()
                    .asFile
            } else {
                layout.buildDirectory
                    .dir("appimage/main/$appName.AppDir")
                    .get()
                    .asFile
            }
        if (appDir.exists()) {
            appDir.deleteRecursively()
        }

        FileUtils.copyDirectory(appDirSrc, appDir)
        FileUtils.copyDirectory(packageOutput, appDir)

        val versionName =
            libs.versions.version.name
                .get()
        val desktopFile = appDir.resolve("simpmusic.desktop")
        desktopFile.writeText(
            """[Desktop Entry]
            |Type=Application
            |Version=1.0
            |Name=SimpMusic
            |Comment=SimpMusic v$versionName - FOSS YouTube Music Client
            |Exec=bin/SimpMusic %u
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
            |# Install icon to XDG icon directories for desktop integration
            |ICON_DIR="${'$'}HOME/.local/share/icons/hicolor/256x256/apps"
            |if [ ! -f "${'$'}ICON_DIR/simpmusic.png" ] || [ "${'$'}HERE/simpmusic.png" -nt "${'$'}ICON_DIR/simpmusic.png" ]; then
            |    mkdir -p "${'$'}ICON_DIR"
            |    cp "${'$'}HERE/simpmusic.png" "${'$'}ICON_DIR/simpmusic.png"
            |    gtk-update-icon-cache -f -t "${'$'}HOME/.local/share/icons/hicolor" 2>/dev/null || true
            |fi
            |
            |# Install .desktop file with WM_CLASS name so GNOME/KDE can match window to icon
            |DESKTOP_DIR="${'$'}HOME/.local/share/applications"
            |mkdir -p "${'$'}DESKTOP_DIR"
            |APPIMAGE_PATH="${'$'}{APPIMAGE:-${'$'}SELF}"
            |sed "s|Exec=bin/SimpMusic|Exec=${'$'}APPIMAGE_PATH|" "${'$'}HERE/simpmusic.desktop" > "${'$'}DESKTOP_DIR/com-maxrave-simpmusic-MainKt.desktop"
            |update-desktop-database "${'$'}DESKTOP_DIR" 2>/dev/null || true
            |
            |cd "${'$'}HERE"
            |exec bin/$appName "${'$'}@"
            |
            """.trimMargin(),
        )
        appRun.setExecutable(true, false)

        val appExecutable = appDir.resolve("bin/$appName")
        if (!appExecutable.canExecute()) {
            appExecutable.setExecutable(true)
        }

        val process =
            ProcessBuilder(
                appimagetool.canonicalPath,
                "$appName.AppDir",
                "$appName-x86_64.AppImage",
            ).directory(appDir.parentFile)
                .apply { environment()["ARCH"] = "x86_64" }
                .inheritIO()
                .start()

        val exitCode = process.waitFor()
        if (exitCode != 0) {
            throw GradleException("appimagetool failed with exit code $exitCode")
        }
    }

    tasks.findByName("packageAppImage")?.doLast {
        packAppImage(false)
    }
    tasks.findByName("packageReleaseAppImage")?.doLast {
        packAppImage(true)
    }
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
