import com.android.build.gradle.internal.tasks.CompileArtProfileTask

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.android.lint)
}

kotlin {
    android {
        namespace = "org.simpmusic.lastfm"
        compileSdk = 37
        minSdk = 26
    }
    val xcfName = "lastfmKit"

    iosArm64 {
        binaries.framework {
            baseName = xcfName
        }
    }

    iosSimulatorArm64 {
        binaries.framework {
            baseName = xcfName
        }
    }

    jvm()

    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kotlin.stdlib)
                // Logger only — no Ktor, no okio, no API key. A FOSS build carries no Last.fm code.
                implementation(projects.common)
            }
        }
    }
}

tasks.withType<CompileArtProfileTask> {
    enabled = false
}
