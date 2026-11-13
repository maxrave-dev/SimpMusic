@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package com.maxrave.simpmusic.expect.ui

import androidx.compose.runtime.*
import com.maxrave.simpmusic.expect.topViewController
import platform.Foundation.*
import platform.PhotosUI.*
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

@Composable
actual fun photoPickerResult(onResultUri: (String?) -> Unit): PhotoPickerLauncher {
    val callback by rememberUpdatedState(onResultUri)
    val delegate = remember { object : NSObject(), PHPickerViewControllerDelegateProtocol {
        override fun picker(picker: PHPickerViewController, didFinishPicking: List<*>) {
            picker.dismissViewControllerAnimated(true, null)
            val result = didFinishPicking.firstOrNull() as? PHPickerResult
            if (result == null) { callback(null); return }
            result.itemProvider.loadFileRepresentationForTypeIdentifier("public.image") { source, _ ->
                val output = source?.let {
                    val target = NSURL.fileURLWithPath(NSTemporaryDirectory() + NSUUID().UUIDString + "-" + (it.lastPathComponent ?: "image"))
                    if (NSFileManager.defaultManager.copyItemAtURL(it, target, null)) target.absoluteString else null
                }
                dispatch_async(dispatch_get_main_queue()) { callback(output) }
            }
        }
    } }
    return remember(delegate) { object : PhotoPickerLauncher {
        override fun launch() {
            val presenter = topViewController() ?: return callback(null)
            val configuration = PHPickerConfiguration().apply { selectionLimit = 1; filter = PHPickerFilter.imagesFilter }
            val picker = PHPickerViewController(configuration)
            picker.delegate = delegate
            presenter.presentViewController(picker, true, null)
        }
    } }
}
