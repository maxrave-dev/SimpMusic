package com.maxrave.simpmusic.expect

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask

@OptIn(ExperimentalForeignApi::class)
actual fun getDownloadFolderPath(): String {
    // iOS has no writable public Downloads directory. Keep music inside the app sandbox.
    val documents = NSSearchPathForDirectoriesInDomains(
        NSDocumentDirectory,
        NSUserDomainMask,
        true,
    ).firstOrNull() as? String ?: error("The app's Documents directory is unavailable")
    val downloads = "$documents/Downloads"
    check(NSFileManager.defaultManager.createDirectoryAtPath(downloads, true, null, null)) {
        "Cannot create the app's Downloads directory"
    }
    return downloads
}
