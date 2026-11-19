package com.maxrave.simpmusic.ui.screen.player.content

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PlayerViewportLayoutTest {
    @Test
    fun tallPhonePreservesUnconstrainedArtworkAndSpaciousPadding() {
        val layout =
            computePreFoldPlayerLayout(
                viewportHeightDp = 915,
                viewportWidthDp = 412,
                topAppBarHeightDp = 76,
                infoLayoutHeightDp = 240,
                bottomInsetDp = 24,
            )

        assertNull(layout.maxArtworkHeightDp)
        assertTrue(layout.verticalGapDp >= 30)
    }

    @Test
    fun foldableUnfoldedPortraitWithNavigationBarCapsArtworkAboveNavInset() {
        val layout =
            computePreFoldPlayerLayout(
                viewportHeightDp = 841,
                viewportWidthDp = 674,
                topAppBarHeightDp = 76,
                infoLayoutHeightDp = 250,
                bottomInsetDp = 48,
            )

        val cappedHeight = assertNotNull(layout.maxArtworkHeightDp)
        assertEquals(10, layout.verticalGapDp)
        assertTrue(76 + 2 * layout.verticalGapDp + cappedHeight + 250 <= 841 - 48)
    }

    @Test
    fun intermediateViewportCompressesGapsBeforeShrinkingArtwork() {
        val layout =
            computePreFoldPlayerLayout(
                viewportHeightDp = 800,
                viewportWidthDp = 480,
                topAppBarHeightDp = 72,
                infoLayoutHeightDp = 240,
            )

        assertNull(layout.maxArtworkHeightDp)
        assertEquals(24, layout.verticalGapDp)
    }

    @Test
    fun widescreenVideoSlotAspectRatioComputesWidthAndHeightCleanly() {
        val unfoldedVideoLayout =
            computePreFoldPlayerLayout(
                viewportHeightDp = 841,
                viewportWidthDp = 674,
                topAppBarHeightDp = 76,
                infoLayoutHeightDp = 250,
                bottomInsetDp = 24,
                slotAspectRatio = 16f / 9f,
            )
        assertNull(unfoldedVideoLayout.maxArtworkHeightDp)
        assertTrue(unfoldedVideoLayout.verticalGapDp >= 30)

        val compactVideoLayout =
            computePreFoldPlayerLayout(
                viewportHeightDp = 500,
                viewportWidthDp = 674,
                topAppBarHeightDp = 76,
                infoLayoutHeightDp = 250,
                bottomInsetDp = 24,
                slotAspectRatio = 16f / 9f,
            )
        val cappedHeight = assertNotNull(compactVideoLayout.maxArtworkHeightDp)
        assertEquals(10, compactVideoLayout.verticalGapDp)
        assertTrue(76 + 2 * compactVideoLayout.verticalGapDp + cappedHeight + 250 <= 500 - 24)
    }

    @Test
    fun unmeasuredZeroInputsReturnSafeDefault() {
        val zeroViewport =
            computePreFoldPlayerLayout(
                viewportHeightDp = 0,
                viewportWidthDp = 0,
                topAppBarHeightDp = 76,
                infoLayoutHeightDp = 250,
            )
        assertNull(zeroViewport.maxArtworkHeightDp)
        assertEquals(30, zeroViewport.verticalGapDp)

        val zeroHeaderAndInfo =
            computePreFoldPlayerLayout(
                viewportHeightDp = 841,
                viewportWidthDp = 674,
                topAppBarHeightDp = 0,
                infoLayoutHeightDp = 0,
            )
        assertNull(zeroHeaderAndInfo.maxArtworkHeightDp)
        assertEquals(30, zeroHeaderAndInfo.verticalGapDp)
    }
}
