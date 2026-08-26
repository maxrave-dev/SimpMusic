package com.maxrave.simpmusic.expect

/**
 * Writes [bytes] somewhere the user can find it later: the system gallery on Android, the
 * downloads folder on Desktop.
 *
 * Returns false rather than throwing, because every caller is a button press — the UI has to say
 * something either way, and an exception crossing back into a click handler would take the app
 * down instead.
 */
expect suspend fun saveImageToDevice(
    bytes: ByteArray,
    fileName: String,
): Boolean

/**
 * Hands [bytes] to whatever the platform uses to pass a file to another app.
 *
 * Android opens the system share sheet. Desktop has no such thing, so it writes the file out and
 * reveals it in the file manager — the closest honest equivalent, and the reason this returns a
 * plain Boolean instead of pretending a chooser appeared.
 */
expect suspend fun shareImage(
    bytes: ByteArray,
    fileName: String,
    chooserTitle: String,
): Boolean
