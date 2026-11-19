package com.maxrave.simpmusic

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Redirects JVM, JNA, and OpenJDK Unix-domain socket temp directories off
 * virtualized or over-long MSIX paths on Windows before any third-party `<clinit>`
 * reads `java.io.tmpdir`, `jna.tmpdir`, `jdk.net.unixdomain.tmpdir`, or `%TEMP%`.
 *
 * Uses ONLY the JDK standard library.
 */
object DesktopTempEnvironment {
    internal const val MAX_UNIX_SOCKET_TMP_BYTES = 72

    @Volatile
    private var configured = false

    fun configure() {
        if (configured) return
        synchronized(this) {
            if (configured) return
            configureFromSystem()
            configured = true
        }
    }

    internal data class TempRedirectionPlan(
        val javaIoTmpDir: String?,
        val jnaTmpDir: String?,
        val unixDomainTmpDir: String?,
    )

    internal fun computeRedirectionPlan(
        osName: String,
        userHome: String?,
        publicDir: String?,
        currentJavaTmp: String?,
        currentJnaTmp: String?,
        currentUnixSocketTmp: String?,
        envTemp: String?,
        resolveRealPath: (String) -> String? = ::defaultResolveRealPath,
        probeWritableDir: (Path) -> Boolean = ::probeTempDirectory,
    ): TempRedirectionPlan {
        if (!osName.contains("windows", ignoreCase = true)) {
            return TempRedirectionPlan(null, null, null)
        }

        val javaTmpBad =
            currentJavaTmp.isNullOrBlank() ||
                isMsixOrRestrictedTempPath(currentJavaTmp, resolveRealPath) ||
                !isPathWritable(currentJavaTmp, probeWritableDir)

        val jnaTmpBad =
            javaTmpBad ||
                (
                    !currentJnaTmp.isNullOrBlank() &&
                        (
                            isMsixOrRestrictedTempPath(currentJnaTmp, resolveRealPath) ||
                                !isPathWritable(currentJnaTmp, probeWritableDir)
                        )
                )

        // OpenJDK 21 UnixDomainSocketsUtil checks jdk.net.unixdomain.tmpdir, then System.getenv("TEMP")
        // (never "TMP"), and otherwise falls back to StaticProperty.javaIoTmpDir() cached before main().
        val effectiveSocketDefault = envTemp?.takeIf { it.isNotBlank() } ?: currentJavaTmp
        val unixTmpBad =
            if (!currentUnixSocketTmp.isNullOrBlank()) {
                isMsixOrRestrictedTempPath(currentUnixSocketTmp, resolveRealPath) ||
                    exceedsUnixSocketBudget(currentUnixSocketTmp) ||
                    !isPathWritable(currentUnixSocketTmp, probeWritableDir)
            } else {
                javaTmpBad ||
                    effectiveSocketDefault.isNullOrBlank() ||
                    isMsixOrRestrictedTempPath(effectiveSocketDefault, resolveRealPath) ||
                    exceedsUnixSocketBudget(effectiveSocketDefault) ||
                    !isPathWritable(effectiveSocketDefault, probeWritableDir)
            }

        if (!javaTmpBad && !jnaTmpBad && !unixTmpBad) {
            return TempRedirectionPlan(null, null, null)
        }

        val primaryPath =
            resolveCandidateDir(
                baseDir = userHome,
                ".simpmusic",
                "tmp",
                resolveRealPath = resolveRealPath,
                probeWritableDir = probeWritableDir,
            )?.let(::resolveAbsoluteString)

        val needSecondaryForGeneral = (javaTmpBad || jnaTmpBad) && primaryPath == null
        val needSecondaryForUnix = unixTmpBad && (primaryPath == null || exceedsUnixSocketBudget(primaryPath))

        val secondaryPath =
            if (needSecondaryForGeneral || needSecondaryForUnix) {
                resolveCandidateDir(
                    baseDir = publicDir,
                    ".simpmusic-tmp",
                    resolveRealPath = resolveRealPath,
                    probeWritableDir = probeWritableDir,
                )?.let(::resolveAbsoluteString)
            } else {
                null
            }

        val generalDir = primaryPath ?: secondaryPath
        val unixDir =
            listOfNotNull(primaryPath, secondaryPath)
                .firstOrNull { !exceedsUnixSocketBudget(it) }

        return TempRedirectionPlan(
            javaIoTmpDir = if (javaTmpBad) generalDir else null,
            jnaTmpDir = if (jnaTmpBad) generalDir else null,
            unixDomainTmpDir = if (unixTmpBad) unixDir else null,
        )
    }

    internal fun isMsixOrRestrictedTempPath(
        path: String?,
        resolveRealPath: (String) -> String? = ::defaultResolveRealPath,
    ): Boolean {
        if (path.isNullOrBlank()) return false
        if (isRestrictedNormalizedPath(path)) return true
        val resolved = resolveRealPath(path) ?: return false
        return resolved != path && isRestrictedNormalizedPath(resolved)
    }

    internal fun exceedsUnixSocketBudget(path: String): Boolean =
        path.toByteArray(Charsets.UTF_8).size > MAX_UNIX_SOCKET_TMP_BYTES

    internal fun probeTempDirectory(dir: Path): Boolean =
        runCatching {
            Files.createDirectories(dir)
            val probeFile = Files.createTempFile(dir, "simpmusic-", ".tmp")
            Files.deleteIfExists(probeFile)
            true
        }.getOrDefault(false)

    internal fun defaultResolveRealPath(path: String): String? =
        runCatching {
            Paths.get(path).toRealPath().toString()
        }.getOrNull()

    private fun configureFromSystem() {
        val plan =
            computeRedirectionPlan(
                osName = System.getProperty("os.name").orEmpty(),
                userHome = System.getProperty("user.home"),
                publicDir = System.getenv("PUBLIC"),
                currentJavaTmp = System.getProperty("java.io.tmpdir"),
                currentJnaTmp = System.getProperty("jna.tmpdir"),
                currentUnixSocketTmp = System.getProperty("jdk.net.unixdomain.tmpdir"),
                envTemp = System.getenv("TEMP"),
            )
        plan.javaIoTmpDir?.let { System.setProperty("java.io.tmpdir", it) }
        plan.jnaTmpDir?.let { System.setProperty("jna.tmpdir", it) }
        plan.unixDomainTmpDir?.let { System.setProperty("jdk.net.unixdomain.tmpdir", it) }
    }

    private fun isRestrictedNormalizedPath(path: String): Boolean {
        val normalized = path.replace('\\', '/').lowercase()
        return "/wpsystem/" in normalized ||
            normalized.endsWith("/wpsystem") ||
            "/windowsapps/" in normalized ||
            normalized.endsWith("/windowsapps") ||
            ("/appdata/local/packages/" in normalized && ("/localcache/" in normalized || normalized.endsWith("/localcache")))
    }

    private fun isPathWritable(
        path: String,
        probeWritableDir: (Path) -> Boolean,
    ): Boolean =
        runCatching {
            probeWritableDir(Paths.get(path))
        }.getOrDefault(false)

    private fun resolveCandidateDir(
        baseDir: String?,
        vararg segments: String,
        resolveRealPath: (String) -> String?,
        probeWritableDir: (Path) -> Boolean,
    ): Path? {
        val trimmed = baseDir?.takeIf { it.isNotBlank() } ?: return null
        if (isMsixOrRestrictedTempPath(trimmed, resolveRealPath)) return null
        val candidate = runCatching { Paths.get(trimmed, *segments) }.getOrNull() ?: return null
        val candidateStr = resolveAbsoluteString(candidate)
        if (isRestrictedNormalizedPath(candidateStr)) return null
        if (!runCatching { probeWritableDir(candidate) }.getOrDefault(false)) return null
        if (isMsixOrRestrictedTempPath(candidateStr, resolveRealPath)) return null
        return candidate
    }

    private fun resolveAbsoluteString(path: Path): String {
        val raw = path.toString()
        return if (path.isAbsolute || isWindowsAbsolutePath(raw)) {
            raw
        } else {
            path.toAbsolutePath().toString()
        }
    }

    private fun isWindowsAbsolutePath(path: String): Boolean =
        (path.length >= 3 && path[0].isLetter() && path[1] == ':' && (path[2] == '\\' || path[2] == '/')) ||
            path.startsWith("\\\\")
}
