pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven { setUrl("https://jitpack.io") }
        // oss.sonatype.org (legacy OSSRH) removed — Sonatype shut it down; its flaky
        // 504s disabled the whole repo set and blocked resolution fallbacks.
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
        // compottie's Compose-1.12 snapshot builds (skiko 0.148.2) are published to the
        // Maven Central Portal snapshot repo below. The legacy OSSRH repo
        // (oss.sonatype.org) was removed — Sonatype shut it down and its flaky 504s
        // disabled the repo set, blocking fallback to this one.
        maven("https://central.sonatype.com/repository/maven-snapshots/")
        maven("https://jogamp.org/deployment/maven")
        maven(url = "https://raw.githubusercontent.com/bravepipeproject/maven-repo/master/repository")
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

// Core modules live in the `core` git submodule INSIDE this repo.
// We intentionally resolve ONLY the in-repo submodule and no longer probe a
// sibling `../core` outside SimpMusic: another project (FPT Play `core`) shares
// the same folder name one level up, and the old co-development lookup bound to
// it by mistake, breaking configuration with ":common ... does not exist".
val coreDir = File(rootDir, "core")
val serviceDir = File(rootDir, "core/service")
val mediaDir = File(rootDir, "core/media")

rootProject.name = "SimpMusic"
include(
    ":androidApp",
    ":composeApp",
    ":desktopApp",
    ":common",
    ":data",
    ":domain",
    ":ktorExt",
    ":kotlinYtmusicScraper",
    ":spotify",
    ":aiService",
    ":lyricsService",
    ":media-jvm",
    ":media-jvm-ui",
    ":media3",
    ":media3-ui",
    ":crashlytics",
    ":crashlytics-empty",
    ":kizzy",
)

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