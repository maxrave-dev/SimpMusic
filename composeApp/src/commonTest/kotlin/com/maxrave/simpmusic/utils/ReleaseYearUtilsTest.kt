package com.maxrave.simpmusic.utils

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ReleaseYearUtilsTest {
    @Test
    fun normalizeReleaseYearRejectsNullBlankAndLiteralNullStrings() {
        assertNull(normalizeReleaseYear(null))
        assertNull(normalizeReleaseYear(""))
        assertNull(normalizeReleaseYear("   "))
        assertNull(normalizeReleaseYear("null"))
        assertNull(normalizeReleaseYear("NULL"))
        assertNull(normalizeReleaseYear("  Null  "))
        assertNull(normalizeReleaseYear("0"))
    }

    @Test
    fun normalizeReleaseYearPreservesValidYears() {
        assertEquals("2024", normalizeReleaseYear("2024"))
        assertEquals("1999", normalizeReleaseYear(" 1999 "))
    }

    @Test
    fun formatYearAndCategoryOmitsSeparatorWhenYearMissing() {
        assertEquals(
            "Album",
            formatYearAndCategory("null", "Album") { year, category -> "$year • $category" },
        )
        assertEquals(
            "Album",
            formatYearAndCategory("", "Album") { year, category -> "$year • $category" },
        )
        assertEquals(
            "Album",
            formatYearAndCategory(null, "Album") { year, category -> "$year • $category" },
        )
        assertEquals(
            "2025 • Album",
            formatYearAndCategory("2025", "Album") { year, category -> "$year • $category" },
        )
    }
}
