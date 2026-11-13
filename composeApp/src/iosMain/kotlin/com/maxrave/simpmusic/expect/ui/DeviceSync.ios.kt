package com.maxrave.simpmusic.expect.ui

import androidx.compose.runtime.Composable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton

@Composable
private fun UnsupportedDialog(onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss,
        text = { Text("Device pairing is unavailable on iOS.") },
        confirmButton = { TextButton(onClick = onDismiss) { Text("OK") } })
}
@Composable actual fun LoginSyncDialog(onDismiss: () -> Unit) = UnsupportedDialog(onDismiss)
@Composable actual fun LibrarySyncDialog(onDismiss: () -> Unit) = UnsupportedDialog(onDismiss)
actual fun restartAfterSyncedSettings() = Unit
@Composable actual fun SongRecognitionSheet(onFind: (String) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss,
        text = { Text("Song recognition is unavailable on iOS.") },
        confirmButton = { TextButton(onClick = onDismiss) { Text("OK") } })
}
