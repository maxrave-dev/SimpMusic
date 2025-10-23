package com.maxrave.simpmusic.expect

import multiplatform.network.cmptoast.showToast
import java.awt.Desktop
import java.awt.Toolkit
import java.awt.datatransfer.Clipboard
import java.awt.datatransfer.StringSelection
import java.net.URI

actual fun openUrl(url: String) {
    if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
        Desktop.getDesktop().browse(URI(url))
    }
}

actual fun shareUrl(
    title: String,
    url: String,
) {
    val stringSelection = StringSelection(url)
    val clipboard: Clipboard = Toolkit.getDefaultToolkit().systemClipboard
    clipboard.setContents(stringSelection, null)
    showToast("Copied to clipboard")
}