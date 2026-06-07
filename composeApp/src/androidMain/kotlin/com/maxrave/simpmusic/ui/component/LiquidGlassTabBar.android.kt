package com.maxrave.simpmusic.ui.component

import android.os.SystemClock
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.MutatorMutex
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.maxrave.simpmusic.expect.ui.PlatformBackdrop
import com.maxrave.simpmusic.ui.theme.bottomBarSeedDark
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.ui.theme.white
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sign

private val CapsuleShape = RoundedCornerShape(percent = 50)
private val TabWidth = 96.dp
private val BarHeight = 64.dp
private val BlobHeight = 56.dp

/**
 * iOS 26 / Kyant-style liquid sliding tab bar.
 *
 * Three stacked layers (bottom → top):
 *  1. a dark, luminance-adaptive glass **capsule** (the same [drawInteractiveGlass] as the MiniPlayer),
 *  2. a **frosted blob** that slides to the selected tab, squashing/stretching with
 *     drag velocity (`DampedDragAnimation`) — the selection indicator, and
 *  3. crisp **icons + labels** on top (so the active label stays sharp, unlike when
 *     the blob is drawn over it).
 *
 * Adapted from Kyant's `LiquidBottomTabs`/`DampedDragAnimation` to SimpMusic's
 * [BottomNavScreen]s, fixed tab width (to fit the existing ConstraintLayout) and the
 * bottom bar's luminance sampling.
 *
 * @param layer shared graphics layer the capsule records into for luminance sampling.
 * @param luminance current sampled luminance (0..1) driving the glass brightness.
 * @param onTabSelected fired when the user taps a tab or drag-snaps the blob.
 */
@Composable
fun LiquidGlassTabBar(
    tabs: List<BottomNavScreen>,
    selectedTab: Int,
    backdrop: PlatformBackdrop,
    layer: GraphicsLayer,
    luminance: Float,
    modifier: Modifier = Modifier,
    onTabSelected: (Int) -> Unit,
) {
    val density = LocalDensity.current
    val tabsCount = tabs.size
    val tabWidthPx = with(density) { TabWidth.toPx() }
    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
    val animationScope = rememberCoroutineScope()
    val barInteraction = rememberGlassInteraction()

    var currentIndex by remember { mutableIntStateOf(selectedTab.coerceAtLeast(0)) }
    // [0] = a real drag happened (vs a pure tap) — keeps the blob from snapping back
    // and stealing a tab tap.
    val draggedFlag = remember { booleanArrayOf(false) }

    val dampedDrag =
        remember(animationScope, tabsCount) {
            DampedDragAnimation(
                animationScope = animationScope,
                initialValue = selectedTab.coerceAtLeast(0).toFloat(),
                valueRange = 0f..(tabsCount - 1).toFloat(),
                visibilityThreshold = 0.001f,
                initialScale = 1f,
                // On press the blob inflates from 56dp up to ~76dp — past the 64dp bar height — so it
                // visibly bulges out of the capsule row (Apple Music style), then springs back on release.
                pressedScale = 76f / 56f,
                onDragStarted = { draggedFlag[0] = false },
                onDragStopped = {
                    if (draggedFlag[0]) {
                        val target = targetValue.roundToInt().coerceIn(0, tabsCount - 1)
                        currentIndex = target
                        // Always snap the blob to a tab — even if the rounded target is unchanged —
                        // otherwise `value` settles mid-slide and the blob sticks after release.
                        animateToValue(target.toFloat())
                    }
                },
                onDrag = { _, dragAmount ->
                    if (dragAmount.x != 0f) draggedFlag[0] = true
                    updateValue(
                        (targetValue + dragAmount.x / tabWidthPx * if (isLtr) 1f else -1f)
                            .coerceIn(0f, (tabsCount - 1).toFloat()),
                    )
                },
            )
        }

    // Keep the blob in sync when selection changes from outside (e.g. back stack).
    LaunchedEffect(selectedTab) {
        if (selectedTab >= 0 && currentIndex != selectedTab) currentIndex = selectedTab
    }
    // Drive navigation + blob animation from the internal current index.
    LaunchedEffect(dampedDrag) {
        snapshotFlow { currentIndex }
            .drop(1)
            .collectLatest { index ->
                dampedDrag.animateToValue(index.toFloat())
                onTabSelected(index)
            }
    }

    Box(
        modifier =
            modifier
                .height(BarHeight)
                .width(TabWidth * tabsCount)
                // Press detection for the whole capsule lives on the outer Box — it's the common
                // ancestor of the glass, blob and tab Row, so it sees the touch on the Initial pass
                // before the children. The capsule glass sits underneath the Row and would never get
                // the event on its own; it only reads barInteraction to draw the scale + glow.
                .pointerInput(barInteraction) { barInteraction.detectPress(this) },
        contentAlignment = Alignment.CenterStart,
    ) {
        // 1) Dark frosted glass capsule — the exact same glass the MiniPlayer uses, so the bottom
        // bar and the mini player read as one material (drawInteractiveGlass, no white veil).
        // barInteraction makes the whole capsule respond to a press (scale + touch glow) like iOS;
        // it's observe-only, so tab taps and the blob drag keep working.
        Box(Modifier.matchParentSize().drawInteractiveGlass(backdrop, layer, luminance, CapsuleShape, barInteraction))

        // 2) Frosted blob selection indicator — slides behind the icons.
        Box(
            Modifier
                .graphicsLayer {
                    // Per-tab slot start + a symmetric 4dp inset, so the pill keeps equal padding on
                    // both the first and last tab (the last tab used to sit flush to the edge).
                    translationX =
                        (if (isLtr) dampedDrag.value else (tabsCount - 1) - dampedDrag.value) * tabWidthPx +
                            4.dp.toPx()
                }
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { CapsuleShape },
                    effects = {
                        // Luminance only drives the blur here (frosted pill); brightness/contrast stay
                        // neutral and the "đục đen" darkening is applied in onDrawSurface.
                        val l = (luminance * 2f - 1f).let { sign(it) * it * it }
                        val progress = dampedDrag.pressProgress
                        vibrancy()
                        colorControls(
                            brightness = 0.05f,
                            contrast = 1f,
                            saturation = 1.5f,
                        )
                        blur(
                            // Stronger than the bar's blur so the active pill reads as a clearly
                            // frosted surface (the previous amount was too weak / too close to the bar).
                            (if (l > 0f) lerp(8f.dp.toPx(), 16f.dp.toPx(), l) else lerp(8f.dp.toPx(), 2f.dp.toPx(), -l)) +
                                20f.dp.toPx(),
                        )
                        lens(10f.dp.toPx() * progress, 14f.dp.toPx() * progress, chromaticAberration = true)
                    },
                    highlight = { Highlight.Default.copy(alpha = 0.6f) },
                    shadow = { Shadow(radius = 4f.dp, alpha = 0.4f) },
                    innerShadow = {
                        val progress = dampedDrag.pressProgress
                        InnerShadow(radius = 8f.dp * progress, alpha = progress)
                    },
                    layerBlock = {
                        scaleX = dampedDrag.scaleX
                        scaleY = dampedDrag.scaleY
                        val velocity = dampedDrag.velocity / 10f
                        scaleX /= 1f - (velocity * 0.75f).fastCoerceIn(-0.2f, 0.2f)
                        scaleY *= 1f - (velocity * 0.25f).fastCoerceIn(-0.2f, 0.2f)
                    },
                    onDrawSurface = {
                        // Dark "đục đen" active pill — always a touch darker than the bar, scaling
                        // with the background so it never washes out to white.
                        val darken = lerp(0.22f, 0.55f, ((luminance - 0.3f) / 0.5f).coerceIn(0f, 1f))
                        drawRect(Color.Black.copy(alpha = darken))
                    },
                )
                .width(TabWidth - 8.dp)
                .height(BlobHeight),
        )

        // 3) Crisp icons + labels on top, carrying the blob drag gesture.
        Row(
            // No horizontal padding here: tabs tile exactly [0..TabWidth..] so each icon's centre
            // lines up with the blob's per-tab slot (the Home icon used to sit 4dp off).
            Modifier
                .matchParentSize()
                .then(dampedDrag.modifier),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabs.forEachIndexed { position, screen ->
                LiquidGlassTab(
                    screen = screen,
                    selected = currentIndex == position,
                ) {
                    if (position == currentIndex) {
                        // Re-tapping the active tab: snapshotFlow won't fire (state unchanged),
                        // so call onTabSelected directly to keep the reload / scroll-to-top behaviour.
                        onTabSelected(position)
                    } else {
                        currentIndex = position
                    }
                }
            }
        }
    }
}

@Composable
private fun LiquidGlassTab(
    screen: BottomNavScreen,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val color = if (selected) bottomBarSeedDark else white
    Column(
        Modifier
            .width(TabWidth)
            .fillMaxHeight()
            .clip(CapsuleShape)
            .clickable(
                interactionSource = null,
                indication = null,
                role = Role.Tab,
                onClick = onClick,
            ),
        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CompositionLocalProvider(LocalContentColor provides color) {
            screen.icon()
            Text(
                text = stringResource(screen.title),
                style = typo().bodySmall,
                color = color,
                maxLines = 1,
            )
        }
    }
}

/**
 * Spring-damped 1-D drag animation: tracks a continuous [value] across the tab
 * range, the drag [velocity] (for squash/stretch), a [pressProgress] (rest → lifted
 * glass) and decoupled [scaleX]/[scaleY] springs. Project-local port of Kyant's
 * catalog `DampedDragAnimation` (uses [withFrameNanos] for `awaitFrame` and
 * [SystemClock] for velocity timestamps).
 */
class DampedDragAnimation(
    private val animationScope: CoroutineScope,
    val initialValue: Float,
    val valueRange: ClosedRange<Float>,
    val visibilityThreshold: Float,
    val initialScale: Float,
    val pressedScale: Float,
    val onDragStarted: DampedDragAnimation.(position: Offset) -> Unit,
    val onDragStopped: DampedDragAnimation.() -> Unit,
    val onDrag: DampedDragAnimation.(size: IntSize, dragAmount: Offset) -> Unit,
) {
    private val valueAnimationSpec = spring(1f, 1000f, visibilityThreshold)
    private val velocityAnimationSpec = spring(0.5f, 300f, visibilityThreshold * 10f)
    private val pressProgressAnimationSpec = spring(1f, 1000f, 0.001f)
    private val scaleXAnimationSpec = spring(0.6f, 250f, 0.001f)
    private val scaleYAnimationSpec = spring(0.7f, 250f, 0.001f)

    private val valueAnimation = Animatable(initialValue, visibilityThreshold)
    private val velocityAnimation = Animatable(0f, 5f)
    private val pressProgressAnimation = Animatable(0f, 0.001f)
    private val scaleXAnimation = Animatable(initialScale, 0.001f)
    private val scaleYAnimation = Animatable(initialScale, 0.001f)

    private val mutatorMutex = MutatorMutex()
    private val velocityTracker = VelocityTracker()

    val value: Float get() = valueAnimation.value
    val targetValue: Float get() = valueAnimation.targetValue
    val pressProgress: Float get() = pressProgressAnimation.value
    val scaleX: Float get() = scaleXAnimation.value
    val scaleY: Float get() = scaleYAnimation.value
    val velocity: Float get() = velocityAnimation.value

    val modifier: Modifier =
        Modifier.pointerInput(Unit) {
            inspectDragGestures(
                onDragStart = { down ->
                    onDragStarted(down.position)
                    press()
                },
                onDragEnd = {
                    onDragStopped()
                    release()
                },
                onDragCancel = {
                    onDragStopped()
                    release()
                },
            ) { _, dragAmount ->
                onDrag(size, dragAmount)
            }
        }

    fun press() {
        velocityTracker.resetTracking()
        animationScope.launch {
            launch { pressProgressAnimation.animateTo(1f, pressProgressAnimationSpec) }
            launch { scaleXAnimation.animateTo(pressedScale, scaleXAnimationSpec) }
            launch { scaleYAnimation.animateTo(pressedScale, scaleYAnimationSpec) }
        }
    }

    fun release() {
        animationScope.launch {
            withFrameNanos {}
            if (value != targetValue) {
                val threshold = (valueRange.endInclusive - valueRange.start) * 0.025f
                snapshotFlow { valueAnimation.value }
                    .filter { abs(it - valueAnimation.targetValue) < threshold }
                    .first()
            }
            launch { pressProgressAnimation.animateTo(0f, pressProgressAnimationSpec) }
            launch { scaleXAnimation.animateTo(initialScale, scaleXAnimationSpec) }
            launch { scaleYAnimation.animateTo(initialScale, scaleYAnimationSpec) }
        }
    }

    fun updateValue(value: Float) {
        val target = value.coerceIn(valueRange.start, valueRange.endInclusive)
        animationScope.launch {
            valueAnimation.animateTo(target, valueAnimationSpec) { updateVelocity() }
        }
    }

    fun animateToValue(value: Float) {
        animationScope.launch {
            mutatorMutex.mutate {
                press()
                val target = value.coerceIn(valueRange.start, valueRange.endInclusive)
                launch { valueAnimation.animateTo(target, valueAnimationSpec) }
                if (velocity != 0f) {
                    launch { velocityAnimation.animateTo(0f, velocityAnimationSpec) }
                }
                release()
            }
        }
    }

    private fun updateVelocity() {
        velocityTracker.addPosition(SystemClock.uptimeMillis(), Offset(value, 0f))
        val targetVelocity =
            velocityTracker.calculateVelocity().x / (valueRange.endInclusive - valueRange.start)
        animationScope.launch { velocityAnimation.animateTo(targetVelocity, velocityAnimationSpec) }
    }
}
