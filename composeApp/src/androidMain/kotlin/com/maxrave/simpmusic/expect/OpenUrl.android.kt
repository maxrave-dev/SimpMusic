package com.maxrave.simpmusic.expect

import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import androidx.core.net.toUri
import com.maxrave.simpmusic.MainActivity
import org.koin.mp.KoinPlatform.getKoin

actual fun openUrl(url: String) {
    val context: MainActivity = getKoin().get()
    val browserIntent =
        Intent(
            Intent.ACTION_VIEW,
            url.toUri(),
        )
    browserIntent.setFlags(FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(browserIntent)
}

actual fun shareUrl(
    title: String,
    url: String,
) {
    val context: MainActivity = getKoin().get()
    val shareIntent = Intent(Intent.ACTION_SEND)
    shareIntent.type = "text/plain"
    shareIntent.putExtra(Intent.EXTRA_TEXT, url)
    shareIntent.setFlags(FLAG_ACTIVITY_NEW_TASK)
    val chooserIntent =
        Intent.createChooser(shareIntent, title)
    context.startActivity(chooserIntent)
}