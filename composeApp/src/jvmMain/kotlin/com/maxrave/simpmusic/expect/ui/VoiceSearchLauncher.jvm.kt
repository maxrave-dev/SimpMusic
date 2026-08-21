package com.maxrave.simpmusic.expect.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberVoiceSearchLauncher(onResult: (String) -> Unit): () -> Unit {
    return remember {
        {
            // Desktop does not support native voice search Intent natively out of the box
        }
    }
}
