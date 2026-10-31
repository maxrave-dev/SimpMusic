package com.maxrave.simpmusic.expect.ui

import androidx.compose.runtime.Composable

/**
 * Listens through the microphone and names the song playing nearby. Android only: the search bar
 * offers it on Android alone, so Desktop's actual is never shown. [onFind] gets "title artist" once
 * the sheet has closed.
 */
@Composable
expect fun SongRecognitionSheet(
    onFind: (String) -> Unit,
    onDismiss: () -> Unit,
)
