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