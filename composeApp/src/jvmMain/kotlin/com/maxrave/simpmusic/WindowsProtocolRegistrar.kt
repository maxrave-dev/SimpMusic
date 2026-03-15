package com.maxrave.simpmusic

import com.maxrave.logger.Logger

/**
 * Registers the "simpmusic://" custom URI protocol handler in Windows Registry
 * under HKEY_CURRENT_USER (no admin rights required).
 *
 * Registry structure:
 * ```
 * HKCU\Software\Classes\simpmusic
 *     (Default) = "URL:SimpMusic Protocol"
 *     URL Protocol = ""
 *     \DefaultIcon
 *         (Default) = "\"<exe_path>\",0"
 *     \shell\open\command
 *         (Default) = "\"<exe_path>\" \"%1\""
 * ```
 */
object WindowsProtocolRegistrar {
    private const val TAG = "WindowsProtocolRegistrar"
    private const val SCHEME = "simpmusic"
    private const val REG_KEY = "HKCU\\Software\\Classes\\$SCHEME"

    fun register() {
        if (!System.getProperty("os.name", "").contains("Windows", ignoreCase = true)) return

        val exePath = resolveExePath() ?: run {
            Logger.e(TAG, "Could not resolve executable path, skipping protocol registration")
            return
        }

        try {
            if (isAlreadyRegistered(exePath)) {
                Logger.d(TAG, "Protocol handler already registered with correct path")
                return
            }

            Logger.d(TAG, "Registering simpmusic:// protocol handler -> $exePath")

            // Main key with protocol description
            regAdd(REG_KEY, null, "URL:SimpMusic Protocol")
            regAdd(REG_KEY, "URL Protocol", "")

            // DefaultIcon
            regAdd("$REG_KEY\\DefaultIcon", null, "\"$exePath\",0")

            // shell\open\command
            regAdd("$REG_KEY\\shell\\open\\command", null, "\"$exePath\" \"%1\"")

            Logger.d(TAG, "Protocol handler registered successfully")
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to register protocol handler: ${e.message}")
        }
    }

    private fun isAlreadyRegistered(currentExePath: String): Boolean {
        return try {
            val result = regQuery("$REG_KEY\\shell\\open\\command", null)
            // Registry stores path with quotes: "C:\path\to\SimpMusic.exe" "%1"
            // Normalize both for comparison
            val normalizedExe = currentExePath.replace("\\", "/").lowercase()
            result?.replace("\\", "/")?.lowercase()?.contains(normalizedExe) == true
        } catch (_: Exception) {
            false
        }
    }

    private fun resolveExePath(): String? {
        // JPackage directory structure:
        //   <app>/runtime/...  (java.home points here)
        //   <app>/SimpMusic.exe
        // So we go: java.home → parent (runtime) → parent (app) → SimpMusic.exe
        val javaHome = System.getProperty("java.home") ?: return null
        val javaHomeDir = java.io.File(javaHome)

        // Try JPackage layout: java.home is <app>/runtime/... or <app>/runtime
        val appDir = if (javaHomeDir.name == "runtime") {
            javaHomeDir.parentFile
        } else {
            // java.home might be deeper, e.g., <app>/runtime/conf/...
            generateSequence(javaHomeDir) { it.parentFile }
                .firstOrNull { it.name == "runtime" }
                ?.parentFile
        }

        if (appDir != null) {
            val exeFile = java.io.File(appDir, "SimpMusic.exe")
            if (exeFile.exists()) {
                return exeFile.absolutePath
            }
        }

        // Fallback: running from IDE/dev environment, use current process
        return ProcessHandle.current().info().command().orElse(null)
    }

    private fun regAdd(key: String, valueName: String?, data: String) {
        // Build command as a single string for cmd.exe to avoid
        // ProcessBuilder double-escaping embedded quotes in data
        val valueFlag = if (valueName != null) "/v \"$valueName\"" else "/ve"
        val escapedData = data.replace("\"", "\\\"")
        val cmdString = "reg add \"$key\" /f $valueFlag /t REG_SZ /d \"$escapedData\""

        val process = ProcessBuilder("cmd.exe", "/c", cmdString)
            .redirectErrorStream(true)
            .start()
        val exitCode = process.waitFor()
        if (exitCode != 0) {
            val output = process.inputStream.bufferedReader().readText()
            Logger.e(TAG, "reg add failed (exit=$exitCode): $output")
        }
    }

    private fun regQuery(key: String, valueName: String?): String? {
        val command = mutableListOf("reg", "query", key)
        if (valueName != null) {
            command.addAll(listOf("/v", valueName))
        } else {
            command.add("/ve")
        }

        val process = ProcessBuilder(command)
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader().readText()
        val exitCode = process.waitFor()
        return if (exitCode == 0) output else null
    }
}
