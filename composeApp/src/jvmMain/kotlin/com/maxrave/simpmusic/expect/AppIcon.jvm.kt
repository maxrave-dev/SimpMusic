package com.maxrave.simpmusic.expect

// Desktop keeps its packaged icon: the Windows MSIX taskbar and Start icons come from the package manifest.
actual fun currentAppIcon(): AppIconStyle? = null

actual fun setAppIcon(style: AppIconStyle) = Unit
