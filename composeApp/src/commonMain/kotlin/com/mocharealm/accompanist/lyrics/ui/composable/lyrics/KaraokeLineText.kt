package com.mocharealm.accompanist.lyrics.ui.composable.lyrics

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeAlignment
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import com.mocharealm.accompanist.lyrics.ui.utils.LayerPaint
import com.mocharealm.accompanist.lyrics.ui.utils.easing.Bounce
import com.mocharealm.accompanist.lyrics.ui.utils.easing.DipAndRise
import com.mocharealm.accompanist.lyrics.ui.utils.easing.Swell
import com.mocharealm.accompanist.lyrics.ui.utils.isPunctuation
import com.mocharealm.accompanist.lyrics.ui.utils.isRtl
import kotlin.math.roundToInt
import kotlin.math.pow

private const val MaxSimpleFloatOffsetY = 4f
private val SimpleFloatEasing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1.0f)      // CSS ease
private val EaseInOutEasing = CubicBezierEasing(0.42f, 0.0f, 0.58f, 1.0f)         // CSS ease-in-out

/** Zero-allocation 4-stop piecewise keyframe interpolation for grow-dynamic */
private inline fun interpGrow(
    progress: Float,
    v0: Float,
    v30: Float,
    v75: Float,
    v100: Float,
    easing: CubicBezierEasing = EaseInOutEasing
): Float {
    return when {
        progress <= 0f -> v0
        progress < 0.30f -> v0 + (v30 - v0) * easing.transform(progress / 0.30f)
        progress < 0.75f -> v30 + (v75 - v30) * easing.transform((progress - 0.30f) / 0.45f)
        progress < 1.0f -> v75 + (v100 - v75) * easing.transform((progress - 0.75f) / 0.25f)
        else -> v100
    }
}

/** Zero-allocation 3-stop piecewise keyframe interpolation for rise-char */
private inline fun interpRise(
    progress: Float,
    v0: Float,
    v55: Float,
    v100: Float,
    easing: CubicBezierEasing = EaseInOutEasing
): Float {
    return when {
        progress <= 0f -> v0
        progress < 0.55f -> v0 + (v55 - v0) * easing.transform(progress / 0.55f)
        progress < 1.0f -> v55 + (v100 - v55) * easing.transform((progress - 0.55f) / 0.45f)
        else -> v100
    }
}

/** Zero-allocation 2-stop keyframe interpolation for drag-char */
private inline fun interpDrag(
    progress: Float,
    v0: Float,
    v100: Float,
    easing: CubicBezierEasing = SimpleFloatEasing
): Float {
    return when {
        progress <= 0f -> v0
        progress >= 1f -> v100
        else -> v0 + (v100 - v0) * easing.transform(progress)
    }
}

/**
 * Creates a horizontal gradient brush that represents the karaoke progress.
 * The gradient moves from inactive color to active color based on the current time.
 *
 * @param lineLayout The layout information for syllables in the line.
 * @param currentTimeMs The current playback time in milliseconds.
 * @param isRtl Whether the layout direction is Right-to-Left.
 */
private fun createLineGradientBrush(
    rowData: RowRenderData,
    currentTimeMs: Int,
    isRtl: Boolean
): Brush {
    val activeColor = Color.White
    val inactiveColor = Color.White.copy(alpha = 0.2f)
    // 20f creates a very sharp but smooth gradient wipe matching Apple Music's style,
    // rather than the original 100f which was too wide and looked like a sliding block.
    val minFadeWidth = 20f

    val lineLayout = rowData.rowLayouts
    val totalMinX = rowData.totalMinX
    val totalMaxX = rowData.totalMaxX
    val totalWidth = rowData.totalWidth

    if (totalWidth <= 0f) {
        val isFinished = currentTimeMs >= lineLayout.last().syllable.end
        val color = if (isFinished) activeColor else inactiveColor
        return SolidColor(color)
    }

    val firstSyllableStart = rowData.firstSyllableStart
    val lastSyllableEnd = rowData.lastSyllableEnd

    val lineProgress = run {
        if (currentTimeMs <= firstSyllableStart) return Brush.horizontalGradient(
            listOf(inactiveColor, inactiveColor)
        )
        if (currentTimeMs >= lastSyllableEnd) return Brush.horizontalGradient(
            listOf(activeColor, activeColor)
        )

        val activeSyllableLayout = lineLayout.find {
            currentTimeMs in it.syllable.start until it.syllable.end
        }

        val currentPixelPosition = when {
            activeSyllableLayout != null -> {
                val syllableProgress = activeSyllableLayout.syllable.progress(currentTimeMs)
                if (isRtl) {
                    activeSyllableLayout.position.x + activeSyllableLayout.width * (1f - syllableProgress)
                } else {
                    activeSyllableLayout.position.x + activeSyllableLayout.width * syllableProgress
                }
            }

            else -> {
                val lastFinished = lineLayout.lastOrNull { currentTimeMs >= it.syllable.end }
                if (isRtl) {
                    lastFinished?.position?.x ?: totalMaxX
                } else {
                    lastFinished?.let { it.position.x + it.width } ?: totalMinX
                }
            }
        }
        ((currentPixelPosition - totalMinX) / totalWidth).coerceIn(0f, 1f)
    }

    val fadeRange = (minFadeWidth / totalWidth).coerceAtMost(1f)
    val fadeCenterStart = -fadeRange / 2f
    val fadeCenterEnd = 1f + fadeRange / 2f
    val fadeCenter = fadeCenterStart + (fadeCenterEnd - fadeCenterStart) * lineProgress
    val fadeStart = (fadeCenter - fadeRange / 2f).coerceIn(0f, 1f)
    val fadeMid = fadeCenter.coerceIn(0f, 1f)
    val fadeEnd = (fadeCenter + fadeRange / 2f).coerceIn(0f, 1f)

    val midColor = Color(
        red = activeColor.red * 0.7f + inactiveColor.red * 0.3f,
        green = activeColor.green * 0.7f + inactiveColor.green * 0.3f,
        blue = activeColor.blue * 0.7f + inactiveColor.blue * 0.3f,
        alpha = activeColor.alpha * 0.7f + inactiveColor.alpha * 0.3f
    )

    val colorStops = if (isRtl) {
        arrayOf(
            0.0f to inactiveColor,
            fadeStart to inactiveColor,
            fadeMid to midColor,
            fadeEnd to activeColor,
            1.0f to activeColor
        )
    } else {
        arrayOf(
            0.0f to activeColor,
            fadeStart to activeColor,
            fadeMid to midColor,
            fadeEnd to inactiveColor,
            1.0f to inactiveColor
        )
    }

    return Brush.horizontalGradient(
        colorStops = colorStops,
        startX = totalMinX,
        endX = totalMaxX
    )
}

/**
 * Draws a multi-row lyrics line into the canvas.
 * Handles row wrapping, padding, and applying the karaoke progress gradient.
 *
 * @param lineLayouts The pre-calculated layout of syllables, organized by rows.
 * @param currentTimeMs The current playback time in milliseconds.
 * @param color The base text color.
 * @param blendMode The blend mode to use for drawing.
 * @param isRtl Whether the layout direction is Right-to-Left.
 * @param showDebugRectangles Whether to draw debug outlines around glyphs.
 */
fun DrawScope.drawLyricsLine(
    rowRenderData: List<RowRenderData>,
    currentTimeMs: Int,
    color: Color,
    blendMode: BlendMode,
    isRtl: Boolean,
    showDebugRectangles: Boolean = false,
    showPhonetic: Boolean = true
) {
    rowRenderData.forEach { rowData ->
        val rowLayouts = rowData.rowLayouts
        val lastSyllableEnd = rowData.lastSyllableEnd

        if (currentTimeMs >= lastSyllableEnd) {
            drawRowText(
                rowLayouts,
                color,
                blendMode,
                showDebugRectangles,
                currentTimeMs,
                showPhonetic
            )
            return@forEach
        }

        drawIntoCanvas { canvas ->
            val layerBounds = rowData.layerBounds
            canvas.saveLayer(layerBounds, LayerPaint)

            drawRowText(
                rowLayouts,
                color,
                blendMode,
                showDebugRectangles,
                currentTimeMs,
                showPhonetic
            )

            val progressBrush = createLineGradientBrush(rowData, currentTimeMs, isRtl)
            drawRect(
                brush = progressBrush,
                topLeft = layerBounds.topLeft,
                size = layerBounds.size,
                blendMode = BlendMode.DstIn
            )
            canvas.restore()
        }
    }
}

/**
 * Draws text for a single row, handling word and character animations.
 *
 * @param rowLayouts The layouts for syllables in this row.
 * @param drawColor The color to draw the text with.
 * @param blendMode The blend mode to use.
 * @param showDebugRectangles Whether to show debug bounds.
 * @param currentTimeMs Current playback time.
 * @param showPhonetic Whether to show syllable-level phonetics.
 */
private fun DrawScope.drawRowText(
    rowLayouts: List<SyllableLayout>,
    drawColor: Color,
    blendMode: BlendMode,
    showDebugRectangles: Boolean,
    currentTimeMs: Int,
    showPhonetic: Boolean = true
) {
    rowLayouts.forEachIndexed { index, syllableLayout ->
        val wordAnimInfo = syllableLayout.wordAnimInfo
        val phoneticDrawColor = drawColor
        val amMode = syllableLayout.amMode

        // Resolve punctuation driver — punctuation borrows timing from the preceding real word
        val driverLayout = if (syllableLayout.syllable.content.trim().isPunctuation()) {
            var searchIndex = index - 1
            while (searchIndex >= 0) {
                val candidate = rowLayouts[searchIndex]
                if (!candidate.syllable.content.trim().isPunctuation()) break
                searchIndex--
            }
            if (searchIndex < 0) syllableLayout else rowLayouts[searchIndex]
        } else {
            syllableLayout
        }

        when (amMode) {
            // ── Mode A: GROW_DYNAMIC ─────────────────────────────────
            // Per-character stagger with dynamic scale + glow + translateY + horizontal expansion
            AppleMusicMode.GROW_DYNAMIC -> {
                if (wordAnimInfo != null) {
                    val charLayouts = syllableLayout.charLayouts ?: emptyList()
                    val charBounds = syllableLayout.charOriginalBounds ?: emptyList()
                    val numCharsInWord = wordAnimInfo.wordContent.length
                    val wordDurationMs = wordAnimInfo.wordDuration.toFloat()

                    // AmLyrics.ts: growDurationMs = finalDuration * 1.5
                    val growDuration = wordDurationMs * 1.5f
                    // AmLyrics.ts: baseDelayPerChar = finalDuration * 0.09
                    val baseDelayPerChar = wordDurationMs * 0.09f

                    // Dynamic amplitude parameters from AmLyrics.ts (+15% sensitivity boost)
                    val durationProgress = ((wordDurationMs - 350f) / (2800f - 350f)).coerceIn(0f, 1f).pow(2.7f)
                    val isLongWord = numCharsInWord > 5
                    val isShortDuration = wordDurationMs < 1200f
                    var decayStrength = 0f
                    if (isLongWord) {
                        decayStrength += ((numCharsInWord - 5) / 5f).coerceAtMost(1f) * 0.4f
                    }
                    if (isShortDuration && numCharsInWord > 3) {
                        decayStrength += (1f - (wordDurationMs - 800f) / 400f).coerceIn(0f, 1f) * 0.3f
                    } else if (isShortDuration && numCharsInWord <= 3) {
                        decayStrength += (1f - (wordDurationMs - 800f) / 400f).coerceIn(0f, 1f) * 0.1f
                    }
                    val maxDecayRate = decayStrength.coerceAtMost(0.7f)
                    val baseGrowth = (if (numCharsInWord <= 3) 0.05f else 0.04f) * 1.15f
                    val glowDurFactor = (wordDurationMs / 1500f).coerceAtMost(1.1f)
                    val glowLenFactor = if (numCharsInWord <= 3) 0.85f else if (numCharsInWord >= 6) 1.1f else 1.0f
                    val effectiveDuration = (wordDurationMs + (syllableLayout.syllable.end - syllableLayout.syllable.start) * 2f) / 3f
                    val peakMultiplier = (effectiveDuration / 2000f).coerceIn(0.3f, 1.0f)

                    syllableLayout.syllable.content.forEachIndexed { charIndex, _ ->
                        val charLayout = charLayouts.getOrNull(charIndex) ?: return@forEachIndexed
                        val charBox = charBounds.getOrNull(charIndex) ?: return@forEachIndexed

                        val absoluteCharIndex = syllableLayout.charOffsetInWord + charIndex
                        val charDelay = baseDelayPerChar * absoluteCharIndex
                        val charElapsed = currentTimeMs - wordAnimInfo.wordStartTime - charDelay
                        val progress = (charElapsed / growDuration).coerceIn(0f, 1f)

                        // Per-letter dynamic amplitude calculation (+15% boosted)
                        val positionInWord = if (numCharsInWord > 1) absoluteCharIndex.toFloat() / (numCharsInWord - 1) else 0f
                        val charProgress = durationProgress * (1f - positionInWord * maxDecayRate)
                        val charMaxScale = 1.0f + baseGrowth + (charProgress * 0.092f)
                        val normalizedGrowth = (charMaxScale - 1.0f) / 0.1f
                        val translateYPeak = -normalizedGrowth * (2.3f * peakMultiplier)
                        val charRiseY = -1.29f

                        val charPos = (absoluteCharIndex + 0.5f) / numCharsInWord
                        val horizontalOffset = (charPos - 0.5f) * 2f * ((charMaxScale - 1.0f) * 28.75f)
                        val shadowIntensity = (0.28f + charProgress * 0.385f) * glowDurFactor * glowLenFactor

                        // Zero-allocation dynamic keyframe interpolation
                        val translateYPx = interpGrow(progress, 0f, translateYPeak, charRiseY, charRiseY, EaseInOutEasing).dp.toPx()
                        val scale = interpGrow(progress, 1f, charMaxScale, 1f, 1f, EaseInOutEasing)
                        val glowAlpha = interpGrow(progress, 0f, shadowIntensity, 0f, 0f, EaseInOutEasing)
                        val glowBlurPx = 5.dp.toPx()
                        val offsetXPx = interpGrow(progress, 0f, horizontalOffset, 0f, 0f, EaseInOutEasing).dp.toPx()

                        val centeredOffsetX = (charBox.width - charLayout.size.width) / 2f
                        val xPos = syllableLayout.position.x + charBox.left + centeredOffsetX + offsetXPx
                        val yPos = syllableLayout.position.y + charBox.top + translateYPx

                        val shadow = if (glowAlpha > 0f) Shadow(
                            color = drawColor.copy(alpha = (glowAlpha * 0.5f).coerceIn(0f, 0.66f)),
                            offset = Offset.Zero,
                            blurRadius = glowBlurPx
                        ) else null

                        val charCenterX = xPos + charLayout.size.width / 2f
                        val charCenterY = yPos + charLayout.size.height / 2f
                        val pivot = Offset(charCenterX, charCenterY)

                        withTransform({ scale(scale = scale, pivot = pivot) }) {
                            drawText(
                                textLayoutResult = charLayout,
                                color = drawColor,
                                topLeft = Offset(xPos, yPos),
                                shadow = shadow,
                            )
                        }

                        if (showDebugRectangles) {
                            drawRect(
                                color = Color.Red, topLeft = Offset(xPos, yPos), size = Size(
                                    charLayout.size.width.toFloat(),
                                    charLayout.size.height.toFloat()
                                ), style = Stroke(1f)
                            )
                        }
                    }

                    // Phonetics for GROW_DYNAMIC
                    if (showPhonetic) {
                        syllableLayout.phoneticLayoutResult?.let { phoneticLayout ->
                            val syllableMidIndex =
                                syllableLayout.charOffsetInWord + (syllableLayout.syllable.content.length - 1) / 2f
                            val midDelay = baseDelayPerChar * syllableMidIndex
                            val midElapsed = currentTimeMs - wordAnimInfo.wordStartTime - midDelay
                            val midProgress = (midElapsed / growDuration).coerceIn(0f, 1f)

                            val midPositionInWord = if (numCharsInWord > 1) syllableMidIndex / (numCharsInWord - 1) else 0f
                            val midCharProgress = durationProgress * (1f - midPositionInWord * maxDecayRate)
                            val midScale = 1.0f + baseGrowth + (midCharProgress * 0.08f)
                            val midNormGrowth = (midScale - 1.0f) / 0.1f
                            val midTranslateYPeak = -midNormGrowth * (2f * peakMultiplier)

                            val phoneticTranslateY = interpGrow(midProgress, 0f, midTranslateYPeak, -1.12f, -1.12f, EaseInOutEasing).dp.toPx()
                            val phoneticScale = interpGrow(midProgress, 1f, midScale, 1f, 1f, EaseInOutEasing)

                            val phoneticX = syllableLayout.position.x
                            val phoneticY =
                                syllableLayout.position.y + syllableLayout.textLayoutResult.size.height + 2.dp.toPx() + phoneticTranslateY

                            val midAlphaBoost = 1.0f + (midCharProgress * 0.4f)
                            val phoneticColor = drawColor.copy(alpha = (drawColor.alpha * midAlphaBoost).coerceIn(0f, 1f))

                            withTransform({ scale(scale = phoneticScale, pivot = syllableLayout.wordPivot) }) {
                                drawText(
                                    textLayoutResult = phoneticLayout,
                                    color = phoneticColor,
                                    topLeft = Offset(phoneticX, phoneticY)
                                )
                            }
                        }
                    }
                }
            }

            // ── Mode B: CHAR_RISE ────────────────────────────────────
            // Per-character dip-and-return wave using clipRect + translate
            AppleMusicMode.CHAR_RISE -> {
                if (wordAnimInfo != null) {
                    val charBounds = syllableLayout.charOriginalBounds ?: emptyList()
                    val numCharsInWord = wordAnimInfo.wordContent.length
                    val wordDurationMs = wordAnimInfo.wordDuration.toFloat()

                    // AmLyrics.ts: riseDurationMs = finalDuration * 1.2
                    val riseDuration = wordDurationMs * 1.2f
                    // AmLyrics.ts: baseDelayPerChar = finalDuration * 0.06
                    val baseDelayPerChar = wordDurationMs * 0.06f
                    val headroomPx = 8.dp.toPx()

                    syllableLayout.syllable.content.forEachIndexed { charIndex, _ ->
                        val charBox = charBounds.getOrNull(charIndex) ?: return@forEachIndexed

                        val absoluteCharIndex = syllableLayout.charOffsetInWord + charIndex
                        val charDelay = baseDelayPerChar * absoluteCharIndex
                        val charElapsed = currentTimeMs - wordAnimInfo.wordStartTime - charDelay
                        val progress = (charElapsed / riseDuration).coerceIn(0f, 1f)

                        val translateYPx = interpRise(progress, 0f, -1.44f, 0f, EaseInOutEasing).dp.toPx()

                        clipRect(
                            left = syllableLayout.position.x + charBox.left,
                            top = syllableLayout.position.y + charBox.top - headroomPx,
                            right = syllableLayout.position.x + charBox.right,
                            bottom = syllableLayout.position.y + charBox.bottom + headroomPx
                        ) {
                            translate(top = translateYPx) {
                                drawText(
                                    textLayoutResult = syllableLayout.textLayoutResult,
                                    color = drawColor,
                                    topLeft = syllableLayout.position
                                )
                            }
                        }
                    }

                    // Phonetics for CHAR_RISE
                    if (showPhonetic) {
                        syllableLayout.phoneticLayoutResult?.let { phoneticLayout ->
                            val phoneticX = syllableLayout.position.x
                            val phoneticY = syllableLayout.position.y + syllableLayout.textLayoutResult.size.height + 2.dp.toPx()
                            drawText(
                                textLayoutResult = phoneticLayout,
                                color = phoneticDrawColor,
                                topLeft = Offset(phoneticX, phoneticY),
                            )
                        }
                    }
                }
            }

            // ── Mode C: CHAR_DRAG ────────────────────────────────────
            // Per-character monotonic lift using clipRect + translate
            AppleMusicMode.CHAR_DRAG -> {
                if (wordAnimInfo != null) {
                    val charBounds = syllableLayout.charOriginalBounds ?: emptyList()
                    val wordDurationMs = wordAnimInfo.wordDuration.toFloat()

                    // AmLyrics.ts: dragDurationMs = clamp(finalDuration * 0.82, 560, 900)
                    val dragDuration = (wordDurationMs * 0.82f).coerceIn(560f, 900f)
                    // AmLyrics.ts: baseDelayPerChar = clamp(finalDuration * 0.15, 64, 118)
                    val baseDelayPerChar = (wordDurationMs * 0.15f).coerceIn(64f, 118f)
                    val headroomPx = 8.dp.toPx()

                    syllableLayout.syllable.content.forEachIndexed { charIndex, _ ->
                        val charBox = charBounds.getOrNull(charIndex) ?: return@forEachIndexed

                        val absoluteCharIndex = syllableLayout.charOffsetInWord + charIndex
                        val charDelay = baseDelayPerChar * absoluteCharIndex
                        val charElapsed = currentTimeMs - wordAnimInfo.wordStartTime - charDelay
                        val progress = (charElapsed / dragDuration).coerceIn(0f, 1f)

                        val translateYPx = interpDrag(progress, 0f, -1.29f, SimpleFloatEasing).dp.toPx()

                        clipRect(
                            left = syllableLayout.position.x + charBox.left,
                            top = syllableLayout.position.y + charBox.top - headroomPx,
                            right = syllableLayout.position.x + charBox.right,
                            bottom = syllableLayout.position.y + charBox.bottom + headroomPx
                        ) {
                            translate(top = translateYPx) {
                                drawText(
                                    textLayoutResult = syllableLayout.textLayoutResult,
                                    color = drawColor,
                                    topLeft = syllableLayout.position
                                )
                            }
                        }
                    }

                    // Phonetics for CHAR_DRAG
                    if (showPhonetic) {
                        syllableLayout.phoneticLayoutResult?.let { phoneticLayout ->
                            val phoneticX = syllableLayout.position.x
                            val phoneticY = syllableLayout.position.y + syllableLayout.textLayoutResult.size.height + 2.dp.toPx()
                            drawText(
                                textLayoutResult = phoneticLayout,
                                color = phoneticDrawColor,
                                topLeft = Offset(phoneticX, phoneticY),
                            )
                        }
                    }
                }
            }

            // ── Mode D: STATIC ───────────────────────────────────────
            // Standard block lift with dynamic duration clamping
            AppleMusicMode.STATIC -> {
                val actualDurationMs = (driverLayout.syllable.end - driverLayout.syllable.start).toFloat()
                val dynamicDurationMs = actualDurationMs.coerceIn(560f, 900f)
                val timeSinceStart = currentTimeMs - driverLayout.syllable.start
                val animationProgress = (timeSinceStart / dynamicDurationMs).coerceIn(0f, 1f)

                val floatCurveValue = SimpleFloatEasing.transform(animationProgress)
                val floatOffset = MaxSimpleFloatOffsetY * (1f - floatCurveValue)

                val finalPosition = syllableLayout.position.copy(
                    y = syllableLayout.position.y + floatOffset
                )

                drawText(
                    textLayoutResult = syllableLayout.textLayoutResult,
                    color = drawColor,
                    topLeft = finalPosition,
                )

                if (showPhonetic) {
                    syllableLayout.phoneticLayoutResult?.let { phoneticLayout ->
                        val phoneticX = syllableLayout.position.x
                        val phoneticY = finalPosition.y + syllableLayout.textLayoutResult.size.height + 2.dp.toPx()
                        drawText(
                            textLayoutResult = phoneticLayout,
                            color = phoneticDrawColor,
                            topLeft = Offset(phoneticX, phoneticY),
                        )
                    }
                }

                if (showDebugRectangles) {
                    drawRect(
                        color = Color.Green, topLeft = finalPosition, size = Size(
                            syllableLayout.textLayoutResult.size.width.toFloat(),
                            syllableLayout.textLayoutResult.size.height.toFloat()
                        ), style = Stroke(2f)
                    )
                }
            }
        }
    }
}

/**
 * Renders a single karaoke line, capable of handling multi-row wrapping.
 *
 * This composable pre-calculates the text layout and then
 * renders the frames using an efficient Canvas drawing strategy. It handles:
 * - Text measurement and line breaking
 * - Syllable and character-level animations (bounce, rise, swell)
 * - Karaoke fill gradient application
 *
 * @param line The karaoke line data.
 * @param currentTimeProvider Provider for the current playback time.
 * @param modifier Modifier for the layout.
 * @param normalLineTextStyle Style for normal lines.
 * @param accompanimentLineTextStyle Style for accompaniment lines.
 * @param phoneticTextStyle Style for phonetics.
 * @param activeColor Color for the active (sung) portion of text.
 * @param blendMode Blend mode for drawing.
 * @param showDebugRectangles Debug flag for layout bounds.
 * @param showTranslation Whether to show line-level translations.
 * @param showPhonetic Whether to show phonetics (both syllable and line level).
 * @param precalculatedLayouts Optional pre-calculated layouts (optimization).
 * @param isDuoView Whether this line is part of a duet view.
 * @param textMeasurer Text measurer for layout (default provided).
 */
@Composable
fun KaraokeLineText(
    line: KaraokeLine,
    currentTimeProvider: () -> Int,
    modifier: Modifier = Modifier,
    nextLineStartMs: Int? = null,
    nextLineHasBackground: Boolean = false,
    effectiveEndTimeMs: Int? = null,
    normalLineTextStyle: TextStyle = LocalTextStyle.current,
    accompanimentLineTextStyle: TextStyle = LocalTextStyle.current,
    phoneticTextStyle: TextStyle = LocalTextStyle.current,
    activeColor: Color = Color.White,
    blendMode: BlendMode = BlendMode.SrcOver,
    showDebugRectangles: Boolean = false,
    showTranslation: Boolean = true,
    showPhonetic: Boolean = true,
    precalculatedLayouts: List<SyllableLayout>? = null,
    isDuoView: Boolean = false,
    textMeasurer: TextMeasurer = rememberTextMeasurer()
) {
    val isLineRtl = remember(line.syllables) { line.syllables.any { it.content.isRtl() } }

    val isRightAligned = remember(line.alignment, isLineRtl) {
        when (line.alignment) {
            KaraokeAlignment.Start, KaraokeAlignment.Unspecified -> isLineRtl
            KaraokeAlignment.End -> !isLineRtl
        }
    }

    val translationTextAlign = remember(isRightAligned) {
        if (isRightAligned) TextAlign.End else TextAlign.Start
    }

    val columnHorizontalAlignment = remember(isRightAligned) {
        if (isRightAligned) Alignment.End else Alignment.Start
    }

    val mainLine = line as? KaraokeLine.MainKaraokeLine
    val allAccompanimentLines = mainLine?.accompanimentLines.orEmpty()

    @Composable
    fun AccompanimentLines(accompanimentLines: List<KaraokeLine>) {
        accompanimentLines.forEach { bgLine ->
            val mainStart = line.start
            val parentEnd = effectiveEndTimeMs ?: line.end
            val effectiveStart = minOf(mainStart, bgLine.start)
            val parentTerminationTime = if (nextLineStartMs != null && (nextLineStartMs - line.end <= 7000)) {
                maxOf(parentEnd, nextLineStartMs)
            } else {
                parentEnd
            }
            val effectiveEnd = if (nextLineHasBackground && nextLineStartMs != null) {
                nextLineStartMs
            } else {
                parentTerminationTime
            }

            val isAccompanimentVisible by remember(bgLine, mainStart, parentTerminationTime, nextLineStartMs, nextLineHasBackground) {
                derivedStateOf {
                    val currentTime = currentTimeProvider()
                    currentTime in effectiveStart..effectiveEnd
                }
            }

            AnimatedVisibility(
                visible = isAccompanimentVisible,
                enter = slideInVertically(
                    animationSpec = tween(380, easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)),
                    initialOffsetY = { -it }
                ) + fadeIn(tween(380)),
                exit = fadeOut(tween(150)) + shrinkVertically(
                    animationSpec = tween(200, easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)),
                    shrinkTowards = Alignment.Top
                ),
            ) {
                LyricsLineItem(
                    isFocused = true,
                    isRightAligned = isRightAligned,
                    onLineClicked = { },
                    onLinePressed = { },
                    blurRadius = { 0f },
                    blendMode = blendMode,
                    activeAlpha = 0.6f,
                    inactiveAlpha = 0.2f
                ) {
                    KaraokeLineText(
                        line = bgLine,
                        currentTimeProvider = currentTimeProvider,
                        effectiveEndTimeMs = effectiveEnd,
                        nextLineStartMs = nextLineStartMs,
                        nextLineHasBackground = nextLineHasBackground,
                        normalLineTextStyle = normalLineTextStyle,
                        accompanimentLineTextStyle = accompanimentLineTextStyle,
                        phoneticTextStyle = phoneticTextStyle,
                        activeColor = activeColor,
                        blendMode = blendMode,
                        showDebugRectangles = showDebugRectangles,
                        showTranslation = showTranslation,
                        showPhonetic = showPhonetic,
                        textMeasurer = textMeasurer
                    )
                }
            }
        }
    }

    Column(
        modifier = modifier.fillMaxWidth().padding(
            vertical = 8.dp
        ),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        horizontalAlignment = columnHorizontalAlignment
    ) {
        val hasSyllablePhonetics = remember(line.syllables) {
            line.syllables.any { !it.phonetic.isNullOrBlank() }
        }

        BoxWithConstraints {
            val density = LocalDensity.current
            val availableWidthPx = with(density) { maxWidth.toPx() }
            val spacingPx = with(density) { 6.dp.toPx() }

            val textStyle = remember(line is KaraokeLine.AccompanimentKaraokeLine) {
                val baseStyle =
                    if (line is KaraokeLine.AccompanimentKaraokeLine) accompanimentLineTextStyle
                    else normalLineTextStyle
                baseStyle.copy(textDirection = TextDirection.Content)
            }

            val spaceWidth = remember(textMeasurer, textStyle) {
                textMeasurer.measure(" ", textStyle).size.width.toFloat()
            }

            val processedSyllables = remember(line.syllables, line.alignment) {
                if (line.alignment == KaraokeAlignment.End) {
                    line.syllables.dropLastWhile { it.content.isBlank() }
                } else {
                    line.syllables
                }
            }

            val initialLayouts by remember(precalculatedLayouts) {
                derivedStateOf {
                    precalculatedLayouts ?: measureSyllablesAndDetermineAnimation(
                        syllables = processedSyllables,
                        textMeasurer = textMeasurer,
                        style = textStyle,
                        phoneticStyle = phoneticTextStyle,
                        isAccompanimentLine = line is KaraokeLine.AccompanimentKaraokeLine,
                        spaceWidth = spaceWidth
                    )
                }
            }

            val wrappedLines by remember {
                derivedStateOf {
                    calculateBalancedLines(
                        syllableLayouts = initialLayouts,
                        availableWidthPx = availableWidthPx,
                        textMeasurer = textMeasurer,
                        style = textStyle
                    )
                }
            }

            val lineHeight = remember(textStyle) {
                textMeasurer.measure("M", textStyle).size.height.toFloat()
            }

            val phoneticHeight = remember(phoneticTextStyle) {
                textMeasurer.measure("M", phoneticTextStyle).size.height.toFloat()
            }

            val finalLineLayouts = remember(
                wrappedLines, availableWidthPx, lineHeight, isLineRtl, isRightAligned, showPhonetic
            ) {
                calculateStaticLineLayout(
                    wrappedLines = wrappedLines,
                    isLineRightAligned = isRightAligned,
                    canvasWidth = availableWidthPx,
                    lineHeight = lineHeight,
                    phoneticHeight = if (showPhonetic) phoneticHeight else 0f,
                    isRtl = isLineRtl
                )
            }

            val rowRenderData = remember(finalLineLayouts, showPhonetic, density) {
                calculateRowRenderData(
                    lineLayouts = finalLineLayouts,
                    showPhonetic = showPhonetic,
                    density = density.density
                )
            }

            val hasPhonetics = remember(initialLayouts, showPhonetic) {
                showPhonetic && initialLayouts.any { it.phoneticLayoutResult != null }
            }

            val totalHeight = remember(wrappedLines, lineHeight, hasPhonetics, phoneticHeight) {
                var height = lineHeight * wrappedLines.size
                if (hasPhonetics) {
                    height += phoneticHeight * wrappedLines.size
                }
                height
            }

            Canvas(modifier = Modifier.size(maxWidth, (totalHeight.roundToInt() + 8).toDp())) {
                val time = currentTimeProvider()
                drawLyricsLine(
                    rowRenderData = rowRenderData,
                    currentTimeMs = time,
                    color = activeColor,
                    blendMode = blendMode,
                    isRtl = isLineRtl,
                    showDebugRectangles = showDebugRectangles,
                    showPhonetic = showPhonetic
                )
            }
        }

        if (showTranslation) {
            line.translation?.let { translation ->
                val lineText = line.syllables.joinToString("") { it.content }.trim()
                if (translation.trim().isNotEmpty() &&
                    !translation.trim().equals(lineText, ignoreCase = true) &&
                    !translation.trim().equals(line.phonetic?.trim(), ignoreCase = true)
                ) {
                    Text(
                        text = translation,
                        style = normalLineTextStyle.copy(
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Medium,
                        ),
                        color = activeColor.copy(alpha = 0.65f),
                        modifier = Modifier.graphicsLayer {
                            this.blendMode = blendMode
                        },
                        textAlign = translationTextAlign
                    )
                }
            }
        }

        if (showPhonetic && !hasSyllablePhonetics) {
            line.phonetic?.let { phonetic ->
                val lineText = line.syllables.joinToString("") { it.content }.trim()
                if (phonetic.trim().isNotEmpty() && !phonetic.trim().equals(lineText, ignoreCase = true)) {
                    Text(
                        text = phonetic,
                        style = phoneticTextStyle,
                        color = activeColor.copy(alpha = 0.6f),
                        modifier = Modifier.graphicsLayer {
                            this.blendMode = blendMode
                        },
                        textAlign = translationTextAlign
                    )
                }
            }
        }
        AccompanimentLines(allAccompanimentLines)
    }
}

@Composable
private fun Int.toDp(): Dp = with(LocalDensity.current) { this@toDp.toDp() }
