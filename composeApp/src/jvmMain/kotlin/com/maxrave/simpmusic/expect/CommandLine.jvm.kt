package com.maxrave.simpmusic.expect

import com.maxrave.logger.Logger
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

actual fun checkYtdlp(): Boolean {
    val processBuilder = ProcessBuilder("yt-dlp", "--help")
    processBuilder.redirectErrorStream()
    val process = processBuilder.start()
    val input = BufferedReader(InputStreamReader(process.inputStream))
    var line: String? = null
    return try {
        while ((input.readLine().also { line = it }) != null) {
            line?.let { Logger.w("Check YTDLP", it) }
        }
        true
    } catch (e: IOException) {
        e.printStackTrace()
        false
    }
}