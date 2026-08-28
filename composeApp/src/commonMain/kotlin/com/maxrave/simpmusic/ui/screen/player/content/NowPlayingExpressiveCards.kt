package com.maxrave.simpmusic.ui.screen.player.content

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.maxrave.common.Config.MAIN_PLAYER
import com.maxrave.simpmusic.Platform
import com.maxrave.simpmusic.expect.ui.MediaPlayerView
import com.maxrave.simpmusic.expect.ui.MediaPlayerViewWithSubtitle
import com.maxrave.simpmusic.expect.ui.toImageBitmap
import com.maxrave.simpmusic.extension.parseTimestampToMilliseconds
import com.maxrave.simpmusic.extension.smoothScrimBrush
import com.maxrave.simpmusic.getPlatform
import com.maxrave.simpmusic.ui.component.AIBadge
import com.maxrave.simpmusic.ui.component.DescriptionView
import com.maxrave.simpmusic.ui.component.ExplicitBadge
import com.maxrave.simpmusic.ui.component.HeartCheckBox
import com.maxrave.simpmusic.ui.component.LyricsView
import com.maxrave.simpmusic.ui.component.PlayPauseButton
import com.maxrave.simpmusic.ui.component.lyrics.ShareLyricsSheet
import com.maxrave.simpmusic.ui.component.lyrics.toShareLyricsLines
import com.maxrave.simpmusic.ui.component.rememberHolderPainter
import com.maxrave.simpmusic.ui.icon.Forward5
import com.maxrave.simpmusic.ui.icon.Fullscreen
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
import org.jetbrains.compose.resources.stringResource
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.artists
import simpmusic.composeapp.generated.resources.description
import simpmusic.composeapp.generated.resources.like_and_dislike
import simpmusic.composeapp.generated.resources.line_synced
import simpmusic.composeapp.generated.resources.lyrics
import simpmusic.composeapp.generated.resources.lyrics_provider_betterlyrics
import simpmusic.composeapp.generated.resources.lyrics_provider_lrc
import simpmusic.composeapp.generated.resources.lyrics_provider_simpmusic
import simpmusic.composeapp.generated.resources.lyrics_provider_youtube
import simpmusic.composeapp.generated.resources.offline_mode
import simpmusic.composeapp.generated.resources.published_at
import simpmusic.composeapp.generated.resources.rate_lyrics
import simpmusic.composeapp.generated.resources.rich_synced
import simpmusic.composeapp.generated.resources.share_lyrics
import simpmusic.composeapp.generated.resources.show
import simpmusic.composeapp.generated.resources.spotify_lyrics_provider
import simpmusic.composeapp.generated.resources.unsynced
import simpmusic.composeapp.generated.resources.view_count

// Shared shape for the below-the-fold tonal cards.
private val ExpressiveCardShape = RoundedCornerShape(20.dp)

// Artwork card corner radius — the signature shape of the "Tonal pills" design.
private val ArtworkCardShape = RoundedCornerShape(28.dp)

/**
 * One pager page: a rounded square card (width = screen − 40dp, 1:1, 28dp corners).
 *
 * Current page: live artwork (feeds the palette via [NowPlayingContentActions.onArtworkBitmap])
 * and the inline video player with the Classic overlay controls when watch-video mode is
 * active. When Spotify canvas data is present the canvas plays FULLSCREEN as the bottom
 * layer of the page and the card hides (alpha 0, layout slot kept) — Classic's canvas mode
 * verbatim; a tap on the page then toggles the controls via
 * [NowPlayingContentActions.onToggleControls]. Adjacent pages: the track's static thumbnail
 * as a card.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ExpressiveArtworkCardPage(
    state: NowPlayingContentState,
    actions: NowPlayingContentActions,
    page: Int,
    topAppBarHeightDp: Int,
    middleLayoutPaddingDp: Int,
    showHideFullscreenOverlay: Boolean,
    onToggleFullscreenOverlay: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val pageTrack = state.artworkQueue.getOrNull(page)
    val isCurrentArtworkPage = page == state.currentOrderIndex
    val pageHasCanvas = isCurrentArtworkPage && state.screenData.canvasData != null
    // While a video plays, the card frame itself shrinks to the video's 16:9 — no letterbox
    // bands inside a square card. Every page shares the ratio so the pager height matches the
    // measuring spacer in the content column (which uses the same condition).
    val cardAspectRatio = if (state.screenData.isVideo && state.shouldShowVideo) 16f / 9 else 1f

    Box(
        contentAlignment = Alignment.Center,
        modifier =
            Modifier
                .fillMaxSize()
                // Prevent the canvas video (9:16 aspect, can be wider than the
                // page) and any other content from bleeding into adjacent pages.
                .clipToBounds()
                // Tap toggles controls only when the canvas is covering this page;
                // otherwise no-op — Classic verbatim.
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
        // ── Fullscreen canvas backdrop (current track + canvas data) — Classic verbatim ──
        // The canvas is the bottom layer of the page: a 9:16 video keeps its full height and
        // is center-cropped by the page clip on mobile; Desktop keeps the plain modifier.
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
            // Bottom gradient overlay — different intensity per state (Classic verbatim):
            // - Focus: full-height heavy gradient (controls readability)
            // - Unfocus: compact dark coverage at the very bottom only, just enough for
            //   the metadata overlay, letting more canvas show through.
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

        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.height(topAppBarHeightDp.dp))
            Spacer(
                modifier =
                    Modifier
                        .animateContentSize()
                        .height(middleLayoutPaddingDp.dp)
                        .fillMaxWidth(),
            )
            // While the canvas covers the page the card hides (alpha 0) but keeps its
            // layout slot occupied — Classic's exact approach, so nothing below moves.
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .alpha(if (pageHasCanvas) 0f else 1f)
                        .aspectRatio(cardAspectRatio)
                        .clip(ArtworkCardShape)
                        .background(colorScheme.surfaceContainer),
            ) {
                if (isCurrentArtworkPage) {
                    // Live artwork — kept composed even under canvas/video (alpha 0) so
                    // onSuccess keeps feeding the palette that drives the whole scheme.
                    // The artwork URL that is actually loading. `maxresdefault.jpg` — the fallback
                    // artworkUri many video tracks carry — only EXISTS for videos with an HD
                    // thumbnail; everything else 404s, onSuccess never fires, the palette never
                    // generates, and the whole M3E scheme sits on the app-seed cyan for a grey
                    // song. On error we retry once with `hqdefault.jpg`, which YouTube guarantees
                    // for every video. Song artwork (googleusercontent) never matches the replace,
                    // so this is a no-op for it.
                    var artworkUrl by remember(state.screenData.thumbnailURL) {
                        mutableStateOf(state.screenData.thumbnailURL)
                    }
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
                                .then(
                                    if (state.screenData.isVideo) {
                                        Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(16f / 9)
                                    } else {
                                        Modifier.fillMaxSize()
                                    },
                                ).alpha(
                                    if (pageHasCanvas || (state.screenData.isVideo && state.shouldShowVideo)) 0f else 1f,
                                ),
                    )

                    // Inline video player — same condition as Classic, rendered inside the card.
                    // Fully qualified: the outer Column's ColumnScope.AnimatedVisibility member
                    // otherwise shadows the top-level overload (same workaround as Classic).
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
                                            onClick = onToggleFullscreenOverlay,
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
                                                modifier = Modifier.align(Alignment.TopEnd),
                                            ) {
                                                Icon(
                                                    imageVector = SimpIcons.Fullscreen,
                                                    contentDescription = "",
                                                    // Over-video control, not a semantic surface
                                                    // foreground — stays white like Classic.
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
                                                    modifier = Modifier.align(Alignment.BottomEnd),
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
                    // Adjacent page — static thumbnail card.
                    val staticThumb =
                        pageTrack.thumbnails
                            ?.maxByOrNull { it.width * it.height }
                            ?.url
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
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}

/**
 * Below-the-fold content of the M3 Expressive style: lyrics, artist and description cards.
 * All logic (visibility conditions, vote gating, timestamp seeking, link opening) is ported
 * verbatim from [NowPlayingContentSpotify]; only the containers are tonal.
 */
@Composable
internal fun ExpressiveBelowTheFold(
    state: NowPlayingContentState,
    actions: NowPlayingContentActions,
) {
    val colorScheme = MaterialTheme.colorScheme
    val localDensity = LocalDensity.current
    val uriHandler = LocalUriHandler.current
    var showShareLyricsSheet by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.padding(horizontal = 20.dp)) {
        // Lyrics card
        AnimatedVisibility(
            visible = state.screenData.lyricsData != null,
            modifier = Modifier.padding(top = 10.dp),
        ) {
            Surface(
                shape = ExpressiveCardShape,
                color = colorScheme.surfaceContainer,
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
                        // Vote button — only when the lyrics or the translation come from SimpMusic
                        // Lyrics. The rule itself lives on the shared contract (canVote), so a style
                        // cannot ship without it the way the Apple Music tab did.
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
                                        tint = colorScheme.onSurfaceVariant,
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
                                    tint = colorScheme.onSurfaceVariant,
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
                                backgroundColor = colorScheme.surfaceContainer,
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
        // Artist card
        AnimatedVisibility(visible = state.screenData.songInfoData != null) {
            Surface(
                onClick = {
                    actions.onNavigateToArtist()
                },
                shape = ExpressiveCardShape,
                color = colorScheme.surfaceContainer,
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Artwork occupies the top of the card; only the section label sits on
                    // top of it. Name and subscriber count live below on the card surface.
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
                            modifier = Modifier.fillMaxSize(),
                        )
                        // Scrim behind the label — artist photos are often bright at the top.
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
                            // Over the photo scrim, not on a semantic surface.
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
        // Description card
        AnimatedVisibility(visible = state.screenData.songInfoData != null) {
            Surface(
                shape = ExpressiveCardShape,
                color = colorScheme.surfaceContainer,
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
        }
        Spacer(modifier = Modifier.height(10.dp))
        Spacer(
            modifier =
                Modifier.height(
                    with(localDensity) { WindowInsets.systemBars.getBottom(localDensity).toDp() },
                ),
        )
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

/**
 * Collapsed sticky toolbar shown once the seek bar scrolls out of view — same mechanism as
 * [NowPlayingContentSpotify]'s toolbar ([NowPlayingContentState.shouldShowToolbar] driven by
 * the isElementVisible callback), restyled tonal: surfaceContainerHigh background and a thin
 * primary progress line.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ExpressiveCollapsedToolbar(
    state: NowPlayingContentState,
    actions: NowPlayingContentActions,
) {
    val colorScheme = MaterialTheme.colorScheme
    val localDensity = LocalDensity.current
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
                    containerColor = colorScheme.surfaceContainerHigh,
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
                                                    .padding(end = 4.dp),
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
                                    color = colorScheme.onSurfaceVariant,
                                    strokeWidth = 3.dp,
                                )
                            }
                        } else {
                            PlayPauseButton(
                                isPlaying = state.controllerState.isPlaying,
                                modifier = Modifier.size(48.dp),
                                tint = colorScheme.onSurface,
                            ) {
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
                        color = colorScheme.primary,
                        trackColor = colorScheme.surfaceContainerHighest,
                        strokeCap = StrokeCap.Round,
                        drawStopIndicator = {},
                    )
                }
            }
        }
    }
}
