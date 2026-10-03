package com.maxrave.simpmusic.ui.component.taste

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kmpalette.loader.rememberNetworkLoader
import com.kmpalette.rememberDominantColorState
import com.maxrave.domain.data.model.taste.TasteProfile
import com.maxrave.simpmusic.ui.component.ShareImageSheet
import com.maxrave.simpmusic.ui.component.lyrics.ShareCardSignature
import com.maxrave.simpmusic.ui.component.lyrics.ShareLyricsCardMaxWidth
import com.maxrave.simpmusic.ui.component.lyrics.ShareLyricsPalette
import com.maxrave.simpmusic.ui.component.lyrics.shareCardContentColor
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.http.Url
import org.jetbrains.compose.resources.stringResource
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.taste_card_subtitle
import simpmusic.composeapp.generated.resources.taste_card_title
import simpmusic.composeapp.generated.resources.taste_share_title

/**
 * The taste reading as an image to save or send, in the shared [ShareImageSheet].
 *
 * Same header, palette, pills and capture path as
 * [com.maxrave.simpmusic.ui.component.lyrics.ShareLyricsSheet], minus the line picker: there is
 * nothing to choose, the reading is the card. The surface is tinted from the top artist's picture
 * the way the lyrics sheet is tinted from the song's artwork.
 */
@Composable
fun ShareTasteSheet(
    profile: TasteProfile,
    onDismiss: () -> Unit,
) {
    val seedColor = rememberTasteSeedColor(profile.artistImages.firstOrNull())
    // Null follows the seed, which only arrives once the picture has been read; a swatch the user
    // picked in the meantime is not overwritten by it.
    var picked by remember { mutableStateOf<Color?>(null) }
    val cardBackground = picked ?: seedColor

    ShareImageSheet(
        title = stringResource(Res.string.taste_share_title),
        seedColor = seedColor,
        fileNamePrefix = "SimpMusic_taste",
        onDismiss = onDismiss,
        belowImage = { content ->
            ShareLyricsPalette(
                seedColor = seedColor,
                selected = cardBackground,
                content = content,
                onSelect = { picked = it },
            )
        },
    ) { captureModifier ->
        ShareTasteCard(
            profile = profile,
            background = cardBackground,
            modifier = captureModifier,
        )
    }
}

/**
 * The image that gets shared: who the reading is about, the reading, and whose app made it — the
 * same order and type sizes as the lyrics card, so the two read as one family.
 */
@Composable
private fun ShareTasteCard(
    profile: TasteProfile,
    background: Color,
    modifier: Modifier = Modifier,
) {
    val content = background.shareCardContentColor()
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .widthIn(max = ShareLyricsCardMaxWidth)
                .clip(RoundedCornerShape(20.dp))
                .background(Brush.verticalGradient(listOf(background, lerp(background, Color.Black, 0.28f))))
                .padding(24.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (profile.artistImages.isNotEmpty()) {
                ArtistStack(images = profile.artistImages, size = 38.dp, overlap = 12.dp, ring = background)
                Spacer(modifier = Modifier.width(12.dp))
            }
            Column {
                Text(
                    text = stringResource(Res.string.taste_card_title),
                    color = content,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(Res.string.taste_card_subtitle),
                    color = content.copy(alpha = 0.68f),
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Spacer(modifier = Modifier.height(26.dp))

        Text(
            text = profile.headline,
            color = content,
            fontSize = 22.sp,
            lineHeight = 30.sp,
            fontWeight = FontWeight.Bold,
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = profile.summary,
            color = content.copy(alpha = 0.86f),
            fontSize = 14.sp,
            lineHeight = 21.sp,
        )

        Spacer(modifier = Modifier.height(24.dp))

        ShareCardSignature(content = content, background = background)
    }
}

/**
 * The dominant colour of [imageUrl], read the way the Wrapped reel reads its seed.
 *
 * The client is remembered rather than built inline: the colour is read during composition, so a
 * fresh client every pass would key a fresh loader and state, and the colour would fall back on the
 * very recomposition it caused. It is closed with the sheet.
 */
@Composable
private fun rememberTasteSeedColor(imageUrl: String?): Color {
    val httpClient = remember { HttpClient(CIO) }
    DisposableEffect(httpClient) { onDispose { httpClient.close() } }
    val networkLoader = rememberNetworkLoader(httpClient)
    val dominantColorState =
        rememberDominantColorState(
            defaultColor = FALLBACK_SEED,
            defaultOnColor = FALLBACK_SEED,
            loader = networkLoader,
        )
    LaunchedEffect(imageUrl) {
        imageUrl?.takeIf { it.isNotBlank() }?.let { dominantColorState.updateFrom(Url(it)) }
    }
    return dominantColorState.color
}

/** The first fixed swatch of the palette, so a picture that cannot be read still lands on a choice. */
private val FALLBACK_SEED = Color(0xFF1F1F1F)
