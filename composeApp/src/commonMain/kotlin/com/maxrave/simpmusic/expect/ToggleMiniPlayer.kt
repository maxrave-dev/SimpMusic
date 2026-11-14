package com.maxrave.simpmusic.expect

/**
 * Platform-specific function to toggle mini player window.
 * Only implemented on Desktop (JVM), no-op on other platforms.
 */
expect fun toggleMiniPlayer()

/**
 * Platform-specific function to set mini player window state directly.
 * Only implemented on Desktop (JVM), no-op on other platforms.
 */
expect fun setMiniPlayerOpen(open: Boolean)

