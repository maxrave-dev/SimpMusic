package com.maxrave.simpmusic.ui.screen.home.wrapped.cards

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade

/**
 * The vocabulary every Wrapped card is built from — one artwork treatment, one texture, one
 * eyebrow, one type ramp.
 *
 * It lives in a file of its own rather than inside the card that first needed it, because all ten
 * cards use it and two different authors extend it. A shared helper hidden in card 01 reads as
 * card 01's private business, which is how a second scanline pass at a slightly different alpha
 * gets written for card 10 and the reel quietly stops looking like one object.
 *
 * Everything here is `internal`: shared across the package, invisible outside it.
 */

/**
 * One artwork, always under the reel's scanlines.
 *
 * Every card that shows a photograph shows it through this, so a cover behind the opening mosaic
 * and a 30dp thumbnail in a list are visibly the same material. No placeholder and no error slot,
 * matching the app's other artwork call sites: a cover that has not arrived leaves the ground
 * colour showing, which under these scrims is indistinguishable from a very dark cover.
 */
@Composable
internal fun WrappedArtwork(
    url: String?,
    modifier: Modifier = Modifier,
) {
    AsyncImage(
        model =
            ImageRequest
                .Builder(LocalPlatformContext.current)
                .data(url)
                .diskCachePolicy(CachePolicy.ENABLED)
                .diskCacheKey(url ?: "")
                .crossfade(550)
                .build(),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier.wrappedScanlines(MaterialTheme.colorScheme.onSurface.copy(alpha = SCANLINE_ALPHA)),
    )
}

/**
 * Distinct by the *image*, not by the URL string.
 *
 * YouTube Music hands the same sleeve back under different links, because the requested size is
 * part of the URL: a track row carries `…=w544-h544`, the artist row for the same record carries
 * `…=w120-h120`, and the fallback shape is `i.ytimg.com/vi/<id>/maxresdefault.jpg` entirely. So
 * `distinct()` on the raw string keeps the same cover two or three times — invisible in a list,
 * unmissable in a mosaic, where it reads as the year having fewer records in it than it does.
 *
 * The app already normalises that size segment when it hands artwork to the media session
 * (`JvmMediaPlayerHandlerImpl`); this is the same rule applied to a comparison instead of to a
 * request, so nothing is rewritten — the first URL for each image is the one kept and loaded.
 */
internal fun List<String>.distinctByArtwork(): List<String> = distinctBy { it.artworkKey() }

/** Everything after `=w120-h120` is a rendering instruction, not part of the image's identity. */
private val ARTWORK_SIZE_SEGMENT = Regex("=w\\d+-h\\d+[^/]*$")

/** `i.ytimg.com/vi/<id>/<anything>.jpg` — the id alone identifies the image. */
private val ARTWORK_VIDEO_ID = Regex("/vi/(?<videoId>[^/]+)/")

private fun String.artworkKey(): String =
    ARTWORK_VIDEO_ID.find(this)?.groups?.get("videoId")?.value
        ?: ARTWORK_SIZE_SEGMENT.replace(this, "")

/**
 * A 1dp white hairline every 3dp, at 2.8% — the reel's one texture.
 *
 * It is what stops the cards reading as photographs with text on them: at this alpha nobody sees
 * lines, they see the artwork sitting *behind* something, which is the difference between a screen
 * and a printed object. Drawn as a single repeating-tile shader rather than a loop of drawLine, so
 * a full-bleed mosaic costs one rect per tile however tall the window gets.
 */
internal fun Modifier.wrappedScanlines(hairline: Color): Modifier =
    this.drawWithCache {
        val period = SCANLINE_PERIOD.toPx()
        val brush =
            Brush.linearGradient(
                0f to hairline,
                SCANLINE_DUTY to hairline,
                SCANLINE_DUTY to Color.Transparent,
                1f to Color.Transparent,
                start = Offset.Zero,
                end = Offset(0f, period),
                tileMode = TileMode.Repeated,
            )
        onDrawWithContent {
            drawContent()
            drawRect(brush)
        }
    }

/**
 * The small uppercase line that tells the card what it is about.
 *
 * `bodySmall` — 11sp, the smallest step in the app's own scale and the nearest one to the
 * artboards' 10px eyebrow; `labelSmall` is 14sp and reads as a subtitle rather than a label. Bold
 * is copied on because the scale's smallest step is Normal. The reel runs under
 * `ForceDarkContent`, so the colour arrives with the style and no card asks for one.
 *
 * Uppercased here rather than in the resource so translators receive an ordinary sentence, and
 * only ever on strings Wrapped alone renders: the axis and month labels it shares with the
 * Analytics screen stay as authored, or the same word would read two different ways in one app.
 */
@Composable
internal fun WrappedEyebrow(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text.uppercase(),
        style =
            MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.22.em,
            ),
        modifier = modifier,
    )
}

private val SCANLINE_PERIOD = 3.dp

/** One dp of the three-dp period is lit. */
private const val SCANLINE_DUTY = 1f / 3f

private const val SCANLINE_ALPHA = 0.028f
