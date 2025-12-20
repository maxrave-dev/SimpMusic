package com.maxrave.simpmusic.expect.ui

import androidx.compose.foundation.HorizontalScrollbar
import androidx.compose.foundation.ScrollbarStyle
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.maxrave.simpmusic.Platform
import com.maxrave.simpmusic.getPlatform

@Composable
actual fun HorizontalScrollBar(
    modifier: Modifier,
    scrollState: LazyListState,
) {
    if (getPlatform() == Platform.Desktop) {
        HorizontalScrollbar(
            modifier = modifier,
            style =
                ScrollbarStyle(
                    minimalHeight = 8.dp,
                    thickness = 4.dp,
                    shape = RoundedCornerShape(8.dp),
                    hoverDurationMillis = 300,
                    unhoverColor = Color.Gray.copy(alpha = 0.2f),
                    hoverColor = Color.Gray.copy(alpha = 0.6f),
                ),
            adapter =
                rememberScrollbarAdapter(
                    scrollState = scrollState,
                ),
        )
    }
}