package com.maxrave.simpmusic.expect.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.HorizontalScrollbar
import androidx.compose.foundation.ScrollbarStyle
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.v2.ScrollbarAdapter
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.carousel.CarouselState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.maxrave.simpmusic.Platform
import com.maxrave.simpmusic.getPlatform
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val scrollBarStyle =
    ScrollbarStyle(
        minimalHeight = 8.dp,
        thickness = 4.dp,
        shape = RoundedCornerShape(8.dp),
        hoverDurationMillis = 300,
        unhoverColor = Color.Gray.copy(alpha = 0.2f),
        hoverColor = Color.Gray.copy(alpha = 0.6f),
    )

/**
 * Dragging the thumb sets the scroll position directly — no fling — so a snapping list or grid would
 * stop between items. The thumb reports its drag on the returned source; when it is let go, a
 * zero-velocity run of the container's own snap fling settles on the nearest item.
 */
@Composable
private fun rememberSnapOnThumbRelease(
    scrollState: ScrollableState,
    flingBehavior: FlingBehavior?,
): MutableInteractionSource {
    val interactionSource = remember { MutableInteractionSource() }
    if (flingBehavior != null) {
        LaunchedEffect(interactionSource, scrollState, flingBehavior) {
            interactionSource.interactions.collect { interaction ->
                if (interaction is DragInteraction.Stop || interaction is DragInteraction.Cancel) {
                    scrollState.scroll { with(flingBehavior) { performFling(0f) } }
                }
            }
        }
    }
    return interactionSource
}

@Composable
actual fun HorizontalScrollBar(
    modifier: Modifier,
    scrollState: LazyListState,
    flingBehavior: FlingBehavior?,
) {
    val interactionSource = rememberSnapOnThumbRelease(scrollState, flingBehavior)
    if (getPlatform() == Platform.Desktop) {
        HorizontalScrollbar(
            modifier = modifier,
            style = scrollBarStyle,
            adapter =
                rememberScrollbarAdapter(
                    scrollState = scrollState,
                ),
            interactionSource = interactionSource,
        )
    }
}

@Composable
actual fun HorizontalScrollBar(
    modifier: Modifier,
    scrollState: LazyGridState,
    flingBehavior: FlingBehavior?,
) {
    // LazyGridScrollbarAdapter reads the grid's orientation ONCE, in its constructor, and until the
    // grid's first measure the state reports the empty layout, whose orientation is Vertical. An
    // adapter created alongside the grid therefore treats a horizontal grid as vertical for good:
    // it measures rows against the height, finds nothing to scroll and draws no thumb. Create it
    // only once the grid has measured as horizontal.
    val measuredHorizontal by remember(scrollState) {
        derivedStateOf { scrollState.layoutInfo.orientation == Orientation.Horizontal }
    }
    val interactionSource = rememberSnapOnThumbRelease(scrollState, flingBehavior)
    if (getPlatform() == Platform.Desktop && measuredHorizontal) {
        HorizontalScrollbar(
            modifier = modifier,
            style = scrollBarStyle,
            // The LazyGridState overload is the v2 adapter (JVM name rememberScrollbarAdapter2),
            // taken by the v2 HorizontalScrollbar overload.
            adapter = rememberScrollbarAdapter(scrollState = scrollState),
            interactionSource = interactionSource,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
actual fun HorizontalScrollBar(
    modifier: Modifier,
    scrollState: CarouselState,
    itemCount: Int,
) {
    if (getPlatform() != Platform.Desktop || itemCount < 2) return
    // The carousel reports whole cards only, so the thumb glides to the next one instead of jumping.
    val offset = animateFloatAsState(scrollState.currentItem.toFloat(), label = "carouselScrollbar")
    // The thumb shows whole cards, so a drag is added up here instead: the slider adds each drag delta to the
    // offset it reads back, and a whole-card offset would round every few-pixel delta away. Null outside a drag.
    val dragPosition = remember { mutableStateOf<Double?>(null) }
    val interactionSource = remember { MutableInteractionSource() }
    LaunchedEffect(interactionSource, scrollState) {
        interactionSource.interactions.collect {
            when (it) {
                is DragInteraction.Start -> dragPosition.value = scrollState.currentItem.toDouble()
                is DragInteraction.Stop, is DragInteraction.Cancel -> dragPosition.value = null
            }
        }
    }
    val scope = rememberCoroutineScope()
    val adapter =
        remember(scrollState, itemCount) {
            object : ScrollbarAdapter {
                private var target = -1

                override val scrollOffset get() = offset.value.toDouble()
                override val contentSize get() = itemCount.toDouble()
                override val viewportSize get() = 1.0

                override suspend fun scrollTo(scrollOffset: Double) {
                    // A track click asks for the next card outright; a drag moves its own running total.
                    val position =
                        dragPosition.value
                            ?.let { (it + scrollOffset - this.scrollOffset).coerceIn(0.0, itemCount - 1.0) }
                            ?.also { dragPosition.value = it }
                            ?: scrollOffset
                    val item = position.roundToInt().coerceIn(0, itemCount - 1)
                    if (scrollState.isScrollInProgress && item == target) return
                    if (!scrollState.isScrollInProgress && item == scrollState.currentItem) return
                    target = item
                    // Launched, not awaited: the slider holds back the next drag delta until this returns.
                    scope.launch { scrollState.animateScrollToItem(item) }
                }
            }
        }
    HorizontalScrollbar(
        adapter = adapter,
        modifier = modifier,
        style = scrollBarStyle,
        interactionSource = interactionSource,
    )
}
