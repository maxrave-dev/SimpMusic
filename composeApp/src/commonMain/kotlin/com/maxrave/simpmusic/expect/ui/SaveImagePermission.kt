@file:Suppress("ktlint:standard:filename")

package com.maxrave.simpmusic.expect.ui

import androidx.compose.runtime.Composable

interface SaveImagePermissionRequester {
    /**
     * Runs the permission check, then reports the answer through the callback the requester was
     * built with — immediately when nothing needs asking, or after the system dialog closes.
     *
     * Always answers exactly once per call, so a caller can put the save itself in the callback
     * and never branch on a return value.
     */
    fun requestIfNeeded()
}

/**
 * Gate in front of writing an image where the user can see it.
 *
 * Only Android 9 and older need anything: `MediaStore` there still writes through shared storage
 * and demands `WRITE_EXTERNAL_STORAGE` at runtime. Android 10 brought scoped storage, where an
 * app writing its own new image into `MediaStore` needs no permission at all — so on 10 and up,
 * and on Desktop, this grants instantly rather than showing a dialog nobody should be asked.
 */
@Composable
expect fun rememberSaveImagePermission(onResult: (granted: Boolean) -> Unit): SaveImagePermissionRequester
