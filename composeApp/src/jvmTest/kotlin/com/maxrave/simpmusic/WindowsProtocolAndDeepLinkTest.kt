package com.maxrave.simpmusic

import com.maxrave.domain.data.model.intent.GenericIntent
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WindowsProtocolAndDeepLinkTest {
    private var tempDir: File? = null

    @BeforeTest
    fun setUp() {
        DesktopDeepLinkHandler.resetForTest()
    }

    @AfterTest
    fun tearDown() {
        DesktopDeepLinkHandler.resetForTest()
        tempDir?.deleteRecursively()
        tempDir = null
    }

    @Test
    fun msixWindowsAppsPathSkipsRegistrationAndFlagsStaleHkcuForCleanup() {
        val msixPaths =
            listOf(
                """C:\Program Files\WindowsApps\com.maxrave.simpmusic_2.6.0.0_x64__abc123\SimpMusic.exe""",
                """C:\Program Files\WindowsApps\com.maxrave.simpmusic_2.6.0.0_x64__abc123\simpmusic.exe""",
                "C:/Program Files/WindowsApps/com.maxrave.simpmusic_2.6.0.0_x64__abc123/SimpMusic.exe",
                "\"C:/Program Files/WindowsApps/com.maxrave.simpmusic_2.6.0.0_x64__abc123/simpmusic.exe\"",
            )

        msixPaths.forEach { path ->
            assertEquals(
                ProtocolRegistrationDecision.SKIP_MSIX_AND_CLEANUP,
                WindowsProtocolRegistrar.decideRegistration(path, isDevBuild = false),
                "Expected SKIP_MSIX_AND_CLEANUP for $path",
            )
            assertTrue(
                WindowsProtocolRegistrar.shouldDeleteStaleHkcuCommand("\"$path\" \"%1\""),
                "Expected stale HKCU cleanup for $path",
            )
        }

        assertFalse(WindowsProtocolRegistrar.shouldDeleteStaleHkcuCommand(null))
        assertFalse(WindowsProtocolRegistrar.shouldDeleteStaleHkcuCommand("   "))
    }

    @Test
    fun devBuildAndJavaExeSkipRegistration() {
        val standaloneExe = """C:\Users\Alice\AppData\Local\Programs\SimpMusic\SimpMusic.exe"""
        assertEquals(
            ProtocolRegistrationDecision.SKIP_DEV_OR_JAVA,
            WindowsProtocolRegistrar.decideRegistration(standaloneExe, isDevBuild = true),
        )

        val javaExePaths =
            listOf(
                """C:\Program Files\Java\jdk-21\bin\java.exe""",
                """C:\Program Files\Java\jdk-21\bin\javaw.exe""",
                "C:/Program Files/Java/jdk-21/bin/java.exe",
                "C:/Program Files/Java/jdk-21/bin/javaw.exe",
            )

        javaExePaths.forEach { javaPath ->
            assertEquals(
                ProtocolRegistrationDecision.SKIP_DEV_OR_JAVA,
                WindowsProtocolRegistrar.decideRegistration(javaPath, isDevBuild = false),
                "Expected SKIP_DEV_OR_JAVA for $javaPath",
            )
            assertTrue(
                WindowsProtocolRegistrar.shouldDeleteStaleHkcuCommand("\"$javaPath\" \"%1\""),
                "Expected stale HKCU cleanup for $javaPath",
            )
        }
    }

    @Test
    fun standaloneInstallerPathRegistersNormally() {
        val standalonePaths =
            listOf(
                """C:\Users\Alice\AppData\Local\Programs\SimpMusic\SimpMusic.exe""",
                """C:\Users\Alice\AppData\Local\Programs\SimpMusic\simpmusic.exe""",
            )

        standalonePaths.forEach { path ->
            assertEquals(
                ProtocolRegistrationDecision.REGISTER,
                WindowsProtocolRegistrar.decideRegistration(path, isDevBuild = false),
                "Expected REGISTER for $path",
            )
            assertFalse(
                WindowsProtocolRegistrar.shouldDeleteStaleHkcuCommand("\"$path\" \"%1\""),
                "Standalone path should not be flagged for stale HKCU deletion: $path",
            )
        }
    }

    @Test
    fun pendingUriRoundTripDeliversDiscordAndLastfmCallbacks() {
        val dir = createTempDirectory("simpmusic-deeplink-test").toFile().also { tempDir = it }
        val pendingFile = File(File(dir, "nested"), "simpmusic_pending_deeplink.txt")

        val delivered = mutableListOf<GenericIntent>()
        DesktopDeepLinkHandler.listener = { intent -> delivered.add(intent) }

        val discordUri = "simpmusic://discord-auth?code=abc&state=123"
        DesktopDeepLinkHandler.writePendingUri(discordUri, pendingFile)
        assertTrue(pendingFile.exists(), "Pending URI file should exist after writePendingUri")

        assertTrue(DesktopDeepLinkHandler.consumePendingUri(pendingFile))
        assertFalse(pendingFile.exists(), "Pending URI file should be deleted after consumePendingUri")
        assertEquals(1, delivered.size)

        val discordData = delivered.single().data
        assertNotNull(discordData)
        assertEquals("simpmusic", discordData.scheme)
        assertEquals("discord-auth", discordData.host)
        assertEquals("code=abc&state=123", discordData.query)
        assertEquals("abc", discordData.getQueryParameter("code"))
        assertEquals("123", discordData.getQueryParameter("state"))

        delivered.clear()

        val lastfmUri = "wordbyword://lastfm-auth?token=tok"
        DesktopDeepLinkHandler.writePendingUri(lastfmUri, pendingFile)
        assertTrue(pendingFile.exists())

        assertTrue(DesktopDeepLinkHandler.consumePendingUri(pendingFile))
        assertFalse(pendingFile.exists())
        assertEquals(1, delivered.size)

        val lastfmData = delivered.single().data
        assertNotNull(lastfmData)
        assertEquals("wordbyword", lastfmData.scheme)
        assertEquals("lastfm-auth", lastfmData.host)
        assertEquals("token=tok", lastfmData.query)
        assertEquals("tok", lastfmData.getQueryParameter("token"))

        // Consuming again when file is absent returns false and emits nothing.
        assertFalse(DesktopDeepLinkHandler.consumePendingUri(pendingFile))
        assertEquals(1, delivered.size)

        // clearPendingUri deletes staged file only when expectedUri matches (preserving newer second-instance URIs).
        DesktopDeepLinkHandler.writePendingUri(discordUri, pendingFile)
        assertTrue(pendingFile.exists())
        DesktopDeepLinkHandler.clearPendingUri(expectedUri = lastfmUri, targetFile = pendingFile)
        assertTrue(pendingFile.exists(), "clearPendingUri must preserve file if overwritten by a newer URI")
        DesktopDeepLinkHandler.clearPendingUri(expectedUri = discordUri, targetFile = pendingFile)
        assertFalse(pendingFile.exists())
        assertFalse(DesktopDeepLinkHandler.consumePendingUri(pendingFile))
        assertNull(delivered.getOrNull(1))
    }
}
