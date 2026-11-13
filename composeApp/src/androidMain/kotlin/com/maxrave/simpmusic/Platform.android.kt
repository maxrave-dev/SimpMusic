package com.maxrave.simpmusic

actual fun getPlatform(): Platform = Platform.Android

internal actual fun desktopOsName(): String = System.getProperty("os.name") ?: "jvm"
