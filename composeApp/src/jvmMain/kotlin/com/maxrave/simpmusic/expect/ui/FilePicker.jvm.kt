package com.maxrave.simpmusic.expect.ui

import androidx.compose.runtime.Composable

@Composable
actual fun filePickerResult(
    mimeType: String,
    onResultUri: (String?) -> Unit,
): FilePickerLauncher =
    object : FilePickerLauncher {
        override fun launch() {
            onResultUri(null)
        }
    }

@Composable
actual fun fileSaverResult(
    fileName: String,
    mimeType: String,
    onResultUri: (String?) -> Unit,
): FilePickerLauncher =
    object : FilePickerLauncher {
        override fun launch() {
            onResultUri(null)
        }
    }