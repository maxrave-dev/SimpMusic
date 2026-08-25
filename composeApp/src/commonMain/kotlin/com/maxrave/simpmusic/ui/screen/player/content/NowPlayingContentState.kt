package com.maxrave.simpmusic.ui.screen.player.content

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector4D
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import com.maxrave.domain.data.model.browse.album.Track
import com.maxrave.domain.data.model.streams.TimeLine
import com.maxrave.domain.data.player.GenericCastState
import com.maxrave.domain.mediaservice.handler.ControlState
import com.maxrave.simpmusic.extension.GradientOffset
import com.maxrave.simpmusic.viewModel.LyricsProvider
import com.maxrave.simpmusic.viewModel.NowPlayingScreenData
import com.maxrave.simpmusic.viewModel.UIEvent
import kotlinx.coroutines.flow.StateFlow

/**
 * Whether the lyrics currently on screen can be rated.
 *
 * SimpMusic Lyrics is the only provider with a vote endpoint, and the vote is cast against the
 * SimpMusic record itself — so the provider tag alone is NOT the condition: `simpMusicLyrics`
 * must actually be there. Either half qualifying is enough, because the dialog rates whichever
 * of the two came from SimpMusic.
 *
 * Lives on the contract the three styles share. The Apple Music style shipped its floating vote
 * button ungated — offering a rating on YouTube, LRCLIB and Spotify lyrics alike — precisely
 * because this rule existed only as an expression copy-pasted inside the other two styles, where
 * a new style had no reason to go looking for it.
 */
internal fun NowPlayingScreenData.LyricsData?.canVote(): Boolean {
    val data = this ?: return false
    val votableLyrics =
        data.lyricsProvider == LyricsProvider.SIMPMUSIC && data.lyrics.simpMusicLyrics != null
    val votableTranslation =
        data.translatedLyrics?.second == LyricsProvider.SIMPMUSIC &&
            data.translatedLyrics?.first?.simpMusicLyrics != null
    return votableLyrics || votableTranslation
}

// Backdrop behind the player. A dark surface rather than pure black: #000000 reads as a hole
// next to the artwork-tinted gradient and cards, which is why Spotify sits its player on a
// near-black surface instead. Used for the gradient's end colour, the fade-to target and the
// area below the gradient so all three match exactly and leave no seam.
internal val PlayerBackdropColor = Color(0xFF121212)

private val RICH_SYNC_TIMESTAMP_REGEX = Regex("""<\d{2}:\d{2}\.\d{2,3}>\s*""")
private val WHITESPACE_REGEX = Regex("""\s+""")

// Word-by-word lyrics carry a timestamp per word; replace each with a space
// (not ""), then collapse — otherwise the words run together.
// Shared by every Now Playing content style (Spotify + M3 Expressive).
internal fun String.stripRichSyncTimestamps(): String =
    replace(RICH_SYNC_TIMESTAMP_REGEX, " ")
        .replace(WHITESPACE_REGEX, " ")
        .trim()

// Codec label for the Apple Music style's progress-bar badge. Derived from the stream's
// mimeType (e.g. `audio/webm; codecs="opus"`, `audio/mp4; codecs="mp4a.40.2"`) rather than the
// itag, which the two YouTube audio families always encode as one of these two codecs. Returns
// null for anything else so the badge hides instead of showing a guess.
internal fun String?.toAudioCodecLabel(): String? {
    // Fed NewFormatEntity.codecs — "opus", or "mp4a.40.2" for AAC. The regex that fills that
    // column falls back to the WHOLE mimeType when it fails to match, so both shapes have to be
    // recognised here; "aac" covers the Piped path, which reports the codec by name.
    val codec = this ?: return null
    return when {
        codec.contains("opus", ignoreCase = true) -> "OPUS"
        codec.contains("mp4a", ignoreCase = true) || codec.contains("aac", ignoreCase = true) -> "AAC"
        else -> null
    }
}

/**
 * Everything a Now Playing content layer reads. The shell ([com.maxrave.simpmusic.ui.screen.player.NowPlayingScreenContent])
 * owns the ViewModel collection, palette animation, sheets/dialogs and gesture state machines;
 * a content composable only renders from this snapshot.
 */
@Stable
class NowPlayingContentState(
    val screenData: NowPlayingScreenData,
    val controllerState: ControlState,
    val timelineState: TimeLine,
    val timelineFlow: StateFlow<TimeLine>,
    val likeStatus: Boolean,
    val castState: GenericCastState,
    val shouldShowVideo: Boolean,
    val isUserLoggedIn: Boolean,
    val artworkQueue: List<Track>,
    val currentOrderIndex: Int,
    val artworkPagerState: PagerState,
    val startColor: Animatable<Color, AnimationVector4D>,
    val endColor: Animatable<Color, AnimationVector4D>,
    val spotShadowColor: Color,
    val gradientOffset: GradientOffset,
    val sliderTrackColor: Color,
    val sliderValue: Float,
    val currentLyricLineIndex: Int,
    val showControlLayout: Boolean,
    val controlLayoutAlpha: Float,
    val showHideMiddleLayout: Boolean,
    val shouldShowToolbar: Boolean,
    val isInPipMode: Boolean,
    val mainScrollState: ScrollState,
    val isExpanded: Boolean,
    val dismissIcon: ImageVector,
    /** Current track's audio codec ("OPUS"/"AAC"), or null while unknown — see [toAudioCodecLabel]. */
    val audioCodecLabel: String? = null,
)

/**
 * Everything a Now Playing content layer can do. All callbacks land in the shell, which owns
 * the ViewModel, the navController and the sheet/dialog visibility flags.
 */
@Stable
class NowPlayingContentActions(
    val onUIEvent: (UIEvent) -> Unit,
    val onSeekToQueueIndex: (Int) -> Unit,
    val onArtworkBitmap: (ImageBitmap) -> Unit,
    val onSliderChange: (Float) -> Unit,
    val onSliderChangeFinished: () -> Unit,
    val onToggleControls: () -> Unit,
    val onNavigateToArtist: () -> Unit,
    val onAddToYouTubeLiked: () -> Unit,
    val onShowMoreSheet: () -> Unit,
    val onShowQueue: () -> Unit,
    val onShowInfo: () -> Unit,
    val onShowAddToPlaylist: () -> Unit,
    val onShowFullscreenLyrics: () -> Unit,
    val onShowVoteDialog: () -> Unit,
    val onEnterFullscreenVideo: () -> Unit,
    val onDismiss: () -> Unit,
    val onToolbarVisibilityChange: (Boolean) -> Unit,
    /** Reorders the queue. `from`/`to` are absolute indices into [NowPlayingContentState.artworkQueue]. */
    val onMoveQueueItem: (from: Int, to: Int) -> Unit,
    /** Removes one queue entry. `index` is an absolute index into [NowPlayingContentState.artworkQueue]. */
    val onRemoveQueueItem: (index: Int) -> Unit,
)
