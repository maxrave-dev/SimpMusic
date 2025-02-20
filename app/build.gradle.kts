import com.android.build.gradle.internal.tasks.CompileArtProfileTask

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.navigation.safeargs)
    alias(libs.plugins.ksp)
    alias(libs.plugins.aboutlibraries)
}

android {
    namespace = "com.maxrave.simpmusic"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.maxrave.simpmusic"
        minSdk = 26
        targetSdk = 35
        versionCode =
            libs.versions.version.code
                .get()
                .toInt()
        versionName =
            libs.versions.version.name
                .get()
        vectorDrawables.useSupportLibrary = true

        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
            arg("KOIN_CONFIG_CHECK", "true")
            arg("KOIN_USE_COMPOSE_VIEWMODEL", "true")
        }

        androidResources.localeFilters +=
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
            )
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
                    include("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        jvmToolchain(17)
    }
    kotlinOptions {
        freeCompilerArgs = freeCompilerArgs + "-Xcontext-receivers"
        jvmTarget = "17"
    }
    // enable view binding
    buildFeatures {
        viewBinding = true
        compose = true
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
                "META-INF/notice.txt",
                "META-INF/NOTICE.txt",
                "META-INF/LICENSE.txt",
                "META-INF/license.txt",
                "META-INF/notice.txt",
                "META-INF/NOTICE",
                "META-INF/LICENSE",
                "META-INF/notice",
                "META-INF/notice.txt",
                "META-INF/NOTICE.txt",
                "META-INF/LICENSE.txt",
                "META-INF/license.txt",
                "META-INF/notice.txt",
                "META-INF/NOTICE",
                "META-INF/LICENSE",
                "META-INF/notice",
                "META-INF/notice.txt",
                "META-INF/NOTICE.txt",
                "META-INF/LICENSE.txt",
                "META-INF/license.txt",
                "META-INF/notice.txt",
                "META-INF/NOTICE",
                "META-INF/LICENSE",
                "META-INF/notice",
                "META-INF/notice.txt",
                "META-INF/NOTICE.txt",
                "META-INF/LICENSE.txt",
                "META-INF/license.txt",
                "META-INF/notice.txt",
                "META-INF/NOTICE",
                "META-INF/LICENSE",
                "META-INF/notice",
                "META-INF/notice.txt",
                "META-INF/NOTICE.txt",
                "META-INF/LICENSE.txt",
                "META-INF/license.txt",
                "META-INF/notice.txt",
                "META-INF/NOTICE",
                "META-INF/LICENSE",
                "META-INF/notice",
                "META-INF/notice.txt",
                "META-INF/NOTICE.txt",
                "META-INF/LICENSE.txt",
                "META-INF/license.txt",
                "META-INF/notice.txt",
                "META-INF/NOTICE",
                "META-INF/LICENSE",
                "META-INF/notice",
                "META-INF/notice.txt",
                "META-INF/NOTICE.txt",
                "META-INF/LICENSE.txt",
                "META-INF/license.txt",
                "META-INF/notice.txt",
                "META-INF/NOTICE",
                "META-INF/LICENSE",
                "META-INF/notice",
                "META-INF/notice.txt",
                "META-INF/NOTICE.txt",
                "META-INF/LICENSE.txt",
                "META-INF/license.txt",
                "META-INF/notice.txt",
                "META-INF/NOTICE",
                "META-INF/LICENSE",
                "META-INF/notice",
                "META-INF/notice.txt",
                "META-INF/NOTICE.txt",
                "META-INF/LICENSE.txt",
                "META-INF/license.txt",
                "META-INF/notice",
                "META-INF/ASL2.0",
                "META-INF/*.kotlin_module",
            )
    }
}

dependencies {

    implementation(project(":lyricsProviders"))
    // Compose
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation(libs.compose.material3.lib)
    implementation(libs.compose.material3.sizeclass)
    implementation(libs.compose.ui)
    implementation(libs.compose.material.ripple)
    implementation(libs.compose.material.icons.core)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.compose.ui.viewbinding)
    implementation(libs.constraintlayout.compose)

    // Android Studio Preview support
    implementation(libs.ui.tooling.preview)
    implementation(libs.activity.compose)
    // Optional - Integration with ViewModels
    implementation(libs.lifecycle.viewmodel.compose)

    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.core.ktx)
    implementation(libs.appcompat)

    implementation(libs.work.runtime.ktx)
    androidTestImplementation(libs.work.testing)

    // Material Design 3
    implementation(libs.material)
    // Runtime
    implementation(libs.startup.runtime)
    // Other module
    implementation(project(mapOf("path" to ":kotlinYtmusicScraper")))
    implementation(project(mapOf("path" to ":spotify")))

    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)
    debugImplementation(libs.ui.tooling)

    // ExoPlayer
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.ui)
    implementation(libs.media3.session)
    implementation(libs.media3.exoplayer.dash)
    implementation(libs.media3.exoplayer.hls)
    implementation(libs.media3.exoplayer.rtsp)
    implementation(libs.media3.exoplayer.smoothstreaming)
    implementation(libs.media3.exoplayer.workmanager)
    implementation(libs.media3.datasource.okhttp)
    implementation(libs.okhttp3.logging.interceptor)

    // Palette Color
    implementation(libs.palette.ktx)
    // Expandable Text View
    implementation(libs.expandable.text)

    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.espresso.core)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    // Legacy Support
    implementation(libs.legacy.support.v4)
    // Coroutines
    implementation(libs.coroutines.android)
    implementation(libs.coroutines.guava)
    // Navigation
    implementation(libs.navigation.fragment.ktx)
    implementation(libs.navigation.ui.ktx)

    implementation(libs.gson)

    // Coil
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.kmpalette.core)
    // Easy Permissions
    implementation(libs.easypermissions)
    // Palette Color
    implementation(libs.palette.ktx)

    // Preference
    implementation(libs.preference.ktx)

    // Fragment KTX
    implementation(libs.fragment.ktx)
    ksp(libs.kotlinx.metadata.jvm)
    // DataStore
    implementation(libs.datastore.preferences)
    // Swipe To Refresh
    implementation(libs.swiperefreshlayout)
    // Insetter
    implementation(libs.insetter)
    implementation(libs.insetter.dbx)

    // Shimmer
    implementation(libs.shimmer)

    // Lottie
    implementation(libs.lottie)
    implementation(libs.lottie.compose)

    // Paging 3
    implementation(libs.paging.runtime.ktx)
    implementation(libs.paging.compose)

    // Custom Activity On Crash
    implementation(libs.customactivityoncrash)

    implementation(libs.sdp.android)
    implementation(libs.ssp.android)

    implementation(libs.aboutlibraries)
    implementation(libs.aboutlibraries.compose.m3)

    implementation(libs.flexbox)
    implementation(libs.balloon)

    // InsetsX
    implementation(libs.insetsx)

    coreLibraryDesugaring(libs.desugaring)

    // Koin
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.core)
    implementation(libs.koin.android)
    implementation(libs.koin.workmanager)
    implementation(libs.koin.androidx.compose)

    // Store5
    implementation(libs.store)

    // Jetbrains Markdown
    api(libs.markdown)

    // Blur Haze
    implementation(libs.haze)
    implementation(libs.haze.material)
}
aboutLibraries {
    prettyPrint = true
    registerAndroidTasks = false
    excludeFields = arrayOf("generated")
}
tasks.withType<CompileArtProfileTask> {
    enabled = false
}