@file:Suppress("ktlint:standard:filename")

package com.maxrave.simpmusic.expect.ui

import androidx.compose.ui.graphics.ImageBitmap

/**
 * Decodes encoded image bytes — whatever the picker handed back — into something Compose can draw.
 *
 * Returns null rather than throwing: the bytes come from a file the user chose, which can be a
 * format the platform decoder does not know, or truncated.
 */
expect fun decodeImageBitmap(bytes: ByteArray): ImageBitmap?

/**
 * Writes [bytes] into the app's own storage and returns a `file:` uri for it.
 *
 * Cropped images cannot stay at the uri the picker returned — that one points at the ORIGINAL,
 * uncropped file, and on Android the read permission granted for it does not survive a restart.
 * Returns null when the write fails, so the caller can keep the previous cover instead.
 */
expect suspend fun persistPickedImage(
    bytes: ByteArray,
    fileName: String,
): String?
