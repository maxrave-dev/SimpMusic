package com.maxrave.simpmusic.expect.ui

import androidx.compose.runtime.Composable

// Desktop has no single app-scoped "device volume" the way Android's STREAM_MUSIC is — the OS
// mixer sits outside the app. Null hides the Apple Music style's volume row on this platform.
@Composable
actual fun rememberDeviceVolumeController(): DeviceVolumeController? = null
