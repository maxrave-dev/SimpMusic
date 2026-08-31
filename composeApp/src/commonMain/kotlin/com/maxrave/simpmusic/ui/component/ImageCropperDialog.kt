package com.maxrave.simpmusic.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.maxrave.simpmusic.expect.ui.decodeImageBitmap
import com.maxrave.simpmusic.expect.ui.toByteArray
import kotlin.math.max
import kotlin.math.roundToInt
import androidx.compose.ui.graphics.Canvas as GraphicsCanvas

/**
 * Longest side of the image handed back. Big enough for any cover slot that renders it, small
 * enough that the upload stays one quick request rather than several megabytes of phone photo.
 */
private const val OUTPUT_SIZE = 1024

/** The visible area is taller than the square cut out of it, so the discarded part stays on show. */
private const val STAGE_ASPECT = 0.8f

/** Keeps the dialog a dialog on a desktop-sized window instead of stretching across it. */
private val MAX_DIALOG_WIDTH = 380.dp

/**
 * Square crop, because the surfaces this feeds are square — YouTube Music's playlist cover slot is
 * literally named `studio_square_thumbnail`. Handing it a 16:9 photo unchanged is what squashed
 * people's covers, so WHICH square is given back to the user rather than guessed.
 *
 * The picture moves under a fixed frame rather than the frame moving over the picture: the frame is
 * the thing whose shape matters, and keeping it still means the preview is exactly the result.
 */
@Composable
fun ImageCropperDialog(
    imageBytes: ByteArray,
    titleText: String,
    confirmText: String,
    cancelText: String,
    onDismiss: () -> Unit,
    onCropped: (ByteArray) -> Unit,
) {
    val source = remember(imageBytes) { decodeImageBitmap(imageBytes) }
    if (source == null) {
        onDismiss()
        return
    }

    var frameSizePx by remember { mutableStateOf(0) }
    var stageHeightPx by remember { mutableStateOf(0) }
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    // The smallest scale that still covers the frame; below it the crop would include corners the
    // image has no pixels for.
    val minScale =
        if (frameSizePx == 0) {
            1f
        } else {
            max(frameSizePx.toFloat() / source.width, frameSizePx.toFloat() / source.height)
        }

    // Platform default width, deliberately. Turning it off hands the dialog the whole window, and
    // on a desktop-sized window fillMaxWidth then produced a stage taller than the screen — the
    // title and the buttons ended up off-screen with nothing but a giant photo visible.
    Dialog(onDismissRequest = onDismiss) {
        // Surface, not a Column with a background modifier. A Compose Dialog is an empty window
        // with no surface of its own, so a background only paints the area the content happens to
        // occupy — everything else stays see-through onto whatever sits behind the dialog.
        Surface(
            modifier = Modifier.fillMaxWidth().widthIn(max = MAX_DIALOG_WIDTH),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = titleText,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
                // The stage is TALLER than the square being cut, on purpose. Clipping the picture
                // to the crop frame — which is what this did first — leaves nothing on screen but
                // a picture: no edge, no discarded area, no way to tell it is a cropper at all.
                // Showing the rest of the image dimmed is what makes the frame readable.
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .aspectRatio(STAGE_ASPECT)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black)
                            .onSizeChanged { size ->
                                if (size.width != frameSizePx && size.width > 0) {
                                    frameSizePx = size.width
                                    stageHeightPx = size.height
                                    scale =
                                        max(
                                            size.width.toFloat() / source.width,
                                            size.width.toFloat() / source.height,
                                        )
                                    offset =
                                        Offset(
                                            x = (size.width - source.width * scale) / 2f,
                                            y = (size.height - source.height * scale) / 2f,
                                        )
                                }
                            }.pointerInput(source) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    scale = (scale * zoom).coerceAtLeast(minScale)
                                    val drawnWidth = source.width * scale
                                    val drawnHeight = source.height * scale
                                    val frameTop = (stageHeightPx - frameSizePx) / 2f
                                    val moved = offset + pan
                                    // Clamped against the FRAME, not the stage: the picture may
                                    // hang off the dimmed area freely, but never leave a gap
                                    // inside the square that will actually be cut.
                                    offset =
                                        Offset(
                                            x = moved.x.coerceIn((frameSizePx - drawnWidth).coerceAtMost(0f), 0f),
                                            y =
                                                moved.y.coerceIn(
                                                    (frameTop + frameSizePx - drawnHeight).coerceAtMost(frameTop),
                                                    frameTop,
                                                ),
                                        )
                                }
                            },
                ) {
                    Canvas(modifier = Modifier.fillMaxWidth().aspectRatio(STAGE_ASPECT)) {
                        drawImage(
                            image = source,
                            dstOffset = IntOffset(offset.x.roundToInt(), offset.y.roundToInt()),
                            dstSize =
                                IntSize(
                                    (source.width * scale).roundToInt(),
                                    (source.height * scale).roundToInt(),
                                ),
                            filterQuality = FilterQuality.High,
                        )
                        // Everything outside the square, dimmed — this is the part being thrown
                        // away, and seeing it is the whole point of the taller stage.
                        val frameTop = (size.height - size.width) / 2f
                        val scrim = Color.Black.copy(alpha = 0.6f)
                        drawRect(color = scrim, size = Size(size.width, frameTop))
                        drawRect(
                            color = scrim,
                            topLeft = Offset(0f, frameTop + size.width),
                            size = Size(size.width, frameTop),
                        )
                        drawRect(
                            color = Color.White,
                            topLeft = Offset(0f, frameTop),
                            size = Size(size.width, size.width),
                            style = Stroke(width = 2.dp.toPx()),
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onDismiss) { Text(cancelText) }
                    TextButton(
                        onClick = {
                            val bytes =
                                cropToSquare(source, frameSizePx, stageHeightPx, scale, offset)?.toByteArray()
                            if (bytes != null) onCropped(bytes) else onDismiss()
                        },
                    ) { Text(confirmText) }
                }
            }
        }
    }
}

/**
 * Turns what the frame is showing into a square bitmap.
 *
 * The frame is measured in SCREEN pixels and the crop in IMAGE pixels, so every value is divided
 * back by [scale]. Reading the rectangle straight off the offsets would crop a region as many times
 * too small as the user had zoomed in.
 */
private fun cropToSquare(
    source: ImageBitmap,
    frameSizePx: Int,
    stageHeightPx: Int,
    scale: Float,
    offset: Offset,
): ImageBitmap? {
    if (frameSizePx <= 0 || scale <= 0f) return null
    // The square sits in the middle of a taller stage, so the offset has to be measured from the
    // frame's own top edge rather than the stage's.
    val frameTop = (stageHeightPx - frameSizePx) / 2f
    val srcX = (-offset.x / scale).roundToInt().coerceIn(0, source.width - 1)
    val srcY = ((frameTop - offset.y) / scale).roundToInt().coerceIn(0, source.height - 1)
    val srcSize =
        (frameSizePx / scale)
            .roundToInt()
            .coerceAtMost(minOf(source.width - srcX, source.height - srcY))
    if (srcSize <= 0) return null

    val output = ImageBitmap(OUTPUT_SIZE, OUTPUT_SIZE)
    GraphicsCanvas(output).drawImageRect(
        image = source,
        srcOffset = IntOffset(srcX, srcY),
        srcSize = IntSize(srcSize, srcSize),
        dstOffset = IntOffset.Zero,
        dstSize = IntSize(OUTPUT_SIZE, OUTPUT_SIZE),
        paint = Paint().apply { filterQuality = FilterQuality.High },
    )
    return output
}
