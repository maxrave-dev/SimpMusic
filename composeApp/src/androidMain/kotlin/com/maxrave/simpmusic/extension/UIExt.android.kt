package com.maxrave.simpmusic.extension

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Point
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.core.app.PictureInPictureModeChangedInfo
import androidx.core.util.Consumer
import com.maxrave.domain.data.model.ui.ScreenSizeInfo
import com.maxrave.logger.Logger

fun Context.getActivityOrNull(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }

    return null
}

@Suppress("DEPRECATION")
@Composable
actual fun getScreenSizeInfo(): ScreenSizeInfo {
    val context = LocalContext.current
    val activity = context.getActivityOrNull()
    val configuration = LocalConfiguration.current
    val localDensity = LocalDensity.current

    return remember(configuration) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowMetrics = activity?.windowManager?.currentWindowMetrics
            Logger.w("getScreenSizeInfo", "WindowMetrics: ${windowMetrics?.bounds?.height()}")
            ScreenSizeInfo(
                hDP = with(localDensity) { (windowMetrics?.bounds?.height())?.toDp()?.value?.toInt() ?: 0 },
                wDP = with(localDensity) { (windowMetrics?.bounds?.height())?.toDp()?.value?.toInt() ?: 0 },
                hPX = windowMetrics?.bounds?.height() ?: 0,
                wPX = windowMetrics?.bounds?.width() ?: 0,
            )
        } else {
            val point = Point()
            activity?.windowManager?.defaultDisplay?.getRealSize(point)
            Logger.w("getScreenSizeInfo", "WindowMetrics: ${point.y}")
            ScreenSizeInfo(
                hDP =
                    with(localDensity) {
                        point.y
                            .toDp()
                            .value
                            .toInt()
                    },
                wDP =
                    with(localDensity) {
                        point.x
                            .toDp()
                            .value
                            .toInt()
                    },
                hPX = point.y,
                wPX = point.x,
            )
        }
    }
}

@Composable
actual fun KeepScreenOn() {
    val currentView = LocalView.current
    DisposableEffect(Unit) {
        currentView.keepScreenOn = true
        onDispose {
            currentView.keepScreenOn = false
        }
    }
}

fun Context.findActivity(): ComponentActivity {
    var context = this
    while (context is ContextWrapper) {
        if (context is ComponentActivity) return context
        context = context.baseContext
    }
    throw IllegalStateException("Picture in picture should be called in the context of an Activity")
}

@Composable
actual fun rememberIsInPipMode(): Boolean {
    val activity = LocalContext.current.findActivity()
    var pipMode by remember { mutableStateOf(activity.isInPictureInPictureMode) }
    DisposableEffect(activity) {
        val observer =
            Consumer<PictureInPictureModeChangedInfo> { info ->
                pipMode = info.isInPictureInPictureMode
            }
        activity.addOnPictureInPictureModeChangedListener(
            observer,
        )
        onDispose { activity.removeOnPictureInPictureModeChangedListener(observer) }
    }
    return pipMode
}