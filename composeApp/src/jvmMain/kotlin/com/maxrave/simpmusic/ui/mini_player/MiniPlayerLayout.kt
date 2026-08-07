package com.maxrave.simpmusic.ui.mini_player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.MarqueeAnimationMode
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.maxrave.domain.data.model.browse.album.Track
import com.maxrave.domain.data.model.streams.TimeLine
import com.maxrave.domain.mediaservice.handler.ControlState
import com.maxrave.simpmusic.ui.component.LyricsView
import com.maxrave.simpmusic.ui.component.PlayPauseButton
import com.maxrave.simpmusic.ui.component.RichSyncLyricsLineItem
import com.maxrave.simpmusic.ui.component.RippleIconButton
import com.maxrave.simpmusic.ui.component.RingPlayerArtwork
import com.maxrave.simpmusic.ui.component.rememberHolderPainter
import com.maxrave.simpmusic.extension.formatDuration
import com.maxrave.simpmusic.extension.parseRichSyncWords
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.NowPlayingScreenData
import com.maxrave.simpmusic.viewModel.UIEvent
import kotlinx.coroutines.flow.StateFlow
import org.jetbrains.compose.resources.painterResource
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.baseline_keyboard_double_arrow_down_24
import simpmusic.composeapp.generated.resources.baseline_keyboard_double_arrow_up_24
import simpmusic.composeapp.generated.resources.baseline_skip_next_24
import simpmusic.composeapp.generated.resources.baseline_skip_previous_24

@Composable
private fun MiniPlayerSeekBar(
    timeline: TimeLine,
    onUIEvent: (UIEvent) -> Unit,
    modifier: Modifier = Modifier,
    trackHeight: Dp = 4.dp,
    thumbSize: Dp = 6.dp,
    hitHeight: Dp = 24.dp,
) {
    if (timeline.total <= 0L) return

    val progress =
        (timeline.current.toFloat() / timeline.total.toFloat())
            .coerceIn(0f, 1f)

    BoxWithConstraints(
        modifier =
            modifier
                .fillMaxWidth()
                .height(hitHeight)
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val percent =
                            (offset.x / size.width)
                                .coerceIn(0f, 1f) * 100f
                        onUIEvent(UIEvent.UpdateProgress(percent))
                    }
                }.pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val percent =
                                (offset.x / size.width)
                                    .coerceIn(0f, 1f) * 100f
                            onUIEvent(UIEvent.UpdateProgress(percent))
                        },
                        onDrag = { change, _ ->
                            val percent =
                                (change.position.x / size.width)
                                    .coerceIn(0f, 1f) * 100f
                            onUIEvent(UIEvent.UpdateProgress(percent))
                        },
                    )
                },
        contentAlignment = Alignment.CenterStart,
    ) {
        // Track
        Box(
            Modifier
                .fillMaxWidth()
                .height(trackHeight)
                .align(Alignment.Center)
                .background(
                    Color.White.copy(alpha = 0.25f),
                    RoundedCornerShape(50),
                ),
        )

        // Progress
        Box(
            Modifier
                .width(maxWidth * progress)
                .height(trackHeight)
                .align(Alignment.CenterStart)
                .background(
                    Color.White,
                    RoundedCornerShape(50),
                ),
        )

        // Thumb
        Box(
            Modifier
                .offset(x = (maxWidth * progress) - (thumbSize / 2))
                .size(thumbSize)
                .align(Alignment.CenterStart)
                .background(Color.White, CircleShape),
        )
    }
}

/**
 * Time label shown in place of the seek bar when ring player mode is on — the ring already
 * shows progress and handles seeking, so the bar is replaced with a "current / total" readout.
 */
@Composable
private fun MiniPlayerTimeLabel(
    timeline: TimeLine,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.End,
    ) {
        Text(
            text = "${formatDuration(timeline.current)} / ${formatDuration(timeline.total)}",
            style = typo().bodySmall,
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 11.sp,
        )
    }
}

/**
 * Shared top bar for the mini player: album art + track/artist info + transport controls
 * plus an expand/collapse chevron (double arrow up when collapsed, down when expanded).
 */
@Composable
private fun MiniPlayerBarContent(
    nowPlayingData: NowPlayingScreenData,
    controllerState: ControlState,
    timeline: TimeLine,
    ringPlayerEnabled: Boolean,
    expanded: Boolean,
    onUIEvent: (UIEvent) -> Unit,
    onToggleExpand: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Album art (ring player mode shows a spinning vinyl with progress ring)
        if (ringPlayerEnabled) {
            RingPlayerArtwork(
                thumbnailURL = nowPlayingData.thumbnailURL,
                isPlaying = controllerState.isPlaying,
                progress =
                    if (timeline.total > 0) {
                        timeline.current.toFloat() / timeline.total.toFloat()
                    } else {
                        0f
                    },
                onSeek = { p ->
                    onUIEvent(UIEvent.UpdateProgress(p * 100f))
                },
                modifier = Modifier.size(52.dp),
                ringWidth = 3.dp,
                rimWidth = 1.dp,
            )
        } else {
            AsyncImage(
                model = nowPlayingData.thumbnailURL,
                contentDescription = "Album Art",
                placeholder = rememberHolderPainter(),
                error = rememberHolderPainter(),
                contentScale = ContentScale.Crop,
                modifier =
                    Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(8.dp)),
            )
        }

        // Track + artist info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = nowPlayingData.nowPlayingTitle,
                style = typo().bodyMedium,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 14.sp,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = nowPlayingData.artistName,
                style = typo().bodySmall,
                color = Color(0xFF9A9A9A),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 12.sp,
            )
        }

        // Transport controls
        Row(
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Like button
            IconButton(
                onClick = { onUIEvent(UIEvent.ToggleLike) },
                modifier = Modifier.size(28.dp),
            ) {
                Icon(
                    imageVector =
                        if (controllerState.isLiked) {
                            Icons.Filled.Favorite
                        } else {
                            Icons.Outlined.FavoriteBorder
                        },
                    contentDescription = "Like",
                    tint =
                        if (controllerState.isLiked) {
                            Color(0xFFFF4081)
                        } else {
                            Color.White.copy(alpha = 0.7f)
                        },
                    modifier = Modifier.size(18.dp),
                )
            }

            RippleIconButton(
                resId = Res.drawable.baseline_skip_previous_24,
                modifier = Modifier.size(28.dp),
                tint = if (controllerState.isPreviousAvailable) Color.White else Color.Gray,
                onClick = {
                    if (controllerState.isPreviousAvailable) {
                        onUIEvent(UIEvent.Previous)
                    }
                },
            )

            PlayPauseButton(
                isPlaying = controllerState.isPlaying,
                modifier = Modifier.size(38.dp),
                onClick = { onUIEvent(UIEvent.PlayPause) },
            )

            RippleIconButton(
                resId = Res.drawable.baseline_skip_next_24,
                modifier = Modifier.size(28.dp),
                tint = if (controllerState.isNextAvailable) Color.White else Color.Gray,
                onClick = {
                    if (controllerState.isNextAvailable) {
                        onUIEvent(UIEvent.Next)
                    }
                },
            )

            // Volume button
            IconButton(
                onClick = {
                    val newVolume = if (controllerState.volume > 0f) 0f else 1f
                    onUIEvent(UIEvent.UpdateVolume(newVolume))
                },
                modifier = Modifier.size(28.dp),
            ) {
                Icon(
                    imageVector =
                        if (controllerState.volume > 0f) {
                            Icons.AutoMirrored.Filled.VolumeUp
                        } else {
                            Icons.AutoMirrored.Filled.VolumeOff
                        },
                    contentDescription = if (controllerState.volume > 0f) "Mute" else "Unmute",
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp),
                )
            }

            // Expand/collapse chevron
            IconButton(
                onClick = onToggleExpand,
                modifier = Modifier.size(28.dp),
            ) {
                Icon(
                    painter =
                        painterResource(
                            if (expanded) {
                                Res.drawable.baseline_keyboard_double_arrow_down_24
                            } else {
                                Res.drawable.baseline_keyboard_double_arrow_up_24
                            },
                        ),
                    contentDescription = if (expanded) "Collapse lyrics" else "Expand lyrics",
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}

/**
 * Default collapsed mini player layout: album art + track/artist info + controls + seek bar.
 * Clean Spotify-style bar themed from the album artwork's dominant color.
 */
@Composable
fun MiniPlayerBarLayout(
    nowPlayingData: NowPlayingScreenData,
    controllerState: ControlState,
    timeline: TimeLine,
    background: Color,
    ringPlayerEnabled: Boolean,
    onUIEvent: (UIEvent) -> Unit,
    onToggleExpand: () -> Unit,
) {
    Surface(
        modifier =
            Modifier
                .fillMaxSize()
                .animateContentSize(animationSpec = tween(300)),
        color = background,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            MiniPlayerBarContent(
                nowPlayingData = nowPlayingData,
                controllerState = controllerState,
                timeline = timeline,
                ringPlayerEnabled = ringPlayerEnabled,
                expanded = false,
                onUIEvent = onUIEvent,
                onToggleExpand = onToggleExpand,
            )

            if (ringPlayerEnabled) {
                // Ring player mode: the ring is the progress/seek control, so the seek bar
                // is replaced with a simple current/total time readout.
                MiniPlayerTimeLabel(
                    timeline = timeline,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                )
            } else {
                Box(modifier = Modifier.padding(horizontal = 12.dp)) {
                    MiniPlayerSeekBar(
                        timeline = timeline,
                        onUIEvent = onUIEvent,
                    )
                }
            }
        }
    }
}

/**
 * Expanded mini player layout: the compact bar on top, then a Lyrics / Up Next tab row,
 * and either a full scrolling list of animated synced lyrics (current line highlighted +
 * auto-centered) or the upcoming queue, with the seek bar at the bottom. Themed from the
 * album artwork's dominant color.
 */
@Composable
fun LyricsExpandedMiniLayout(
    nowPlayingData: NowPlayingScreenData,
    controllerState: ControlState,
    timeline: TimeLine,
    timelineFlow: StateFlow<TimeLine>,
    lyricsData: NowPlayingScreenData.LyricsData?,
    queue: List<Track>,
    currentVideoId: String?,
    background: Color,
    ringPlayerEnabled: Boolean,
    onUIEvent: (UIEvent) -> Unit,
    onToggleExpand: () -> Unit,
    onPlayQueueItem: (Int) -> Unit,
) {
    var tab by remember { mutableStateOf(MiniExpandedTab.LYRICS) }

    Surface(
        modifier =
            Modifier
                .fillMaxSize()
                .animateContentSize(animationSpec = tween(300)),
        color = background,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            MiniPlayerBarContent(
                nowPlayingData = nowPlayingData,
                controllerState = controllerState,
                timeline = timeline,
                ringPlayerEnabled = ringPlayerEnabled,
                expanded = true,
                onUIEvent = onUIEvent,
                onToggleExpand = onToggleExpand,
            )

            // Divider between bar and content
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color.White.copy(alpha = 0.08f)),
            )

            // Lyrics / Up Next tabs
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MiniPlayerTab(
                    title = "Lyrics",
                    selected = tab == MiniExpandedTab.LYRICS,
                    onClick = { tab = MiniExpandedTab.LYRICS },
                )
                MiniPlayerTab(
                    title = "Up Next",
                    selected = tab == MiniExpandedTab.QUEUE,
                    onClick = { tab = MiniExpandedTab.QUEUE },
                )
            }

            if (tab == MiniExpandedTab.QUEUE) {
                MiniPlayerQueueView(
                    queue = queue,
                    currentVideoId = currentVideoId,
                    onPlayQueueItem = onPlayQueueItem,
                    modifier =
                        Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                )
            } else {
                // Animated synced lyrics
                val hasLyrics =
                    lyricsData != null && !lyricsData.lyrics.error && !lyricsData.lyrics.lines.isNullOrEmpty()
                if (hasLyrics) {
                    LyricsView(
                        lyricsData = lyricsData,
                        timeLine = timelineFlow,
                        onLineClick = { f ->
                            onUIEvent(UIEvent.UpdateProgress(f))
                        },
                        modifier =
                            Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                    )
                } else {
                    Box(
                        modifier =
                            Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = if (lyricsData == null) "Loading lyrics..." else "Lyrics not found",
                            style = typo().bodyMedium,
                            color = Color.White.copy(alpha = 0.5f),
                        )
                    }
                }
            }

            // Seek bar (replaced by the ring + a time readout in ring player mode)
            if (ringPlayerEnabled) {
                MiniPlayerTimeLabel(
                    timeline = timeline,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                )
            } else {
                Box(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    MiniPlayerSeekBar(
                        timeline = timeline,
                        onUIEvent = onUIEvent,
                    )
                }
            }
        }
    }
}

private enum class MiniExpandedTab {
    LYRICS,
    QUEUE,
}

@Composable
private fun MiniPlayerTab(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Text(
        text = title,
        style = typo().bodyMedium,
        color = if (selected) Color.White else Color.White.copy(alpha = 0.5f),
        fontSize = 13.sp,
        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        modifier =
            Modifier
                .clip(RoundedCornerShape(50))
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 5.dp),
    )
}

/**
 * Up Next queue list for the expanded mini player. Highlights the currently playing track
 * and jumps to the tapped track via [onPlayQueueItem].
 */
@Composable
private fun MiniPlayerQueueView(
    queue: List<Track>,
    currentVideoId: String?,
    onPlayQueueItem: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (queue.isEmpty()) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Nothing up next",
                style = typo().bodyMedium,
                color = Color.White.copy(alpha = 0.5f),
            )
        }
        return
    }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(vertical = 8.dp),
    ) {
        itemsIndexed(queue) { index, track ->
            val isCurrent = track.videoId == currentVideoId
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable { onPlayQueueItem(index) }
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AsyncImage(
                    model = track.thumbnails?.lastOrNull()?.url ?: track.thumbnails?.firstOrNull()?.url,
                    contentDescription = null,
                    placeholder = rememberHolderPainter(),
                    error = rememberHolderPainter(),
                    contentScale = ContentScale.Crop,
                    modifier =
                        Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(6.dp)),
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        style = typo().bodyMedium,
                        color = if (isCurrent) Color.White else Color.White.copy(alpha = 0.85f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 13.sp,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = track.artists?.joinToString(", ") { it.name }.orEmpty(),
                        style = typo().bodySmall,
                        color = if (isCurrent) Color.White.copy(alpha = 0.9f) else Color.White.copy(alpha = 0.45f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 11.sp,
                    )
                }

                if (isCurrent) {
                    Text(
                        text = "Playing",
                        style = typo().bodySmall,
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 11.sp,
                    )
                }
            }
        }
    }
}

/**
 * Compact layout (< 260dp): Controls only, no artwork or text
 * Perfect for very narrow windows
 */
@Composable
fun CompactMiniLayout(
    controllerState: ControlState,
    timeline: TimeLine,
    onUIEvent: (UIEvent) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val alpha by animateFloatAsState(
        targetValue = if (isHovered) 1f else 0.9f,
        animationSpec = tween(200),
    )

    Surface(
        modifier =
            Modifier
                .fillMaxSize()
                .animateContentSize(animationSpec = tween(300))
                .hoverable(interactionSource),
        color = Color(0xFF1C1C1E),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Controls only - centered
            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .alpha(alpha),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RippleIconButton(
                        resId = Res.drawable.baseline_skip_previous_24,
                        modifier = Modifier.size(28.dp),
                        tint = if (controllerState.isPreviousAvailable) Color.White else Color.Gray,
                        onClick = {
                            if (controllerState.isPreviousAvailable) {
                                onUIEvent(UIEvent.Previous)
                            }
                        },
                    )

                    PlayPauseButton(
                        isPlaying = controllerState.isPlaying,
                        modifier = Modifier.size(36.dp),
                        onClick = { onUIEvent(UIEvent.PlayPause) },
                    )

                    RippleIconButton(
                        resId = Res.drawable.baseline_skip_next_24,
                        modifier = Modifier.size(28.dp),
                        tint = if (controllerState.isNextAvailable) Color.White else Color.Gray,
                        onClick = {
                            if (controllerState.isNextAvailable) {
                                onUIEvent(UIEvent.Next)
                            }
                        },
                    )
                }
            }

            // Seek bar
            Box(
                modifier = Modifier.padding(horizontal = 12.dp),
            ) {
                MiniPlayerSeekBar(
                    timeline = timeline,
                    onUIEvent = onUIEvent,
                )
            }
        }
    }
}

/**
 * Medium layout (260-360dp): Artwork + controls, no text
 * Good balance for medium-sized windows
 */
@Composable
fun MediumMiniLayout(
    nowPlayingData: NowPlayingScreenData,
    controllerState: ControlState,
    timeline: TimeLine,
    lyricsData: NowPlayingScreenData.LyricsData?,
    onUIEvent: (UIEvent) -> Unit,
) {
    val artworkInteractionSource = remember { MutableInteractionSource() }
    val isArtworkHovered by artworkInteractionSource.collectIsHoveredAsState()
    val artworkScale by animateFloatAsState(
        targetValue = if (isArtworkHovered) 1.05f else 1f,
        animationSpec = tween(200),
    )

    Surface(
        modifier =
            Modifier
                .fillMaxSize()
                .animateContentSize(animationSpec = tween(300)),
        color = Color(0xFF1C1C1E),
    ) {
        BoxWithConstraints {
            val showExtraButtons = maxWidth >= 300.dp

            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier =
                        Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // Smaller artwork with hover effect
                    AsyncImage(
                        model = nowPlayingData.thumbnailURL,
                        contentDescription = "Album Art",
                        placeholder = rememberHolderPainter(),
                        error = rememberHolderPainter(),
                        contentScale = ContentScale.Crop,
                        modifier =
                            Modifier
                                .size(48.dp)
                                .scale(artworkScale)
                                .clip(RoundedCornerShape(6.dp))
                                .hoverable(artworkInteractionSource),
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    // Controls - show extra buttons only if width >= 300dp
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Like button - only show if width >= 300dp
                        AnimatedVisibility(
                            visible = showExtraButtons,
                            enter = scaleIn() + fadeIn(),
                            exit = scaleOut() + fadeOut(),
                        ) {
                            IconButton(
                                onClick = { onUIEvent(UIEvent.ToggleLike) },
                                modifier = Modifier.size(28.dp),
                            ) {
                                Icon(
                                    imageVector =
                                        if (controllerState.isLiked) {
                                            Icons.Filled.Favorite
                                        } else {
                                            Icons.Outlined.FavoriteBorder
                                        },
                                    contentDescription = "Like",
                                    tint =
                                        if (controllerState.isLiked) {
                                            Color(0xFFFF4081)
                                        } else {
                                            Color.White.copy(alpha = 0.7f)
                                        },
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }

                        RippleIconButton(
                            resId = Res.drawable.baseline_skip_previous_24,
                            modifier = Modifier.size(28.dp),
                            tint = if (controllerState.isPreviousAvailable) Color.White else Color.Gray,
                            onClick = {
                                if (controllerState.isPreviousAvailable) {
                                    onUIEvent(UIEvent.Previous)
                                }
                            },
                        )

                        PlayPauseButton(
                            isPlaying = controllerState.isPlaying,
                            modifier = Modifier.size(36.dp),
                            onClick = { onUIEvent(UIEvent.PlayPause) },
                        )

                        RippleIconButton(
                            resId = Res.drawable.baseline_skip_next_24,
                            modifier = Modifier.size(28.dp),
                            tint = if (controllerState.isNextAvailable) Color.White else Color.Gray,
                            onClick = {
                                if (controllerState.isNextAvailable) {
                                    onUIEvent(UIEvent.Next)
                                }
                            },
                        )

                        // Volume button - only show if width >= 300dp
                        AnimatedVisibility(
                            visible = showExtraButtons,
                            enter = scaleIn() + fadeIn(),
                            exit = scaleOut() + fadeOut(),
                        ) {
                            IconButton(
                                onClick = {
                                    // Toggle mute/unmute
                                    val newVolume = if (controllerState.volume > 0f) 0f else 1f
                                    onUIEvent(UIEvent.UpdateVolume(newVolume))
                                },
                                modifier = Modifier.size(28.dp),
                            ) {
                                Icon(
                                    imageVector =
                                        if (controllerState.volume > 0f) {
                                            Icons.AutoMirrored.Filled.VolumeUp
                                        } else {
                                            Icons.AutoMirrored.Filled.VolumeOff
                                        },
                                    contentDescription = if (controllerState.volume > 0f) "Mute" else "Unmute",
                                    tint = Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }
                }

                // Lyrics display (if available)
                if (lyricsData != null && !lyricsData.lyrics.error && lyricsData.lyrics.lines != null) {
                    val currentLine =
                        remember(timeline.current) {
                            lyricsData.lyrics.lines?.findLast { line ->
                                line.startTimeMs.toLongOrNull()?.let { it <= timeline.current } ?: false
                            }
                        }

                    if (currentLine != null) {
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (lyricsData.lyrics.syncType == "RICH_SYNCED") {
                                val parsedLine =
                                    remember(currentLine.words, currentLine.startTimeMs, currentLine.endTimeMs) {
                                        val result = parseRichSyncWords(currentLine.words, currentLine.startTimeMs, currentLine.endTimeMs)
                                        result
                                    }

                                if (parsedLine != null) {
                                    RichSyncLyricsLineItem(
                                        parsedLine = parsedLine,
                                        translatedWords = null,
                                        currentTimeMs = timeline.current,
                                        isCurrent = true,
                                        customFontSize = typo().bodySmall.fontSize,
                                        modifier = Modifier,
                                    )
                                }
                            } else {
                                Text(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .wrapContentHeight(
                                                align = Alignment.CenterVertically,
                                            ).basicMarquee(
                                                iterations = Int.MAX_VALUE,
                                                animationMode = MarqueeAnimationMode.Immediately,
                                            ).focusable(),
                                    textAlign = TextAlign.Center,
                                    text = currentLine.words,
                                    style = typo().bodySmall,
                                    color = Color.White.copy(alpha = 0.9f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontSize = 11.sp,
                                )
                            }
                        }
                    }
                }

                // Seek bar
                Box(
                    modifier = Modifier.padding(horizontal = 12.dp),
                ) {
                    MiniPlayerSeekBar(
                        timeline = timeline,
                        onUIEvent = onUIEvent,
                    )
                }
            }
        }
    }
}

/**
 * Square/Tall layout (Spotify-style): Large artwork centered with controls below
 * Appears when window is square or taller (aspect ratio <= 1.3)
 * Includes like/favorite and volume/mute buttons
 */
@Composable
fun SquareMiniLayout(
    nowPlayingData: NowPlayingScreenData,
    controllerState: ControlState,
    timeline: TimeLine,
    lyricsData: NowPlayingScreenData.LyricsData?,
    onUIEvent: (UIEvent) -> Unit,
) {
    val artworkInteractionSource = remember { MutableInteractionSource() }
    val isArtworkHovered by artworkInteractionSource.collectIsHoveredAsState()
    val artworkScale by animateFloatAsState(
        targetValue = if (isArtworkHovered) 1.03f else 1f,
        animationSpec = tween(300),
    )

    Surface(
        modifier =
            Modifier
                .fillMaxSize()
                .animateContentSize(animationSpec = tween(300)),
        color = Color(0xFF1C1C1E),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Large centered album artwork
            AsyncImage(
                model = nowPlayingData.thumbnailURL,
                contentDescription = "Album Art",
                placeholder = rememberHolderPainter(),
                error = rememberHolderPainter(),
                contentScale = ContentScale.Crop,
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxWidth(0.85f)
                        .scale(artworkScale)
                        .clip(RoundedCornerShape(12.dp))
                        .hoverable(artworkInteractionSource),
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Track info
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = nowPlayingData.nowPlayingTitle,
                    style = typo().bodyLarge,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 16.sp,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = nowPlayingData.artistName,
                    style = typo().bodyMedium,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 13.sp,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Lyrics display (if available)
            if (lyricsData != null && !lyricsData.lyrics.error && lyricsData.lyrics.lines != null) {
                val currentLine =
                    remember(timeline.current) {
                        lyricsData.lyrics.lines?.findLast { line ->
                            line.startTimeMs.toLongOrNull()?.let { it <= timeline.current } ?: false
                        }
                    }

                if (currentLine != null) {
                    if (lyricsData.lyrics.syncType == "RICH_SYNCED") {
                        val parsedLine =
                            remember(currentLine.words, currentLine.startTimeMs, currentLine.endTimeMs) {
                                val result = parseRichSyncWords(currentLine.words, currentLine.startTimeMs, currentLine.endTimeMs)
                                result
                            }

                        if (parsedLine != null) {
                            RichSyncLyricsLineItem(
                                parsedLine = parsedLine,
                                translatedWords = null,
                                currentTimeMs = timeline.current,
                                isCurrent = true,
                                customFontSize = typo().bodySmall.fontSize,
                                modifier = Modifier.padding(horizontal = 8.dp),
                            )
                        }
                    } else {
                        Text(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp)
                                    .wrapContentHeight(
                                        align = Alignment.CenterVertically,
                                    ).basicMarquee(
                                        iterations = Int.MAX_VALUE,
                                        animationMode = MarqueeAnimationMode.Immediately,
                                    ).focusable(),
                            textAlign = TextAlign.Center,
                            text = currentLine.words,
                            style = typo().bodySmall,
                            color = Color.White.copy(alpha = 0.9f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 12.sp,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }

            // Seek bar
            Box(
                modifier = Modifier.padding(horizontal = 12.dp),
            ) {
                MiniPlayerSeekBar(
                    timeline = timeline,
                    onUIEvent = onUIEvent,
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            // Main playback controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Like/Favorite button
                IconButton(
                    onClick = { onUIEvent(UIEvent.ToggleLike) },
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        imageVector =
                            if (controllerState.isLiked) {
                                Icons.Filled.Favorite
                            } else {
                                Icons.Outlined.FavoriteBorder
                            },
                        contentDescription = "Like",
                        tint =
                            if (controllerState.isLiked) {
                                Color(0xFFFF4081)
                            } else {
                                Color.White.copy(alpha = 0.7f)
                            },
                        modifier = Modifier.size(24.dp),
                    )
                }

                // Previous
                RippleIconButton(
                    resId = Res.drawable.baseline_skip_previous_24,
                    modifier = Modifier.size(36.dp),
                    tint = if (controllerState.isPreviousAvailable) Color.White else Color.Gray,
                    onClick = {
                        if (controllerState.isPreviousAvailable) {
                            onUIEvent(UIEvent.Previous)
                        }
                    },
                )

                // Play/Pause
                PlayPauseButton(
                    isPlaying = controllerState.isPlaying,
                    modifier = Modifier.size(52.dp),
                    onClick = { onUIEvent(UIEvent.PlayPause) },
                )

                // Next
                RippleIconButton(
                    resId = Res.drawable.baseline_skip_next_24,
                    modifier = Modifier.size(36.dp),
                    tint = if (controllerState.isNextAvailable) Color.White else Color.Gray,
                    onClick = {
                        if (controllerState.isNextAvailable) {
                            onUIEvent(UIEvent.Next)
                        }
                    },
                )

                // Volume/Mute button
                IconButton(
                    onClick = {
                        // Toggle mute/unmute
                        val newVolume = if (controllerState.volume > 0f) 0f else 1f
                        onUIEvent(UIEvent.UpdateVolume(newVolume))
                    },
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        imageVector =
                            if (controllerState.volume > 0f) {
                                Icons.AutoMirrored.Filled.VolumeUp
                            } else {
                                Icons.AutoMirrored.Filled.VolumeOff
                            },
                        contentDescription = if (controllerState.volume > 0f) "Mute" else "Unmute",
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(24.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

/**
 * Empty state when no track is playing
 */
@Composable
fun EmptyMiniPlayerState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "No track playing",
                style = typo().bodyMedium.copy(fontSize = 13.sp),
                color = Color.White.copy(alpha = 0.6f),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Play something to see controls",
                style = typo().bodySmall.copy(fontSize = 11.sp),
                color = Color.White.copy(alpha = 0.4f),
            )
        }
    }
}

/**
 * Legacy full layout - now used only when Box shows > 360dp
 * Kept for backwards compatibility
 */
@Composable
fun ExpandedMiniLayout(
    nowPlayingData: NowPlayingScreenData,
    controllerState: ControlState,
    timeline: TimeLine,
    lyricsData: NowPlayingScreenData.LyricsData?,
    onUIEvent: (UIEvent) -> Unit,
) {
    val artworkInteractionSource = remember { MutableInteractionSource() }
    val isArtworkHovered by artworkInteractionSource.collectIsHoveredAsState()
    val artworkScale by animateFloatAsState(
        targetValue = if (isArtworkHovered) 1.08f else 1f,
        animationSpec = tween(250),
    )

    Surface(
        modifier =
            Modifier
                .fillMaxSize()
                .animateContentSize(animationSpec = tween(300)),
        color = Color(0xFF1C1C1E),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            // Main content area
            Row(
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Album artwork with hover animation
                AsyncImage(
                    model = nowPlayingData.thumbnailURL,
                    contentDescription = "Album Art",
                    placeholder = rememberHolderPainter(),
                    error = rememberHolderPainter(),
                    contentScale = ContentScale.Crop,
                    modifier =
                        Modifier
                            .size(64.dp)
                            .scale(artworkScale)
                            .clip(RoundedCornerShape(8.dp))
                            .hoverable(artworkInteractionSource),
                )

                // Track info
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = nowPlayingData.nowPlayingTitle,
                        style = typo().bodyMedium,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 14.sp,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = nowPlayingData.artistName,
                        style = typo().bodySmall,
                        color = Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 12.sp,
                    )
                }

                // Playback controls
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(tween(300, delayMillis = 150)),
                    exit = fadeOut(tween(200)),
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Like button
                        IconButton(
                            onClick = { onUIEvent(UIEvent.ToggleLike) },
                            modifier = Modifier.size(28.dp),
                        ) {
                            Icon(
                                imageVector =
                                    if (controllerState.isLiked) {
                                        Icons.Filled.Favorite
                                    } else {
                                        Icons.Outlined.FavoriteBorder
                                    },
                                contentDescription = "Like",
                                tint =
                                    if (controllerState.isLiked) {
                                        Color(0xFFFF4081)
                                    } else {
                                        Color.White.copy(alpha = 0.7f)
                                    },
                                modifier = Modifier.size(20.dp),
                            )
                        }

                        RippleIconButton(
                            resId = Res.drawable.baseline_skip_previous_24,
                            modifier = Modifier.size(28.dp),
                            tint = if (controllerState.isPreviousAvailable) Color.White else Color.Gray,
                            onClick = {
                                if (controllerState.isPreviousAvailable) {
                                    onUIEvent(UIEvent.Previous)
                                }
                            },
                        )

                        PlayPauseButton(
                            isPlaying = controllerState.isPlaying,
                            modifier = Modifier.size(40.dp),
                            onClick = {
                                onUIEvent(UIEvent.PlayPause)
                            },
                        )

                        RippleIconButton(
                            resId = Res.drawable.baseline_skip_next_24,
                            modifier = Modifier.size(32.dp),
                            tint = if (controllerState.isNextAvailable) Color.White else Color.Gray,
                            onClick = {
                                if (controllerState.isNextAvailable) {
                                    onUIEvent(UIEvent.Next)
                                }
                            },
                        )

                        // Volume button
                        IconButton(
                            onClick = {
                                // Toggle mute/unmute
                                val newVolume = if (controllerState.volume > 0f) 0f else 1f
                                onUIEvent(UIEvent.UpdateVolume(newVolume))
                            },
                            modifier = Modifier.size(28.dp),
                        ) {
                            Icon(
                                imageVector =
                                    if (controllerState.volume > 0f) {
                                        Icons.AutoMirrored.Filled.VolumeUp
                                    } else {
                                        Icons.AutoMirrored.Filled.VolumeOff
                                    },
                                contentDescription = if (controllerState.volume > 0f) "Mute" else "Unmute",
                                tint = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }

            // Lyrics display below thumbnail row
            if (lyricsData != null && !lyricsData.lyrics.error && lyricsData.lyrics.lines != null) {
                val currentLine =
                    remember(timeline.current) {
                        lyricsData.lyrics.lines?.findLast { line ->
                            line.startTimeMs.toLongOrNull()?.let { it <= timeline.current } ?: false
                        }
                    }

                if (currentLine != null) {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp),
                    ) {
                        if (lyricsData.lyrics.syncType == "RICH_SYNCED") {
                            val parsedLine =
                                remember(currentLine.words, currentLine.startTimeMs, currentLine.endTimeMs) {
                                    val result = parseRichSyncWords(currentLine.words, currentLine.startTimeMs, currentLine.endTimeMs)
                                    result
                                }

                            if (parsedLine != null) {
                                RichSyncLyricsLineItem(
                                    parsedLine = parsedLine,
                                    translatedWords = null,
                                    currentTimeMs = timeline.current,
                                    isCurrent = true,
                                    customFontSize = typo().bodySmall.fontSize,
                                    customPadding = 4.dp,
                                    modifier = Modifier,
                                )
                            }
                        } else {
                            Text(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .wrapContentHeight(
                                            align = Alignment.CenterVertically,
                                        ).basicMarquee(
                                            iterations = Int.MAX_VALUE,
                                            animationMode = MarqueeAnimationMode.Immediately,
                                        ).focusable()
                                        .padding(bottom = 4.dp),
                                textAlign = TextAlign.Center,
                                text = currentLine.words,
                                style = typo().bodySmall,
                                color = Color.White.copy(alpha = 0.9f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontSize = 11.sp,
                            )
                        }
                    }
                }
            }

            // Seek bar
            Box(
                modifier = Modifier.padding(horizontal = 12.dp),
            ) {
                MiniPlayerSeekBar(
                    timeline = timeline,
                    onUIEvent = onUIEvent,
                )
            }
        }
    }
}