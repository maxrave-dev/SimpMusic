package com.maxrave.simpmusic.expect.ui

// skiko implements Modifier.blur on every desktop target, with no version gate to respect. This is
// plain Compose blur, NOT haze's progressive path — that one throws NoSuchMethodError against the
// pinned Compose (see CLAUDE.md), and nothing here goes near it.
actual fun isLyricsBlurSupported(): Boolean = true
