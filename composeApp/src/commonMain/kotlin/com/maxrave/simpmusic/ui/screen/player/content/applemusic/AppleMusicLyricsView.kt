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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maxrave.domain.manager.DataStoreManager
import com.maxrave.simpmusic.expect.ui.DeviceVolumeController
import com.maxrave.simpmusic.expect.ui.isLyricsBlurSupported
import com.maxrave.simpmusic.ui.component.AppleMusicLyricPaddingX
import com.maxrave.simpmusic.ui.component.LyricsView
import com.maxrave.simpmusic.ui.component.lyrics.ShareLyricsSheet
import com.maxrave.simpmusic.ui.component.lyrics.toShareLyricsLines
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
import simpmusic.composeapp.generated.resources.spotify_lyrics_provider
import simpmusic.composeapp.generated.resources.unsynced

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
    LaunchedEffect(showCluster, interactionTick) {
        if (showCluster) {
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
                // Transparent background: the page's artwork-tinted gradient is the backdrop,
                // not LyricsView's own dark card color.
                // The app's own renderer, with its own line sizes/animations. The provider caption
                // rides as its LAST list item so it scrolls with the lyrics, per the owner's
                // original spec.
                LyricsView(
                    lyricsData = lyricsData,
                    timeLine = state.timelineFlow,
                    onLineClick = { f ->
                        actions.onUIEvent(UIEvent.UpdateProgress(f))
                    },
                    modifier =
                        Modifier
                            .fillMaxSize()
                            // The lines dissolve at both edges instead of being guillotined by
                            // the header/cluster — applied from OUTSIDE the component.
                            // Softer bottom fade than the top: a lyric line is ~54dp tall, and a
                            // 44dp fade dissolved most of the last visible line.
                            .appleMusicVerticalFadeEdges(topFade = 28.dp, bottomFade = 18.dp)
                            // Zero under the Apple Music renderer, which insets its own lines so
                            // its blur has margin to spill into; the full gutter under Classic,
                            // which insets nothing. Applied AFTER the fade so the fade still spans
                            // the full width.
                            .padding(horizontal = if (rendererOwnsGutter) 0.dp else AppleMusicLyricPaddingX),
                    backgroundColor = Color.Transparent,
                    footerContent = {
                        // Right-aligned, and stacked: plain text rather than the AIBadge pill the
                        // other styles use. Classic puts that badge beside a "Lyrics" heading where
                        // it has a row to itself; here it would sit inline with the provider
                        // caption, which is already the quietest thing on the page — a filled pill
                        // next to it shouts. Two right-anchored lines read as one footnote block.
                        Column(
                            horizontalAlignment = Alignment.End,
                            modifier = Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 8.dp),
                        ) {
                            if (lyricsData.translatedLyrics?.second == LyricsProvider.AI) {
                                Text(
                                    text = stringResource(Res.string.ai_translated),
                                    style = typography.footer,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                            // One line per fact, not one line joined by a bullet. "Word by word"
                            // and "Lyrics provided by SimpMusic Lyrics" are two different things,
                            // and glued together they make a single line long enough to run the
                            // width of the screen.
                            //
                            // fillMaxWidth + TextAlign.End on each, not just the Column's
                            // alignment: Column alignment places a whole text block, so a line that
                            // wraps still ends up left-aligned within itself.
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
                    },
                )
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