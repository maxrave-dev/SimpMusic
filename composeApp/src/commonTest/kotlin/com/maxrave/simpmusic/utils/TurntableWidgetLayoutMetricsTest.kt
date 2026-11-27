package com.maxrave.simpmusic.utils

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TurntableWidgetLayoutMetricsTest {
    @Test
    fun spaciousGridPreservesFullControls() {
        val metrics = computeTurntableWidgetMetrics(180f, 180f)

        assertEquals(8, metrics.outerPaddingDp)
        assertEquals(16, metrics.bottomMarginDp)
        assertEquals(34, metrics.sideButtonSizeDp)
        assertEquals(42, metrics.playDiscSizeDp)
        assertEquals(24, metrics.playIconSizeDp)
        assertEquals(162, metrics.totalRequiredWidthDp)
        assertTrue(metrics.totalRequiredHeightDp <= 180)
    }

    @Test
    fun narrowHonorX7AndFiveColumnCellsNeverOverflow() {
        listOf(110f to 110f, 128f to 128f, 140f to 120f).forEach { (width, height) ->
            val metrics = computeTurntableWidgetMetrics(width, height)

            assertTrue(
                metrics.totalRequiredWidthDp <= width.toInt(),
                "Expected width ${metrics.totalRequiredWidthDp} <= ${width.toInt()} for ${width}x$height",
            )
            assertTrue(
                metrics.totalRequiredHeightDp <= height.toInt(),
                "Expected height ${metrics.totalRequiredHeightDp} <= ${height.toInt()} for ${width}x$height",
            )
            assertTrue(metrics.sideButtonSizeDp > 0)
            assertTrue(metrics.playDiscSizeDp > 0)
            assertTrue(metrics.playIconSizeDp > 0)
        }
    }

    @Test
    fun extremeCompactAndNonFiniteInputsRemainSafe() {
        val compact = computeTurntableWidgetMetrics(88f, 90f)
        assertTrue(compact.totalRequiredWidthDp <= 88)
        assertTrue(compact.totalRequiredHeightDp <= 90)
        assertTrue(compact.sideButtonSizeDp >= 16)
        assertTrue(compact.playDiscSizeDp >= 20)
        assertTrue(compact.playIconSizeDp >= 12)

        val nonFinite = computeTurntableWidgetMetrics(Float.NaN, Float.POSITIVE_INFINITY)
        assertEquals(162, nonFinite.totalRequiredWidthDp)
        assertEquals(8, nonFinite.outerPaddingDp)
        assertEquals(42, nonFinite.playDiscSizeDp)

        val nonPositive = computeTurntableWidgetMetrics(0f, -24f)
        assertEquals(162, nonPositive.totalRequiredWidthDp)
        assertTrue(nonPositive.playIconSizeDp > 0)
    }
}
