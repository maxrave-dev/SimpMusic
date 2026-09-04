package com.maxrave.simpmusic.ui.component.lyrics.accompanist

import com.maxrave.domain.data.model.metadata.Line
import com.maxrave.domain.data.model.metadata.Lyrics
import com.maxrave.simpmusic.viewModel.LyricsProvider
import com.maxrave.simpmusic.viewModel.NowPlayingScreenData
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class LyricsAdapterTest {

    @Test
    fun testWordGapPreservation() {
        val line = Line(
            startTimeMs = "1000",
            endTimeMs = "4000",
            words = "<00:01.00>Hello<00:01.50> <00:03.00>world<00:04.00>",
            syllables = emptyList()
        )
        val lyrics = Lyrics(
            lines = listOf(line),
            syncType = "RICH_SYNCED"
        )
        val lyricsData = NowPlayingScreenData.LyricsData(
            lyrics = lyrics,
            lyricsProvider = LyricsProvider.SIMPMUSIC
        )

        val syncedLyrics = LyricsAdapter.toSyncedLyrics(lyricsData)
        assertNotNull(syncedLyrics)
        assertEquals(1, syncedLyrics.lines.size)

        val mainLine = syncedLyrics.lines[0] as KaraokeLine.MainKaraokeLine
        assertEquals(2, mainLine.syllables.size)

        val syl1 = mainLine.syllables[0]
        val syl2 = mainLine.syllables[1]

        assertEquals("Hello ", syl1.content)
        assertEquals(1000, syl1.start)
        // syl1 end MUST be 1500 (freeze wipe during the 1500-3000ms pause!), NOT dragged to syl2.start (3000)!
        assertEquals(1500, syl1.end)

        assertEquals("world", syl2.content)
        assertEquals(3000, syl2.start)
        assertEquals(4000, syl2.end)
    }

    @Test
    fun testSimpMusicParenthesesAccompanimentAttachment() {
        // SimpMusic / Musixmatch style simultaneous lyrics:
        // Main line and backing line in parentheses overlapping in time
        val line1 = Line(
            startTimeMs = "24954",
            endTimeMs = "29012",
            words = "<00:24.95>To <00:25.50>watch <00:26.00>our <00:27.00>life",
            syllables = emptyList()
        )
        val line2 = Line(
            startTimeMs = "25763",
            endTimeMs = "29012",
            words = "[bg]<00:25.76>To <00:26.50>watch <00:27.00>our <00:27.50>life <00:28.00>together",
            syllables = emptyList()
        )
        val lyrics = Lyrics(
            lines = listOf(line1, line2),
            syncType = "RICH_SYNCED"
        )
        val lyricsData = NowPlayingScreenData.LyricsData(
            lyrics = lyrics,
            lyricsProvider = LyricsProvider.SIMPMUSIC
        )

        val syncedLyrics = LyricsAdapter.toSyncedLyrics(lyricsData)
        assertNotNull(syncedLyrics)
        // Should collapse into 1 MainKaraokeLine with an attached AccompanimentKaraokeLine
        assertEquals(1, syncedLyrics.lines.size)

        val mainLine = syncedLyrics.lines[0] as KaraokeLine.MainKaraokeLine
        val accs = mainLine.accompanimentLines
        assertNotNull(accs)
        assertEquals(1, accs.size)

        val bgLine = accs[0]
        // Parentheses should be cleanly stripped from syllables/text
        val bgText = bgLine.syllables.joinToString("") { it.content }
        assertEquals("To watch our life together", bgText)
        assertEquals(25763, bgLine.start)
        assertEquals(29012, bgLine.end)
    }

    @Test
    fun testExplicitBgLineAttachment() {
        val line1 = Line(
            startTimeMs = "10000",
            endTimeMs = "15000",
            words = "<00:10.00>Main <00:14.00>line",
            syllables = emptyList()
        )
        val line2 = Line(
            startTimeMs = "11000",
            endTimeMs = "13000",
            words = "[bg]<00:11.00>Echo<00:13.00>",
            syllables = emptyList()
        )
        val lyrics = Lyrics(
            lines = listOf(line1, line2),
            syncType = "RICH_SYNCED"
        )
        val lyricsData = NowPlayingScreenData.LyricsData(
            lyrics = lyrics,
            lyricsProvider = LyricsProvider.SIMPMUSIC
        )

        val syncedLyrics = LyricsAdapter.toSyncedLyrics(lyricsData)
        assertNotNull(syncedLyrics)
        assertEquals(1, syncedLyrics.lines.size)

        val mainLine = syncedLyrics.lines[0] as KaraokeLine.MainKaraokeLine
        val accs = mainLine.accompanimentLines
        assertNotNull(accs)
        assertEquals(1, accs.size)

        val bgLine = accs[0]
        val bgText = bgLine.syllables.joinToString("") { it.content }
        assertEquals("Echo", bgText)
    }

    @Test
    fun testIdenticalTranslatedLyricsAreFilteredOut() {
        val origLine = Line(
            startTimeMs = "1000",
            endTimeMs = "4000",
            words = "<00:01.00>Hello<00:02.00> <00:03.00>world<00:04.00>",
            syllables = emptyList()
        )
        val transLine = Line(
            startTimeMs = "1000",
            endTimeMs = "4000",
            words = "Hello world",
            syllables = emptyList()
        )
        val lyrics = Lyrics(
            lines = listOf(origLine),
            syncType = "RICH_SYNCED"
        )
        val transLyrics = Lyrics(
            lines = listOf(transLine),
            syncType = "LINE_SYNCED"
        )
        val lyricsData = NowPlayingScreenData.LyricsData(
            lyrics = lyrics,
            translatedLyrics = transLyrics to LyricsProvider.YOUTUBE,
            lyricsProvider = LyricsProvider.YOUTUBE
        )

        val syncedLyrics = LyricsAdapter.toSyncedLyrics(lyricsData)
        assertNotNull(syncedLyrics)
        val mainLine = syncedLyrics.lines[0] as KaraokeLine.MainKaraokeLine
        // Since translation words are identical to the original line, translation must be null!
        assertEquals(null, mainLine.translation)
        // Since original line is English/Latin, phonetic must also be null!
        assertEquals(null, mainLine.phonetic)
        assertTrue(mainLine.syllables.all { it.phonetic == null })
    }

    @Test
    fun testMixedRomanizedLineGivesSameEnglishWordsBelow() {
        val line = Line(
            startTimeMs = "1000",
            endTimeMs = "4000",
            words = "<00:01.00>사랑 <00:02.00>love",
            syllables = emptyList()
        )
        val lyrics = Lyrics(
            lines = listOf(line),
            syncType = "RICH_SYNCED"
        )
        val lyricsData = NowPlayingScreenData.LyricsData(
            lyrics = lyrics,
            lyricsProvider = LyricsProvider.SIMPMUSIC
        )

        val syncedLyrics = LyricsAdapter.toSyncedLyrics(
            lyricsData,
            enabledLanguages = setOf(com.maxrave.domain.data.model.lyrics.RomanizationLanguage.KOREAN)
        )
        assertNotNull(syncedLyrics)
        val mainLine = syncedLyrics.lines[0] as KaraokeLine.MainKaraokeLine
        assertEquals(2, mainLine.syllables.size)

        val koreanSyl = mainLine.syllables[0]
        val englishSyl = mainLine.syllables[1]

        // Korean syllable should be romanized as main text, with original Hangul as phonetic below
        assertTrue(koreanSyl.content.startsWith("salang") || koreanSyl.content.startsWith("sarang"))
        assertEquals("사랑 ", koreanSyl.phonetic)

        // English syllable in the romanized line should have the same English word below!
        assertEquals("love", englishSyl.content)
        assertEquals("love", englishSyl.phonetic)
    }

    @Test
    fun testPureEnglishLineHasNoPhoneticsEvenWhenLanguagesEnabled() {
        val line = Line(
            startTimeMs = "1000",
            endTimeMs = "4000",
            words = "<00:01.00>Hello <00:02.00>world",
            syllables = emptyList()
        )
        val lyrics = Lyrics(
            lines = listOf(line),
            syncType = "RICH_SYNCED"
        )
        val lyricsData = NowPlayingScreenData.LyricsData(
            lyrics = lyrics,
            lyricsProvider = LyricsProvider.SIMPMUSIC
        )

        val syncedLyrics = LyricsAdapter.toSyncedLyrics(
            lyricsData,
            enabledLanguages = setOf(
                com.maxrave.domain.data.model.lyrics.RomanizationLanguage.KOREAN,
                com.maxrave.domain.data.model.lyrics.RomanizationLanguage.JAPANESE
            )
        )
        assertNotNull(syncedLyrics)
        val mainLine = syncedLyrics.lines[0] as KaraokeLine.MainKaraokeLine

        // When the whole line itself is English, no phonetics should be shown below
        assertEquals(null, mainLine.phonetic)
        assertTrue(mainLine.syllables.all { it.phonetic == null })
    }
}


