package com.maxrave.simpmusic.ui.screen.player.content

import kotlin.math.roundToInt

data class PreFoldPlayerLayout(
    val verticalGapDp: Int,
    val maxArtworkHeightDp: Int?,
)

/**
 * Computes the vertical gap above/below the artwork slot and an optional [maxArtworkHeightDp] cap
 * so the pre-fold player section (top bar + top gap + artwork + lyric gap + info/controls) always
 * fits within [viewportHeightDp] minus [bottomInsetDp] on square/foldable displays (e.g. Galaxy Z
 * Fold 8, #2350) while leaving tall phone layouts unconstrained (`maxArtworkHeightDp == null`,
 * `verticalGapDp >= 30`).
 */
fun computePreFoldPlayerLayout(
    viewportHeightDp: Int,
    viewportWidthDp: Int,
    topAppBarHeightDp: Int,
    infoLayoutHeightDp: Int,
    bottomInsetDp: Int = 0,
    horizontalPaddingDp: Int = 40,
    preferredMinGapDp: Int = 30,
    compactMinGapDp: Int = 10,
    minArtworkHeightDp: Int = 140,
    slotAspectRatio: Float = 1f,
): PreFoldPlayerLayout {
    if (viewportHeightDp <= 0 || viewportWidthDp <= 0 || topAppBarHeightDp <= 0 || infoLayoutHeightDp <= 0) {
        return PreFoldPlayerLayout(
            verticalGapDp = preferredMinGapDp,
            maxArtworkHeightDp = null,
        )
    }
    val safeRatio = if (slotAspectRatio.isFinite() && slotAspectRatio > 0f) slotAspectRatio else 1f
    val naturalArtworkWidth = (viewportWidthDp - horizontalPaddingDp).coerceAtLeast(1)
    val naturalArtworkHeight = (naturalArtworkWidth / safeRatio).roundToInt().coerceAtLeast(1)
    val usableViewportHeight = (viewportHeightDp - bottomInsetDp.coerceAtLeast(0)).coerceAtLeast(1)
    val remainingAfterHeaderAndControls = usableViewportHeight - topAppBarHeightDp - infoLayoutHeightDp

    // Tall phone path: full-width artwork + 2 * preferredMinGapDp (60dp) fits comfortably.
    // Match the legacy formula `(h - top - middle - info - minGap) / 2` clamped at preferredMinGapDp
    // when unconstrained.
    val legacyResult = (remainingAfterHeaderAndControls - naturalArtworkHeight - preferredMinGapDp) / 2
    if (remainingAfterHeaderAndControls - naturalArtworkHeight >= preferredMinGapDp * 2) {
        val gap = maxOf(legacyResult, preferredMinGapDp)
        return PreFoldPlayerLayout(
            verticalGapDp = gap,
            maxArtworkHeightDp = null,
        )
    }

    // Compact / foldable path: shrink the two symmetric gaps first (down to compactMinGapDp) before
    // shrinking the artwork itself; if still overflowing, cap artwork height so controls stay visible.
    if (remainingAfterHeaderAndControls - naturalArtworkHeight >= compactMinGapDp * 2) {
        val gap = ((remainingAfterHeaderAndControls - naturalArtworkHeight) / 2).coerceAtLeast(compactMinGapDp)
        return PreFoldPlayerLayout(
            verticalGapDp = gap,
            maxArtworkHeightDp = null,
        )
    }

    val gap = compactMinGapDp
    val cappedArtworkHeight =
        (remainingAfterHeaderAndControls - gap * 2)
            .coerceAtLeast(minArtworkHeightDp)
    return PreFoldPlayerLayout(
        verticalGapDp = gap,
        maxArtworkHeightDp = cappedArtworkHeight,
    )
}
