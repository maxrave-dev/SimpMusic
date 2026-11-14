package com.maxrave.simpmusic.expect

import androidx.compose.runtime.Composable

/**
 * Platform-specific function to toggle fullscreen player window.
 * Only implemented on Desktop (JVM), no-op on other platforms.
 */
expect fun toggleFullscreenPlayer()

/**
 * Returns whether the fullscreen player window is currently open.
 * Always false on non-desktop platforms.
 */
@Composable
expect fun isFullscreenPlayerOpen(): Boolean

