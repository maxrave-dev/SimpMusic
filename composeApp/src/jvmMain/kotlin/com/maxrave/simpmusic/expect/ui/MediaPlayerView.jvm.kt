package com.maxrave.simpmusic.expect.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import com.maxrave.domain.data.model.metadata.Lyrics
import com.maxrave.domain.data.model.streams.TimeLine
import com.maxrave.media_jvm_ui.ui.MediaPlayerViewWithSubtitleJvm
import com.maxrave.media_jvm_ui.ui.MediaPlayerViewWithUrl
import com.maxrave.simpmusic.ui.theme.typo

@Composable
actual fun MediaPlayerView(
    url: String,
    modifier: Modifier,
) {
    MediaPlayerViewWithUrl(
        url = url,
        modifier = modifier,
    )
}

@Composable
actual fun MediaPlayerViewWithSubtitle(
    modifier: Modifier,
    playerName: String,
    shouldPip: Boolean,
    shouldShowSubtitle: Boolean,
    shouldScaleDownSubtitle: Boolean,
    isInPipMode: Boolean,
    timelineState: TimeLine,
    lyricsData: Lyrics?,
    translatedLyricsData: Lyrics?,
    mainTextStyle: TextStyle,
    translatedTextStyle: TextStyle,
) {
    MediaPlayerViewWithSubtitleJvm(
        playerName = playerName,
        modifier = modifier,
        shouldShowSubtitle = shouldShowSubtitle,
        shouldScaleDownSubtitle = shouldScaleDownSubtitle,
        timelineState = timelineState,
        lyricsData = lyricsData,
        translatedLyricsData = translatedLyricsData,
        mainTextStyle = typo().bodyLarge,
        translatedTextStyle = typo().bodyMedium,
    )
}