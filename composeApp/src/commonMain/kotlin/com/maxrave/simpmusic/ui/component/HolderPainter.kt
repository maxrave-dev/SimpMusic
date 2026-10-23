package com.maxrave.simpmusic.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.maxrave.simpmusic.ui.theme.LocalForceDarkText
import com.maxrave.simpmusic.ui.theme.LocalIsDarkTheme
import org.jetbrains.compose.resources.imageResource
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.holder
import simpmusic.composeapp.generated.resources.holder_light
import kotlin.math.min
import kotlin.math.roundToInt

/** The note's box inside the square holder art, measured from holder.png: centred, 51.2% wide, 65.7% tall. */
private const val NOTE_WIDTH = 0.512f
private const val NOTE_HEIGHT = 0.657f

/** Least room between the note and any edge, as a share of the frame's shorter side (owner's pick, 2026-10-20). */
private const val MIN_PADDING = 0.30f

/**
 * Theme-aware artwork placeholder that fills whatever frame it is drawn in.
 *
 * Immersive screens (Artist/Album/Playlist/LocalPlaylist/NowPlaying) mark their subtree with
 * [LocalForceDarkText] = true because they always sit on dark artwork; there the dark holder art is
 * kept. Everywhere else the placeholder follows the app theme, using the light variant in light mode
 * so it blends into the white page instead of showing a dark tile.
 *
 * It reports no size of its own, so it is drawn across the whole frame, whatever its shape: a square
 * row, a 16:9 video, a 2.4:1 artist banner on Desktop, a portrait tile. The holder's background fills
 * the frame and the note sits in its centre, as tall as the frame's height allows while keeping
 * [MIN_PADDING] from every edge. A frame that takes its size from the image (`heightIn`,
 * `wrapContentWidth`) would collapse around a size-less placeholder; pass [square] there to keep a
 * square box while the image loads.
 */
@Composable
fun rememberHolderPainter(square: Boolean = false): Painter {
    val dark = LocalForceDarkText.current || LocalIsDarkTheme.current
    val art = imageResource(if (dark) Res.drawable.holder else Res.drawable.holder_light)
    return remember(art, square) { HolderPainter(art, square) }
}

private class HolderPainter(
    private val art: ImageBitmap,
    square: Boolean,
) : Painter() {
    /** The art's own background, read off its corner so the fill around it always matches. */
    private val background = Color(IntArray(1).also { art.readPixels(it, width = 1, height = 1) }[0])

    override val intrinsicSize: Size =
        if (square) Size(art.width.toFloat(), art.height.toFloat()) else Size.Unspecified

    override fun DrawScope.onDraw() {
        drawRect(background)
        val padding = MIN_PADDING * min(size.width, size.height)
        // The art is as tall as the frame, unless that brings the note closer than the padding to an edge.
        val side =
            minOf(
                size.height,
                (size.width - 2 * padding) / NOTE_WIDTH,
                (size.height - 2 * padding) / NOTE_HEIGHT,
            )
        val sidePx = side.roundToInt()
        if (sidePx <= 0) return
        drawImage(
            image = art,
            dstOffset = IntOffset(((size.width - side) / 2).roundToInt(), ((size.height - side) / 2).roundToInt()),
            dstSize = IntSize(sidePx, sidePx),
        )
    }
}
