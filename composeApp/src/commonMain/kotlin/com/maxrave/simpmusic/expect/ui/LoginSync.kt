package com.maxrave.simpmusic.expect.ui

import androidx.compose.runtime.Composable

/** Desktop shows the pairing QR and receives sign-ins; Android scans it and sends them. */
@Composable
expect fun LoginSyncDialog(onDismiss: () -> Unit)
