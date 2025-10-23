package com.maxrave.simpmusic.expect.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable

@Composable
actual fun filePickerResult(
    mimeType: String,
    onResultUri: (String?) -> Unit,
): FilePickerLauncher {
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                onResultUri(uri.toString())
            }
        }
    return object : FilePickerLauncher {
        override fun launch() {
            launcher.launch(arrayOf(mimeType))
        }
    }
}

@Composable
actual fun fileSaverResult(
    fileName: String,
    mimeType: String,
    onResultUri: (String?) -> Unit,
): FilePickerLauncher {
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(mimeType)) { uri ->
            if (uri != null) {
                onResultUri(uri.toString())
            }
        }
    return object : FilePickerLauncher {
        override fun launch() {
            launcher.launch(fileName)
        }
    }
}