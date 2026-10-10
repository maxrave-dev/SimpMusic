import java.util.Properties

// SimpMusic for vivo: androidApp's sources, shipped under a package vivo's Origin Island follows.
// vivo's island and music widget only follow apps on vivo's own list of package names, and a normal
// app cannot add itself to that list. This module builds androidApp unchanged under one of the listed
// names. That name must stay in Config.OFFICIAL_PACKAGE_NAMES, and the APK must be signed with our key,
// or the unofficial-build check blocks it. Built with: ./build_and_sign_apk.sh --release --full --vivo
// Everything except the package and the ABI mirrors androidApp/build.gradle.kts: keep the two in step.

val isFullBuild: Boolean =
    try {
        extra["isFullBuild"] == "true"
    } catch (e: Exception) {
        false
    }

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.sentry.gradle)
    alias(libs.plugins.compose.compiler)
}

val androidAppDir = rootProject.file("androidApp")

android {
    namespace = "com.maxrave.simpmusic"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.spotify.music"
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
                    "zh-rCN",
                    "id",
                    "in",
                    "ar",
                    "ja",
                    "zh-rTW",
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
                    "sv",
                    "hr",
                )
        }

        // vivo phones with Origin Island are all arm64, so one APK and no ABI splits.
        ndk {
            abiFilters.add("arm64-v8a")
        }
    }

    sourceSets {
        getByName("main") {
            manifest.srcFile(File(androidAppDir, "src/main/AndroidManifest.xml"))
            // androidApp keeps its Kotlin under src/main/java; both sets point there so built-in Kotlin
            // compiles it whichever directory set it reads.
            java.srcDirs(File(androidAppDir, "src/main/java"))
            kotlin.srcDirs(File(androidAppDir, "src/main/java"))
            res.srcDirs(File(androidAppDir, "src/main/res"))
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                File(androidAppDir, "proguard-rules.pro"),
            )
        }
        debug {
            // No ".dev" suffix: vivo matches the exact package, and a debug build is how this is tried on a vivo phone.
            isMinifyEnabled = false
        }
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
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
            // Same reasons as androidApp: kuromoji's duplicate META-INF docs, and its dictionary is
            // downloaded on demand instead of shipped.
            excludes += "META-INF/*.md"
            excludes += "com/atilika/kuromoji/ipadic/*.bin"
        }
    }
}

dependencies {
    coreLibraryDesugaring(libs.desugaring)
    val debugImplementation = "debugImplementation"
    debugImplementation(libs.ui.tooling)
    implementation(libs.activity.compose)
    implementation(libs.customactivityoncrash)
    implementation(libs.easypermissions)
    implementation(libs.legacy.support.v4)
    implementation(libs.coroutines.android)
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

// Only the Full build is shipped for vivo, so androidApp's FOSS-only Sentry meta cleanup is not repeated.
sentry {
    org.set("simpmusic")
    projectName.set("android")
    ignoredBuildTypes.set(setOf("debug"))
    autoInstallation.enabled = false
    if (isFullBuild) {
        val token =
            try {
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
