package com.maxrave.simpmusic.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import com.kmpalette.loader.rememberNetworkLoader
import com.kmpalette.rememberDominantColorState
import com.maxrave.simpmusic.Platform
import com.maxrave.simpmusic.extension.angledGradientBackground
import com.maxrave.simpmusic.extension.artworkScrimBrush
import com.maxrave.simpmusic.extension.rgbFactor
import com.maxrave.simpmusic.getPlatform
import com.maxrave.simpmusic.ui.theme.desktopPanelDark
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.http.Url

/**
 * Home's ambient top glow for pages that have no artwork of their own: the theme's primary tone
 * fading into the page across the top 360dp, drawn with the same angled-gradient-plus-bottom-scrim
 * recipe HomeScreen paints under its first shelf (light theme lifts the tone toward white for a
 * pastel; dark keeps it deep via rgbFactor). Static, since the theme colour does not move.
 *
 * Emit it as the FIRST sibling of the screen's content: inside the nav host every destination
 * composes into a Box, so an earlier sibling is simply the layer underneath — no wrapper needed.
 *
 * The gradient tail aims at what is ACTUALLY painted behind the screen. The desktop shell wraps
 * content in its own panel colour rather than colorScheme.background, and a tail aimed at the
 * wrong one ends on a hard seam where the glow stops — see HomeScreen's pageBackground note.
 *
 * Screens that need more than this stay hand-rolled on purpose: Mix for you animates the tone from
 * its first playlist's artwork, and the Listen Together pair paint the glow inside a liquid-glass
 * backdrop source so their floating back button has something to refract.
 */
/** How tall the glow is — callers gate their bar frost on "has this fully scrolled past". */
val AmbientGlowHeight = 360.dp

@Composable
fun AmbientThemeGlow(
    modifier: Modifier = Modifier,
    // The RAW tone to glow with — an artwork colour (see [rememberNowPlayingGlowTint]) or the
    // theme primary, handed in as-is; light/dark shaping happens here. Null means NO GLOW: the
    // gradient collapses into the page colour, so the layer is simply invisible until a tone
    // arrives — and the animation below is what makes it breathe in rather than pop.
    tint: Color? = null,
) {
    val backgroundColor = MaterialTheme.colorScheme.background
    val isLightTheme = backgroundColor.luminance() > 0.5f
    val pageBackground =
        if (getPlatform() == Platform.Desktop) {
            if (isLightTheme) MaterialTheme.colorScheme.surfaceContainer else desktopPanelDark
        } else {
            backgroundColor
        }
    // Animated so a track change breathes to the new tone instead of snapping — Home's 500ms.
    val glow by animateColorAsState(
        targetValue =
            when {
                tint == null -> pageBackground
                isLightTheme -> lerp(tint, Color.White, 0.85f)
                // 0.45, not Home's 0.3: Home multiplies a mid-saturated ARTWORK colour, while a
                // pastel primary handed in here at 0.3 read as near-black.
                else -> tint.rgbFactor(0.45f)
            },
        animationSpec = tween(500),
        label = "ambientGlow",
    )
    Box(
        modifier
            .fillMaxWidth()
            .height(AmbientGlowHeight)
            .angledGradientBackground(listOf(glow, pageBackground), 25f),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(180.dp)
                .align(Alignment.BottomCenter)
                .background(artworkScrimBrush(pageBackground)),
        )
    }
}

/**
 * The dominant colour of the now-playing artwork, for feeding [AmbientThemeGlow]'s tint.
 *
 * The same DominantColorState + network-loader machinery HomeScreen runs on its main thumbnail.
 * Null while nothing is playing AND while the artwork has not resolved yet — deliberately not a
 * theme-colour fallback, so a silent app shows a plain page rather than a stand-in glow.
 * `Color.Unspecified` is the sentinel for "not resolved": the state must start somewhere, and any
 * real colour would flash as a wrong glow for the first frames.
 */
@Composable
fun rememberNowPlayingGlowTint(thumbnailUrl: String?): Color? {
    val networkLoader = rememberNetworkLoader(HttpClient(CIO))
    val dominantColorState =
        rememberDominantColorState(
            defaultColor = Color.Unspecified,
            defaultOnColor = Color.Unspecified,
            loader = networkLoader,
        )
    LaunchedEffect(thumbnailUrl) {
        thumbnailUrl?.let { dominantColorState.updateFrom(Url(it)) }
    }
    if (thumbnailUrl == null) return null
    return dominantColorState.color.takeIf { it.isSpecified }
}
