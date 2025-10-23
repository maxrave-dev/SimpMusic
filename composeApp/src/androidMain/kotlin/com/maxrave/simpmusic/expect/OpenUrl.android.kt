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

actual fun shareUrl(
    title: String,
    url: String,
) {
    val context: Context = getKoin().get()
    val shareIntent = Intent(Intent.ACTION_SEND)
    shareIntent.type = "text/plain"
    shareIntent.putExtra(Intent.EXTRA_TEXT, url)
    val chooserIntent =
        Intent.createChooser(shareIntent, title)
    context.startActivity(chooserIntent)
}