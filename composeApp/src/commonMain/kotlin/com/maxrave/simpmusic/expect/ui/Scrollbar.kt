package com.maxrave.simpmusic.expect.ui

import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** [flingBehavior]: the list's own snap fling, run once the thumb is let go so a drag ends on an item. */
@Composable
expect fun HorizontalScrollBar(
    modifier: Modifier,
    scrollState: LazyListState,
    flingBehavior: FlingBehavior? = null,
)

/** [flingBehavior]: the grid's own snap fling, run once the thumb is let go so a drag ends on a column. */
@Composable
expect fun HorizontalScrollBar(
    modifier: Modifier,
    scrollState: LazyGridState,
    flingBehavior: FlingBehavior? = null,
)
