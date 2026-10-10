package com.maxrave.simpmusic.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.kyant.backdrop.highlight.Highlight
import com.maxrave.simpmusic.expect.ui.layerBackdrop
import com.maxrave.simpmusic.expect.ui.rememberBackdrop
import com.maxrave.simpmusic.ui.icon.Close
import com.maxrave.simpmusic.ui.icon.SimpIcons

/** True while App.kt has a dialog of its own on screen. Home's launch banner waits until it is closed. */
val LocalAppDialogOpen = compositionLocalOf { false }

/** True while App.kt's Now Playing sheet is open. The launch banner never shows over it. */
val LocalNowPlayingOpen = compositionLocalOf { false }

/**
 * True once the user has touched or scrolled anything in the app this launch, or opened Now Playing.
 * A launch banner that has not had its turn yet then waits for a later launch.
 */
val LocalUserStarted = compositionLocalOf { false }

// Banners are portrait 720×1280 pictures; one of another shape is fitted, centred, inside this frame.
private const val BANNER_ASPECT_RATIO = 9f / 16f

data class PromoBannerData(
    val id: String,
    val imageUrl: String,
    val link: String,
)

/**
 * The launch banner: the picture alone, loaded by Coil like any other image, with the app's usual
 * placeholder while it loads. Tapping it opens [onOpen]; only the Wrapped glass button closes it, not
 * a tap outside and not Back. A picture that cannot be loaded calls [onFailed] instead of leaving an
 * empty frame on screen.
 */
@Composable
fun PromoBanner(
    banner: PromoBannerData,
    onOpen: () -> Unit,
    onClose: () -> Unit,
    onFailed: () -> Unit,
) {
    Dialog(
        // Only the X closes it.
        onDismissRequest = {},
        properties =
            DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = false,
                dismissOnClickOutside = false,
            ),
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            // As wide as the window allows, but never taller than three quarters of it.
            val width = min(maxWidth * 0.82f, maxHeight * 0.75f * BANNER_ASPECT_RATIO)
            val backdrop = rememberBackdrop(Color.Black)
            Box(modifier = Modifier.size(width, width / BANNER_ASPECT_RATIO)) {
                AsyncImage(
                    model = banner.imageUrl,
                    contentDescription = null,
                    placeholder = rememberHolderPainter(),
                    contentScale = ContentScale.Fit,
                    alignment = Alignment.Center,
                    onError = { onFailed() },
                    modifier =
                        Modifier
                            .matchParentSize()
                            .clip(MaterialTheme.shapes.extraLarge)
                            .layerBackdrop(backdrop)
                            .clickable(onClick = onOpen),
                )
                // A sibling of the picture, never inside it: the glass samples what lies under it.
                LiquidGlassIconButton(
                    backdrop = backdrop,
                    imageVector = SimpIcons.Close,
                    highlight = Highlight(width = 1.dp),
                    modifier =
                        Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                            .size(48.dp),
                    onClick = onClose,
                )
            }
        }
    }
}
