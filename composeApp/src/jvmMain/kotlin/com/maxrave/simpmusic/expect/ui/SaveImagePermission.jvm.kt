package com.maxrave.simpmusic.expect.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState

/** Writing into the user's own downloads folder needs no permission on any desktop OS. */
@Composable
actual fun rememberSaveImagePermission(onResult: (granted: Boolean) -> Unit): SaveImagePermissionRequester {
    val currentOnResult by rememberUpdatedState(onResult)
    return remember {
        object : SaveImagePermissionRequester {
            override fun requestIfNeeded() = currentOnResult(true)
        }
    }
}
