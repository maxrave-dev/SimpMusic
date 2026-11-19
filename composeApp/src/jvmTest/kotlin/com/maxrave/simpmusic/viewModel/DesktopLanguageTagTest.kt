package com.maxrave.simpmusic.viewModel

import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class DesktopLanguageTagTest {
    private val fallbackLocale: Locale = Locale.forLanguageTag("en-US")

    @Test
    fun emptyOrBlankLanguageFromBackupFallsBackToSystemDefault() {
        assertEquals(fallbackLocale, resolveDesktopLocale("", fallbackLocale))
        assertEquals(fallbackLocale, resolveDesktopLocale("   ", fallbackLocale))
        assertEquals(fallbackLocale, resolveDesktopLocale(null, fallbackLocale))
        assertNotEquals(Locale.ROOT, resolveDesktopLocale("", fallbackLocale))
    }

    @Test
    fun malformedOrSingleCharTagFallsBackToSystemDefault() {
        assertEquals(fallbackLocale, resolveDesktopLocale("1", fallbackLocale))
        assertEquals(fallbackLocale, resolveDesktopLocale("und", fallbackLocale))
        assertEquals(fallbackLocale, resolveDesktopLocale("---", fallbackLocale))
    }

    @Test
    fun commaSeparatedAndroidLocaleListUsesPrimaryLocale() {
        val resolved = resolveDesktopLocale("vi-VN,en-US", fallbackLocale)
        assertEquals("vi", resolved.language)
        assertEquals("VN", resolved.country)
    }

    @Test
    fun chineseSimplifiedAndTraditionalPreserveRegionSubtags() {
        val simplified = resolveDesktopLocale("zh-CN", fallbackLocale)
        assertEquals("zh", simplified.language)
        assertEquals("CN", simplified.country)

        val simplifiedAndroidRes = resolveDesktopLocale("zh-rCN", fallbackLocale)
        assertEquals("zh", simplifiedAndroidRes.language)
        assertEquals("CN", simplifiedAndroidRes.country)

        val traditionalHant = resolveDesktopLocale("zh-Hant-TW", fallbackLocale)
        assertEquals("zh", traditionalHant.language)
        assertEquals("TW", traditionalHant.country)

        val traditionalAndroidRes = resolveDesktopLocale("zh-rTW", fallbackLocale)
        assertEquals("zh", traditionalAndroidRes.language)
        assertEquals("TW", traditionalAndroidRes.country)
    }

    @Test
    fun indonesianAndHebrewTagsResolveValidLanguageAndRegion() {
        val indonesian = resolveDesktopLocale("id-ID", fallbackLocale)
        assertEquals("ID", indonesian.country)
        assertNotEquals("", indonesian.language)

        val hebrew = resolveDesktopLocale("iw-IL", fallbackLocale)
        assertEquals("IL", hebrew.country)
        assertNotEquals("", hebrew.language)
    }
}
