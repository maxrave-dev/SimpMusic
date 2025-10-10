package com.maxrave.simpmusic.expect

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import org.koin.mp.KoinPlatform.getKoin

actual fun openUrl(url: String) {
    val context: Context = getKoin().get()
    val browserIntent =
        Intent(
            Intent.ACTION_VIEW,
            "https://simpmusic.org/download".toUri(),
        )
    context.startActivity(browserIntent)
}