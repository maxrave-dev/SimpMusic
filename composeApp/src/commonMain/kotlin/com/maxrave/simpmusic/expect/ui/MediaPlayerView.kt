package com.maxrave.simpmusic.expect.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import com.maxrave.domain.data.model.metadata.Lyrics
import com.maxrave.domain.data.model.streams.TimeLine

@Composable
expect fun MediaPlayerView(
    url: String,
    modifier: Modifier,
    // When true, the video center scales-to-cover the given frame (ContentScale.Crop)
    // and clips the overflow. Default false keeps the legacy fit-height-by-screen
    // behavior used by NowPlaying / Fullscreen.
    cropToBounds: Boolean = false,
)

@Composable
expect fun MediaPlayerViewWithSubtitle(
    modifier: Modifier,
    playerName: String,
    shouldPip: Boolean = false,
    shouldShowSubtitle: Boolean,
    shouldScaleDownSubtitle: Boolean = false,
    isInPipMode: Boolean,
    timelineState: TimeLine,
    lyricsData: Lyrics? = null,
    translatedLyricsData: Lyrics? = null,
    mainTextStyle: TextStyle,
    translatedTextStyle: TextStyle,
)