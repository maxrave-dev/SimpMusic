package com.maxrave.simpmusic.ui.screen.other

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import com.maxrave.simpmusic.R
import com.maxrave.simpmusic.common.Config
import com.maxrave.simpmusic.data.model.browse.album.Track
import com.maxrave.simpmusic.data.model.home.Content
import com.maxrave.simpmusic.data.model.searchResult.songs.Artist
import com.maxrave.simpmusic.extension.rgbFactor
import com.maxrave.simpmusic.extension.toSongEntity
import com.maxrave.simpmusic.extension.toTrack
import com.maxrave.simpmusic.service.PlaylistType
import com.maxrave.simpmusic.service.QueueData
import com.maxrave.simpmusic.ui.component.CenterLoadingBox
import com.maxrave.simpmusic.ui.component.CollapsingToolbarParallaxEffect
import com.maxrave.simpmusic.ui.component.DescriptionView
import com.maxrave.simpmusic.ui.component.EndOfPage
import com.maxrave.simpmusic.ui.component.HomeItemArtist
import com.maxrave.simpmusic.ui.component.HomeItemContentPlaylist
import com.maxrave.simpmusic.ui.component.HomeItemVideo
import com.maxrave.simpmusic.ui.component.LimitedBorderAnimationView
import com.maxrave.simpmusic.ui.component.MediaPlayerView
import com.maxrave.simpmusic.ui.component.NowPlayingBottomSheet
import com.maxrave.simpmusic.ui.component.SongFullWidthItems
import com.maxrave.simpmusic.ui.navigation.destination.list.AlbumDestination
import com.maxrave.simpmusic.ui.navigation.destination.list.ArtistDestination
import com.maxrave.simpmusic.ui.navigation.destination.list.MoreAlbumsDestination
import com.maxrave.simpmusic.ui.navigation.destination.list.PlaylistDestination
import com.maxrave.simpmusic.ui.theme.md_theme_dark_background
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.ArtistScreenState
import com.maxrave.simpmusic.viewModel.ArtistViewModel
import com.maxrave.simpmusic.viewModel.SharedViewModel
import kotlinx.coroutines.flow.map
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@Composable
@UnstableApi
@ExperimentalMaterial3Api
fun ArtistScreen(
    channelId: String,
    viewModel: ArtistViewModel = koinViewModel(),
    sharedViewModel: SharedViewModel = koinInject(),
    navController: NavController,
) {
    @Suppress("ktlint:standard:property-naming")
    val TAG = "ArtistScreen"
    val context = LocalContext.current

    val artistScreenState by viewModel.artistScreenState.collectAsState()
    val isFollowed by viewModel.followed.collectAsState()
    val canvasUrl by viewModel.canvasUrl.collectAsState()

    val playingTrack by sharedViewModel.nowPlayingState.map { it?.track?.videoId }.collectAsState(null)

    // Choosing song to show Bottom sheet
    var choosingTrack by remember {
        mutableStateOf<Track?>(null)
    }
    var showBottomSheet by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(channelId) {
        if (channelId != artistScreenState.data.channelId) {
            viewModel.browseArtist(channelId)
        }
    }

    Crossfade(artistScreenState) { state ->
        when (state) {
            is ArtistScreenState.Loading -> {
                Box(Modifier.fillMaxSize()) {
                    CenterLoadingBox(
                        Modifier
                            .size(48.dp)
                            .align(Alignment.Center),
                    )
                }
            }
            is ArtistScreenState.Success -> {
                CollapsingToolbarParallaxEffect(
                    modifier = Modifier.fillMaxSize(),
                    title = state.data.title ?: "",
                    imageUrl = state.data.imageUrl,
                    onBack = {
                        navController.navigateUp()
                    },
                ) { color ->
                    Column {
                        Column(
                            Modifier
                                .background(
                                    Brush.verticalGradient(
                                        colors =
                                            listOf(
                                                color.rgbFactor(0.5f),
                                                md_theme_dark_background,
                                            ),
                                    ),
                                ).padding(vertical = 16.dp, horizontal = 20.dp),
                        ) {
                            Row {
                                Text(
                                    text = state.data.subscribers ?: stringResource(R.string.unknown),
                                    style = typo.bodySmall,
                                    textAlign = TextAlign.Start,
                                    modifier = Modifier.weight(1f),
                                )
                                Text(
                                    text = state.data.playCount ?: stringResource(R.string.unknown),
                                    style = typo.bodySmall,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            Spacer(Modifier.height(16.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                AnimatedVisibility(canvasUrl != null) {
                                    Row {
                                        val canvas = canvasUrl ?: return@Row
                                        LimitedBorderAnimationView(
                                            isAnimated = true,
                                            brush = Brush.sweepGradient(listOf(Color.Transparent, Color.White)),
                                            backgroundColor = Color.Transparent,
                                            contentPadding = 2.dp,
                                            borderWidth = 1.dp,
                                            shape = RoundedCornerShape(4.dp),
                                            oneCircleDurationMillis = 3000,
                                            interactionNumber = 1,
                                        ) {
                                            MediaPlayerView(
                                                url = canvas.first,
                                                modifier =
                                                    Modifier
                                                        .width(28.dp)
                                                        .height(ButtonDefaults.MinHeight)
                                                        .align(Alignment.CenterVertically)
                                                        .border(
                                                            width = 0.5.dp,
                                                            color =
                                                                Color.White.copy(
                                                                    alpha = 0.8f,
                                                                ),
                                                            shape = RoundedCornerShape(4.dp),
                                                        ).clip(RoundedCornerShape(4.dp))
                                                        .clickable {
                                                            val firstQueue: Track = canvas.second.toTrack()
                                                            viewModel.setQueueData(
                                                                QueueData(
                                                                    listTracks = arrayListOf(firstQueue),
                                                                    firstPlayedTrack = firstQueue,
                                                                    playlistId = "RDAMVM${firstQueue.videoId}",
                                                                    playlistName = "\"${(state.data.title ?: "")}\" ${context.getString(
                                                                        R.string.popular,
                                                                    )}",
                                                                    playlistType = PlaylistType.RADIO,
                                                                    continuation = null,
                                                                ),
                                                            )
                                                            viewModel.loadMediaItem(
                                                                firstQueue,
                                                                type = Config.SONG_CLICK,
                                                            )
                                                        },
                                            )
                                        }
                                        Spacer(Modifier.width(12.dp))
                                    }
                                }
                                LimitedBorderAnimationView(
                                    isAnimated = !isFollowed,
                                    brush = Brush.sweepGradient(listOf(Color.Gray, Color.White)),
                                    backgroundColor = Color.Transparent,
                                    contentPadding = 0.dp,
                                    borderWidth = 2.dp,
                                    shape = ButtonDefaults.outlinedShape,
                                    oneCircleDurationMillis = 3000,
                                    interactionNumber = 1,
                                ) {
                                    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                                        OutlinedButton(
                                            onClick = {
                                                viewModel.updateFollowed(
                                                    if (isFollowed) 0 else 1,
                                                    state.data.channelId ?: return@OutlinedButton,
                                                )
                                            },
                                            colors =
                                                ButtonDefaults.outlinedButtonColors().copy(
                                                    contentColor = Color.White,
                                                ),
                                        ) {
                                            if (isFollowed) {
                                                Text(text = stringResource(R.string.followed))
                                            } else {
                                                Text(text = stringResource(R.string.follow))
                                            }
                                        }
                                    }
                                }
                                Spacer(Modifier.width(4.dp))
                                IconButton(
                                    onClick = {
                                        if (state.data.shuffleParam != null) {
                                            viewModel.onShuffleClick(state.data.shuffleParam)
                                        } else {
                                            Toast.makeText(context, context.getString(R.string.error), Toast.LENGTH_LONG).show()
                                        }
                                    },
                                ) {
                                    Icon(Icons.Outlined.Shuffle, "Shuffle")
                                }
                                Spacer(Modifier.weight(1f))
                                TextButton(
                                    onClick = {
                                        if (state.data.radioParam != null) {
                                            viewModel.onRadioClick(state.data.radioParam)
                                        } else {
                                            Toast.makeText(context, context.getString(R.string.error), Toast.LENGTH_LONG).show()
                                        }
                                    },
                                    colors =
                                        ButtonDefaults
                                            .textButtonColors()
                                            .copy(
                                                contentColor = Color.White,
                                            ),
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Icon(Icons.Outlined.Sensors, "")
                                        if (canvasUrl == null) {
                                            Spacer(Modifier.width(6.dp))
                                            Text(text = stringResource(R.string.start_radio))
                                        }
                                    }
                                }
                            }
                        }

                        // Popular Songs
                        AnimatedVisibility(state.data.popularSongs.isNotEmpty()) {
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 20.dp),
                                ) {
                                    Text(
                                        text = stringResource(R.string.popular),
                                        style = typo.labelMedium,
                                        modifier = Modifier.weight(1f),
                                    )
                                    TextButton(
                                        onClick = {
                                            val id = state.data.listSongParam
                                            if (id != null) {
                                                navController.navigate(PlaylistDestination(id))
                                            } else {
                                                Toast.makeText(context, context.getString(R.string.error), Toast.LENGTH_LONG).show()
                                            }
                                        },
                                        colors =
                                            ButtonDefaults
                                                .textButtonColors()
                                                .copy(
                                                    contentColor = Color.White,
                                                ),
                                    ) {
                                        Text(stringResource(R.string.more), style = typo.bodySmall)
                                    }
                                }
                                state.data.popularSongs.forEach { song ->
                                    SongFullWidthItems(
                                        track = song,
                                        isPlaying = song.videoId == playingTrack,
                                        modifier = Modifier.fillMaxWidth(),
                                        onMoreClickListener = {
                                            choosingTrack = song
                                            showBottomSheet = true
                                        },
                                        onClickListener = {
                                            val firstQueue: Track = song
                                            viewModel.setQueueData(
                                                QueueData(
                                                    listTracks = arrayListOf(firstQueue),
                                                    firstPlayedTrack = firstQueue,
                                                    playlistId = "RDAMVM${song.videoId}",
                                                    playlistName = "\"${state.data.title ?: ""}\" ${context.getString(R.string.popular)}",
                                                    playlistType = PlaylistType.RADIO,
                                                    continuation = null,
                                                ),
                                            )
                                            viewModel.loadMediaItem(
                                                firstQueue,
                                                type = Config.SONG_CLICK,
                                            )
                                        },
                                    )
                                }
                            }
                        }

                        // Singles
                        AnimatedVisibility(
                            state.data.singles != null &&
                                state.data.singles.results
                                    .isNotEmpty(),
                        ) {
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 20.dp),
                                ) {
                                    Text(
                                        text = stringResource(R.string.singles),
                                        style = typo.labelMedium,
                                        modifier = Modifier.weight(1f),
                                    )
                                    TextButton(
                                        onClick = {
                                            if (state.data.channelId != null) {
                                                val id = "MPAD${state.data.channelId}"
                                                navController.navigate(
                                                    MoreAlbumsDestination(
                                                        id = id,
                                                        type = MoreAlbumsDestination.SINGLE_TYPE,
                                                    ),
                                                )
                                            } else {
                                                Toast.makeText(context, context.getString(R.string.error), Toast.LENGTH_LONG).show()
                                            }
                                        },
                                        colors =
                                            ButtonDefaults
                                                .textButtonColors()
                                                .copy(
                                                    contentColor = Color.White,
                                                ),
                                    ) {
                                        Text(stringResource(R.string.more), style = typo.bodySmall)
                                    }
                                }
                                LazyRow(
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    item {
                                        Spacer(Modifier.size(10.dp))
                                    }
                                    items(state.data.singles?.results ?: emptyList()) { single ->
                                        HomeItemContentPlaylist(
                                            onClick = {
                                                navController.navigate(
                                                    AlbumDestination(
                                                        single.browseId,
                                                    ),
                                                )
                                            },
                                            data = single,
                                            thumbSize = 180.dp,
                                        )
                                    }
                                    item {
                                        Spacer(Modifier.size(10.dp))
                                    }
                                }
                            }
                        }

                        // Albums
                        AnimatedVisibility(
                            state.data.albums != null &&
                                state.data.albums.results
                                    .isNotEmpty(),
                        ) {
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 20.dp),
                                ) {
                                    Text(
                                        text = stringResource(R.string.albums),
                                        style = typo.labelMedium,
                                        modifier = Modifier.weight(1f),
                                    )
                                    TextButton(
                                        onClick = {
                                            if (state.data.channelId != null) {
                                                val id = "MPAD${state.data.channelId}"
                                                navController.navigate(
                                                    MoreAlbumsDestination(
                                                        id = id,
                                                        type = MoreAlbumsDestination.ALBUM_TYPE,
                                                    ),
                                                )
                                            } else {
                                                Toast.makeText(context, context.getString(R.string.error), Toast.LENGTH_LONG).show()
                                            }
                                        },
                                        colors =
                                            ButtonDefaults
                                                .textButtonColors()
                                                .copy(
                                                    contentColor = Color.White,
                                                ),
                                    ) {
                                        Text(stringResource(R.string.more), style = typo.bodySmall)
                                    }
                                }
                                LazyRow(
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    item {
                                        Spacer(Modifier.size(10.dp))
                                    }
                                    items(state.data.albums?.results ?: emptyList()) { album ->
                                        HomeItemContentPlaylist(
                                            onClick = {
                                                navController.navigate(
                                                    AlbumDestination(
                                                        browseId = album.browseId,
                                                    ),
                                                )
                                            },
                                            data = album,
                                            thumbSize = 180.dp,
                                        )
                                    }
                                    item {
                                        Spacer(Modifier.size(10.dp))
                                    }
                                }
                            }
                        }

                        // Videos
                        AnimatedVisibility(
                            state.data.video != null &&
                                state.data.video.video
                                    .isNotEmpty(),
                        ) {
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 20.dp),
                                ) {
                                    Text(
                                        text = stringResource(R.string.videos),
                                        style = typo.labelMedium,
                                        modifier = Modifier.weight(1f),
                                    )
                                    TextButton(
                                        onClick = {
                                            if (state.data.video?.videoListParam != null) {
                                                navController.navigate(
                                                    PlaylistDestination(
                                                        state.data.video.videoListParam,
                                                    ),
                                                )
                                            } else {
                                                Toast.makeText(context, context.getString(R.string.error), Toast.LENGTH_LONG).show()
                                            }
                                        },
                                        colors =
                                            ButtonDefaults
                                                .textButtonColors()
                                                .copy(
                                                    contentColor = Color.White,
                                                ),
                                    ) {
                                        Text(stringResource(R.string.more), style = typo.bodySmall)
                                    }
                                }
                                LazyRow(
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    item {
                                        Spacer(Modifier.size(10.dp))
                                    }
                                    items(state.data.video?.video ?: emptyList()) { video ->
                                        HomeItemVideo(
                                            onClick = {
                                                val firstQueue: Track = video
                                                viewModel.setQueueData(
                                                    QueueData(
                                                        listTracks = arrayListOf(firstQueue),
                                                        firstPlayedTrack = firstQueue,
                                                        playlistId = "RDAMVM${video.videoId}",
                                                        playlistName = (state.data.title ?: "") + context.getString(R.string.videos),
                                                        playlistType = PlaylistType.RADIO,
                                                        continuation = null,
                                                    ),
                                                )
                                                viewModel.loadMediaItem(
                                                    firstQueue,
                                                    type = Config.VIDEO_CLICK,
                                                )
                                            },
                                            onLongClick = {
                                                choosingTrack = video
                                                showBottomSheet = true
                                            },
                                            data =
                                                Content(
                                                    album = null,
                                                    artists = video.artists,
                                                    description = null,
                                                    isExplicit = video.isExplicit,
                                                    playlistId = null,
                                                    browseId = null,
                                                    thumbnails = video.thumbnails ?: emptyList(),
                                                    title = video.title,
                                                    videoId = video.videoId,
                                                    views = video.videoType,
                                                ),
                                        )
                                    }
                                    item {
                                        Spacer(Modifier.size(10.dp))
                                    }
                                }
                            }
                        }

                        // Feature on
                        AnimatedVisibility(state.data.featuredOn.isNotEmpty()) {
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 20.dp),
                                ) {
                                    Text(
                                        text = stringResource(R.string.featured_inArtist),
                                        style = typo.labelMedium,
                                        modifier =
                                            Modifier
                                                .weight(1f)
                                                .padding(vertical = 10.dp),
                                    )
                                }
                                LazyRow(
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    item {
                                        Spacer(Modifier.size(10.dp))
                                    }
                                    items(state.data.featuredOn) { feature ->
                                        HomeItemContentPlaylist(
                                            onClick = {
                                                navController.navigate(
                                                    PlaylistDestination(
                                                        feature.id,
                                                    ),
                                                )
                                            },
                                            data = feature,
                                            thumbSize = 180.dp,
                                        )
                                    }
                                    item {
                                        Spacer(Modifier.size(10.dp))
                                    }
                                }
                            }
                        }

                        // Related
                        AnimatedVisibility(
                            state.data.related != null &&
                                state.data.related.results
                                    .isNotEmpty(),
                        ) {
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 20.dp),
                                ) {
                                    Text(
                                        text = stringResource(R.string.related_artists),
                                        style = typo.labelMedium,
                                        modifier =
                                            Modifier
                                                .weight(1f)
                                                .padding(vertical = 10.dp),
                                    )
                                }
                                LazyRow(
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    item {
                                        Spacer(Modifier.size(10.dp))
                                    }
                                    items(state.data.related?.results ?: emptyList()) { related ->
                                        HomeItemArtist(
                                            onClick = {
                                                navController.navigate(
                                                    ArtistDestination(
                                                        channelId = related.browseId,
                                                    ),
                                                )
                                            },
                                            data =
                                                Content(
                                                    album = null,
                                                    artists =
                                                        listOf(
                                                            Artist(
                                                                id = related.browseId,
                                                                name = related.title,
                                                            ),
                                                        ),
                                                    description = related.subscribers,
                                                    isExplicit = null,
                                                    playlistId = null,
                                                    browseId = related.browseId,
                                                    thumbnails = related.thumbnails,
                                                    title = related.title,
                                                    videoId = null,
                                                    views = null,
                                                    durationSeconds = null,
                                                    radio = null,
                                                ),
                                        )
                                    }
                                    item {
                                        Spacer(Modifier.size(10.dp))
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 20.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.description),
                                style = typo.labelMedium,
                                modifier =
                                    Modifier
                                        .weight(1f)
                                        .padding(vertical = 12.dp),
                            )
                        }
                        val urlHandler = LocalUriHandler.current
                        ElevatedCard(
                            modifier = Modifier.padding(horizontal = 20.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors =
                                CardDefaults.elevatedCardColors().copy(
                                    containerColor = color.rgbFactor(0.5f),
                                ),
                        ) {
                            DescriptionView(
                                modifier = Modifier.padding(16.dp),
                                text = state.data.description ?: stringResource(R.string.no_description),
                                limitLine = 5,
                                onTimeClicked = {},
                                onURLClicked = { url ->
                                    urlHandler.openUri(url)
                                },
                            )
                        }
                        EndOfPage()
                    }
                    if (showBottomSheet && choosingTrack != null) {
                        NowPlayingBottomSheet(
                            onDismiss = {
                                showBottomSheet = false
                                choosingTrack = null
                            },
                            navController = navController,
                            song = choosingTrack?.toSongEntity(),
                        )
                    }
                }
            }
            is ArtistScreenState.Error -> {
                Toast.makeText(LocalContext.current, state.message ?: stringResource(R.string.error), Toast.LENGTH_LONG).show()
                navController.navigateUp()
            }
        }
    }
}