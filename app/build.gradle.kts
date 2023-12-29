plugins {
    id ("com.android.application")
    id ("org.jetbrains.kotlin.android")
    id ("androidx.navigation.safeargs")
    id ("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
    id ("com.mikepenz.aboutlibraries.plugin")
}

android {
    namespace = "com.maxrave.simpmusic"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.maxrave.simpmusic"
        minSdk = 26
        targetSdk = 34
        versionCode = 14
        versionName = "0.1.7"

        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
        }

        resourceConfigurations += listOf(
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
            "in"
        )
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            applicationIdSuffix = ".dev"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_1_8.toString()
    }
    //enable view binding
    buildFeatures {
        viewBinding = true
    }
    packaging {
        jniLibs.useLegacyPackaging = true
        jniLibs.excludes += listOf("META-INF/META-INF/DEPENDENCIES", "META-INF/LICENSE", "META-INF/LICENSE.txt", "META-INF/license.txt", "META-INF/NOTICE", "META-INF/NOTICE.txt", "META-INF/notice.txt", "META-INF/ASL2.0", "META-INF/asm-license.txt", "META-INF/notice.txt", "META-INF/NOTICE.txt", "META-INF/LICENSE.txt", "META-INF/license.txt", "META-INF/notice.txt", "META-INF/NOTICE", "META-INF/LICENSE", "META-INF/notice", "META-INF/notice.txt", "META-INF/NOTICE.txt", "META-INF/LICENSE.txt", "META-INF/license.txt", "META-INF/notice.txt", "META-INF/NOTICE", "META-INF/LICENSE", "META-INF/notice", "META-INF/notice.txt", "META-INF/NOTICE.txt", "META-INF/LICENSE.txt", "META-INF/license.txt", "META-INF/notice.txt", "META-INF/NOTICE", "META-INF/LICENSE", "META-INF/notice", "META-INF/notice.txt", "META-INF/NOTICE.txt", "META-INF/LICENSE.txt", "META-INF/license.txt", "META-INF/notice.txt", "META-INF/NOTICE", "META-INF/LICENSE", "META-INF/notice", "META-INF/notice.txt", "META-INF/NOTICE.txt", "META-INF/LICENSE.txt", "META-INF/license.txt", "META-INF/notice.txt", "META-INF/NOTICE", "META-INF/LICENSE", "META-INF/notice", "META-INF/notice.txt", "META-INF/NOTICE.txt", "META-INF/LICENSE.txt", "META-INF/license.txt", "META-INF/notice.txt", "META-INF/NOTICE", "META-INF/LICENSE", "META-INF/notice", "META-INF/notice.txt", "META-INF/NOTICE.txt", "META-INF/LICENSE.txt", "META-INF/license.txt", "META-INF/notice.txt", "META-INF/NOTICE", "META-INF/LICENSE", "META-INF/notice", "META-INF/notice.txt", "META-INF/NOTICE.txt", "META-INF/LICENSE.txt", "META-INF/license.txt", "META-INF/notice.txt", "META-INF/NOTICE", "META-INF/LICENSE", "META-INF/notice", "META-INF/notice.txt", "META-INF/NOTICE.txt", "META-INF/LICENSE.txt", "META-INF/license.txt", "META-INF/notice", "META-INF/ASL2.0", "META-INF/*.kotlin_module")
    }
}

dependencies {

    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    //material design3
    implementation("com.google.android.material:material:1.11.0")
    //runtime
    implementation("androidx.startup:startup-runtime:1.1.1")
    implementation(project(mapOf("path" to ":kotlinYtmusicScraper")))
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.6.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2")

    //ExoPlayer
    val media3_version = "1.2.0"

    implementation("androidx.media3:media3-exoplayer:$media3_version")
    implementation("androidx.media3:media3-ui:$media3_version")
    implementation("androidx.media3:media3-session:$media3_version")
    implementation("androidx.media3:media3-exoplayer-dash:$media3_version")
    implementation("androidx.media3:media3-exoplayer-hls:$media3_version")
    implementation("androidx.media3:media3-exoplayer-rtsp:$media3_version")
    implementation("androidx.media3:media3-exoplayer-smoothstreaming:$media3_version")
    implementation("androidx.media3:media3-exoplayer-workmanager:$media3_version")
    implementation("androidx.media3:media3-datasource-okhttp:$media3_version")

    //palette color
    implementation("androidx.palette:palette-ktx:1.0.0")
    //expandable text view
    implementation("com.github.giangpham96:expandable-text:2.0.0")


    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
    //Legacy Support
    implementation("androidx.legacy:legacy-support-v4:1.0.0")
    //Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-guava:1.7.3")
    //Navigation
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.6")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.6")

    implementation("com.google.code.gson:gson:2.10.1")

    //Coil
    implementation("io.coil-kt:coil:2.4.0")
    //Glide
    implementation("com.github.bumptech.glide:glide:4.16.0")
    //Easy Permissions
    implementation("pub.devrel:easypermissions:3.0.0")
    //Palette Color
    implementation("androidx.palette:palette-ktx:1.0.0")

    //Preference
    implementation("androidx.preference:preference-ktx:1.2.1")

    //fragment ktx
    implementation("androidx.fragment:fragment-ktx:1.6.2")
    //Hilt
    implementation("com.google.dagger:hilt-android:2.48.1")
    ksp("com.google.dagger:hilt-compiler:2.48.1")
    ksp("org.jetbrains.kotlinx:kotlinx-metadata-jvm:0.7.0")
    //DataStore
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    //Swipe To Refresh
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.2.0-alpha01")
    //Insetter
    implementation("dev.chrisbanes.insetter:insetter:0.6.1")
    implementation("dev.chrisbanes.insetter:insetter-dbx:0.6.1")

    //Shimmer
    implementation("com.facebook.shimmer:shimmer:0.5.0")

    //Lottie
    val lottieVersion = "6.1.0"
    implementation("com.airbnb.android:lottie:$lottieVersion")

    //Paging 3
    val paging_version= "3.2.1"
    implementation("androidx.paging:paging-runtime-ktx:$paging_version")

    implementation("com.daimajia.swipelayout:library:1.2.0@aar")


    //Custom Activity On Crash
    implementation ("cat.ereza:customactivityoncrash:2.4.0")

    implementation("com.intuit.sdp:sdp-android:1.1.0")
    implementation("com.intuit.ssp:ssp-android:1.1.0")

    val latestAboutLibsRelease = "10.9.2"
    implementation ("com.mikepenz:aboutlibraries:${latestAboutLibsRelease}")

    implementation("com.google.android.flexbox:flexbox:3.0.0")

}
hilt {
    enableAggregatingTask = true
}
aboutLibraries {
    prettyPrint = true
    registerAndroidTasks = false
    excludeFields = arrayOf("generated")
}
