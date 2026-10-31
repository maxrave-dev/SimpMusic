package com.maxrave.simpmusic.expect.ui

import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.carousel.CarouselState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun HorizontalScrollBar(
    modifier: Modifier,
    scrollState: LazyListState,
    flingBehavior: FlingBehavior?,
) {
}

@Composable
actual fun HorizontalScrollBar(
    modifier: Modifier,
    scrollState: LazyGridState,
    flingBehavior: FlingBehavior?,
) {
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
actual fun HorizontalScrollBar(
    modifier: Modifier,
    scrollState: CarouselState,
    itemCount: Int,
) {
}
