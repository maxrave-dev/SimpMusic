plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        freeCompilerArgs.add("-Xwhen-guards")
        freeCompilerArgs.add("-Xcontext-parameters")
        freeCompilerArgs.add("-Xmulti-dollar-interpolation")
    }
}

android {
    namespace = "com.maxrave.simpmusic.wear"
    compileSdk = 36

    defaultConfig {
        // Use the same package name as the phone app so Wear Data Layer messages can be delivered.
        // (Data Layer comms are scoped per-app package.)
        applicationId = "com.maxrave.simpmusic"
        minSdk = 26
        targetSdk = 36
        versionCode = libs.versions.version.code.get().toInt()
        versionName = libs.versions.version.name.get()

        // Keep it simple for the first buildable Wear target.
        vectorDrawables.useSupportLibrary = true
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources.excludes +=
            listOf(
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/*.kotlin_module",
            )
    }
}

dependencies {
    coreLibraryDesugaring(libs.desugaring)

    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)

    implementation(libs.activity.compose)
    implementation(libs.compose.ui)
    implementation(libs.ui.tooling.preview)
    implementation(libs.compose.material3.lib)
    implementation(libs.compose.material.icons.core)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.navigation.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.coil.compose)
    implementation(libs.wear.compose.foundation)
    implementation(libs.wear.compose.material3)
    implementation(libs.wear.compose.navigation)
    implementation(libs.wear.input)
    // core/common has XML resources referencing Material theme attrs (e.g. ?attr/colorOnPrimary).
    implementation(libs.material)
    // Wear Data Layer (phone-assisted login, etc.)
    implementation(libs.play.services.wearable)

    // DI (data layer is wired via Koin; dependency is not exposed transitively).
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.core)
    implementation(libs.koin.android)

    // Shared app logic. We’ll reuse these and build a Wear-specific UI on top.
    implementation(project(":common"))
    implementation(project(":domain"))
    implementation(project(":data"))
    implementation(project(":media3"))

    debugImplementation(libs.ui.tooling)
    debugImplementation(libs.wear.compose.ui.tooling)
}
