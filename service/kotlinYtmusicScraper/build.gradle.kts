import com.android.build.gradle.internal.tasks.CompileArtProfileTask

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.aboutlibraries)
}

kotlin {
    jvmToolchain(17)
}

android {
    namespace = "com.maxrave.kotlinytmusicscraper"
    compileSdk = 36

    defaultConfig {
        minSdk = 26

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
    kotlin {
        jvmToolchain(17)
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
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
}

dependencies {
    coreLibraryDesugaring(libs.desugaring)
    implementation(libs.core.ktx)
    implementation(libs.appcompat)
    implementation(libs.androidx.ui.text.android)
    implementation(libs.androidx.webkit)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.espresso.core)

    implementation(project(":common"))

    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.encoding)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.logging)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.xml)
    implementation(libs.ktor.serialization.kotlinx.protobuf)

    implementation(libs.brotli.dec)

    implementation(libs.kotlin.reflect)
    implementation(libs.gson)

    implementation(libs.ksoup.html)
    implementation(libs.ksoup.entities)
    implementation(libs.smart.exception)

    implementation(project(mapOf("path" to ":youtubeapi")))
    implementation(project(mapOf("path" to ":sharedutils")))
    implementation(libs.ffmpeg.kit.audio)
    implementation(libs.newpipe.extractor)

    implementation(libs.ytdlp.android)
}
tasks.withType<CompileArtProfileTask> {
    enabled = false
}