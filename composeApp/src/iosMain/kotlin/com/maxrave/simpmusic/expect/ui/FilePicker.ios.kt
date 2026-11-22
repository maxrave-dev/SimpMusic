@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package com.maxrave.simpmusic.expect.ui

import androidx.compose.runtime.*
import com.maxrave.simpmusic.expect.topViewController
import platform.Foundation.*
import platform.UIKit.*
import platform.darwin.NSObject

@Composable
actual fun filePickerResult(onResultUri: (String?) -> Unit): FilePickerLauncher {
    val callback by rememberUpdatedState(onResultUri)
    val delegate = remember { object : NSObject(), UIDocumentPickerDelegateProtocol {
        override fun documentPicker(controller: UIDocumentPickerViewController, didPickDocumentsAtURLs: List<*>) {
            callback((didPickDocumentsAtURLs.firstOrNull() as? NSURL)?.absoluteString)
        }
        override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) { callback(null) }
    } }
    return remember(delegate) { object : FilePickerLauncher {
        override fun launch() {
            val presenter = topViewController() ?: return callback(null)
            val picker = UIDocumentPickerViewController(documentTypes = listOf("public.data"), inMode = UIDocumentPickerMode.UIDocumentPickerModeImport)
            picker.delegate = delegate
            presenter.presentViewController(picker, true, null)
        }
    } }
}

@Composable
actual fun fileSaverResult(fileName: String, mimeType: String, onResultUri: (String?) -> Unit): FilePickerLauncher {
    val callback by rememberUpdatedState(onResultUri)
    return remember(fileName) { object : FilePickerLauncher {
        override fun launch() {
            // backupNative writes the archive here, then presents the system export sheet.
            val directory = NSTemporaryDirectory() + NSUUID().UUIDString
            if (!NSFileManager.defaultManager.createDirectoryAtPath(directory, true, null, null)) return callback(null)
            val name = fileName.substringAfterLast('/').substringAfterLast('\\')
            callback(NSURL.fileURLWithPath("$directory/$name").absoluteString)
        }
    } }
}
