package com.maxrave.simpmusic.ui.screen.player

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.maxrave.simpmusic.extension.findActivity

@Composable
actual fun FullScreenRotationImmersive(
    onLaunch: () -> Unit,
    onDispose: () -> Unit,
) {
    val resources = LocalResources.current
    val context = LocalContext.current

    val originalOrientation by rememberSaveable {
        mutableIntStateOf(resources.configuration.orientation)
    }

    DisposableEffect(true) {
        onLaunch.invoke()
        val activity = context.findActivity()
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        val window = context.findActivity().window
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)

        onDispose {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

            insetsController.apply {
                show(WindowInsetsCompat.Type.systemBars())
                systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
            // restore original orientation when view disappears
            activity.requestedOrientation =
                when (originalOrientation) {
                    Configuration.ORIENTATION_PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    Configuration.ORIENTATION_LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                    else -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                }
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            onDispose.invoke()
        }
    }

    LaunchedEffect(true) {
        val activity = context.findActivity()
        val window = activity.window

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}