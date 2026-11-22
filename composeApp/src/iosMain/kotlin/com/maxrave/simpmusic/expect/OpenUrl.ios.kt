package com.maxrave.simpmusic.expect

import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

actual fun openUrl(url: String) {
    val target = NSURL.URLWithString(url) ?: return
    dispatch_async(dispatch_get_main_queue()) {
        UIApplication.sharedApplication.openURL(target, emptyMap<Any?, Any>(), null)
    }
}

actual fun shareUrl(title: String, url: String) {
    val target = NSURL.URLWithString(url) ?: return
    dispatch_async(dispatch_get_main_queue()) { presentShare(listOf(title, target), title) }
}
