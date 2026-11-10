package com.maxrave.simpmusic.ui.screen.player.content

import kotlin.test.Test
import kotlin.test.assertEquals

class SoundCloudCommentTimestampTest {
    @Test
    fun formatsMinuteAndHourBoundaries() {
        listOf(
            0L to "00:00",
            999L to "00:00",
            59_999L to "00:59",
            60_000L to "01:00",
            83_000L to "01:23",
            3_599_999L to "59:59",
            3_600_000L to "1:00:00",
            3_661_000L to "1:01:01",
        ).forEach { (position, expected) ->
            assertEquals(expected, soundCloudCommentTimestamp(position))
        }
    }

    @Test
    fun unknownNegativePositionStartsAtZero() {
        assertEquals("00:00", soundCloudCommentTimestamp(-1L))
        assertEquals("00:00", soundCloudCommentTimestamp(Long.MIN_VALUE))
    }
}
