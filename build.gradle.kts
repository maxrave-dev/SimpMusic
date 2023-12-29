// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.2.0" apply false
    id("com.android.library") version "8.2.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.10" apply false
    id("androidx.navigation.safeargs.kotlin") version "2.5.3" apply false
    id("com.google.dagger.hilt.android") version "2.48.1" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.10" apply false
    id("com.google.devtools.ksp") version "1.9.10-1.0.13" apply false
    val latestAboutLibsRelease = "10.9.1"
    id("com.mikepenz.aboutlibraries.plugin") version latestAboutLibsRelease apply false
}
