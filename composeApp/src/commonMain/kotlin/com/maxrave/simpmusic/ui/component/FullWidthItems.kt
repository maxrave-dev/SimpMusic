package com.maxrave.simpmusic.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.MarqueeAnimationMode
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
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
import com.maxrave.domain.data.type.ArtistType
import com.maxrave.domain.data.type.PlaylistType
import com.maxrave.domain.repository.SongRepository
import com.maxrave.domain.utils.connectArtists
import com.maxrave.domain.utils.toListName
import com.maxrave.simpmusic.ui.icon.Add
import com.maxrave.simpmusic.ui.icon.Check
import com.maxrave.simpmusic.ui.icon.DownloadForOffline
import com.maxrave.simpmusic.ui.icon.DragHandle
import com.maxrave.simpmusic.ui.icon.MoreVert
import com.maxrave.simpmusic.ui.icon.PushPin
import com.maxrave.simpmusic.ui.icon.QueueMusic
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.theme.LocalForceDarkText
import com.maxrave.simpmusic.ui.theme.seed
import com.maxrave.simpmusic.ui.theme.typo
import io.github.alexzhirkevich.compottie.Compottie
import io.github.alexzhirkevich.compottie.LottieCompositionSpec
import io.github.alexzhirkevich.compottie.rememberLottieComposition
import io.github.alexzhirkevich.compottie.rememberLottiePainter
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.add_to_queue
import simpmusic.composeapp.generated.resources.album
import simpmusic.composeapp.generated.resources.artists
import simpmusic.composeapp.generated.resources.playlist
import simpmusic.composeapp.generated.resources.podcasts
import simpmusic.composeapp.generated.resources.radio
import simpmusic.composeapp.generated.resources.you
import kotlin.math.roundToInt

/**
 * This is the song item in the playlist or other places.
 *
 * Multi-selection is opt-in per screen: pass [onLongClick] to let a long press start it, then
 * drive [selectionMode] and [isSelected] from the screen's
 * [com.maxrave.simpmusic.ui.component.selection.SongSelectionState].
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SongFullWidthItems(
    track: Track? = null,
    index: Int? = null,
    songEntity: SongEntity? = null,
    isPlaying: Boolean,
    shouldShowDragHandle: Boolean = false,
    onMoreClickListener: ((videoId: String) -> Unit)? = null,
    onClickListener: ((videoId: String) -> Unit)? = null,
    onAddToQueue: ((videoId: String) -> Unit)? = null,
    selectionMode: Boolean = false,
    isSelected: Boolean = false,
    onLongClick: ((videoId: String) -> Unit)? = null,
    onSelectToggle: ((videoId: String) -> Unit)? = null,
    modifier: Modifier,
    rightView: @Composable (() -> Unit)? = null,
    forceDark: Boolean = LocalForceDarkText.current,
) {
    val contentColor = if (forceDark) Color.White else MaterialTheme.colorScheme.onSurface
    val subtitleColor = if (forceDark) Color(0xC4FFFFFF) else MaterialTheme.colorScheme.onSurfaceVariant
    val maxOffset = 360f
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val songRepository: SongRepository = koinInject<SongRepository>()
    val downloadState by songRepository
        .getSongAsFlow(songEntity?.videoId ?: track?.videoId ?: "")
        .mapNotNull { it?.downloadState }
        .collectAsState(initial = DownloadState.STATE_NOT_DOWNLOADED)
    val composition by rememberLottieComposition {
        LottieCompositionSpec.JsonString(
            Res.readBytes("files/audio_playing_animation.json").decodeToString(),
        )
    }
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
                        tint = contentColor,
                        imageVector = SimpIcons.QueueMusic,
                        contentDescription = stringResource(Res.string.add_to_queue),
                    )
                }
            }
        }
        val itemVideoId = track?.videoId ?: songEntity?.videoId ?: ""
        Box(
            modifier =
                modifier
                    .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                    .background(
                        if (isSelected) seed.copy(alpha = 0.18f) else Color.Transparent,
                    ).combinedClickable(
                        onClick = {
                            if (selectionMode) {
                                onSelectToggle?.invoke(itemVideoId)
                            } else {
                                onClickListener?.invoke(itemVideoId)
                            }
                        },
                        onLongClick =
                            if (onLongClick != null) {
                                { onLongClick(itemVideoId) }
                            } else {
                                null
                            },
                    ).animateContentSize()
                    // Keyed on selectionMode so the swipe detector is torn down when selection
                    // starts — keyed on Unit it would keep running with the stale flag captured.
                    .pointerInput(selectionMode) {
                        if (!isPlaying && onAddToQueue != null && !selectionMode) {
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
                    .padding(vertical = 6.dp, horizontal = 15.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Spacer(modifier = Modifier.width(8.dp))
                AnimatedVisibility(
                    visible = selectionMode,
                    enter = fadeIn() + expandHorizontally(),
                    exit = fadeOut() + shrinkHorizontally(),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier =
                                Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) seed else Color.Transparent)
                                    .border(
                                        width = 1.5.dp,
                                        color = if (isSelected) seed else contentColor.copy(alpha = 0.6f),
                                        shape = CircleShape,
                                    ),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = SimpIcons.Check,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(14.dp),
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                    }
                }
                Box(
                    modifier = Modifier.size(48.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Crossfade(isPlaying) {
                        if (it) {
                            Image(
                                painter =
                                    rememberLottiePainter(
                                        composition = composition,
                                        iterations = Compottie.IterateForever,
                                    ),
                                contentDescription = "Lottie animation",
                            )
                        } else if (index == null) {
                            val thumb = track?.thumbnails?.lastOrNull()?.url ?: songEntity?.thumbnails
                            AsyncImage(
                                model =
                                    ImageRequest
                                        .Builder(LocalPlatformContext.current)
                                        .data(thumb)
                                        .diskCachePolicy(CachePolicy.ENABLED)
                                        .diskCacheKey(thumb)
                                        .crossfade(true)
                                        .build(),
                                placeholder = rememberHolderPainter(),
                                error = rememberHolderPainter(),
                                contentDescription = null,
                                contentScale = ContentScale.FillWidth,
                                modifier =
                                    Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(4.dp)),
                            )
                        } else {
                            Text(
                                text = (index + 1).toString(),
                                color = contentColor,
                                style = typo().titleMedium,
                                modifier = Modifier.align(Alignment.Center),
                            )
                        }
                    }
                }
                Column(
                    Modifier
                        .weight(1f)
                        .padding(start = 12.dp, end = 10.dp)
                        .align(Alignment.CenterVertically),
                    verticalArrangement = Arrangement.SpaceEvenly,
                ) {
                    Text(
                        text = track?.title ?: songEntity?.title ?: "",
                        style = typo().titleSmall,
                        maxLines = 1,
                        color = contentColor,
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
                                    imageVector = SimpIcons.DownloadForOffline,
                                    tint = contentColor,
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
                            style = typo().bodySmall,
                            maxLines = 1,
                            color = subtitleColor,
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
                if (rightView != null) {
                    rightView()
                }
                // Hidden while selecting: the per-item menu moves up to the selection app bar,
                // so one tap cannot mean both "act on this song" and "pick this song".
                if (onMoreClickListener != null && !selectionMode) {
                    RippleIconButton(imageVector = SimpIcons.MoreVert, fillMaxSize = false, tint = contentColor) {
                        if (itemVideoId.isNotBlank()) onMoreClickListener.invoke(itemVideoId)
                    }
                }
                AnimatedVisibility(
                    shouldShowDragHandle,
                    enter = fadeIn() + expandHorizontally(),
                    exit = fadeOut() + shrinkHorizontally(),
                ) {
                    Icon(
                        SimpIcons.DragHandle,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.padding(horizontal = 8.dp),
                    )
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
    forceDark: Boolean = LocalForceDarkText.current,
) {
    val contentColor = if (forceDark) Color.White else MaterialTheme.colorScheme.onSurface
    val subtitleColor = if (forceDark) Color(0xC4FFFFFF) else MaterialTheme.colorScheme.onSurfaceVariant
    val composition by rememberLottieComposition {
        LottieCompositionSpec.JsonString(
            Res.readBytes("files/audio_playing_animation.json").decodeToString(),
        )
    }
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
                .padding(vertical = 6.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.size(40.dp)) {
                Crossfade(isPlaying) {
                    if (it) {
                        Image(
                            painter =
                                rememberLottiePainter(
                                    composition = composition,
                                    iterations = Compottie.IterateForever,
                                ),
                            contentDescription = "Lottie animation",
                        )
                    } else {
                        val thumb = track.thumbnails?.lastOrNull()?.url
                        AsyncImage(
                            model =
                                ImageRequest
                                    .Builder(LocalPlatformContext.current)
                                    .data(thumb)
                                    .diskCachePolicy(CachePolicy.ENABLED)
                                    .diskCacheKey(thumb)
                                    .crossfade(true)
                                    .build(),
                            placeholder = rememberHolderPainter(),
                            error = rememberHolderPainter(),
                            contentDescription = null,
                            contentScale = ContentScale.FillWidth,
                            modifier =
                                Modifier
                                    .wrapContentHeight()
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(4.dp)),
                        )
                    }
                }
            }
            Column(
                Modifier
                    .weight(1f)
                    .padding(start = 12.dp, end = 10.dp)
                    .align(Alignment.CenterVertically),
            ) {
                Text(
                    text = track.title,
                    style = typo().titleSmall,
                    maxLines = 1,
                    color = contentColor,
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
                    style = typo().bodySmall,
                    maxLines = 1,
                    color = subtitleColor,
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
                imageVector = SimpIcons.Add,
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
    rightView: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    forceDark: Boolean = LocalForceDarkText.current,
) {
    val contentColor = if (forceDark) Color.White else MaterialTheme.colorScheme.onSurface
    val subtitleColor = if (forceDark) Color(0xC4FFFFFF) else MaterialTheme.colorScheme.onSurfaceVariant
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

        var shouldPin = false

        firstSubtitle =
            when (data.playlistType()) {
                PlaylistType.Type.YOUTUBE_PLAYLIST -> stringResource(Res.string.playlist)
                PlaylistType.Type.RADIO -> stringResource(Res.string.radio)
                PlaylistType.Type.LOCAL -> stringResource(Res.string.playlist)
                PlaylistType.Type.ALBUM -> stringResource(Res.string.album)
                PlaylistType.Type.PODCAST -> stringResource(Res.string.podcasts)
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
                if (data.description == "PIN") { // LIKED MUSIC
                    shouldPin = true
                }
            }

            is LocalPlaylistEntity -> {
                title = data.title
                thumb = data.thumbnail ?: ""
                secondSubtitle = stringResource(Res.string.you)
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
                            .Builder(LocalPlatformContext.current)
                            .data(thumb)
                            .diskCachePolicy(CachePolicy.ENABLED)
                            .diskCacheKey(thumb)
                            .crossfade(true)
                            .build(),
                    placeholder = rememberHolderPainter(),
                    error = rememberHolderPainter(),
                    contentDescription = null,
                    contentScale = ContentScale.FillWidth,
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(4.dp)),
                )
            }
            Column(
                Modifier
                    .weight(1f)
                    .padding(start = 12.dp, end = 10.dp)
                    .align(Alignment.CenterVertically),
            ) {
                Text(
                    text = title,
                    style = typo().titleSmall,
                    maxLines = 1,
                    color = contentColor,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(align = Alignment.CenterVertically)
                            .basicMarquee(
                                iterations = Int.MAX_VALUE,
                                animationMode = MarqueeAnimationMode.Immediately,
                            ).focusable(),
                )

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (shouldPin) {
                        Image(
                            imageVector = SimpIcons.PushPin,
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(if (forceDark) Color.Cyan else MaterialTheme.colorScheme.primary),
                            modifier =
                                Modifier
                                    .rotate(30f)
                                    .size(16.dp),
                        )
                    }
                    Text(
                        text = "$firstSubtitle ${if (secondSubtitle.isNotEmpty()) " • $secondSubtitle" else ""}",
                        style = typo().bodySmall,
                        maxLines = 1,
                        color = subtitleColor,
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

                if (thirdRowSubtitle != null) {
                    Text(
                        text = thirdRowSubtitle,
                        style = typo().bodySmall,
                        maxLines = 1,
                        color = subtitleColor,
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
            if (rightView != null) {
                rightView()
            }
        }
    }
}

@Composable
fun ArtistFullWidthItems(
    data: ArtistType,
    onClickListener: (() -> Unit)? = null,
    rightView: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    forceDark: Boolean = LocalForceDarkText.current,
) {
    val contentColor = if (forceDark) Color.White else MaterialTheme.colorScheme.onSurface
    val subtitleColor = if (forceDark) Color(0xC4FFFFFF) else MaterialTheme.colorScheme.onSurfaceVariant
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
                            .Builder(LocalPlatformContext.current)
                            .data(thumbnails)
                            .diskCachePolicy(CachePolicy.ENABLED)
                            .diskCacheKey(thumbnails)
                            .crossfade(true)
                            .build(),
                    placeholder = rememberHolderPainter(),
                    error = rememberHolderPainter(),
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
                    .padding(start = 12.dp, end = 10.dp)
                    .align(Alignment.CenterVertically),
            ) {
                Text(
                    text = name,
                    style = typo().titleSmall,
                    maxLines = 1,
                    color = contentColor,
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
                    text = stringResource(Res.string.artists),
                    style = typo().bodySmall,
                    maxLines = 1,
                    color = subtitleColor,
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
            if (rightView != null) {
                rightView()
            }
        }
    }
}