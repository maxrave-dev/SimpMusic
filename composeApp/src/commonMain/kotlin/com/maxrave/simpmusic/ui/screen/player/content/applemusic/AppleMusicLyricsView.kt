package com.maxrave.simpmusic.ui.screen.player.content.applemusic

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.maxrave.simpmusic.expect.ui.DeviceVolumeController
import com.maxrave.simpmusic.ui.component.LyricsView
import com.maxrave.simpmusic.ui.icon.OpenInFull
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.icon.ThumbsUpDown
import com.maxrave.simpmusic.ui.screen.player.content.NowPlayingContentActions
import com.maxrave.simpmusic.ui.screen.player.content.NowPlayingContentState
import com.maxrave.simpmusic.viewModel.LyricsProvider
import com.maxrave.simpmusic.viewModel.NowPlayingScreenData
import com.maxrave.simpmusic.viewModel.UIEvent
import org.jetbrains.compose.resources.stringResource
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.line_synced
import simpmusic.composeapp.generated.resources.lyrics_provider_betterlyrics
import simpmusic.composeapp.generated.resources.lyrics_provider_lrc
import simpmusic.composeapp.generated.resources.lyrics_provider_simpmusic
import simpmusic.composeapp.generated.resources.lyrics_provider_youtube
import simpmusic.composeapp.generated.resources.lyrics_sync_provider_footer
import simpmusic.composeapp.generated.resources.offline_mode
import simpmusic.composeapp.generated.resources.rich_synced
import simpmusic.composeapp.generated.resources.spotify_lyrics_provider
import simpmusic.composeapp.generated.resources.unsynced

/**
 * The LYRICS body: compact header, the app's own [LyricsView] — the SAME renderer the other
 * styles' lyrics card embeds (owner's rule: reuse the existing component, never rebuild it) —
 * two floating action buttons, the "sync • provider" caption, and the shared bottom cluster.
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
) {
    val localDensity = LocalDensity.current
    val lyricsData = state.screenData.lyricsData

    Column(modifier = modifier.fillMaxSize()) {
        Spacer(modifier = Modifier.height(with(localDensity) { WindowInsets.statusBars.getTop(localDensity).toDp() }))
        AppleMusicCompactHeader(state = state, actions = actions, typography = typography)

        Box(modifier = Modifier.weight(1f)) {
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
                            // 24dp, the same gutter the header, progress row, transport and dock
                            // use — at 8dp the lyric block visibly stuck out to the left of
                            // everything else on the screen.
                            .padding(horizontal = 20.dp)
                            // The lines dissolve at both edges instead of being guillotined by
                            // the header/cluster — applied from OUTSIDE the component.
                            // Softer bottom fade than the top: a lyric line is ~54dp tall, and a
                            // 44dp fade dissolved most of the last visible line.
                            .appleMusicVerticalFadeEdges(topFade = 28.dp, bottomFade = 18.dp),
                    backgroundColor = Color.Transparent,
                    footerContent = {
                        Text(
                            text = appleMusicLyricsFooterText(lyricsData),
                            style = typography.footer,
                            modifier = Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 8.dp),
                        )
                    },
                )
                // Bottom-end, inside the list's own bottom fade so they sit over the dimmest
                // lyrics rather than over a bright active line.
                Column(
                    modifier = Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    AppleMusicFloatingCircleButton(icon = SimpIcons.ThumbsUpDown, onClick = { actions.onShowVoteDialog() })
                    AppleMusicFloatingCircleButton(icon = SimpIcons.OpenInFull, onClick = { actions.onShowFullscreenLyrics() })
                }
            }
        }

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

/** "<sync type> • <provider>" — same sources as the other styles' lyrics card footer, joined into one caption line. */
@Composable
private fun appleMusicLyricsFooterText(lyricsData: NowPlayingScreenData.LyricsData): String {
    val syncTypeText =
        when (lyricsData.lyrics.syncType) {
            "LINE_SYNCED" -> stringResource(Res.string.line_synced)
            "RICH_SYNCED" -> stringResource(Res.string.rich_synced)
            else -> stringResource(Res.string.unsynced)
        }
    val providerText =
        when (lyricsData.lyricsProvider) {
            LyricsProvider.SIMPMUSIC -> stringResource(Res.string.lyrics_provider_simpmusic)
            LyricsProvider.LRCLIB -> stringResource(Res.string.lyrics_provider_lrc)
            LyricsProvider.YOUTUBE -> stringResource(Res.string.lyrics_provider_youtube)
            LyricsProvider.SPOTIFY -> stringResource(Res.string.spotify_lyrics_provider)
            LyricsProvider.OFFLINE -> stringResource(Res.string.offline_mode)
            LyricsProvider.BETTER_LYRICS -> stringResource(Res.string.lyrics_provider_betterlyrics)
            LyricsProvider.AI -> ""
        }
    return stringResource(Res.string.lyrics_sync_provider_footer, syncTypeText, providerText)
}
