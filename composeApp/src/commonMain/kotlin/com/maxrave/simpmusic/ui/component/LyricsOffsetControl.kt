package com.maxrave.simpmusic.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.maxrave.simpmusic.expect.ui.PlatformBackdrop
import com.maxrave.simpmusic.ui.icon.Add
import com.maxrave.simpmusic.ui.icon.Remove
import com.maxrave.simpmusic.ui.icon.RestartAlt
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.NowPlayingScreenData
import org.jetbrains.compose.resources.stringResource
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.lyrics_offset_value

// One tap moves the lyrics by this much: fine enough for Bluetooth latency, which is what the
// offset exists for. A sheet that is seconds off is still quicker to type in Settings.
private const val LYRICS_OFFSET_STEP_MS = 100

/** Only timed lyrics can run early or late, so only they get the timing control. */
internal fun NowPlayingScreenData.LyricsData?.hasTiming(): Boolean =
    this?.lyrics?.syncType == "LINE_SYNCED" || this?.lyrics?.syncType == "RICH_SYNCED"

/**
 * `[−] +200 ms [+] [↺]` for the lyrics timing offset — the very value Settings › Lyrics edits.
 * Every lyrics display subtracts it when it reads the position, so the lines move as the user taps.
 */
@Composable
fun LyricsOffsetBar(
    offsetMs: Int,
    onOffsetChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = Color.White.copy(alpha = 0.24f),
    contentColor: Color = Color.White,
) {
    // ponytail: each step adds to the value last composed, so two taps inside one DataStore round
    // trip count once. Fine for fingers; give DataStoreManager an atomic adjust if the buttons
    // ever repeat on hold.
    // Signed the way the Settings row prints it.
    val signedMs = if (offsetMs > 0) "+$offsetMs" else offsetMs.toString()
    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
        Row(
            modifier = modifier.clip(CircleShape).background(containerColor),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RippleIconButton(imageVector = SimpIcons.Remove, tint = contentColor) {
                onOffsetChange(offsetMs - LYRICS_OFFSET_STEP_MS)
            }
            Text(
                text = stringResource(Res.string.lyrics_offset_value, signedMs),
                style = typo().labelSmall,
                color = contentColor,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier.widthIn(min = 72.dp),
            )
            RippleIconButton(imageVector = SimpIcons.Add, tint = contentColor) {
                onOffsetChange(offsetMs + LYRICS_OFFSET_STEP_MS)
            }
            AnimatedVisibility(visible = offsetMs != 0) {
                RippleIconButton(imageVector = SimpIcons.RestartAlt, tint = contentColor) { onOffsetChange(0) }
            }
        }
    }
}

/**
 * Opens [LyricsOffsetBar] to the left of [button], for pages whose lyrics have no header to put a
 * button in. The page draws [button] itself, in its own style; [backdrop] puts the bar on glass for a
 * glass page, and null leaves it on the flat fill of the Apple Music tab's round buttons.
 * [onInteraction] lets a page that hides its controls on a timer count these taps.
 */
@Composable
fun LyricsOffsetFloatingControl(
    offsetMs: Int,
    onOffsetChange: (Int) -> Unit,
    backdrop: PlatformBackdrop?,
    modifier: Modifier = Modifier,
    onInteraction: () -> Unit = {},
    button: @Composable (onClick: () -> Unit) -> Unit,
) {
    var open by rememberSaveable { mutableStateOf(false) }
    val onBarChange: (Int) -> Unit = {
        onInteraction()
        onOffsetChange(it)
    }
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        AnimatedVisibility(visible = open) {
            if (backdrop != null) {
                // The fullscreen page's own glass pill: 48dp, 24dp corners, static like its volume pill.
                LiquidGlassContainer(
                    backdrop = backdrop,
                    modifier = Modifier.padding(end = 8.dp).height(48.dp),
                    shape = RoundedCornerShape(24.dp),
                    interactive = false,
                ) {
                    LyricsOffsetBar(
                        offsetMs = offsetMs,
                        onOffsetChange = onBarChange,
                        modifier = Modifier.padding(horizontal = 4.dp),
                        containerColor = Color.Transparent,
                    )
                }
            } else {
                LyricsOffsetBar(
                    offsetMs = offsetMs,
                    onOffsetChange = onBarChange,
                    modifier = Modifier.padding(end = 8.dp),
                )
            }
        }
        button {
            open = !open
            onInteraction()
        }
    }
}
