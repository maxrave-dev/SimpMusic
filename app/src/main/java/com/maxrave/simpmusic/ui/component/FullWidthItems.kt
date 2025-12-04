package com.maxrave.simpmusic.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.MarqueeAnimationMode
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.maxrave.common.R
import com.maxrave.domain.data.entities.AlbumEntity
import com.maxrave.domain.data.entities.ArtistEntity
import com.maxrave.domain.data.entities.DownloadState
import com.maxrave.domain.data.entities.LocalPlaylistEntity
import com.maxrave.domain.data.entities.PlaylistEntity
import com.maxrave.domain.data.entities.PodcastsEntity
import com.maxrave.domain.data.entities.SongEntity
import com.maxrave.domain.data.model.browse.album.Track
import com.maxrave.domain.data.model.searchResult.albums.AlbumsResult
import com.maxrave.domain.data.model.searchResult.artists.ArtistsResult
import com.maxrave.domain.data.model.searchResult.playlists.PlaylistsResult
import com.maxrave.domain.data.model.searchResult.songs.SongsResult
import com.maxrave.domain.data.model.searchResult.videos.VideosResult
import com.maxrave.domain.data.type.ArtistType
import com.maxrave.domain.data.type.PlaylistType
import com.maxrave.domain.repository.SongRepository
import com.maxrave.domain.repository.PlaylistRepository
import com.maxrave.domain.utils.connectArtists
import com.maxrave.domain.utils.toListName
import com.maxrave.domain.utils.toTrack
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.common.Config
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import kotlin.math.roundToInt

/**
 * This is the song item in the playlist or other places.
 */
@Composable
fun SongFullWidthItems(
    track: Track? = null,
    index: Int? = null,
    songEntity: SongEntity? = null,
    isPlaying: Boolean,
    onMoreClickListener: ((videoId: String) -> Unit)? = null,
    onClickListener: ((videoId: String) -> Unit)? = null,
    onAddToQueue: ((videoId: String) -> Unit)? = null,
    onSaveClick: ((videoId: String) -> Unit)? = null,
    modifier: Modifier,
) {
    val maxOffset = 360f
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val songRepository: SongRepository = koinInject<SongRepository>()
    val downloadState by songRepository
        .getSongAsFlow(songEntity?.videoId ?: track?.videoId ?: "")
        .mapNotNull { it?.downloadState }
        .collectAsState(initial = DownloadState.STATE_NOT_DOWNLOADED)
    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(com.maxrave.simpmusic.R.raw.audio_playing_animation),
    )
    val offsetX = remember { Animatable(initialValue = 0f) }
    var heightDp by remember { mutableStateOf(0.dp) }

    Box(
        modifier =
        modifier,
    ) {
        Crossfade(
            offsetX.value >= maxOffset / 2,
        ) { shouldShowAddToQueue ->
            if (shouldShowAddToQueue) {
                Box(
                    modifier =
                        Modifier
                            .height(heightDp)
                            .aspectRatio(1f)
                            .padding(start = 15.dp)
                            .align(Alignment.CenterStart),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        tint = Color.White,
                        imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                        contentDescription = stringResource(R.string.add_to_queue),
                    )
                }
            }
        }
        Box(
            modifier =
                modifier
                    .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                    .clickable {
                        onClickListener?.invoke(track?.videoId ?: songEntity?.videoId ?: "")
                    }.animateContentSize()
                    .pointerInput(Unit) {
                        if (!isPlaying && onAddToQueue != null) {
                            detectHorizontalDragGestures(
                                onHorizontalDrag = { change, dragAmount ->
                                    if (offsetX.value + dragAmount > 0) {
                                        change.consume()
                                        coroutineScope.launch {
                                            offsetX.snapTo(
                                                (offsetX.value + dragAmount).coerceAtMost(maxOffset),
                                            )
                                        }
                                    }
                                },
                                onDragEnd = {
                                    if (offsetX.value == maxOffset) {
                                        onAddToQueue(
                                            track?.videoId ?: songEntity?.videoId ?: "",
                                        )
                                    }
                                    coroutineScope.launch {
                                        offsetX.animateTo(0f)
                                    }
                                },
                            )
                        }
                    }.onGloballyPositioned { coordinates ->
                        with(density) {
                            heightDp = coordinates.size.height.toDp()
                        }
                    },
        ) {
            Row(
                Modifier
                    .padding(vertical = 10.dp, horizontal = 15.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier.size(48.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Crossfade(isPlaying) {
                        if (it) {
                            LottieAnimation(composition, iterations = LottieConstants.IterateForever)
                        } else if (index == null) {
                            val thumb = track?.thumbnails?.lastOrNull()?.url ?: songEntity?.thumbnails
                            AsyncImage(
                                model =
                                    ImageRequest
                                        .Builder(LocalContext.current)
                                        .data(thumb)
                                        .diskCachePolicy(CachePolicy.ENABLED)
                                        .diskCacheKey(thumb)
                                        .crossfade(true)
                                        .build(),
                                placeholder = painterResource(R.drawable.holder),
                                error = painterResource(R.drawable.holder),
                                contentDescription = null,
                                contentScale = ContentScale.FillWidth,
                                modifier =
                                    Modifier
                                        .fillMaxSize(),
                            )
                        } else {
                            Text(
                                text = (index + 1).toString(),
                                color = Color.White,
                                style = typo.titleMedium,
                                modifier = Modifier.align(Alignment.Center),
                            )
                        }
                    }
                }
                Column(
                    Modifier
                        .weight(1f)
                        .padding(start = 20.dp, end = 10.dp)
                        .align(Alignment.CenterVertically),
                ) {
                    Text(
                        text = track?.title ?: songEntity?.title ?: "",
                        style = typo.labelMedium,
                        maxLines = 1,
                        color = Color.White,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .wrapContentHeight(align = Alignment.CenterVertically)
                                .basicMarquee(
                                    iterations = Int.MAX_VALUE,
                                    animationMode = MarqueeAnimationMode.Immediately,
                                ).focusable(),
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AnimatedVisibility(
                            visible =
                                if (songEntity != null || track != null) {
                                    downloadState == DownloadState.STATE_DOWNLOADED
                                } else {
                                    false
                                },
                        ) {
                            Row {
                                Icon(
                                    painter = painterResource(id = R.drawable.download_for_offline_white),
                                    tint = Color.White,
                                    contentDescription = "",
                                    modifier = Modifier.size(16.dp).padding(2.dp),
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                        }
                        AnimatedVisibility(
                            visible =
                                songEntity?.isExplicit
                                    ?: (track?.isExplicit ?: false),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                ExplicitBadge(
                                    modifier = Modifier.size(20.dp).padding(1.dp),
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                            }
                        }
                        Text(
                            text =
                                (
                                    track?.artists?.toListName()?.connectArtists()
                                        ?: songEntity?.artistName?.connectArtists()
                                ) ?: "",
                            style = typo.bodyMedium,
                            maxLines = 1,
                            color = Color(0xC4FFFFFF),
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
                if (onSaveClick != null) {
                    val videoId = track?.videoId ?: songEntity?.videoId ?: ""
                    val isSaved by songRepository
                        .getSongAsFlow(videoId)
                        .map { entity -> entity?.inLibrary?.let { dt -> dt != Config.REMOVED_SONG_DATE_TIME } ?: false }
                        .collectAsState(initial = false)
                    RippleIconButton(
                        resId = R.drawable.baseline_playlist_add_24,
                        fillMaxSize = false,
                        enabled = !isSaved,
                    ) {
                        val videoId = track?.videoId ?: songEntity?.videoId
                        videoId?.let { onSaveClick.invoke(it) }
                    }
                }
                if (onMoreClickListener != null) {
                    RippleIconButton(resId = R.drawable.baseline_more_vert_24, fillMaxSize = false) {
                        val videoId = track?.videoId ?: songEntity?.videoId
                        videoId?.let { onMoreClickListener.invoke(it) }
                    }
                }
            }
        }
    }
}

@Composable
fun SuggestItems(
    track: Track,
    isPlaying: Boolean,
    onClickListener: (() -> Unit)? = null,
    onAddClickListener: (() -> Unit)? = null,
) {
    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(com.maxrave.simpmusic.R.raw.audio_playing_animation),
    )
    Box(
        modifier =
            Modifier
                .clickable {
                    if (onClickListener != null) {
                        onClickListener()
                    }
                }.animateContentSize(),
    ) {
        Row(
            Modifier
                .padding(vertical = 10.dp)
                .fillMaxWidth(),
        ) {
            Box(modifier = Modifier.size(40.dp)) {
                Crossfade(isPlaying) {
                    if (it) {
                        LottieAnimation(composition, iterations = LottieConstants.IterateForever)
                    } else {
                        val thumb = track.thumbnails?.lastOrNull()?.url
                        AsyncImage(
                            model =
                                ImageRequest
                                    .Builder(LocalContext.current)
                                    .data(thumb)
                                    .diskCachePolicy(CachePolicy.ENABLED)
                                    .diskCacheKey(thumb)
                                    .crossfade(true)
                                    .build(),
                            placeholder = painterResource(R.drawable.holder),
                            error = painterResource(R.drawable.holder),
                            contentDescription = null,
                            contentScale = ContentScale.FillWidth,
                            modifier =
                                Modifier
                                    .wrapContentHeight()
                                    .fillMaxWidth(),
                        )
                    }
                }
            }
            Column(
                Modifier
                    .weight(1f)
                    .padding(start = 20.dp, end = 10.dp)
                    .align(Alignment.CenterVertically),
            ) {
                Text(
                    text = track.title,
                    style = typo.titleSmall,
                    maxLines = 1,
                    color = Color.White,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(align = Alignment.CenterVertically)
                            .basicMarquee(
                                iterations = Int.MAX_VALUE,
                                animationMode = MarqueeAnimationMode.Immediately,
                            ).focusable(),
                )
                Text(
                    text =
                        (
                            track.artists?.toListName()?.connectArtists()
                        ) ?: "",
                    style = typo.bodySmall,
                    maxLines = 1,
                    color = Color(0xC4FFFFFF),
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
            RippleIconButton(
                resId = R.drawable.baseline_add_24,
                fillMaxSize = false,
                onClick =
                    onAddClickListener ?: {
                    },
            )
        }
    }
}

@Composable
fun PlaylistFullWidthItems(
    data: PlaylistType,
    onClickListener: (() -> Unit)? = null,
    onSaveClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .clickable {
                    onClickListener?.invoke()
                }.animateContentSize(),
    ) {
        var title = ""
        var thumb = ""
        var firstSubtitle = ""
        var secondSubtitle = ""
        var thirdRowSubtitle: String? = null

        firstSubtitle =
            when (data.playlistType()) {
                PlaylistType.Type.YOUTUBE_PLAYLIST -> stringResource(id = R.string.playlist)
                PlaylistType.Type.RADIO -> stringResource(id = R.string.radio)
                PlaylistType.Type.LOCAL -> stringResource(id = R.string.playlist)
                PlaylistType.Type.ALBUM -> stringResource(id = R.string.album)
                PlaylistType.Type.PODCAST -> stringResource(id = R.string.podcasts)
            }
        when (data) {
            is AlbumEntity -> {
                title = data.title
                thumb = data.thumbnails ?: ""
                secondSubtitle = data.artistName?.connectArtists() ?: ""
                thirdRowSubtitle = data.year
            }
            is PlaylistEntity -> {
                title = data.title
                thumb = data.thumbnails
                secondSubtitle = data.author ?: ""
            }
            is LocalPlaylistEntity -> {
                title = data.title
                thumb = data.thumbnail ?: ""
                secondSubtitle = stringResource(R.string.you)
            }
            is PlaylistsResult -> {
                title = data.title
                thumb = data.thumbnails.lastOrNull()?.url ?: ""
                secondSubtitle = data.author
            }
            is AlbumsResult -> {
                title = data.title
                thumb = data.thumbnails.lastOrNull()?.url ?: ""
                secondSubtitle = data.artists.toListName().connectArtists()
                thirdRowSubtitle = data.year
            }
            is PodcastsEntity -> {
                title = data.title
                thumb = data.thumbnail ?: ""
                secondSubtitle = data.authorName
                thirdRowSubtitle = data.description
            }
        }
        Row(
            Modifier
                .padding(vertical = 10.dp, horizontal = 15.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(modifier = Modifier.size(48.dp)) {
                AsyncImage(
                    model =
                        ImageRequest
                            .Builder(LocalContext.current)
                            .data(thumb)
                            .diskCachePolicy(CachePolicy.ENABLED)
                            .diskCacheKey(thumb)
                            .crossfade(true)
                            .build(),
                    placeholder = painterResource(R.drawable.holder),
                    error = painterResource(R.drawable.holder),
                    contentDescription = null,
                    contentScale = ContentScale.FillWidth,
                    modifier =
                        Modifier
                            .fillMaxSize(),
                )
            }
            Column(
                Modifier
                    .weight(1f)
                    .padding(start = 20.dp, end = 10.dp)
                    .align(Alignment.CenterVertically),
            ) {
                Text(
                    text = title,
                    style = typo.labelMedium,
                    maxLines = 1,
                    color = Color.White,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(align = Alignment.CenterVertically)
                            .basicMarquee(
                                iterations = Int.MAX_VALUE,
                                animationMode = MarqueeAnimationMode.Immediately,
                            ).focusable(),
                )

                Text(
                    text = "$firstSubtitle ${if (secondSubtitle.isNotEmpty()) " • $secondSubtitle" else ""}",
                    style = typo.bodyMedium,
                    maxLines = 1,
                    color = Color(0xC4FFFFFF),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(align = Alignment.CenterVertically)
                            .basicMarquee(
                                iterations = Int.MAX_VALUE,
                                animationMode = MarqueeAnimationMode.Immediately,
                            ).focusable(),
                )

                if (thirdRowSubtitle != null) {
                    Text(
                        text = thirdRowSubtitle,
                        style = typo.bodyMedium,
                        maxLines = 1,
                        color = Color(0xC4FFFFFF),
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
            if (onSaveClick != null) {
                val playlistRepository: PlaylistRepository = koinInject()
                val playlistId =
                    when (data) {
                        is PlaylistsResult -> data.browseId
                        is PlaylistEntity -> data.id
                        is LocalPlaylistEntity -> data.id.toString()
                        else -> null
                    }
                val isSaved: Boolean =
                    playlistId?.let { id ->
                        val saved by playlistRepository.getPlaylist(id).map { it != null }.collectAsState(initial = false)
                        saved
                    } ?: false
                RippleIconButton(
                    resId = R.drawable.baseline_playlist_add_24,
                    fillMaxSize = false,
                    enabled = !isSaved,
                ) {
                    onSaveClick.invoke()
                }
            }
        }
    }
}

@Composable
fun ArtistFullWidthItems(
    data: ArtistType,
    onClickListener: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val (name: String, thumbnails: String?) =
        when (data) {
            is ArtistEntity -> Pair(data.name, data.thumbnails)
            is ArtistsResult -> Pair(data.artist, data.thumbnails.lastOrNull()?.url)
            else -> Pair("", null)
        }
    Box(
        modifier
            .clickable {
                onClickListener?.invoke()
            },
    ) {
        Row(
            Modifier
                .padding(vertical = 10.dp, horizontal = 15.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(modifier = Modifier.size(48.dp)) {
                AsyncImage(
                    model =
                        ImageRequest
                            .Builder(LocalContext.current)
                            .data(thumbnails)
                            .diskCachePolicy(CachePolicy.ENABLED)
                            .diskCacheKey(thumbnails)
                            .crossfade(true)
                            .build(),
                    placeholder = painterResource(R.drawable.holder),
                    error = painterResource(R.drawable.holder),
                    contentDescription = null,
                    contentScale = ContentScale.FillHeight,
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                )
            }
            Column(
                Modifier
                    .weight(1f)
                    .padding(start = 20.dp, end = 10.dp)
                    .align(Alignment.CenterVertically),
            ) {
                Text(
                    text = name,
                    style = typo.labelMedium,
                    maxLines = 1,
                    color = Color.White,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(align = Alignment.CenterVertically)
                            .basicMarquee(
                                iterations = Int.MAX_VALUE,
                                animationMode = MarqueeAnimationMode.Immediately,
                            ).focusable(),
                )

                Text(
                    text = stringResource(R.string.artists),
                    style = typo.bodyMedium,
                    maxLines = 1,
                    color = Color(0xC4FFFFFF),
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

/**
 * Specialized search result components for different content types
 */

// Songs Search Result Item - optimized for music tracks
@Composable
fun SongSearchResultItem(
    songsResult: SongsResult,
    isPlaying: Boolean,
    onClickListener: (() -> Unit)? = null,
    onMoreClickListener: (() -> Unit)? = null,
    onAddToQueue: (() -> Unit)? = null,
    onSaveClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(com.maxrave.simpmusic.R.raw.audio_playing_animation),
    )
    
    Box(
        modifier = modifier
            .clickable { onClickListener?.invoke() }
            .animateContentSize(),
    ) {
        Row(
            Modifier
                .padding(vertical = 12.dp, horizontal = 16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Song thumbnail with music note overlay
            Box(
                modifier = Modifier.size(56.dp),
                contentAlignment = Alignment.Center,
            ) {
                Crossfade(isPlaying) {
                    if (it) {
                        LottieAnimation(composition, iterations = LottieConstants.IterateForever)
                    } else {
                        Box(modifier = Modifier.fillMaxSize()) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(songsResult.thumbnails?.lastOrNull()?.url)
                                    .diskCachePolicy(CachePolicy.ENABLED)
                                    .diskCacheKey(songsResult.thumbnails?.lastOrNull()?.url)
                                    .crossfade(true)
                                    .build(),
                                placeholder = painterResource(R.drawable.holder),
                                error = painterResource(R.drawable.holder),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(8.dp)),
                            )
                            // Music note overlay for visual distinction
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .align(Alignment.Center)
                                    .background(Color(0x80000000), RoundedCornerShape(4.dp)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.baseline_music_note_24),
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp),
                                )
                            }
                        }
                    }
                }
            }
            
            Column(
                Modifier
                    .weight(1f)
                    .padding(start = 16.dp, end = 12.dp)
                    .align(Alignment.CenterVertically),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = songsResult.title ?: "",
                        style = typo.labelMedium,
                        maxLines = 1,
                        color = Color.White,
                        modifier = Modifier
                            .weight(1f)
                            .basicMarquee(
                                iterations = Int.MAX_VALUE,
                                animationMode = MarqueeAnimationMode.Immediately,
                            )
                            .focusable(),
                    )
                    
                    // Duration badge for songs
                    songsResult.duration?.let { duration ->
                        Text(
                            text = duration,
                            style = typo.bodySmall,
                            color = Color(0x80FFFFFF),
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp),
                ) {
                    // Explicit badge
                    if (songsResult.isExplicit == true) {
                        ExplicitBadge(modifier = Modifier.size(16.dp).padding(end = 4.dp))
                    }
                    
                    Text(
                        text = songsResult.artists?.map { it.name }?.connectArtists() ?: "",
                        style = typo.bodyMedium,
                        maxLines = 1,
                        color = Color(0xC4FFFFFF),
                        modifier = Modifier
                            .weight(1f)
                            .basicMarquee(
                                iterations = Int.MAX_VALUE,
                                animationMode = MarqueeAnimationMode.Immediately,
                            )
                            .focusable(),
                    )
                }
                
                // Album info for songs
                songsResult.album?.name?.let { albumName ->
                    Text(
                        text = albumName,
                        style = typo.bodySmall,
                        maxLines = 1,
                        color = Color(0x80FFFFFF),
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .basicMarquee(
                                iterations = Int.MAX_VALUE,
                                animationMode = MarqueeAnimationMode.Immediately,
                            )
                            .focusable(),
                    )
                }
            }
            
            // Action buttons
            Column(horizontalAlignment = Alignment.End) {
                if (onSaveClick != null) {
                    RippleIconButton(
                        resId = R.drawable.baseline_playlist_add_24,
                        fillMaxSize = false,
                    ) { onSaveClick.invoke() }
                    Spacer(modifier = Modifier.height(4.dp))
                }
                if (onMoreClickListener != null) {
                    RippleIconButton(
                        resId = R.drawable.baseline_more_vert_24,
                        fillMaxSize = false,
                    ) { onMoreClickListener.invoke() }
                }
            }
        }
    }
}

// Videos Search Result Item - optimized for video content
@Composable
fun VideoSearchResultItem(
    videosResult: VideosResult,
    isPlaying: Boolean,
    onClickListener: (() -> Unit)? = null,
    onMoreClickListener: (() -> Unit)? = null,
    onAddToQueue: (() -> Unit)? = null,
    onSaveClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(com.maxrave.simpmusic.R.raw.audio_playing_animation),
    )
    
    // Determine video type for appropriate styling
    val isLiveVideo = videosResult.videoType?.contains("live", ignoreCase = true) == true ||
                     videosResult.resultType?.contains("live", ignoreCase = true) == true ||
                     videosResult.title.contains("live", ignoreCase = true) ||
                     videosResult.category?.contains("live", ignoreCase = true) == true
    
    val isMusicVideo = videosResult.category?.contains("music", ignoreCase = true) == true ||
                      videosResult.videoType?.contains("music", ignoreCase = true) == true ||
                      videosResult.resultType?.contains("music", ignoreCase = true) == true
    
    Box(
        modifier = modifier
            .clickable { onClickListener?.invoke() }
            .animateContentSize(),
    ) {
        Row(
            Modifier
                .padding(vertical = 12.dp, horizontal = 16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Video thumbnail with type-specific overlay
            Box(
                modifier = Modifier.size(56.dp),
                contentAlignment = Alignment.Center,
            ) {
                Crossfade(isPlaying) {
                    if (it) {
                        LottieAnimation(composition, iterations = LottieConstants.IterateForever)
                    } else {
                        Box(modifier = Modifier.fillMaxSize()) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(videosResult.thumbnails?.lastOrNull()?.url)
                                    .diskCachePolicy(CachePolicy.ENABLED)
                                    .diskCacheKey(videosResult.thumbnails?.lastOrNull()?.url)
                                    .crossfade(true)
                                    .build(),
                                placeholder = painterResource(R.drawable.holder_video),
                                error = painterResource(R.drawable.holder_video),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(6.dp)),
                            )
                            
                            // Type-specific overlay
                            when {
                                isLiveVideo -> {
                                    // Live video indicator - red background with LIVE text
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .align(Alignment.TopEnd)
                                            .background(Color(0xFFCC0000), CircleShape),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            text = "LIVE",
                                            style = typo.labelSmall,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                        )
                                    }
                                }
                                isMusicVideo -> {
                                    // Music video indicator - music note icon
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .align(Alignment.Center)
                                            .background(Color(0x80000000), RoundedCornerShape(4.dp)),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.baseline_music_note_24),
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp),
                                        )
                                    }
                                }
                                else -> {
                                    // Regular video indicator - play button
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .align(Alignment.Center)
                                            .background(Color(0x80000000), CircleShape),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.baseline_play_arrow_24),
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            Column(
                Modifier
                    .weight(1f)
                    .padding(start = 16.dp, end = 12.dp)
                    .align(Alignment.CenterVertically),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    // Live indicator badge in title row
                    if (isLiveVideo) {
                        Text(
                            text = "🔴 LIVE",
                            style = typo.bodySmall,
                            color = Color(0xFFCC0000),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(end = 8.dp),
                        )
                    }
                    
                    Text(
                        text = videosResult.title,
                        style = typo.labelMedium,
                        maxLines = 1,
                        color = Color.White,
                        modifier = Modifier
                            .weight(1f)
                            .basicMarquee(
                                iterations = Int.MAX_VALUE,
                                animationMode = MarqueeAnimationMode.Immediately,
                            )
                            .focusable(),
                    )
                    
                    // Duration badge for videos
                    videosResult.duration?.let { duration ->
                        Text(
                            text = duration,
                            style = typo.bodySmall,
                            color = Color(0x80FFFFFF),
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp),
                ) {
                    Text(
                        text = videosResult.artists?.map { it.name }?.connectArtists() ?: "",
                        style = typo.bodyMedium,
                        maxLines = 1,
                        color = Color(0xC4FFFFFF),
                        modifier = Modifier
                            .weight(1f)
                            .basicMarquee(
                                iterations = Int.MAX_VALUE,
                                animationMode = MarqueeAnimationMode.Immediately,
                            )
                            .focusable(),
                    )
                }
                
                // Video type and metadata
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 2.dp),
                ) {
                    // Video type indicator
                    when {
                        isLiveVideo -> {
                            Text(
                                text = "Live",
                                style = typo.bodySmall,
                                color = Color(0xFFCC0000),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(end = 8.dp),
                            )
                        }
                        isMusicVideo -> {
                            Text(
                                text = "Music",
                                style = typo.bodySmall,
                                color = Color(0xFF4CAF50),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(end = 8.dp),
                            )
                        }
                        else -> {
                            videosResult.category?.let { category ->
                                Text(
                                    text = category,
                                    style = typo.bodySmall,
                                    color = Color(0x80FFFFFF),
                                    modifier = Modifier.padding(end = 8.dp),
                                )
                            }
                        }
                    }
                    
                    // Views
                    videosResult.views?.let { views ->
                        Text(
                            text = views,
                            style = typo.bodySmall,
                            color = Color(0x80FFFFFF),
                            modifier = Modifier.basicMarquee(
                                iterations = Int.MAX_VALUE,
                                animationMode = MarqueeAnimationMode.Immediately,
                            ).focusable(),
                        )
                    }
                    
                    // Published date
                    videosResult.year?.let { year ->
                        if (videosResult.views != null) {
                            Text(
                                text = " • ",
                                style = typo.bodySmall,
                                color = Color(0x60FFFFFF),
                            )
                        }
                        Text(
                            text = year.toString(),
                            style = typo.bodySmall,
                            color = Color(0x80FFFFFF),
                            modifier = Modifier.basicMarquee(
                                iterations = Int.MAX_VALUE,
                                animationMode = MarqueeAnimationMode.Immediately,
                            ).focusable(),
                        )
                    }
                }
            }
            
            // Action buttons
            Column(horizontalAlignment = Alignment.End) {
                if (onSaveClick != null) {
                    RippleIconButton(
                        resId = R.drawable.baseline_playlist_add_24,
                        fillMaxSize = false,
                    ) { onSaveClick.invoke() }
                    Spacer(modifier = Modifier.height(4.dp))
                }
                if (onMoreClickListener != null) {
                    RippleIconButton(
                        resId = R.drawable.baseline_more_vert_24,
                        fillMaxSize = false,
                    ) { onMoreClickListener.invoke() }
                }
            }
        }
    }
}

// Albums Search Result Item - optimized for albums
@Composable
fun AlbumSearchResultItem(
    albumsResult: AlbumsResult,
    onClickListener: (() -> Unit)? = null,
    onSaveClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clickable { onClickListener?.invoke() }
            .animateContentSize(),
    ) {
        Row(
            Modifier
                .padding(vertical = 12.dp, horizontal = 16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Album thumbnail with album overlay
            Box(
                modifier = Modifier.size(56.dp),
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(albumsResult.thumbnails.lastOrNull()?.url)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .diskCacheKey(albumsResult.thumbnails.lastOrNull()?.url)
                        .crossfade(true)
                        .build(),
                    placeholder = painterResource(R.drawable.holder),
                    error = painterResource(R.drawable.holder),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(8.dp)),
                )
                // Album icon overlay for visual distinction
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .align(Alignment.Center)
                        .background(Color(0x80000000), RoundedCornerShape(4.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.baseline_album_24),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
            
            Column(
                Modifier
                    .weight(1f)
                    .padding(start = 16.dp, end = 12.dp)
                    .align(Alignment.CenterVertically),
            ) {
                Text(
                    text = albumsResult.title,
                    style = typo.labelMedium,
                    maxLines = 1,
                    color = Color.White,
                    modifier = Modifier
                        .fillMaxWidth()
                        .basicMarquee(
                            iterations = Int.MAX_VALUE,
                            animationMode = MarqueeAnimationMode.Immediately,
                        )
                        .focusable(),
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp),
                ) {
                    Text(
                        text = albumsResult.year ?: "",
                        style = typo.bodySmall,
                        color = Color(0x80FFFFFF),
                        modifier = Modifier.basicMarquee(
                            iterations = Int.MAX_VALUE,
                            animationMode = MarqueeAnimationMode.Immediately,
                        ).focusable(),
                    )
                    
                    if (albumsResult.year != null) {
                        Text(
                            text = " • ",
                            style = typo.bodySmall,
                            color = Color(0x60FFFFFF),
                        )
                    }
                    
                    // Albums don't have trackCount in this data model, showing year instead
                    albumsResult.year?.let { year ->
                        Text(
                            text = year,
                            style = typo.bodySmall,
                            color = Color(0x80FFFFFF),
                            modifier = Modifier.basicMarquee(
                                iterations = Int.MAX_VALUE,
                                animationMode = MarqueeAnimationMode.Immediately,
                            ).focusable(),
                        )
                    }
                }
                
                Text(
                    text = albumsResult.artists.map { it.name }.connectArtists(),
                    style = typo.bodyMedium,
                    maxLines = 1,
                    color = Color(0xC4FFFFFF),
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .fillMaxWidth()
                        .basicMarquee(
                            iterations = Int.MAX_VALUE,
                            animationMode = MarqueeAnimationMode.Immediately,
                        )
                        .focusable(),
                )
            }
            
            // Action buttons
            Column(horizontalAlignment = Alignment.End) {
                if (onSaveClick != null) {
                    RippleIconButton(
                        resId = R.drawable.baseline_playlist_add_24,
                        fillMaxSize = false,
                    ) { onSaveClick.invoke() }
                }
            }
        }
    }
}

// Artists Search Result Item - optimized for artists
@Composable
fun ArtistSearchResultItem(
    artistsResult: ArtistsResult,
    onClickListener: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clickable { onClickListener?.invoke() }
            .animateContentSize(),
    ) {
        Row(
            Modifier
                .padding(vertical = 12.dp, horizontal = 16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Artist avatar with artist overlay
            Box(
                modifier = Modifier.size(56.dp),
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(artistsResult.thumbnails.lastOrNull()?.url)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .diskCacheKey(artistsResult.thumbnails.lastOrNull()?.url)
                        .crossfade(true)
                        .build(),
                    placeholder = painterResource(R.drawable.holder),
                    error = painterResource(R.drawable.holder),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                )
                // Artist icon overlay for visual distinction
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .align(Alignment.Center)
                        .background(Color(0x80000000), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.baseline_people_alt_24),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
            
            Column(
                Modifier
                    .weight(1f)
                    .padding(start = 16.dp, end = 12.dp)
                    .align(Alignment.CenterVertically),
            ) {
                Text(
                    text = artistsResult.artist,
                    style = typo.labelMedium,
                    maxLines = 1,
                    color = Color.White,
                    modifier = Modifier
                        .fillMaxWidth()
                        .basicMarquee(
                            iterations = Int.MAX_VALUE,
                            animationMode = MarqueeAnimationMode.Immediately,
                        )
                        .focusable(),
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp),
                ) {
                    Text(
                        text = stringResource(R.string.artists),
                        style = typo.bodyMedium,
                        maxLines = 1,
                        color = Color(0xC4FFFFFF),
                        modifier = Modifier.basicMarquee(
                            iterations = Int.MAX_VALUE,
                            animationMode = MarqueeAnimationMode.Immediately,
                        ).focusable(),
                    )
                    
                    // ArtistsResult doesn't have subscribers field in this data model
                    // Could add channel description or other metadata here if available
                }
                
                // ArtistsResult doesn't have description field in this data model
                // Could add category or other metadata here if available
            }
        }
    }
}

// Playlists Search Result Item - optimized for playlists
@Composable
fun PlaylistSearchResultItem(
    playlistsResult: PlaylistsResult,
    onClickListener: (() -> Unit)? = null,
    onSaveClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clickable { onClickListener?.invoke() }
            .animateContentSize(),
    ) {
        Row(
            Modifier
                .padding(vertical = 12.dp, horizontal = 16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Playlist thumbnail with playlist overlay
            Box(
                modifier = Modifier.size(56.dp),
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(playlistsResult.thumbnails.lastOrNull()?.url)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .diskCacheKey(playlistsResult.thumbnails.lastOrNull()?.url)
                        .crossfade(true)
                        .build(),
                    placeholder = painterResource(R.drawable.holder),
                    error = painterResource(R.drawable.holder),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(8.dp)),
                )
                // Playlist icon overlay for visual distinction
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .align(Alignment.Center)
                        .background(Color(0x80000000), RoundedCornerShape(4.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.baseline_queue_music_24),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
            
            Column(
                Modifier
                    .weight(1f)
                    .padding(start = 16.dp, end = 12.dp)
                    .align(Alignment.CenterVertically),
            ) {
                Text(
                    text = playlistsResult.title,
                    style = typo.labelMedium,
                    maxLines = 1,
                    color = Color.White,
                    modifier = Modifier
                        .fillMaxWidth()
                        .basicMarquee(
                            iterations = Int.MAX_VALUE,
                            animationMode = MarqueeAnimationMode.Immediately,
                        )
                        .focusable(),
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp),
                ) {
                    Text(
                        text = if (playlistsResult.resultType == "Podcast") stringResource(R.string.podcasts) else stringResource(R.string.playlist),
                        style = typo.bodyMedium,
                        maxLines = 1,
                        color = Color(0xC4FFFFFF),
                        modifier = Modifier.basicMarquee(
                            iterations = Int.MAX_VALUE,
                            animationMode = MarqueeAnimationMode.Immediately,
                        ).focusable(),
                    )
                    
                    playlistsResult.itemCount?.let { count ->
                        val countInt = count.toIntOrNull() ?: 0
                        Text(
                            text = " • $count ${if (countInt == 1) stringResource(R.string.track) else stringResource(R.string.tracks)}",
                            style = typo.bodyMedium,
                            maxLines = 1,
                            color = Color(0xC4FFFFFF),
                            modifier = Modifier.basicMarquee(
                                iterations = Int.MAX_VALUE,
                                animationMode = MarqueeAnimationMode.Immediately,
                            ).focusable(),
                        )
                    }
                }
                
                Text(
                    text = playlistsResult.author.ifEmpty { stringResource(R.string.youtube_music) },
                    style = typo.bodySmall,
                    maxLines = 1,
                    color = Color(0x80FFFFFF),
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .fillMaxWidth()
                        .basicMarquee(
                            iterations = Int.MAX_VALUE,
                            animationMode = MarqueeAnimationMode.Immediately,
                        )
                        .focusable(),
                )
            }
            
            // Action buttons
            Column(horizontalAlignment = Alignment.End) {
                if (onSaveClick != null) {
                    RippleIconButton(
                        resId = R.drawable.baseline_playlist_add_24,
                        fillMaxSize = false,
                    ) { onSaveClick.invoke() }
                }
            }
        }
    }
}