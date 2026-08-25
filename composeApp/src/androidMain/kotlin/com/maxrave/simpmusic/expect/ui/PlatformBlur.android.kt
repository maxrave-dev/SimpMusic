package com.maxrave.simpmusic.expect.ui

import android.os.Build

// Modifier.blur is backed by RenderEffect, added in API 31 (Android 12, VERSION_CODES.S). Below
// that the modifier is a documented no-op, so the style has to be gated rather than degraded.
actual fun isLyricsBlurSupported(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
