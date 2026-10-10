package com.maxrave.simpmusic.expect.ui

import androidx.compose.runtime.Composable

interface FilePickerLauncher {
    fun launch()
}

/**
 * Opens a chooser for one file of any type. [onResultUri] gets a content Uri on Android and a plain
 * file path on Desktop — what restoreNative and readPickedFile open on each — or null.
 */
@Composable
expect fun filePickerResult(onResultUri: (String?) -> Unit): FilePickerLauncher

@Composable
expect fun fileSaverResult(
    fileName: String,
    mimeType: String,
    onResultUri: (String?) -> Unit,
): FilePickerLauncher
