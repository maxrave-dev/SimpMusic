package com.maxrave.simpmusic.extension

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PlaybackDurationTest {
    @Test
    fun truncatesSubsecondPrecision() {
        assertEquals("00:00", formatPlaybackDuration(0))
        assertEquals("00:00", formatPlaybackDuration(999))
        assertEquals("00:01", formatPlaybackDuration(1_999))
        assertEquals("00:59", formatPlaybackDuration(59_999))
        assertEquals("01:00", formatPlaybackDuration(60_000))
    }

    @Test
    fun preservesTotalMinutesForLongTracksWithoutOverflow() {
        assertEquals("60:00", formatPlaybackDuration(3_600_000))
        assertEquals("100:01", formatPlaybackDuration(6_001_000))
        assertEquals("153722867280912:55", formatPlaybackDuration(Long.MAX_VALUE))
    }

    @Test
    fun rejectsUnknownDurationWhichTheUiLocalizesSeparately() {
        assertFailsWith<IllegalArgumentException> { formatPlaybackDuration(-1) }
    }
}
