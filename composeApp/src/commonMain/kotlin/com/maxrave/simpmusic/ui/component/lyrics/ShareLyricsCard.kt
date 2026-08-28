package com.maxrave.simpmusic.ui.component.lyrics

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.painterResource
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.mono

/**
 * The card's preferred width — a ceiling, not a fixed size.
 *
 * Fixed would be nicer for turning out identical images, but 340dp does not fit inside the sheet's
 * own 20dp gutters on a 360dp phone, and the card would be clipped on exactly the devices this
 * feature is aimed at. The capture takes whatever width the card ends up at.
 */
val ShareLyricsCardMaxWidth = 340.dp

/**
 * Text that stays legible on [this] background.
 *
 * A luminance test rather than a fixed white, because the palette offers light backgrounds too —
 * and one of them is a near-white paper tone where white text would vanish completely.
 */
internal fun Color.shareCardContentColor(): Color = if (luminance() > 0.5f) Color(0xFF141414) else Color.White

/**
 * [this] artwork tint, pushed far enough from [fill] to stay readable on top of it.
 *
 * Needed wherever the UI inverts — a filled pill, a picked lyric line. Printing the raw tint there
 * is wrong and fails quietly: an artwork whose dominant colour sits mid-luminance lands about 2:1
 * against a white fill, which looks like a disabled control rather than a bug. Ramping it towards
 * black on a light fill (or towards white on a dark one) keeps the hue — which is the whole point
 * of tinting from the artwork — while forcing the lightness apart.
 *
 * 0.55 rather than 1.0 so the result still reads as the song's colour instead of plain black,
 * which is what Spotify's card does: dark red-brown text on white, never grey.
 */
internal fun Color.shareTintOn(fill: Color): Color =
    if (fill.luminance() > 0.5f) lerp(this, Color.Black, 0.55f) else lerp(this, Color.White, 0.55f)

/**
 * The image the user ends up sharing.
 *
 * Element order follows what both Spotify and YouTube Music settled on: who the song is, then the
 * words, then whose app made the card. The artwork is the track's already-decoded bitmap rather
 * than a URL — a card capture happens the instant the button is pressed, and an image still
 * loading over the network would be captured as a blank square.
 */
@Composable
internal fun ShareLyricsCard(
    lines: List<String>,
    songTitle: String,
    artistName: String,
    artwork: ImageBitmap?,
    background: Color,
    modifier: Modifier = Modifier,
) {
    val content = background.shareCardContentColor()
    val secondary = content.copy(alpha = 0.68f)

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .widthIn(max = ShareLyricsCardMaxWidth)
                .clip(RoundedCornerShape(20.dp))
                // Not a flat fill: Spotify's card ramps its tint top-to-bottom, which is what stops
                // a large block of one colour from reading as a coloured rectangle rather than a
                // designed object.
                .background(Brush.verticalGradient(listOf(background, lerp(background, Color.Black, 0.28f))))
                .padding(24.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (artwork != null) {
                Image(
                    bitmap = artwork,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(38.dp).clip(RoundedCornerShape(5.dp)),
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = songTitle,
                    color = content,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = artistName,
                    color = secondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Spacer(modifier = Modifier.height(26.dp))

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            lines.forEach { line ->
                Text(
                    text = line,
                    color = content,
                    fontSize = 22.sp,
                    lineHeight = 30.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            // Disc + glyph, the way Spotify signs its card. The circle is filled with the content
            // colour and the logo prints on it through shareTintOn — the same rule as every other
            // inverted surface here, so the mark keeps the song's hue and still reads on a
            // near-white palette swatch as well as on a dark one.
            Box(
                modifier = Modifier.size(14.dp).clip(CircleShape).background(content),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(Res.drawable.mono),
                    contentDescription = null,
                    tint = background.shareTintOn(content),
                    modifier = Modifier.size(10.dp),
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "SimpMusic",
                color = secondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}
