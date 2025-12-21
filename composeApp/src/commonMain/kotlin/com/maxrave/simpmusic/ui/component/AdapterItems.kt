package com.maxrave.simpmusic.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.MarqueeAnimationMode
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.snapping.SnapLayoutInfoProvider
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.maxrave.common.Config
import com.maxrave.domain.data.entities.AlbumEntity
import com.maxrave.domain.data.entities.DownloadState
import com.maxrave.domain.data.entities.LocalPlaylistEntity
import com.maxrave.domain.data.entities.PlaylistEntity
import com.maxrave.domain.data.entities.PodcastsEntity
import com.maxrave.domain.data.model.browse.album.Track
import com.maxrave.domain.data.model.browse.artist.ResultAlbum
import com.maxrave.domain.data.model.browse.artist.ResultPlaylist
import com.maxrave.domain.data.model.browse.artist.ResultSingle
import com.maxrave.domain.data.model.home.Content
import com.maxrave.domain.data.model.home.HomeItem
import com.maxrave.domain.data.model.home.chart.ItemArtist
import com.maxrave.domain.data.model.home.chart.ItemVideo
import com.maxrave.domain.data.model.mood.genre.ItemsPlaylist
import com.maxrave.domain.data.model.mood.moodmoments.Item
import com.maxrave.domain.data.model.searchResult.albums.AlbumsResult
import com.maxrave.domain.data.model.searchResult.playlists.PlaylistsResult
import com.maxrave.domain.data.type.HomeContentType
import com.maxrave.domain.mediaservice.handler.PlaylistType
import com.maxrave.domain.mediaservice.handler.QueueData
import com.maxrave.domain.utils.connectArtists
import com.maxrave.domain.utils.toListName
import com.maxrave.domain.utils.toSongEntity
import com.maxrave.domain.utils.toTrack
import com.maxrave.logger.Logger
import com.maxrave.simpmusic.Platform
import com.maxrave.simpmusic.expect.ui.HorizontalScrollBar
import com.maxrave.simpmusic.extension.generateRandomColor
import com.maxrave.simpmusic.extension.ifNullOrEmpty
import com.maxrave.simpmusic.getPlatform
import com.maxrave.simpmusic.ui.navigation.destination.list.AlbumDestination
import com.maxrave.simpmusic.ui.navigation.destination.list.ArtistDestination
import com.maxrave.simpmusic.ui.navigation.destination.list.PlaylistDestination
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.HomeViewModel
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.album
import simpmusic.composeapp.generated.resources.artists
import simpmusic.composeapp.generated.resources.available_online
import simpmusic.composeapp.generated.resources.description
import simpmusic.composeapp.generated.resources.downloaded
import simpmusic.composeapp.generated.resources.holder
import simpmusic.composeapp.generated.resources.holder_video
import simpmusic.composeapp.generated.resources.playlist
import simpmusic.composeapp.generated.resources.podcasts
import simpmusic.composeapp.generated.resources.songs
import simpmusic.composeapp.generated.resources.subscribers
import simpmusic.composeapp.generated.resources.videos
import simpmusic.composeapp.generated.resources.you
import simpmusic.composeapp.generated.resources.your_youtube_playlists

@Composable
fun HomeItem(
    homeViewModel: HomeViewModel = koinViewModel(),
    navController: NavController,
    data: HomeItem,
) {
    var bottomSheetShow by remember { mutableStateOf(false) }

    val lazyListState = rememberLazyListState()
    val snapperFlingBehavior = rememberSnapFlingBehavior(SnapLayoutInfoProvider(lazyListState = lazyListState))

    var track by remember { mutableStateOf<Track?>(null) }

    if (bottomSheetShow) {
        NowPlayingBottomSheet(
            onDismiss = { bottomSheetShow = false },
            song = track?.toSongEntity(),
            navController = navController,
        )
    }

    val channelId = data.channelId
    Column {
        Row(
            modifier =
                if (channelId != null) {
                    Modifier
                        .focusable(true)
                        .clickable {
                            navController.navigate(
                                ArtistDestination(
                                    channelId = channelId,
                                ),
                            )
                        }
                } else {
                    Modifier
                },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AnimatedVisibility(
                visible = (data.thumbnail?.lastOrNull() != null),
                modifier = Modifier.align(Alignment.CenterVertically),
            ) {
                AsyncImage(
                    model =
                        ImageRequest
                            .Builder(LocalPlatformContext.current)
                            .data(data.thumbnail?.lastOrNull()?.url)
                            .diskCachePolicy(CachePolicy.ENABLED)
                            .diskCacheKey(data.thumbnail?.lastOrNull()?.url)
                            .crossfade(550)
                            .build(),
                    contentDescription = "",
                    placeholder = painterResource(Res.drawable.holder),
                    error = painterResource(Res.drawable.holder),
                    modifier =
                        Modifier
                            .size(36.dp)
                            .clip(
                                CircleShape,
                            ),
                )
            }
            Column(
                Modifier
                    .padding(start = 10.dp),
            ) {
                AnimatedVisibility(visible = (data.subtitle != null && data.subtitle != "")) {
                    Text(
                        text = data.subtitle ?: "",
                        style = typo().bodySmall,
                    )
                }
                Text(
                    text = data.title,
                    style = typo().headlineMedium,
                    color = Color.White,
                    maxLines = 1,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        LazyRow(
            state = lazyListState,
            flingBehavior = snapperFlingBehavior,
        ) {
            items(data.contents) { temp ->
                if (temp != null) {
                    val browseId = temp.browseId
                    val playlistId = temp.playlistId
                    if ((playlistId != null && temp.videoId == null) || (playlistId != null && temp.videoId == "")) {
                        if (playlistId.startsWith("UC")) {
                            HomeItemArtist(onClick = {
                                navController.navigate(
                                    ArtistDestination(
                                        channelId = playlistId,
                                    ),
                                )
                            }, data = temp)
                        } else {
                            HomeItemContentPlaylist(onClick = {
                                navController.navigate(
                                    PlaylistDestination(
                                        playlistId = playlistId,
                                    ),
                                )
                            }, data = temp)
                        }
                    } else if ((browseId != null && temp.videoId == null) || (browseId != null && temp.videoId == "")) {
                        if (browseId.startsWith("UC")) {
                            HomeItemArtist(onClick = {
                                navController.navigate(
                                    ArtistDestination(
                                        channelId = browseId,
                                    ),
                                )
                            }, data = temp)
                        } else {
                            HomeItemContentPlaylist(onClick = {
                                navController.navigate(
                                    AlbumDestination(
                                        browseId = browseId,
                                    ),
                                )
                            }, data = temp)
                        }
                    } else if (temp.thumbnails.firstOrNull()?.width != temp.thumbnails.firstOrNull()?.height) {
                        HomeItemVideo(
                            onClick = {
                                val firstQueue: Track = temp.toTrack()
                                homeViewModel.setQueueData(
                                    QueueData.Data(
                                        listTracks = arrayListOf(firstQueue),
                                        firstPlayedTrack = firstQueue,
                                        playlistId = "RDAMVM${temp.videoId}",
                                        playlistName = temp.title,
                                        playlistType = PlaylistType.RADIO,
                                        continuation = null,
                                    ),
                                )
                                homeViewModel.loadMediaItem(
                                    firstQueue,
                                    Config.SONG_CLICK,
                                )
                            },
                            onLongClick = {
                                track = temp.toTrack()
                                bottomSheetShow = true
                            },
                            data = temp,
                        )
                    } else {
                        HomeItemSong(
                            onClick = {
                                val firstQueue: Track = temp.toTrack()
                                homeViewModel.setQueueData(
                                    QueueData.Data(
                                        listTracks = arrayListOf(firstQueue),
                                        firstPlayedTrack = firstQueue,
                                        playlistId = "RDAMVM${temp.videoId}",
                                        playlistName = temp.title,
                                        playlistType = PlaylistType.RADIO,
                                        continuation = null,
                                    ),
                                )
                                homeViewModel.loadMediaItem(
                                    firstQueue,
                                    Config.SONG_CLICK,
                                )
                            },
                            onLongClick = {
                                track = temp.toTrack()
                                bottomSheetShow = true
                            },
                            data = temp,
                        )
                    }
                }
            }
        }
        if (getPlatform() == Platform.Desktop) {
            HorizontalScrollBar(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                scrollState = lazyListState,
            )
        }
    }
}

@Composable
fun HomeItemContentPlaylist(
    onClick: () -> Unit,
    data: HomeContentType,
    thumbSize: Dp = 160.dp,
) {
    Box(
        Modifier
            .wrapContentSize()
            .focusable(true)
            .clickable {
                onClick()
            },
    ) {
        Column(
            modifier =
                Modifier
                    .padding(10.dp),
        ) {
            val thumb =
                when (data) {
                    is Content -> data.thumbnails.lastOrNull()?.url
                    is com.maxrave.domain.data.model.mood.genre.Content -> data.thumbnail?.lastOrNull()?.url
                    is com.maxrave.domain.data.model.mood.moodmoments.Content -> data.thumbnails?.lastOrNull()?.url
                    is LocalPlaylistEntity -> data.thumbnail
                    is PlaylistsResult -> data.thumbnails.lastOrNull()?.url
                    is AlbumEntity -> data.thumbnails
                    is PlaylistEntity -> data.thumbnails
                    is ResultSingle -> data.thumbnails.lastOrNull()?.url
                    is ResultAlbum -> data.thumbnails.lastOrNull()?.url
                    is ResultPlaylist -> data.thumbnails.lastOrNull()?.url
                    is PodcastsEntity -> data.thumbnail
                    is AlbumsResult -> data.thumbnails.lastOrNull()?.url
                    else -> null
                }
            AsyncImage(
                model =
                    ImageRequest
                        .Builder(LocalPlatformContext.current)
                        .data(thumb)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .diskCacheKey(thumb)
                        .crossfade(550)
                        .build(),
                placeholder =
                    if (data is LocalPlaylistEntity) {
                        painterPlaylistThumbnail(
                            data.title,
                            style = typo().bodySmall,
                            thumbSize * 0.9f to thumbSize * 0.9f,
                        )
                    } else {
                        painterResource(Res.drawable.holder)
                    },
                error =
                    if (data is LocalPlaylistEntity) {
                        painterPlaylistThumbnail(
                            data.title,
                            style = typo().bodySmall,
                            thumbSize * 0.9f to thumbSize * 0.9f,
                        )
                    } else {
                        painterResource(Res.drawable.holder)
                    },
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier =
                    Modifier
                        .size(thumbSize)
                        .aspectRatio(1f)
                        .clip(
                            RoundedCornerShape(10.dp),
                        ),
            )
            Text(
                text =
                    when (data) {
                        is Content -> data.title
                        is com.maxrave.domain.data.model.mood.genre.Content -> data.title.title
                        is com.maxrave.domain.data.model.mood.moodmoments.Content -> data.title
                        is LocalPlaylistEntity -> data.title
                        is PlaylistsResult -> data.title
                        is AlbumEntity -> data.title
                        is PlaylistEntity -> data.title
                        is ResultSingle -> data.title
                        is ResultAlbum -> data.title
                        is ResultPlaylist -> data.title
                        is PodcastsEntity -> data.title
                        is AlbumsResult -> data.title
                        else -> ""
                    },
                style = typo().titleSmall,
                color = Color.White,
                maxLines = 1,
                modifier =
                    Modifier
                        .width(thumbSize)
                        .wrapContentHeight(align = Alignment.CenterVertically)
                        .padding(top = 8.dp)
                        .basicMarquee(
                            iterations = Int.MAX_VALUE,
                            animationMode = MarqueeAnimationMode.Immediately,
                        ).focusable(),
            )
            Text(
                text =
                    when (data) {
                        is Content -> {
                            data.description
                                ?: if (data.playlistId == null) {
                                    (
                                        if (!data.artists.isNullOrEmpty()) {
                                            data.artists
                                                .toListName()
                                                .connectArtists()
                                        } else {
                                            stringResource(Res.string.album)
                                        }
                                    )
                                } else {
                                    stringResource(Res.string.playlist)
                                }
                        }

                        is com.maxrave.domain.data.model.mood.genre.Content -> {
                            data.title.subtitle
                        }

                        is com.maxrave.domain.data.model.mood.moodmoments.Content -> {
                            data.subtitle
                        }

                        is LocalPlaylistEntity -> {
                            stringResource(Res.string.you)
                        }

                        is PlaylistsResult -> {
                            data.author
                        }

                        is AlbumEntity -> {
                            data.artistName?.connectArtists() ?: stringResource(Res.string.album)
                        }

                        is PlaylistEntity -> {
                            data.author ?: stringResource(Res.string.playlist)
                        }

                        is ResultSingle -> {
                            data.year
                        }

                        is ResultAlbum -> {
                            data.year
                        }

                        is ResultPlaylist -> {
                            data.author
                        }

                        is PodcastsEntity -> {
                            data.authorName
                        }

                        is AlbumsResult -> {
                            data.year
                        }

                        else -> {
                            ""
                        }
                    },
                style = typo().bodySmall,
                maxLines = 1,
                modifier =
                    Modifier
                        .width(thumbSize)
                        .wrapContentHeight(align = Alignment.CenterVertically)
                        .basicMarquee(
                            iterations = Int.MAX_VALUE,
                            animationMode = MarqueeAnimationMode.Immediately,
                        ).focusable(),
            )
            if (data is com.maxrave.domain.data.type.PlaylistType && data !is AlbumsResult) {
                val subtitle =
                    if (data is LocalPlaylistEntity) {
                        if (data.downloadState != DownloadState.STATE_DOWNLOADED) {
                            stringResource(Res.string.available_online)
                        } else {
                            stringResource(Res.string.downloaded)
                        }
                    } else if (data is PlaylistEntity) {
                        stringResource(Res.string.playlist)
                    } else if (data is AlbumEntity) {
                        stringResource(Res.string.album)
                    } else if (data is PodcastsEntity) {
                        stringResource(Res.string.podcasts)
                    } else {
                        stringResource(Res.string.your_youtube_playlists)
                    }
                Text(
                    text = subtitle,
                    style = typo().bodySmall,
                    maxLines = 1,
                    modifier =
                        Modifier
                            .width(thumbSize)
                            .wrapContentHeight(align = Alignment.CenterVertically)
                            .basicMarquee(
                                iterations = Int.MAX_VALUE,
                                animationMode = MarqueeAnimationMode.Immediately,
                            ).focusable(),
                )
            }
        }
    }
}

@Composable
fun QuickPicksItem(
    onClick: () -> Unit,
    widthDp: Dp,
    data: Content,
) {
    val tag = "QuickPicksItem"
    Box(
        modifier =
            Modifier
                .wrapContentHeight()
                .width(widthDp - 30.dp)
                .focusable(true)
                .clickable {
                    onClick()
                },
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
        ) {
            AsyncImage(
                model =
                    ImageRequest
                        .Builder(LocalPlatformContext.current)
                        .data(data.thumbnails.lastOrNull()?.url)
                        .crossfade(550)
                        .diskCacheKey(data.thumbnails.lastOrNull()?.url)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .build(),
                placeholder = painterResource(Res.drawable.holder),
                contentDescription = stringResource(Res.string.description),
                contentScale = ContentScale.Crop,
                modifier =
                    Modifier
                        .align(Alignment.CenterVertically)
                        .size(44.dp)
                        .clip(
                            RoundedCornerShape(10),
                        ),
            )
            Column(
                Modifier
                    .padding(
                        start = 16.dp,
                    ).align(Alignment.CenterVertically),
                verticalArrangement = Arrangement.SpaceEvenly,
            ) {
                Text(
                    text = data.title,
                    style = typo().titleSmall,
                    maxLines = 1,
                    color = Color.White,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(align = Alignment.CenterVertically)
                            .basicMarquee(
                                iterations = Int.MAX_VALUE,
                                animationMode = MarqueeAnimationMode.Immediately,
                            ).focusable()
                            .padding(
                                bottom = 3.dp,
                            ),
                )
                LazyRow(verticalAlignment = Alignment.CenterVertically) {
                    item {
                        androidx.compose.animation.AnimatedVisibility(visible = data.isExplicit == true) {
                            ExplicitBadge(
                                modifier =
                                    Modifier
                                        .size(20.dp)
                                        .padding(end = 4.dp)
                                        .weight(1f),
                            )
                        }
                    }
                    item {
                        Text(
                            text = data.artists.toListName().connectArtists(),
                            style = typo().bodySmall,
                            maxLines = 1,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .wrapContentHeight(align = Alignment.CenterVertically)
                                    .basicMarquee(
                                        iterations = Int.MAX_VALUE,
                                        animationMode = MarqueeAnimationMode.Immediately,
                                    ).focusable(),
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeItemSong(
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    data: Content,
) {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .focusable(true)
                .clickable {
                    onClick()
                }.combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick,
                ),
    ) {
        Column(
            modifier =
                Modifier
                    .padding(10.dp),
        ) {
            val thumb =
                data.thumbnails.lastOrNull()?.url?.let {
                    if (it.contains("w120")) {
                        Regex("([wh])120").replace(it, "$1544")
                    } else {
                        it
                    }
                }
            Logger.w("AsyncImage", "HomeItemSong: $thumb")
            AsyncImage(
                model =
                    ImageRequest
                        .Builder(LocalPlatformContext.current)
                        .data(thumb)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .diskCacheKey(thumb)
                        .crossfade(550)
                        .build(),
                placeholder = painterResource(Res.drawable.holder),
                error = painterResource(Res.drawable.holder),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier =
                    Modifier
                        .align(Alignment.CenterHorizontally)
                        .size(160.dp)
                        .clip(
                            RoundedCornerShape(10.dp),
                        ),
            )
            Text(
                text = data.title,
                style = typo().titleSmall,
                color = Color.White,
                maxLines = 1,
                modifier =
                    Modifier
                        .width(160.dp)
                        .wrapContentHeight(align = Alignment.CenterVertically)
                        .padding(top = 8.dp)
                        .basicMarquee(
                            iterations = Int.MAX_VALUE,
                            animationMode = MarqueeAnimationMode.Immediately,
                        ).focusable(),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                AnimatedVisibility(visible = data.isExplicit == true) {
                    ExplicitBadge(
                        modifier =
                            Modifier
                                .size(20.dp)
                                .padding(end = 4.dp)
                                .weight(1f),
                    )
                }
                Text(
                    text = data.artists.toListName().connectArtists(),
                    style = typo().bodySmall,
                    maxLines = 1,
                    modifier =
                        Modifier
                            .width(160.dp)
                            .wrapContentHeight(align = Alignment.CenterVertically)
                            .basicMarquee(
                                iterations = Int.MAX_VALUE,
                                animationMode = MarqueeAnimationMode.Immediately,
                            ).focusable()
                            .padding(vertical = 3.dp),
                )
            }
            Text(
                text = data.album?.name ?: stringResource(Res.string.songs),
                style = typo().bodySmall,
                maxLines = 1,
                modifier =
                    Modifier
                        .width(160.dp)
                        .wrapContentHeight(align = Alignment.CenterVertically)
                        .basicMarquee(
                            iterations = Int.MAX_VALUE,
                            animationMode = MarqueeAnimationMode.Immediately,
                        ).focusable(),
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeItemVideo(
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    data: Content,
) {
    Box(
        Modifier
            .fillMaxSize()
            .focusable(true)
            .clickable {
                onClick()
            }.combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            ),
    ) {
        Column(
            modifier =
                Modifier
                    .padding(10.dp),
        ) {
            val thumb = data.thumbnails.lastOrNull()?.url
            Logger.w("AsyncImage", "HomeItemSong: $thumb")
            AsyncImage(
                model =
                    ImageRequest
                        .Builder(LocalPlatformContext.current)
                        .data(thumb)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .diskCacheKey(thumb)
                        .crossfade(550)
                        .build(),
                placeholder = painterResource(Res.drawable.holder_video),
                error = painterResource(Res.drawable.holder_video),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier =
                    Modifier
                        .align(Alignment.CenterHorizontally)
                        .height(160.dp)
                        .aspectRatio(16f / 9f)
                        .clip(
                            RoundedCornerShape(10.dp),
                        ),
            )
            Text(
                text = data.title,
                style = typo().titleSmall,
                color = Color.White,
                maxLines = 1,
                modifier =
                    Modifier
                        .width(284.5.dp)
                        .wrapContentHeight(align = Alignment.CenterVertically)
                        .padding(top = 8.dp)
                        .basicMarquee(
                            iterations = Int.MAX_VALUE,
                            animationMode = MarqueeAnimationMode.Immediately,
                        ).focusable(),
            )
            Text(
                text = data.artists.toListName().connectArtists(),
                style = typo().bodySmall,
                maxLines = 1,
                modifier =
                    Modifier
                        .width(284.5.dp)
                        .wrapContentHeight(align = Alignment.CenterVertically)
                        .basicMarquee(
                            iterations = Int.MAX_VALUE,
                            animationMode = MarqueeAnimationMode.Immediately,
                        ).focusable()
                        .padding(vertical = 2.dp),
            )
            Text(
                text = data.views ?: stringResource(Res.string.videos),
                style = typo().bodySmall,
                maxLines = 1,
                modifier =
                    Modifier
                        .width(284.5.dp)
                        .wrapContentHeight(align = Alignment.CenterVertically)
                        .basicMarquee(
                            iterations = Int.MAX_VALUE,
                            animationMode = MarqueeAnimationMode.Immediately,
                        ).focusable(),
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeItemArtist(
    onClick: () -> Unit,
    data: Content,
) {
    Box(
        Modifier
            .fillMaxSize()
            .focusable(true)
            .clickable {
                onClick()
            },
    ) {
        Column(
            modifier =
                Modifier
                    .padding(10.dp),
        ) {
            val thumb = data.thumbnails.lastOrNull()?.url
            Logger.w("AsyncImage", "HomeItemSong: $thumb")
            AsyncImage(
                model =
                    ImageRequest
                        .Builder(LocalPlatformContext.current)
                        .data(thumb)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .diskCacheKey(thumb)
                        .crossfade(550)
                        .build(),
                placeholder = painterResource(Res.drawable.holder),
                error = painterResource(Res.drawable.holder),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier =
                    Modifier
                        .align(Alignment.CenterHorizontally)
                        .size(160.dp)
                        .clip(
                            CircleShape,
                        ),
            )
            Text(
                text = data.title,
                style = typo().titleSmall,
                color = Color.White,
                maxLines = 1,
                textAlign = TextAlign.Center,
                modifier =
                    Modifier
                        .width(160.dp)
                        .wrapContentHeight(align = Alignment.CenterVertically)
                        .padding(top = 8.dp)
                        .basicMarquee(
                            iterations = Int.MAX_VALUE,
                            animationMode = MarqueeAnimationMode.Immediately,
                        ).focusable(),
            )
            Text(
                text = data.description.ifNullOrEmpty { stringResource(Res.string.artists) },
                style = typo().bodySmall,
                maxLines = 1,
                textAlign = TextAlign.Center,
                modifier =
                    Modifier
                        .width(160.dp)
                        .wrapContentHeight(align = Alignment.CenterVertically)
                        .basicMarquee(
                            iterations = Int.MAX_VALUE,
                            animationMode = MarqueeAnimationMode.Immediately,
                        ).focusable(),
            )
            Text(
                text = "",
                style = typo().bodySmall,
                maxLines = 1,
                textAlign = TextAlign.Center,
                modifier =
                    Modifier
                        .width(160.dp)
                        .wrapContentHeight(align = Alignment.CenterVertically)
                        .basicMarquee(
                            iterations = Int.MAX_VALUE,
                            animationMode = MarqueeAnimationMode.Immediately,
                        ).focusable(),
            )
        }
    }
}

@Composable
fun MoodMomentAndGenreHomeItem(
    title: String,
    onClick: () -> Unit,
) {
    ElevatedCard(
        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 6.dp,
            ),
        onClick = onClick,
        shape = RoundedCornerShape(5.dp),
        modifier =
            Modifier
                .width(160.dp)
                .height(50.dp)
                .padding(8.dp),
    ) {
        Row {
            Box(
                Modifier
                    .width(10.dp)
                    .height(64.dp)
                    .background(generateRandomColor()),
            )
            Text(
                text = title,
                style = typo().titleSmall,
                textAlign = TextAlign.Center,
                color = Color.White,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .align(Alignment.CenterVertically),
            )
        }
    }
}

@Composable
fun ItemVideoChart(
    onClick: () -> Unit,
    data: ItemVideo,
    position: Int,
) {
    Box(
        Modifier
            .wrapContentSize()
            .focusable(true)
            .clickable {
                onClick()
            },
    ) {
        Column(
            modifier =
                Modifier
                    .padding(10.dp),
        ) {
            val thumb = data.thumbnails.lastOrNull()?.url
            Logger.w("AsyncImage", "HomeItemSong: $thumb")
            AsyncImage(
                model =
                    ImageRequest
                        .Builder(LocalPlatformContext.current)
                        .data(thumb)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .diskCacheKey(thumb)
                        .crossfade(550)
                        .build(),
                placeholder = painterResource(Res.drawable.holder_video),
                error = painterResource(Res.drawable.holder_video),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier =
                    Modifier
                        .align(Alignment.CenterHorizontally)
                        .width(280.dp)
                        .height(160.dp)
                        .clip(
                            RoundedCornerShape(10),
                        ),
            )
            Row {
                Text(
                    text = position.toString(),
                    style = typo().titleLarge,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier =
                        Modifier
                            .width(40.dp)
                            .wrapContentHeight(align = Alignment.CenterVertically)
                            .align(Alignment.CenterVertically)
                            .basicMarquee(
                                iterations = Int.MAX_VALUE,
                                animationMode = MarqueeAnimationMode.Immediately,
                            ).focusable(),
                )
                Column(Modifier.padding(start = 10.dp)) {
                    Text(
                        text = data.title,
                        style = typo().titleMedium,
                        maxLines = 1,
                        color = Color.White,
                        modifier =
                            Modifier
                                .width(210.dp)
                                .wrapContentHeight(align = Alignment.CenterVertically)
                                .padding(top = 10.dp)
                                .basicMarquee(
                                    iterations = Int.MAX_VALUE,
                                    animationMode = MarqueeAnimationMode.Immediately,
                                ).focusable(),
                    )
                    Text(
                        text = data.artists.toListName().connectArtists(),
                        style = typo().bodyMedium,
                        modifier =
                            Modifier
                                .width(210.dp)
                                .wrapContentHeight(align = Alignment.CenterVertically)
                                .basicMarquee(
                                    iterations = Int.MAX_VALUE,
                                    animationMode = MarqueeAnimationMode.Immediately,
                                ).focusable()
                                .padding(vertical = 3.dp),
                    )
                    Text(
                        text = data.views,
                        style = typo().bodySmall,
                        modifier =
                            Modifier
                                .width(210.dp)
                                .wrapContentHeight(align = Alignment.CenterVertically)
                                .basicMarquee(
                                    iterations = Int.MAX_VALUE,
                                    animationMode = MarqueeAnimationMode.Immediately,
                                ).focusable()
                                .padding(end = 10.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun ItemArtistChart(
    onClick: () -> Unit,
    data: ItemArtist,
    widthDp: Dp,
) {
    Box(
        Modifier
            .wrapContentSize()
            .focusable(true)
            .clickable {
                onClick()
            },
    ) {
        Row(
            modifier =
                Modifier
                    .padding(10.dp)
                    .width(widthDp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = data.rank,
                style = typo().titleLarge,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier =
                    Modifier
                        .wrapContentSize(Alignment.Center)
                        .align(Alignment.CenterVertically)
                        .padding(end = 20.dp),
            )
            val thumb = data.thumbnails.lastOrNull()?.url
            Logger.w("AsyncImage", "HomeItemSong: $thumb")
            AsyncImage(
                model =
                    ImageRequest
                        .Builder(LocalPlatformContext.current)
                        .data(thumb)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .diskCacheKey(thumb)
                        .crossfade(550)
                        .build(),
                placeholder = painterResource(Res.drawable.holder),
                error = painterResource(Res.drawable.holder),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier =
                    Modifier
                        .align(Alignment.CenterVertically)
                        .size(60.dp)
                        .clip(
                            CircleShape,
                        ),
            )
            Column(
                Modifier
                    .padding(start = 15.dp)
                    .width(160.dp)
                    .align(Alignment.CenterVertically),
            ) {
                Text(
                    text = data.title,
                    style = typo().titleMedium,
                    modifier =
                        Modifier
                            .wrapContentHeight(align = Alignment.CenterVertically)
                            .basicMarquee(
                                iterations = Int.MAX_VALUE,
                                animationMode = MarqueeAnimationMode.Immediately,
                            ).focusable(),
                )
                Text(
                    text =
                        if (data.subscribers.contains(
                                stringResource(Res.string.subscribers).replace("%1\$s ", ""),
                            )
                        ) {
                            data.subscribers
                        } else {
                            stringResource(
                                Res.string.subscribers,
                                data.subscribers,
                            )
                        },
                    style = typo().bodySmall,
                    modifier =
                        Modifier
                            .wrapContentHeight(align = Alignment.CenterVertically)
                            .basicMarquee(
                                iterations = Int.MAX_VALUE,
                                animationMode = MarqueeAnimationMode.Immediately,
                            ).focusable(),
                )
            }
        }
    }
}

@Composable
fun ItemTrackChart(
    onClick: () -> Unit,
    data: Track,
    position: Int?,
    widthDp: Dp,
) {
    Box(
        modifier =
            Modifier
                .wrapContentSize()
                .focusable(true)
                .clickable {
                    onClick()
                },
    ) {
        Row(
            modifier =
                Modifier
                    .width(widthDp)
                    .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Crossfade(targetState = position != null, label = "Chart Track Position") {
                if (it) {
                    Row {
                        Text(
                            text = position.toString(),
                            style = typo().titleLarge,
                            textAlign = TextAlign.Center,
                            modifier =
                                Modifier
                                    .width(40.dp)
                                    .wrapContentHeight(align = Alignment.CenterVertically)
                                    .align(Alignment.CenterVertically)
                                    .basicMarquee(
                                        iterations = Int.MAX_VALUE,
                                        animationMode = MarqueeAnimationMode.Immediately,
                                    ).focusable(),
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                    }
                }
            }
            val thumb = data.thumbnails?.lastOrNull()?.url
            Logger.w("AsyncImage", "HomeItemSong: $thumb")
            AsyncImage(
                model =
                    ImageRequest
                        .Builder(LocalPlatformContext.current)
                        .data(thumb)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .diskCacheKey(thumb)
                        .crossfade(550)
                        .build(),
                placeholder = painterResource(Res.drawable.holder),
                error = painterResource(Res.drawable.holder),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier =
                    Modifier
                        .align(Alignment.CenterVertically)
                        .size(50.dp)
                        .clip(
                            RoundedCornerShape(10),
                        ),
            )
            Column(
                Modifier
                    .padding(
                        start = 20.dp,
                    ).align(Alignment.CenterVertically),
                verticalArrangement = Arrangement.SpaceEvenly,
            ) {
                Text(
                    text = data.title,
                    style = typo().titleSmall,
                    maxLines = 1,
                    color = Color.White,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(align = Alignment.CenterVertically)
                            .basicMarquee(
                                iterations = Int.MAX_VALUE,
                                animationMode = MarqueeAnimationMode.Immediately,
                            ).focusable()
                            .padding(
                                bottom = 3.dp,
                            ),
                )

                Text(
                    text = data.artists.toListName().connectArtists(),
                    style = typo().bodySmall,
                    maxLines = 1,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(align = Alignment.CenterVertically)
                            .basicMarquee(
                                iterations = Int.MAX_VALUE,
                                animationMode = MarqueeAnimationMode.Immediately,
                            ).focusable(),
                )
            }
        }
    }
}

@Composable
fun MoodAndGenresContentItem(
    data: Any?,
    navController: NavController,
) {
    Column(
        modifier = Modifier.wrapContentHeight(align = Alignment.CenterVertically, unbounded = true),
    ) {
        Text(
            text =
                when (data) {
                    is ItemsPlaylist -> (data).header
                    is Item -> (data).header
                    else -> ""
                },
            style = typo().titleMedium,
            color = Color.White,
            modifier =
                Modifier
                    .padding(top = 8.dp)
                    .padding(
                        horizontal = 15.dp,
                    ).fillMaxWidth(),
        )
        LazyRow(
            modifier =
                Modifier.padding(
                    10.dp,
                ),
        ) {
            val itemList =
                when (data) {
                    is ItemsPlaylist -> (data).contents
                    is Item -> (data).contents
                    else -> listOf()
                }
            items(itemList) { item ->
                HomeItemContentPlaylist(onClick = {
                    navController.navigate(
                        PlaylistDestination(
                            playlistId =
                                if (item is com.maxrave.domain.data.model.mood.genre.Content) {
                                    item.playlistBrowseId
                                } else {
                                    (item as com.maxrave.domain.data.model.mood.moodmoments.Content).playlistBrowseId
                                },
                        ),
                    )
                }, data = item)
            }
        }
    }
}