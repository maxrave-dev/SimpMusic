plugins {
    id ("com.android.application")
    id ("org.jetbrains.kotlin.android")
    id ("androidx.navigation.safeargs")
    id ("kotlin-kapt")
    id ("com.google.dagger.hilt.android")
}

android {
    namespace = "com.maxrave.simpmusic"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.maxrave.simpmusic"
        minSdk = 26
        targetSdk = 34
        versionCode = 3
        versionName = "0.0.3-beta"

        resourceConfigurations += listOf("en", "vi")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles (getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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
    }
}

dependencies {

    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.1")
    implementation("androidx.core:core-ktx:1.10.1")
    implementation("androidx.appcompat:appcompat:1.6.1")
    //material design3
    implementation("com.google.android.material:material:1.9.0")
    //runtime
    implementation("androidx.startup:startup-runtime:1.1.1")
    //ExoPlayer
    val media3_version= "1.1.0"

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

    implementation("androidx.room:room-runtime:2.5.2")
    implementation("androidx.room:room-ktx:2.5.2")
    kapt ("androidx.room:room-compiler:2.5.2")
    //Legacy Support
    implementation("androidx.legacy:legacy-support-v4:1.0.0")
    //Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    //Navigation
    implementation("androidx.navigation:navigation-fragment-ktx:2.6.0")
    implementation("androidx.navigation:navigation-ui-ktx:2.6.0")

    //Retrofit 2
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    //OkHttp
    implementation("com.squareup.okhttp3:logging-interceptor:5.0.0-alpha.11")

    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    //Coil
    implementation("io.coil-kt:coil:2.4.0")
    //Glide
    implementation("com.github.bumptech.glide:glide:4.15.1")
    //Easy Permissions
    implementation("pub.devrel:easypermissions:3.0.0")
    //Palette Color
    implementation("androidx.palette:palette-ktx:1.0.0")

    //Preference
    implementation("androidx.preference:preference-ktx:1.2.0")

    //fragment ktx
    implementation("androidx.fragment:fragment-ktx:1.6.0")
    //Hilt
    implementation("com.google.dagger:hilt-android:2.47")
    kapt ("com.google.dagger:hilt-compiler:2.47")
    //Preference ktx
    implementation("androidx.preference:preference-ktx:1.2.0")
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
    val paging_version= "3.2.0-rc01"
    implementation("androidx.paging:paging-runtime-ktx:$paging_version")

    implementation("androidx.legacy:legacy-support-v4:1.0.0")
    implementation("com.daimajia.swipelayout:library:1.2.0@aar")

}
// Allow references to generated code
kapt {
    correctErrorTypes = true
}
hilt {
    enableAggregatingTask = true
}
