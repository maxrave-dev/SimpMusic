package com.maxrave.simpmusic.expect

import android.content.Context
import android.content.res.Configuration
import org.koin.mp.KoinPlatform.getKoin

actual fun currentOrientation(): Orientation {
    val context: Context = getKoin().get()
    val orientation = context.resources.configuration.orientation
    return when (orientation) {
        Configuration.ORIENTATION_PORTRAIT -> Orientation.PORTRAIT
        Configuration.ORIENTATION_LANDSCAPE -> Orientation.LANDSCAPE
        else -> Orientation.UNSPECIFIED
    }
}