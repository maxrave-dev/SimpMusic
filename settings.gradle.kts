pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven { setUrl("https://jitpack.io") }
        maven {
            url = uri("https://oss.sonatype.org/content/repositories/snapshots/")
        }
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
    }
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
    if (File(rootDir, "../service").exists()) {
        File(rootDir, "../service")
    } else {
        File(rootDir, "./service")
    }

val mediaDir =
    if (File(rootDir, "../media").exists()) {
        File(rootDir, "../media")
    } else {
        File(rootDir, "./media")
    }

rootProject.name = "SimpMusic"
include(
    "app",
    ":common",
    ":data",
    ":domain",
    ":kotlinYtmusicScraper",
    ":spotify",
    ":aiService",
    ":lyricsService",
    ":mediaserviceinterfaces",
    ":youtubeapi",
    ":googleapi",
    ":sharedtests",
    ":sharedutils",
    ":j2v8",
    ":media3",
    ":media3-ui",
)
project(":mediaserviceinterfaces").projectDir = File(mediaServiceCore, "mediaserviceinterfaces")
project(":youtubeapi").projectDir = File(mediaServiceCore, "youtubeapi")
project(":googleapi").projectDir = File(mediaServiceCore, "googleapi")
project(":sharedtests").projectDir = File(sharedDir, "sharedtests")
project(":sharedutils").projectDir = File(sharedDir, "sharedutils")
project(":j2v8").projectDir = File(sharedDir, "j2v8")

// core modules
project(":common").projectDir = File(coreDir, "common")
project(":data").projectDir = File(coreDir, "data")
project(":domain").projectDir = File(coreDir, "domain")

// service modules
project(":aiService").projectDir = File(serviceDir, "aiService")
project(":lyricsService").projectDir = File(serviceDir, "lyricsService")
project(":kotlinYtmusicScraper").projectDir = File(serviceDir, "kotlinYtmusicScraper")
project(":spotify").projectDir = File(serviceDir, "spotify")

// media modules
project(":media3").projectDir = File(mediaDir, "media3")
project(":media3-ui").projectDir = File(mediaDir, "media3-ui")