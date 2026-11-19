package com.maxrave.simpmusic.utils

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VivoSpotifyVersionCompatTest {
    @Test
    fun compatVersionCodeExceedsLegacyVivoCodeAndIncrementsMonotonically() {
        val code63 = VivoSpotifyVersionCompat.compatVersionCode(63)
        val code64 = VivoSpotifyVersionCompat.compatVersionCode(64)

        assertEquals(120_000_063, code63)
        assertTrue(code63 > 63)
        assertTrue(code64 > code63)
    }

    @Test
    fun compatVersionNameProducesModernFourSegmentSpotifyVersion() {
        val versionName = VivoSpotifyVersionCompat.compatVersionName(63)
        assertEquals("9.1.28.63", versionName)

        val segments = versionName.split('.').map { it.toInt() }
        assertEquals(4, segments.size)
        assertTrue(segments.first() >= 9)
    }

    @Test
    fun appIdentityVersionNamePreservesSimpMusicRealVersion() {
        assertEquals(
            "2.6.0",
            VivoSpotifyVersionCompat.appIdentityVersionName("2.6.0", isDebug = false),
        )
        assertEquals(
            "2.6.0-dev",
            VivoSpotifyVersionCompat.appIdentityVersionName("2.6.0", isDebug = true),
        )
        assertEquals(
            "2.6.0-dev",
            VivoSpotifyVersionCompat.appIdentityVersionName("2.6.0-dev", isDebug = true),
        )
    }
}
