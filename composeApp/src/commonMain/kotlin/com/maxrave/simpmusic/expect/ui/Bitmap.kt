package com.maxrave.simpmusic.expect.ui

import androidx.compose.ui.graphics.ImageBitmap
import coil3.Image

expect fun ImageBitmap.toByteArray(): ByteArray?

/**
 * PNG rather than the JPEG [toByteArray] produces.
 *
 * The share card is mostly flat colour behind crisp text, which is the exact case JPEG handles
 * worst — its 8x8 blocks ring around every glyph edge, and at the sizes these cards get viewed
 * the artefacts are plainly visible. PNG also keeps the card's rounded corners transparent
 * instead of filling them black.
 */
expect fun ImageBitmap.toPngByteArray(): ByteArray?

expect fun Image.toImageBitmap(): ImageBitmap
