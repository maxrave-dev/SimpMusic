package com.maxrave.simpmusic.extension

/**
 * Represents a single word with its timing information for rich sync lyrics
 */
data class WordTiming(
    val text: String,
    val startTimeMs: Long,
)

/**
 * Represents a parsed rich sync line with all word timings
 */
data class ParsedRichSyncLine(
    val words: List<WordTiming>,
    val lineStartTimeMs: Long,
    val lineEndTimeMs: Long,
)

/**
 * Parses rich sync lyrics format into word-level timing data
 *
 * Expected format: <MM:SS.mm> word <MM:SS.mm> word ...
 * Example: <00:16.62> Và <00:16.64> em <00:16.68> nói
 *
 * @param words The rich sync string containing timestamps and words
 * @param lineStartTimeMs The start time of the line as a string
 * @param lineEndTimeMs The end time of the line as a string
 * @return ParsedRichSyncLine if parsing succeeds, null if parsing fails or input is invalid
 */
fun parseRichSyncWords(
    words: String,
    lineStartTimeMs: String,
    lineEndTimeMs: String,
): ParsedRichSyncLine? {
    // Handle edge cases
    if (words.isBlank()) {
        println("[parseRichSyncWords] Input is blank")
        return null
    }

    println("[parseRichSyncWords] Input preview: ${words.take(100)}")

    // Strategy: Find all timestamps first, then extract text between them
    // Regex to match timestamp only: <MM:SS.mm>
    val timestampRegex = Regex("""<(\d{2}):(\d{2})\.(\d{2})>""")

    val wordTimings = mutableListOf<WordTiming>()

    // Find all timestamp matches with their positions
    val timestamps = timestampRegex.findAll(words).toList()

    timestamps.forEachIndexed { index, match ->
        val (minutes, seconds, centiseconds) = match.destructured

        // Convert time to milliseconds
        val timeMs =
            (minutes.toLongOrNull() ?: 0L) * 60000L +
                (seconds.toLongOrNull() ?: 0L) * 1000L +
                (centiseconds.toLongOrNull() ?: 0L) * 10L

        // Extract text after this timestamp until the next timestamp (or end of string)
        val startPos = match.range.last + 1
        val endPos =
            if (index < timestamps.size - 1) {
                timestamps[index + 1].range.first
            } else {
                words.length
            }

        // Get the text between timestamps and trim it
        val textBetween = words.substring(startPos, endPos).trim()

        // Only add if there's actual text (not just whitespace or empty)
        if (textBetween.isNotBlank()) {
            wordTimings.add(WordTiming(text = textBetween, startTimeMs = timeMs))
        }
    }

    // If no valid words were parsed, return null (fallback to LINE_SYNCED)
    if (wordTimings.isEmpty()) {
        println("[parseRichSyncWords] No words matched the regex")
        return null
    }

    println("[parseRichSyncWords] Successfully parsed ${wordTimings.size} words")

    // Parse line timing
    val lineStart = lineStartTimeMs.toLongOrNull() ?: 0L
    val lineEnd = lineEndTimeMs.toLongOrNull() ?: Long.MAX_VALUE

    return ParsedRichSyncLine(
        words = wordTimings,
        lineStartTimeMs = lineStart,
        lineEndTimeMs = lineEnd,
    )
}
