package com.maxrave.simpmusic.extension

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import com.maxrave.domain.data.model.ui.ScreenSizeInfo
import kotlin.math.roundToInt

@Composable
actual fun getScreenSizeInfo(): ScreenSizeInfo {
    val density = LocalDensity.current
    val window = LocalWindowInfo.current
    // Every OS keeps its own title bar, and native decorations sit outside containerSize.
    val contentHeightPx = window.containerSize.height
    return ScreenSizeInfo(
        hDP = with(density) {
            contentHeightPx.toDp().value.roundToInt()
        },
        wDP = with(density) {
            window.containerSize.width.toDp().value.roundToInt()
        },
        hPX = contentHeightPx,
        wPX = window.containerSize.width
    )
}

@Composable
actual fun KeepScreenOn() {
    // TODO: Implement if needed
}

@Composable
actual fun rememberIsInPipMode(): Boolean {
    return false
}