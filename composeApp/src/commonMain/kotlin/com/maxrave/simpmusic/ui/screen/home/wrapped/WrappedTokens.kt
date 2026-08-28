package com.maxrave.simpmusic.ui.screen.home.wrapped

import androidx.compose.ui.unit.dp

/**
 * The only things Wrapped defines for itself: geometry the shell and the cards must agree on, and
 * how long a card holds.
 *
 * **There are no colours and no text sizes here, deliberately.** An earlier version of this file
 * carried a literal palette and a bespoke type ramp, and every card was then built out of raw
 * `Box` + border rather than the app's own surfaces — which is how a feature ends up looking like
 * it came from somewhere else. Wrapped takes:
 *
 * - **Colour** from `MaterialTheme.colorScheme`, inside the `MaterialExpressiveTheme` the shell
 *   wraps the reel in, whose scheme is `rememberDynamicColorScheme(seed = artwork, isDark = true,
 *   style = PaletteStyle.Vibrant)` — the same construction the M3 Expressive player uses.
 * - **Type** from `MaterialTheme.typography`, i.e. `typo()` in `ui/theme/Typo.kt`. The reel runs
 *   under `ForceDarkContent`, so that scale already resolves to white titles over muted body copy
 *   without any card asking for a colour.
 * - **Surfaces** from the app's own components — `LiquidGlassIconButton`, `LiquidGlassContainer`
 *   and `liquidGlass` over a `rememberBackdrop(...)` for anything floating over a card, and real
 *   Material 3 buttons for anything the user presses.
 *
 * A card that needs a size outside the type scale (the one enormous figure a card is built around)
 * takes `MaterialTheme.typography.displayLarge` and copies a font size onto it, so the family, the
 * weight relationship and the colour still come from the theme.
 */
object WrappedTokens {
    /** Chrome geometry the shell owns. Cards get the space between [HeaderHeight] and [FooterHeight]. */
    val ScreenPadding = 20.dp
    val HeaderHeight = 72.dp
    val FooterHeight = 74.dp
    val SegmentHeight = 3.dp
    val SegmentGap = 3.dp

    /**
     * The rim width for the small round glass buttons.
     *
     * `Highlight`'s default is DIRECTIONAL — lit along one angle — which an elongated pill catches
     * along its long edge and a 48dp circle barely catches at all. 1.dp is the smallest step that
     * stays visible without reading as a plain border, and is the value the detail screens settled
     * on; copy it rather than re-deriving it.
     */
    val GlassRimWidth = 1.dp

    /** How long each card holds before the reel advances on its own. */
    const val CARD_DURATION_MS = 6_000L

    /** The share card holds longer — it is the one the user is meant to act on. */
    const val SHARE_CARD_DURATION_MS = 30_000L
}
