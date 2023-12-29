plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    kotlin("plugin.serialization")
    id ("com.mikepenz.aboutlibraries.plugin")
}

android {
    namespace = "com.maxrave.kotlinytmusicscraper"
    compileSdk = 34

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
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    packaging {
        jniLibs.useLegacyPackaging = true
        jniLibs.excludes += listOf("META-INF/META-INF/DEPENDENCIES", "META-INF/LICENSE", "META-INF/LICENSE.txt", "META-INF/license.txt", "META-INF/NOTICE", "META-INF/NOTICE.txt", "META-INF/notice.txt", "META-INF/ASL2.0", "META-INF/asm-license.txt", "META-INF/notice.txt", "META-INF/NOTICE.txt", "META-INF/LICENSE.txt", "META-INF/license.txt", "META-INF/notice.txt", "META-INF/NOTICE", "META-INF/LICENSE", "META-INF/notice", "META-INF/notice.txt", "META-INF/NOTICE.txt", "META-INF/LICENSE.txt", "META-INF/license.txt", "META-INF/notice.txt", "META-INF/NOTICE", "META-INF/LICENSE", "META-INF/notice", "META-INF/notice.txt", "META-INF/NOTICE.txt", "META-INF/LICENSE.txt", "META-INF/license.txt", "META-INF/notice.txt", "META-INF/NOTICE", "META-INF/LICENSE", "META-INF/notice", "META-INF/notice.txt", "META-INF/NOTICE.txt", "META-INF/LICENSE.txt", "META-INF/license.txt", "META-INF/notice.txt", "META-INF/NOTICE", "META-INF/LICENSE", "META-INF/notice", "META-INF/notice.txt", "META-INF/NOTICE.txt", "META-INF/LICENSE.txt", "META-INF/license.txt", "META-INF/notice.txt", "META-INF/NOTICE", "META-INF/LICENSE", "META-INF/notice", "META-INF/notice.txt", "META-INF/NOTICE.txt", "META-INF/LICENSE.txt", "META-INF/license.txt", "META-INF/notice.txt", "META-INF/NOTICE", "META-INF/LICENSE", "META-INF/notice", "META-INF/notice.txt", "META-INF/NOTICE.txt", "META-INF/LICENSE.txt", "META-INF/license.txt", "META-INF/notice.txt", "META-INF/NOTICE", "META-INF/LICENSE", "META-INF/notice", "META-INF/notice.txt", "META-INF/NOTICE.txt", "META-INF/LICENSE.txt", "META-INF/license.txt", "META-INF/notice.txt", "META-INF/NOTICE", "META-INF/LICENSE", "META-INF/notice", "META-INF/notice.txt", "META-INF/NOTICE.txt", "META-INF/LICENSE.txt", "META-INF/license.txt", "META-INF/notice", "META-INF/ASL2.0", "META-INF/*.kotlin_module")
    }
}

dependencies {

    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    val ktor_version= "2.3.5"

    implementation("io.ktor:ktor-client-core:$ktor_version")
    implementation("io.ktor:ktor-client-cio:$ktor_version")
    implementation("io.ktor:ktor-client-okhttp:$ktor_version")
    implementation("io.ktor:ktor-client-encoding:$ktor_version")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktor_version")
    implementation("io.ktor:ktor-client-content-negotiation:$ktor_version")
    implementation("io.ktor:ktor-serialization-kotlinx-xml:$ktor_version")

    implementation("org.brotli:dec:0.1.2")

    implementation(kotlin("reflect"))
    implementation("com.google.code.gson:gson:2.10.1")

    val version = "0.2.1"

// For parsing HTML
    implementation("com.mohamedrejeb.ksoup:ksoup-html:$version")

// Only for encoding and decoding HTML entities
    implementation("com.mohamedrejeb.ksoup:ksoup-entites:$version")
}