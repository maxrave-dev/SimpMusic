package com.maxrave.simpmusic.expect.ui

import androidx.compose.runtime.*
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.maxrave.simpmusic.expect.photoWritePermission
import kotlinx.coroutines.launch

actual fun isLyricsBlurSupported(): Boolean = true
actual fun isWallpaperDynamicColorSupported(): Boolean = false
@Composable actual fun platformDynamicColorScheme(isDark: Boolean): ColorScheme? = null
@Composable actual fun SystemBarAppearanceEffect(isDark: Boolean) {
    SideEffect {
        com.maxrave.simpmusic.expect.activeWindow()?.overrideUserInterfaceStyle =
            if (isDark) platform.UIKit.UIUserInterfaceStyle.UIUserInterfaceStyleDark
            else platform.UIKit.UIUserInterfaceStyle.UIUserInterfaceStyleLight
    }
}
// Google Cast is optional, as on FOSS Android. AVPlayer provides AirPlay through iOS.
actual fun isPlatformCastAvailable(): Boolean = false
@Composable actual fun PlatformCastButton(modifier: Modifier, tint: Color) { }
@Composable actual fun rememberCastReceivers(discover: Boolean): CastReceivers = remember { CastReceivers(emptyList(), {}, {}) }
@Composable actual fun rememberDeviceVolumeController(): DeviceVolumeController? = null
@Composable actual fun rememberLocalNetworkPermission(): LocalNetworkPermissionRequester = remember {
    // iOS shows its own permission prompt on the first local-network connection.
    object : LocalNetworkPermissionRequester { override fun requestIfNeeded(url: String) {} }
}
@Composable actual fun rememberSaveImagePermission(onResult: (Boolean) -> Unit): SaveImagePermissionRequester {
    val callback by rememberUpdatedState(onResult)
    val scope = rememberCoroutineScope()
    return remember(scope) { object : SaveImagePermissionRequester {
        override fun requestIfNeeded() { scope.launch { callback(photoWritePermission()) } }
    } }
}
@Composable actual fun openEqResult(audioSessionId: Int): OpenEqLauncher = remember {
    // iOS has no external system equalizer activity. The app's own audio settings remain separate.
    object : OpenEqLauncher { override fun launch() {} }
}
