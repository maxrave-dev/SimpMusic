@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package com.maxrave.simpmusic.expect

import com.maxrave.data.io.fileSystem
import kotlinx.cinterop.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okio.Path.Companion.toPath
import platform.Foundation.*
import platform.Photos.*
import kotlin.coroutines.resume

internal suspend fun photoWritePermission(): Boolean = suspendCancellableCoroutine { continuation ->
    PHPhotoLibrary.requestAuthorizationForAccessLevel(PHAccessLevelAddOnly) { status ->
        if (continuation.isActive) continuation.resume(status == PHAuthorizationStatusAuthorized || status == PHAuthorizationStatusLimited)
    }
}

actual suspend fun saveImageToDevice(bytes: ByteArray, fileName: String): Boolean {
    if (bytes.isEmpty() || !photoWritePermission()) return false
    val data = bytes.usePinned { NSData.create(bytes = it.addressOf(0), length = bytes.size.toULong()) }
    return suspendCancellableCoroutine { continuation ->
        PHPhotoLibrary.sharedPhotoLibrary().performChanges({
            PHAssetCreationRequest.creationRequestForAsset().addResourceWithType(PHAssetResourceTypePhoto, data, null)
        }) { success, _ -> if (continuation.isActive) continuation.resume(success) }
    }
}

actual suspend fun shareImage(bytes: ByteArray, fileName: String, chooserTitle: String, mimeType: String): Boolean {
    val path = withContext(Dispatchers.IO) {
        runCatching {
            val directory = (NSTemporaryDirectory() + "sharing/" + NSUUID().UUIDString).toPath()
            fileSystem().createDirectories(directory)
            val name = fileName.substringAfterLast('/').substringAfterLast('\\')
            require(name.isNotBlank() && name != "." && name != "..")
            (directory / name).also { fileSystem().write(it) { write(bytes) } }
        }.getOrNull()
    } ?: return false
    return withContext(Dispatchers.Main) {
        presentShare(listOf(NSURL.fileURLWithPath(path.toString())), chooserTitle)
    }
}
