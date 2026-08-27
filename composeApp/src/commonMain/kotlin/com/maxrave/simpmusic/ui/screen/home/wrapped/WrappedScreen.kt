package com.maxrave.simpmusic.ui.screen.home.wrapped

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.kmpalette.loader.rememberNetworkLoader
import com.kmpalette.rememberDominantColorState
import com.kyant.backdrop.highlight.Highlight
import com.materialkolor.PaletteStyle
import com.materialkolor.rememberDynamicColorScheme
import com.maxrave.simpmusic.Platform
import com.maxrave.simpmusic.expect.saveImageToDevice
import com.maxrave.simpmusic.expect.shareImage
import com.maxrave.simpmusic.expect.ui.PlatformBackdrop
import com.maxrave.simpmusic.expect.ui.layerBackdrop
import com.maxrave.simpmusic.expect.ui.rememberBackdrop
import com.maxrave.simpmusic.expect.ui.rememberSaveImagePermission
import com.maxrave.simpmusic.expect.ui.toPngByteArray
import com.maxrave.simpmusic.getPlatform
import com.maxrave.simpmusic.ui.component.CenterLoadingBox
import com.maxrave.simpmusic.ui.component.LiquidGlassIconButton
import com.maxrave.simpmusic.ui.component.capture.CaptureController
import com.maxrave.simpmusic.ui.component.capture.capturable
import com.maxrave.simpmusic.ui.component.capture.rememberCaptureController
import com.maxrave.simpmusic.ui.component.liquidGlass
import com.maxrave.simpmusic.ui.icon.Close
import com.maxrave.simpmusic.ui.icon.Share
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.screen.home.wrapped.cards.WrappedBiggestDayCard
import com.maxrave.simpmusic.ui.screen.home.wrapped.cards.WrappedClockCard
import com.maxrave.simpmusic.ui.screen.home.wrapped.cards.WrappedDecadesCard
import com.maxrave.simpmusic.ui.screen.home.wrapped.cards.WrappedEyebrow
import com.maxrave.simpmusic.ui.screen.home.wrapped.cards.WrappedMinutesCard
import com.maxrave.simpmusic.ui.screen.home.wrapped.cards.WrappedOpeningCard
import com.maxrave.simpmusic.ui.screen.home.wrapped.cards.WrappedShareCard
import com.maxrave.simpmusic.ui.screen.home.wrapped.cards.WrappedTopAlbumsCard
import com.maxrave.simpmusic.ui.screen.home.wrapped.cards.WrappedTopArtistsCard
import com.maxrave.simpmusic.ui.screen.home.wrapped.cards.WrappedTopTracksCard
import com.maxrave.simpmusic.ui.screen.home.wrapped.cards.WrappedTypeCard
import com.maxrave.simpmusic.ui.theme.seed
import com.maxrave.simpmusic.viewModel.WrappedCard
import com.maxrave.simpmusic.viewModel.WrappedUiState
import com.maxrave.simpmusic.viewModel.WrappedViewModel
import com.maxrave.simpmusic.viewModel.WrappedYear
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.http.Url
import kotlinx.coroutines.launch
import multiplatform.network.cmptoast.ToastGravity
import multiplatform.network.cmptoast.showToast
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.share_lyrics_permission_denied
import simpmusic.composeapp.generated.resources.share_lyrics_save_failed
import simpmusic.composeapp.generated.resources.share_lyrics_saved
import simpmusic.composeapp.generated.resources.share_lyrics_saved_desktop
import simpmusic.composeapp.generated.resources.share_lyrics_share_failed
import simpmusic.composeapp.generated.resources.wrapped_share
import simpmusic.composeapp.generated.resources.wrapped_tap_to_begin
import simpmusic.composeapp.generated.resources.wrapped_tap_to_continue
import simpmusic.composeapp.generated.resources.wrapped_year
import kotlin.random.Random

/**
 * SimpMusic Wrapped: the year told as a story reel.
 *
 * The screen is a shell around cards it does not draw. Everything constant lives here — the
 * progress segments, the year label, the close button, the footer, the timer, the capture — and
 * each card fills the slot between them and does nothing else. That split is what lets ten cards
 * be written to ten different layouts without ten copies of the chrome drifting apart.
 *
 * Nothing here names a colour or builds a text style. The reel runs inside [WrappedTheme], and
 * every surface it draws is one of the app's own components.
 */
@Composable
fun WrappedScreen(
    navController: NavController,
    hideNavBar: () -> Unit,
    showNavBar: () -> Unit,
    wrappedViewModel: WrappedViewModel = koinViewModel(),
) {
    val state by wrappedViewModel.uiState.collectAsStateWithLifecycle()

    // The reel is a takeover, the way the fullscreen player is: the bottom bar would sit across
    // the footer's own hint and share pill, and the mini player rides in the same slot. Restoring
    // it on dispose rather than on the close button covers leaving by the system back gesture too.
    DisposableEffect(Unit) {
        hideNavBar()
        onDispose { showNavBar() }
    }

    WrappedTheme(
        artworkUrl = (state as? WrappedUiState.Ready)?.wrapped?.topTracks?.firstOrNull()?.song?.thumbnails,
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
        ) {
            when (val current = state) {
                is WrappedUiState.Loading -> CenterLoadingBox(modifier = Modifier.fillMaxSize())

                is WrappedUiState.NotEnoughData ->
                    WrappedNotEnoughDataScreen(
                        state = current,
                        onBack = { navController.navigateUp() },
                    )

                is WrappedUiState.Ready ->
                    WrappedReel(
                        wrapped = current.wrapped,
                        onClose = { navController.navigateUp() },
                    )
            }
        }
    }
}

/**
 * The reel's colour system: a whole dark tonal scheme grown from the user's own top-track artwork.
 *
 * Same construction as the M3 Expressive Now Playing style, and for the same reason — one seed
 * colour reaches every card, so a figure, an eyebrow and a chart axis are automatically related
 * without any of them naming a colour. [seed] is the app accent and stands in until the artwork
 * resolves, which is also what a year with no artwork at all keeps.
 *
 * `isDark = true` unconditionally, deliberately: every card is captured as a share image, and an
 * image that came out of a light-theme phone would not be the same object as one from a dark-theme
 * phone. The reel is a poster, not a page.
 */
@Composable
private fun WrappedTheme(
    artworkUrl: String?,
    content: @Composable () -> Unit,
) {
    // The same DominantColorState + network-loader machinery HomeScreen and the ambient glow run
    // on their own thumbnails: the artwork is fetched for its colour alone, so the shell never has
    // to render an image it does not show just to read a palette off it. The client is remembered
    // rather than constructed inline the way those two do it — this composable reads the resolved
    // colour during composition, so a fresh client every pass would key a fresh loader and a fresh
    // state, and the colour would reset to the fallback on the very recomposition it caused.
    val httpClient = remember { HttpClient(CIO) }
    val networkLoader = rememberNetworkLoader(httpClient)
    val dominantColorState =
        rememberDominantColorState(
            defaultColor = seed,
            defaultOnColor = seed,
            loader = networkLoader,
        )
    LaunchedEffect(artworkUrl) {
        artworkUrl?.takeIf { it.isNotBlank() }?.let { dominantColorState.updateFrom(Url(it)) }
    }
    // The seed arrives in one step, and a whole tonal scheme snapping at once reads as a flash.
    val animatedSeed by animateColorAsState(
        targetValue = dominantColorState.color,
        animationSpec = tween(durationMillis = 800),
        label = "wrappedSeed",
    )
    MaterialExpressiveTheme(
        colorScheme =
            rememberDynamicColorScheme(
                seedColor = animatedSeed,
                isDark = true,
                isAmoled = false,
                style = PaletteStyle.Vibrant,
            ),
        // Typography is deliberately not passed: MaterialExpressiveTheme inherits the ambient one,
        // which is the ForceDarkContent typography the nav graph wraps this destination in. That is
        // what makes titles land white over muted body copy without a card asking for a colour.
        content = content,
    )
}

/**
 * The reel itself.
 *
 * The pager is the only place the current card is recorded. The timer does not keep an index of
 * its own — it is keyed on [androidx.compose.foundation.pager.PagerState.currentPage] and restarts
 * whenever that moves, so a swipe, a tap and an expiring timer all reach the same card by the same
 * route. Two counters here would drift the moment a swipe and a timeout landed in the same frame.
 */
@Composable
private fun WrappedReel(
    wrapped: WrappedYear,
    onClose: () -> Unit,
) {
    val cards = remember(wrapped) { wrapped.cards }
    val pagerState = rememberPagerState(pageCount = { cards.size })
    val scope = rememberCoroutineScope()

    val page = pagerState.currentPage
    val isShareCard = cards[page] == WrappedCard.SHARE

    // How far through the current card's hold we are, 0..1. Held as a float state and read only
    // inside the segment's progress lambda, so sixty writes a second repaint three pixels of bar
    // instead of recomposing the card on top of them.
    var progress by remember { mutableFloatStateOf(0f) }

    // Set for as long as a finger is down anywhere on the reel. A press pauses; the release
    // resumes. This is the gesture people already expect from every story UI they use.
    var pressed by remember { mutableStateOf(false) }

    // Set while a save or a share is in flight, which also freezes the timer: the reel advancing
    // underneath a share sheet would hand the next card's image to whatever the user picked.
    var busy by remember { mutableStateOf(false) }

    val captureController = rememberCaptureController()
    val share = rememberWrappedShare(wrapped, captureController) { busy = it }

    // What the close button and the share pill refract. The SOURCE is the card layer below; the
    // two glass surfaces are its SIBLINGS, never its children — nesting a glass surface inside the
    // layer it samples is the render-feedback loop that kills the RuntimeShader. The ground colour
    // is handed to the backdrop itself rather than painted into the source, so the chrome still has
    // something to refract over the header strip, which no card reaches.
    val backdrop = rememberBackdrop(MaterialTheme.colorScheme.background)

    /**
     * The one way the reel moves. Past the last card is not a page — it is the end of the story,
     * and the reel closes rather than sitting on a card with nothing after it.
     */
    fun goTo(target: Int) {
        when {
            target < 0 -> Unit
            target >= cards.size -> onClose()
            else -> scope.launch { pagerState.animateScrollToPage(target) }
        }
    }

    LaunchedEffect(page, cards) {
        progress = 0f
        val holdMs = if (cards[page] == WrappedCard.SHARE) WrappedTokens.SHARE_CARD_DURATION_MS else WrappedTokens.CARD_DURATION_MS
        var elapsed = 0L
        var lastFrame = 0L
        while (elapsed < holdMs) {
            withFrameMillis { frame ->
                // Frozen time still moves the clock forward, it just does not count: taking the
                // delta from the previous frame regardless is what stops a long press from
                // banking a second's worth of progress and consuming the card on release.
                val delta = if (lastFrame == 0L) 0L else frame - lastFrame
                lastFrame = frame
                if (!pressed && !busy && !pagerState.isScrollInProgress) elapsed += delta
            }
            progress = (elapsed.toFloat() / holdMs).coerceIn(0f, 1f)
        }
        goTo(page + 1)
    }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .pointerInput(cards) {
                    detectTapGestures(
                        onPress = {
                            pressed = true
                            // Returns on the finger lifting AND on the gesture being cancelled —
                            // which is what a drag the pager took over looks like from here — so
                            // the reel can never be left paused with nothing holding it.
                            tryAwaitRelease()
                            pressed = false
                        },
                        // Deliberately empty, and deliberately not absent: supplying it at all is
                        // what makes detectTapGestures treat a long press as its own gesture. With
                        // it null, holding and then releasing arrives as an ordinary tap and the
                        // card the user was pausing to read advances the instant they let go.
                        onLongPress = { },
                        onTap = { offset ->
                            // targetPage, not currentPage: a second tap during the first tap's
                            // animation must step past where that one is going, not re-issue it.
                            val from = pagerState.targetPage
                            goTo(if (offset.x > size.width / 2f) from + 1 else from - 1)
                        },
                    )
                },
    ) {
        // Everything the glass samples. matchParentSize, so it takes part in no measurement and the
        // chrome above it is laid out against the reel's own bounds.
        Box(modifier = Modifier.matchParentSize().layerBackdrop(backdrop)) {
            // Only the header is subtracted here. The footer band is subtracted inside each page
            // instead, because the share card is the one card that needs it back — and taking it
            // off the pager's own box would resize every page the moment the pager decided card 10
            // was the current one, which is halfway through the swipe that brings it in.
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.systemBars)
                        .padding(top = WrappedTokens.HeaderHeight),
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    key = { cards[it].name },
                ) { index ->
                    WrappedCardSlot(
                        card = cards[index],
                        wrapped = wrapped,
                        captureController = captureController,
                        onSave = share.onSave,
                        onShare = share.onShare,
                    )
                }
            }

            // Sits over the card and under the header, exactly as the artboards layer it: the cards
            // are artwork-led and the year label has to stay readable over whatever a given year's
            // covers turn out to be. Ramped from the scheme's own ground rather than to
            // Color.Transparent, which is black at alpha 0 and drags the middle of the ramp grey.
            val ground = MaterialTheme.colorScheme.background
            Box(
                modifier =
                    Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .height(SCRIM_HEIGHT)
                        .background(
                            Brush.verticalGradient(
                                0f to ground.copy(alpha = 0.62f),
                                0.52f to ground.copy(alpha = 0.28f),
                                1f to ground.copy(alpha = 0f),
                            ),
                        ),
            )
        }

        WrappedHeader(
            year = wrapped.year,
            cardCount = cards.size,
            currentCard = page,
            progress = { progress },
            backdrop = backdrop,
            onClose = onClose,
            modifier = Modifier.align(Alignment.TopCenter),
        )

        WrappedFooter(
            isFirstCard = page == 0,
            // The share card carries its own Save and Share, so the footer stands down entirely
            // there rather than offering a third button and a hint to tap past the thing the user
            // was just invited to act on.
            showActions = !isShareCard,
            backdrop = backdrop,
            onShare = share.onShare,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

/**
 * One page of the reel.
 *
 * The capture region is the card, never the shell: an image with a progress bar and a close button
 * printed in it is not the card the user meant to send. The share card is the exception, and the
 * reason is the same rule applied one level in — its own Save and Share pills sit inside its slot,
 * so the capture is handed to the poster it draws rather than to the whole page.
 */
@Composable
private fun WrappedCardSlot(
    card: WrappedCard,
    wrapped: WrappedYear,
    captureController: CaptureController,
    onSave: () -> Unit,
    onShare: () -> Unit,
) {
    val isShareCard = card == WrappedCard.SHARE
    val capture = Modifier.capturable(captureController)
    val slot =
        Modifier
            .fillMaxSize()
            // The share card owns the footer band as well, and gets it here rather than from the
            // pager's box so no other page changes size when it arrives. The footer draws nothing
            // over it, so its Save and Share pills land where every other card's footer row sits.
            // Applied BEFORE background and capture, so both see the slot's real bounds — after
            // them, a captured card would carry 74dp of empty ground along the bottom.
            .padding(bottom = if (isShareCard) 0.dp else WrappedTokens.FooterHeight)
            .background(MaterialTheme.colorScheme.background)
            .then(if (isShareCard) Modifier else capture)

    when (card) {
        WrappedCard.OPENING -> WrappedOpeningCard(wrapped = wrapped, modifier = slot)
        WrappedCard.MINUTES -> WrappedMinutesCard(wrapped = wrapped, modifier = slot)
        WrappedCard.TOP_TRACKS -> WrappedTopTracksCard(wrapped = wrapped, modifier = slot)
        WrappedCard.TOP_ARTISTS -> WrappedTopArtistsCard(wrapped = wrapped, modifier = slot)
        WrappedCard.TOP_ALBUMS -> WrappedTopAlbumsCard(wrapped = wrapped, modifier = slot)
        WrappedCard.CLOCK -> WrappedClockCard(wrapped = wrapped, modifier = slot)
        WrappedCard.BIGGEST_DAY -> WrappedBiggestDayCard(wrapped = wrapped, modifier = slot)
        WrappedCard.TYPE -> WrappedTypeCard(wrapped = wrapped, modifier = slot)
        WrappedCard.DECADES -> WrappedDecadesCard(wrapped = wrapped, modifier = slot)
        WrappedCard.SHARE ->
            WrappedShareCard(
                wrapped = wrapped,
                onSave = onSave,
                onShare = onShare,
                modifier = slot,
                posterModifier = capture,
            )
    }
}

/** Progress segments, the year, and the way out. */
@Composable
private fun WrappedHeader(
    year: Int,
    cardCount: Int,
    currentCard: Int,
    progress: () -> Float,
    backdrop: PlatformBackdrop,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .height(WrappedTokens.HeaderHeight),
    ) {
        Spacer(modifier = Modifier.height(SEGMENT_TOP_INSET))
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(WrappedTokens.SegmentGap),
        ) {
            repeat(cardCount) { index ->
                // A real progress indicator rather than a hand-drawn rect, and the same one for all
                // three states — a spent card is simply a full one. The fraction is read inside the
                // indicator's own draw, so the sixty writes a second the timer makes never reach
                // composition; the gap and the stop indicator are turned off because at 3dp they
                // are the whole segment rather than a detail of it.
                LinearProgressIndicator(
                    progress = {
                        when {
                            index < currentCard -> 1f
                            index == currentCard -> progress().coerceIn(0f, 1f)
                            else -> 0f
                        }
                    },
                    color = MaterialTheme.colorScheme.onSurface,
                    trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = SEGMENT_TRACK_ALPHA),
                    strokeCap = StrokeCap.Round,
                    gapSize = 0.dp,
                    drawStopIndicator = {},
                    modifier =
                        Modifier
                            .weight(1f)
                            .height(WrappedTokens.SegmentHeight),
                )
            }
        }
        Spacer(modifier = Modifier.height(SEGMENT_BOTTOM_INSET))
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).height(CLOSE_BUTTON_SIZE),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // The reel's eyebrow, drawn by the cards' own composable rather than restyled here.
            // Matching it by hand is what let the header and the cards drift a scale step apart
            // once already; one definition cannot. `wrapped_year` is rendered nowhere but Wrapped,
            // which is the condition for uppercasing a translated string in code at all.
            WrappedEyebrow(
                text = stringResource(Res.string.wrapped_year, year.toString()),
                modifier = Modifier.weight(1f),
            )
            LiquidGlassIconButton(
                backdrop = backdrop,
                imageVector = SimpIcons.Close,
                tint = MaterialTheme.colorScheme.onSurface,
                // Round buttons need the rim named: Highlight's default is a DIRECTIONAL sweep that
                // an elongated pill catches along its long edge and a circle barely catches at all.
                highlight = Highlight(width = WrappedTokens.GlassRimWidth),
                modifier = Modifier.size(CLOSE_BUTTON_SIZE),
                onClick = onClose,
            )
        }
    }
}

/** The hint on the left, the way to send this card on the right. */
@Composable
private fun WrappedFooter(
    isFirstCard: Boolean,
    showActions: Boolean,
    backdrop: PlatformBackdrop,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!showActions) return

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.systemBars)
                .height(WrappedTokens.FooterHeight)
                // The row is not centred in the band: the artboard hangs the pill from the top of
                // it with 34dp underneath, which leaves the 40dp pill filling the remainder
                // exactly. Centring it instead drops everything 17dp and closes that gap.
                .padding(start = 24.dp, end = 24.dp, bottom = 34.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text =
                stringResource(
                    if (isFirstCard) Res.string.wrapped_tap_to_begin else Res.string.wrapped_tap_to_continue,
                ),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f),
        )
        // The same glass pill the Analytics day-range trigger is: liquidGlass for the surface, then
        // clip + clickable so the ripple and the hit target follow the same shape. Highlight is left
        // at its directional default here — an elongated pill is exactly the geometry that catches
        // the sweep along its long edge, which is what makes it read as glass rather than as a rim.
        Row(
            modifier =
                Modifier
                    .height(SHARE_PILL_HEIGHT)
                    .liquidGlass(backdrop, RoundedCornerShape(SHARE_PILL_HEIGHT / 2))
                    .clip(RoundedCornerShape(SHARE_PILL_HEIGHT / 2))
                    .clickable(onClick = onShare)
                    .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = SimpIcons.Share,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(15.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(Res.string.wrapped_share),
                style = MaterialTheme.typography.titleSmall,
            )
        }
    }
}

/** The two things the reel can do with the card that is on screen. */
private class WrappedShareActions(
    val onSave: () -> Unit,
    val onShare: () -> Unit,
)

/**
 * Save and share, wired to whichever node currently holds [captureController].
 *
 * Follows [com.maxrave.simpmusic.ui.component.lyrics.ShareLyricsSheet] rather than inventing a
 * second pipeline: the same controller, the same permission gate, the same five result strings —
 * a Wrapped card and a lyric card end up in the same place by the same route, so they say the same
 * thing when they get there.
 */
@Composable
private fun rememberWrappedShare(
    wrapped: WrappedYear,
    captureController: CaptureController,
    onBusy: (Boolean) -> Unit,
): WrappedShareActions {
    val scope = rememberCoroutineScope()

    val savedMessage =
        stringResource(
            if (getPlatform() == Platform.Desktop) Res.string.share_lyrics_saved_desktop else Res.string.share_lyrics_saved,
        )
    val saveFailedMessage = stringResource(Res.string.share_lyrics_save_failed)
    val shareFailedMessage = stringResource(Res.string.share_lyrics_share_failed)
    val permissionDeniedMessage = stringResource(Res.string.share_lyrics_permission_denied)
    val chooserTitle = stringResource(Res.string.wrapped_year, wrapped.year.toString())

    // Random suffix for the same reason the lyric card carries one: saving two cards from the same
    // reel must not silently overwrite the first.
    val fileName =
        remember(wrapped.year) {
            "SimpMusic_Wrapped_${wrapped.year}_${Random.nextInt(100_000, 999_999)}.png"
        }

    // Only saving can be refused; sharing goes through the app's own cache and needs nothing. On
    // Android 10 and up, and on Desktop, this grants without a dialog.
    val savePermission =
        rememberSaveImagePermission { granted ->
            if (!granted) {
                showToast(permissionDeniedMessage, ToastGravity.Bottom)
                onBusy(false)
                return@rememberSaveImagePermission
            }
            scope.launch {
                val bytes = captureController.captureAsync().await().toPngByteArray()
                val ok = bytes != null && saveImageToDevice(bytes, fileName)
                showToast(if (ok) savedMessage else saveFailedMessage, ToastGravity.Bottom)
                onBusy(false)
            }
        }

    return remember(savePermission, fileName, chooserTitle, shareFailedMessage) {
        WrappedShareActions(
            onSave = {
                onBusy(true)
                // The save itself lives in the permission callback, which always fires exactly
                // once, so busy is cleared on every path out of it.
                savePermission.requestIfNeeded()
            },
            onShare = {
                onBusy(true)
                scope.launch {
                    val bytes = captureController.captureAsync().await().toPngByteArray()
                    val ok = bytes != null && shareImage(bytes, fileName, chooserTitle)
                    if (!ok) showToast(shareFailedMessage, ToastGravity.Bottom)
                    onBusy(false)
                }
            },
        )
    }
}

/** How far the ground colour reaches down over the first card, so the year label stays readable. */
private val SCRIM_HEIGHT = 118.dp

/**
 * The header's own rhythm, and it has to add up.
 *
 * 12 + 3 + 9 + 48 is exactly [WrappedTokens.HeaderHeight], which the card slot is also offset by —
 * a taller close button here would be clipped rather than pushing the cards down. 48dp is the size
 * the app's other glass buttons are, and is not free to change: the icon button inside expands to
 * the platform minimum touch target, so a smaller container crops its own ripple.
 */
private val SEGMENT_TOP_INSET = 12.dp
private val SEGMENT_BOTTOM_INSET = 9.dp
private val CLOSE_BUTTON_SIZE = 48.dp

/** Matches the footer band's 40dp remainder after its 34dp bottom inset. */
private val SHARE_PILL_HEIGHT = 40.dp


/** Spent and unspent segments are the same ink; the unspent ones are simply held back. */
private const val SEGMENT_TRACK_ALPHA = 0.24f
