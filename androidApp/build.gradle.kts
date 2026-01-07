import java.util.Properties

val isFullBuild: Boolean =
    try {
        extra["isFullBuild"] == "true"
    } catch (e: Exception) {
        false
    }

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.sentry.gradle)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
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
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

dependencies {
    coreLibraryDesugaring(libs.desugaring)
    val debugImplementation = "debugImplementation"
    debugImplementation(libs.ui.tooling)
    implementation(libs.activity.compose)

    // Custom Activity On Crash
    implementation(libs.customactivityoncrash)

    // Easy Permissions
    implementation(libs.easypermissions)

    // Legacy Support
    implementation(libs.legacy.support.v4)
    // Coroutines
    implementation(libs.coroutines.android)

    // Glance
    implementation(libs.glance)
    implementation(libs.glance.appwidget)
    implementation(libs.glance.material3)

    implementation(projects.composeApp)
    implementation(projects.data)

    if (isFullBuild) {
        implementation(projects.crashlytics)
    } else {
        implementation(projects.crashlyticsEmpty)
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