package com.maxrave.simpmusic.extension

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import com.maxrave.domain.data.model.ui.ScreenSizeInfo
import platform.UIKit.UIApplication
import kotlin.math.roundToInt

@Composable
actual fun getScreenSizeInfo(): ScreenSizeInfo {
    val size = LocalWindowInfo.current.containerSize
    val density = LocalDensity.current
    return ScreenSizeInfo(with(density) { size.height.toDp().value.roundToInt() },
        with(density) { size.width.toDp().value.roundToInt() }, size.height, size.width)
}

@Composable
actual fun KeepScreenOn() {
    DisposableEffect(Unit) {
        val previous = UIApplication.sharedApplication.idleTimerDisabled
        UIApplication.sharedApplication.idleTimerDisabled = true
        onDispose { UIApplication.sharedApplication.idleTimerDisabled = previous }
    }
}

@Composable
actual fun rememberIsInPipMode(): Boolean = false
