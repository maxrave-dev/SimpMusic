package com.maxrave.simpmusic.utils

import kotlin.math.roundToInt

data class TurntableWidgetMetrics(
    val outerPaddingDp: Int,
    val bottomMarginDp: Int,
    val pillHorizontalPaddingDp: Int,
    val pillVerticalPaddingDp: Int,
    val sideButtonSizeDp: Int,
    val sideIconPaddingDp: Int,
    val playDiscSizeDp: Int,
    val playIconSizeDp: Int,
    val buttonSpacingDp: Int,
) {
    val totalPillWidthDp: Int
        get() = (pillHorizontalPaddingDp * 2) + (sideButtonSizeDp * 2) + playDiscSizeDp + (buttonSpacingDp * 2)

    val totalRequiredWidthDp: Int
        get() = (outerPaddingDp * 2) + totalPillWidthDp

    val totalRequiredHeightDp: Int
        get() = (outerPaddingDp * 2) + bottomMarginDp + (pillVerticalPaddingDp * 2) + playDiscSizeDp
}

fun computeTurntableWidgetMetrics(
    widthDp: Float,
    heightDp: Float,
): TurntableWidgetMetrics {
    val safeWidth = if (widthDp.isFinite() && widthDp > 0f) widthDp else 162f
    val safeHeight = if (heightDp.isFinite() && heightDp > 0f) heightDp else 162f

    val outerPadding =
        when {
            safeWidth < 120f || safeHeight < 120f -> 4
            safeWidth < 150f || safeHeight < 140f -> 6
            else -> 8
        }
    val availableInnerWidth = (safeWidth - outerPadding * 2).coerceAtLeast(72f)
    // Default spacious pill is 146dp wide (10*2 + 34*2 + 42 + 8*2). Scale down smoothly when narrower.
    val widthScale = (availableInnerWidth / 146f).coerceIn(0.5f, 1f)
    val heightScale = ((safeHeight - outerPadding * 2) / 130f).coerceIn(0.5f, 1f)
    val scale = minOf(widthScale, heightScale)

    val pillHorizontalPadding = (10f * scale).roundToInt().coerceAtLeast(4)
    val pillVerticalPadding = (6f * scale).roundToInt().coerceAtLeast(3)
    val buttonSpacing = (8f * scale).roundToInt().coerceAtLeast(2)
    var playDiscSize = (42f * scale).roundToInt().coerceAtLeast(24)
    var sideButtonSize = (34f * scale).roundToInt().coerceAtLeast(18)

    // Guarantee totalPillWidth <= availableInnerWidth.toInt() even after rounding.
    val maxPillWidth = availableInnerWidth.toInt()
    while ((pillHorizontalPadding * 2) + (sideButtonSize * 2) + playDiscSize + (buttonSpacing * 2) > maxPillWidth &&
        (sideButtonSize > 16 || playDiscSize > 20)
    ) {
        if (sideButtonSize > 16) sideButtonSize--
        if ((pillHorizontalPadding * 2) + (sideButtonSize * 2) + playDiscSize + (buttonSpacing * 2) > maxPillWidth && playDiscSize > 20) {
            playDiscSize--
        }
    }

    val sideIconPadding = (5f * scale).roundToInt().coerceIn(2, 5)
    val playIconSize = (playDiscSize * 0.57f).roundToInt().coerceIn(12, 24)
    val bottomMargin =
        when {
            safeHeight < 115f -> 6
            safeHeight < 145f -> 10
            else -> 16
        }

    return TurntableWidgetMetrics(
        outerPaddingDp = outerPadding,
        bottomMarginDp = bottomMargin,
        pillHorizontalPaddingDp = pillHorizontalPadding,
        pillVerticalPaddingDp = pillVerticalPadding,
        sideButtonSizeDp = sideButtonSize,
        sideIconPaddingDp = sideIconPadding,
        playDiscSizeDp = playDiscSize,
        playIconSizeDp = playIconSize,
        buttonSpacingDp = buttonSpacing,
    )
}
