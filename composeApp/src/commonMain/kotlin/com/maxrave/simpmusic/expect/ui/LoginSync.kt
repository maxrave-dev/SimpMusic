package com.maxrave.simpmusic.expect.ui

import androidx.compose.runtime.Composable

/** Desktop and Android TV show the pairing QR and receive sign-ins; an Android phone scans it and sends them. */
@Composable
expect fun LoginSyncDialog(onDismiss: () -> Unit)
