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
    fun restoreRequestCarriesDiscordAndLastfmCallbacks() {
        val dir = createTempDirectory("simpmusic-deeplink-test").toFile().also { tempDir = it }
        val restoreRequest = File(dir, "simpmusic.restore_request").toPath()

        val delivered = mutableListOf<GenericIntent>()
        DesktopDeepLinkHandler.listener = { intent -> delivered.add(intent) }

        // Second instance writes the link into its restore request; the running one reads it off
        // the request (watcher thread) and delivers it when the window is restored.
        val discordUri = "simpmusic://discord-auth?code=abc&state=123"
        DesktopDeepLinkHandler.writePendingUri(restoreRequest, discordUri)
        DesktopDeepLinkHandler.readPendingUri(restoreRequest)
        DesktopDeepLinkHandler.consumePendingUri()
        assertEquals(1, delivered.size)

        val discordData = delivered.single().data
        assertNotNull(discordData)
        assertEquals("simpmusic", discordData.scheme)
        assertEquals("discord-auth", discordData.host)
        assertEquals("abc", discordData.getQueryParameter("code"))
        assertEquals("123", discordData.getQueryParameter("state"))

        // A link is delivered once: a later restore (tray, Dock) carries nothing.
        DesktopDeepLinkHandler.consumePendingUri()
        assertEquals(1, delivered.size)

        delivered.clear()
        val lastfmUri = "wordbyword://lastfm-auth?token=tok"
        DesktopDeepLinkHandler.writePendingUri(restoreRequest, lastfmUri)
        DesktopDeepLinkHandler.readPendingUri(restoreRequest)
        DesktopDeepLinkHandler.consumePendingUri()
        val lastfmData = delivered.single().data
        assertNotNull(lastfmData)
        assertEquals("wordbyword", lastfmData.scheme)
        assertEquals("lastfm-auth", lastfmData.host)
        assertEquals("tok", lastfmData.getQueryParameter("token"))

        // A restore request with no link (second launch without arguments) delivers nothing.
        delivered.clear()
        restoreRequest.toFile().writeText("")
        DesktopDeepLinkHandler.readPendingUri(restoreRequest)
        DesktopDeepLinkHandler.consumePendingUri()
        assertTrue(delivered.isEmpty())
    }
}
