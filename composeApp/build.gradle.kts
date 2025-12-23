@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.INT
import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

val isFullBuild: Boolean =
    try {
        extra["isFullBuild"] == "true"
    } catch (e: Exception) {
        false
    }

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.application)
    alias(libs.plugins.sentry.gradle)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
//    alias(libs.plugins.compose.hotReload)
    alias(libs.plugins.aboutlibraries.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.build.config)
    alias(libs.plugins.osdetector)
}

dependencies {
    coreLibraryDesugaring(libs.desugaring)
    val debugImplementation = "debugImplementation"
    debugImplementation(libs.ui.tooling)
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xwhen-guards")
        freeCompilerArgs.add("-Xcontext-parameters")
        freeCompilerArgs.add("-Xmulti-dollar-interpolation")
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
    androidTarget {
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
        }
        androidMain.dependencies {
            implementation(libs.koin.android)
            implementation(libs.koin.androidx.compose)

            implementation(libs.jetbrains.ui.tooling.preview)
            implementation(libs.activity.compose)
            implementation(libs.constraintlayout.compose)

            implementation(libs.work.runtime.ktx)

            // Runtime
            implementation(libs.startup.runtime)

            // Glance
            implementation(libs.glance)
            implementation(libs.glance.appwidget)
            implementation(libs.glance.material3)

            // Liquid glass
            implementation(libs.liquid.glass)

            // Custom Activity On Crash
            implementation(libs.customactivityoncrash)

            // Easy Permissions
            implementation(libs.easypermissions)

            // Legacy Support
            implementation(libs.legacy.support.v4)
            // Coroutines
            implementation(libs.coroutines.android)

            implementation(projects.media3)
            implementation(projects.media3Ui)

            if (isFullBuild) {
                implementation(projects.crashlytics)
            } else {
                implementation(projects.crashlyticsEmpty)
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
            implementation(projects.common)
            implementation(projects.domain)
            implementation(projects.data)

            // Navigation Compose
            implementation(libs.navigation.compose)

            // Kotlin Serialization
            implementation(libs.kotlinx.serialization.json)

            // Coil
            implementation(libs.coil.compose)
            implementation(libs.coil.network.okhttp)
            implementation(libs.kmpalette.core)

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

            implementation(libs.cmptoast)
            implementation(libs.file.picker)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
            implementation(libs.sentry.jvm)
            implementation(projects.mediaJvmUi)
        }
    }
}

android {
    val abis = arrayOf("armeabi-v7a", "arm64-v8a", "x86_64")

    namespace = "com.maxrave.simpmusic"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.maxrave.simpmusic"
        minSdk = 26
        targetSdk = 36
        versionCode =
            libs.versions.version.code
                .get()
                .toInt()
        versionName =
            libs.versions.version.name
                .get()
        vectorDrawables.useSupportLibrary = true
        multiDexEnabled = true

        @Suppress("UnstableApiUsage")
        androidResources {
            localeFilters +=
                listOf(
                    "en",
                    "vi",
                    "it",
                    "de",
                    "ru",
                    "tr",
                    "fi",
                    "pl",
                    "pt",
                    "fr",
                    "es",
                    "zh",
                    "in",
                    "ar",
                    "ja",
                    "b+zh+Hant+TW",
                    "uk",
                    "iw",
                    "az",
                    "hi",
                    "th",
                    "nl",
                    "ko",
                    "ca",
                    "fa",
                    "bg",
                )
        }
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            abiFilters.add("x86_64")
            abiFilters.add("armeabi-v7a")
            abiFilters.add("arm64-v8a")
        }
    }

    bundle {
        language {
            enableSplit = false
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "consumer-rules.pro",
                "proguard-rules.pro",
            )
            splits {
                abi {
                    isEnable = true
                    reset()
                    isUniversalApk = true
                    include(*abis)
                }
            }
        }
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
        }
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    // enable view binding
    buildFeatures {
        viewBinding = true
        compose = true
        buildConfig = true
    }
    packaging {
        jniLibs.useLegacyPackaging = true
        jniLibs.excludes +=
            listOf(
                "META-INF/META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/license.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/notice.txt",
                "META-INF/ASL2.0",
                "META-INF/asm-license.txt",
                "META-INF/notice",
                "META-INF/*.kotlin_module",
            )
        resources {
            excludes += "META-INF/versions/9/OSGI-INF/MANIFEST.MF"
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.maxrave.simpmusic.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb, TargetFormat.Rpm)
            modules("jdk.unsupported")
            packageName = "SimpMusic"
            macOS {
                includeAllModules = true
                packageVersion = "2025.12.24"
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

sentry {
    org.set("simpmusic")
    projectName.set("android")
    ignoredFlavors.set(setOf("foss"))
    ignoredBuildTypes.set(setOf("debug"))
    autoInstallation.enabled = false
    if (isFullBuild) {
        val token =
            try {
                println("Full build detected, enabling Sentry Auth Token")
                val properties = Properties()
                properties.load(rootProject.file("local.properties").inputStream())
                properties.getProperty("SENTRY_AUTH_TOKEN")
            } catch (e: Exception) {
                println("Failed to load SENTRY_AUTH_TOKEN from local.properties: ${e.message}")
                null
            }
        authToken.set(token ?: "")
        includeProguardMapping.set(true)
        autoUploadProguardMapping.set(true)
    } else {
        includeProguardMapping.set(false)
        autoUploadProguardMapping.set(false)
        uploadNativeSymbols.set(false)
        includeDependenciesReport.set(false)
        includeSourceContext.set(false)
        includeNativeSources.set(false)
    }
    telemetry.set(false)
}

if (!isFullBuild) {
    abstract class CleanSentryMetaTask : DefaultTask() {
        @get:InputFiles
        abstract val assetDirectories: ConfigurableFileCollection

        @get:Internal
        abstract val buildDirectory: DirectoryProperty

        @TaskAction
        fun execute() {
            assetDirectories.forEach { assetDir ->
                val sentryFile = File(assetDir, "sentry-debug-meta.properties")
                if (sentryFile.exists()) {
                    sentryFile.delete()
                    println("Deleted: ${sentryFile.absolutePath}")
                }
            }

            val dirName = "release/mergeReleaseAssets"
            val injectDirName = "release/injectSentryDebugMetaPropertiesIntoAssetsRelease"
            println("Cleaning Sentry meta files in build directories")
            println("Build directory: ${buildDirectory.asFile.get().absolutePath}")

            val buildAssetsDir = File(buildDirectory.asFile.get(), "intermediates/assets/$dirName")
            println("Checking directory buildAssetsDir: ${buildAssetsDir.absolutePath}")
            val sentryFile = File(buildAssetsDir, "sentry-debug-meta.properties")
            if (sentryFile.exists()) {
                sentryFile.delete()
                println("Deleted: ${sentryFile.absolutePath}")
            }

            val injectBuildAssetsDir = File(buildDirectory.asFile.get(), "intermediates/assets/$injectDirName")
            println("Checking directory injectBuildAssetsDir: ${injectBuildAssetsDir.absolutePath}")
            val injectSentryFile = File(injectBuildAssetsDir, "sentry-debug-meta.properties")
            if (injectSentryFile.exists()) {
                injectSentryFile.delete()
                println("Deleted: ${injectSentryFile.absolutePath}")
                val sentryFile = File(injectBuildAssetsDir, "sentry-debug-meta.properties")
                sentryFile.writeText("")
                println("✓ Overwritten: ${sentryFile.absolutePath}")
            }
        }
    }

    tasks.whenTaskAdded {
        if (name.contains("injectSentryDebugMetaPropertiesIntoAssetsRelease")) {
            val cleanSentryMetaTaskName = "cleanSentryMetaForRelease"
            val cleanSentryMetaTask =
                tasks.register<CleanSentryMetaTask>(cleanSentryMetaTaskName) {
                    assetDirectories.from(android.sourceSets.flatMap { it.assets.srcDirs })
                    buildDirectory.set(layout.buildDirectory)
                }
            tasks.named(name).configure {
                finalizedBy(cleanSentryMetaTask)
            }
        }
    }
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
}