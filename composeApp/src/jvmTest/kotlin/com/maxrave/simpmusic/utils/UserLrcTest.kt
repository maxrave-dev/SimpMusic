package com.maxrave.simpmusic.utils

import com.maxrave.simpmusic.utils.UserLrcProblem.Kind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UserLrcTest {
    private fun lyrics(text: String) = checkNotNull(checkUserLrc(text).lyrics) { checkUserLrc(text).problems.toString() }

    private fun problems(text: String) = checkUserLrc(text).problems.map { Triple(it.kind, it.line, it.value) }

    @Test
    fun `reads plain lrc into line-synced lyrics and skips id tags`() {
        val result = lyrics("[ti:Song]\n[ar:Artist]\n[00:12.30] First line\n[00:15.80] Second line")
        assertEquals("LINE_SYNCED", result.syncType)
        assertEquals(listOf("12300", "15800"), result.lines!!.map { it.startTimeMs })
        assertEquals(listOf("First line", "Second line"), result.lines!!.map { it.words })
    }

    @Test
    fun `plays a line at every time it carries, in time order`() {
        val result = lyrics("[00:22.40][01:05.20] Chorus\n[00:30.00] Verse")
        assertEquals(listOf("22400", "30000", "65200"), result.lines!!.map { it.startTimeMs })
        assertEquals(listOf("Chorus", "Verse", "Chorus"), result.lines!!.map { it.words })
    }

    @Test
    fun `reads word times into the shape the rich-sync renderer reads`() {
        val line = lyrics("[00:12.30]<00:12.30>First <00:12.80>line<00:14.90>").lines!!.single()
        assertEquals("<00:12.30>First <00:12.80>line", line.words)
        assertEquals("12300", line.startTimeMs)
        assertEquals("14900", line.endTimeMs)
    }

    @Test
    fun `text before the first word time starts at the line time`() {
        val result = lyrics("[00:19.10]Third <00:19.90>line")
        assertEquals("RICH_SYNCED", result.syncType)
        assertEquals("<00:19.10>Third <00:19.90>line", result.lines!!.single().words)
    }

    @Test
    fun `a line without word times stays a line in word-synced lyrics`() {
        val result = lyrics("[00:01.00]<00:01.00>One\n[00:05.00] Two")
        assertEquals("RICH_SYNCED", result.syncType)
        assertEquals("Two", result.lines!![1].words)
    }

    @Test
    fun `accepts short times and writes them in full`() {
        val result = lyrics("[0:05] a\n[00:06.3]<0:06.3>b")
        assertEquals(listOf("5000", "6300"), result.lines!!.map { it.startTimeMs })
        assertEquals("<00:06.30>b", result.lines!![1].words)
    }

    @Test
    fun `offset makes every time sooner and never below zero`() {
        val result = lyrics("[offset:+250]\n[00:00.10] a\n[00:01.00]<00:01.00>b")
        assertEquals(listOf("0", "750"), result.lines!!.map { it.startTimeMs })
        assertEquals("<00:00.75>b", result.lines!![1].words)
        assertEquals("1500", lyrics("[offset:-500]\n[00:01.00] a").lines!!.single().startTimeMs)
    }

    @Test
    fun `an empty timed line is the note the lrc parser writes`() {
        assertEquals("♫", lyrics("[00:26.00]").lines!!.single().words)
    }

    @Test
    fun `a heart in the lyrics is not a word time`() {
        val result = lyrics("[00:01.00] I <3 you")
        assertEquals("LINE_SYNCED", result.syncType)
        assertEquals("I <3 you", result.lines!!.single().words)
    }

    @Test
    fun `windows line endings are read like any other`() {
        assertEquals(listOf("a", "b"), lyrics("[00:01.00] a\r\n[00:02.00] b\r\n").lines!!.map { it.words })
    }

    @Test
    fun `reports each problem with its line and the text at fault`() {
        assertEquals(listOf(Triple(Kind.NO_TIME, 2, "")), problems("[00:01.00] a\nSecond line"))
        assertEquals(listOf(Triple(Kind.NO_TIME, 1, "")), problems("[Chorus]"))
        assertEquals(listOf(Triple(Kind.BAD_TIME, 1, "00:75.10")), problems("[00:75.10] a"))
        assertEquals(listOf(Triple(Kind.BAD_WORD_TIME, 1, "0:1")), problems("[00:01.00]<0:1>a"))
        assertEquals(listOf(Triple(Kind.BAD_WORD_TIME, 1, "100:00.00")), problems("[00:01.00]<100:00.00>a"))
        assertEquals(
            listOf(Triple(Kind.WORD_TIME_BACKWARDS, 1, "00:20.50")),
            problems("[00:21.00]<00:21.00>Fourth <00:20.50>line"),
        )
        assertEquals(listOf(Triple(Kind.REPEATED_WORD_LINE, 1, "")), problems("[00:01.00][00:09.00]<00:01.00>a"))
        assertEquals(listOf(Triple(Kind.BAD_OFFSET, 1, "abc")), problems("[offset:abc]\n[00:01.00] a"))
        assertEquals(listOf(Triple(Kind.NO_TIMED_LINES, 0, "")), problems("[ti:Song]"))
    }

    @Test
    fun `a problem keeps the lyrics from being saved`() {
        assertNull(checkUserLrc("[00:01.00] a\nb").lyrics)
    }

    @Test
    fun `blank text has no lyrics and nothing to report`() {
        val result = checkUserLrc(" \n ")
        assertNull(result.lyrics)
        assertTrue(result.problems.isEmpty())
    }

    @Test
    fun `marks cover the times, the tags and the problems`() {
        val marks = checkUserLrc("[ar:A]\n[00:01.00]<00:01.00>a\nb").marks
        assertEquals(
            listOf(
                UserLrcMark(UserLrcMark.Kind.TAG, 0, 6),
                UserLrcMark(UserLrcMark.Kind.LINE_TIME, 7, 17),
                UserLrcMark(UserLrcMark.Kind.WORD_TIME, 17, 27),
                UserLrcMark(UserLrcMark.Kind.PROBLEM, 29, 30),
            ),
            marks,
        )
    }
}
