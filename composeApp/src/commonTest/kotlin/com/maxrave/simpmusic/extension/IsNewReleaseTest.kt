package com.maxrave.simpmusic.extension

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IsNewReleaseTest {
    private val known = setOf("MPREb_seen")
    private val march = LocalDate(2026, 3, 14)
    private val january = LocalDate(2026, 1, 10)

    @Test
    fun unseenReleaseFromThisYearIsNew() {
        assertTrue(isNewRelease("MPREb_new", "2026", known, march))
    }

    @Test
    fun seenReleaseIsNotAnnouncedAgain() {
        assertFalse(isNewRelease("MPREb_seen", "2026", known, march))
    }

    @Test
    fun unseenOldReleaseIsNotNew() {
        assertFalse(isNewRelease("MPREb_new", "2020", known, march))
    }

    @Test
    fun lastYearsReleaseCountsOnlyInJanuary() {
        assertTrue(isNewRelease("MPREb_new", "2025", known, january))
        assertFalse(isNewRelease("MPREb_new", "2025", known, march))
    }

    @Test
    fun releaseWithoutYearIsNotNew() {
        assertFalse(isNewRelease("MPREb_new", "", known, march))
    }
}
