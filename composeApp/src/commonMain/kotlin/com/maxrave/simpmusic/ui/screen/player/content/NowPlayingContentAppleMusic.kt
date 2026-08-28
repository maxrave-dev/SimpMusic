package com.maxrave.simpmusic.ui.screen.player.content

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.kyant.backdrop.highlight.Highlight
import com.maxrave.common.Config.MAIN_PLAYER
import com.maxrave.domain.mediaservice.handler.RepeatState
import com.maxrave.simpmusic.Platform
import com.maxrave.simpmusic.expect.ui.DeviceVolumeController
import com.maxrave.simpmusic.expect.ui.MediaPlayerView
import com.maxrave.simpmusic.expect.ui.MediaPlayerViewWithSubtitle
import com.maxrave.simpmusic.expect.ui.layerBackdrop
import com.maxrave.simpmusic.expect.ui.rememberBackdrop
import com.maxrave.simpmusic.expect.ui.rememberDeviceVolumeController
import com.maxrave.simpmusic.expect.ui.toImageBitmap
import com.maxrave.simpmusic.extension.getScreenSizeInfo
import com.maxrave.simpmusic.extension.smoothScrimBrush
import com.maxrave.simpmusic.getPlatform
import com.maxrave.simpmusic.ui.component.ExplicitBadge
import com.maxrave.simpmusic.ui.component.LiquidGlassIconButton
import com.maxrave.simpmusic.ui.component.rememberHolderPainter
import com.maxrave.simpmusic.ui.screen.player.content.applemusic.AppleMusicBottomCluster
import com.maxrave.simpmusic.ui.screen.player.content.applemusic.AppleMusicHeaderActions
import com.maxrave.simpmusic.ui.screen.player.content.applemusic.AppleMusicLyricsView
import com.maxrave.simpmusic.ui.screen.player.content.applemusic.AppleMusicQueueView
import com.maxrave.simpmusic.ui.screen.player.content.applemusic.AppleMusicTypography
import com.maxrave.simpmusic.ui.icon.Forward5
import com.maxrave.simpmusic.ui.icon.Fullscreen
import com.maxrave.simpmusic.ui.icon.Replay5
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.icon.Subtitles
import com.maxrave.simpmusic.ui.icon.SubtitlesOff
import com.maxrave.simpmusic.ui.screen.player.content.applemusic.AppleMusicView
import com.maxrave.simpmusic.ui.screen.player.content.applemusic.appleMusicGradientColorAt
import com.maxrave.simpmusic.ui.screen.player.content.applemusic.appleMusicVerticalFadeEdges
import com.maxrave.simpmusic.ui.screen.player.content.applemusic.rememberAppleMusicTypography
import com.maxrave.simpmusic.ui.theme.seed
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.SharedViewModel
import com.maxrave.simpmusic.viewModel.UIEvent
import kotlinx.coroutines.delay
import org.koin.compose.koinInject

/**
 * The Apple Music Now Playing style: a style-internal dock (Lyrics · Cast · Queue) swaps the
 * BODY between three full-screen views instead of scrolling a single column, à la
 * [NowPlayingContentSpotify]/[NowPlayingContentM3Expressive]. The page background is the
 * artwork's own tint (a vertical gradient), not a Material scheme — see the color-system note on
 * [AppleMusicMainView] for the canvas/video exception.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NowPlayingContentAppleMusic(
    state: NowPlayingContentState,
    actions: NowPlayingContentActions,
) {
    // Seeded from the view model, not from MAIN: this player lives in a ModalBottomSheet, so
    // dismissing it disposes the tree and rememberSaveable dies with it. rememberSaveable is still
    // the right holder WITHIN a session (it survives rotation without a round trip), the view model
    // just supplies where the user was last.
    val sharedViewModel: SharedViewModel = koinInject()
    var viewState by rememberSaveable {
        mutableStateOf(
            sharedViewModel.lastPlayerViewTab.value
                ?.let { saved -> AppleMusicView.entries.firstOrNull { it.name == saved } }
                ?: AppleMusicView.MAIN,
        )
    }
    LaunchedEffect(viewState) { sharedViewModel.setLastPlayerViewTab(viewState.name) }

    // Lyrics can disappear mid-session (provider swap gone offline, a track that simply has none)
    // — fall back to MAIN rather than stranding the user on an empty body, matching the dock's own
    // disabled-Lyrics-button gate.
    //
    // The wait is the whole point. Every track change rebuilds NowPlayingScreenData from scratch
    // with lyricsData = null and only THEN fetches (SharedViewModel.kt:342), so "null" is the
    // normal state of every song for as long as the request takes. Reacting to it immediately —
    // which is what this did — threw the user out of the lyrics tab on every single skip, even
    // when the incoming track had lyrics arriving a moment later. Lyrics landing restarts this
    // effect and cancels the wait, so the fallback only ever fires for a track that really has
    // none.
    LaunchedEffect(state.screenData.lyricsData, viewState) {
        if (viewState != AppleMusicView.LYRICS || state.screenData.lyricsData != null) return@LaunchedEffect
        delay(LYRICS_ABSENCE_GRACE_MS)
        viewState = AppleMusicView.MAIN
    }

    // This style has no scroll and no collapsed toolbar (unlike Classic/M3E); park the shared
    // toolbar-visibility flag at false so a style switch mid-session can't leave it stuck shown.
    LaunchedEffect(Unit) {
        actions.onToolbarVisibilityChange(false)
    }

    // The artwork bitmap feeds BOTH the frosted backdrop below and the palette every colour on
    // this page is derived from. The only thing that ever supplied it is the AsyncImage inside the
    // artwork pager, which lives in MAIN — and the Crossfade composes exactly one body, so on
    // QUEUE or LYRICS that pager does not exist. Changing track there fed nothing, and the page
    // fell back to a flat gradient.
    //
    // The loader below sits OUTSIDE the Crossfade so it covers every body, and it is an AsyncImage
    // rather than an imperative ImageLoader.execute(): the pager's AsyncImage demonstrably loads
    // this exact url while the execute() call did not, so this uses the path already proven to
    // work rather than a second one that has to be kept working.
    var backdropUrl by remember(state.screenData.thumbnailURL) { mutableStateOf(state.screenData.thumbnailURL) }

    val paletteColor = state.startColor.value
    val seedColor = if (paletteColor == Color.Black) seed else paletteColor
    val activePillContainer = remember(seedColor) { lerp(seedColor, Color.White, 0.75f) }
    val activePillContent = remember(seedColor) { lerp(seedColor, Color.Black, 0.6f) }

    val showCanvasBackdrop =
        viewState == AppleMusicView.MAIN &&
            (state.screenData.canvasData != null || (state.screenData.isVideo && state.shouldShowVideo))
    val isVideoBackdropTop = showCanvasBackdrop && state.screenData.canvasData == null

    // The approved mock's page gradient is THREE stops — a clearly-tinted top, ~55%-darkened by
    // mid-page (48%), warm near-black at the bottom. The first cut's two stops to near-black read
    // as a flat black page on any dark artwork (first device screenshots).
    val backdropBrush =
        remember(seedColor) {
            Brush.verticalGradient(
                0f to appleMusicGradientColorAt(seedColor, 0f),
                0.48f to appleMusicGradientColorAt(seedColor, 0.48f),
                1f to appleMusicGradientColorAt(seedColor, 1f),
            )
        }

    val deviceVolumeController = rememberDeviceVolumeController()
    val typography = rememberAppleMusicTypography()
    val localDensity = LocalDensity.current

    // The canvas page is black and every other state is the artwork gradient — but the swap must
    // NOT be instant. viewState flips the moment a tab is tapped, while the Crossfade below still
    // spends 300ms fading MAIN out: a hard swap repaints the page bright underneath a canvas that
    // is still on screen, which is the flicker when leaving MAIN for Queue/Lyrics and again on the
    // way back. Fading the black layer on the SAME 300ms curve keeps the two in step.
    val canvasBackdropAlpha by animateFloatAsState(
        targetValue = if (showCanvasBackdrop && !isVideoBackdropTop) 1f else 0f,
        animationSpec = tween(300),
        label = "appleMusicCanvasBackdrop",
    )

    // Backdrop source for the Desktop dismiss button below. The glass layers MUST be a sibling of
    // the button, never its parent: nesting the button inside the source is the render-feedback
    // loop that crashes the RuntimeShader.
    val panelBackdrop = rememberBackdrop(Color.Black)

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        Box(modifier = Modifier.matchParentSize().layerBackdrop(panelBackdrop)) {
            // Apple frosts the COVER ART into the page background — the colour and the soft blotches
            // of the artwork stay visible through it. A flat tinted gradient, which is what this used
            // to be, gets the hue right and loses everything else: the page reads as a solid colour
            // swatch rather than as the record it belongs to.
            //
            // Loaded straight from the url by AsyncImage rather than through the screen state's
            // decoded bitmap. The background IS an image, so there is no reason to route it through a
            // bitmap someone else has to remember to fill in — which is exactly what broke: the only
            // thing feeding that bitmap was the artwork pager inside MAIN, so on QUEUE or LYRICS a
            // track change left it null and the page fell back to a bare gradient.
            //
            // The palette still needs a bitmap, and it comes off this same load. One source, so the
            // frosted art and the tint over it cannot end up belonging to different songs.
            //
            // The heavy blur radius is safe because the whole style is gated behind Android 12 for
            // exactly this reason (isLyricsBlurSupported), and Crop + fillMaxSize means the artwork is
            // scaled far past its own resolution — at this blur that costs nothing visually.
            if (!backdropUrl.isNullOrBlank()) {
                AsyncImage(
                    model =
                        ImageRequest
                            .Builder(LocalPlatformContext.current)
                            .data(backdropUrl)
                            .diskCachePolicy(CachePolicy.ENABLED)
                            .diskCacheKey(backdropUrl + "BIGGER")
                            .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    onSuccess = { actions.onArtworkBitmap(it.result.image.toImageBitmap()) },
                    // Same fallback the artwork pager carries: maxresdefault is missing for plenty of
                    // videos, and without this the page would simply stay black.
                    onError = {
                        val fallback = backdropUrl?.replace("maxresdefault", "hqdefault")
                        if (fallback != null && fallback != backdropUrl) backdropUrl = fallback
                    },
                    modifier = Modifier.fillMaxSize().blur(BACKDROP_BLUR_RADIUS, BlurredEdgeTreatment.Unbounded),
                )
            }
            // The tint still rides on top, but as a translucent wash rather than the whole background:
            // it keeps the vertical darkening that makes the controls readable at the bottom, while the
            // frosted artwork shows through it.
            Box(modifier = Modifier.fillMaxSize().alpha(BACKDROP_TINT_ALPHA).background(backdropBrush))
            // Flat black only for a CANVAS (it fills the screen). A video letterboxes, so a black page
            // turns the bars above and below it into dead black slabs — keep the artwork-tinted
            // gradient there.
            if (canvasBackdropAlpha > 0f) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = canvasBackdropAlpha)))
            }
        }
        Crossfade(targetState = viewState, animationSpec = tween(300), label = "appleMusicView") { view ->
            when (view) {
                AppleMusicView.MAIN ->
                    AppleMusicMainView(
                        state = state,
                        actions = actions,
                        typography = typography,
                        viewState = view,
                        onSelectView = { viewState = it },
                        seedColor = seedColor,
                        activePillContainer = activePillContainer,
                        activePillContent = activePillContent,
                        deviceVolumeController = deviceVolumeController,
                    )

                AppleMusicView.LYRICS ->
                    AppleMusicLyricsView(
                        state = state,
                        actions = actions,
                        typography = typography,
                        viewState = view,
                        onSelectView = { viewState = it },
                        activePillContainer = activePillContainer,
                        activePillContent = activePillContent,
                        deviceVolumeController = deviceVolumeController,
                    )

                AppleMusicView.QUEUE ->
                    AppleMusicQueueView(
                        state = state,
                        actions = actions,
                        typography = typography,
                        viewState = view,
                        onSelectView = { viewState = it },
                        activePillContainer = activePillContainer,
                        activePillContent = activePillContent,
                        deviceVolumeController = deviceVolumeController,
                    )
            }
        }

        // How the page is dismissed, and it differs by platform because the page itself does.
        //
        // On Android this is a bottom sheet, so a grabber is the native way out and tapping it
        // dismisses. On Desktop the same content is a side panel that never slides anywhere — a
        // grabber there is a handle for a gesture that does not exist, which is why this style
        // shipped with no visible way to close the panel at all while Classic and M3 Expressive
        // both draw state.dismissIcon in their top bar.
        // MAIN only: Queue and Lyrics each have their own header, and a floating button over
        // those reads as belonging to the list rather than to the panel.
        if (getPlatform() == Platform.Desktop) {
            if (viewState == AppleMusicView.MAIN) {
                LiquidGlassIconButton(
                    backdrop = panelBackdrop,
                    imageVector = state.dismissIcon,
                    shape = RoundedCornerShape(24.dp),
                    // Same as AnalyticsScreen's back button: a 48dp circle catches only a short arc
                    // of the default directional sweep and reads as rimless. 1.dp is the smallest
                    // step that stays visible without looking like a plain border.
                    highlight = Highlight(width = 1.dp),
                    modifier =
                        Modifier
                            .align(Alignment.TopStart)
                            .padding(12.dp)
                            .size(48.dp),
                    onClick = { actions.onDismiss() },
                )
            }
        } else {
            // Grabber. It sits ABOVE the Crossfade, not inside a view, so it stays put across a tab
            // switch instead of fading with the body — and so Queue/Lyrics get it too. The shell's
            // ModalBottomSheet passes dragHandle = {} for every style, so nothing above this draws one.
            Box(
                modifier =
                    Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = with(localDensity) { WindowInsets.statusBars.getTop(localDensity).toDp() })
                        .size(width = 64.dp, height = 28.dp)
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

/**
 * MAIN: full-bleed artwork pager (top ~60%, fading into the page gradient) with the shell's
 * artwork-pager sync, a title row, and the shared bottom cluster.
 *
 * Canvas/video mode (design decision A — full screen): when [NowPlayingScreenData.canvasData] is
 * set or the track is a shown video, the CURRENT page's canvas/video fills the entire pager
 * (bottom layer, page background goes black) and this same title-row-plus-cluster layout floats
 * on top with a scrim, auto-hiding via [NowPlayingContentState.showControlLayout] exactly like
 * [NowPlayingContentM3Expressive] does for its info block. A tap re-shows hidden controls; a tap
 * on an already-shown VIDEO (not canvas) enters the existing fullscreen video route instead of
 * rebuilding Classic's ±5s/subtitle overlay here.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppleMusicMainView(
    state: NowPlayingContentState,
    actions: NowPlayingContentActions,
    typography: AppleMusicTypography,
    viewState: AppleMusicView,
    onSelectView: (AppleMusicView) -> Unit,
    seedColor: Color,
    activePillContainer: Color,
    activePillContent: Color,
    deviceVolumeController: DeviceVolumeController?,
) {
    val screenInfo = getScreenSizeInfo()
    val localDensity = LocalDensity.current
    val isRepeatOne = state.controllerState.repeatState is RepeatState.One

    val showCanvasBackdrop =
        state.screenData.canvasData != null || (state.screenData.isVideo && state.shouldShowVideo)
    val isVideoBackdrop = showCanvasBackdrop && state.screenData.canvasData == null

    // Same fade/half-blended-frame fix M3E uses: fast fade-in, relaxed fade-out.
    val controlsAlpha by animateFloatAsState(
        targetValue = if (state.showControlLayout) 1f else 0f,
        animationSpec = tween(durationMillis = if (state.showControlLayout) 180 else 500, easing = LinearEasing),
        label = "appleMusicControlsAlpha",
    )

    // Over-video controls, exactly like Classic/M3E: hidden by default, a tap on the video shows
    // them, and they auto-hide again after 3s. Subtitles default on, toggled from that overlay.
    var showVideoOverlay by rememberSaveable { mutableStateOf(false) }
    var showSubtitle by rememberSaveable { mutableStateOf(true) }
    LaunchedEffect(showVideoOverlay) {
        if (showVideoOverlay) {
            delay(3000)
            showVideoOverlay = false
        }
    }

    // The artwork zone is whatever's left above the measured title-row+cluster block, so the
    // whole MAIN view always totals exactly one screen height — no scroll, per spec.
    // remember, not rememberSaveable: a measured height must not survive a rotation, or a portrait
    // cluster height is paired with a landscape screen height for a frame. Seeded near its real
    // value so the FIRST frame doesn't draw a full-screen artwork that then snaps up.
    var bottomContentHeightDp by remember { mutableIntStateOf(330) }
    val artworkZoneHeightDp = (screenInfo.hDP - bottomContentHeightDp).coerceAtLeast(200)

    Box(modifier = Modifier.fillMaxSize()) {
        HorizontalPager(
            state = state.artworkPagerState,
            modifier = Modifier.fillMaxSize(),
            beyondViewportPageCount = 1,
            userScrollEnabled = !isRepeatOne && state.artworkQueue.isNotEmpty(),
            key = { idx ->
                val vid = state.artworkQueue.getOrNull(idx)?.videoId.orEmpty()
                "appleMusicArtwork_${vid}_$idx"
            },
        ) { page ->
            AppleMusicArtworkPage(
                state = state,
                actions = actions,
                localDensity = localDensity,
                page = page,
                artworkZoneHeightDp = artworkZoneHeightDp,
                isVideoBackdrop = isVideoBackdrop,
                showVideoOverlay = showVideoOverlay,
                onToggleVideoOverlay = { showVideoOverlay = !showVideoOverlay },
                showSubtitle = showSubtitle,
                onToggleSubtitle = { showSubtitle = !showSubtitle },
            )
        }

        // Top scrim, CANVAS ONLY — same rule the flat black backdrop above follows: a canvas
        // replaces the whole page, so the status bar and grabber would otherwise float on raw
        // picture; a video letterboxes and the strip around it is still the artwork gradient,
        // which is exactly what a plain-artwork track's controls already sit on. Painting these
        // over a video only dimmed the picture — measured on a 480x830 panel, the top 25% of the
        // 16:9 frame under up to 0.20 black and the bottom 20% under up to 0.22.
        //
        // Rides controlsAlpha, like the scrim below it. A scrim is an accessory of the CONTROLS,
        // not of the canvas: gating it on showCanvasBackdrop alone left both scrims painted at full
        // strength after the controls auto-hid, so 82% of the screen height stayed covered — 0.55
        // black down to y=22%, then 0.45 at y=53% ramping to 0.97 at the bottom — with nothing left
        // underneath them to make readable. That is the whole of "đen thui cả canvas". The idle
        // overlay that replaces the controls carries its own page-tint scrim, so the bottom stays
        // readable without these.
        if (showCanvasBackdrop && !isVideoBackdrop && controlsAlpha > 0f) {
            Box(
                modifier =
                    Modifier
                        .align(Alignment.TopCenter)
                        .alpha(controlsAlpha)
                        .fillMaxWidth()
                        .fillMaxHeight(0.22f)
                        .background(
                            Brush.verticalGradient(
                                0f to Color.Black.copy(alpha = 0.55f),
                                1f to Color.Transparent,
                            ),
                        ),
            )
        }

        // Owner's spec, verbatim: a BLACK→TRANSPARENT gradient under the title row + cluster
        // while a canvas plays — solid black at the very bottom, fading out upward.
        // Height is a FRACTION of this Box, never a dp computed from screenInfo: a zero/stale
        // hDP silently collapsed this scrim to nothing, which is why it kept "not existing".
        // Canvas only, for the reason spelled out on the top scrim.
        if (showCanvasBackdrop && !isVideoBackdrop && controlsAlpha > 0f) {
            Box(
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .alpha(controlsAlpha)
                        .fillMaxWidth()
                        .fillMaxHeight(0.6f)
                        .background(
                            Brush.verticalGradient(
                                // Ramps harder early: at the old stops the TITLE row sat at only
                                // ~56% black over the video while the lower controls had 76-92%.
                                0f to Color.Transparent,
                                0.22f to Color.Black.copy(alpha = 0.45f),
                                0.50f to Color.Black.copy(alpha = 0.82f),
                                1f to Color.Black.copy(alpha = 0.97f),
                            ),
                        ),
            )
        }

        // Box, not Column: the real content and the idle overlay below must OVERLAP (Z-stack),
        // not lay out one after another — matches NowPlayingContentM3Expressive's exact structure
        // for this same canvas-unfocused-overlay pattern.
        Box(
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
        ) {
            Column(
                modifier =
                    Modifier
                        .alpha(controlsAlpha)
                        .onGloballyPositioned { coords ->
                            bottomContentHeightDp =
                                with(localDensity) { coords.size.height.toDp().value.toInt() }
                        },
            ) {
                Spacer(modifier = Modifier.height(20.dp))
                AppleMusicMainTitleRow(state = state, actions = actions, typography = typography)
                Spacer(modifier = Modifier.height(16.dp))
                AppleMusicBottomCluster(
                    state = state,
                    actions = actions,
                    typography = typography,
                    viewState = viewState,
                    onSelectView = onSelectView,
                    activePillContainer = activePillContainer,
                    activePillContent = activePillContent,
                    deviceVolumeController = deviceVolumeController,
                )
            }
            // Idle overlay — replaces the (now invisible) title row + cluster while canvas/video
            // controls are auto-hidden, mirroring M3E's canvas-unfocused overlay verbatim.
            if (showCanvasBackdrop) {
                AnimatedVisibility(
                    visible = !state.showControlLayout,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(bottomContentHeightDp.dp)
                                .clickable(
                                    onClick = { actions.onToggleControls() },
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() },
                                ),
                        contentAlignment = Alignment.BottomStart,
                    ) {
                        // The page's OWN colour, not black. This scrim is what the artwork fades
                        // INTO, so it has to be the same tone the page is painted with below it —
                        // appleMusicGradientColorAt(seed, 1f), i.e. the thumbnail's colour darkened,
                        // exactly like the bottom stop of backdropBrush. Fading to black instead
                        // laid a dark slab over a tinted page and left a visible seam where the two
                        // met, which read as "black smudge into transparent".
                        //
                        // Fully opaque at the bottom for the same reason: at 0.85 the artwork still
                        // showed through the last few pixels and tinted the seam a different colour
                        // than the page continuing beneath it.
                        val pageScrimColor = appleMusicGradientColorAt(seedColor, 1f)
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxSize()
                                    .background(
                                        smoothScrimBrush(
                                            from = pageScrimColor.copy(alpha = 0f),
                                            to = pageScrimColor,
                                        ),
                                    ),
                        )
                        Column(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        bottom =
                                            with(localDensity) {
                                                WindowInsets.systemBars.getBottom(localDensity).toDp()
                                            } + 16.dp,
                                    ),
                        ) {
                            AnimatedVisibility(
                                visible = state.currentLyricLineIndex > -1,
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically(),
                            ) {
                                val lineText =
                                    state.screenData.lyricsData
                                        ?.lyrics
                                        ?.lines
                                        ?.getOrNull(state.currentLyricLineIndex)
                                        ?.words
                                        ?.stripRichSyncTimestamps()
                                if (!lineText.isNullOrBlank()) {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Text(
                                            text = lineText,
                                            style = typography.idleLyric,
                                            maxLines = 1,
                                            modifier =
                                                Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 20.dp, vertical = 2.dp)
                                                    .basicMarquee(iterations = Int.MAX_VALUE, animationMode = MarqueeAnimationMode.Immediately)
                                                    .focusable(),
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
                                                text = translatedLineText,
                                                style = typography.idleTranslated,
                                                maxLines = 1,
                                                modifier =
                                                    Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 20.dp, vertical = 2.dp)
                                                        .basicMarquee(iterations = Int.MAX_VALUE, animationMode = MarqueeAnimationMode.Immediately)
                                                        .focusable(),
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            // The DEFAULT style shows its full NowPlayingTrackInfoRow here — 55dp thumbnail
                            // and the ⊕ / ☆ / ⋯ actions. This is that row, not a stripped-down copy of it.
                            // It types with the COMPACT slots, not the main ones: this is a track header
                            // floating over a canvas, the same job the Lyrics/Queue header does.
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                            ) {
                                AsyncImage(
                                    model =
                                        ImageRequest
                                            .Builder(LocalPlatformContext.current)
                                            .data(state.screenData.thumbnailURL)
                                            .diskCachePolicy(CachePolicy.ENABLED)
                                            .diskCacheKey(state.screenData.thumbnailURL)
                                            .crossfade(300)
                                            .build(),
                                    placeholder = rememberHolderPainter(),
                                    error = rememberHolderPainter(),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(55.dp).clip(RoundedCornerShape(4.dp)),
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = state.screenData.nowPlayingTitle,
                                        style = typography.compactTitle,
                                        maxLines = 1,
                                        modifier =
                                            Modifier
                                                .fillMaxWidth()
                                                .basicMarquee(iterations = Int.MAX_VALUE, animationMode = MarqueeAnimationMode.Immediately)
                                                .focusable(),
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = state.screenData.artistName,
                                        style = typography.compactArtist,
                                        maxLines = 1,
                                        modifier =
                                            Modifier
                                                .fillMaxWidth()
                                                .basicMarquee(iterations = Int.MAX_VALUE, animationMode = MarqueeAnimationMode.Immediately)
                                                .focusable(),
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                AppleMusicHeaderActions(state = state, actions = actions)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppleMusicMainTitleRow(
    state: NowPlayingContentState,
    actions: NowPlayingContentActions,
    typography: AppleMusicTypography,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = state.screenData.nowPlayingTitle,
                style = typography.mainTitle,
                maxLines = 1,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .basicMarquee(iterations = Int.MAX_VALUE, animationMode = MarqueeAnimationMode.Immediately)
                        .focusable(),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (state.screenData.isExplicit) {
                    ExplicitBadge(modifier = Modifier.size(20.dp).padding(end = 4.dp))
                }
                Text(
                    text = state.screenData.artistName,
                    style = typography.mainArtist,
                    maxLines = 1,
                    modifier =
                        Modifier
                            .basicMarquee(iterations = Int.MAX_VALUE, animationMode = MarqueeAnimationMode.Immediately)
                            .focusable()
                            .clickable { actions.onNavigateToArtist() },
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        AppleMusicHeaderActions(state = state, actions = actions)
    }
}

/**
 * One pager page. Current page: the live artwork stays composed (alpha 0 under canvas/video) so
 * [NowPlayingContentActions.onArtworkBitmap] keeps feeding the palette that drives the page
 * background even while a canvas/video is the visible backdrop — otherwise the background would
 * go stale on the next track if it also opens on a canvas. Adjacent pages: a static thumbnail,
 * bounded the same way non-canvas artwork is.
 */
@Composable
private fun AppleMusicArtworkPage(
    state: NowPlayingContentState,
    actions: NowPlayingContentActions,
    localDensity: Density,
    page: Int,
    artworkZoneHeightDp: Int,
    isVideoBackdrop: Boolean,
    showVideoOverlay: Boolean,
    onToggleVideoOverlay: () -> Unit,
    showSubtitle: Boolean,
    onToggleSubtitle: () -> Unit,
) {
    val pageTrack = state.artworkQueue.getOrNull(page)
    val isCurrentPage = page == state.currentOrderIndex
    val pageShowsCanvasOrVideo =
        isCurrentPage && (state.screenData.canvasData != null || (state.screenData.isVideo && state.shouldShowVideo))

    Box(modifier = Modifier.fillMaxSize()) {
        if (isCurrentPage) {
            var artworkUrl by remember(state.screenData.thumbnailURL) { mutableStateOf(state.screenData.thumbnailURL) }
            Box(
                modifier =
                    Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .height(artworkZoneHeightDp.dp),
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
                    onSuccess = { actions.onArtworkBitmap(it.result.image.toImageBitmap()) },
                    onError = {
                        val fallback = artworkUrl?.replace("maxresdefault", "hqdefault")
                        if (fallback != null && fallback != artworkUrl) artworkUrl = fallback
                    },
                    contentScale = ContentScale.Crop,
                    placeholder = rememberHolderPainter(),
                    error = rememberHolderPainter(),
                    // The artwork DISSOLVES (alpha mask) instead of being covered by a colour
                    // overlay: that overlay had to land on exactly the page gradient's colour at
                    // that Y, and any drift drew a hard horizontal line across the screen.
                    // Masking lets the real background show through — nothing left to match.
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .alpha(if (pageShowsCanvasOrVideo) 0f else 1f)
                            .appleMusicVerticalFadeEdges(topFade = 0.dp, bottomFade = 300.dp),
                )
            }
            if (pageShowsCanvasOrVideo) {
                if (isVideoBackdrop) {
                    // Centre the video in the region ABOVE the controls (top → cluster), not in
                    // the whole screen: screen-centred, half of a 16:9 video sat behind the
                    // control cluster and its subtitles landed on the dock.
                    // fillMaxWidth (never fillMaxSize) leaves the height free so the surface's
                    // own .aspectRatio(videoRatio) still applies — that is what keeps it from
                    // being stretched.
                    Box(
                        modifier =
                            Modifier
                                .align(Alignment.TopCenter)
                                .fillMaxWidth()
                                .height(artworkZoneHeightDp.dp)
                                .clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() },
                                ) { onToggleVideoOverlay() },
                        contentAlignment = Alignment.Center,
                    ) {
                        // THE VIDEO FRAME. Everything over-video — the surface, the subtitle and
                        // the whole control overlay — is anchored to THIS box, so the fullscreen
                        // button sits on the video's own top-right corner and the subtitle button
                        // on its bottom-right, instead of being flung to the corners of the much
                        // taller zone (fullscreen ended up under the status bar, subtitles far
                        // below the picture).
                        Box(modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)) {
                        MediaPlayerViewWithSubtitle(
                            playerName = MAIN_PLAYER,
                            // fillMaxWidth, never fillMaxSize: a free height lets the surface's
                            // own .aspectRatio(videoRatio) apply, which is what stops it being
                            // stretched.
                            modifier = Modifier.fillMaxWidth().align(Alignment.Center),
                            shouldShowSubtitle = showSubtitle,
                            shouldPip = false,
                            shouldScaleDownSubtitle = true,
                            timelineState = state.timelineState,
                            lyricsData = state.screenData.lyricsData?.lyrics,
                            translatedLyricsData = state.screenData.lyricsData?.translatedLyrics?.first,
                            isInPipMode = state.isInPipMode,
                            mainTextStyle = typo().bodyLarge,
                            translatedTextStyle = typo().bodyMedium,
                        )

                        // Classic/M3E's over-video controls, ported: fullscreen, ±5s, subtitles.
                        // A tap on the video toggles them; they auto-hide after 3s.
                        // Rendered INSIDE the video zone and drawn after the tap-catcher below,
                        // so its buttons are the topmost target — otherwise the catcher swallows
                        // every tap and the buttons look dead.
                        Crossfade(targetState = showVideoOverlay, label = "appleMusicVideoOverlay") { shown ->
                            if (shown) {
                                Box(
                                    modifier =
                                        Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.28f)),
                                ) {
                                    IconButton(
                                        onClick = { actions.onEnterFullscreenVideo() },
                                        modifier = Modifier.align(Alignment.TopEnd),
                                    ) {
                                        Icon(imageVector = SimpIcons.Fullscreen, contentDescription = "", tint = Color.White)
                                    }
                                    Row(
                                        modifier = Modifier.align(Alignment.Center).fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceEvenly,
                                    ) {
                                        IconButton(
                                            onClick = { actions.onUIEvent(UIEvent.Backward) },
                                            modifier = Modifier.size(48.dp).clip(CircleShape),
                                        ) {
                                            Icon(
                                                imageVector = SimpIcons.Replay5,
                                                contentDescription = "",
                                                tint = Color.White,
                                                modifier = Modifier.size(36.dp).alpha(0.8f),
                                            )
                                        }
                                        IconButton(
                                            onClick = { actions.onUIEvent(UIEvent.Forward) },
                                            modifier = Modifier.size(48.dp).clip(CircleShape),
                                        ) {
                                            Icon(
                                                imageVector = SimpIcons.Forward5,
                                                contentDescription = "",
                                                tint = Color.White,
                                                modifier = Modifier.size(36.dp).alpha(0.8f),
                                            )
                                        }
                                    }
                                    if (state.screenData.lyricsData != null) {
                                        IconButton(
                                            onClick = { onToggleSubtitle() },
                                            modifier = Modifier.align(Alignment.BottomEnd),
                                        ) {
                                            Icon(
                                                imageVector = if (showSubtitle) SimpIcons.SubtitlesOff else SimpIcons.Subtitles,
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
                } else {
                    Crossfade(targetState = state.screenData.canvasData?.isVideo, label = "appleMusicCanvasKind") { isVideo ->
                        if (isVideo == true) {
                            state.screenData.canvasData?.url?.let { url ->
                                // cropToBounds, NOT the default style's fill-height-and-overflow
                                // modifiers. Those drive MediaPlayerView's legacy path, which sizes
                                // the surface from a `widthPx` seeded to the SCREEN width and only
                                // corrects it once onVideoSizeChanged reports the real aspect ratio.
                                // For a 9:16 canvas the true width is far wider than the screen, so
                                // the first frame renders fitted and the next one jumps to cropped —
                                // the sideways flash when returning from the Lyrics tab, where the
                                // Crossfade had disposed this whole subtree and every remember with
                                // it. The crop path takes its size from Media3's own
                                // presentationState instead, so there is no wrong guess to correct,
                                // and it holds a shutter over the surface until the first frame is
                                // actually ready.
                                MediaPlayerView(
                                    url = url,
                                    cropToBounds = true,
                                    modifier = Modifier.fillMaxSize(),
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
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                }
                // Canvas only: a full-size tap-catcher toggles the player's controls. The VIDEO
                // case must NOT have one — it would sit above the over-video overlay and swallow
                // every tap meant for fullscreen/±5s/subtitles (its tap is handled by the video
                // zone Box instead).
                if (!isVideoBackdrop) {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() },
                                ) { actions.onToggleControls() },
                    )
                }
            }
        } else if (pageTrack != null) {
            val staticThumb = pageTrack.thumbnails?.maxByOrNull { it.width * it.height }?.url
            Box(
                modifier =
                    Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .height(artworkZoneHeightDp.dp),
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
                    modifier = Modifier.fillMaxSize().appleMusicVerticalFadeEdges(topFade = 0.dp, bottomFade = 300.dp),
                )
            }
        }
    }
}

// Radius of the frosted cover art behind the page. Large enough that no detail of the artwork
// survives as a shape — what is left is its colour and its broad light and dark areas, which is
// precisely what Apple's background is.
private val BACKDROP_BLUR_RADIUS = 80.dp

// How much of the artwork-derived gradient sits over the frosted art. Enough to darken the page
// towards the bottom so the transport stays readable; not so much that it hides the art again.
private const val BACKDROP_TINT_ALPHA = 0.62f

// How long the LYRICS tab waits for a track's lyrics before deciding the track has none. Long
// enough to cover a normal fetch on a normal connection, short enough that a song with no lyrics
// does not leave the user staring at an empty page. Not a fixed budget for the request itself:
// lyrics arriving at any point cancel the wait outright.
private const val LYRICS_ABSENCE_GRACE_MS = 2_500L
