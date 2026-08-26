@file:OptIn(ExperimentalMaterial3Api::class)

package com.maxrave.simpmusic.ui.screen.player.content

import androidx.compose.animation.Animatable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.MarqueeAnimationMode
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.kmpalette.rememberPaletteState
import com.maxrave.common.Config.MAIN_PLAYER
import com.maxrave.domain.mediaservice.handler.RepeatState
import com.maxrave.simpmusic.Platform
import com.maxrave.simpmusic.expect.ui.MediaPlayerView
import com.maxrave.simpmusic.expect.ui.MediaPlayerViewWithSubtitle
import com.maxrave.simpmusic.expect.ui.PlatformCastButton
import com.maxrave.simpmusic.expect.ui.toImageBitmap
import com.maxrave.simpmusic.extension.formatDuration
import com.maxrave.simpmusic.extension.getColorFromPalette
import com.maxrave.simpmusic.extension.getScreenSizeInfo
import com.maxrave.simpmusic.extension.isElementVisible
import com.maxrave.simpmusic.extension.parseTimestampToMilliseconds
import com.maxrave.simpmusic.extension.smoothScrimBrush
import com.maxrave.simpmusic.getPlatform
import com.maxrave.simpmusic.ui.component.AIBadge
import com.maxrave.simpmusic.ui.component.DescriptionView
import com.maxrave.simpmusic.ui.component.ExplicitBadge
import com.maxrave.simpmusic.ui.component.HeartCheckBox
import com.maxrave.simpmusic.ui.component.LyricsView
import com.maxrave.simpmusic.ui.component.PlayPauseButton
import com.maxrave.simpmusic.ui.component.PlayerControlLayout
import com.maxrave.simpmusic.ui.component.lyrics.ShareLyricsSheet
import com.maxrave.simpmusic.ui.component.lyrics.toShareLyricsLines
import com.maxrave.simpmusic.ui.component.rememberHolderPainter
import com.maxrave.simpmusic.ui.icon.AddCircleOutline
import com.maxrave.simpmusic.ui.icon.CheckCircle
import com.maxrave.simpmusic.ui.icon.Forward5
import com.maxrave.simpmusic.ui.icon.Fullscreen
import com.maxrave.simpmusic.ui.icon.Info
import com.maxrave.simpmusic.ui.icon.MoreVert
import com.maxrave.simpmusic.ui.icon.PlaylistAdd
import com.maxrave.simpmusic.ui.icon.QueueMusic
import com.maxrave.simpmusic.ui.icon.Replay5
import com.maxrave.simpmusic.ui.icon.Share
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.icon.Subtitles
import com.maxrave.simpmusic.ui.icon.SubtitlesOff
import com.maxrave.simpmusic.ui.icon.ThumbsUpDown
import com.maxrave.simpmusic.ui.theme.blackMoreOverlay
import com.maxrave.simpmusic.ui.theme.overlay
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.LyricsProvider
import com.maxrave.simpmusic.viewModel.UIEvent
import kotlin.math.roundToLong
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.artists
import simpmusic.composeapp.generated.resources.crossfading
import simpmusic.composeapp.generated.resources.description
import simpmusic.composeapp.generated.resources.like_and_dislike
import simpmusic.composeapp.generated.resources.line_synced
import simpmusic.composeapp.generated.resources.lyrics
import simpmusic.composeapp.generated.resources.lyrics_provider_betterlyrics
import simpmusic.composeapp.generated.resources.lyrics_provider_lrc
import simpmusic.composeapp.generated.resources.lyrics_provider_simpmusic
import simpmusic.composeapp.generated.resources.lyrics_provider_youtube
import simpmusic.composeapp.generated.resources.now_playing_upper
import simpmusic.composeapp.generated.resources.offline_mode
import simpmusic.composeapp.generated.resources.playing_on_device
import simpmusic.composeapp.generated.resources.published_at
import simpmusic.composeapp.generated.resources.rate_lyrics
import simpmusic.composeapp.generated.resources.rich_synced
import simpmusic.composeapp.generated.resources.share_lyrics
import simpmusic.composeapp.generated.resources.show
import simpmusic.composeapp.generated.resources.spotify_lyrics_provider
import simpmusic.composeapp.generated.resources.unsynced
import simpmusic.composeapp.generated.resources.view_count

// stripRichSyncTimestamps() lives in NowPlayingContentState.kt (same package) so both
// content styles share one copy.

/**
 * The original Spotify-inspired Now Playing UI, moved verbatim out of
 * [com.maxrave.simpmusic.ui.screen.player.NowPlayingScreenContent]. Reads only
 * [NowPlayingContentState] and calls back only through [NowPlayingContentActions].
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NowPlayingContentSpotify(
    state: NowPlayingContentState,
    actions: NowPlayingContentActions,
) {
    val screenInfo = getScreenSizeInfo()

    val localDensity = LocalDensity.current
    val uriHandler = LocalUriHandler.current

    val isRepeatOne = state.controllerState.repeatState is RepeatState.One

    var showShareLyricsSheet by rememberSaveable { mutableStateOf(false) }

    // Height
    var topAppBarHeightDp by rememberSaveable {
        mutableIntStateOf(0)
    }
    var middleLayoutHeightDp by rememberSaveable {
        mutableIntStateOf(0)
    }
    var infoLayoutHeightDp by rememberSaveable {
        mutableIntStateOf(0)
    }
    var middleLayoutPaddingDp by rememberSaveable {
        mutableIntStateOf(0)
    }
    val minimumPaddingDp by rememberSaveable {
        mutableIntStateOf(
            30,
        )
    }
    LaunchedEffect(
        topAppBarHeightDp,
        screenInfo,
        infoLayoutHeightDp,
        minimumPaddingDp,
    ) {
        if (topAppBarHeightDp > 0 && middleLayoutHeightDp > 0 && infoLayoutHeightDp > 0 && screenInfo.hDP > 0) {
            val result = (screenInfo.hDP - topAppBarHeightDp - middleLayoutHeightDp - infoLayoutHeightDp - minimumPaddingDp) / 2
            middleLayoutPaddingDp =
                if (result > minimumPaddingDp) {
                    result
                } else {
                    minimumPaddingDp
                }
        }
    }

    // Fullscreen overlay
    var showHideFullscreenOverlay by rememberSaveable {
        mutableStateOf(false)
    }

    LaunchedEffect(key1 = showHideFullscreenOverlay) {
        if (showHideFullscreenOverlay) {
            delay(3000)
            showHideFullscreenOverlay = false
        }
    }

    Box {
        Column(
            Modifier
                .verticalScroll(
                    state.mainScrollState,
                    enabled = state.isExpanded,
                )
                // Horizontal swipe is handled by the unified ArtworkPager below.
                // Spacers in this Column have no pointer input and don't block hits, so
                // drags fall through to the Pager.
                .then(
                    if (state.showHideMiddleLayout) {
                        Modifier
                            // The backdrop fills the whole scrollable content, then the gradient
                            // is drawn over just the first screen height. Using background() for
                            // the gradient instead would stretch it across the entire content,
                            // which is what made it run on forever while scrolling.
                            .background(PlayerBackdropColor)
                            .drawBehind {
                                val gradientHeight = screenInfo.hPX.toFloat()
                                val area = Size(size.width, gradientHeight)
                                // Palette gradient, keeping its diagonal angle.
                                drawRect(
                                    brush =
                                        Brush.linearGradient(
                                            colors =
                                                listOf(
                                                    state.startColor.value,
                                                    state.endColor.value,
                                                ),
                                            start = state.gradientOffset.start,
                                            end = state.gradientOffset.end,
                                        ),
                                    size = area,
                                )
                                // Vertical fade to the backdrop colour, fully opaque from 90%
                                // down, so the bottom edge meets the area underneath seamlessly
                                // across the whole width. Adding the same colour as a stop to
                                // the diagonal gradient above could not do that — it would only
                                // arrive in one corner and leave a visible diagonal seam.
                                drawRect(
                                    brush =
                                        smoothScrimBrush(
                                            from = PlayerBackdropColor.copy(alpha = 0f),
                                            to = PlayerBackdropColor,
                                            startY = 0f,
                                            // Reaches full opacity at 95% and is held there by Clamp,
                                            // same as the old `0.95f to PlayerBackdropColor` stop.
                                            endY = gradientHeight * 0.95f,
                                        ),
                                    size = area,
                                )
                            }
                    } else {
                        Modifier.background(Color.Black)
                    },
                ),
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                // === Unified ArtworkPager (Spotify-style swipe) ===
                // ONE HorizontalPager wraps both the fullscreen canvas backdrop AND the
                // centered square thumbnail. Both layers slide together as a single page
                // so when the user swipes during canvas mode, they see the next track's
                // thumbnail enter and the canvas exit in lockstep.
                HorizontalPager(
                    state = state.artworkPagerState,
                    modifier =
                        Modifier
                            .height(screenInfo.hDP.dp)
                            .fillMaxWidth(),
                    beyondViewportPageCount = 1,
                    userScrollEnabled = !isRepeatOne && state.artworkQueue.isNotEmpty(),
                    key = { idx ->
                        val vid = state.artworkQueue.getOrNull(idx)?.videoId.orEmpty()
                        "artwork_${vid}_$idx"
                    },
                ) { page ->
                    val pageTrack = state.artworkQueue.getOrNull(page)
                    val isCurrentArtworkPage = page == state.currentOrderIndex
                    val pageHasCanvas = isCurrentArtworkPage && state.screenData.canvasData != null

                    // Per-page palette state for the gradient backdrop.
                    // The bitmap is fed in by Layer 2's adjacent-thumbnail AsyncImage
                    // (onSuccess), so we use the SAME bitmap that's painted on screen —
                    // matches the outer Column's palette extraction characteristics.
                    val pagePaletteState = rememberPaletteState()
                    val pageStartColor =
                        remember(pageTrack?.videoId) {
                            Animatable(Color.Black)
                        }
                    LaunchedEffect(pagePaletteState, pageTrack?.videoId) {
                        snapshotFlow { pagePaletteState.palette }
                            .distinctUntilChanged()
                            .collectLatest { palette ->
                                pageStartColor.animateTo(
                                    palette.getColorFromPalette(),
                                )
                            }
                    }

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier =
                            Modifier
                                .fillMaxSize()
                                // Prevent the canvas video (9:16 aspect, can be wider than the
                                // page) and any other content from bleeding into adjacent pages.
                                .clipToBounds()
                                // Tap toggles controls only when the canvas is covering this page;
                                // otherwise no-op (matches the legacy behaviour where the touch
                                // overlay only appeared in canvas mode).
                                .clickable(
                                    enabled = pageHasCanvas,
                                    onClick = {
                                        if (state.mainScrollState.value == 0) {
                                            actions.onToggleControls()
                                        }
                                    },
                                    indication = null,
                                    interactionSource =
                                        remember {
                                            MutableInteractionSource()
                                        },
                                ),
                    ) {
                        // ── Layer 0: per-page backdrop (adjacent pages only) ──
                        // Palette gradient (startColor → endColor) so the adjacent page never
                        // falls back to a flat dark void during a swipe.
                        // The CURRENT page deliberately skips this layer so the existing
                        // gradient / canvas on the Column stays visible.
                        if (!isCurrentArtworkPage && pageTrack != null) {
                            // Palette is fed by Layer 2's adjacent-thumbnail AsyncImage
                            // (see below) so the gradient color stays consistent with
                            // the bitmap actually painted for that page.
                            Box(
                                modifier =
                                    Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.linearGradient(
                                                colors =
                                                    listOf(
                                                        pageStartColor.value,
                                                        Color.Black,
                                                    ),
                                                start = state.gradientOffset.start,
                                                end = state.gradientOffset.end,
                                            ),
                                        ),
                            )
                        }

                        // ── Layer 1: fullscreen canvas backdrop (current track + canvas data) ──
                        if (pageHasCanvas) {
                            Crossfade(targetState = state.screenData.canvasData?.isVideo) { isVideo ->
                                if (isVideo == true) {
                                    state.screenData.canvasData?.url?.let { url ->
                                        MediaPlayerView(
                                            url = url,
                                            modifier =
                                                Modifier
                                                    .fillMaxHeight()
                                                    .then(
                                                        if (getPlatform() == Platform.Desktop) {
                                                            Modifier
                                                        } else {
                                                            Modifier
                                                                .wrapContentWidth(unbounded = true, align = Alignment.CenterHorizontally)
                                                                .align(Alignment.Center)
                                                        },
                                                    ),
                                        )
                                    }
                                } else if (isVideo == false) {
                                    AsyncImage(
                                        model =
                                            ImageRequest
                                                .Builder(LocalPlatformContext.current)
                                                .data(state.screenData.canvasData?.url)
                                                .diskCachePolicy(CachePolicy.ENABLED)
                                                .diskCacheKey(state.screenData.canvasData?.url)
                                                .crossfade(550)
                                                .build(),
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                }
                            }
                            // Bottom gradient overlay — different intensity per state:
                            // - Focus: original full-height heavy gradient (controls readability)
                            // - Unfocus: 50% height + 50% lighter colors (just enough for metadata,
                            //   lets more canvas show through)
                            Crossfade(
                                targetState = state.showControlLayout,
                                modifier =
                                    Modifier
                                        .fillMaxSize()
                                        .align(Alignment.BottomCenter),
                            ) { focused ->
                                if (focused) {
                                    Box(
                                        modifier =
                                            Modifier
                                                .fillMaxSize()
                                                .background(
                                                    smoothScrimBrush(
                                                        from = overlay,
                                                        to = Color.Black,
                                                        startFraction = 0.2f,
                                                    ),
                                                ),
                                    )
                                } else {
                                    // Box fullscreen — gradient stops control where darkening starts.
                                    // Note: pager content can extend past visible viewport bottom due
                                    // to parent offsets. We span the FULL pager height (instead of a
                                    // fixed 120.dp BottomCenter Box) so the colorStops are anchored
                                    // to pager height — guaranteeing the visible viewport bottom
                                    // always falls inside the held-Black region (>=0.85f).
                                    // Unfocused gradient — compact dark coverage at the very bottom only:
                                    //   - 0%-92%: fully Transparent (canvas clear)
                                    //   - 92%-97%: quick fade to Black
                                    //   - 97%-100%: held solid Black (avoids canvas bleed-through
                                    //     at visible viewport bottom — alpha must reach 0xFF before
                                    //     the visible bottom, which sits at ~94-95% of pager).
                                    Box(
                                        modifier =
                                            Modifier
                                                .fillMaxSize()
                                                .background(
                                                    smoothScrimBrush(
                                                        from = Color.Black.copy(alpha = 0f),
                                                        to = Color.Black,
                                                        startFraction = 0.92f,
                                                        endFraction = 0.97f,
                                                    ),
                                                ),
                                    )
                                }
                            }
                        }

                        // ── Layer 2: centered square thumbnail ──
                        // Positioned at the same Y as the original middle layout
                        // (TopAppBar height + middleLayoutPaddingDp from the top of the page).
                        // alpha=0 when the canvas is covering this page; otherwise visible so
                        // adjacent pages always show the upcoming/previous track artwork.
                        Column(modifier = Modifier.fillMaxSize()) {
                            Spacer(modifier = Modifier.height(topAppBarHeightDp.dp))
                            Spacer(
                                modifier =
                                    Modifier
                                        .animateContentSize()
                                        .height(middleLayoutPaddingDp.dp)
                                        .fillMaxWidth(),
                            )
                            Box(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp)
                                        .alpha(if (pageHasCanvas) 0f else 1f)
                                        .aspectRatio(1f),
                            ) {
                                if (isCurrentArtworkPage) {
                                    // Live artwork (drives palette extraction via setBitmap).
                                    // The artwork URL that is actually loading. `maxresdefault.jpg` —
                                    // the fallback artworkUri many video tracks carry — only EXISTS
                                    // for videos with an HD thumbnail; everything else 404s,
                                    // onSuccess never fires, the palette never generates, and the
                                    // gradient sits on its fallback for a grey song. On error we
                                    // retry once with `hqdefault.jpg`, which YouTube guarantees for
                                    // every video. Song artwork (googleusercontent) never matches
                                    // the replace, so this is a no-op for it.
                                    var artworkUrl by remember(state.screenData.thumbnailURL) {
                                        mutableStateOf(state.screenData.thumbnailURL)
                                    }
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier =
                                            Modifier
                                                .align(Alignment.Center)
                                                .background(Color.Transparent)
                                                .shadow(
                                                    elevation = 3.dp,
                                                    shape = RoundedCornerShape(8.dp),
                                                    spotColor =
                                                        state.spotShadowColor.copy(
                                                            alpha = 0.6f,
                                                        ),
                                                    ambientColor = Color.Transparent,
                                                ),
                                    ) {
                                        AsyncImage(
                                            model =
                                                ImageRequest
                                                    .Builder(LocalPlatformContext.current)
                                                    .data(artworkUrl)
                                                    .diskCachePolicy(CachePolicy.ENABLED)
                                                    .diskCacheKey(artworkUrl + "BIGGER")
                                                    .crossfade(550)
                                                    .build(),
                                            contentDescription = "",
                                            onSuccess = {
                                                actions.onArtworkBitmap(
                                                    it.result.image.toImageBitmap(),
                                                )
                                            },
                                            onError = {
                                                val fallback = artworkUrl?.replace("maxresdefault", "hqdefault")
                                                if (fallback != null && fallback != artworkUrl) artworkUrl = fallback
                                            },
                                            contentScale = ContentScale.Crop,
                                            placeholder = rememberHolderPainter(),
                                            error = rememberHolderPainter(),
                                            modifier =
                                                Modifier
                                                    .align(Alignment.Center)
                                                    .padding(3.dp)
                                                    .fillMaxWidth()
                                                    .background(Color.Transparent)
                                                    .aspectRatio(
                                                        if (!state.screenData.isVideo) 1f else 16f / 9,
                                                    ).clip(
                                                        RoundedCornerShape(8.dp),
                                                    ).alpha(
                                                        if (!state.screenData.isVideo || !state.shouldShowVideo) 1f else 0f,
                                                    ),
                                        )
                                    }

                                    // Inline video player (current page + isVideo + shouldShowVideo).
                                    androidx.compose.animation.AnimatedVisibility(
                                        visible = state.screenData.isVideo && state.shouldShowVideo,
                                        modifier = Modifier.align(Alignment.Center),
                                    ) {
                                        var internalShowSubtitle by rememberSaveable {
                                            mutableStateOf(true)
                                        }
                                        Box(
                                            modifier =
                                                Modifier
                                                    .fillMaxWidth()
                                                    .aspectRatio(16f / 9)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(Color.Black),
                                        ) {
                                            Box(Modifier.fillMaxSize()) {
                                                MediaPlayerViewWithSubtitle(
                                                    playerName = MAIN_PLAYER,
                                                    modifier = Modifier.align(Alignment.Center),
                                                    shouldShowSubtitle = internalShowSubtitle,
                                                    shouldPip = false,
                                                    shouldScaleDownSubtitle = true,
                                                    timelineState = state.timelineState,
                                                    lyricsData = state.screenData.lyricsData?.lyrics,
                                                    translatedLyricsData = state.screenData.lyricsData?.translatedLyrics?.first,
                                                    isInPipMode = state.isInPipMode,
                                                    mainTextStyle = typo().bodyLarge,
                                                    translatedTextStyle = typo().bodyMedium,
                                                )
                                            }
                                            Box(
                                                modifier =
                                                    Modifier
                                                        .fillMaxSize()
                                                        .clickable(
                                                            onClick = { showHideFullscreenOverlay = !showHideFullscreenOverlay },
                                                            indication = null,
                                                            interactionSource =
                                                                remember {
                                                                    MutableInteractionSource()
                                                                },
                                                        ),
                                            ) {
                                                Crossfade(targetState = showHideFullscreenOverlay) {
                                                    if (it) {
                                                        Box(
                                                            modifier =
                                                                Modifier
                                                                    .fillMaxSize()
                                                                    .background(
                                                                        // The old middle stop (0.15f to overlay)
                                                                        // hand-approximated a convex falloff;
                                                                        // smoothstep does that on its own.
                                                                        smoothScrimBrush(
                                                                            from = blackMoreOverlay,
                                                                            to = overlay.copy(alpha = 0f),
                                                                            startFraction = 0.03f,
                                                                            endFraction = 0.8f,
                                                                        ),
                                                                    ),
                                                        ) {
                                                            IconButton(
                                                                onClick = {
                                                                    actions.onEnterFullscreenVideo()
                                                                },
                                                                Modifier.align(Alignment.TopEnd),
                                                            ) {
                                                                Icon(
                                                                    imageVector = SimpIcons.Fullscreen,
                                                                    contentDescription = "",
                                                                    tint = Color.White,
                                                                )
                                                            }
                                                            Row(
                                                                Modifier
                                                                    .align(Alignment.Center)
                                                                    .fillMaxWidth(),
                                                                horizontalArrangement = Arrangement.SpaceEvenly,
                                                            ) {
                                                                FilledTonalIconButton(
                                                                    colors =
                                                                        IconButtonDefaults.iconButtonColors().copy(
                                                                            containerColor = Color.Transparent,
                                                                        ),
                                                                    modifier =
                                                                        Modifier
                                                                            .size(48.dp)
                                                                            .aspectRatio(1f)
                                                                            .clip(CircleShape),
                                                                    onClick = {
                                                                        actions.onUIEvent(UIEvent.Backward)
                                                                    },
                                                                ) {
                                                                    Icon(
                                                                        imageVector = SimpIcons.Replay5,
                                                                        tint = Color.White,
                                                                        contentDescription = "",
                                                                        modifier =
                                                                            Modifier
                                                                                .size(36.dp)
                                                                                .alpha(0.8f),
                                                                    )
                                                                }
                                                                FilledTonalIconButton(
                                                                    colors =
                                                                        IconButtonDefaults.iconButtonColors().copy(
                                                                            containerColor = Color.Transparent,
                                                                        ),
                                                                    modifier =
                                                                        Modifier
                                                                            .size(48.dp)
                                                                            .aspectRatio(1f)
                                                                            .clip(CircleShape),
                                                                    onClick = {
                                                                        actions.onUIEvent(UIEvent.Forward)
                                                                    },
                                                                ) {
                                                                    Icon(
                                                                        imageVector = SimpIcons.Forward5,
                                                                        tint = Color.White,
                                                                        contentDescription = "",
                                                                        modifier =
                                                                            Modifier
                                                                                .size(36.dp)
                                                                                .alpha(0.8f),
                                                                    )
                                                                }
                                                            }
                                                            if (state.screenData.lyricsData != null) {
                                                                IconButton(
                                                                    onClick = {
                                                                        internalShowSubtitle = !internalShowSubtitle
                                                                    },
                                                                    Modifier.align(Alignment.BottomEnd),
                                                                ) {
                                                                    Icon(
                                                                        imageVector =
                                                                            if (internalShowSubtitle) {
                                                                                SimpIcons.SubtitlesOff
                                                                            } else {
                                                                                SimpIcons.Subtitles
                                                                            },
                                                                        contentDescription = "",
                                                                        tint = Color.White,
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } else if (pageTrack != null) {
                                    // Adjacent page — static thumbnail from Track.thumbnails.
                                    val staticThumb =
                                        pageTrack.thumbnails
                                            ?.maxByOrNull { it.width * it.height }
                                            ?.url
                                    val palettePageScope = rememberCoroutineScope()
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier =
                                            Modifier
                                                .align(Alignment.Center)
                                                .background(Color.Transparent)
                                                .shadow(
                                                    elevation = 3.dp,
                                                    shape = RoundedCornerShape(8.dp),
                                                    spotColor = Color.Black.copy(alpha = 0.4f),
                                                    ambientColor = Color.Transparent,
                                                ),
                                    ) {
                                        AsyncImage(
                                            model =
                                                ImageRequest
                                                    .Builder(LocalPlatformContext.current)
                                                    .data(staticThumb)
                                                    .diskCachePolicy(CachePolicy.ENABLED)
                                                    .diskCacheKey(staticThumb)
                                                    .crossfade(300)
                                                    .build(),
                                            contentDescription = pageTrack.title,
                                            contentScale = ContentScale.Crop,
                                            placeholder = rememberHolderPainter(),
                                            error = rememberHolderPainter(),
                                            // Feed the per-page palette using the SAME bitmap
                                            // we just rendered so the Layer 0 gradient backdrop
                                            // matches what the user sees on screen.
                                            onSuccess = { state ->
                                                palettePageScope.launch {
                                                    pagePaletteState.generate(
                                                        state.result.image.toImageBitmap(),
                                                    )
                                                }
                                            },
                                            modifier =
                                                Modifier
                                                    .align(Alignment.Center)
                                                    .padding(3.dp)
                                                    .fillMaxWidth()
                                                    .aspectRatio(1f)
                                                    .clip(RoundedCornerShape(8.dp)),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                CenterAlignedTopAppBar(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                            .onGloballyPositioned {
                                topAppBarHeightDp =
                                    with(localDensity) {
                                        it.size.height
                                            .toDp()
                                            .value
                                            .toInt()
                                    }
                            }.padding(
                                top = with(localDensity) { WindowInsets.statusBars.getTop(localDensity).toDp() },
                            ),
                    colors =
                        TopAppBarDefaults.topAppBarColors().copy(
                            containerColor = Color.Transparent,
                        ),
                    // Position-aware insets shrink per frame while the sheet is dragged (pinned
                    // bar + layout jitter) — status-bar space is static padding on the modifier.
                    windowInsets = WindowInsets(0, 0, 0, 0),
                    title = {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text(
                                text = stringResource(Res.string.now_playing_upper),
                                style = typo().bodyMedium,
                                color = Color.White,
                            )
                            Text(
                                text = state.screenData.playlistName,
                                style = typo().labelMedium,
                                color = Color.White,
                                textAlign = TextAlign.Center,
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
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            actions.onDismiss()
                        }) {
                            Icon(
                                imageVector = state.dismissIcon,
                                contentDescription = "",
                                tint = Color.White,
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            actions.onShowMoreSheet()
                        }) {
                            Icon(
                                imageVector = SimpIcons.MoreVert,
                                contentDescription = "",
                                tint = Color.White,
                            )
                        }
                    },
                )
                Column {
                    Spacer(
                        modifier =
                            Modifier.height(
                                topAppBarHeightDp.dp,
                            ),
                    )
                    Box {
                        Column(
                            Modifier
                                .fillMaxWidth(),
                        ) {
                            Spacer(
                                modifier =
                                    Modifier
                                        .animateContentSize()
                                        .height(
                                            middleLayoutPaddingDp.dp,
                                        ).fillMaxWidth(),
                            )

                            // Artwork is rendered by the unified ArtworkPager above (which lives in the
                            // outer Box). Reserve the same vertical space here so the Info Layout below
                            // stays at its original Y position. Spacer has no pointer input so it does
                            // not block the pager swipe gesture beneath it.
                            Spacer(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp)
                                        .onGloballyPositioned { coords ->
                                            middleLayoutHeightDp =
                                                with(localDensity) {
                                                    coords.size.height
                                                        .toDp()
                                                        .value
                                                        .toInt()
                                                }
                                        }.aspectRatio(1f),
                            )

                            // Spotify-style current lyric line — vertically centered in the gap
                            // between the artwork and the info layout below. This Box replaces the
                            // plain gap Spacer at the same middleLayoutPaddingDp height, so the
                            // info layout position never moves; the line just lives inside the gap.
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier =
                                    Modifier
                                        .animateContentSize()
                                        .height(
                                            middleLayoutPaddingDp.dp,
                                        ).fillMaxWidth(),
                            ) {
                                val inlineLyrics = state.screenData.lyricsData?.lyrics
                                val hasSyncedLyrics =
                                    inlineLyrics != null &&
                                        inlineLyrics.syncType != null &&
                                        inlineLyrics.syncType != "UNSYNCED" &&
                                        inlineLyrics.lines != null
                                // Canvas mode has its own subtitle overlay — never show both.
                                val currentLyricLineText =
                                    if (!hasSyncedLyrics ||
                                        state.screenData.canvasData != null ||
                                        state.currentLyricLineIndex < 0
                                    ) {
                                        ""
                                    } else {
                                        inlineLyrics
                                            ?.lines
                                            ?.getOrNull(state.currentLyricLineIndex)
                                            ?.words
                                            ?.stripRichSyncTimestamps()
                                            .orEmpty()
                                    }
                                Crossfade(
                                    targetState = currentLyricLineText,
                                    animationSpec = tween(durationMillis = 300),
                                    label = "inlineLyricLine",
                                ) { lineText ->
                                    Text(
                                        text = lineText,
                                        style = typo().labelSmall,
                                        color = Color.White,
                                        maxLines = 1,
                                        modifier =
                                            Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 20.dp)
                                                .basicMarquee(
                                                    iterations = Int.MAX_VALUE,
                                                    animationMode = MarqueeAnimationMode.Immediately,
                                                ).focusable(),
                                    )
                                }
                            }

                            // Info Layout
                            Box {
                                Column(
                                    Modifier
                                        .alpha(state.controlLayoutAlpha)
                                        .onGloballyPositioned {
                                            infoLayoutHeightDp =
                                                with(localDensity) {
                                                    it.size.height
                                                        .toDp()
                                                        .value
                                                        .toInt()
                                                }
                                        },
                                ) {
                                    NowPlayingTrackInfoRow(
                                        state = state,
                                        actions = actions,
                                    )
                                    if (getPlatform() == Platform.Android) {
                                        // Real Slider
                                        Box(
                                            Modifier
                                                .padding(
                                                    top = 15.dp,
                                                ).padding(horizontal = 20.dp)
                                                .isElementVisible {
                                                    actions.onToolbarVisibilityChange(!it && state.isExpanded && state.mainScrollState.value > 0)
                                                },
                                        ) {
                                            Box(
                                                modifier =
                                                    Modifier
                                                        .fillMaxWidth()
                                                        .height(24.dp),
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                Crossfade(state.timelineState.loading) {
                                                    if (it) {
                                                        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                                                            LinearProgressIndicator(
                                                                modifier =
                                                                    Modifier
                                                                        .fillMaxWidth()
                                                                        .height(4.dp)
                                                                        .padding(
                                                                            horizontal = 3.dp,
                                                                        ).clip(
                                                                            RoundedCornerShape(8.dp),
                                                                        ),
                                                                color = Color.Gray,
                                                                trackColor = Color.DarkGray,
                                                                strokeCap = StrokeCap.Round,
                                                            )
                                                        }
                                                    } else {
                                                        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                                                            LinearProgressIndicator(
                                                                progress = { state.timelineState.bufferedPercent.toFloat() / 100 },
                                                                modifier =
                                                                    Modifier
                                                                        .fillMaxWidth()
                                                                        .height(4.dp)
                                                                        .padding(
                                                                            horizontal = 3.dp,
                                                                        ).clip(
                                                                            RoundedCornerShape(8.dp),
                                                                        ),
                                                                color = Color.Gray,
                                                                trackColor =
                                                                    Color.Gray.copy(
                                                                        alpha = 0.6f,
                                                                    ),
                                                                strokeCap = StrokeCap.Round,
                                                                drawStopIndicator = {},
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                            CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                                                Slider(
                                                    // material3 1.5.0-alpha25 keeps a
                                                    // binary-compatibility overload of Slider that
                                                    // accepts valueRange and then forwards without
                                                    // it, so the slider silently runs on the
                                                    // default 0f..1f and anything larger is clamped
                                                    // to a full track. Hand it a fraction instead;
                                                    // sliderValue stays on the 0..100 scale that
                                                    // UIEvent.UpdateProgress and the time labels
                                                    // are built around.
                                                    value = state.sliderValue / 100f,
                                                    onValueChangeFinished = {
                                                        actions.onSliderChangeFinished()
                                                    },
                                                    onValueChange = {
                                                        actions.onSliderChange(it * 100f)
                                                    },
                                                    modifier =
                                                        Modifier
                                                            .fillMaxWidth()
                                                            .padding(top = 3.dp)
                                                            .align(
                                                                Alignment.TopCenter,
                                                            ),
                                                    track = { sliderState ->
                                                        SliderDefaults.Track(
                                                            modifier =
                                                                Modifier
                                                                    .height(5.dp),
                                                            enabled = true,
                                                            sliderState = sliderState,
                                                            colors =
                                                                SliderDefaults.colors().copy(
                                                                    thumbColor = state.sliderTrackColor,
                                                                    activeTrackColor = state.sliderTrackColor,
                                                                    inactiveTrackColor = Color.Transparent,
                                                                ),
                                                            thumbTrackGapSize = 0.dp,
                                                            drawTick = { _, _ -> },
                                                            drawStopIndicator = null,
                                                        )
                                                    },
                                                    thumb = {
                                                        SliderDefaults.Thumb(
                                                            modifier =
                                                                Modifier
                                                                    .height(18.dp)
                                                                    .width(8.dp)
                                                                    .padding(
                                                                        vertical = 4.dp,
                                                                    ),
                                                            thumbSize = DpSize(8.dp, 8.dp),
                                                            interactionSource =
                                                                remember {
                                                                    MutableInteractionSource()
                                                                },
                                                            colors =
                                                                SliderDefaults.colors().copy(
                                                                    thumbColor = state.sliderTrackColor,
                                                                    activeTrackColor = state.sliderTrackColor,
                                                                    inactiveTrackColor = Color.Transparent,
                                                                ),
                                                            enabled = true,
                                                        )
                                                    },
                                                )
                                            }
                                        }
                                        // Time Layout
                                        Row(
                                            Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 20.dp),
                                        ) {
                                            Text(
                                                text = formatDuration((state.timelineState.total * (state.sliderValue / 100f)).roundToLong()),
                                                style = typo().bodyMedium,
                                                modifier = Modifier.weight(1f),
                                                textAlign = TextAlign.Left,
                                            )
                                            // Sweep head for the "Crossfading" shimmer, 0..1. Runs
                                            // unconditionally: behind the crossfade check it would
                                            // restart from zero each time the label appears (same
                                            // rationale as MiniPlayer's crossfadeSweep).
                                            val sweepTransition = rememberInfiniteTransition(label = "nowPlayingCrossfadeSweep")
                                            val crossfadeSweep by sweepTransition.animateFloat(
                                                initialValue = 0f,
                                                targetValue = 1f,
                                                animationSpec =
                                                    infiniteRepeatable(
                                                        animation = tween(3200, easing = LinearEasing),
                                                        repeatMode = RepeatMode.Restart,
                                                    ),
                                                label = "nowPlayingSweepHead",
                                            )
                                            AnimatedVisibility(
                                                enter = fadeIn(),
                                                exit = fadeOut(),
                                                visible = state.timelineState.isCrossfading,
                                            ) {
                                                // Same effect as the desktop MiniPlayer label: a
                                                // highlight sweeping through the glyphs via a text
                                                // brush — no overlay, no clipping.
                                                val shimmerSpan = 140f
                                                val shimmerHead = crossfadeSweep * (shimmerSpan * 3f) - shimmerSpan
                                                val labelColor = typo().bodyMedium.color
                                                Text(
                                                    text = stringResource(Res.string.crossfading),
                                                    style =
                                                        typo().bodyMedium.copy(
                                                            brush =
                                                                Brush.horizontalGradient(
                                                                    0f to labelColor.copy(alpha = 0.45f),
                                                                    // The sweep head is PURE white, not the resting label colour — the label
                                                                    // colour is an adaptive grey, and a grey gleam reads as no gleam at all.
                                                                    0.5f to Color.White,
                                                                    1f to labelColor.copy(alpha = 0.45f),
                                                                    startX = shimmerHead,
                                                                    endX = shimmerHead + shimmerSpan,
                                                                    tileMode = TileMode.Clamp,
                                                                ),
                                                        ),
                                                    modifier = Modifier.weight(1f),
                                                    textAlign = TextAlign.Center,
                                                )
                                            }
                                            Text(
                                                text = formatDuration(state.timelineState.total),
                                                style = typo().bodyMedium,
                                                modifier = Modifier.weight(1f),
                                                textAlign = TextAlign.Right,
                                            )
                                        }

                                        Spacer(
                                            modifier =
                                                Modifier
                                                    .fillMaxWidth()
                                                    .height(5.dp),
                                        )
                                        // Control Button Layout
                                        PlayerControlLayout(
                                            state.controllerState,
                                        ) {
                                            actions.onUIEvent(it)
                                        }
                                    } else {
                                        Spacer(Modifier.height(16.dp))
                                    }
                                    // List Bottom Buttons - MODIFIED TO ADD PLAYLIST BUTTON
                                    Row(
                                        modifier =
                                            Modifier
                                                .height(32.dp)
                                                .fillMaxWidth()
                                                .padding(horizontal = 20.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        // Info + Cast Buttons (Left)
                                        // weight(fill = false) keeps a long device name from shoving the
                                        // playlist/queue buttons off the end of this SpaceBetween row.
                                        Row(
                                            modifier = Modifier.weight(1f, fill = false),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            IconButton(
                                                modifier =
                                                    Modifier
                                                        .size(24.dp)
                                                        .aspectRatio(1f)
                                                        .clip(CircleShape),
                                                onClick = {
                                                    actions.onShowInfo()
                                                },
                                            ) {
                                                Icon(imageVector = SimpIcons.Info, tint = Color.White, contentDescription = "")
                                            }
                                            // Cyan rather than colorScheme.primary: this screen is force-dark whatever
                                            // the app theme is, so a light-theme primary would sink into the black
                                            // backdrop. Mirrors the `if (forceDark) Color.Cyan` rule in FullWidthItems.
                                            PlatformCastButton(
                                                modifier = Modifier.size(24.dp),
                                                tint = if (state.castState.isRemote) Color.Cyan else Color.White,
                                            )
                                            AnimatedVisibility(visible = state.castState.isRemote) {
                                                Text(
                                                    text =
                                                        stringResource(
                                                            Res.string.playing_on_device,
                                                            state.castState.deviceName ?: "Cast",
                                                        ),
                                                    style = typo().bodySmall,
                                                    color = Color.Cyan,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                )
                                            }
                                        }

                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            // NEW: Add to Playlist Button (Center-Right)
                                            IconButton(
                                                modifier =
                                                    Modifier
                                                        .size(24.dp)
                                                        .aspectRatio(1f)
                                                        .clip(CircleShape),
                                                onClick = {
                                                    actions.onShowAddToPlaylist()
                                                },
                                            ) {
                                                Icon(
                                                    imageVector = SimpIcons.PlaylistAdd,
                                                    tint = Color.White,
                                                    contentDescription = "Add to Playlist",
                                                )
                                            }

                                            // Queue Button (Right)
                                            IconButton(
                                                modifier =
                                                    Modifier
                                                        .size(24.dp)
                                                        .aspectRatio(1f)
                                                        .clip(CircleShape),
                                                onClick = {
                                                    actions.onShowQueue()
                                                },
                                            ) {
                                                Icon(
                                                    imageVector = SimpIcons.QueueMusic,
                                                    tint = Color.White,
                                                    contentDescription = "",
                                                )
                                            }
                                        }
                                    }
                                }
                                this@Column.AnimatedVisibility(
                                    visible = !state.showControlLayout,
                                    enter = fadeIn(),
                                    exit = fadeOut(),
                                ) {
                                    Box(
                                        modifier =
                                            Modifier
                                                .height(
                                                    infoLayoutHeightDp.dp,
                                                ).fillMaxWidth()
                                                .clickable(
                                                    onClick = {
                                                        if (state.mainScrollState.value == 0) {
                                                            actions.onToggleControls()
                                                        }
                                                    },
                                                    indication = null,
                                                    interactionSource =
                                                        remember {
                                                            MutableInteractionSource()
                                                        },
                                                ),
                                        contentAlignment = Alignment.BottomStart,
                                    ) {
                                        // Gradient backdrop — transparent at top so Canvas shows
                                        // through, fading to dark at the bottom for a Spotify-like
                                        // backdrop under the metadata row.
                                        Box(
                                            modifier =
                                                Modifier
                                                    .fillMaxSize()
                                                    .background(
                                                        smoothScrimBrush(
                                                            from = Color.Black.copy(alpha = 0f),
                                                            to = Color.Black.copy(alpha = 0.85f),
                                                        ),
                                                    ),
                                        )

                                        Column(
                                            modifier =
                                                Modifier
                                                    .fillMaxWidth()
                                                    .animateContentSize(),
                                        ) {
                                            this@Column.AnimatedVisibility(
                                                visible = state.currentLyricLineIndex > -1,
                                                enter = fadeIn() + expandVertically(),
                                                exit = fadeOut() + shrinkVertically(),
                                            ) {
                                                // Canvas subtitle - Spotify-style: lyrics line above metadata row
                                                val lineText =
                                                    state.screenData.lyricsData
                                                        ?.lyrics
                                                        ?.lines
                                                        ?.getOrNull(state.currentLyricLineIndex)
                                                        ?.words
                                                        ?.stripRichSyncTimestamps()
                                                if (!lineText.isNullOrBlank()) {
                                                    Column(
                                                        modifier = Modifier.fillMaxWidth(),
                                                    ) {
                                                        Text(
                                                            modifier =
                                                                Modifier
                                                                    .fillMaxWidth()
                                                                    .padding(horizontal = 20.dp)
                                                                    .padding(bottom = 4.dp)
                                                                    .basicMarquee(
                                                                        iterations = Int.MAX_VALUE,
                                                                        animationMode = MarqueeAnimationMode.Immediately,
                                                                    ).focusable(),
                                                            text = lineText,
                                                            style = typo().bodyMedium,
                                                            color = Color.White,
                                                            maxLines = 1,
                                                        )
                                                        val translatedLineText =
                                                            state.screenData.lyricsData
                                                                ?.translatedLyrics
                                                                ?.first
                                                                ?.lines
                                                                ?.getOrNull(state.currentLyricLineIndex)
                                                                ?.words
                                                                ?.stripRichSyncTimestamps()
                                                        if (!translatedLineText.isNullOrBlank()) {
                                                            Text(
                                                                modifier =
                                                                    Modifier
                                                                        .fillMaxWidth()
                                                                        .padding(horizontal = 20.dp)
                                                                        .padding(bottom = 8.dp)
                                                                        .basicMarquee(
                                                                            iterations = Int.MAX_VALUE,
                                                                            animationMode = MarqueeAnimationMode.Immediately,
                                                                        ).focusable(),
                                                                text = translatedLineText,
                                                                style = typo().bodyMedium,
                                                                color = Color.Yellow,
                                                                maxLines = 1,
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                            NowPlayingTrackInfoRow(
                                                state = state,
                                                actions = actions,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        // The original Touch Area overlay was removed: tap-to-toggle is now
                        // wired directly onto each ArtworkPager page (canvas + middle), so
                        // drag gestures reach HorizontalPager without competing with a
                        // sibling clickable.
                    }
                    Column(Modifier.padding(horizontal = 20.dp)) {
                        // Lyrics Layout
                        AnimatedVisibility(
                            visible = state.screenData.lyricsData != null,
                            modifier = Modifier.padding(top = 10.dp),
                        ) {
                            ElevatedCard(
                                onClick = {},
                                shape = RoundedCornerShape(8.dp),
                                colors =
                                    CardDefaults.elevatedCardColors().copy(
                                        containerColor = state.startColor.value,
                                    ),
                            ) {
                                Column(modifier = Modifier.padding(15.dp)) {
                                    Spacer(modifier = Modifier.height(5.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = stringResource(Res.string.lyrics),
                                            style = typo().labelMedium,
                                            color = Color.White,
                                        )
                                        if (state.screenData.lyricsData?.translatedLyrics?.second == LyricsProvider.AI) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            AIBadge()
                                        }
                                        Spacer(modifier = Modifier.weight(1f))
                                        // Vote button — only when the lyrics or the translation came from
                                        // SimpMusic Lyrics. The rule itself lives on the shared contract
                                        // (canVote), so a style cannot ship without it the way the Apple
                                        // Music tab did.
                                        if (state.screenData.lyricsData.canVote()) {
                                            CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                                                IconButton(
                                                    onClick = {
                                                        actions.onShowVoteDialog()
                                                    },
                                                ) {
                                                    Icon(
                                                        imageVector = SimpIcons.ThumbsUpDown,
                                                        contentDescription = stringResource(Res.string.rate_lyrics),
                                                        tint = Color.White,
                                                        modifier = Modifier.size(16.dp),
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                        }
                                        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                                            IconButton(
                                                onClick = { showShareLyricsSheet = true },
                                            ) {
                                                Icon(
                                                    imageVector = SimpIcons.Share,
                                                    contentDescription = stringResource(Res.string.share_lyrics),
                                                    tint = Color.White,
                                                    modifier = Modifier.size(16.dp),
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                                            TextButton(
                                                onClick = {
                                                    actions.onShowFullscreenLyrics()
                                                },
                                                contentPadding = PaddingValues(0.dp),
                                                modifier =
                                                    Modifier
                                                        .height(20.dp)
                                                        .wrapContentWidth(),
                                            ) {
                                                Text(text = stringResource(Res.string.show), color = Color.White)
                                            }
                                        }
                                    }
                                    // Lyrics Layout
                                    Spacer(modifier = Modifier.height(18.dp))
                                    Box(
                                        modifier =
                                            Modifier
                                                .fillMaxWidth()
                                                .height(300.dp),
                                    ) {
                                        state.screenData.lyricsData?.let {
                                            LyricsView(
                                                lyricsData = it,
                                                timeLine = state.timelineFlow,
                                                onLineClick = { f ->
                                                    actions.onUIEvent(UIEvent.UpdateProgress(f))
                                                },
                                            )
                                        }
                                    }

                                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
                                        Text(
                                            text =
                                                when (state.screenData.lyricsData?.lyrics?.syncType) {
                                                    "LINE_SYNCED" -> stringResource(Res.string.line_synced)
                                                    "RICH_SYNCED" -> stringResource(Res.string.rich_synced)
                                                    else -> stringResource(Res.string.unsynced)
                                                },
                                            style = typo().bodySmall,
                                            textAlign = TextAlign.End,
                                            modifier =
                                                Modifier
                                                    .fillMaxWidth()
                                                    .padding(top = 10.dp),
                                        )
                                        Text(
                                            text =
                                                when (state.screenData.lyricsData?.lyricsProvider) {
                                                    LyricsProvider.SIMPMUSIC -> {
                                                        stringResource(Res.string.lyrics_provider_simpmusic)
                                                    }

                                                    LyricsProvider.LRCLIB -> {
                                                        stringResource(Res.string.lyrics_provider_lrc)
                                                    }

                                                    LyricsProvider.YOUTUBE -> {
                                                        stringResource(Res.string.lyrics_provider_youtube)
                                                    }

                                                    LyricsProvider.SPOTIFY -> {
                                                        stringResource(Res.string.spotify_lyrics_provider)
                                                    }

                                                    LyricsProvider.OFFLINE -> {
                                                        stringResource(Res.string.offline_mode)
                                                    }

                                                    LyricsProvider.BETTER_LYRICS -> {
                                                        stringResource(Res.string.lyrics_provider_betterlyrics)
                                                    }

                                                    else -> {
                                                        ""
                                                    }
                                                },
                                            style = typo().bodySmall,
                                            textAlign = TextAlign.End,
                                            modifier =
                                                Modifier
                                                    .fillMaxWidth(),
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        AnimatedVisibility(visible = state.screenData.songInfoData != null) {
                            ElevatedCard(
                                onClick = {
                                    actions.onNavigateToArtist()
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors =
                                    CardDefaults.elevatedCardColors().copy(
                                        containerColor = Color(0xFF212121),
                                    ),
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    // Artwork occupies the top of the card; only the section
                                    // label sits on top of it. Name and subscriber count moved
                                    // below onto the solid card surface so they stay readable
                                    // regardless of how bright the artist photo is.
                                    Box(
                                        modifier =
                                            Modifier
                                                .fillMaxWidth()
                                                .height(250.dp),
                                    ) {
                                        val thumb = state.screenData.songInfoData?.authorThumbnail
                                        AsyncImage(
                                            model =
                                                ImageRequest
                                                    .Builder(LocalPlatformContext.current)
                                                    .data(thumb)
                                                    .diskCachePolicy(CachePolicy.ENABLED)
                                                    .diskCacheKey(thumb)
                                                    .crossfade(550)
                                                    .build(),
                                            placeholder = rememberHolderPainter(isVideo = true),
                                            error = rememberHolderPainter(isVideo = true),
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            // No explicit clip: the ElevatedCard already clips to
                                            // its 8.dp shape, so only the card's top corners round
                                            // and the image meets the panel below flush.
                                            modifier = Modifier.fillMaxSize(),
                                        )
                                        // Scrim behind the label: artist photos are often bright
                                        // at the top, which swallowed the white text.
                                        Box(
                                            modifier =
                                                Modifier
                                                    .matchParentSize()
                                                    .background(
                                                        smoothScrimBrush(
                                                            from = Color.Black.copy(alpha = 0.6f),
                                                            to = Color.Black.copy(alpha = 0f),
                                                            endFraction = 0.4f,
                                                        ),
                                                    ),
                                        )
                                        Text(
                                            text = stringResource(Res.string.artists),
                                            style = typo().labelMedium,
                                            color = Color.White,
                                            modifier =
                                                Modifier
                                                    .align(Alignment.TopStart)
                                                    .padding(15.dp),
                                        )
                                    }
                                    Column(
                                        modifier =
                                            Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 15.dp, vertical = 12.dp),
                                    ) {
                                        Text(
                                            text = state.screenData.songInfoData?.author ?: "",
                                            style = typo().titleMedium,
                                            color = Color.White,
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = state.screenData.songInfoData?.subscribers ?: "",
                                            style = typo().bodySmall,
                                            color = Color.White.copy(alpha = 0.7f),
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        AnimatedVisibility(visible = state.screenData.songInfoData != null) {
                            ElevatedCard(
                                onClick = {},
                                shape = RoundedCornerShape(8.dp),
                                colors =
                                    CardDefaults.elevatedCardColors().copy(
                                        containerColor = state.startColor.value,
                                    ),
                            ) {
                                Column(
                                    Modifier
                                        .padding(15.dp)
                                        .fillMaxWidth(),
                                ) {
                                    Spacer(modifier = Modifier.height(5.dp))
                                    Text(
                                        text = stringResource(Res.string.published_at, state.screenData.songInfoData?.uploadDate ?: ""),
                                        style = typo().labelSmall,
                                        color = Color.White,
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text =
                                            stringResource(
                                                Res.string.view_count,
                                                "%,d".format(state.screenData.songInfoData?.viewCount),
                                            ),
                                        style = typo().labelMedium,
                                        color = Color.White,
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text =
                                            stringResource(
                                                Res.string.like_and_dislike,
                                                state.screenData.songInfoData?.like ?: 0,
                                                state.screenData.songInfoData?.dislike ?: 0,
                                            ),
                                        style = typo().bodyMedium,
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = stringResource(Res.string.description),
                                        style = typo().labelSmall,
                                        color = Color.White,
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    DescriptionView(
                                        text = state.screenData.songInfoData?.description ?: "",
                                        onTimeClicked = { raw ->
                                            val timestamp = parseTimestampToMilliseconds(raw)
                                            if (timestamp != 0.0 && timestamp < state.timelineState.total) {
                                                actions.onUIEvent(
                                                    UIEvent.UpdateProgress(
                                                        ((timestamp * 100) / state.timelineState.total).toFloat(),
                                                    ),
                                                )
                                            }
                                        },
                                        onURLClicked = { url ->
                                            uriHandler.openUri(
                                                url,
                                            )
                                        },
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(5.dp))
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Spacer(
                            modifier =
                                Modifier.height(
                                    with(localDensity) { WindowInsets.systemBars.getBottom(localDensity).toDp() },
                                ),
                        )
                    }
                }
            }
        }
        AnimatedVisibility(
            visible = state.shouldShowToolbar && state.isExpanded,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically(),
        ) {
            ElevatedCard(
                elevation = CardDefaults.elevatedCardElevation(10.dp),
                shape = RectangleShape,
                colors =
                    CardDefaults.elevatedCardColors(
                        containerColor =
                            state.startColor.value
                                .copy(
                                    red = state.startColor.value.red - 0.05f,
                                    green = state.startColor.value.green - 0.05f,
                                    blue = state.startColor.value.blue - 0.05f,
                                ),
                    ),
                modifier =
                    Modifier
                        .clipToBounds()
                        .wrapContentHeight()
                        .fillMaxWidth(),
            ) {
                Box(
                    modifier =
                        Modifier.padding(
                            top = with(localDensity) { WindowInsets.statusBars.getTop(localDensity).toDp() },
                        ),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier =
                            Modifier
                                .padding(
                                    vertical = 8.dp,
                                    horizontal = 15.dp,
                                ).fillMaxWidth(),
                    ) {
                        Spacer(modifier = Modifier.size(8.dp))
                        Box(modifier = Modifier.weight(1F)) {
                            Column(
                                Modifier
                                    .wrapContentHeight(),
                            ) {
                                Text(
                                    text = state.screenData.nowPlayingTitle,
                                    style = typo().bodyMedium,
                                    color = Color.White,
                                    maxLines = 1,
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .wrapContentHeight(
                                                align = Alignment.CenterVertically,
                                            ).basicMarquee(
                                                iterations = Int.MAX_VALUE,
                                                animationMode = MarqueeAnimationMode.Immediately,
                                            ).focusable(),
                                )
                                LazyRow(verticalAlignment = Alignment.CenterVertically) {
                                    item {
                                        AnimatedVisibility(visible = state.screenData.isExplicit) {
                                            ExplicitBadge(
                                                modifier =
                                                    Modifier
                                                        .size(20.dp)
                                                        .padding(end = 4.dp)
                                                        .weight(1f),
                                            )
                                        }
                                    }
                                    item(
                                        key = state.screenData.artistName,
                                    ) {
                                        Text(
                                            text = state.screenData.artistName,
                                            style = typo().bodySmall,
                                            maxLines = 1,
                                            modifier =
                                                Modifier
                                                    .fillMaxWidth()
                                                    .wrapContentHeight(
                                                        align = Alignment.CenterVertically,
                                                    ).basicMarquee(
                                                        iterations = Int.MAX_VALUE,
                                                        animationMode = MarqueeAnimationMode.Immediately,
                                                    ).focusable(),
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.width(15.dp))
                        HeartCheckBox(checked = state.controllerState.isLiked, size = 30) {
                            actions.onUIEvent(UIEvent.ToggleLike)
                        }
                        Spacer(modifier = Modifier.width(15.dp))
                        Crossfade(targetState = state.timelineState.loading, label = "") {
                            if (it) {
                                Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = Color.LightGray,
                                        strokeWidth = 3.dp,
                                    )
                                }
                            } else {
                                PlayPauseButton(isPlaying = state.controllerState.isPlaying, modifier = Modifier.size(48.dp)) {
                                    actions.onUIEvent(UIEvent.PlayPause)
                                }
                            }
                        }
                    }
                    Box(
                        modifier =
                            Modifier
                                .wrapContentSize(Alignment.Center)
                                .align(Alignment.BottomCenter),
                    ) {
                        LinearProgressIndicator(
                            progress = { state.timelineState.current.toFloat() / state.timelineState.total },
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(
                                        color = Color.Transparent,
                                        shape = RoundedCornerShape(4.dp),
                                    ),
                            color = Color.White,
                            trackColor = Color.Gray.copy(alpha = 0.4f),
                            strokeCap = StrokeCap.Round,
                            drawStopIndicator = {},
                        )
                    }
                }
            }
        }
    }

    state.screenData.lyricsData?.let { lyricsData ->
        if (showShareLyricsSheet) {
            ShareLyricsSheet(
                lines = lyricsData.toShareLyricsLines(),
                songTitle = state.screenData.nowPlayingTitle,
                artistName = state.screenData.artistName,
                artwork = state.screenData.bitmap,
                seedColor = state.startColor.value,
                initialLineIndex = state.currentLyricLineIndex,
                onDismiss = { showShareLyricsSheet = false },
            )
        }
    }
}

// The focused info layout (controls visible) and the canvas-unfocused overlay rendered this
// exact metadata row twice — thumbnail-when-canvas, title, explicit badge + artists,
// YouTube like button, favourite heart. Extracted once; both call sites pass the same holders.
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NowPlayingTrackInfoRow(
    state: NowPlayingContentState,
    actions: NowPlayingContentActions,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AnimatedVisibility(state.screenData.canvasData != null) {
            AsyncImage(
                model =
                    ImageRequest
                        .Builder(LocalPlatformContext.current)
                        .data(state.screenData.thumbnailURL)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .diskCacheKey(state.screenData.thumbnailURL + "BIGGER")
                        .crossfade(true)
                        .build(),
                placeholder = rememberHolderPainter(),
                error = rememberHolderPainter(),
                contentDescription = null,
                contentScale = ContentScale.FillWidth,
                modifier =
                    Modifier
                        .heightIn(0.dp, 55.dp)
                        .width(55.dp)
                        .padding(end = 10.dp)
                        .clip(
                            RoundedCornerShape(4.dp),
                        ).align(Alignment.CenterVertically),
            )
        }

        Column(Modifier.weight(1f)) {
            Text(
                text = state.screenData.nowPlayingTitle,
                style = typo().titleMedium,
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
            Spacer(modifier = Modifier.height(3.dp))
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                item(state.screenData.isExplicit) {
                    AnimatedVisibility(visible = state.screenData.isExplicit) {
                        ExplicitBadge(
                            modifier =
                                Modifier
                                    .size(20.dp)
                                    .padding(end = 4.dp)
                                    .weight(1f),
                        )
                    }
                }
                item(state.screenData.artistName) {
                    Text(
                        text = state.screenData.artistName,
                        style = typo().bodyMedium,
                        maxLines = 1,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .wrapContentHeight(align = Alignment.CenterVertically)
                                .basicMarquee(
                                    iterations = Int.MAX_VALUE,
                                    animationMode = MarqueeAnimationMode.Immediately,
                                ).focusable()
                                .clickable {
                                    actions.onNavigateToArtist()
                                },
                    )
                }
            }
        }
        if (state.isUserLoggedIn) {
            Spacer(modifier = Modifier.size(16.dp))
            Crossfade(
                targetState = state.likeStatus,
            ) {
                if (it) {
                    IconButton(
                        modifier =
                            Modifier
                                .size(24.dp)
                                .aspectRatio(1f)
                                .clip(
                                    CircleShape,
                                ),
                        onClick = {
                            actions.onAddToYouTubeLiked()
                        },
                    ) {
                        Icon(imageVector = SimpIcons.CheckCircle, tint = Color.White, contentDescription = "")
                    }
                } else {
                    IconButton(
                        modifier =
                            Modifier
                                .size(24.dp)
                                .aspectRatio(1f)
                                .clip(
                                    CircleShape,
                                ),
                        onClick = {
                            actions.onAddToYouTubeLiked()
                        },
                    ) {
                        Icon(
                            imageVector = SimpIcons.AddCircleOutline,
                            tint = Color.White,
                            contentDescription = "",
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.size(12.dp))
        HeartCheckBox(checked = state.controllerState.isLiked, size = 32) {
            actions.onUIEvent(UIEvent.ToggleLike)
        }
    }
}
