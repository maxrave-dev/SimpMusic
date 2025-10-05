package org.simpmusic.lyrics.parser

import org.simpmusic.lyrics.domain.Lyrics

fun parseSyncedLyrics(data: String): Lyrics {
    val regex = Regex("\\[(\\d{2}):(\\d{2})\\.(\\d{2})\\](.+)")
    val lines = data.lines()
    val linesLyrics = ArrayList<Lyrics.LyricsX.Line>()
    lines.map { line ->
        val matchResult = regex.matchEntire(line)
        if (matchResult != null) {
            val minutes = matchResult.groupValues[1].toLong()
            val seconds = matchResult.groupValues[2].toLong()
            val milliseconds = matchResult.groupValues[3].toLong()
            val timeInMillis = minutes * 60_000L + seconds * 1000L + milliseconds
            val content = (if (matchResult.groupValues[4] == " ") " ♫" else matchResult.groupValues[4]).removeRange(0, 1)
            linesLyrics.add(
                Lyrics.LyricsX.Line(
                    endTimeMs = "0",
                    startTimeMs = timeInMillis.toString(),
                    syllables = listOf(),
                    words = content,
                ),
            )
        }
    }
    return Lyrics(
        lyrics =
            Lyrics.LyricsX(
                lines = linesLyrics,
                syncType = "LINE_SYNCED",
            ),
    )
}

fun parseUnsyncedLyrics(data: String): Lyrics {
    val lines = data.lines()
    val linesLyrics = ArrayList<Lyrics.LyricsX.Line>()
    lines.map { line ->
        linesLyrics.add(
            Lyrics.LyricsX.Line(
                endTimeMs = "0",
                startTimeMs = "0",
                syllables = listOf(),
                words = line,
            ),
        )
    }
    return Lyrics(
        lyrics =
            Lyrics.LyricsX(
                lines = linesLyrics,
                syncType = "UNSYNCED",
            ),
    )
}