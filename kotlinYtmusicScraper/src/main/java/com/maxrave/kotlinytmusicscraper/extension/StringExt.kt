package com.maxrave.kotlinytmusicscraper.extension

import java.security.MessageDigest
import kotlin.random.Random

fun String.sha256(): String {
    val digest = MessageDigest.getInstance("SHA-256")
    val hash = digest.digest(toByteArray())
    return hash.fold("", { str, it -> str + "%02x".format(it) })
}

fun randomString(length: Int): String {
    val chars = "0123456789abcdef"
    return (1..length)
        .map { chars[Random.nextInt(chars.length)] }
        .joinToString("")
}

fun String.verifyYouTubePlaylistId(): String = if (startsWith("VL")) this else "VL$this"

fun String.isTwoLetterCode(): Boolean {
    val regex = "^[A-Za-z]{2}$".toRegex()
    return regex.matches(this)
}

fun isValidProxyHost(host: String): Boolean {
    // Regular expression to validate proxy host (without port)
    val proxyHostRegex =
        Regex(
            pattern = "^(?!-)[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*(?<!-)\$",
            options = setOf(RegexOption.IGNORE_CASE),
        )

    // Return true if the host matches the regex or is an IP address
    return proxyHostRegex.matches(host) || isIPAddress(host)
}

private fun isIPAddress(host: String): Boolean {
    // Check if the host is an IPv4 address
    val ipv4Regex =
        Regex(
            pattern = "^([0-9]{1,3}\\.){3}[0-9]{1,3}\$",
        )
    if (ipv4Regex.matches(host)) {
        return host.split('.').all { it.toInt() in 0..255 }
    }

    // Check if the host is an IPv6 address
    val ipv6Regex =
        Regex(
            pattern = "^[0-9a-fA-F:]+$",
        )
    return ipv6Regex.matches(host)
}

fun stripMarkdown(markdown: String): String =
    markdown
        // Remove headings (e.g., # Heading)
        .replace(Regex("""(?m)^#{1,6}\s*"""), "")
        // Remove bold (**text**)
        .replace(Regex("""\*\*(.*?)\*\*"""), "$1")
        // Remove italic (*text*)
        .replace(Regex("""\*(.*?)\*"""), "$1")
        // Remove strikethrough (~~text~~)
        .replace(Regex("""~~(.*?)~~"""), "$1")
        // Remove inline code (`code`)
        .replace(Regex("""`([^`]*)`"""), "$1")
        // Remove images (![alt](url))
        .replace(Regex("""!$\begin:math:display$.*?$\end:math:display$$\begin:math:text$.*?$\end:math:text$"""), "")
        // Remove links ([text](url))
        .replace(Regex("""\[.*?]$\begin:math:text$.*?$\end:math:text$"""), "")
        // Remove horizontal rules (---, ***, ___)
        .replace(Regex("""(?m)^\s*[-*_]{3,}\s*$"""), "\n")
        // Remove unordered list markers (- item, * item)
        .replace(Regex("""(?m)^\s*[-*+]\s+"""), " - ")
        // Remove ordered list markers (1. item, 2. item)
        .replace(Regex("""(?m)^\s*\d+\.\s+"""), " - ")
        // Replace multiple newlines with a single newline
        .replace(Regex("""\n{2,}"""), "\n\n")
        // Trim each line
        .lines()
        .joinToString("\n") { it.trim() }