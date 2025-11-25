pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven { setUrl("https://jitpack.io") }
        maven {
            url = uri("https://oss.sonatype.org/content/repositories/snapshots/")
        }
        maven("https://jogamp.org/deployment/maven")
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven { url = uri("https://jitpack.io") }
        maven {
            url = uri("https://oss.sonatype.org/content/repositories/snapshots/")
        }
        maven("https://jogamp.org/deployment/maven")
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

// prepare for git submodules
val mediaServiceCore =
    if (File(rootDir, "../MediaServiceCore").exists()) {
        File(rootDir, "../MediaServiceCore")
    } else {
        File(rootDir, "./MediaServiceCore")
    }

val sharedDir =
    if (File(rootDir, "../MediaServiceCore/SharedModules").exists()) {
        File(rootDir, "../MediaServiceCore/SharedModules")
    } else {
        File(rootDir, "./MediaServiceCore/SharedModules")
    }

val coreDir =
    if (File(rootDir, "../core").exists()) {
        File(rootDir, "../core")
    } else {
        File(rootDir, "./core")
    }

val serviceDir =
    if (File(rootDir, "../core/service").exists()) {
        File(rootDir, "../core/service")
    } else {
        File(rootDir, "./core/service")
    }

val mediaDir =
    if (File(rootDir, "../core/media").exists()) {
        File(rootDir, "../core/media")
    } else {
        File(rootDir, "./core/media")
    }

rootProject.name = "SimpMusic"
include(
    "composeApp",
    ":common",
    ":data",
    ":domain",
    ":ktorExt",
    ":kotlinYtmusicScraper",
    ":spotify",
    ":aiService",
    ":lyricsService",
    ":mediaserviceinterfaces",
    ":youtubeapi",
    ":googleapi",
    ":sharedtests",
    ":sharedutils",
    ":media-jvm",
    ":media-jvm-ui",
    ":media3",
    ":media3-ui",
    ":crashlytics",
    ":crashlytics-empty",
    ":kizzy",
)
project(":mediaserviceinterfaces").projectDir = File(mediaServiceCore, "mediaserviceinterfaces")
project(":youtubeapi").projectDir = File(mediaServiceCore, "youtubeapi")
project(":googleapi").projectDir = File(mediaServiceCore, "googleapi")
project(":sharedtests").projectDir = File(sharedDir, "sharedtests")
project(":sharedutils").projectDir = File(sharedDir, "sharedutils")

// core modules
project(":common").projectDir = File(coreDir, "common")
project(":data").projectDir = File(coreDir, "data")
project(":domain").projectDir = File(coreDir, "domain")

// service modules
project(":ktorExt").projectDir = File(serviceDir, "ktorExt")
project(":aiService").projectDir = File(serviceDir, "aiService")
project(":lyricsService").projectDir = File(serviceDir, "lyricsService")
project(":kotlinYtmusicScraper").projectDir = File(serviceDir, "kotlinYtmusicScraper")
project(":spotify").projectDir = File(serviceDir, "spotify")
project(":kizzy").projectDir = File(serviceDir, "kizzy")

// media modules
project(":media-jvm").projectDir = File(mediaDir, "media-jvm")
project(":media-jvm-ui").projectDir = File(mediaDir, "media-jvm-ui")
project(":media3").projectDir = File(mediaDir, "media3")
project(":media3-ui").projectDir = File(mediaDir, "media3-ui")

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")