package com.maxrave.simpmusic.ui.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.carousel.CarouselItemDrawInfo
import androidx.compose.material3.carousel.HorizontalCenteredHeroCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.maxrave.domain.data.model.browse.album.Track
import com.maxrave.domain.data.model.home.HomeItem
import com.maxrave.domain.utils.connectArtists
import com.maxrave.domain.utils.toListName
import com.maxrave.domain.utils.toSongEntity
import com.maxrave.domain.utils.toTrack
import com.maxrave.simpmusic.extension.smoothScrimBrush
import com.maxrave.simpmusic.ui.theme.ForceDarkContent
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.HomeViewModel
import kotlinx.coroutines.launch

// Rows at least this wide (tablets, desktop) get the wide-window hero.
private const val WIDE_LAYOUT_MIN_DP = 600

/**
 * "Your daily discover" as ArchiveTune's hero carousel: one large card in the middle, narrow
 * neighbours, and the song's name on the art over a scrim. Past half the row only one large item
 * fits, so the large width is set above that and a wide window never lines up two side by side.
 * Tapping a neighbour brings it to the middle; tapping the middle card starts the song's radio, a
 * long press opens the song menu, and on a pointer device the card's play button shows on hover.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeDiscoverSection(
    data: HomeItem,
    navController: NavController,
    homeViewModel: HomeViewModel,
) {
    val songs = remember(data.contents) { data.contents.filterNotNull() }
    if (songs.isEmpty()) return
    var sheetTrack by remember { mutableStateOf<Track?>(null) }
    sheetTrack?.let { track ->
        NowPlayingBottomSheet(
            onDismiss = { sheetTrack = null },
            song = track.toSongEntity(),
            navController = navController,
        )
    }
    val state = rememberCarouselState { songs.size }
    val scope = rememberCoroutineScope()
    Column {
        HomeSectionHeader(data = data, navController = navController, onPlayAll = { homeViewModel.playAll(data) })
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val wide = maxWidth >= WIDE_LAYOUT_MIN_DP.dp
            val heroWidth = maxWidth * (if (wide) 0.6f else 0.74f)
            HorizontalCenteredHeroCarousel(
                state = state,
                maxItemWidth = heroWidth,
                itemSpacing = 8.dp,
                contentPadding = PaddingValues(horizontal = 10.dp),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(if (wide) 400.dp else heroWidth * 1.1f),
            ) { index ->
                val song = songs[index]
                val info = carouselItemDrawInfo
                // A neighbour is only brought to the middle; the middle card is the one that plays.
                val play: () -> Unit = {
                    if (info.isHero()) song.playRadio(homeViewModel) else scope.launch { state.animateScrollToItem(index) }
                }
                val (hoverModifier, hovered) = rememberCardHover(enabled = true)
                Box(
                    Modifier
                        .fillMaxSize()
                        .then(hoverModifier)
                        .maskClip(RoundedCornerShape(28.dp))
                        .combinedClickable(
                            onClick = play,
                            onLongClick = { sheetTrack = song.toTrack() },
                        ),
                ) {
                    AsyncImage(
                        model =
                            ImageRequest
                                .Builder(LocalPlatformContext.current)
                                .data(song.thumbnails.lastOrNull()?.url)
                                .diskCachePolicy(CachePolicy.ENABLED)
                                .diskCacheKey(song.thumbnails.lastOrNull()?.url)
                                .crossfade(550)
                                .build(),
                        contentDescription = null,
                        placeholder = rememberHolderPainter(),
                        error = rememberHolderPainter(),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(
                                smoothScrimBrush(
                                    from = MaterialTheme.colorScheme.scrim.copy(alpha = 0f),
                                    to = MaterialTheme.colorScheme.scrim.copy(alpha = 0.78f),
                                    startFraction = 0.45f,
                                ),
                            ),
                    )
                    // The scrim is dark in either theme, so the text takes the app's force-dark styles.
                    ForceDarkContent {
                        Column(
                            Modifier
                                .align(Alignment.BottomStart)
                                .padding(start = 20.dp, end = 64.dp, bottom = 20.dp),
                        ) {
                            Text(
                                text = song.title,
                                style = if (wide) typo().titleLarge else typo().headlineMedium,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text =
                                    listOfNotNull(
                                        song.artists
                                            .toListName()
                                            .connectArtists()
                                            .takeIf { it.isNotBlank() },
                                        song.album?.name?.takeIf { it.isNotBlank() },
                                    ).joinToString(" • "),
                                style = typo().bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    HoverPlayButton(visible = hovered && info.isHero(), onClick = play)
                }
            }
        }
    }
}

// 0 while the item sits in a narrow neighbour slot, 1 once it is the full-size middle card.
@OptIn(ExperimentalMaterial3Api::class)
private fun CarouselItemDrawInfo.heroFraction(): Float =
    if (maxSize <= minSize) 1f else ((size - minSize) / (maxSize - minSize)).coerceIn(0f, 1f)

@OptIn(ExperimentalMaterial3Api::class)
private fun CarouselItemDrawInfo.isHero(): Boolean = heroFraction() > 0.9f
