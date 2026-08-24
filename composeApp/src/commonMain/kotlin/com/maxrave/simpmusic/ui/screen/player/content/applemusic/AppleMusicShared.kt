package com.maxrave.simpmusic.ui.screen.player.content.applemusic

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.maxrave.domain.data.player.GenericCastState
import com.maxrave.domain.mediaservice.handler.ControlState
import com.maxrave.simpmusic.Platform
import com.maxrave.simpmusic.expect.ui.DeviceVolumeController
import com.maxrave.simpmusic.expect.ui.PlatformCastButton
import com.maxrave.simpmusic.expect.ui.isPlatformCastAvailable
import com.maxrave.simpmusic.extension.formatDuration
import com.maxrave.simpmusic.getPlatform
import com.maxrave.simpmusic.ui.component.ExplicitBadge
import com.maxrave.simpmusic.ui.component.heartBurst
import com.maxrave.simpmusic.ui.component.rememberHeartBurstState
import com.maxrave.simpmusic.ui.component.rememberHolderPainter
import com.maxrave.simpmusic.ui.icon.AddCircleOutline
import com.maxrave.simpmusic.ui.icon.CheckCircle
import com.maxrave.simpmusic.ui.icon.FastForward
import com.maxrave.simpmusic.ui.icon.FastRewind
import com.maxrave.simpmusic.ui.icon.GraphicEq
import com.maxrave.simpmusic.ui.icon.Lyrics
import com.maxrave.simpmusic.ui.icon.MoreVert
import com.maxrave.simpmusic.ui.icon.Pause
import com.maxrave.simpmusic.ui.icon.PlayArrow
import com.maxrave.simpmusic.ui.icon.QueueMusic
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.icon.Star
import com.maxrave.simpmusic.ui.icon.StarBorder
import com.maxrave.simpmusic.ui.icon.VolumeDown
import com.maxrave.simpmusic.ui.icon.VolumeUp
import com.maxrave.simpmusic.ui.screen.player.content.NowPlayingContentActions
import com.maxrave.simpmusic.ui.screen.player.content.NowPlayingContentState
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.UIEvent
import kotlin.math.roundToLong

/** Which body the dock is currently showing. Held by the top-level Apple Music composable. */
internal enum class AppleMusicView { MAIN, LYRICS, QUEUE }

/**
 * The page gradient's colour at [fraction] of the screen height: the seed barely darkened at the
 * top, ~32%-darkened by mid-page (48%), ~78%-darkened at the bottom. Used to BUILD the gradient
 * and to land the artwork fade on the gradient's own colour at the exact Y where it ends.
 */
internal fun appleMusicGradientColorAt(
    seedColor: Color,
    fraction: Float,
): Color {
    // Every stop is the SEED darkened, never a fixed near-black: the old bottom stop was a
    // hardcoded warm black, so on the artwork view — where only the lower half of the gradient
    // is visible under the artwork — the page read as plain black instead of tinted.
    val top = lerp(seedColor, Color.Black, 0.05f)
    val mid = lerp(seedColor, Color.Black, 0.32f)
    val bottom = lerp(seedColor, Color.Black, 0.78f)
    return if (fraction <= 0.48f) {
        lerp(top, mid, (fraction / 0.48f).coerceIn(0f, 1f))
    } else {
        lerp(mid, bottom, ((fraction - 0.48f) / 0.52f).coerceIn(0f, 1f))
    }
}

internal val AppleMusicTextSecondary = Color.White.copy(alpha = 0.72f)
internal val AppleMusicPillInactive = Color.White.copy(alpha = 0.24f)
internal val AppleMusicTrackInactive = Color.White.copy(alpha = 0.26f)
internal val AppleMusicTrackActive = Color.White.copy(alpha = 0.92f)

@Immutable
internal data class AppleMusicTypography(
    val mainTitle: TextStyle,
    val mainArtist: TextStyle,
    val compactTitle: TextStyle,
    val compactArtist: TextStyle,
    val queueSectionHeader: TextStyle,
    val queueSectionSubtitle: TextStyle,
    val times: TextStyle,
    val badge: TextStyle,
    val footer: TextStyle,
    val idleLyric: TextStyle,
    val idleTranslated: TextStyle,
)

/**
 * Maps every Apple Music text slot 1:1 onto the [typo] roles the OTHER styles use for the same
 * element — no custom sizes, no custom scaling (owner's rule: this style types like the rest of
 * the app). Precedents: M3E's track title/artist row (titleMedium/bodyMedium), LyricsView's
 * in-player line (headlineMedium), SongFullWidthItems rows (titleSmall/bodySmall), the queue
 * sheet's section headers (titleMedium) and M3E's canvas overlay lines (bodyMedium white/yellow).
 */
@Composable
internal fun rememberAppleMusicTypography(): AppleMusicTypography {
    val t = typo()
    return AppleMusicTypography(
        mainTitle = t.titleMedium,
        mainArtist = t.bodyMedium,
        // Compact header sits directly over the queue list, so it types like those rows
        // (SongFullWidthItems: titleSmall over bodySmall) instead of shouting at 18sp.
        compactTitle = t.titleMedium,
        compactArtist = t.bodyMedium,
        queueSectionHeader = t.titleMedium,
        queueSectionSubtitle = t.bodySmall,
        times = t.bodyMedium,
        badge = t.bodySmall,
        footer = t.bodySmall,
        idleLyric = t.bodyMedium.copy(color = Color.White),
        idleTranslated = t.bodyMedium.copy(color = Color.Yellow),
    )
}

/**
 * True alpha fade at the top/bottom edges of a scrolling region (DstIn mask): content dissolves
 * into whatever is behind it — the missing "scrim" on the lyrics list's hard-clipped edges —
 * without painting a color and without touching the wrapped component.
 */
internal fun Modifier.appleMusicVerticalFadeEdges(
    topFade: Dp,
    bottomFade: Dp,
): Modifier =
    graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        .drawWithContent {
            drawContent()
            val topPx = topFade.toPx().coerceAtMost(size.height / 2f)
            val bottomPx = bottomFade.toPx().coerceAtMost(size.height / 2f)
            val topStop = if (size.height > 0f) topPx / size.height else 0f
            val bottomStop = if (size.height > 0f) 1f - bottomPx / size.height else 1f
            drawRect(
                brush =
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        topStop to Color.Black,
                        bottomStop to Color.Black,
                        1f to Color.Transparent,
                    ),
                blendMode = BlendMode.DstIn,
            )
        }

/**
 * Press-to-swell ("phồng to ra") like the liquid-glass buttons: the control springs up while a
 * finger is on it and settles back on release.
 *
 * Detection is a plain awaitEachGesture down/up watch, NOT GlassInteraction's drag inspector —
 * that one only starts its animation from a DRAG start, so a normal tap on these buttons never
 * animated anything. `requireUnconsumed = false` keeps it working under the wrapped `clickable`,
 * and nothing here consumes events, so clicks and slider drags still land.
 */
@Composable
internal fun Modifier.appleMusicPressInflate(pressedScale: Float = 1.35f): Modifier {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = 380f),
        label = "appleMusicPressInflate",
    )
    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }.pointerInput(Unit) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                pressed = true
                waitForUpOrCancellation()
                pressed = false
            }
        }
}

/**
 * One 25dp glyph button with a tight circular ripple — [IconButton] sized and clipped exactly
 * like Classic/M3E's plain icon buttons (Info, PlaylistAdd, Queue, Replay5…), not a custom
 * Box+clickable and not an unconstrained [com.maxrave.simpmusic.ui.component.RippleIconButton].
 */
@Composable
internal fun AppleMusicGlyphButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    tint: Color = Color.White,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.appleMusicPressInflate().size(size).clip(CircleShape),
    ) {
        Icon(imageVector = icon, contentDescription = "", tint = tint)
    }
}

/**
 * The ⊕ (YouTube-liked) / ☆ (favourite, with the heart-burst) / ⋯ (more sheet) trio — shared by
 * the MAIN title row and the LYRICS/QUEUE compact header. Fires the heart-burst on the tap that
 * likes, never on state, matching every other style's like button.
 */
@Composable
internal fun AppleMusicHeaderActions(
    state: NowPlayingContentState,
    actions: NowPlayingContentActions,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (state.isUserLoggedIn) {
            // 40dp target around a 22dp glyph: a bare 22dp clickable is under half the Material
            // minimum, on the row that gets tapped most.
            Box(
                modifier = Modifier.appleMusicPressInflate().size(24.dp).clip(CircleShape).clickable { actions.onAddToYouTubeLiked() },
                contentAlignment = Alignment.Center,
            ) {
                Crossfade(targetState = state.likeStatus, label = "appleMusicYtLiked") { liked ->
                    Icon(
                        imageVector = if (liked) SimpIcons.CheckCircle else SimpIcons.AddCircleOutline,
                        contentDescription = "",
                        tint = Color.White,
                    )
                }
            }
        }
        val likeBurst = rememberHeartBurstState()
        Box(
            modifier =
                Modifier
                    .appleMusicPressInflate()
                    .size(32.dp)
                    .heartBurst(likeBurst)
                    .clip(CircleShape)
                    .clickable {
                        if (!state.controllerState.isLiked) likeBurst.fire()
                        actions.onUIEvent(UIEvent.ToggleLike)
                    },
            contentAlignment = Alignment.Center,
        ) {
            Crossfade(targetState = state.controllerState.isLiked, label = "appleMusicFavorite") { liked ->
                Icon(
                    imageVector = if (liked) SimpIcons.Star else SimpIcons.StarBorder,
                    contentDescription = "",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp),
                )
            }
        }
        AppleMusicGlyphButton(icon = SimpIcons.MoreVert, onClick = { actions.onShowMoreSheet() })
    }
}

/**
 * 56dp rounded thumbnail + title/artist column + [AppleMusicHeaderActions], used where the
 * artwork isn't on screen (LYRICS and QUEUE bodies).
 */
@Composable
internal fun AppleMusicCompactHeader(
    state: NowPlayingContentState,
    actions: NowPlayingContentActions,
    typography: AppleMusicTypography,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
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
            modifier = Modifier.size(55.dp).clip(RoundedCornerShape(4.dp)),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            // Ellipsis, not marquee: a marquee in this narrow header scrolls constantly and
            // snapshots as garbage ("Vill Be Okay … Eve" in the first device screenshots).
            Text(
                text = state.screenData.nowPlayingTitle,
                style = typography.compactTitle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (state.screenData.isExplicit) {
                    ExplicitBadge(modifier = Modifier.size(20.dp).padding(end = 4.dp))
                }
                Text(
                    text = state.screenData.artistName,
                    style = typography.compactArtist,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.clickable { actions.onNavigateToArtist() },
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        AppleMusicHeaderActions(state = state, actions = actions)
    }
}

/** Thin 4dp track shared by the progress bar and the volume row — same visual language, only the color and callbacks differ. */
@Composable
internal fun AppleMusicThinSlider(
    value: Float,
    activeColor: Color,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    onValueChangeFinished: (() -> Unit)? = null,
) {
    // The slider's "phồng" is a track that thickens while touched/dragged — same spring feel as
    // the button inflate, expressed the way a bar can.
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val dragged by interactionSource.collectIsDraggedAsState()
    val trackHeight by animateDpAsState(
        targetValue = if (pressed || dragged) 11.dp else 5.dp,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 300f),
        label = "appleMusicSliderInflate",
    )
    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
        Slider(
            value = value,
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            modifier = modifier,
            interactionSource = interactionSource,
            track = { sliderState ->
                SliderDefaults.Track(
                    modifier = Modifier.fillMaxWidth().height(trackHeight),
                    enabled = true,
                    sliderState = sliderState,
                    colors =
                        SliderDefaults.colors().copy(
                            activeTrackColor = activeColor,
                            inactiveTrackColor = AppleMusicTrackInactive,
                            thumbColor = activeColor,
                        ),
                    thumbTrackGapSize = 0.dp,
                    drawTick = { _, _ -> },
                    drawStopIndicator = null,
                )
            },
            thumb = {
                // No thumb — the approved mock and Apple's own bars are track-only; the
                // active/inactive split marks the position (same call the MiniPlayer makes).
                Spacer(Modifier.size(0.dp))
            },
        )
    }
}

/** Elapsed time, the codec badge (hidden when unknown), and the remaining time as "-m:ss". */
@Composable
internal fun AppleMusicTimesRow(
    state: NowPlayingContentState,
    typography: AppleMusicTypography,
    modifier: Modifier = Modifier,
) {
    val elapsedMs = (state.timelineState.total * (state.sliderValue / 100f)).roundToLong()
    // Clamp BEFORE formatDuration — it renders any negative as "NA:NA", and the remaining time
    // must never show that even at the very end of the track.
    val remainingMs = (state.timelineState.total - elapsedMs).coerceAtLeast(0L)
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = formatDuration(elapsedMs),
            style = typography.times,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Left,
        )
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            val codec = state.audioCodecLabel
            if (codec != null) {
                // A PILL, like the mock's badge (and Apple's "Lossless"): translucent rounded
                // background, not bare text floating between the two times.
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier =
                        Modifier
                            .clip(RoundedCornerShape(9.dp))
                            .background(Color.White.copy(alpha = 0.16f))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Icon(
                        imageVector = SimpIcons.GraphicEq,
                        contentDescription = "",
                        tint = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.size(15.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = codec, style = typography.badge.copy(color = Color.White.copy(alpha = 0.9f)))
                }
            }
        }
        Text(
            text = "-" + formatDuration(remainingMs),
            style = typography.times,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Right,
        )
    }
}

/** FastRewind(44dp) → Previous, Play/Pause(62dp, plain white — no container disc), FastForward(44dp) → Next. */
@Composable
internal fun AppleMusicTransportRow(
    controllerState: ControlState,
    loading: Boolean,
    onUIEvent: (UIEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        // Mock: a tight centered cluster with a 58dp gap — NOT SpaceEvenly, which spreads the
        // rewind/forward glyphs to the screen edges (first device screenshots).
        horizontalArrangement = Arrangement.spacedBy(58.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = { if (controllerState.isPreviousAvailable) onUIEvent(UIEvent.Previous) },
            modifier = Modifier.appleMusicPressInflate().size(56.dp).clip(CircleShape),
        ) {
            Icon(
                imageVector = SimpIcons.FastRewind,
                contentDescription = "",
                tint = Color.White.copy(alpha = if (controllerState.isPreviousAvailable) 1f else 0.4f),
                modifier = Modifier.size(46.dp),
            )
        }
        Box(
            modifier =
                Modifier
                    .appleMusicPressInflate()
                    .size(76.dp)
                    .clip(CircleShape)
                    .clickable(enabled = !loading) { onUIEvent(UIEvent.PlayPause) },
            contentAlignment = Alignment.Center,
        ) {
            Crossfade(targetState = loading, label = "appleMusicPlayLoading") { isLoading ->
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(26.dp),
                        color = Color.White,
                        strokeWidth = 3.dp,
                    )
                } else {
                    Crossfade(targetState = controllerState.isPlaying, label = "appleMusicPlayPauseIcon") { isPlaying ->
                        Icon(
                            imageVector = if (isPlaying) SimpIcons.Pause else SimpIcons.PlayArrow,
                            contentDescription = "",
                            tint = Color.White,
                            modifier = Modifier.size(66.dp),
                        )
                    }
                }
            }
        }
        IconButton(
            onClick = { if (controllerState.isNextAvailable) onUIEvent(UIEvent.Next) },
            modifier = Modifier.appleMusicPressInflate().size(56.dp).clip(CircleShape),
        ) {
            Icon(
                imageVector = SimpIcons.FastForward,
                contentDescription = "",
                tint = Color.White.copy(alpha = if (controllerState.isNextAvailable) 1f else 0.4f),
                modifier = Modifier.size(46.dp),
            )
        }
    }
}

@Composable
internal fun AppleMusicVolumeRow(
    controller: DeviceVolumeController,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = SimpIcons.VolumeDown,
            contentDescription = "",
            tint = AppleMusicTextSecondary,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Box(modifier = Modifier.weight(1f).height(14.dp), contentAlignment = Alignment.Center) {
            AppleMusicThinSlider(
                value = controller.volumeFraction,
                activeColor = AppleMusicTrackActive,
                onValueChange = { controller.setVolumeFraction(it) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Icon(
            imageVector = SimpIcons.VolumeUp,
            contentDescription = "",
            tint = AppleMusicTextSecondary,
            modifier = Modifier.size(18.dp),
        )
    }
}

/** One 44dp dock button: a light circle behind a DARK glyph while [active] (white-on-light was unreadable). */
@Composable
internal fun AppleMusicDockButton(
    icon: ImageVector,
    active: Boolean,
    activeColor: Color,
    activeContentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Box(
        modifier =
            modifier
                .appleMusicPressInflate()
                .size(40.dp)
                .clip(CircleShape)
                .background(if (active) activeColor else Color.Transparent)
                .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = "",
            tint =
                when {
                    !enabled -> Color.White.copy(alpha = 0.4f)
                    active -> activeContentColor
                    else -> Color.White.copy(alpha = 0.85f)
                },
            modifier = Modifier.size(22.dp),
        )
    }
}

/**
 * Lyrics · Cast · Queue. The Cast slot renders [PlatformCastButton] itself (which hides when
 * Cast is unavailable) and takes no "active" tint of its own — same rule M3E's connected group
 * follows for its Cast slot.
 */
@Composable
internal fun AppleMusicDock(
    viewState: AppleMusicView,
    onSelectView: (AppleMusicView) -> Unit,
    castState: GenericCastState,
    lyricsAvailable: Boolean,
    activeColor: Color,
    activeContentColor: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Re-tapping the active tab returns to MAIN — the dock is a toggle, not one-way nav.
        AppleMusicDockButton(
            icon = SimpIcons.Lyrics,
            active = viewState == AppleMusicView.LYRICS,
            activeColor = activeColor,
            activeContentColor = activeContentColor,
            enabled = lyricsAvailable,
            onClick = {
                onSelectView(if (viewState == AppleMusicView.LYRICS) AppleMusicView.MAIN else AppleMusicView.LYRICS)
            },
        )
        if (isPlatformCastAvailable()) {
            Box(modifier = Modifier.appleMusicPressInflate().size(40.dp), contentAlignment = Alignment.Center) {
                PlatformCastButton(
                    modifier = Modifier.size(22.dp),
                    tint = if (castState.isRemote) activeColor else Color.White,
                )
            }
        }
        AppleMusicDockButton(
            icon = SimpIcons.QueueMusic,
            active = viewState == AppleMusicView.QUEUE,
            activeColor = activeColor,
            activeContentColor = activeContentColor,
            onClick = {
                onSelectView(if (viewState == AppleMusicView.QUEUE) AppleMusicView.MAIN else AppleMusicView.QUEUE)
            },
        )
    }
}

/**
 * Progress bar + times + transport + volume + dock — the fixed block every Apple Music body
 * (MAIN, LYRICS, QUEUE) renders at the bottom, identically. On Desktop only the dock renders
 * (no slider/transport/volume), matching the `Platform.Android` gate the other two styles use.
 */
@Composable
internal fun AppleMusicBottomCluster(
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
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 8.dp)) {
        if (getPlatform() == Platform.Android) {
            // Fixed 14dp shell: the track swells on touch, but inside a CONSTANT footprint —
            // otherwise the growing slider re-measures this whole column and the artwork above
            // it visibly jumps. It also gives the bar a real 14dp touch target instead of 4dp.
            Box(modifier = Modifier.fillMaxWidth().height(14.dp), contentAlignment = Alignment.Center) {
                AppleMusicThinSlider(
                    value = state.sliderValue / 100f,
                    activeColor = if (state.timelineState.isCrossfading) state.sliderTrackColor else AppleMusicTrackActive,
                    onValueChange = { actions.onSliderChange(it * 100f) },
                    onValueChangeFinished = actions.onSliderChangeFinished,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            AppleMusicTimesRow(state = state, typography = typography, modifier = Modifier.padding(top = 8.dp))
            Spacer(modifier = Modifier.height(12.dp))
            AppleMusicTransportRow(
                controllerState = state.controllerState,
                loading = state.timelineState.loading,
                onUIEvent = actions.onUIEvent,
            )
            Spacer(modifier = Modifier.height(14.dp))
            deviceVolumeController?.let { controller ->
                AppleMusicVolumeRow(controller = controller)
                Spacer(modifier = Modifier.height(14.dp))
            }
        } else {
            Spacer(modifier = Modifier.height(16.dp))
        }
        AppleMusicDock(
            viewState = viewState,
            onSelectView = onSelectView,
            castState = state.castState,
            lyricsAvailable = state.screenData.lyricsData != null,
            activeColor = activePillContainer,
            activeContentColor = activePillContent,
        )
        Spacer(
            modifier =
                Modifier.height(
                    // Breathing room under the dock: the bare inset parked the icons right on the
                    // gesture bar.
                    with(localDensity) { WindowInsets.systemBars.getBottom(localDensity).toDp() } + 12.dp,
                ),
        )
    }
}
