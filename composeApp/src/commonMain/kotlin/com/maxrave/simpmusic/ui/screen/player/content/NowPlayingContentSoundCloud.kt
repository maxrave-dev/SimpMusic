package com.maxrave.simpmusic.ui.screen.player.content

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.IntState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.kyant.backdrop.highlight.Highlight
import com.maxrave.common.Config.MAIN_PLAYER
import com.maxrave.domain.data.model.comments.YouTubeComment
import com.maxrave.domain.mediaservice.handler.RepeatState
import com.maxrave.simpmusic.Platform
import com.maxrave.simpmusic.expect.ui.MediaPlayerView
import com.maxrave.simpmusic.expect.ui.MediaPlayerViewWithSubtitle
import com.maxrave.simpmusic.expect.ui.PlatformBackHandler
import com.maxrave.simpmusic.expect.ui.PlatformBackdrop
import com.maxrave.simpmusic.expect.ui.layerBackdrop
import com.maxrave.simpmusic.expect.ui.rememberBackdrop
import com.maxrave.simpmusic.expect.ui.toImageBitmap
import com.maxrave.simpmusic.extension.LeftToRight
import com.maxrave.simpmusic.extension.formatDuration
import com.maxrave.simpmusic.extension.lengthLabel
import com.maxrave.simpmusic.extension.smoothScrimBrush
import com.maxrave.simpmusic.getPlatform
import com.maxrave.simpmusic.ui.component.LiquidGlassContainer
import com.maxrave.simpmusic.ui.component.LiquidGlassIconButton
import com.maxrave.simpmusic.ui.component.RippleIconButton
import com.maxrave.simpmusic.ui.component.TIMESTAMP_REGEX
import com.maxrave.simpmusic.ui.component.liquidGlass
import com.maxrave.simpmusic.ui.component.rememberHolderPainter
import com.maxrave.simpmusic.ui.icon.AddCircleOutline
import com.maxrave.simpmusic.ui.icon.CheckCircle
import com.maxrave.simpmusic.ui.icon.CommentOutline
import com.maxrave.simpmusic.ui.icon.Favorite
import com.maxrave.simpmusic.ui.icon.FavoriteBorder
import com.maxrave.simpmusic.ui.icon.Forward5
import com.maxrave.simpmusic.ui.icon.Fullscreen
import com.maxrave.simpmusic.ui.icon.Headphones
import com.maxrave.simpmusic.ui.icon.Lyrics
import com.maxrave.simpmusic.ui.icon.MoreVert
import com.maxrave.simpmusic.ui.icon.PlayArrow
import com.maxrave.simpmusic.ui.icon.PlaylistAdd
import com.maxrave.simpmusic.ui.icon.QueueMusic
import com.maxrave.simpmusic.ui.icon.Repeat
import com.maxrave.simpmusic.ui.icon.RepeatOne
import com.maxrave.simpmusic.ui.icon.Replay5
import com.maxrave.simpmusic.ui.icon.Shuffle
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.icon.SkipNext
import com.maxrave.simpmusic.ui.icon.SkipPrevious
import com.maxrave.simpmusic.ui.icon.Subtitles
import com.maxrave.simpmusic.ui.icon.SubtitlesOff
import com.maxrave.simpmusic.ui.screen.player.content.applemusic.AppleMusicLyricStrip
import com.maxrave.simpmusic.ui.screen.player.content.applemusic.AppleMusicLyricsView
import com.maxrave.simpmusic.ui.screen.player.content.applemusic.AppleMusicView
import com.maxrave.simpmusic.ui.screen.player.content.applemusic.AudioOutputSheetHost
import com.maxrave.simpmusic.ui.screen.player.content.applemusic.rememberAppleMusicTypography
import com.maxrave.simpmusic.ui.screen.player.content.soundcloud.SoundCloudWaveform
import com.maxrave.simpmusic.ui.screen.player.content.soundcloud.rememberSteadyPlayhead
import com.maxrave.simpmusic.ui.theme.LocalLiquidGlassEnabled
import com.maxrave.simpmusic.ui.theme.seed
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.SoundCloudPlayerViewModel
import com.maxrave.simpmusic.viewModel.UIEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.comment_placeholder
import simpmusic.composeapp.generated.resources.crossfading
import simpmusic.composeapp.generated.resources.soundcloud_play_count_info
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Now Playing laid out like SoundCloud's player (Mobbin, iOS 2024 and Android, and the owner's own
 * iOS captures): the artwork fills a rounded card; title, artist and a "Song info" chip sit on black
 * strips in its top-left corner, one strip per line; dismiss, follow, output and lyrics run down its
 * top-right; the waveform slides past a playhead fixed at the centre, with the Apple Music style's
 * line of lyrics and the comments that name a moment above it and a comment box below it; like,
 * comments, shuffle, repeat and more run along the bottom. Every colour is a role of the
 * artwork-seeded dark scheme that [NowPlayingExpressiveTheme] builds for the M3 Expressive style,
 * except where SoundCloud puts its orange: the played part of the waveform keeps the app's own
 * primary on every track.
 *
 * Playing, the card shows nothing but the artwork and its labels; a tap anywhere on it pauses.
 * Paused, the artwork blurs and dims, the strips give way to plain text, the waveform folds into a
 * line, and previous, play and next come up across the middle. Dragging the waveform scrubs: the
 * labels and controls give way to the time in large type, as SoundCloud's Android player does.
 */
@Composable
fun NowPlayingContentSoundCloud(
    state: NowPlayingContentState,
    actions: NowPlayingContentActions,
) {
    // The waveform keeps the app's own primary on every track (owner's call), so it is read here,
    // before the artwork scheme is entered.
    val appPrimary = MaterialTheme.colorScheme.primary
    // The Apple Music style's Lyrics page, borrowed without its bottom cluster (owner's call).
    var showLyrics by rememberSaveable { mutableStateOf(false) }
    // As the Apple Music style does: every track change starts with no lyrics until they arrive, so
    // only a track that still has none after the wait sends the page back to the card.
    LaunchedEffect(state.screenData.lyricsData, showLyrics) {
        if (!showLyrics || state.screenData.lyricsData != null) return@LaunchedEffect
        delay(SOUNDCLOUD_LYRICS_GRACE_MS)
        showLyrics = false
    }
    Crossfade(targetState = showLyrics, animationSpec = tween(300), label = "soundCloudView") { lyrics ->
        if (lyrics) {
            SoundCloudLyricsPage(state = state, actions = actions, onBack = { showLyrics = false })
        } else {
            NowPlayingExpressiveTheme(state = state) {
                NowPlayingSoundCloudLayout(
                    state = state,
                    actions = actions,
                    waveformColor = appPrimary,
                    // Nothing without lyrics, as the Apple Music style's Lyrics pill is disabled then.
                    onOpenLyrics = { if (state.screenData.lyricsData != null) showLyrics = true },
                )
            }
        }
    }
}

/**
 * The Apple Music style's Lyrics page as that style draws it — frosted cover, compact header, the
 * app's lyrics renderer and its floating buttons — minus the bottom cluster. The header (or Back on
 * Android) returns to the card.
 */
@Composable
private fun SoundCloudLyricsPage(
    state: NowPlayingContentState,
    actions: NowPlayingContentActions,
    onBack: () -> Unit,
) {
    val paletteColor = state.startColor.value
    val seedColor = if (paletteColor == Color.Black) seed else paletteColor
    val localDensity = LocalDensity.current
    PlatformBackHandler(enabled = true, onBack = onBack)
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AppleMusicArtworkBackdrop(state = state, actions = actions, seedColor = seedColor)
        AppleMusicLyricsView(
            state = state,
            actions = actions,
            typography = rememberAppleMusicTypography(),
            viewState = AppleMusicView.LYRICS,
            onSelectView = {},
            activePillContainer = Color.Transparent,
            activePillContent = Color.Transparent,
            deviceVolumeController = null,
            outputName = null,
            onOpenOutput = {},
            showBottomCluster = false,
            onBack = onBack,
        )
        // The Apple Music style's grabber, which this page leaves room for; Desktop draws none on its
        // Lyrics page either.
        if (getPlatform() != Platform.Desktop) {
            Box(
                modifier =
                    Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = with(localDensity) { WindowInsets.statusBars.getTop(localDensity).toDp() })
                        .size(width = 64.dp, height = SOUNDCLOUD_GRABBER_HEIGHT)
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                        ) { actions.onDismiss() },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(width = 36.dp, height = 5.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color.White.copy(alpha = 0.35f)),
                )
            }
        }
    }
}

@Composable
private fun NowPlayingSoundCloudLayout(
    state: NowPlayingContentState,
    actions: NowPlayingContentActions,
    waveformColor: Color,
    onOpenLyrics: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val glassOn = LocalLiquidGlassEnabled.current
    val backdrop = rememberBackdrop(colors.surface)
    val viewModel: SoundCloudPlayerViewModel = koinViewModel()
    val waveform by viewModel.waveform.collectAsStateWithLifecycle()
    val trackComments by viewModel.comments.collectAsStateWithLifecycle()
    var showOutputSheet by rememberSaveable { mutableStateOf(false) }
    // The same condition the other styles show their inline video on.
    val showVideo = state.screenData.canvasData == null && state.screenData.isVideo && state.shouldShowVideo
    var showSubtitle by rememberSaveable { mutableStateOf(true) }

    // Off the media item, prefix stripped (see NowPlayingContentState.videoId).
    val videoId = state.videoId.orEmpty()
    LaunchedEffect(videoId) { viewModel.load(videoId) }
    // No scroll and no collapsing toolbar here, as in the Apple Music style.
    LaunchedEffect(Unit) { actions.onToolbarVisibilityChange(false) }

    val timeline = state.timelineState
    val durationMs = timeline.total.coerceAtLeast(0L)
    val isPlaying = state.controllerState.isPlaying
    // Paused as the listener sees it. The player also reports "not playing" while it buffers (a track
    // change, a seek), sometimes a beat before it reports loading, so only a pause that lasts shows as
    // one; a tap on the card shows its own outcome at once.
    var paused by remember { mutableStateOf(!isPlaying && !timeline.loading) }
    LaunchedEffect(isPlaying, timeline.loading) {
        if (isPlaying) {
            paused = false
        } else if (!timeline.loading) {
            delay(PAUSE_SETTLE_MS)
            paused = true
        }
    }
    val togglePlayPause = {
        paused = !paused
        actions.onUIEvent(UIEvent.PlayPause)
    }
    var scrubbing by remember { mutableStateOf(false) }
    var scrubMs by remember { mutableLongStateOf(0L) }
    // After a scrub the player takes a moment to report the new spot; holding the scrubbed value
    // until it does keeps the waveform from springing back to where the drag started.
    var pendingSeekMs by remember { mutableStateOf<Long?>(null) }
    val latestReported by rememberUpdatedState(timeline.current)
    LaunchedEffect(pendingSeekMs) {
        val target = pendingSeekMs ?: return@LaunchedEffect
        withTimeoutOrNull(SEEK_SETTLE_TIMEOUT_MS) { snapshotFlow { latestReported }.first { abs(it - target) < SEEK_SETTLED_WITHIN_MS } }
        pendingSeekMs = null
    }
    // Not running while a scrub settles either: it then follows the reports, so it starts again from
    // where the player actually landed instead of gliding there from where the drag began.
    val playhead = rememberSteadyPlayhead(rawMs = timeline.current, running = isPlaying && !scrubbing && pendingSeekMs == null)
    // Read where it is used (the waveform draws it every frame) rather than here, so a moving
    // playhead redraws the waveform without recomposing the whole card each frame.
    val position = remember { { if (scrubbing) scrubMs else pendingSeekMs ?: playhead.value } }
    // Read while drawing, so the pause and fold animations redraw without recomposing.
    val pausedProgress = animateFloatAsState(if (paused) 1f else 0f, tween(PAUSE_FADE_MS), label = "soundCloudPaused")
    val waveformFold = animateFloatAsState(if (paused && !scrubbing) 1f else 0f, tween(PAUSE_FADE_MS), label = "soundCloudFold")
    val stripAlpha = { 1f - pausedProgress.value }
    val pausedBlur =
        remember(pausedProgress) {
            Modifier.graphicsLayer {
                val radius = PAUSED_BLUR.toPx() * pausedProgress.value
                renderEffect = if (radius > 0f) BlurEffect(radius, radius, TileMode.Clamp) else null
            }
        }
    // The lower block's height, which places the lyric line for the blur and the scrim under it.
    val lowerBlockHeight = remember { mutableIntStateOf(0) }
    // Remembered, so the shell's recompositions (one per position report) do not redraw the artwork,
    // the blur over it and the glass that samples it.
    val surface = colors.surface
    val scrim = colors.scrim
    val artworkModifier =
        remember(glassOn, backdrop, pausedBlur, surface, scrim, lowerBlockHeight) {
            Modifier
                // The glass source while glass is on: the artwork as it is shown, dim and scrim
                // included. The glass buttons are its siblings, never inside it.
                .then(if (glassOn) Modifier.layerBackdrop(backdrop) else Modifier)
                .then(pausedBlur)
                // Drawn after the artwork's own content, so it covers a video surface too, which
                // takes no Compose effect: the paused dim, then from the card's bottom up to the line
                // of lyrics the scheme's scrim over the blurred cover. The scrim ramps in over the band
                // above the line, on the same curve as the blur, and keeps darkening to the bottom.
                .drawWithContent {
                    drawContent()
                    drawRect(color = surface, alpha = PAUSED_DIM * pausedProgress.value)
                    val lineY = lyricLineY(lowerBlockHeight.intValue)
                    val fadeTop = lineY - LOWER_FADE_BAND.toPx()
                    val atLine = scrim.copy(alpha = LOWER_SCRIM_AT_LINE)
                    drawRect(
                        brush = smoothScrimBrush(from = scrim.copy(alpha = 0f), to = atLine, startY = fadeTop, endY = lineY),
                        topLeft = Offset(0f, fadeTop),
                        size = Size(size.width, lineY - fadeTop),
                    )
                    if (size.height > lineY) {
                        drawRect(
                            brush =
                                smoothScrimBrush(
                                    from = atLine,
                                    to = scrim.copy(alpha = LOWER_SCRIM_AT_BOTTOM),
                                    startY = lineY,
                                    endY = size.height,
                                ),
                            topLeft = Offset(0f, lineY),
                            size = Size(size.width, size.height - lineY),
                        )
                    }
                }
        }

    val timedComments = remember(trackComments) { trackComments.comments.mapNotNull { it.toTimedComment() } }
    val shownComment by remember(timedComments) {
        derivedStateOf {
            val now = position()
            timedComments.lastOrNull { now in it.positionMs until it.positionMs + COMMENT_SHOWN_MS }
        }
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(colors.surface)
                .windowInsetsPadding(WindowInsets.statusBars),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(CARD_SHAPE)
                    .background(colors.surfaceContainer)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = togglePlayPause),
        ) {
            if (getPlatform() == Platform.Android) {
                LeftToRight {
                    HorizontalPager(
                        state = state.artworkPagerState,
                        // Record the visible pager once, not each neighboring page, for the glass.
                        modifier = artworkModifier.fillMaxSize(),
                        beyondViewportPageCount = 1,
                        userScrollEnabled = state.controllerState.repeatState !is RepeatState.One && state.artworkQueue.isNotEmpty(),
                        key = { page ->
                            val id = state.artworkQueue.getOrNull(page)?.videoId.orEmpty()
                            "soundCloudArtwork_${id}_$page"
                        },
                    ) { page ->
                        SoundCloudArtwork(
                            state = state,
                            actions = actions,
                            showVideo = showVideo,
                            showSubtitle = showSubtitle,
                            lowerBlockHeight = lowerBlockHeight,
                            modifier = Modifier.clipToBounds(),
                            page = page,
                        )
                    }
                }
            } else {
                SoundCloudArtwork(
                    state = state,
                    actions = actions,
                    showVideo = showVideo,
                    showSubtitle = showSubtitle,
                    lowerBlockHeight = lowerBlockHeight,
                    modifier = artworkModifier,
                )
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = !scrubbing,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier =
                    Modifier
                        .align(Alignment.TopStart)
                        .padding(CARD_PADDING)
                        .fillMaxWidth(LABELS_WIDTH_FRACTION),
            ) {
                Column(horizontalAlignment = Alignment.Start) {
                    SoundCloudLabel(
                        text = state.screenData.nowPlayingTitle,
                        textStyle = typo().titleMedium.copy(fontSize = 16.sp),
                        color = colors.onSurface,
                        maxLines = TITLE_MAX_LINES,
                        stripAlpha = stripAlpha,
                    )
                    SoundCloudLabel(
                        text = state.screenData.artistName,
                        textStyle = typo().bodyMedium.copy(fontSize = 14.sp),
                        color = colors.onSurfaceVariant,
                        maxLines = ARTIST_MAX_LINES,
                        stripAlpha = stripAlpha,
                        onClick = actions.onNavigateToArtist,
                    )
                    // Hidden until the track's info has arrived, then faded in. AnimatedContent, so
                    // the outgoing chip keeps its own count while it fades.
                    AnimatedContent(
                        targetState = state.screenData.songInfoData?.viewCount?.toCompactCount(),
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "soundCloudPlayCount",
                    ) { count ->
                        if (count != null) {
                            Column {
                                Spacer(modifier = Modifier.height(6.dp))
                                PlayCountChip(count = count, stripAlpha = stripAlpha, onClick = actions.onShowInfo)
                            }
                        }
                    }
                }
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = !scrubbing,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.TopEnd).padding(CARD_PADDING),
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    LiquidGlassIconButton(
                        backdrop = backdrop,
                        imageVector = state.dismissIcon,
                        modifier = Modifier.size(40.dp),
                        tint = colors.onSurface,
                        // Closing already animates the whole sheet; do not animate refraction and
                        // press-scale on this button at the same time.
                        interactive = false,
                        highlight = Highlight(width = 1.dp),
                        onClick = actions.onDismiss,
                    )
                    // The output button Classic and M3 Expressive use, opening the same sheet.
                    CornerIconButton(
                        backdrop = backdrop,
                        icon = SimpIcons.Headphones,
                        tint = if (state.castState.isRemote) colors.primary else colors.onSurface,
                        onClick = { showOutputSheet = true },
                    )
                    CornerIconButton(
                        backdrop = backdrop,
                        icon = SimpIcons.Lyrics,
                        // Dimmed while the track has no lyrics, when it does nothing.
                        tint = if (state.screenData.lyricsData != null) colors.onSurface else colors.onSurface.copy(alpha = 0.38f),
                        onClick = onOpenLyrics,
                    )
                    // The other styles' add-to-YouTube-Liked (signed in only, a tick once liked) and
                    // add-to-playlist, with the same actions.
                    if (state.isUserLoggedIn) {
                        CornerIconButton(
                            backdrop = backdrop,
                            icon = if (state.likeStatus) SimpIcons.CheckCircle else SimpIcons.AddCircleOutline,
                            tint = colors.onSurface,
                            onClick = actions.onAddToYouTubeLiked,
                        )
                    }
                    CornerIconButton(
                        backdrop = backdrop,
                        icon = SimpIcons.PlaylistAdd,
                        tint = colors.onSurface,
                        onClick = actions.onShowAddToPlaylist,
                    )
                    // A music video gets what the other styles put over theirs: fullscreen, and the
                    // lyrics as subtitles on the video (on by default, as there).
                    if (showVideo) {
                        CornerIconButton(
                            backdrop = backdrop,
                            icon = SimpIcons.Fullscreen,
                            tint = colors.onSurface,
                            onClick = actions.onEnterFullscreenVideo,
                        )
                        if (state.screenData.lyricsData != null) {
                            CornerIconButton(
                                backdrop = backdrop,
                                icon = if (showSubtitle) SimpIcons.SubtitlesOff else SimpIcons.Subtitles,
                                tint = colors.onSurface,
                                onClick = { showSubtitle = !showSubtitle },
                            )
                        }
                    }
                }
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = paused && !scrubbing,
                enter = fadeIn(tween(PAUSE_FADE_MS)),
                exit = fadeOut(tween(PAUSE_FADE_MS)),
                modifier =
                    Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth()
                        .padding(horizontal = CARD_PADDING),
            ) {
                // Left to right in every language, as every player style's transport is.
                LeftToRight {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // A video also gets the other styles' 5-second steps, on the outside.
                        if (showVideo) {
                            RoundButton(backdrop = backdrop, icon = SimpIcons.Replay5, size = 44.dp, iconSize = 26.dp) {
                                actions.onUIEvent(UIEvent.Backward)
                            }
                        }
                        RoundButton(backdrop = backdrop, icon = SimpIcons.SkipPrevious, size = 52.dp, iconSize = 30.dp) {
                            actions.onUIEvent(UIEvent.Previous)
                        }
                        RoundButton(backdrop = backdrop, icon = SimpIcons.PlayArrow, size = 76.dp, iconSize = 44.dp, onClick = togglePlayPause)
                        RoundButton(
                            backdrop = backdrop,
                            icon = SimpIcons.SkipNext,
                            size = 52.dp,
                            iconSize = 30.dp,
                            enabled = state.controllerState.isNextAvailable,
                        ) {
                            actions.onUIEvent(UIEvent.Next)
                        }
                        if (showVideo) {
                            RoundButton(backdrop = backdrop, icon = SimpIcons.Forward5, size = 44.dp, iconSize = 26.dp) {
                                actions.onUIEvent(UIEvent.Forward)
                            }
                        }
                    }
                }
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = scrubbing,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.Center),
            ) {
                Text(
                    text = formatDuration(scrubMs) + "   |   " + formatDuration(durationMs),
                    style = typo().titleLarge.copy(fontSize = 24.sp, fontWeight = FontWeight.Normal),
                    color = colors.onSurface,
                )
            }

            Column(
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .onSizeChanged { lowerBlockHeight.intValue = it.height }
                        .padding(bottom = CARD_PADDING),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Height reserved whether or not a comment is up, so the waveform never moves.
                Box(modifier = Modifier.height(COMMENT_ROW_HEIGHT), contentAlignment = Alignment.Center) {
                    AnimatedContent(
                        targetState = shownComment,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "soundCloudComment",
                    ) { comment ->
                        if (comment != null) TimedCommentBubble(comment)
                    }
                }
                Spacer(modifier = Modifier.height(LYRIC_STRIP_SPACING))
                // The Apple Music style's line of lyrics, lit as it is sung; a tap opens the lyrics page.
                AppleMusicLyricStrip(
                    state = state,
                    typography = rememberAppleMusicTypography(),
                    onClick = onOpenLyrics,
                )
                Spacer(modifier = Modifier.height(6.dp))
                if (timeline.isLive) {
                    SoundCloudLabel(
                        text = timeline.lengthLabel(),
                        textStyle = typo().labelMedium,
                        color = colors.onSurface,
                        maxLines = 1,
                        stripAlpha = { 1f },
                    )
                } else {
                    // Time runs left to right in every language: the waveform's drag, its playhead and its label.
                    LeftToRight {
                        SoundCloudWaveform(
                            mediaId = videoId,
                            bars = waveform?.takeIf { it.videoId == videoId }?.bars?.takeIf { it.isNotEmpty() },
                            position = position,
                            durationMs = durationMs,
                            playedColor = waveformColor,
                            fold = { waveformFold.value },
                            onScrubStart = {
                                scrubMs = position()
                                scrubbing = true
                            },
                            onScrub = { scrubMs = it },
                            onScrubEnd = { target ->
                                scrubbing = false
                                if (durationMs > 0L) {
                                    pendingSeekMs = target
                                    actions.onSliderChange(target * 100f / durationMs)
                                    actions.onSliderChangeFinished()
                                }
                            },
                        )
                    }
                }
                SoundCloudPlaybackStatus(state = state)
                Spacer(modifier = Modifier.height(12.dp))
                CommentBox(
                    backdrop = backdrop,
                    position = position,
                    onWriteComment = actions.onWriteComment,
                    modifier = Modifier.padding(horizontal = CARD_PADDING),
                )
            }
        }

        // Left to right in every language, as the action rows of the other styles are.
        LeftToRight {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .height(ACTION_ROW_HEIGHT)
                        .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Like and comments, shuffle and repeat (how it plays), the queue, then more. No counts
                // (owner's call): the like is the app's own, which has none.
                RippleIconButton(
                    imageVector = if (state.controllerState.isLiked) SimpIcons.Favorite else SimpIcons.FavoriteBorder,
                    tint = if (state.controllerState.isLiked) colors.primary else colors.onSurface,
                    onClick = { actions.onUIEvent(UIEvent.ToggleLike) },
                )
                RippleIconButton(imageVector = SimpIcons.CommentOutline, tint = colors.onSurface, onClick = actions.onShowComments)
                RippleIconButton(
                    imageVector = SimpIcons.Shuffle,
                    tint = if (state.controllerState.isShuffle) colors.primary else colors.onSurface,
                    onClick = { actions.onUIEvent(UIEvent.Shuffle) },
                )
                val repeatState = state.controllerState.repeatState
                RippleIconButton(
                    imageVector = if (repeatState is RepeatState.One) SimpIcons.RepeatOne else SimpIcons.Repeat,
                    tint = if (repeatState is RepeatState.None) colors.onSurface else colors.primary,
                    onClick = { actions.onUIEvent(UIEvent.Repeat) },
                )
                RippleIconButton(imageVector = SimpIcons.QueueMusic, tint = colors.onSurface, onClick = actions.onShowQueue)
                RippleIconButton(imageVector = SimpIcons.MoreVert, tint = colors.onSurface, onClick = actions.onShowMoreSheet)
            }
        }
    }

    if (showOutputSheet) {
        AudioOutputSheetHost(
            castState = state.castState,
            onDismiss = { showOutputSheet = false },
        )
    }
}

/** One stable slot by the waveform: crossfade status takes the stream-quality line's place. */
@Composable
private fun SoundCloudPlaybackStatus(state: NowPlayingContentState) = LeftToRight {
    val colors = MaterialTheme.colorScheme
    val transition = rememberInfiniteTransition(label = "soundCloudCrossfadeSweep")
    val sweep by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(3200, easing = LinearEasing), repeatMode = RepeatMode.Restart),
        label = "soundCloudSweepHead",
    )
    Crossfade(
        targetState = state.timelineState.isCrossfading to state.audioQualityLabel,
        animationSpec = tween(300),
        label = "soundCloudPlaybackStatus",
        // Reserve the line even before format metadata arrives, so the waveform never jumps.
        modifier = Modifier.fillMaxWidth().height(24.dp),
    ) { (crossfading, quality) ->
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (crossfading) {
                val span = 140f
                val head = sweep * (span * 3f) - span
                Text(
                    text = stringResource(Res.string.crossfading),
                    style =
                        typo().labelSmall.copy(
                            brush =
                                Brush.horizontalGradient(
                                    0f to colors.onSurface.copy(alpha = 0.45f),
                                    0.5f to Color.White,
                                    1f to colors.onSurface.copy(alpha = 0.45f),
                                    startX = head,
                                    endX = head + span,
                                    tileMode = TileMode.Clamp,
                                ),
                        ),
                    maxLines = 1,
                )
            } else if (quality != null) {
                Row(modifier = Modifier.padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = SimpIcons.Headphones,
                        contentDescription = null,
                        tint = colors.onSurfaceVariant,
                        modifier = Modifier.size(13.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = quality, style = typo().labelSmall.copy(fontSize = 13.sp), color = colors.onSurfaceVariant, maxLines = 1)
                }
            }
        }
    }
}

/**
 * The artwork filling the card, or what plays in its place: a canvas (Spotify's, or Apple Music's
 * animated cover) cropped to the card, or the music video at its own shape on black. A video surface
 * is composited under the window, so Compose cannot fade IT; the cover is drawn over the canvas
 * instead and fades out once the canvas has its first frame.
 *
 * The cover stays composed under a video too, as M3 Expressive keeps it: its onSuccess is the only
 * thing that hands the artwork to the shell, and without it the scheme this style is drawn in sits on
 * the app's seed colour for the whole video.
 */
@Composable
private fun SoundCloudArtwork(
    state: NowPlayingContentState,
    actions: NowPlayingContentActions,
    showVideo: Boolean,
    showSubtitle: Boolean,
    lowerBlockHeight: IntState,
    modifier: Modifier = Modifier,
    page: Int? = null,
) {
    // Only the playing page may attach video/canvas or feed the shell's artwork palette.
    // Neighboring pages preview their own queue covers; Desktop keeps its existing single page.
    val isCurrentPage = page == null || state.artworkQueue.isEmpty() || page == state.currentOrderIndex
    val canvas = state.screenData.canvasData.takeIf { isCurrentPage }
    val showCurrentVideo = showVideo && isCurrentPage
    val thumbnailUrl =
        if (isCurrentPage) {
            state.screenData.thumbnailURL
        } else {
            page?.let { state.artworkQueue.getOrNull(it) }?.thumbnails?.maxByOrNull { it.width * it.height }?.url
        }
    Box(modifier = modifier.fillMaxSize()) {
        var canvasReady by remember(canvas?.url) { mutableStateOf(false) }
        if (canvas != null) {
            MediaPlayerView(
                url = canvas.url,
                modifier = Modifier.fillMaxSize(),
                cropToBounds = true,
                onFirstFrame = { canvasReady = true },
            )
        }
        val coverAlpha by animateFloatAsState(
            targetValue = if (showCurrentVideo || (canvas != null && canvasReady)) 0f else 1f,
            animationSpec = tween(COVER_FADE_MS),
            label = "soundCloudCover",
        )
        var artworkUrl by remember(thumbnailUrl) { mutableStateOf(thumbnailUrl) }
        val request =
            ImageRequest
                .Builder(LocalPlatformContext.current)
                .data(artworkUrl)
                .diskCachePolicy(CachePolicy.ENABLED)
                .diskCacheKey(artworkUrl + "BIGGER")
                .crossfade(550)
                .build()
        // Re-enter when a prefetched cover becomes current, even if its URL is unchanged, so the
        // memory-cache success still hands the new track's bitmap to the shell.
        key(isCurrentPage) {
            AsyncImage(
                model = request,
                contentDescription = null,
                onSuccess = { if (isCurrentPage) actions.onArtworkBitmap(it.result.image.toImageBitmap()) },
                onError = {
                    val fallback = artworkUrl?.replace("maxresdefault", "hqdefault")
                    if (fallback != null && fallback != artworkUrl) artworkUrl = fallback
                },
                contentScale = ContentScale.Crop,
                placeholder = rememberHolderPainter(),
                error = rememberHolderPainter(),
                modifier = Modifier.fillMaxSize().alpha(coverAlpha),
            )
        }
        // From the bottom up to the line of lyrics the cover melts into a blurred copy of itself, faded
        // in by a DstIn mask on the smoothstep curve the scrim over it uses. At the top of the ramp the
        // copy is fully transparent, so it leaves no seam (ArtistScreen's header does the same). It fades
        // with the cover, so a canvas or a video that takes over gets the scrim alone. Below Android 12
        // blur is a no-op and the copy matches the cover.
        val scrim = MaterialTheme.colorScheme.scrim
        val lowerBlur =
            remember(lowerBlockHeight, scrim) {
                Modifier
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                    .drawWithContent {
                        drawContent()
                        val lineY = lyricLineY(lowerBlockHeight.intValue)
                        drawRect(
                            brush = smoothScrimBrush(from = scrim.copy(alpha = 0f), to = scrim, startY = lineY - LOWER_FADE_BAND.toPx(), endY = lineY),
                            blendMode = BlendMode.DstIn,
                        )
                    }.blur(LOWER_BLUR)
            }
        AsyncImage(
            model = request,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().alpha(coverAlpha).then(lowerBlur),
        )
        if (showCurrentVideo) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainerLowest)) {
                // A frame of the video's own shape, as M3 Expressive and the Apple Music style give
                // theirs. Without it the view grows to the whole card (its subtitle layer fills its
                // parent), so the subtitles sat on the card's bottom edge under the comment box and
                // the scrim, and Desktop letterboxed the picture across the whole card. The frame fits
                // above the lower block, and stays centred in the card unless that would run it under.
                val ratio = state.videoAspectRatio.takeIf { it > 0f } ?: (16f / 9)
                val lowerBlock = with(LocalDensity.current) { lowerBlockHeight.intValue.toDp() }
                val zone = (maxHeight - lowerBlock).coerceAtLeast(0.dp)
                val frameHeight = minOf(maxWidth / ratio, zone)
                val frameTop = minOf((maxHeight - frameHeight) / 2, zone - frameHeight).coerceAtLeast(0.dp)
                Box(
                    modifier =
                        Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = frameTop)
                            .size(width = frameHeight * ratio, height = frameHeight),
                ) {
                    MediaPlayerViewWithSubtitle(
                        playerName = MAIN_PLAYER,
                        // fillMaxWidth, never fillMaxSize: a free height lets the video keep its shape.
                        modifier = Modifier.fillMaxWidth().align(Alignment.Center),
                        shouldShowSubtitle = showSubtitle,
                        shouldScaleDownSubtitle = true,
                        isInPipMode = state.isInPipMode,
                        timelineState = state.timelineState,
                        lyricsData = state.screenData.lyricsData?.lyrics,
                        translatedLyricsData = state.screenData.lyricsData?.translatedLyrics?.first,
                        mainTextStyle = typo().bodyLarge,
                        translatedTextStyle = typo().bodyMedium,
                    )
                }
            }
        }
    }
}

/**
 * SoundCloud's label: text on black strips, one per line and only as wide as that line, so a title
 * that wraps reads as a stack of strips. [stripAlpha] is read while drawing; paused, the strips give
 * way to plain text over the dimmed artwork.
 */
@Composable
private fun SoundCloudLabel(
    text: String,
    textStyle: TextStyle,
    color: Color,
    maxLines: Int,
    stripAlpha: () -> Float,
    onClick: (() -> Unit)? = null,
) {
    val strip = MaterialTheme.colorScheme.surfaceContainerLowest
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    Text(
        text = text,
        // Room above and below every line, the first and last included, for the strip around it.
        style =
            textStyle.copy(
                lineHeight = LABEL_LINE_HEIGHT,
                lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None),
            ),
        color = color,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        onTextLayout = { layout = it },
        modifier =
            Modifier
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .drawBehind {
                    val lines = layout ?: return@drawBehind
                    val alpha = stripAlpha()
                    if (alpha <= 0f) return@drawBehind
                    val padX = LABEL_PADDING_X.toPx()
                    val gap = LABEL_LINE_GAP.toPx()
                    // Text coordinates sit padX in from this box, so each strip reaches padX past both
                    // ends of its line.
                    for (line in 0 until lines.lineCount) {
                        val left = lines.getLineLeft(line)
                        val top = lines.getLineTop(line) + gap / 2
                        drawRect(
                            color = strip,
                            alpha = alpha,
                            topLeft = Offset(left, top),
                            size = Size(lines.getLineRight(line) - left + 2 * padX, lines.getLineBottom(line) - gap / 2 - top),
                        )
                    }
                }.padding(horizontal = LABEL_PADDING_X),
    )
}

/**
 * The track's YouTube view count under the labels, where SoundCloud shows its play count, on a strip
 * of its own that fades with theirs. A tap opens the track's info, as the chip did before.
 */
@Composable
private fun PlayCountChip(
    count: String,
    stripAlpha: () -> Float,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier =
            Modifier
                .drawBehind { drawRect(color = colors.surfaceContainerLowest, alpha = stripAlpha()) }
                .clickable(onClick = onClick)
                .padding(horizontal = LABEL_PADDING_X, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(imageVector = SimpIcons.PlayArrow, contentDescription = null, tint = colors.onSurface, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = stringResource(Res.string.soundcloud_play_count_info, count),
            style = typo().bodyMedium.copy(fontSize = 12.sp),
            color = colors.onSurface,
            maxLines = 1,
        )
    }
}

/**
 * Shared glass surface with its built-in fallback, retaining each control's icon size and state.
 */
@Composable
private fun RoundButton(
    backdrop: PlatformBackdrop,
    icon: ImageVector,
    size: Dp,
    iconSize: Dp,
    enabled: Boolean = true,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit,
) {
    // A 1dp highlight, as on the other small round glass buttons: a circle this size catches only a
    // short arc of the default directional rim and reads as rimless.
    LiquidGlassContainer(backdrop = backdrop, modifier = Modifier.size(size), highlight = Highlight(width = 1.dp)) {
        IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxSize()) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) tint else tint.copy(alpha = 0.38f),
                modifier = Modifier.size(iconSize),
            )
        }
    }
}

/**
 * Corner controls share the dismiss button's glass circle and solid fallback.
 */
@Composable
private fun CornerIconButton(
    backdrop: PlatformBackdrop,
    icon: ImageVector,
    tint: Color,
    onClick: () -> Unit,
) {
    RoundButton(backdrop = backdrop, icon = icon, size = 40.dp, iconSize = 24.dp, tint = tint, onClick = onClick)
}

/**
 * The current time leads the placeholder; opening the draft captures that time, including emoji.
 */
@Composable
private fun CommentBox(
    backdrop: PlatformBackdrop,
    position: () -> Long,
    onWriteComment: (String) -> Unit,
    modifier: Modifier = Modifier,
) = LeftToRight {
    val colors = MaterialTheme.colorScheme
    val latestPosition by rememberUpdatedState(position)
    // Refresh just this label once a second, not the whole card on every playhead frame.
    val elapsedSecond by remember { derivedStateOf { latestPosition().coerceAtLeast(0L) / 1000L } }
    val timestamp = soundCloudCommentTimestamp(elapsedSecond * 1000L)
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .height(COMMENT_BOX_HEIGHT)
                .then(
                    // Glass while the app's glass is on; static, since a press would bulge a pill this
                    // wide (the tap still ripples).
                    if (LocalLiquidGlassEnabled.current) {
                        Modifier.liquidGlass(backdrop = backdrop, shape = CircleShape, interactive = false)
                    } else {
                        Modifier.clip(CircleShape).background(colors.surfaceContainerLowest.copy(alpha = 0.6f))
                    },
                ).clip(CircleShape)
                .clickable { onWriteComment("${soundCloudCommentTimestamp(latestPosition())} ") }
                .padding(start = 16.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = timestamp, style = typo().bodyMedium, color = colors.onSurfaceVariant, maxLines = 1)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = stringResource(Res.string.comment_placeholder),
            style = typo().bodyMedium,
            color = colors.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        QUICK_REACTIONS.forEach { emoji ->
            Text(
                text = emoji,
                style = typo().bodyMedium.copy(fontSize = 18.sp),
                modifier =
                    Modifier
                        .clip(CircleShape)
                        .clickable { onWriteComment("${soundCloudCommentTimestamp(latestPosition())} $emoji") }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    }
}

/** YouTube-compatible timestamps, including hour-long tracks; captured again at the actual tap. */
internal fun soundCloudCommentTimestamp(positionMs: Long): String {
    val totalSeconds = positionMs.coerceAtLeast(0L) / 1000L
    val seconds = (totalSeconds % 60L).toString().padStart(2, '0')
    val minutes = totalSeconds / 60L
    return if (minutes < 60L) {
        "${minutes.toString().padStart(2, '0')}:$seconds"
    } else {
        "${minutes / 60L}:${(minutes % 60L).toString().padStart(2, '0')}:$seconds"
    }
}

@Composable
private fun TimedCommentBubble(comment: TimedComment) {
    Row(
        modifier = Modifier.padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = comment.authorAvatar,
            contentDescription = null,
            placeholder = rememberHolderPainter(),
            error = rememberHolderPainter(),
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(28.dp).clip(CircleShape),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = comment.text,
            style = typo().bodyMedium.copy(fontSize = 13.sp),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier =
                Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.6f))
                    .padding(horizontal = 12.dp, vertical = 5.dp),
        )
    }
}

/**
 * The top of the lyric line in the card's coordinates. The lower block (timed comments, lyric line,
 * waveform, comment box) sits on the card's bottom edge, and the line is the second thing in it.
 */
private fun DrawScope.lyricLineY(lowerBlockHeight: Int): Float = size.height - lowerBlockHeight + (COMMENT_ROW_HEIGHT + LYRIC_STRIP_SPACING).toPx()

/** A comment placed at the first moment its text names. */
private data class TimedComment(
    val id: String,
    val positionMs: Long,
    val authorAvatar: String?,
    val text: String,
)

/**
 * The comment at the first "m:ss" or "h:mm:ss" its text names — the same rule descriptions use for
 * their seek links — or null when it names none. A moment past the end of the track simply never
 * comes up.
 */
private fun YouTubeComment.toTimedComment(): TimedComment? {
    val match = TIMESTAMP_REGEX.find(text) ?: return null
    val (first, second, third) = match.destructured
    val a = first.toLongOrNull() ?: return null
    val b = second.toLongOrNull()?.takeIf { it < 60 } ?: return null
    val seconds =
        if (third.isEmpty()) {
            a * 60 + b
        } else {
            val c = third.toLongOrNull()?.takeIf { it < 60 } ?: return null
            a * 3600 + b * 60 + c
        }
    return TimedComment(id = id, positionMs = seconds * 1000, authorAvatar = authorAvatar, text = text)
}

/** 48000 → "48K", 1234 → "1.2K", 1500000 → "1.5M". */
private fun Int.toCompactCount(): String =
    when {
        this >= 1_000_000 -> compact(this / 1_000_000.0, "M")
        this >= 1_000 -> compact(this / 1_000.0, "K")
        else -> toString()
    }

private fun compact(
    value: Double,
    suffix: String,
): String {
    val tenths = (value * 10).roundToInt()
    val text = if (value >= 10 || tenths % 10 == 0) (tenths / 10).toString() else "${tenths / 10}.${tenths % 10}"
    return text + suffix
}

// Read off SoundCloud's own player (Mobbin, iOS 2024, and the owner's iOS captures), converted from
// its 390pt-wide frame.
private val CARD_SHAPE = RoundedCornerShape(20.dp)
private val CARD_PADDING = 16.dp
private const val LABELS_WIDTH_FRACTION = 0.78f
private const val TITLE_MAX_LINES = 3
private const val ARTIST_MAX_LINES = 2
private val LABEL_LINE_HEIGHT = 1.75.em
private val LABEL_LINE_GAP = 3.dp
private val LABEL_PADDING_X = 6.dp
private val COMMENT_ROW_HEIGHT = 32.dp
private val LYRIC_STRIP_SPACING = 4.dp

// The lower card, from the bottom up to the line of lyrics, sits on a blurred copy of the cover under
// the scheme's scrim (the owner's call, so the white line reads on any cover). Both ramp in over this
// band above the line; the scrim then keeps darkening down to the bottom.
private val LOWER_FADE_BAND = 80.dp
private val LOWER_BLUR = 32.dp
private const val LOWER_SCRIM_AT_LINE = 0.6f
private const val LOWER_SCRIM_AT_BOTTOM = 0.85f
private val COMMENT_BOX_HEIGHT = 40.dp
private val QUICK_REACTIONS = listOf("🔥", "👏", "🥹")
private val ACTION_ROW_HEIGHT = 64.dp
private const val COVER_FADE_MS = 400

// Paused, the artwork blurs (Android 12 and later, and Desktop) and sits under the page colour.
private val PAUSED_BLUR = 20.dp
private const val PAUSED_DIM = 0.45f
private const val PAUSE_FADE_MS = 250

// How long "not playing, not loading" must last before it counts as a pause.
private const val PAUSE_SETTLE_MS = 250L

// A comment stays up this long after the moment it names.
private const val COMMENT_SHOWN_MS = 4_500L
private const val SEEK_SETTLE_TIMEOUT_MS = 2_000L
private const val SEEK_SETTLED_WITHIN_MS = 1_000L

// How long the Lyrics page waits for a track's lyrics before going back to the card (the Apple Music
// style's wait), and the height of the grabber it draws as that style does.
private const val SOUNDCLOUD_LYRICS_GRACE_MS = 2_500L
private val SOUNDCLOUD_GRABBER_HEIGHT = 28.dp
