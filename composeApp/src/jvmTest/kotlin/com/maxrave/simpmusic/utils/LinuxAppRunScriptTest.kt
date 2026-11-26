package com.maxrave.simpmusic.utils

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LinuxAppRunScriptTest {
    @Test
    fun skipsIntegrationWhenGearLeverOrAppImageLauncherDesktopEntryExists() {
        val appImagePath = "/home/alice/AppImages/SimpMusic-x86_64.AppImage"
        val gearLeverDesktop =
            mapOf(
                "it.mijorus.gearlever_simpmusic.desktop" to
                    """
                    [Desktop Entry]
                    Type=Application
                    Name=SimpMusic
                    Exec=/home/alice/AppImages/SimpMusic-x86_64.AppImage %U
                    """.trimIndent(),
            )

        assertTrue(
            LinuxAppRunScript.shouldSkipDesktopIntegration(
                desktopIntegrationEnv = null,
                noDesktopFileEnv = null,
                existingDesktopFiles = gearLeverDesktop,
                appImagePath = appImagePath,
            ),
        )
    }

    @Test
    fun doesNotTreatOwnSelfManagedDesktopEntryAsExternalManager() {
        val appImagePath = "/home/alice/Downloads/SimpMusic-x86_64.AppImage"
        val ownDesktopOnly =
            mapOf(
                LinuxAppRunScript.SELF_DESKTOP_FILENAME to
                    """
                    [Desktop Entry]
                    Type=Application
                    Name=SimpMusic
                    Exec="/home/alice/Downloads/SimpMusic-x86_64.AppImage" %u
                    """.trimIndent(),
            )

        assertFalse(
            LinuxAppRunScript.shouldSkipDesktopIntegration(
                desktopIntegrationEnv = null,
                noDesktopFileEnv = null,
                existingDesktopFiles = ownDesktopOnly,
                appImagePath = appImagePath,
            ),
        )
    }

    @Test
    fun honoursDesktopIntegrationAndNoDesktopFileEnvironmentFlags() {
        val appImagePath = "/home/alice/SimpMusic.AppImage"
        assertTrue(
            LinuxAppRunScript.shouldSkipDesktopIntegration(
                desktopIntegrationEnv = "0",
                noDesktopFileEnv = null,
                existingDesktopFiles = emptyMap(),
                appImagePath = appImagePath,
            ),
        )
        assertTrue(
            LinuxAppRunScript.shouldSkipDesktopIntegration(
                desktopIntegrationEnv = "false",
                noDesktopFileEnv = null,
                existingDesktopFiles = emptyMap(),
                appImagePath = appImagePath,
            ),
        )
        assertTrue(
            LinuxAppRunScript.shouldSkipDesktopIntegration(
                desktopIntegrationEnv = null,
                noDesktopFileEnv = "1",
                existingDesktopFiles = emptyMap(),
                appImagePath = appImagePath,
            ),
        )
    }

    @Test
    fun escapeDesktopExecPathQuotesSpacesAndSpecialChars() {
        assertEquals(
            "\"/home/alice/My Apps/SimpMusic \"v2\".AppImage\"",
            LinuxAppRunScript.escapeDesktopExecPath("/home/alice/My Apps/SimpMusic \"v2\".AppImage")
                .replace("\\\"", "\""),
        )
        assertEquals(
            "\"/home/alice/My Apps/SimpMusic \\\"v2\\\".AppImage\"",
            LinuxAppRunScript.escapeDesktopExecPath("/home/alice/My Apps/SimpMusic \"v2\".AppImage"),
        )
    }

    @Test
    fun generatedAppRunScriptUsesPosixShAndCleansUpDuplicatesWhenExternallyManaged() {
        val script = LinuxAppRunScript.generate()
        assertTrue(script.startsWith("#!/bin/sh\n"))
        assertFalse(script.contains("[["))
        assertTrue(script.contains("XDG_DATA_HOME"))
        assertTrue(script.contains("DESKTOPINTEGRATION"))
        assertTrue(script.contains("SIMPMUSIC_NO_DESKTOP_FILE"))
        assertTrue(script.contains("EXTERNAL_MANAGED=1"))
        assertTrue(script.contains("rm -f \"\$SELF_DESKTOP\""))
        assertTrue(script.contains("export MALLOC_ARENA_MAX=2"))
        assertTrue(script.contains("exec bin/simpmusic \"\$@\""))
    }
}
