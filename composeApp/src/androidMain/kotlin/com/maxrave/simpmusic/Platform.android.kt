package com.maxrave.simpmusic

import android.app.UiModeManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import org.koin.mp.KoinPlatform.getKoin

actual fun getPlatform(): Platform = Platform.Android

// A device does not stop being a TV while the app runs.
private val tv: Boolean by lazy {
    val context: Context = getKoin().get()
    // Fire TV reports the television UI mode too; the leanback feature covers boxes that do not.
    context.getSystemService(UiModeManager::class.java)?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION ||
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
}

actual fun isTv(): Boolean = tv
