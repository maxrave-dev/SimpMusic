package com.maxrave.simpmusic.expect.ui

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable

/** Wallpaper-based (Material You) color scheme, or null when the platform can't provide one. */
@Composable
expect fun platformDynamicColorScheme(isDark: Boolean): ColorScheme?

/** Whether this platform can derive a color scheme from the system wallpaper. */
expect fun isWallpaperDynamicColorSupported(): Boolean

/** Keeps the system bar icon appearance in sync with the app theme (Android only; no-op elsewhere). */
@Composable
expect fun SystemBarAppearanceEffect(isDark: Boolean)
