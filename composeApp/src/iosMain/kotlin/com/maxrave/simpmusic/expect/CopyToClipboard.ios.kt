package com.maxrave.simpmusic.expect

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSThread
import platform.UIKit.UIPasteboard
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

@OptIn(ExperimentalForeignApi::class)
actual fun copyToClipboard(label: String, text: String) {
    // UIPasteboard has no Android-style clip label. UIKit access stays on the main thread.
    if (NSThread.isMainThread) {
        UIPasteboard.generalPasteboard.string = text
    } else {
        dispatch_async(dispatch_get_main_queue()) {
            UIPasteboard.generalPasteboard.string = text
        }
    }
}
