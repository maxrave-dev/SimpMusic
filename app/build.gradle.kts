import com.android.build.gradle.internal.tasks.CompileArtProfileTask

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.navigation.safeargs)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.aboutlibraries)
}

android {
    namespace = "com.maxrave.simpmusic"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.maxrave.simpmusic"
        minSdk = 26
        targetSdk = 35
        versionCode = 20
        versionName = "0.2.3-hotfix"
        vectorDrawables.useSupportLibrary = true

        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
        }

        resourceConfigurations +=
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
    composeCompiler {
        enableStrongSkippingMode = true
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

    // Compose
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation(libs.compose.material3.lib)
    implementation(libs.compose.ui)
    implementation(libs.compose.material.ripple)
    implementation(libs.compose.material.icons.core)
    implementation(libs.compose.material.icons.extended)
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
    implementation(project(mapOf("path" to ":kotlinYtmusicScraper")))
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
    implementation(libs.coil)
    implementation(libs.coil.compose)
    // Easy Permissions
    implementation(libs.easypermissions)
    // Palette Color
    implementation(libs.palette.ktx)

    // Preference
    implementation(libs.preference.ktx)

    // Fragment KTX
    implementation(libs.fragment.ktx)
    // Hilt
    implementation(libs.hilt.android)
    implementation(libs.hilt.work)
    ksp(libs.hilt.compiler)
    ksp(libs.dagger.hilt.compiler)
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

    // Custom Activity On Crash
    implementation(libs.customactivityoncrash)

    implementation(libs.sdp.android)
    implementation(libs.ssp.android)

    implementation(libs.aboutlibraries)

    implementation(libs.flexbox)
    implementation(libs.balloon)

    // Landscapist
    implementation(libs.landscapist.bom)
    implementation(libs.landscapist.coil)
    implementation(libs.landscapist.placeholder)
    implementation(libs.landscapist.animation)
    implementation(libs.landscapist.palette)
    implementation(libs.landscapist.transformation)
    // InsetsX
    implementation(libs.insetsx)

    coreLibraryDesugaring(libs.desugaring)
}
hilt {
    enableAggregatingTask = true
}
aboutLibraries {
    prettyPrint = true
    registerAndroidTasks = false
    excludeFields = arrayOf("generated")
}
tasks.withType<CompileArtProfileTask>() {
    enabled = false
}