package com.maxrave.simpmusic.utils

import com.maxrave.domain.data.model.metadata.Line
import com.maxrave.domain.data.model.metadata.Lyrics

/**
 * What [checkUserLrc] made of lyrics typed into Add lyrics.
 *
 * @property lyrics the lyrics in the shape the providers return, or null while there are [problems]
 *   or nothing timed yet.
 * @property problems what stops the lyrics from being saved, in line order.
 * @property marks the ranges of the text to colour in the input: every time, every tag line and
 *   every problem.
 */
data class UserLrcCheck(
    val lyrics: Lyrics?,
    val problems: List<UserLrcProblem>,
    val marks: List<UserLrcMark>,
)

/** [line] is 1-based, 0 when the problem is not about one line; [value] is the text at fault. */
data class UserLrcProblem(
    val kind: Kind,
    val line: Int = 0,
    val value: String = "",
) {
    enum class Kind {
        NO_TIME,
        BAD_TIME,
        BAD_WORD_TIME,
        WORD_TIME_BACKWARDS,
        REPEATED_WORD_LINE,
        BAD_OFFSET,
        NO_TIMED_LINES,
    }
}

/** [start] inclusive and [end] exclusive, both offsets into the checked text. */
data class UserLrcMark(
    val kind: Kind,
    val start: Int,
    val end: Int,
) {
    enum class Kind { LINE_TIME, WORD_TIME, TAG, PROBLEM }
}

// `[mm:ss]`, `[mm:ss.x]`, `[mm:ss.xx]` or `[mm:ss.xxx]`.
private val LINE_TIME = Regex("""\[(\d{1,3}):(\d{2})(?:\.(\d{1,3}))?\]""")

// Two-digit minutes at most: the rich-sync renderer reads nothing longer.
private val WORD_TIME = Regex("""<(\d{1,2}):(\d{2})(?:\.(\d{1,3}))?>""")

// A bracket that opens on a digit and holds only digits, colons and dots is meant as a time, so a
// malformed one is reported instead of being shown as lyrics. `I <3 you` is not one.
private val LINE_TIME_LIKE = Regex("""\[\d[\d:.]*\]""")
private val WORD_TIME_LIKE = Regex("""<\d[\d:.]*>""")

// LRC's ID tags, `[ar:Artist]`, `[ti:Title]`, `[offset:+250]`: a key that starts with a letter or `#`.
private val ID_TAG = Regex("""\[([A-Za-z#][^:\]]*):(.*)\]""")

/**
 * Reads plain LRC (`[01:23.45] line`) and enhanced LRC (`<01:23.45>` before each word) into the
 * [Lyrics] the providers return, so they are cached, shown and contributed like any other.
 *
 * The output follows the existing parsers, which is what lets a contributed copy read back the
 * same: line text loses its leading space, an empty line is `♫` (`parseSyncedLyrics`), and word
 * times are written `<mm:ss.cc>` (`parseRichSyncWords`). Text before a line's first word time
 * starts at the line's time, a word time with nothing after it closes the line, and `[offset:+n]`
 * makes every time n ms sooner, as LRC defines it. Lines are sorted by time, so a line carrying
 * several times plays at each of them.
 */
fun checkUserLrc(text: String): UserLrcCheck {
    val problems = mutableListOf<UserLrcProblem>()
    val marks = mutableListOf<UserLrcMark>()
    val timed = mutableListOf<TimedLine>()
    var offsetMs = 0L
    var lineStart = 0
    text.split('\n').forEachIndexed { index, raw ->
        val lineNumber = index + 1
        val lead = raw.indexOfFirst { !it.isWhitespace() }
        val base = lineStart + lead
        lineStart += raw.length + 1
        if (lead < 0) return@forEachIndexed
        val body = raw.substring(lead).trimEnd()

        fun problem(
            kind: UserLrcProblem.Kind,
            value: String = "",
            start: Int = base,
            end: Int = base + body.length,
        ) {
            problems += UserLrcProblem(kind, lineNumber, value)
            marks += UserLrcMark(UserLrcMark.Kind.PROBLEM, start, end)
        }

        ID_TAG.matchEntire(body)?.let { tag ->
            val (key, value) = tag.destructured
            if (key.equals("offset", ignoreCase = true)) {
                val parsed = value.trim().toLongOrNull() ?: return@forEachIndexed problem(UserLrcProblem.Kind.BAD_OFFSET, value.trim())
                offsetMs = parsed
            }
            marks += UserLrcMark(UserLrcMark.Kind.TAG, base, base + body.length)
            return@forEachIndexed
        }

        val times = mutableListOf<Long>()
        var badTime = false
        var pos = 0
        while (true) {
            // Spaces between two times belong to neither; before the lyric they stay its text.
            var probe = pos
            while (probe < body.length && body[probe] == ' ' && times.isNotEmpty()) probe++
            val match = LINE_TIME_LIKE.find(body, probe)?.takeIf { it.range.first == probe } ?: break
            val start = base + match.range.first
            val end = base + match.range.last + 1
            val ms = LINE_TIME.matchEntire(match.value)?.toMs()
            if (ms == null) {
                problem(UserLrcProblem.Kind.BAD_TIME, match.value.removeSurrounding("[", "]"), start, end)
                badTime = true
            } else {
                times += ms
                marks += UserLrcMark(UserLrcMark.Kind.LINE_TIME, start, end)
            }
            pos = match.range.last + 1
        }
        if (badTime) return@forEachIndexed
        if (times.isEmpty()) return@forEachIndexed problem(UserLrcProblem.Kind.NO_TIME)

        val rest = body.substring(pos)
        val restBase = base + pos
        val wordTimes = WORD_TIME_LIKE.findAll(rest).toList()
        if (wordTimes.isEmpty()) {
            val words = rest.trimStart().ifBlank { "♫" }
            times.forEach { timed += TimedLine(it, words, null, null) }
            return@forEachIndexed
        }
        // Word times are absolute, so the same words cannot be timed at two places in the song.
        if (times.size > 1) return@forEachIndexed problem(UserLrcProblem.Kind.REPEATED_WORD_LINE)

        val lineMs = times.single()
        val leading = rest.substring(0, wordTimes.first().range.first)
        val segments = mutableListOf<Pair<Long, String>>()
        var lastMs = Long.MIN_VALUE
        if (leading.isNotBlank()) {
            segments += lineMs to leading.trimStart()
            lastMs = lineMs
        }
        var endMs: Long? = null
        var lineOk = true
        wordTimes.forEachIndexed { i, match ->
            val start = restBase + match.range.first
            val end = restBase + match.range.last + 1
            val ms = WORD_TIME.matchEntire(match.value)?.toMs()
            val kind =
                when {
                    ms == null -> UserLrcProblem.Kind.BAD_WORD_TIME
                    ms < lastMs -> UserLrcProblem.Kind.WORD_TIME_BACKWARDS
                    else -> null
                }
            if (ms == null || kind != null) {
                // One problem per line is enough to fix it; the rest of the line stays coloured.
                if (lineOk) problems += UserLrcProblem(kind ?: UserLrcProblem.Kind.BAD_WORD_TIME, lineNumber, match.value.removeSurrounding("<", ">"))
                marks += UserLrcMark(UserLrcMark.Kind.PROBLEM, start, end)
                lineOk = false
                return@forEachIndexed
            }
            marks += UserLrcMark(UserLrcMark.Kind.WORD_TIME, start, end)
            lastMs = ms
            val word = rest.substring(match.range.last + 1, wordTimes.getOrNull(i + 1)?.range?.first ?: rest.length)
            when {
                word.isNotBlank() -> segments += ms to word
                i == wordTimes.lastIndex -> endMs = ms
            }
        }
        if (lineOk) timed += TimedLine(lineMs, "", segments, endMs)
    }

    if (problems.isEmpty() && timed.isEmpty() && text.isNotBlank()) {
        problems += UserLrcProblem(UserLrcProblem.Kind.NO_TIMED_LINES)
    }
    val lyrics =
        if (problems.isEmpty() && timed.isNotEmpty()) {
            fun shift(ms: Long) = (ms - offsetMs).coerceAtLeast(0L)
            Lyrics(
                lines =
                    timed.sortedBy { it.startMs }.map { line ->
                        Line(
                            endTimeMs = line.endMs?.let { shift(it).toString() } ?: "0",
                            startTimeMs = shift(line.startMs).toString(),
                            syllables = null,
                            words =
                                line.words
                                    ?.joinToString("") { (ms, word) -> wordTime(shift(ms)) + word }
                                    ?.ifBlank { "♫" }
                                    ?: line.text,
                        )
                    },
                syncType = if (timed.any { it.words != null }) "RICH_SYNCED" else "LINE_SYNCED",
            )
        } else {
            null
        }
    return UserLrcCheck(lyrics, problems, marks)
}

/** [words] pairs each word time with the text it starts; null for a line timed only as a whole. */
private class TimedLine(
    val startMs: Long,
    val text: String,
    val words: List<Pair<Long, String>>?,
    val endMs: Long?,
)

private fun MatchResult.toMs(): Long? {
    val (minutes, seconds, fraction) = destructured
    val secondsValue = seconds.toLong()
    if (secondsValue >= 60) return null
    val fractionMs =
        when (fraction.length) {
            0 -> 0L
            1 -> fraction.toLong() * 100
            2 -> fraction.toLong() * 10
            else -> fraction.toLong()
        }
    return minutes.toLong() * 60_000 + secondsValue * 1_000 + fractionMs
}

private fun wordTime(ms: Long): String {
    val centis = ms / 10
    return "<${(centis / 6000).pad()}:${(centis / 100 % 60).pad()}.${(centis % 100).pad()}>"
}

private fun Long.pad() = toString().padStart(2, '0')
