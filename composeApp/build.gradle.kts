@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.INT
import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING
import org.apache.commons.io.FileUtils
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.compose.desktop.application.tasks.AbstractJPackageTask
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.net.URI
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
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

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xwhen-guards")
        freeCompilerArgs.add("-Xcontext-parameters")
        freeCompilerArgs.add("-Xmulti-dollar-interpolation")
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
    android {
        namespace = "com.maxrave.simpmusic.composeapp"
        compileSdk = 36
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
            api(libs.koin.android)
            implementation(libs.koin.androidx.compose)

            implementation(libs.jetbrains.ui.tooling.preview)
            implementation(libs.constraintlayout.compose)

            api(libs.work.runtime.ktx)

            // Runtime
            api(libs.startup.runtime)

            // Liquid glass
            implementation(libs.liquid.glass)

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
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
            implementation(libs.sentry.jvm)
            implementation(libs.native.tray)
            implementation(projects.mediaJvmUi)
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.maxrave.simpmusic.MainKt"

        nativeDistributions {
            val listTarget = mutableListOf<TargetFormat>()
            if (org.gradle.internal.os.OperatingSystem
                    .current()
                    .isMacOsX
            ) {
                listTarget.addAll(
                    listOf(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb, TargetFormat.Rpm),
                )
            } else {
                listTarget.addAll(
                    listOf(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb, TargetFormat.Rpm, TargetFormat.AppImage),
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
                iconFile.set(project.file("icon/circle_app_icon.icns"))
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
                iconFile.set(project.file("icon/circle_app_icon.ico"))
            }
            linux {
                includeAllModules = true
                packageVersion =
                    libs.versions.version.name
                        .get()
                        .removeSuffix("-hf")
                iconFile.set(project.file("icon/circle_app_icon.png"))
            }
        }

        buildTypes.release.proguard {
            optimize.set(true)
            obfuscate.set(true)
            configurationFiles.from("proguard-desktop-rules.pro")
        }
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

linuxDebConfig {
    startupWMClass.set("java-lang-Thread")
}

afterEvaluate {
    tasks.withType<JavaExec> {
        jvmArgs("--add-opens", "java.desktop/sun.awt=ALL-UNNAMED")
        jvmArgs("--add-opens", "java.desktop/java.awt.peer=ALL-UNNAMED")

        if (System.getProperty("os.name").contains("Mac")) {
            jvmArgs("--add-opens", "java.desktop/sun.awt=ALL-UNNAMED")
            jvmArgs("--add-opens", "java.desktop/sun.lwawt=ALL-UNNAMED")
            jvmArgs("--add-opens", "java.desktop/sun.lwawt.macosx=ALL-UNNAMED")
        }
    }

    fun packAppImage(isRelease: Boolean) {
        val appName = "SimpMusic"
        val appDirSrc = project.file("appimage")
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

        val appExecutable = appDir.resolve("bin/$appName")
        if (!appExecutable.canExecute()) {
            appimagetool.setExecutable(true)
        }

        val appRun = appDir.resolve("AppRun")
        if (!appRun.canExecute()) {
            appRun.setReadable(true, false) // readable by all
            appRun.setWritable(true, true) // writable only by owner
            appRun.setExecutable(true, false)

            println(
                "Set AppRun executable permissions, readable: ${appRun.canRead()}, writable: ${appRun.canWrite()}, executable: ${appRun.canExecute()}",
            )
        }

        // Use ProcessBuilder instead of exec {} to avoid capturing project reference
        val process =
            ProcessBuilder(
                appimagetool.canonicalPath,
                "$appName.AppDir",
                "$appName-x86_64.AppImage",
            ).directory(appDir.parentFile)
                .apply { environment()["ARCH"] = "x86_64" } // TODO: 支持arm64
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

// Mark JPackage tasks as not compatible with configuration cache
// This must be done outside afterEvaluate to work properly
tasks.withType<AbstractJPackageTask>().configureEach {
    notCompatibleWithConfigurationCache("Compose Desktop JPackage tasks are not yet compatible with configuration cache")
}

private fun downloadFile(
    url: String,
    destFile: File,
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