package com.maxrave.simpmusic.ui.screen.player.content.applemusic

import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maxrave.domain.manager.DataStoreManager
import com.maxrave.simpmusic.expect.ui.DeviceVolumeController
import com.maxrave.simpmusic.expect.ui.isLyricsBlurSupported
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextMotion
import com.maxrave.simpmusic.ui.component.AppleMusicLyricFontSize
import com.maxrave.simpmusic.ui.component.AppleMusicLyricLineHeight
import com.maxrave.simpmusic.ui.component.AppleMusicLyricPaddingX
import com.maxrave.simpmusic.ui.component.LyricsView
import com.maxrave.simpmusic.ui.component.lyrics.ShareLyricsSheet
import com.maxrave.domain.repository.LyricsRomanizerRepository
import com.maxrave.simpmusic.ui.component.lyrics.accompanist.LyricsAdapter
import com.maxrave.simpmusic.ui.component.lyrics.toShareLyricsLines
import com.mocharealm.accompanist.lyrics.ui.composable.lyrics.KaraokeLyricsView
import com.maxrave.simpmusic.ui.icon.OpenInFull
import com.maxrave.simpmusic.ui.icon.Share
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.icon.ThumbsUpDown
import com.maxrave.simpmusic.ui.screen.player.content.NowPlayingContentActions
import com.maxrave.simpmusic.ui.screen.player.content.NowPlayingContentState
import com.maxrave.simpmusic.ui.screen.player.content.canVote
import com.maxrave.simpmusic.viewModel.LyricsProvider
import com.maxrave.simpmusic.viewModel.NowPlayingScreenData
import com.maxrave.simpmusic.viewModel.UIEvent
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.ai_translated
import simpmusic.composeapp.generated.resources.line_synced
import simpmusic.composeapp.generated.resources.lyrics_provider_betterlyrics
import simpmusic.composeapp.generated.resources.lyrics_provider_lrc
import simpmusic.composeapp.generated.resources.lyrics_provider_simpmusic
import simpmusic.composeapp.generated.resources.lyrics_provider_youtube
import simpmusic.composeapp.generated.resources.offline_mode
import simpmusic.composeapp.generated.resources.rich_synced
import simpmusic.composeapp.generated.resources.sf_pro
import simpmusic.composeapp.generated.resources.spotify_lyrics_provider
import simpmusic.composeapp.generated.resources.unsynced
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import org.jetbrains.compose.resources.Font

@OptIn(ExperimentalTextApi::class)
@Composable
private fun sfProFamily(): FontFamily = FontFamily(
    Font(
        resource = Res.font.sf_pro,
        weight = FontWeight.Bold,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(FontWeight.Bold.weight),
        )
    ),
    Font(
        resource = Res.font.sf_pro,
        weight = FontWeight.SemiBold,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(FontWeight.SemiBold.weight),
        )
    ),
    Font(
        resource = Res.font.sf_pro,
        weight = FontWeight.Medium,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(FontWeight.Medium.weight),
        )
    ),
)

/**
 * The LYRICS body: compact header, the app's own [LyricsView] — the SAME renderer the other
 * styles' lyrics card embeds (owner's rule: reuse the existing component, never rebuild it) —
 * two floating action buttons, the sync-type and provider caption lines, and the shared bottom cluster.
 * The top-level composable falls back to MAIN when lyrics disappear mid-session.
 */
@Composable
internal fun AppleMusicLyricsView(
    state: NowPlayingContentState,
    actions: NowPlayingContentActions,
    typography: AppleMusicTypography,
    viewState: AppleMusicView,
    onSelectView: (AppleMusicView) -> Unit,
    activePillContainer: Color,
    activePillContent: Color,
    deviceVolumeController: DeviceVolumeController?,
    modifier: Modifier = Modifier,
    dataStoreManager: DataStoreManager = koinInject(),
    romanizer: LyricsRomanizerRepository = koinInject(),
) {
    val localDensity = LocalDensity.current
    val lyricsData = state.screenData.lyricsData

    // LyricsView applies AppleMusicLyricPaddingX ITSELF, but only on its Apple Music branch — its
    // Classic branch renders LyricsLineItem, which has no horizontal padding of its own. So this
    // tab, which contributes no gutter, left Classic lyrics flush against the screen edge while
    // the header right above them sits at 20dp. Same split the fullscreen sheet already does:
    // the caller contributes whatever the renderer does not, so the TOTAL is the same either way.
    val lyricsStyle by dataStoreManager.lyricsStyle.collectAsStateWithLifecycle(DataStoreManager.LYRICS_STYLE_CLASSIC)
    val rendererOwnsGutter = lyricsStyle == DataStoreManager.LYRICS_STYLE_APPLE_MUSIC && isLyricsBlurSupported()

    // Apple hands the whole page to the lyrics once you stop touching it, and brings the transport
    // back the moment you touch it again. rememberSaveable so a rotation does not yank the
    // controls back into view.
    var showCluster by rememberSaveable { mutableStateOf(true) }
    var showShareSheet by rememberSaveable { mutableStateOf(false) }
    // Bumped on every interaction, and keyed into the timer below, so ANY touch restarts the
    // countdown. Without it a scroll while the cluster is already shown leaves showCluster
    // unchanged, the LaunchedEffect never restarts, and the controls vanish mid-gesture.
    var interactionTick by remember { mutableIntStateOf(0) }
    var isOptionsMenuOpen by remember { mutableStateOf(false) }
    LaunchedEffect(showCluster, interactionTick, isOptionsMenuOpen) {
        if (showCluster && !isOptionsMenuOpen) {
            delay(CLUSTER_AUTO_HIDE_MS)
            showCluster = false
        }
    }

    // Scrolling the lyrics counts as reaching for the player, exactly like tapping does — and the
    // scroll happens inside LyricsView's own LazyColumn, which owns a list state this file cannot
    // see. A nested-scroll connection catches it on the way past without reaching in.
    val scrollWakesControls =
        remember {
            object : NestedScrollConnection {
                override fun onPreScroll(
                    available: Offset,
                    source: NestedScrollSource,
                ): Offset {
                    if (available.y != 0f) {
                        showCluster = true
                        interactionTick++
                    }
                    // Zero: this only observes. Consuming any of it would fight the list's scroll.
                    return Offset.Zero
                }
            }
        }

    Column(modifier = modifier.fillMaxSize()) {
        // statusBars + 20dp, not a bare status-bar offset: the grabber that
        // NowPlayingContentAppleMusic draws above the view Crossfade floats over this column, and
        // without the extra room the header's title slides underneath it.
        Spacer(
            modifier =
                Modifier.height(
                    with(localDensity) { WindowInsets.statusBars.getTop(localDensity).toDp() } + 20.dp,
                ),
        )
        AppleMusicCompactHeader(state = state, actions = actions, typography = typography)

        Box(
            modifier =
                Modifier
                    .weight(1f)
                    .nestedScroll(scrollWakesControls)
                    // The lyric LINES consume their own taps to seek, so this only ever fires on
                    // the gutter and the gaps between lines — which is exactly the "tap the page,
                    // not a lyric" gesture. No ripple: this is a whole page, not a button.
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                    ) {
                        showCluster = !showCluster
                        interactionTick++
                    },
        ) {
            if (lyricsData != null) {
                val showTranslation by dataStoreManager.showLyricsTranslation.collectAsStateWithLifecycle(true)
                val showOriginal by dataStoreManager.showLyricsOriginal.collectAsStateWithLifecycle(true)
                val romanizationLanguagesStr by dataStoreManager.romanizationLanguages.collectAsStateWithLifecycle("")
                val enabledLanguages = remember(romanizationLanguagesStr) {
                    com.maxrave.domain.data.model.lyrics.RomanizationLanguage.parse(romanizationLanguagesStr)
                }

                val syncedLyrics = remember(lyricsData, enabledLanguages) {
                    LyricsAdapter.toSyncedLyrics(lyricsData, enabledLanguages)
                }
                val lyricsListState = rememberLazyListState()
                // Collect the timeline as Compose state so KaraokeLyricsView's derivedStateOf
                // correctly re-reads the current position on every frame.
                val timeline by state.timelineFlow.collectAsStateWithLifecycle()
                val isPlaying = state.controllerState.isPlaying

                // Single Internal Clock Architecture with Monotonic Smooth Synchronization:
                // 1. Never restarts the frame loop on 100ms poll updates (keys only on isPlaying).
                // 2. Extrapolates frame-by-frame using real frame delta time (dt).
                // 3. Smoothly catches up to the player without EVER snapping backwards during playback.
                // 4. Snaps immediately on seeks/track changes (>500ms diff).
                val synchronizedPosition = remember { androidx.compose.runtime.mutableLongStateOf(0L) }
                
                LaunchedEffect(isPlaying) {
                    if (!isPlaying) {
                        // When paused or buffering, lock exactly to the server position.
                        synchronizedPosition.longValue = timeline.current
                    } else {
                        var smoothedPosition = timeline.current.toDouble()
                        var lastFrameTimeMs = 0L

                        while (true) {
                            androidx.compose.animation.core.withInfiniteAnimationFrameMillis { frameTimeMs ->
                                if (lastFrameTimeMs == 0L) {
                                    lastFrameTimeMs = frameTimeMs
                                    smoothedPosition = timeline.current.toDouble()
                                }

                                val dt = (frameTimeMs - lastFrameTimeMs).coerceIn(0L, 100L)
                                lastFrameTimeMs = frameTimeMs

                                val now = System.currentTimeMillis()
                                val timeSinceLastUpdate = if (timeline.lastUpdateTimeMs > 0L) {
                                    (now - timeline.lastUpdateTimeMs).coerceIn(0L, 1000L)
                                } else {
                                    0L
                                }
                                val targetPosition = (timeline.current + timeSinceLastUpdate).toDouble()
                                val diff = targetPosition - smoothedPosition

                                if (kotlin.math.abs(diff) > 500.0) {
                                    // User seeked, skipped, or track changed -> snap immediately
                                    smoothedPosition = targetPosition
                                } else {
                                    // Normal playback: advance smoothly by dt plus a gentle spring catchup (8% per frame)
                                    val catchUp = diff * 0.08
                                    val newPosition = smoothedPosition + dt + catchUp
                                    // Strictly monotonic: time never moves backwards during playback
                                    smoothedPosition = maxOf(smoothedPosition, newPosition)
                                }

                                synchronizedPosition.longValue = smoothedPosition.toLong()
                            }
                        }
                    }
                }

                if (syncedLyrics != null) {
                    KaraokeLyricsView(
                        listState = lyricsListState,
                        lyrics = syncedLyrics,
                        currentPosition = { synchronizedPosition.longValue.toInt() },
                        onLineClicked = { line ->
                            val totalDurationMs = timeline.total
                            if (totalDurationMs > 0) {
                                actions.onUIEvent(UIEvent.UpdateProgress((line.start.toFloat() / totalDurationMs.toFloat()) * 100f))
                            }
                        },
                        onLinePressed = { _ ->
                            showShareSheet = true
                        },
                        normalLineTextStyle = LocalTextStyle.current.copy(
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = sfProFamily(),
                            textMotion = TextMotion.Animated,
                        ),
                        accompanimentLineTextStyle = LocalTextStyle.current.copy(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = sfProFamily(),
                            textMotion = TextMotion.Animated,
                        ),
                        phoneticTextStyle = LocalTextStyle.current.copy(
                            fontSize = 21.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = sfProFamily(),
                            textMotion = TextMotion.Animated,
                        ),
                        textColor = Color.White,
                        useBlurEffect = isLyricsBlurSupported(),
                        showTranslation = showTranslation && lyricsData.translatedLyrics != null,
                        showPhonetic = showOriginal,
                        contentPadding = PaddingValues(horizontal = AppleMusicLyricPaddingX),
                        modifier = Modifier
                            .fillMaxSize()
                            .appleMusicVerticalFadeEdges(topFade = 28.dp, bottomFade = 18.dp),
                        footerContent = {
                            LyricsFooterContent(lyricsData, typography)
                        }
                    )
                } else {
                    val isUnsynced = lyricsData.lyrics.syncType == "UNSYNCED" || lyricsData.lyrics.syncType == null
                    LyricsView(
                        lyricsData = lyricsData,
                        timeLine = state.timelineFlow,
                        onLineClick = { f ->
                            actions.onUIEvent(UIEvent.UpdateProgress(f))
                        },
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .then(
                                    if (isUnsynced) Modifier else Modifier.appleMusicVerticalFadeEdges(topFade = 28.dp, bottomFade = 18.dp)
                                )
                                .padding(horizontal = if (rendererOwnsGutter && !isUnsynced) 0.dp else AppleMusicLyricPaddingX),
                        backgroundColor = Color.Transparent,
                        footerContent = {
                            LyricsFooterContent(lyricsData, typography)
                        },
                    )
                }
                // Bottom-end, inside the list's own bottom fade so they sit over the dimmest
                // lyrics rather than over a bright active line.
                androidx.compose.animation.AnimatedVisibility(
                    visible = showCluster,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.align(Alignment.BottomEnd),
                ) {
                    Column(
                        modifier = Modifier.padding(end = 20.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        // Only when the lyrics (or the translation) actually came from SimpMusic
                        // Lyrics — the sole provider that accepts a vote. Classic and M3E have
                        // always gated theirs; this one did not, so it invited a rating on
                        // YouTube/LRCLIB/Spotify lyrics that had nowhere to go.
                        if (lyricsData.canVote()) {
                            AppleMusicFloatingCircleButton(icon = SimpIcons.ThumbsUpDown, onClick = { actions.onShowVoteDialog() })
                        }
                        AppleMusicFloatingCircleButton(icon = SimpIcons.Share, onClick = { showShareSheet = true })
                        AppleMusicFloatingCircleButton(icon = SimpIcons.OpenInFull, onClick = { actions.onShowFullscreenLyrics() })
                    }
                }
            }

            if (!showCluster) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .align(Alignment.BottomCenter)
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                        ) {
                            showCluster = true
                            interactionTick++
                        },
                )
            }
        }

        if (showShareSheet && lyricsData != null) {
            ShareLyricsSheet(
                lines = lyricsData.toShareLyricsLines(),
                songTitle = state.screenData.nowPlayingTitle,
                artistName = state.screenData.artistName,
                // The track's already-decoded artwork. A URL would still be loading at the moment
                // the card is captured, and would come out blank.
                artwork = state.screenData.bitmap,
                seedColor = state.startColor.value,
                initialLineIndex = state.currentLyricLineIndex,
                onDismiss = { showShareSheet = false },
            )
        }

        // expand/shrink, not just fade: the Box above holds weight(1f), so removing the cluster
        // from the layout is what lets the lyrics grow into the freed space — a fade alone would
        // leave an empty band where the transport used to be.
        androidx.compose.animation.AnimatedVisibility(
            visible = showCluster,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            AppleMusicBottomCluster(
                state = state,
                actions = actions,
                typography = typography,
                viewState = viewState,
                onSelectView = onSelectView,
                activePillContainer = activePillContainer,
                activePillContent = activePillContent,
                deviceVolumeController = deviceVolumeController,
                onOptionsOpenChanged = { isOpen ->
                    isOptionsMenuOpen = isOpen
                    if (isOpen) {
                        interactionTick++
                    }
                },
                onInteraction = {
                    interactionTick++
                },
            )
        }
    }
}

// Long enough to read a line or two and reach for a control, short enough that the page clears
// itself while you are just listening.
private const val CLUSTER_AUTO_HIDE_MS = 8_000L

@Composable
private fun AppleMusicFloatingCircleButton(
    icon: ImageVector,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .appleMusicPressInflate()
                .size(38.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.24f))
                .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(imageVector = icon, contentDescription = "", tint = Color.White, modifier = Modifier.size(18.dp))
    }
}

/** The sync-type half of the caption: "Word by word", "Line Synced" or "Unsynced". Its own line. */
@Composable
private fun appleMusicLyricsSyncText(lyricsData: NowPlayingScreenData.LyricsData): String {
    val syncTypeText =
        when (lyricsData.lyrics.syncType) {
            "LINE_SYNCED" -> stringResource(Res.string.line_synced)
            "RICH_SYNCED" -> stringResource(Res.string.rich_synced)
            else -> stringResource(Res.string.unsynced)
        }
    return syncTypeText
}

/** The provider half of the caption. Its own line — see the footer for why. */
@Composable
private fun appleMusicLyricsProviderText(lyricsData: NowPlayingScreenData.LyricsData): String =
    when (lyricsData.lyricsProvider) {
        LyricsProvider.SIMPMUSIC -> stringResource(Res.string.lyrics_provider_simpmusic)
        LyricsProvider.LRCLIB -> stringResource(Res.string.lyrics_provider_lrc)
        LyricsProvider.YOUTUBE -> stringResource(Res.string.lyrics_provider_youtube)
        LyricsProvider.SPOTIFY -> stringResource(Res.string.spotify_lyrics_provider)
        LyricsProvider.OFFLINE -> stringResource(Res.string.offline_mode)
        LyricsProvider.BETTER_LYRICS -> stringResource(Res.string.lyrics_provider_betterlyrics)
        LyricsProvider.AI -> ""
    }

@Composable
private fun LyricsFooterContent(
    lyricsData: NowPlayingScreenData.LyricsData,
    typography: AppleMusicTypography,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 8.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (lyricsData.translatedLyrics?.second == LyricsProvider.AI) {
                Text(
                    text = stringResource(Res.string.ai_translated),
                    style = typography.footer,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Text(
                text = appleMusicLyricsSyncText(lyricsData),
                style = typography.footer,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth(),
            )
            val provider = appleMusicLyricsProviderText(lyricsData)
            if (provider.isNotBlank()) {
                Text(
                    text = provider,
                    style = typography.footer,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}