package com.maxrave.simpmusic.expect.ui

/**
 * Whether this platform can actually render `Modifier.blur`.
 *
 * Named after the capability, not the OS version, because the answer differs in kind between
 * targets: Android gained the backing `RenderEffect` in API 31 and **silently ignores** the
 * modifier below that — no crash, no warning, just a page with no blur — while Desktop draws it
 * through skiko regardless of any Android version.
 *
 * Used to hide the Apple Music lyrics style from Settings where it would render as a plain
 * mostly-transparent list, which is not the design.
 */
expect fun isLyricsBlurSupported(): Boolean
