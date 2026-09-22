package com.maxrave.simpmusic.expect.ui

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.maxrave.simpmusic.Platform
import com.maxrave.simpmusic.getPlatform

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
