package com.maxrave.simpmusic.ui.screen.library

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals

/**
 * Unit tests for [DownloadSortType] sealed class.
 *
 * Covers:
 * - Serialization round-trip (toKey ↔ fromKey)
 * - Default/fallback behavior
 * - Entries list completeness
 * - Equality semantics
 */
class DownloadSortTypeTest {

    // ─── toKey / fromKey round-trip ─────────────────────────────────

    @Test
    fun titleAsc_roundTrip() {
        val key = DownloadSortType.TitleAsc.toKey()
        assertEquals("title_asc", key)
        assertEquals(DownloadSortType.TitleAsc, DownloadSortType.fromKey(key))
    }

    @Test
    fun titleDesc_roundTrip() {
        val key = DownloadSortType.TitleDesc.toKey()
        assertEquals("title_desc", key)
        assertEquals(DownloadSortType.TitleDesc, DownloadSortType.fromKey(key))
    }

    @Test
    fun dateNewest_roundTrip() {
        val key = DownloadSortType.DateNewest.toKey()
        assertEquals("date_newest", key)
        assertEquals(DownloadSortType.DateNewest, DownloadSortType.fromKey(key))
    }

    @Test
    fun dateOldest_roundTrip() {
        val key = DownloadSortType.DateOldest.toKey()
        assertEquals("date_oldest", key)
        assertEquals(DownloadSortType.DateOldest, DownloadSortType.fromKey(key))
    }

    @Test
    fun artistAsc_roundTrip() {
        val key = DownloadSortType.ArtistAsc.toKey()
        assertEquals("artist_asc", key)
        assertEquals(DownloadSortType.ArtistAsc, DownloadSortType.fromKey(key))
    }

    @Test
    fun artistDesc_roundTrip() {
        val key = DownloadSortType.ArtistDesc.toKey()
        assertEquals("artist_desc", key)
        assertEquals(DownloadSortType.ArtistDesc, DownloadSortType.fromKey(key))
    }

    // ─── All variants round-trip (parameterized-style) ──────────────

    @Test
    fun allVariants_roundTrip() {
        DownloadSortType.entries.forEach { sortType ->
            val key = sortType.toKey()
            val restored = DownloadSortType.fromKey(key)
            assertEquals(
                sortType,
                restored,
                "Round-trip failed for $sortType (key=$key)",
            )
        }
    }

    // ─── fromKey defaults / fallbacks ───────────────────────────────

    @Test
    fun fromKey_null_returnsDefault() {
        val result = DownloadSortType.fromKey(null)
        assertEquals(DownloadSortType.DEFAULT, result)
        assertIs<DownloadSortType.DateNewest>(result)
    }

    @Test
    fun fromKey_emptyString_returnsDefault() {
        assertEquals(DownloadSortType.DEFAULT, DownloadSortType.fromKey(""))
    }

    @Test
    fun fromKey_unknownKey_returnsDefault() {
        assertEquals(DownloadSortType.DEFAULT, DownloadSortType.fromKey("unknown_sort"))
    }

    @Test
    fun fromKey_caseSensitive_unknownReturnsDefault() {
        // Keys should be exact match, not case-insensitive
        assertEquals(DownloadSortType.DEFAULT, DownloadSortType.fromKey("Title_Asc"))
        assertEquals(DownloadSortType.DEFAULT, DownloadSortType.fromKey("TITLE_ASC"))
    }

    // ─── DEFAULT constant ───────────────────────────────────────────

    @Test
    fun default_isDateNewest() {
        assertIs<DownloadSortType.DateNewest>(DownloadSortType.DEFAULT)
    }

    // ─── entries list ───────────────────────────────────────────────

    @Test
    fun entries_containsAllSixVariants() {
        assertEquals(6, DownloadSortType.entries.size)
    }

    @Test
    fun entries_containsEachVariant() {
        val entries = DownloadSortType.entries
        assertIs<DownloadSortType.DateNewest>(entries[0])
        assertIs<DownloadSortType.DateOldest>(entries[1])
        assertIs<DownloadSortType.TitleAsc>(entries[2])
        assertIs<DownloadSortType.TitleDesc>(entries[3])
        assertIs<DownloadSortType.ArtistAsc>(entries[4])
        assertIs<DownloadSortType.ArtistDesc>(entries[5])
    }

    @Test
    fun entries_hasNoDuplicates() {
        val entries = DownloadSortType.entries
        assertEquals(entries.size, entries.toSet().size)
    }

    // ─── toKey uniqueness ───────────────────────────────────────────

    @Test
    fun allKeys_areUnique() {
        val keys = DownloadSortType.entries.map { it.toKey() }
        assertEquals(keys.size, keys.toSet().size, "Duplicate keys found: $keys")
    }

    @Test
    fun allKeys_areNonEmpty() {
        DownloadSortType.entries.forEach { sortType ->
            val key = sortType.toKey()
            assertNotEquals("", key, "Key for $sortType should not be empty")
        }
    }

    // ─── PREFERENCE_KEY ─────────────────────────────────────────────

    @Test
    fun preferenceKey_isNonEmpty() {
        assertNotEquals("", DownloadSortType.PREFERENCE_KEY)
    }

    @Test
    fun preferenceKey_expectedValue() {
        assertEquals("download_sort_type", DownloadSortType.PREFERENCE_KEY)
    }

    // ─── Equality ───────────────────────────────────────────────────

    @Test
    fun sameVariants_areEqual() {
        assertEquals(DownloadSortType.TitleAsc, DownloadSortType.TitleAsc)
        assertEquals(DownloadSortType.ArtistDesc, DownloadSortType.ArtistDesc)
    }

    @Test
    fun differentVariants_areNotEqual() {
        assertNotEquals<DownloadSortType>(DownloadSortType.TitleAsc, DownloadSortType.TitleDesc)
        assertNotEquals<DownloadSortType>(DownloadSortType.DateNewest, DownloadSortType.DateOldest)
        assertNotEquals<DownloadSortType>(DownloadSortType.ArtistAsc, DownloadSortType.ArtistDesc)
        assertNotEquals<DownloadSortType>(DownloadSortType.TitleAsc, DownloadSortType.ArtistAsc)
    }
}
