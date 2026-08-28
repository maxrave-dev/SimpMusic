package com.maxrave.simpmusic.ui.screen.player.content.applemusic

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.Slider
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.TileMode
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
import org.jetbrains.compose.resources.stringResource
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.crossfading
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
        // The compact header — Lyrics, Queue, and the row under a running canvas — carries the
        // SIZES FullscreenLyricsSheet uses for the same job (LyricsView.kt: labelSmall over
        // bodySmall), while mainTitle/mainArtist stay 18/13 because they head the controller
        // layout. Borrow the size from labelSmall but keep the titleMedium ROLE: typo() bakes a
        // color into every role — title* carry titleColor, body*/label* carry bodyColor — so
        // switching the title to labelSmall outright would also switch it to the subtitle's grey.
        // The artist was already a body role, so bodySmall changes its size and nothing else.
        compactTitle = t.titleMedium.copy(fontSize = t.labelSmall.fontSize),
        compactArtist = t.bodySmall,
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
                modifier =
                    Modifier
                        .appleMusicPressInflate()
                        .size(24.dp)
                        .clip(CircleShape)
                        .clickable { actions.onAddToYouTubeLiked() },
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

/** 7dp track shared by the progress bar and the volume row — same visual language, only the color and callbacks differ. */
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
        targetValue = if (pressed || dragged) 14.dp else 7.dp,
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
            track = {
                // Hand-drawn instead of SliderDefaults.Track. M3 rounds each half of the track with
                // TWO different radii: the OUTER end with trackCornerSize (height/2 — fully round)
                // and the INNER end, where the two halves meet, with trackInsideCornerSize (2dp —
                // reads as square). At value 0 the inactive half owns the entire bar, so its square
                // inner end lands on the LEFT while its round outer end sits on the right — the
                // mismatched bar. Clipping ONE container and drawing the played portion inside it
                // keeps both ends of the bar equally round, and gives the played portion the flat
                // right edge Apple's own bar has.
                val fraction = value.coerceIn(0f, 1f)
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(trackHeight)
                            .clip(RoundedCornerShape(percent = 50))
                            .background(AppleMusicTrackInactive),
                ) {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(fraction)
                                .background(activeColor),
                    )
                }
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
    // `total` stays -1 until the player reports a duration, and SimpleMediaState.Ready carries
    // ONLY the duration — never a position. After a queue restore nothing is playing, and the
    // position poll only runs while isPlaying, so no later event arrives to correct it. Deriving
    // elapsed from total therefore zeroed BOTH numbers at once, which is why a restored queue read
    // 00:00 / -00:00: the played time was never actually unknown, TimeLine.current held it.
    val knownTotal = state.timelineState.total.takeIf { it > 0L }
    val elapsedMs =
        if (knownTotal != null) {
            // Still derived from the slider while the duration IS known, so this number tracks the
            // finger while scrubbing instead of waiting for the player to report the seek back.
            (knownTotal * (state.sliderValue / 100f)).roundToLong()
        } else {
            state.timelineState.current.coerceAtLeast(0L)
        }
    // Clamp BEFORE formatDuration — it renders any negative as "NA:NA", and the remaining time
    // must never show that at the end of a track whose length IS known. An UNKNOWN length is
    // exactly what that string is for, so null deliberately takes the negative path below.
    val remainingMs = knownTotal?.let { (it - elapsedMs).coerceAtLeast(0L) }
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = formatDuration(elapsedMs),
            style = typography.times,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Left,
        )
        // ONE slot holding two alternatives — and both of them stay composed, swapping on alpha
        // rather than on presence. AnimatedVisibility removes its content from the LAYOUT, so this
        // Box took the height of whichever state was up: the codec pill is a 15dp icon wrapped in
        // 4dp of vertical padding, "Crossfading" is bare text at the times style, and with NEITHER
        // showing (no codec, not crossfading) the Box collapsed to nothing at all. Every swap
        // therefore resized this row and shoved the whole transport below it up or down. Holding
        // both means the slot is always as tall as the tallest one, at any type scale, with no
        // measured constant to keep in sync. The cross-fade looks identical — alpha is what
        // fadeIn/fadeOut animated anyway.
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            // Sweep head for the "Crossfading" shimmer, 0..1. Runs UNCONDITIONALLY — put behind the
            // crossfade check it would restart from zero every time the label appears, which is the
            // same reason the other two styles declare it outside their own visibility gate.
            val sweepTransition = rememberInfiniteTransition(label = "appleMusicCrossfadeSweep")
            val crossfadeSweep by sweepTransition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec =
                    infiniteRepeatable(
                        animation = tween(3200, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart,
                    ),
                label = "appleMusicSweepHead",
            )
            val codec = state.audioCodecLabel
            val crossfadeLabelAlpha by animateFloatAsState(
                targetValue = if (state.timelineState.isCrossfading) 1f else 0f,
                label = "appleMusicCrossfadeLabelAlpha",
            )
            val codecBadgeAlpha by animateFloatAsState(
                targetValue = if (!state.timelineState.isCrossfading && codec != null) 1f else 0f,
                label = "appleMusicCodecBadgeAlpha",
            )
            Box(modifier = Modifier.alpha(crossfadeLabelAlpha)) {
                // Identical treatment to Classic and M3 Expressive: a highlight swept through the
                // glyphs with a text brush — no overlay, no clipping.
                val shimmerSpan = 140f
                val shimmerHead = crossfadeSweep * (shimmerSpan * 3f) - shimmerSpan
                val labelColor = typography.times.color
                Text(
                    text = stringResource(Res.string.crossfading),
                    style =
                        typography.times.copy(
                            brush =
                                Brush.horizontalGradient(
                                    0f to labelColor.copy(alpha = 0.45f),
                                    // The sweep head is PURE white, not the resting label colour — that
                                    // colour is an adaptive grey, and a grey gleam reads as no gleam.
                                    0.5f to Color.White,
                                    1f to labelColor.copy(alpha = 0.45f),
                                    startX = shimmerHead,
                                    endX = shimmerHead + shimmerSpan,
                                    tileMode = TileMode.Clamp,
                                ),
                        ),
                    textAlign = TextAlign.Center,
                )
            }
            // A PILL, like the mock's badge (and Apple's "Lossless"): translucent rounded
            // background, not bare text floating between the two times. It is the TALLER of the two
            // states, so it is what the slot's height ends up being — see the note above.
            Box(modifier = Modifier.alpha(codecBadgeAlpha)) {
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
                    // orEmpty(), not codec!!: the null check drives the alpha above rather than
                    // guarding this branch, so there is nothing here for the compiler to
                    // smart-cast. It also renders while alpha is 0, which is the point.
                    Text(text = codec.orEmpty(), style = typography.badge.copy(color = Color.White.copy(alpha = 0.9f)))
                }
            }
        }
        Text(
            // No leading "-" when the length is unknown: "-NA:NA" reads as a negative amount of
            // nothing. formatDuration's own out-of-range string is the app's established way to
            // say "no value here".
            text = remainingMs?.let { "-" + formatDuration(it) } ?: formatDuration(-1L),
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
        // No loading state on this button. It is play/pause and nothing else: it stays pressable
        // and keeps showing the transport glyph even while the player is buffering, so the control
        // never disappears out from under a finger reaching for it.
        Box(
            modifier =
                Modifier
                    .appleMusicPressInflate()
                    .size(76.dp)
                    .clip(CircleShape)
                    .clickable { onUIEvent(UIEvent.PlayPause) },
            contentAlignment = Alignment.Center,
        ) {
            Crossfade(targetState = controllerState.isPlaying, label = "appleMusicPlayPauseIcon") { isPlaying ->
                Icon(
                    imageVector = if (isPlaying) SimpIcons.Pause else SimpIcons.PlayArrow,
                    contentDescription = "",
                    tint = Color.White,
                    modifier = Modifier.size(66.dp),
                )
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
        Box(modifier = Modifier.weight(1f).height(18.dp), contentAlignment = Alignment.Center) {
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
            // Fixed 18dp shell: the track swells on touch, but inside a CONSTANT footprint —
            // otherwise the growing slider re-measures this whole column and the artwork above
            // it visibly jumps. It also gives the bar a real 18dp touch target instead of 7dp.
            Box(modifier = Modifier.fillMaxWidth().height(18.dp), contentAlignment = Alignment.Center) {
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