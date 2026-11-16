package com.maxrave.simpmusic

actual fun getPlatform(): Platform = Platform.iOS

internal actual fun desktopOsName(): String = "jvm"
