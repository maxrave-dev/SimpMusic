package com.maxrave.simpmusic.ui.screen

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class VolumeSliderMathTest {
    @Test
    fun verticalSliderFractionMapsTopMiddleQuarterAndBottom() {
        val height = 96f
        assertEquals(1.0f, verticalSliderFraction(0f, height), absoluteTolerance = 1e-6f)
        assertEquals(0.5f, verticalSliderFraction(48f, height), absoluteTolerance = 1e-6f)
        assertEquals(0.25f, verticalSliderFraction(72f, height), absoluteTolerance = 1e-6f)
        assertEquals(0.0f, verticalSliderFraction(96f, height), absoluteTolerance = 1e-6f)
    }

    @Test
    fun verticalSliderFractionClampsOutOfBoundsPointer() {
        val height = 96f
        assertEquals(1.0f, verticalSliderFraction(-20f, height), absoluteTolerance = 1e-6f)
        assertEquals(0.0f, verticalSliderFraction(140f, height), absoluteTolerance = 1e-6f)
    }

    @Test
    fun verticalSliderFractionHandlesDegenerateAndNonFiniteInputsSafely() {
        assertEquals(0.0f, verticalSliderFraction(20f, 0f), absoluteTolerance = 1e-6f)
        assertEquals(0.0f, verticalSliderFraction(20f, -10f), absoluteTolerance = 1e-6f)

        val nanHeightResult = verticalSliderFraction(20f, Float.NaN)
        assertFalse(nanHeightResult.isNaN())
        assertEquals(0.0f, nanHeightResult, absoluteTolerance = 1e-6f)

        val nanPointerResult = verticalSliderFraction(Float.NaN, 96f)
        assertFalse(nanPointerResult.isNaN())
        assertEquals(0.0f, nanPointerResult, absoluteTolerance = 1e-6f)

        assertEquals(0.0f, verticalSliderFraction(Float.POSITIVE_INFINITY, 96f), absoluteTolerance = 1e-6f)
        assertEquals(0.0f, verticalSliderFraction(20f, Float.POSITIVE_INFINITY), absoluteTolerance = 1e-6f)
    }

    @Test
    fun activeTrackTopOffsetMatchesFractionGeometry() {
        val height = 96f
        assertEquals(96.0f, activeTrackTopOffset(0f, height), absoluteTolerance = 1e-6f)
        assertEquals(48.0f, activeTrackTopOffset(0.5f, height), absoluteTolerance = 1e-6f)
        assertEquals(0.0f, activeTrackTopOffset(1f, height), absoluteTolerance = 1e-6f)
    }

    @Test
    fun activeTrackTopOffsetClampsOutOfRangeAndNonFiniteInputs() {
        val height = 96f
        assertEquals(96.0f, activeTrackTopOffset(-0.5f, height), absoluteTolerance = 1e-6f)
        assertEquals(0.0f, activeTrackTopOffset(1.5f, height), absoluteTolerance = 1e-6f)
        assertEquals(0.0f, activeTrackTopOffset(0.5f, 0f), absoluteTolerance = 1e-6f)
        assertEquals(0.0f, activeTrackTopOffset(0.5f, -10f), absoluteTolerance = 1e-6f)

        val nanHeightResult = activeTrackTopOffset(0.5f, Float.NaN)
        assertFalse(nanHeightResult.isNaN())
        assertEquals(0.0f, nanHeightResult, absoluteTolerance = 1e-6f)

        val nanFractionResult = activeTrackTopOffset(Float.NaN, height)
        assertFalse(nanFractionResult.isNaN())
        assertEquals(height, nanFractionResult, absoluteTolerance = 1e-6f)
    }
}
