package com.maxrave.simpmusic.expect.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable

@Composable
actual fun filePickerResult(onResultUri: (String?) -> Unit): FilePickerLauncher {
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            onResultUri(uri?.toString())
        }
    return object : FilePickerLauncher {
        override fun launch() {
            // Any type: a converted playlist .json arrives with whatever MIME its source assigned it.
            launcher.launch(arrayOf("*/*"))
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
