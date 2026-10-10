package com.maxrave.simpmusic.ui.screen.player.content.applemusic

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.maxrave.simpmusic.expect.ui.toReadableBitmap
import com.maxrave.simpmusic.extension.smoothScrimBrush
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.random.Random

/*
 * The page behind the Apple Music MAIN view: the artwork's own colours reduced to a mesh and hung
 * from where the artwork stops. The colour directly under the picture is the colour the picture
 * ended on, so there is no join to hide.
 *
 * How it is built:
 *  - the artwork is averaged into a MESH_GRID square of means, read bottom-up, so row 0 is its
 *    bottom edge;
 *  - every row but row 0 is shifted sideways by a seeded amount, and mirrored half the time. A flip
 *    alone reads as a reflection on a cover with a face or a logo in it; the shift breaks that
 *    without putting any two colours side by side that were not neighbours in the artwork;
 *  - the grid is resampled to MESH_TEX with smoothstep, which is what makes it smooth. The blur only
 *    takes the last of the edge off.
 * Proportions survive on purpose: a cover that is mostly black leaves a mostly black page.
 */

/**
 * A mesh read off one artwork: a small texture, stretched by the sampler when it is drawn, and how
 * bright the artwork's top band is (0..1), which sets the wash under the status bar.
 */
@Immutable
internal class AppleMusicMesh(
    val image: ImageBitmap,
    val topLuminance: Float,
)

/**
 * The mesh for [artwork], or null until one has been read. [seed] fixes the sideways shift, so the
 * same artwork always lands in the same arrangement. The previous mesh stays until the new one is
 * ready, so a skip crossfades between two colourings instead of flashing the fallback between them.
 */
@Composable
internal fun rememberAppleMusicMesh(
    artwork: ImageBitmap?,
    seed: Int,
): AppleMusicMesh? {
    var mesh by remember { mutableStateOf<AppleMusicMesh?>(null) }
    LaunchedEffect(artwork, seed) {
        val source = artwork ?: return@LaunchedEffect
        // No suspension inside meshOf, so nothing here can swallow a cancellation.
        val read = withContext(Dispatchers.Default) { runCatching { meshOf(source, seed) }.getOrNull() }
        if (read != null) mesh = read
    }
    return mesh
}

/**
 * [mesh] hung from [seam], the Y where the artwork's bottom edge sits: above it the mesh's first row
 * is held (that stretch is behind the artwork, which dissolves into it), below it the mesh fills the
 * rest of the page. A new mesh crossfades in over [MESH_FADE_MS].
 */
@Composable
internal fun AppleMusicMeshBackdrop(
    mesh: AppleMusicMesh?,
    seam: Dp,
    modifier: Modifier = Modifier,
) {
    // Two meshes rather than one animated colour: what crossfades here is a pair of pictures, and
    // the outgoing one has to stay drawable until the fade has finished with it.
    var shown by remember { mutableStateOf(mesh) }
    var incoming by remember { mutableStateOf<AppleMusicMesh?>(null) }
    val fade = remember { Animatable(0f) }

    LaunchedEffect(mesh) {
        val next = mesh ?: return@LaunchedEffect
        // A change mid-fade: what was fading in has been on screen a while, so it is what the new
        // one fades FROM. Restarting from the mesh before it would never let a quick skip settle.
        incoming?.let { shown = it }
        incoming = null
        val current = shown
        if (next === current) return@LaunchedEffect
        if (current == null) {
            shown = next
            return@LaunchedEffect
        }
        incoming = next
        fade.snapTo(0f)
        fade.animateTo(1f, tween(MESH_FADE_MS, easing = FastOutSlowInEasing))
        shown = next
        incoming = null
    }

    Canvas(
        modifier =
            modifier
                .fillMaxSize()
                // Outside the blur, so it is an opaque floor the blur cannot thin out — the page is a
                // sheet over the rest of the app and every pixel of it has to be opaque.
                .background(MeshFallback)
                // The default edge treatment clamps; Unbounded would fade alpha in from every edge and
                // let the page underneath show through down the sides.
                .blur(MESH_BLUR),
    ) {
        val seamY = seam.toPx().coerceIn(0f, size.height)
        shown?.let { drawMesh(it, seamY, alpha = 1f) }
        // Read here, not in composition: an Animatable read in a draw lambda only redraws.
        incoming?.let { drawMesh(it, seamY, alpha = fade.value) }
        // Just enough to keep white text readable over a bright sleeve. The colours are the sleeve's
        // own, so a sleeve that ends dark should leave a dark page, not a greyed one.
        drawRect(brush = smoothScrimBrush(from = Color.Black.copy(alpha = 0.06f), to = Color.Black.copy(alpha = 0.30f)))
    }
}

private fun DrawScope.drawMesh(
    mesh: AppleMusicMesh,
    seamY: Float,
    alpha: Float,
) {
    if (alpha <= 0.001f) return
    val width = size.width.roundToInt()
    val seam = seamY.roundToInt()
    if (seam > 0) {
        drawImage(
            image = mesh.image,
            srcOffset = IntOffset.Zero,
            srcSize = IntSize(MESH_TEX, 1),
            dstOffset = IntOffset.Zero,
            dstSize = IntSize(width, seam),
            alpha = alpha,
            filterQuality = FilterQuality.Low,
        )
    }
    drawImage(
        image = mesh.image,
        srcOffset = IntOffset.Zero,
        srcSize = IntSize(MESH_TEX, MESH_TEX),
        dstOffset = IntOffset(0, seam),
        dstSize = IntSize(width, (size.height.roundToInt() - seam).coerceAtLeast(1)),
        alpha = alpha,
        filterQuality = FilterQuality.Low,
    )
}

private fun meshOf(
    artwork: ImageBitmap,
    seed: Int,
): AppleMusicMesh? {
    if (artwork.width < 1 || artwork.height < 1) return null
    // Averaged down to 36 cells, so resolution buys nothing. The height keeps the artwork's own
    // aspect: squashing one axis would reweight which part of the frame each cell is made of.
    val width = MESH_SOURCE_PX
    val height = (MESH_SOURCE_PX.toLong() * artwork.height / artwork.width).toInt().coerceIn(1, MESH_SOURCE_PX * 2)
    val pixels = IntArray(width * height)
    artwork.toReadableBitmap(width, height).readPixels(pixels)

    val cols = MESH_GRID
    val rows = MESH_GRID
    val cells = cols * rows
    val red = LongArray(cells)
    val green = LongArray(cells)
    val blue = LongArray(cells)
    val count = IntArray(cells)
    for (y in 0 until height) {
        // Flipped as it is read: row 0 of the grid is the artwork's bottom edge.
        val rowBase = ((height - 1 - y) * rows / height) * cols
        for (x in 0 until width) {
            val cell = rowBase + x * cols / width
            val pixel = pixels[y * width + x]
            red[cell] += ((pixel shr 16) and 0xFF).toLong()
            green[cell] += ((pixel shr 8) and 0xFF).toLong()
            blue[cell] += (pixel and 0xFF).toLong()
            count[cell]++
        }
    }
    val grid =
        IntArray(cells) { cell ->
            val n = count[cell].coerceAtLeast(1)
            argb((red[cell] / n).toInt(), (green[cell] / n).toInt(), (blue[cell] / n).toInt()).lifted()
        }
    // The grid's last row is the artwork's top band, read before any lifting.
    val topRow = (rows - 1) * cols until rows * cols
    val topPixels = topRow.sumOf { count[it].toLong() }.coerceAtLeast(1L)
    val topLuminance =
        (topRow.sumOf { 0.2126 * red[it] + 0.7152 * green[it] + 0.0722 * blue[it] } / topPixels / 255.0).toFloat().coerceIn(0f, 1f)
    val texels = grid.rotatedBelowSeam(cols, rows, seed).resampled(cols, rows, MESH_TEX)

    val image = ImageBitmap(MESH_TEX, MESH_TEX)
    val canvas = Canvas(image)
    val paint = Paint()
    for (y in 0 until MESH_TEX) {
        for (x in 0 until MESH_TEX) {
            paint.color = Color(texels[y * MESH_TEX + x])
            canvas.drawRect(x.toFloat(), y.toFloat(), x + 1f, y + 1f, paint)
        }
    }
    return AppleMusicMesh(image, topLuminance)
}

// Every row but the seam row, carried sideways by one seeded amount and mirrored half the time.
// A cyclic shift keeps each cell's neighbours, so no colour boundary is invented.
private fun IntArray.rotatedBelowSeam(
    cols: Int,
    rows: Int,
    seed: Int,
): IntArray {
    if (rows <= 1) return this
    val random = Random(seed)
    val mirror = random.nextBoolean()
    val shift = random.nextInt(cols)
    val out = copyOf()
    for (row in 1 until rows) {
        val base = row * cols
        for (x in 0 until cols) {
            val src = if (mirror) cols - 1 - x else x
            out[base + x] = this[base + (src + shift) % cols]
        }
    }
    return out
}

// Smoothstep between control points: bilinear alone leaves a crease at every cell boundary once the
// grid is magnified to a full screen; a curve that is flat at each control point does not.
private fun IntArray.resampled(
    cols: Int,
    rows: Int,
    size: Int,
): IntArray {
    val out = IntArray(size * size)
    for (ty in 0 until size) {
        val fy = (ty + 0.5f) / size * rows - 0.5f
        val y0 = floor(fy).toInt().coerceIn(0, rows - 1)
        val y1 = (y0 + 1).coerceAtMost(rows - 1)
        val wy = smoothstep(fy - y0)
        for (tx in 0 until size) {
            val fx = (tx + 0.5f) / size * cols - 0.5f
            val x0 = floor(fx).toInt().coerceIn(0, cols - 1)
            val x1 = (x0 + 1).coerceAtMost(cols - 1)
            val wx = smoothstep(fx - x0)
            val top = lerpArgb(this[y0 * cols + x0], this[y0 * cols + x1], wx)
            val bottom = lerpArgb(this[y1 * cols + x0], this[y1 * cols + x1], wx)
            out[ty * size + tx] = lerpArgb(top, bottom, wy)
        }
    }
    return out
}

private fun smoothstep(t: Float): Float {
    val x = t.coerceIn(0f, 1f)
    return x * x * (3f - 2f * x)
}

private fun lerpArgb(
    from: Int,
    to: Int,
    t: Float,
): Int {
    if (t <= 0f) return from
    if (t >= 1f) return to
    fun channel(shift: Int): Int {
        val a = (from shr shift) and 0xFF
        val b = (to shr shift) and 0xFF
        return (a + (b - a) * t).roundToInt().coerceIn(0, 255)
    }
    return argb(channel(16), channel(8), channel(0))
}

private fun argb(
    red: Int,
    green: Int,
    blue: Int,
): Int = (0xFF shl 24) or (red shl 16) or (green shl 8) or blue

// The one liberty taken with the colours: averaging greys a block (the mean of a red stripe and the
// black around it is a dull maroon), so saturation gets back roughly what averaging took, and
// lightness is floored just off pure black. Neither touches the proportions.
private fun Int.lifted(): Int {
    val r = ((this shr 16) and 0xFF) / 255f
    val g = ((this shr 8) and 0xFF) / 255f
    val b = (this and 0xFF) / 255f
    val maxC = max(r, max(g, b))
    val minC = min(r, min(g, b))
    val lightness = (maxC + minC) / 2f
    val delta = maxC - minC
    if (delta == 0f) {
        val grey = (lightness.coerceAtLeast(MESH_FLOOR) * 255f).roundToInt()
        return argb(grey, grey, grey)
    }
    val saturation = delta / (1f - abs(2f * lightness - 1f))
    val hue =
        when (maxC) {
            r -> ((g - b) / delta).mod(6f)
            g -> (b - r) / delta + 2f
            else -> (r - g) / delta + 4f
        } * 60f
    val color = Color.hsl(hue, (saturation * MESH_VIBRANCE).coerceIn(0f, 1f), lightness.coerceIn(MESH_FLOOR, 1f))
    return argb((color.red * 255f).roundToInt(), (color.green * 255f).roundToInt(), (color.blue * 255f).roundToInt())
}

/** How many cells across the mesh is: enough that a cover's layout survives, few enough that nothing recognisable does. */
private const val MESH_GRID = 6

/** What the grid is smoothed to before the GPU stretches it. */
private const val MESH_TEX = 32

/** What the artwork is read at before it is averaged — a whole multiple of [MESH_GRID]. */
private const val MESH_SOURCE_PX = 120

private const val MESH_VIBRANCE = 1.12f
private const val MESH_FLOOR = 0.045f

/** How long the page takes to change colour on a skip: long enough to read as a change, not a cut. */
private const val MESH_FADE_MS = 900

// The owner asked for the page to be visibly smooth, and at 44dp no cell edge survives even on a
// cover with hard colour blocks.
private val MESH_BLUR = 44.dp

/** Drawn only until an artwork has been read. */
private val MeshFallback = Color(0xFF121212)
