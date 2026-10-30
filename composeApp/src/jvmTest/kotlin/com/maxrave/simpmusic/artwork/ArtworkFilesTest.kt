package com.maxrave.simpmusic.artwork

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ArtworkFilesTest {
    @Test
    fun youtubeThumbnailsOfTheSameVideoShareOneFile() {
        val maxres = artworkFileName("https://i.ytimg.com/vi/dQw4w9WgXcQ/maxresdefault.jpg")
        val hq = artworkFileName("https://i.ytimg.com/vi/dQw4w9WgXcQ/hqdefault.jpg")
        assertEquals("dQw4w9WgXcQ.jpg", maxres)
        assertEquals(maxres, hq)
    }

    @Test
    fun youtubeMusicSizeSegmentIsNotPartOfTheIdentity() {
        val large = artworkFileName("https://lh3.googleusercontent.com/abc=w544-h544-l90-rj")
        val small = artworkFileName("https://lh3.googleusercontent.com/abc=w120-h120-l90-rj")
        assertNotNull(large)
        assertEquals(large, small)
    }

    @Test
    fun differentImagesGetDifferentFiles() {
        assertNotEquals(
            artworkFileName("https://lh3.googleusercontent.com/abc=w544-h544"),
            artworkFileName("https://lh3.googleusercontent.com/def=w544-h544"),
        )
    }

    @Test
    fun blankUrlHasNoFile() {
        assertNull(artworkFileName(""))
        assertNull(artworkFileName("   "))
    }
}
