package com.maxrave.simpmusic.expect.ui

import android.os.Build
import android.view.RoundedCorner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp

@Composable
actual fun rememberDisplayCornerRadius(): Dp? {
    val context = LocalContext.current
    val density = LocalDensity.current
    return remember(context, density) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return@remember null
        // Read off the display rather than a window: a sheet's own window may not be attached yet.
        val px =
            runCatching { context.display?.getRoundedCorner(RoundedCorner.POSITION_BOTTOM_LEFT)?.radius }
                .getOrNull()
        if (px == null || px <= 0) null else with(density) { px.toDp() }
    }
}
