package com.maxrave.simpmusic.extension

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Point
import android.graphics.drawable.AdaptiveIconDrawable
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SheetValue.Hidden
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.DrawModifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.core.app.PictureInPictureModeChangedInfo
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.drawable.toBitmap
import androidx.core.util.Consumer
import com.kmpalette.palette.graphics.Palette
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.refraction
import com.maxrave.domain.data.model.ui.ScreenSizeInfo
import com.maxrave.logger.Logger
import com.maxrave.simpmusic.ui.theme.md_theme_dark_background
import com.maxrave.simpmusic.ui.theme.shimmerBackground
import com.maxrave.simpmusic.ui.theme.shimmerLine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sign
import kotlin.math.sin
import kotlin.random.Random

fun generateRandomColor(): Color {
    val red = Random.nextInt(256)
    val green = Random.nextInt(256)
    val blue = Random.nextInt(256)
    return Color(red, green, blue)
}

fun Modifier.shimmer(): Modifier =
    composed {
        var size by remember {
            mutableStateOf(IntSize.Zero)
        }
        val transition = rememberInfiniteTransition(label = "Shimmer")
        val startOffsetX by transition.animateFloat(
            initialValue = -2 * size.width.toFloat(),
            targetValue = 2 * size.width.toFloat(),
            animationSpec =
                infiniteRepeatable(
                    animation = tween(1000),
                ),
            label = "Shimmer",
        )

        background(
            brush =
                Brush.linearGradient(
                    colors =
                        listOf(
                            shimmerBackground,
                            shimmerLine,
                            shimmerBackground,
                        ),
                    start = Offset(startOffsetX, 0f),
                    end = Offset(startOffsetX + size.width.toFloat(), size.height.toFloat()),
                ),
        ).onGloballyPositioned {
            size = it.size
        }
    }

class GreyScaleModifier : DrawModifier {
    override fun ContentDrawScope.draw() {
        val saturationMatrix = ColorMatrix().apply { setToSaturation(0f) }
        val saturationFilter = ColorFilter.colorMatrix(saturationMatrix)
        val paint =
            Paint().apply {
                colorFilter = saturationFilter
            }
        drawIntoCanvas {
            it.saveLayer(Rect(0f, 0f, size.width, size.height), paint)
            drawContent()
            it.restore()
        }
    }
}

fun LazyListState.visibilityPercent(info: LazyListItemInfo): Float {
    val cutTop = max(0, layoutInfo.viewportStartOffset - info.offset)
    val cutBottom = max(0, info.offset + info.size - layoutInfo.viewportEndOffset)

    return max(0f, 100f - (cutTop + cutBottom) * 100f / info.size)
}

fun Modifier.greyScale() = this.then(GreyScaleModifier())

fun Modifier.angledGradientBackground(
    colors: List<Color>,
    degrees: Float,
) = this.then(
    Modifier.drawBehind {
        /*
        Have to compute length of gradient vector so that it lies within
        the visible rectangle.
        --------------------------------------------
        | length of gradient ^  /                  |
        |             --->  /  /                   |
        |                  /  / <- rotation angle  |
        |                 /  o --------------------|  y
        |                /  /                      |
        |               /  /                       |
        |              v  /                        |
        --------------------------------------------
                             x

                   diagonal angle = atan2(y, x)
                 (it's hard to draw the diagonal)

        Simply rotating the diagonal around the centre of the rectangle
        will lead to points outside the rectangle area. Further, just
        truncating the coordinate to be at the nearest edge of the
        rectangle to the rotated point will distort the angle.
        Let α be the desired gradient angle (in radians) and γ be the
        angle of the diagonal of the rectangle.
        The correct for the length of the gradient is given by:
        x/|cos(α)|  if -γ <= α <= γ,   or   π - γ <= α <= π + γ
        y/|sin(α)|  if  γ <= α <= π - γ, or π + γ <= α <= 2π - γ
        where γ ∈ (0, π/2) is the angle that the diagonal makes with
        the base of the rectangle.

         */

        val (x, y) = size
        val gamma = atan2(y, x)

        if (gamma == 0f || gamma == (PI / 2).toFloat()) {
            // degenerate rectangle
            return@drawBehind
        }

        val degreesNormalised = (degrees % 360).let { if (it < 0) it + 360 else it }

        val alpha = (degreesNormalised * PI / 180).toFloat()

        val gradientLength =
            when (alpha) {
                // ray from centre cuts the right edge of the rectangle
                in 0f..gamma, in (2 * PI - gamma)..2 * PI -> {
                    x / cos(alpha)
                }
                // ray from centre cuts the top edge of the rectangle
                in gamma..(PI - gamma).toFloat() -> {
                    y / sin(alpha)
                }
                // ray from centre cuts the left edge of the rectangle
                in (PI - gamma)..(PI + gamma) -> {
                    x / -cos(alpha)
                }
                // ray from centre cuts the bottom edge of the rectangle
                in (PI + gamma)..(2 * PI - gamma) -> {
                    y / -sin(alpha)
                }
                // default case (which shouldn't really happen)
                else -> hypot(x, y)
            }

        val centerOffsetX = cos(alpha) * gradientLength / 2
        val centerOffsetY = sin(alpha) * gradientLength / 2

        drawRect(
            brush =
                Brush.linearGradient(
                    colors = colors,
                    // negative here so that 0 degrees is left -> right
                    start = Offset(center.x - centerOffsetX, center.y - centerOffsetY),
                    end = Offset(center.x + centerOffsetX, center.y + centerOffsetY),
                ),
            size = size,
        )
    },
)

// Angle Gradient Background without size
fun GradientOffset(angle: GradientAngle): GradientOffset =
    when (angle) {
        GradientAngle.CW45 ->
            GradientOffset(
                start = Offset.Zero,
                end = Offset.Infinite,
            )
        GradientAngle.CW90 ->
            GradientOffset(
                start = Offset.Zero,
                end = Offset(0f, Float.POSITIVE_INFINITY),
            )
        GradientAngle.CW135 ->
            GradientOffset(
                start = Offset(Float.POSITIVE_INFINITY, 0f),
                end = Offset(0f, Float.POSITIVE_INFINITY),
            )
        GradientAngle.CW180 ->
            GradientOffset(
                start = Offset(Float.POSITIVE_INFINITY, 0f),
                end = Offset.Zero,
            )
        GradientAngle.CW225 ->
            GradientOffset(
                start = Offset.Infinite,
                end = Offset.Zero,
            )
        GradientAngle.CW270 ->
            GradientOffset(
                start = Offset(0f, Float.POSITIVE_INFINITY),
                end = Offset.Zero,
            )
        GradientAngle.CW315 ->
            GradientOffset(
                start = Offset(0f, Float.POSITIVE_INFINITY),
                end = Offset(Float.POSITIVE_INFINITY, 0f),
            )
        else ->
            GradientOffset(
                start = Offset.Zero,
                end = Offset(Float.POSITIVE_INFINITY, 0f),
            )
    }

/**
 * Offset for [Brush.linearGradient] to rotate gradient depending on [start] and [end] offsets.
 */
data class GradientOffset(
    val start: Offset,
    val end: Offset,
)

enum class GradientAngle {
    CW0,
    CW45,
    CW90,
    CW135,
    CW180,
    CW225,
    CW270,
    CW315,
}

fun Context.getActivityOrNull(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }

    return null
}

@Composable
@Suppress("DEPRECATION")
fun getScreenSizeInfo(): ScreenSizeInfo {
    val context = LocalContext.current
    val activity = context.getActivityOrNull()
    val configuration = LocalConfiguration.current
    val localDensity = LocalDensity.current

    return remember(configuration) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowMetrics = activity?.windowManager?.currentWindowMetrics
            Logger.w("getScreenSizeInfo", "WindowMetrics: ${windowMetrics?.bounds?.height()}")
            ScreenSizeInfo(
                hDP = with(localDensity) { (windowMetrics?.bounds?.height())?.toDp()?.value?.toInt() ?: 0 },
                wDP = with(localDensity) { (windowMetrics?.bounds?.height())?.toDp()?.value?.toInt() ?: 0 },
                hPX = windowMetrics?.bounds?.height() ?: 0,
                wPX = windowMetrics?.bounds?.width() ?: 0,
            )
        } else {
            val point = Point()
            activity?.windowManager?.defaultDisplay?.getRealSize(point)
            Logger.w("getScreenSizeInfo", "WindowMetrics: ${point.y}")
            ScreenSizeInfo(
                hDP =
                    with(localDensity) {
                        point.y
                            .toDp()
                            .value
                            .toInt()
                    },
                wDP =
                    with(localDensity) {
                        point.x
                            .toDp()
                            .value
                            .toInt()
                    },
                hPX = point.y,
                wPX = point.x,
            )
        }
    }
}

@Composable
fun NonLazyGrid(
    columns: Int,
    itemCount: Int,
    modifier: Modifier = Modifier,
    content:
        @Composable()
        (Int) -> Unit,
) {
    Column(modifier = modifier) {
        var rows = (itemCount / columns)
        if (itemCount.mod(columns) > 0) {
            rows += 1
        }

        for (rowId in 0 until rows) {
            val firstIndex = rowId * columns

            Row {
                for (columnId in 0 until columns) {
                    val index = firstIndex + columnId
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .weight(1f),
                    ) {
                        if (index < itemCount) {
                            content(index)
                        }
                    }
                }
            }
        }
    }
}

fun LazyListState.animateScrollAndCentralizeItem(
    index: Int,
    scope: CoroutineScope,
) {
    val itemInfo = this.layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
    scope.launch {
        if (itemInfo != null) {
            val center = this@animateScrollAndCentralizeItem.layoutInfo.viewportEndOffset / 2
            val childCenter = itemInfo.offset + itemInfo.size / 2
            this@animateScrollAndCentralizeItem.animateScrollBy((childCenter - center).toFloat())
        } else {
            this@animateScrollAndCentralizeItem.animateScrollToItem(index)
        }
    }
}

@Composable
fun KeepScreenOn() {
    val currentView = LocalView.current
    DisposableEffect(Unit) {
        currentView.keepScreenOn = true
        onDispose {
            currentView.keepScreenOn = false
        }
    }
}

@Composable
fun LazyListState.isScrollingUp(): State<Boolean> {
    var previousIndex by remember(this) { mutableIntStateOf(firstVisibleItemIndex) }
    var previousScrollOffset by remember(this) { mutableIntStateOf(firstVisibleItemScrollOffset) }

    LaunchedEffect(Unit) {
        snapshotFlow { layoutInfo.totalItemsCount }.collect {
            Logger.w("isScrollingUp", "firstVisibleItemIndex: $firstVisibleItemIndex")
            previousIndex = firstVisibleItemIndex
            previousScrollOffset = firstVisibleItemScrollOffset
        }
    }

    return remember(this) {
        derivedStateOf {
            if (firstVisibleItemIndex > 0) {
                if (previousIndex != firstVisibleItemIndex) {
                    previousIndex > firstVisibleItemIndex
                } else {
                    previousScrollOffset >= firstVisibleItemScrollOffset
                }.also {
                    previousIndex = firstVisibleItemIndex
                    previousScrollOffset = firstVisibleItemScrollOffset
                }
            } else {
                true
            }
        }
    }
}

@Composable
fun LazyGridState.isScrollingUp(): State<Boolean> {
    var previousIndex by remember(this) { mutableIntStateOf(firstVisibleItemIndex) }
    var previousScrollOffset by remember(this) { mutableIntStateOf(firstVisibleItemScrollOffset) }

    LaunchedEffect(Unit) {
        snapshotFlow { layoutInfo.totalItemsCount }.collect {
            Logger.w("isScrollingUp", "firstVisibleItemIndex: $firstVisibleItemIndex")
            previousIndex = firstVisibleItemIndex
            previousScrollOffset = firstVisibleItemScrollOffset
        }
    }

    return remember(this) {
        derivedStateOf {
            if (firstVisibleItemIndex > 0) {
                if (previousIndex != firstVisibleItemIndex) {
                    previousIndex > firstVisibleItemIndex
                } else {
                    previousScrollOffset >= firstVisibleItemScrollOffset
                }.also {
                    previousIndex = firstVisibleItemIndex
                    previousScrollOffset = firstVisibleItemScrollOffset
                }
            } else {
                true
            }
        }
    }
}

@Suppress("DEPRECATION")
fun setStatusBarsColor(
    @ColorInt color: Int,
    activity: Activity,
) {
    if (Build.VERSION.SDK_INT < 35) {
        activity.window.statusBarColor = color
    }
}

fun Palette?.getColorFromPalette(): Color {
    val p = this ?: return md_theme_dark_background
    val defaultColor = 0x000000
    var startColor = p.getDarkVibrantColor(defaultColor)
    if (startColor == defaultColor) {
        startColor = p.getDarkMutedColor(defaultColor)
        if (startColor == defaultColor) {
            startColor = p.getVibrantColor(defaultColor)
            if (startColor == defaultColor) {
                startColor =
                    p.getMutedColor(defaultColor)
                if (startColor == defaultColor) {
                    startColor =
                        p.getLightVibrantColor(
                            defaultColor,
                        )
                    if (startColor == defaultColor) {
                        startColor =
                            p.getLightMutedColor(
                                defaultColor,
                            )
                    }
                }
            }
        }
    }
    return Color(ColorUtils.setAlphaComponent(startColor, 255))
}

fun Context.findActivity(): ComponentActivity {
    var context = this
    while (context is ContextWrapper) {
        if (context is ComponentActivity) return context
        context = context.baseContext
    }
    throw IllegalStateException("Picture in picture should be called in the context of an Activity")
}

fun Modifier.isElementVisible(onVisibilityChanged: (Boolean) -> Unit) =
    composed {
        val isVisible by remember { derivedStateOf { mutableStateOf(false) } }
        LaunchedEffect(isVisible.value) { onVisibilityChanged.invoke(isVisible.value) }
        this.onGloballyPositioned { layoutCoordinates ->
            isVisible.value = layoutCoordinates.parentLayoutCoordinates?.let {
                val parentBounds = it.boundsInWindow()
                val childBounds = layoutCoordinates.boundsInWindow()
                parentBounds.overlaps(childBounds)
            } == true
        }
    }

fun Color.rgbFactor(factor: Float): Color {
    val r = min(red * factor, 255f)
    val g = min(green * factor, 255f)
    val b = min(blue * factor, 255f)
    return Color(r, g, b, alpha)
}

fun TextStyle.greyScale(): TextStyle =
    this.copy(
        color = Color.Gray,
    )

@Composable
fun adaptiveIconPainterResource(
    @DrawableRes id: Int,
): Painter? {
    val res = LocalResources.current
    val theme = LocalContext.current.theme

    val adaptiveIcon = ResourcesCompat.getDrawable(res, id, theme) as? AdaptiveIconDrawable
    if (adaptiveIcon != null) {
        return BitmapPainter(adaptiveIcon.toBitmap().asImageBitmap())
    } else {
        return null
    }
}

@Composable
fun rememberIsInPipMode(): Boolean {
    val activity = LocalContext.current.findActivity()
    var pipMode by remember { mutableStateOf(activity.isInPictureInPictureMode) }
    DisposableEffect(activity) {
        val observer =
            Consumer<PictureInPictureModeChangedInfo> { info ->
                pipMode = info.isInPictureInPictureMode
            }
        activity.addOnPictureInPictureModeChangedListener(
            observer,
        )
        onDispose { activity.removeOnPictureInPictureModeChangedListener(observer) }
    }
    return pipMode
}

@Composable
fun animateAlignmentAsState(targetAlignment: Alignment): State<Alignment> {
    val biased = targetAlignment as BiasAlignment
    val horizontal by animateFloatAsState(biased.horizontalBias)
    val vertical by animateFloatAsState(biased.verticalBias)
    return remember { derivedStateOf { BiasAlignment(horizontal, vertical) } }
}

fun Modifier.drawBackdropCustomShape(
    backdrop: Backdrop,
    layer: GraphicsLayer,
    luminanceAnimation: Float,
    shape: Shape,
): Modifier =
    this.drawBackdrop(
        backdrop = backdrop,
        effects = {
            val l = (luminanceAnimation * 2f - 1f).let { sign(it) * it * it }
            colorControls(
                brightness =
                    if (l > 0f) {
                        lerp(0.1f, 0.5f, l)
                    } else {
                        lerp(0.1f, -0.2f, -l)
                    },
                contrast =
                    if (l > 0f) {
                        lerp(1f, 0f, l)
                    } else {
                        1f
                    },
                saturation = 1.5f,
            )
            blur(
                if (l > 0f) {
                    lerp(8f.dp.toPx(), 16f.dp.toPx(), l)
                } else {
                    lerp(8f.dp.toPx(), 2f.dp.toPx(), -l)
                },
            )
            refraction(24f.dp.toPx(), size.minDimension / 2f, true)
        },
        onDrawBackdrop = { drawBackdrop ->
            drawBackdrop()
            layer.record { drawBackdrop() }
        },
        shape = { shape },
    )

@Composable
fun PaddingValues.copy(
    start: Dp? = null,
    top: Dp? = null,
    end: Dp? = null,
    bottom: Dp? = null,
): PaddingValues {
    val layoutDirection = LocalLayoutDirection.current
    return PaddingValues(
        start = start ?: this.calculateStartPadding(layoutDirection),
        top = top ?: this.calculateTopPadding(),
        end = end ?: this.calculateEndPadding(layoutDirection),
        bottom = bottom ?: this.calculateBottomPadding(),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun rememberNoBouncyBottomSheetState(
    skipPartiallyExpanded: Boolean = false,
    confirmValueChange: (SheetValue) -> Boolean = { true },
    initialValue: SheetValue = Hidden,
    skipHiddenState: Boolean = false,
): SheetState {
    val positionalThresholdToPx = 0f
    val velocityThresholdToPx = 0f
    return rememberSaveable(
        skipPartiallyExpanded,
        confirmValueChange,
        skipHiddenState,
        saver =
            SheetState.Saver(
                skipPartiallyExpanded = skipPartiallyExpanded,
                positionalThreshold = { positionalThresholdToPx },
                velocityThreshold = { velocityThresholdToPx },
                confirmValueChange = confirmValueChange,
                skipHiddenState = skipHiddenState,
            ),
    ) {
        SheetState(
            skipPartiallyExpanded,
            { positionalThresholdToPx },
            { velocityThresholdToPx },
            initialValue,
            confirmValueChange,
            skipHiddenState,
        )
    }
}