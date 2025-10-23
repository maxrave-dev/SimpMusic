package com.maxrave.simpmusic.expect

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import org.koin.mp.KoinPlatform.getKoin

actual fun copyToClipboard(
    label: String,
    text: String,
) {
    val context: Context = getKoin().get()
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(label, text)
    clipboardManager.setPrimaryClip(clip)
}