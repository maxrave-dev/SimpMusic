package com.maxrave.simpmusic

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DesktopTempEnvironmentTest {
    private fun assertNormalizedPathEquals(
        expected: String,
        actual: String?,
    ) {
        assertNotNull(actual)
        assertEquals(expected.replace('\\', '/'), actual.replace('\\', '/'))
    }

    @Test
    fun `WpSystem LocalCache temp redirects all three temp properties to userHome fallback`() {
        val wpTemp = """D:\WpSystem\S-1-5-21-123\AppData\Local\Packages\Simpmusic_ejp2bhxmz1qq6\LocalCache\Local\Temp"""
        val plan =
            DesktopTempEnvironment.computeRedirectionPlan(
                osName = "Windows 11",
                userHome = """C:\Users\alice""",
                publicDir = """C:\Users\Public""",
                currentJavaTmp = wpTemp,
                currentJnaTmp = null,
                currentUnixSocketTmp = null,
                envTemp = wpTemp,
                resolveRealPath = { it },
                probeWritableDir = { true },
            )

        assertNormalizedPathEquals("""C:\Users\alice\.simpmusic\tmp""", plan.javaIoTmpDir)
        assertNormalizedPathEquals("""C:\Users\alice\.simpmusic\tmp""", plan.jnaTmpDir)
        assertNormalizedPathEquals("""C:\Users\alice\.simpmusic\tmp""", plan.unixDomainTmpDir)
    }

    @Test
    fun `MSIX Packages LocalCache temp and junction resolving to WpSystem redirect all three properties`() {
        val packagesTemp = """C:\Users\user\AppData\Local\Packages\Simpmusic_ejp2bhxmz1qq6\LocalCache\Local\Temp"""
        val directPlan =
            DesktopTempEnvironment.computeRedirectionPlan(
                osName = "Windows 11",
                userHome = """C:\Users\user""",
                publicDir = """C:\Users\Public""",
                currentJavaTmp = packagesTemp,
                currentJnaTmp = null,
                currentUnixSocketTmp = null,
                envTemp = packagesTemp,
                resolveRealPath = { it },
                probeWritableDir = { true },
            )

        assertNormalizedPathEquals("""C:\Users\user\.simpmusic\tmp""", directPlan.javaIoTmpDir)
        assertNormalizedPathEquals("""C:\Users\user\.simpmusic\tmp""", directPlan.jnaTmpDir)
        assertNormalizedPathEquals("""C:\Users\user\.simpmusic\tmp""", directPlan.unixDomainTmpDir)

        val junctionPath = """C:\Users\user\LocalTempJunction"""
        val junctionTarget = """D:\WpSystem\S-1-5-21-999\AppData\Local\Packages\Simpmusic_ejp2bhxmz1qq6\LocalCache\Local\Temp"""
        val junctionPlan =
            DesktopTempEnvironment.computeRedirectionPlan(
                osName = "Windows 10",
                userHome = """C:\Users\user""",
                publicDir = """C:\Users\Public""",
                currentJavaTmp = junctionPath,
                currentJnaTmp = null,
                currentUnixSocketTmp = null,
                envTemp = junctionPath,
                resolveRealPath = { path -> if (path == junctionPath) junctionTarget else path },
                probeWritableDir = { true },
            )

        assertNormalizedPathEquals("""C:\Users\user\.simpmusic\tmp""", junctionPlan.javaIoTmpDir)
        assertNormalizedPathEquals("""C:\Users\user\.simpmusic\tmp""", junctionPlan.jnaTmpDir)
        assertNormalizedPathEquals("""C:\Users\user\.simpmusic\tmp""", junctionPlan.unixDomainTmpDir)
    }

    @Test
    fun `healthy short javaIoTmpDir with long MSIX env TEMP redirects only unixDomainTmpDir`() {
        val longMsixEnvTemp =
            """C:\Users\alice\AppData\Local\Packages\Simpmusic_ejp2bhxmz1qq6\LocalCache\Local\Temp"""
        assertTrue(DesktopTempEnvironment.exceedsUnixSocketBudget(longMsixEnvTemp))

        val plan =
            DesktopTempEnvironment.computeRedirectionPlan(
                osName = "Windows 11",
                userHome = """C:\Users\alice""",
                publicDir = """C:\Users\Public""",
                currentJavaTmp = """C:\Temp""",
                currentJnaTmp = null,
                currentUnixSocketTmp = null,
                envTemp = longMsixEnvTemp,
                resolveRealPath = { it },
                probeWritableDir = { true },
            )

        assertNull(plan.javaIoTmpDir)
        assertNull(plan.jnaTmpDir)
        assertNormalizedPathEquals("""C:\Users\alice\.simpmusic\tmp""", plan.unixDomainTmpDir)

        val unsetTempWithShortJavaTmp =
            DesktopTempEnvironment.computeRedirectionPlan(
                osName = "Windows 11",
                userHome = """C:\Users\alice""",
                publicDir = """C:\Users\Public""",
                currentJavaTmp = """C:\Temp""",
                currentJnaTmp = null,
                currentUnixSocketTmp = null,
                envTemp = null,
                resolveRealPath = { it },
                probeWritableDir = { true },
            )
        assertNull(unsetTempWithShortJavaTmp.unixDomainTmpDir)
    }

    @Test
    fun `poisoned jnaTmpDir is redirected even when javaIoTmpDir is healthy`() {
        val plan =
            DesktopTempEnvironment.computeRedirectionPlan(
                osName = "Windows 11",
                userHome = """C:\Users\alice""",
                publicDir = """C:\Users\Public""",
                currentJavaTmp = """C:\Temp""",
                currentJnaTmp = """D:\WpSystem\S-1-5-21-1\AppData\Local\Packages\Simpmusic_ejp2bhxmz1qq6\LocalCache\Local\Temp""",
                currentUnixSocketTmp = null,
                envTemp = """C:\Temp""",
                resolveRealPath = { it },
                probeWritableDir = { true },
            )

        assertNull(plan.javaIoTmpDir)
        assertNormalizedPathEquals("""C:\Users\alice\.simpmusic\tmp""", plan.jnaTmpDir)
        assertNull(plan.unixDomainTmpDir)
    }

    @Test
    fun `unwritable javaIoTmpDir redirects cleanly and failing all probes returns nulls without throwing`() {
        val unwritableTmp = """C:\LockedTemp"""
        val redirected =
            DesktopTempEnvironment.computeRedirectionPlan(
                osName = "Windows 11",
                userHome = """C:\Users\alice""",
                publicDir = """C:\Users\Public""",
                currentJavaTmp = unwritableTmp,
                currentJnaTmp = null,
                currentUnixSocketTmp = null,
                envTemp = unwritableTmp,
                resolveRealPath = { it },
                probeWritableDir = { dir: Path -> dir.toString().replace('\\', '/') != unwritableTmp.replace('\\', '/') },
            )

        assertNormalizedPathEquals("""C:\Users\alice\.simpmusic\tmp""", redirected.javaIoTmpDir)
        assertNormalizedPathEquals("""C:\Users\alice\.simpmusic\tmp""", redirected.jnaTmpDir)
        assertNormalizedPathEquals("""C:\Users\alice\.simpmusic\tmp""", redirected.unixDomainTmpDir)

        val allFailed =
            DesktopTempEnvironment.computeRedirectionPlan(
                osName = "Windows 11",
                userHome = """C:\Users\alice""",
                publicDir = """C:\Users\Public""",
                currentJavaTmp = unwritableTmp,
                currentJnaTmp = null,
                currentUnixSocketTmp = null,
                envTemp = unwritableTmp,
                resolveRealPath = { it },
                probeWritableDir = { false },
            )

        assertNull(allFailed.javaIoTmpDir)
        assertNull(allFailed.jnaTmpDir)
        assertNull(allFailed.unixDomainTmpDir)
    }

    @Test
    fun `long userHome keeps userHome fallback for java and jna but uses short publicDir for unixDomainTmpDir`() {
        val longUserHome = """C:\Users\a_very_long_windows_username_that_exceeds_seventy_two_utf8_bytes_easily"""
        assertTrue(DesktopTempEnvironment.exceedsUnixSocketBudget("""$longUserHome\.simpmusic\tmp"""))

        val poisonedTemp = """D:\WpSystem\S-1-5-21-1\AppData\Local\Packages\Simpmusic_ejp2bhxmz1qq6\LocalCache\Local\Temp"""
        val plan =
            DesktopTempEnvironment.computeRedirectionPlan(
                osName = "Windows 11",
                userHome = longUserHome,
                publicDir = """C:\Users\Public""",
                currentJavaTmp = poisonedTemp,
                currentJnaTmp = null,
                currentUnixSocketTmp = null,
                envTemp = poisonedTemp,
                resolveRealPath = { it },
                probeWritableDir = { true },
            )

        assertNormalizedPathEquals("""$longUserHome\.simpmusic\tmp""", plan.javaIoTmpDir)
        assertNormalizedPathEquals("""$longUserHome\.simpmusic\tmp""", plan.jnaTmpDir)
        assertNormalizedPathEquals("""C:\Users\Public\.simpmusic-tmp""", plan.unixDomainTmpDir)
    }

    @Test
    fun `non-Windows OS returns all nulls`() {
        val wpTemp = """D:\WpSystem\S-1-5-21-1\AppData\Local\Packages\Simpmusic_ejp2bhxmz1qq6\LocalCache\Local\Temp"""
        listOf("Linux", "Mac OS X").forEach { os ->
            val plan =
                DesktopTempEnvironment.computeRedirectionPlan(
                    osName = os,
                    userHome = "/home/alice",
                    publicDir = null,
                    currentJavaTmp = wpTemp,
                    currentJnaTmp = wpTemp,
                    currentUnixSocketTmp = wpTemp,
                    envTemp = wpTemp,
                    resolveRealPath = { it },
                    probeWritableDir = { false },
                )

            assertNull(plan.javaIoTmpDir, "Expected null javaIoTmpDir for $os")
            assertNull(plan.jnaTmpDir, "Expected null jnaTmpDir for $os")
            assertNull(plan.unixDomainTmpDir, "Expected null unixDomainTmpDir for $os")
        }
    }

    @Test
    fun `probeTempDirectory creates missing directory, tests write, and deletes probe file`() {
        val root = Files.createTempDirectory("simpmusic-temp-probe-test")
        try {
            val targetDir = root.resolve("nested").resolve("tmp")
            assertFalse(Files.exists(targetDir))

            assertTrue(DesktopTempEnvironment.probeTempDirectory(targetDir))
            assertTrue(Files.isDirectory(targetDir))

            Files.list(targetDir).use { children ->
                assertEquals(0L, children.count(), "Probe temp file must be cleaned up")
            }
        } finally {
            root.toFile().deleteRecursively()
        }
    }
}
