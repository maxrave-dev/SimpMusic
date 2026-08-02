@file:Suppress("UnstableApiUsage")

import org.apache.commons.io.FileUtils
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.compose.desktop.application.tasks.AbstractJPackageTask
import java.io.File
import java.net.URI
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

plugins {
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.conveyor)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.multiplatform)
}

version = libs.versions.version.name.get().removeSuffix("-hf")

kotlin {
    jvmToolchain(21)
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
                implementation(project(":composeApp"))
                implementation(compose.desktop.currentOs)
                implementation(libs.kotlinx.coroutinesSwing)
                implementation(libs.sentry.jvm)
                implementation(libs.native.tray)
                implementation(projects.mediaJvmUi)
                implementation(libs.commons.io)
                implementation(libs.org.json)
            }
        }
    }
}

project.configurations.create("desktopRuntimeClasspath") {
    extendsFrom(project.configurations.getByName("jvmRuntimeClasspath"))
}

dependencies {
    linuxAarch64(libs.compose.linux.arm64)
    linuxAmd64(libs.compose.linux.x64)
    macAarch64(libs.compose.macos.arm64)
    macAmd64(libs.compose.macos.x64)
    windowsAarch64(libs.compose.windows.arm64)
    windowsAmd64(libs.compose.windows.x64)
}

tasks.named<hydraulic.conveyor.gradle.WriteConveyorConfigTask>("writeConveyorConfig") {
    dependsOn(tasks.named("proguardReleaseJars"))
    val proguardJarsDir = layout.buildDirectory.dir("compose/tmp/main-release/proguard")
    doLast {
        destination.get().asFile.appendText(
            """
            |app.fsname = simpmusic
            |app.display-name = SimpMusic
            |app.rdns-name = com.maxrave.simpmusic
            |app.vendor = "Garevyn Streaming"
            |
            |app.inputs = [
            |    "${proguardJarsDir.get().asFile.absolutePath}"
            |]
            """.trimMargin() + "\n",
        )
    }
}

compose.desktop {
    application {
        mainClass = "com.maxrave.simpmusic.MainKt"
        
        jvmArgs += listOf(
            "--add-opens=java.base/java.nio=ALL-UNNAMED",
            "-Dvlc.bundled.path=${project.rootProject.file("app-resources/vlc").absolutePath}",
            "-Djna.library.path=${project.rootProject.file("app-resources/vlc").absolutePath}",
            "-Djna.debug_load=true"
        )

        nativeDistributions {
            vendor = "Garevyn Streaming"
            
            jvmArgs(
                "--add-opens=java.base/java.nio=ALL-UNNAMED",
                "-Dvlc.bundled.path=\$APPDIR/vlc",
                "-Djna.library.path=\$APPDIR/vlc",
                "-Djna.debug_load=true"
            )
            
            val listTarget = mutableListOf<TargetFormat>()
            if (org.gradle.internal.os.OperatingSystem.current().isMacOsX) {
                listTarget.addAll(listOf(TargetFormat.Dmg, TargetFormat.Msi))
            } else {
                listTarget.addAll(listOf(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.AppImage))
            }
            targetFormats(*listTarget.toTypedArray())
            modules("jdk.unsupported")
            packageName = "YT Music Mod"
            
            macOS {
                val formatedDate = Instant.now().let {
                    DateTimeFormatter.ofPattern("yyyy.MM.dd").withZone(ZoneId.of("UTC")).format(it)
                }
                includeAllModules = true
                packageVersion = formatedDate
                iconFile.set(rootDir.resolve("composeApp/icon/circle_app_icon.icns"))
                val macExtraPlistKeys = """
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
                packageVersion = libs.versions.version.name.get().removeSuffix("-hf")
                iconFile.set(rootDir.resolve("composeApp/icon/circle_app_icon.ico"))
                shortcut = true
                menu = true
                console = false
            }
            linux {
                includeAllModules = true
                packageVersion = libs.versions.version.name.get().removeSuffix("-hf")
                iconFile.set(rootDir.resolve("composeApp/icon/circle_app_icon.png"))
            }
        }

        buildTypes.release.proguard {
            version.set("7.8.1")
            optimize.set(false)
            obfuscate.set(false)
            configurationFiles.from(rootDir.resolve("composeApp/proguard-desktop-rules.pro"))
        }
    }
}

tasks.withType<AbstractJPackageTask>().configureEach {
    notCompatibleWithConfigurationCache("Compose Desktop JPackage tasks are not yet compatible with configuration cache")
    
    doFirst {
        val osName = System.getProperty("os.name").lowercase()
        val osArch = System.getProperty("os.arch").lowercase()
        
        val sourceZipName = when {
            osName.contains("win") -> "vlc_windows.zip"
            osName.contains("mac") && (osArch == "aarch64" || osArch == "arm64") -> "vlc_mac_silicon.zip"
            osName.contains("mac") -> "vlc_mac_intel.zip"
            else -> "vlc_linux.zip"
        }

        val sourceZip = project.rootProject.file("app-resources/$sourceZipName")
        val targetJarsDir = project.layout.buildDirectory.dir("compose/tmp/main-release/jars").get().asFile
        val targetProguardDir = project.layout.buildDirectory.dir("compose/tmp/main-release/proguard").get().asFile
        
        if (sourceZip.exists()) {
            listOf(targetJarsDir, targetProguardDir).forEach { targetDir ->
                if (targetDir.exists()) {
                    val destZip = File(targetDir, "vlc_bundle.zip")
                    sourceZip.copyTo(destZip, overwrite = true)
                }
            }
        }
    }
}

afterEvaluate {
    tasks.withType<JavaExec> {
        val vlcNativesPath = rootDir.resolve("app-resources/vlc").absolutePath
        jvmArgs(
            "--add-opens", "java.base/java.nio=ALL-UNNAMED",
            "--add-opens", "java.desktop/sun.awt=ALL-UNNAMED",
            "--add-opens", "java.desktop/java.awt.peer=ALL-UNNAMED",
            "-Dvlc.bundled.path=$vlcNativesPath",
            "-Djna.library.path=$vlcNativesPath"
        )

        if (System.getProperty("os.name").contains("Mac")) {
            jvmArgs("--add-opens", "java.desktop/sun.lwawt=ALL-UNNAMED")
            jvmArgs("--add-opens", "java.desktop/sun.lwawt.macosx=ALL-UNNAMED")
        }
    }
}

val conveyorMakeLinuxApp = tasks.register<Exec>("conveyorMakeLinuxApp") {
    group = "distribution"
    dependsOn(":composeApp:vlcSetup")
    workingDir = rootDir
    commandLine("conveyor", "--agree-to-license=1", "-Kapp.machines=linux.amd64.glibc", "make", "linux-app")
    standardInput = System.`in`
}

tasks.register("packageConveyorAppImage") {
    group = "distribution"
    notCompatibleWithConfigurationCache("Reads project/layout/libs from within doLast to compute appimage paths.")

    doLast {
        val appName = "SimpMusic"
        val conveyorOutput = rootDir.resolve("output")
        if (!conveyorOutput.exists()) throw GradleException("Conveyor output not found.")

        val appimagetool = layout.buildDirectory.dir("tmp").get().asFile.resolve("appimagetool-x86_64.AppImage")
        if (!appimagetool.exists()) {
            downloadFile("https://github.com/AppImage/AppImageKit/releases/download/continuous/appimagetool-x86_64.AppImage", appimagetool)
        }
        if (!appimagetool.canExecute()) appimagetool.setExecutable(true)

        val appDir = layout.buildDirectory.dir("appimage/conveyor/$appName.AppDir").get().asFile
        if (appDir.exists()) appDir.deleteRecursively()

        val appDirSrc = rootDir.resolve("composeApp/appimage")
        if (appDirSrc.exists()) {
            FileUtils.copyDirectory(appDirSrc, appDir)
        } else {
            appDir.mkdirs()
        }
        FileUtils.copyDirectory(conveyorOutput, appDir)

        val iconSrc = rootDir.resolve("composeApp/icon/circle_app_icon.png")
        val iconDst = appDir.resolve("simpmusic.png")
        if (!iconDst.exists() && iconSrc.exists()) FileUtils.copyFile(iconSrc, iconDst)

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
            |StartupWMClass=SimpMusic
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
            |ICON_DIR="${'$'}HOME/.local/share/icons/hicolor/256x256/apps"
            |if [ ! -f "${'$'}ICON_DIR/simpmusic.png" ] || [ "${'$'}HERE/simpmusic.png" -nt "${'$'}ICON_DIR/simpmusic.png" ]; then
            |    mkdir -p "${'$'}ICON_DIR"
            |    cp "${'$'}HERE/simpmusic.png" "${'$'}ICON_DIR/simpmusic.png"
            |    gtk-update-icon-cache -f -t "${'$'}HOME/.local/share/icons/hicolor" 2>/dev/null || true
            |fi
            |
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

        val appExecutable = appDir.resolve("bin/simpmusic")
        if (appExecutable.exists() && !appExecutable.canExecute()) appExecutable.setExecutable(true)

        val outputAppImage = appDir.parentFile.resolve("$appName-x86_64.AppImage")
        val process = ProcessBuilder(appimagetool.canonicalPath, "$appName.AppDir", outputAppImage.name)
            .directory(appDir.parentFile)
            .apply { environment()["ARCH"] = "x86_64" }
            .inheritIO()
            .start()

        val exitCode = process.waitFor()
        if (exitCode != 0) throw GradleException("appimagetool failed with exit code $exitCode")
    }
}

tasks.register("buildLinuxAppImage") {
    group = "distribution"
    dependsOn(conveyorMakeLinuxApp)
    finalizedBy("packageConveyorAppImage")
}

val conveyorMakeMacZipAmd64 = tasks.register<Exec>("conveyorMakeMacZipAmd64") {
    group = "distribution"
    dependsOn(":composeApp:vlcSetup")
    workingDir = rootDir
    commandLine("conveyor", "--agree-to-license=1", "-Kapp.machines=mac.amd64", "make", "unnotarized-mac-zip")
    standardInput = System.`in`
}

val conveyorMakeMacZipAarch64 = tasks.register<Exec>("conveyorMakeMacZipAarch64") {
    group = "distribution"
    dependsOn(":composeApp:vlcSetup")
    workingDir = rootDir
    commandLine("conveyor", "--agree-to-license=1", "-Kapp.machines=mac.aarch64", "make", "unnotarized-mac-zip")
    standardInput = System.`in`
}

tasks.register("buildMacZipAmd64") {
    group = "distribution"
    dependsOn(conveyorMakeMacZipAmd64)
}

tasks.register("buildMacZipAarch64") {
    group = "distribution"
    dependsOn(conveyorMakeMacZipAarch64)
}

val conveyorMakeWindowsMsix = tasks.register<Exec>("conveyorMakeWindowsMsix") {
    group = "distribution"
    dependsOn(":composeApp:vlcSetup")
    workingDir = rootDir
    commandLine("conveyor", "--agree-to-license=1", "-Kapp.machines=windows.amd64", "make", "windows-msix")
    standardInput = System.`in`
}

tasks.register("buildWindowsMsix") {
    group = "distribution"
    dependsOn(conveyorMakeWindowsMsix)
}

listOf("vlcExtract", "vlcFilterPlugins", "vlcSetup", "clean").forEach { taskName ->
    tasks.findByName(taskName)?.let {
        it.notCompatibleWithConfigurationCache("vlc-setup plugin tasks are not yet compatible with configuration cache")
    }
}

private fun downloadFile(url: String, destFile: java.io.File) {
    val destParent = destFile.parentFile
    destParent.mkdirs()
    if (destFile.exists()) destFile.delete()
    URI(url).toURL().openStream().use { input ->
        destFile.outputStream().use { output ->
            input.copyTo(output)
        }
    }
}