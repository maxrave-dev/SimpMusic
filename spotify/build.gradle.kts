import com.android.build.gradle.internal.tasks.CompileArtProfileTask

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    kotlin("plugin.serialization")
    alias(libs.plugins.aboutlibraries)
}

android {
    namespace = "com.maxrave.spotify"
    compileSdk = 35

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

    implementation(libs.core.ktx)
    implementation(libs.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.espresso.core)

    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.encoding)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.protobuf)

    implementation(libs.brotli.dec)

    implementation(libs.kotlin.reflect)
}
tasks.withType<CompileArtProfileTask> {
    enabled = false
}