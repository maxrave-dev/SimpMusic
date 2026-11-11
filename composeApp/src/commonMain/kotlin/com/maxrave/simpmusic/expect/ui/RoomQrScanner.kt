package com.maxrave.simpmusic.expect.ui

import androidx.compose.runtime.Composable

/**
 * Reads a Listen Together room's QR code. Android scans with the camera in a bottom sheet, and keeps
 * scanning past a code that is not a room. Desktop has no camera API, so it reads the code from an
 * image file the user picks, and [onUnreadable] means that image held no room.
 */
@Composable
expect fun RoomQrScanner(
    onRoomCode: (String) -> Unit,
    onUnreadable: () -> Unit,
    onDismiss: () -> Unit,
)
