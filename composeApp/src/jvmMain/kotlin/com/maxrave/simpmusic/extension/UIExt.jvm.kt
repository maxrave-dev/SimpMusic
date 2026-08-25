package com.maxrave.simpmusic.extension

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import com.maxrave.domain.data.model.ui.ScreenSizeInfo
import kotlin.math.roundToInt

/**
 * Main-window chrome facts published once by DesktopApp. The custom title bar is drawn INSIDE
 * the window (above the app content), so `LocalWindowInfo.containerSize` includes it — every
 * hDP-based layout (most visibly the player's one-screen fold math) overflowed by exactly the
 * bar height until it is subtracted here. In VMs the bar is not mounted (native decorations
 * live outside containerSize) and nothing must be subtracted. Only the main window ever calls
 * [getScreenSizeInfo]; the mini-player window does not.
 */
object DesktopWindowChrome {
    const val TITLE_BAR_HEIGHT_DP = 40

    @Volatile
    var customTitleBarVisible: Boolean = false
}

@Composable
actual fun getScreenSizeInfo(): ScreenSizeInfo {
    val density = LocalDensity.current
    val window = LocalWindowInfo.current
    val chromeTopPx =
        if (DesktopWindowChrome.customTitleBarVisible) {
            with(density) { DesktopWindowChrome.TITLE_BAR_HEIGHT_DP.dp.roundToPx() }
        } else {
            0
        }
    val contentHeightPx = (window.containerSize.height - chromeTopPx).coerceAtLeast(0)
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