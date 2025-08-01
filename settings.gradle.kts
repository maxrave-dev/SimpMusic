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

rootProject.name = "SimpMusic"
include(
    "app",
    ":kotlinYtmusicScraper",
    ":spotify",
    ":aiService",
    ":ffmpeg-kit",
    ":lyricsService",
    ":mediaserviceinterfaces",
    ":youtubeapi",
    ":googleapi",
    ":sharedtests",
    ":commons-io-2.8.0",
    ":sharedutils",
)
project(":mediaserviceinterfaces").projectDir = File(mediaServiceCore, "mediaserviceinterfaces")
project(":youtubeapi").projectDir = File(mediaServiceCore, "youtubeapi")
project(":googleapi").projectDir = File(mediaServiceCore, "googleapi")
project(":sharedtests").projectDir = File(sharedDir, "sharedtests")
project(":commons-io-2.8.0").projectDir = File(sharedDir, "commons-io-2.8.0")
project(":sharedutils").projectDir = File(sharedDir, "sharedutils")