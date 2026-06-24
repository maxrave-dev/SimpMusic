package com.maxrave.simpmusic.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.unit.dp
import com.maxrave.simpmusic.expect.ui.PlatformBackdrop
import org.jetbrains.compose.resources.DrawableResource

/**
 * Applies the SimpMusic liquid-glass effect to any element.
 *
 * This is the single platform-specific primitive behind the glass buttons that
 * used to be hand-wired inline on every screen. It encapsulates the per-surface
 * [androidx.compose.ui.graphics.layer.GraphicsLayer], the Kyant `drawBackdrop`
 * effect stack and — on Android — the press/hold "liquid" interaction (a slight
 * scale-up, deeper refraction and a radial glow that follows the finger, springing
 * back on release). The press gesture is observe-only, so wrapped click handlers
 * keep working.
 *
 * The element MUST be a sibling of the backdrop source (the box carrying
 * [com.maxrave.simpmusic.expect.ui.layerBackdrop]); nesting it inside the source
 * creates a render-feedback loop that crashes the RuntimeShader.
 *
 * On desktop (JVM) the underlying backdrop modifiers are no-ops, so this only
 * clips to [shape] and the look degrades gracefully.
 *
 * @param interactive Android only — set false for a static glass surface.
 */
@Composable
expect fun Modifier.liquidGlass(
    backdrop: PlatformBackdrop,
    shape: Shape = CircleShape,
    interactive: Boolean = true,
): Modifier

/**
 * Overload of [liquidGlass] for surfaces that sample their own background luminance
 * (e.g. the MiniPlayer and the bottom bar capsule): the caller owns the [layer] the
 * glass records into and drives [luminanceAnimation], so the glass keeps adapting to
 * the content behind it — unlike the [liquidGlass] above, which uses a fixed
 * mid-luminance. On Android it adds the same press/hold interaction; on desktop it
 * degrades to a plain clip.
 */
@Composable
expect fun Modifier.liquidGlass(
    backdrop: PlatformBackdrop,
    layer: GraphicsLayer,
    luminanceAnimation: Float,
    shape: Shape = CircleShape,
    interactive: Boolean = true,
): Modifier

/**
 * A liquid-glass surface wrapping arbitrary [content] (e.g. a pill of icon
 * buttons). Thin convenience over [liquidGlass]; pure common code.
 */
@Composable
fun LiquidGlassContainer(
    backdrop: PlatformBackdrop,
    modifier: Modifier = Modifier,
    shape: Shape = CircleShape,
    interactive: Boolean = true,
    contentAlignment: Alignment = Alignment.Center,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier.liquidGlass(backdrop, shape, interactive),
        contentAlignment = contentAlignment,
        content = content,
    )
}

/**
 * Convenience wrapper around [LiquidGlassContainer] for the common single-icon
 * case (e.g. the circular back button shared by the detail screens).
 */
@Composable
fun LiquidGlassIconButton(
    backdrop: PlatformBackdrop,
    resId: DrawableResource,
    modifier: Modifier = Modifier.size(48.dp),
    shape: Shape = CircleShape,
    tint: Color = Color.White,
    interactive: Boolean = true,
    onClick: () -> Unit,
) {
    LiquidGlassContainer(
        backdrop = backdrop,
        modifier = modifier,
        shape = shape,
        interactive = interactive,
    ) {
        RippleIconButton(
            resId = resId,
            tint = tint,
            onClick = onClick,
        )
    }
}
