package com.maxrave.simpmusic.ui.screen.other

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.MarqueeAnimationMode
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.PersonAddAlt1
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.maxrave.common.Config
import com.maxrave.domain.data.model.browse.album.Track
import com.maxrave.domain.data.model.home.Content
import com.maxrave.domain.data.model.searchResult.songs.Artist
import com.maxrave.domain.mediaservice.handler.PlaylistType
import com.maxrave.domain.mediaservice.handler.QueueData
import com.maxrave.domain.utils.toSongEntity
import com.maxrave.domain.utils.toTrack
import com.maxrave.simpmusic.Platform
import com.maxrave.simpmusic.expect.ui.MediaPlayerView
import com.maxrave.simpmusic.expect.ui.layerBackdrop
import com.maxrave.simpmusic.expect.ui.rememberBackdrop
import com.maxrave.simpmusic.expect.ui.toImageBitmap
import com.maxrave.simpmusic.extension.getColorFromPalette
import com.maxrave.simpmusic.extension.getScreenSizeInfo
import com.maxrave.simpmusic.extension.getStringBlocking
import com.maxrave.simpmusic.extension.hexToColorOrNull
import com.maxrave.simpmusic.extension.rgbFactor
import com.maxrave.simpmusic.extension.toImmersiveBackground
import com.maxrave.simpmusic.extension.toSquareThumbnailUrl
import com.maxrave.simpmusic.getPlatform
import com.maxrave.simpmusic.ui.component.CenterLoadingBox
import com.maxrave.simpmusic.ui.component.CollapsingToolbarParallaxEffect
import com.maxrave.simpmusic.ui.component.DescriptionView
import com.maxrave.simpmusic.ui.component.EndOfPage
import com.maxrave.simpmusic.ui.component.HomeItemArtist
import com.maxrave.simpmusic.ui.component.HomeItemContentPlaylist
import com.maxrave.simpmusic.ui.component.HomeItemVideo
import com.maxrave.simpmusic.ui.component.LimitedBorderAnimationView
import com.maxrave.simpmusic.ui.component.LiquidGlassIconButton
import com.maxrave.simpmusic.ui.component.NowPlayingBottomSheet
import com.maxrave.simpmusic.ui.component.SongFullWidthItems
import com.maxrave.simpmusic.ui.navigation.destination.list.AlbumDestination
import com.maxrave.simpmusic.ui.navigation.destination.list.ArtistDestination
import com.maxrave.simpmusic.ui.navigation.destination.list.MoreAlbumsDestination
import com.maxrave.simpmusic.ui.navigation.destination.list.PlaylistDestination
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.ArtistScreenState
import com.maxrave.simpmusic.viewModel.ArtistViewModel
import com.maxrave.simpmusic.viewModel.SharedViewModel
import dev.chrisbanes.haze.HazeProgressive
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.albums
import simpmusic.composeapp.generated.resources.baseline_arrow_back_ios_new_24
import simpmusic.composeapp.generated.resources.description
import simpmusic.composeapp.generated.resources.error
import simpmusic.composeapp.generated.resources.featured_inArtist
import simpmusic.composeapp.generated.resources.follow
import simpmusic.composeapp.generated.resources.followed
import simpmusic.composeapp.generated.resources.holder
import simpmusic.composeapp.generated.resources.more
import simpmusic.composeapp.generated.resources.no_description
import simpmusic.composeapp.generated.resources.popular
import simpmusic.composeapp.generated.resources.related_artists
import simpmusic.composeapp.generated.resources.singles
import simpmusic.composeapp.generated.resources.start_radio
import simpmusic.composeapp.generated.resources.unknown
import simpmusic.composeapp.generated.resources.videos

@Composable
@ExperimentalMaterial3Api
fun ArtistScreen(
    channelId: String,
    viewModel: ArtistViewModel = koinViewModel(),
    sharedViewModel: SharedViewModel = koinInject(),
    navController: NavController,
) {
    val artistScreenState by viewModel.artistScreenState.collectAsStateWithLifecycle()
    val isFollowed by viewModel.followed.collectAsStateWithLifecycle()
    val canvasUrl by viewModel.canvasUrl.collectAsStateWithLifecycle()
    val artistLogo by viewModel.artistLogo.collectAsStateWithLifecycle()

    val playingTrack by remember {
        sharedViewModel.nowPlayingState.map { it?.track?.videoId }
    }.collectAsState(null)

    // Choosing song to show Bottom sheet
    var choosingTrack by remember {
        mutableStateOf<Track?>(null)
    }
    var showBottomSheet by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(channelId) {
        if (channelId != artistScreenState.data.channelId) {
            viewModel.browseArtist(channelId)
        }
    }

    // Apple Music-inspired immersive treatment: gated to mobile portrait so tablets,
    // foldable open state, landscape orientation, and Desktop keep the existing layout.
    val screenInfo = getScreenSizeInfo()
    val isMobilePortrait = getPlatform() == Platform.Android && screenInfo.wDP < screenInfo.hDP

    // Palette extraction from the artist artwork (portrait Apple-style only).
    val paletteState = com.kmpalette.rememberPaletteState()
    var bitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var paletteGeneratedFor by remember { mutableStateOf<String?>(null) }
    val currentImageUrl = (artistScreenState as? ArtistScreenState.Success)?.data?.imageUrl

    LaunchedEffect(bitmap) {
        val bm = bitmap
        if (bm != null && currentImageUrl != null && paletteGeneratedFor != currentImageUrl) {
            paletteState.generate(bm)
            paletteGeneratedFor = currentImageUrl
        }
    }

    // Apple Music-style page background from the artwork's dominant tone (see UIExt.toImmersiveBackground).
    val mutedPaletteBg = paletteState.palette.toImmersiveBackground()
    // Tint for the description card, matching the non-portrait CollapsingToolbar color.
    val sectionTint = paletteState.palette.getColorFromPalette()

    // Accent color for the action buttons, sourced from the artist name-logo image's dominant
    // color (hidden catalog). Falls back to white until the logo loads (or if none exists).
    val artistAccent = artistLogo?.bgColorHex?.hexToColorOrNull() ?: Color.White

    val hazeState = rememberHazeState(blurEnabled = true)
    val lazyState = rememberLazyListState()
    val firstItemVisible by remember {
        derivedStateOf { lazyState.firstVisibleItemIndex == 0 }
    }
    var shouldHideTopBar by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(firstItemVisible) {
        shouldHideTopBar = !firstItemVisible
    }

    Crossfade(artistScreenState) { state ->
        when (state) {
            is ArtistScreenState.Loading -> {
                Box(Modifier.fillMaxSize()) {
                    CenterLoadingBox(
                        Modifier
                            .align(Alignment.Center),
                    )
                }
            }

            is ArtistScreenState.Success -> {
                if (isMobilePortrait) {
                    // ---- Apple Music style (mobile portrait only) ----
                    Box(Modifier.fillMaxSize()) {
                        LazyColumn(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .background(mutedPaletteBg)
                                    .hazeSource(hazeState),
                            state = lazyState,
                        ) {
                            item(contentType = "header") {
                                Column(
                                    // Negative spacing pulls the action row up into the header AND
                                    // shrinks the layout, so there's no leftover gap before "Popular"
                                    // (unlike Modifier.offset, which only moves pixels, not layout).
                                    verticalArrangement = Arrangement.spacedBy((-36).dp),
                                ) {
                                    // Edge-to-edge artwork (canvas plays on top of it when available).
                                    // Glass back button MUST be a sibling of the backdrop source
                                    // (not a child) to avoid render feedback loop / RuntimeShader crash.
                                    val artworkBackdrop = rememberBackdrop()
                                    // Haze state for the bottom progressive-blur fade (source = media layer).
                                    val headerHaze = rememberHazeState(blurEnabled = true)
                                    // Clamp the artist thumbnail URL to a square size (logic from
                                    // commit 5e596c5b) so it fills the square frame with FillWidth.
                                    val headerImageUrl = state.data.imageUrl?.toSquareThumbnailUrl()
                                    Box(
                                        modifier =
                                            Modifier
                                                .fillMaxWidth()
                                                .aspectRatio(1f),
                                    ) {
                                        // Inner Box — backdrop SOURCE (artwork + canvas + overlays, NO glass)
                                        Box(modifier = Modifier.fillMaxSize().clipToBounds().layerBackdrop(artworkBackdrop)) {
                                            // Media layer (artwork + canvas) — Haze SOURCE for the bottom blur.
                                            Box(modifier = Modifier.fillMaxSize().hazeSource(headerHaze)) {
                                                AsyncImage(
                                                    model =
                                                        ImageRequest
                                                            .Builder(LocalPlatformContext.current)
                                                            .data(headerImageUrl)
                                                            .diskCachePolicy(CachePolicy.ENABLED)
                                                            .memoryCachePolicy(CachePolicy.ENABLED)
                                                            .diskCacheKey(headerImageUrl)
                                                            .memoryCacheKey(headerImageUrl)
                                                            .crossfade(false)
                                                            .build(),
                                                    placeholder =
                                                        org.jetbrains.compose.resources
                                                            .painterResource(Res.drawable.holder),
                                                    error =
                                                        org.jetbrains.compose.resources
                                                            .painterResource(Res.drawable.holder),
                                                    contentDescription = null,
                                                    contentScale = ContentScale.FillWidth,
                                                    // Always decoded so the page background color can be extracted
                                                    // from the artwork palette, even when a canvas is playing.
                                                    onSuccess = {
                                                        bitmap = it.result.image.toImageBitmap()
                                                    },
                                                    // Hidden (but still decoded above) while a canvas is present —
                                                    // the canvas is shown instead. No canvas -> artwork is shown.
                                                    modifier =
                                                        Modifier
                                                            .fillMaxSize()
                                                            .alpha(if (canvasUrl != null) 0f else 1f),
                                                )
                                                // Canvas (Spotify) plays AS the background when present;
                                                // otherwise the static artwork above is the fallback.
                                                canvasUrl?.let { canvas ->
                                                    // Canvas is a tall/portrait video. cropToBounds center
                                                    // scale-to-covers it into the square frame (ContentScale.Crop):
                                                    // true video aspect ratio, no stretch, overflow clipped.
                                                    MediaPlayerView(
                                                        url = canvas.first,
                                                        modifier = Modifier.fillMaxSize(),
                                                        cropToBounds = true,
                                                    )
                                                }
                                            } // end media layer (Haze source)
                                            // Bottom fade — progressive blur (Haze) over the media layer plus a
                                            // color gradient, so the canvas/artwork edge melts into the page bg.
                                            Box(
                                                modifier =
                                                    Modifier
                                                        .fillMaxWidth()
                                                        .height(200.dp)
                                                        .align(Alignment.BottomCenter)
                                                        .hazeEffect(headerHaze) {
                                                            blurRadius = 32.dp
                                                            progressive =
                                                                HazeProgressive.verticalGradient(
                                                                    startIntensity = 0f,
                                                                    endIntensity = 1f,
                                                                )
                                                        }.background(
                                                            Brush.verticalGradient(
                                                                listOf(
                                                                    Color.Transparent,
                                                                    Color.Transparent,
                                                                    mutedPaletteBg.copy(alpha = 0.5f),
                                                                    mutedPaletteBg,
                                                                ),
                                                            ),
                                                        ),
                                            )
                                            // Artist name (TEXT for now — logo image is roadmap) + subscriber · view
                                            Column(
                                                modifier =
                                                    Modifier
                                                        .align(Alignment.BottomCenter)
                                                        // Lift the name + subtitle together with the action row.
                                                        .offset(y = (-36).dp)
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 20.dp)
                                                        .padding(bottom = 16.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                            ) {
                                                val logo = artistLogo
                                                if (logo != null) {
                                                    // Artist name rendered as a logo image (hidden catalog),
                                                    // in place of the plain-text title.
                                                    AsyncImage(
                                                        model = logo.logoUrl,
                                                        contentDescription = state.data.title,
                                                        contentScale = ContentScale.Fit,
                                                        modifier =
                                                            Modifier
                                                                .fillMaxWidth(0.7f)
                                                                .heightIn(max = 84.dp),
                                                    )
                                                } else {
                                                    Text(
                                                        text = state.data.title ?: stringResource(Res.string.unknown),
                                                        style = typo().titleLarge,
                                                        color = Color.White,
                                                        maxLines = 2,
                                                        textAlign = TextAlign.Center,
                                                    )
                                                }
                                                val meta =
                                                    listOfNotNull(
                                                        state.data.subscribers?.takeIf { it.isNotBlank() },
                                                        state.data.playCount?.takeIf { it.isNotBlank() },
                                                    ).joinToString(" · ")
                                                if (meta.isNotBlank()) {
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        text = meta,
                                                        style = typo().bodyMedium,
                                                        color = Color(0xC4FFFFFF),
                                                        textAlign = TextAlign.Center,
                                                    )
                                                }
                                            }
                                        }
                                        // Back button — liquid glass, sibling of the backdrop source.
                                        LiquidGlassIconButton(
                                            backdrop = artworkBackdrop,
                                            resId = Res.drawable.baseline_arrow_back_ios_new_24,
                                            modifier =
                                                Modifier
                                                    .align(Alignment.TopStart)
                                                    .padding(12.dp)
                                                    .windowInsetsPadding(WindowInsets.statusBars)
                                                    .size(48.dp),
                                        ) {
                                            navController.navigateUp()
                                        }
                                    }

                                    // Apple Music-style action row: [Radio][Shuffle pill][Follow] centered.
                                    // In SimpMusic "play" an artist == shuffle, so the big middle button is Shuffle.
                                    Row(
                                        modifier =
                                            Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 32.dp)
                                                .padding(vertical = 8.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        // Radio — side button: outlined accent (yellow) circle with an
                                        // accent-tinted icon over a transparent fill, matching the reference.
                                        Box(
                                            modifier =
                                                Modifier
                                                    .size(48.dp)
                                                    .clip(CircleShape)
                                                    .border(1.5.dp, artistAccent, CircleShape)
                                                    .clickable {
                                                        val param = state.data.radioParam
                                                        if (param != null) {
                                                            viewModel.onRadioClick(param)
                                                        } else {
                                                            viewModel.makeToast(runBlocking { getString(Res.string.error) })
                                                        }
                                                    },
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Sensors,
                                                contentDescription = "Radio",
                                                tint = artistAccent,
                                                modifier = Modifier.size(22.dp),
                                            )
                                        }
                                        // Shuffle — primary "play" for an artist. Circular icon button filled
                                        // with the artist accent (white fallback); icon uses the dark page
                                        // background color so it stays legible on a bright accent.
                                        Box(
                                            modifier =
                                                Modifier
                                                    .size(64.dp)
                                                    .clip(CircleShape)
                                                    .background(artistAccent)
                                                    .clickable {
                                                        val param = state.data.shuffleParam
                                                        if (param != null) {
                                                            viewModel.onShuffleClick(param)
                                                        } else {
                                                            viewModel.makeToast(runBlocking { getString(Res.string.error) })
                                                        }
                                                    },
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Shuffle,
                                                contentDescription = "Shuffle",
                                                tint = mutedPaletteBg,
                                                modifier = Modifier.size(28.dp),
                                            )
                                        }
                                        // Follow — side button matching Radio: outlined accent (yellow)
                                        // circle when not following; fills with the accent (icon flips to
                                        // the dark page bg) once followed, so the state reads at a glance.
                                        Box(
                                            modifier =
                                                Modifier
                                                    .size(48.dp)
                                                    .clip(CircleShape)
                                                    .background(if (isFollowed) artistAccent else Color.Transparent)
                                                    .border(1.5.dp, artistAccent, CircleShape)
                                                    .clickable {
                                                        viewModel.updateFollowed(
                                                            if (isFollowed) 0 else 1,
                                                            state.data.channelId ?: return@clickable,
                                                        )
                                                    },
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Icon(
                                                imageVector = if (isFollowed) Icons.Rounded.Check else Icons.Rounded.PersonAddAlt1,
                                                contentDescription = if (isFollowed) "Followed" else "Follow",
                                                tint = if (isFollowed) mutedPaletteBg else artistAccent,
                                                modifier = Modifier.size(22.dp),
                                            )
                                        }
                                    }
                                }
                            }
                            item(contentType = "sections") {
                                ArtistSections(
                                    state = state,
                                    playingTrack = playingTrack,
                                    descriptionTint = sectionTint,
                                    navController = navController,
                                    viewModel = viewModel,
                                    sharedViewModel = sharedViewModel,
                                    onTrackMore = { track ->
                                        choosingTrack = track
                                        showBottomSheet = true
                                    },
                                )
                            }
                        }

                        // Haze top bar appears once the header scrolls away.
                        AnimatedVisibility(
                            visible = shouldHideTopBar,
                            enter = fadeIn() + slideInVertically(),
                            exit = fadeOut() + slideOutVertically(),
                        ) {
                            TopAppBar(
                                title = {
                                    Text(
                                        text = state.data.title ?: "",
                                        style = typo().titleMedium,
                                        maxLines = 1,
                                        modifier =
                                            Modifier
                                                .fillMaxWidth()
                                                .wrapContentHeight(align = Alignment.CenterVertically)
                                                .basicMarquee(
                                                    iterations = Int.MAX_VALUE,
                                                    animationMode = MarqueeAnimationMode.Immediately,
                                                ).focusable(),
                                    )
                                },
                                navigationIcon = {
                                    Box(Modifier.padding(horizontal = 5.dp)) {
                                        IconButton(onClick = { navController.navigateUp() }) {
                                            Icon(
                                                painter =
                                                    org.jetbrains.compose.resources.painterResource(
                                                        Res.drawable.baseline_arrow_back_ios_new_24,
                                                    ),
                                                contentDescription = "Back",
                                                tint = Color.White,
                                                modifier = Modifier.size(20.dp),
                                            )
                                        }
                                    }
                                },
                                colors =
                                    TopAppBarDefaults.topAppBarColors(
                                        containerColor = Color.Transparent,
                                    ),
                                modifier =
                                    Modifier.hazeEffect(hazeState) {
                                        blurEnabled = true
                                        blurRadius = 24.dp
                                        backgroundColor = mutedPaletteBg
                                        tints = listOf(HazeTint(mutedPaletteBg.copy(alpha = 0.55f)))
                                    },
                            )
                        }
                    }
                } else {
                    // ---- Existing layout (tablet / landscape / Desktop) ----
                    CollapsingToolbarParallaxEffect(
                        modifier = Modifier.fillMaxSize(),
                        title = state.data.title ?: "",
                        imageUrl = state.data.imageUrl,
                        onBack = {
                            navController.navigateUp()
                        },
                    ) { color ->
                        Column {
                            Column(
                                Modifier
                                    .padding(horizontal = 20.dp)
                                    .padding(top = 16.dp)
                                    .padding(bottom = 8.dp),
                            ) {
                                Row {
                                    Text(
                                        text = state.data.subscribers ?: stringResource(Res.string.unknown),
                                        style = typo().bodySmall,
                                        color = Color.White,
                                        textAlign = TextAlign.Start,
                                        modifier = Modifier.weight(1f),
                                    )
                                    Text(
                                        text = state.data.playCount ?: stringResource(Res.string.unknown),
                                        style = typo().bodySmall,
                                        color = Color.White,
                                        textAlign = TextAlign.End,
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                                Spacer(Modifier.height(8.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    AnimatedVisibility(canvasUrl != null) {
                                        Row {
                                            val canvas = canvasUrl ?: return@Row
                                            LimitedBorderAnimationView(
                                                isAnimated = true,
                                                brush = Brush.sweepGradient(listOf(Color.Transparent, Color.White)),
                                                backgroundColor = Color.Transparent,
                                                contentPadding = 2.dp,
                                                borderWidth = 1.dp,
                                                shape = RoundedCornerShape(4.dp),
                                                oneCircleDurationMillis = 3000,
                                                interactionNumber = 1,
                                            ) {
                                                MediaPlayerView(
                                                    url = canvas.first,
                                                    modifier =
                                                        Modifier
                                                            .width(28.dp)
                                                            .height(ButtonDefaults.MinHeight)
                                                            .align(Alignment.CenterVertically)
                                                            .border(
                                                                width = 0.5.dp,
                                                                color =
                                                                    Color.White.copy(
                                                                        alpha = 0.8f,
                                                                    ),
                                                                shape = RoundedCornerShape(4.dp),
                                                            ).clip(RoundedCornerShape(4.dp))
                                                            .clickable {
                                                                val firstQueue: Track = canvas.second.toTrack()
                                                                viewModel.setQueueData(
                                                                    QueueData.Data(
                                                                        listTracks = arrayListOf(firstQueue),
                                                                        firstPlayedTrack = firstQueue,
                                                                        playlistId = "RDAMVM${firstQueue.videoId}",
                                                                        playlistName = "\"${(state.data.title ?: "")}\" ${
                                                                            getStringBlocking(
                                                                                Res.string.popular,
                                                                            )
                                                                        }",
                                                                        playlistType = PlaylistType.RADIO,
                                                                        continuation = null,
                                                                    ),
                                                                )
                                                                viewModel.loadMediaItem(
                                                                    firstQueue,
                                                                    type = Config.SONG_CLICK,
                                                                )
                                                            },
                                                )
                                            }
                                            Spacer(Modifier.width(12.dp))
                                        }
                                    }
                                    LimitedBorderAnimationView(
                                        isAnimated = !isFollowed,
                                        brush = Brush.sweepGradient(listOf(Color.Gray, Color.White)),
                                        backgroundColor = Color.Transparent,
                                        contentPadding = 0.dp,
                                        borderWidth = 2.dp,
                                        shape = ButtonDefaults.outlinedShape,
                                        oneCircleDurationMillis = 3000,
                                        interactionNumber = 1,
                                    ) {
                                        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                                            OutlinedButton(
                                                onClick = {
                                                    viewModel.updateFollowed(
                                                        if (isFollowed) 0 else 1,
                                                        state.data.channelId ?: return@OutlinedButton,
                                                    )
                                                },
                                                colors =
                                                    ButtonDefaults.outlinedButtonColors().copy(
                                                        contentColor = Color.White,
                                                        containerColor = Color.Transparent,
                                                    ),
                                            ) {
                                                if (isFollowed) {
                                                    Text(text = stringResource(Res.string.followed), color = Color.White)
                                                } else {
                                                    Text(text = stringResource(Res.string.follow), color = Color.White)
                                                }
                                            }
                                        }
                                    }
                                    Spacer(Modifier.width(4.dp))
                                    IconButton(
                                        onClick = {
                                            if (state.data.shuffleParam != null) {
                                                viewModel.onShuffleClick(state.data.shuffleParam)
                                            } else {
                                                viewModel.makeToast(runBlocking { getString(Res.string.error) })
                                            }
                                        },
                                    ) {
                                        Icon(Icons.Rounded.Shuffle, "Shuffle")
                                    }
                                    Spacer(Modifier.weight(1f))
                                    TextButton(
                                        onClick = {
                                            if (state.data.radioParam != null) {
                                                viewModel.onRadioClick(state.data.radioParam)
                                            } else {
                                                viewModel.makeToast(runBlocking { getString(Res.string.error) })
                                            }
                                        },
                                        colors =
                                            ButtonDefaults
                                                .textButtonColors()
                                                .copy(
                                                    contentColor = Color.White,
                                                ),
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Icon(Icons.Outlined.Sensors, "")
                                            if (canvasUrl == null) {
                                                Spacer(Modifier.width(6.dp))
                                                Text(text = stringResource(Res.string.start_radio))
                                            }
                                        }
                                    }
                                }
                            }

                            ArtistSections(
                                state = state,
                                playingTrack = playingTrack,
                                descriptionTint = color,
                                navController = navController,
                                viewModel = viewModel,
                                sharedViewModel = sharedViewModel,
                                onTrackMore = { track ->
                                    choosingTrack = track
                                    showBottomSheet = true
                                },
                            )
                        }
                    }
                }

                if (showBottomSheet && choosingTrack != null) {
                    NowPlayingBottomSheet(
                        onDismiss = {
                            showBottomSheet = false
                            choosingTrack = null
                        },
                        navController = navController,
                        song = choosingTrack?.toSongEntity(),
                    )
                }
            }

            is ArtistScreenState.Error -> {
                viewModel.makeToast(state.message ?: stringResource(Res.string.error))
                navController.navigateUp()
            }
        }
    }
}

/**
 * Shared artist body (Popular → Description). Used by both the portrait Apple-Music layout
 * and the existing CollapsingToolbar layout, so the sections themselves stay untouched.
 */
@Composable
private fun ArtistSections(
    state: ArtistScreenState.Success,
    playingTrack: String?,
    descriptionTint: Color,
    navController: NavController,
    viewModel: ArtistViewModel,
    sharedViewModel: SharedViewModel,
    onTrackMore: (Track) -> Unit,
) {
    Column {
        // Popular Songs
        AnimatedVisibility(state.data.popularSongs.isNotEmpty()) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 20.dp),
                ) {
                    Text(
                        text = stringResource(Res.string.popular),
                        style = typo().labelMedium,
                        color = Color.White,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(
                        onClick = {
                            val id = state.data.listSongParam
                            if (id != null) {
                                navController.navigate(PlaylistDestination(id))
                            } else {
                                viewModel.makeToast(runBlocking { getString(Res.string.error) })
                            }
                        },
                        colors =
                            ButtonDefaults
                                .textButtonColors()
                                .copy(
                                    contentColor = Color.White,
                                ),
                    ) {
                        Text(stringResource(Res.string.more), style = typo().bodySmall)
                    }
                }
                state.data.popularSongs.forEach { song ->
                    SongFullWidthItems(
                        track = song,
                        isPlaying = song.videoId == playingTrack,
                        modifier = Modifier.fillMaxWidth(),
                        onMoreClickListener = {
                            onTrackMore(song)
                        },
                        onClickListener = {
                            val firstQueue: Track = song
                            viewModel.setQueueData(
                                QueueData.Data(
                                    listTracks = arrayListOf(firstQueue),
                                    firstPlayedTrack = firstQueue,
                                    playlistId = "RDAMVM${song.videoId}",
                                    playlistName = "\"${state.data.title ?: ""}\" ${getStringBlocking(Res.string.popular)}",
                                    playlistType = PlaylistType.RADIO,
                                    continuation = null,
                                ),
                            )
                            viewModel.loadMediaItem(
                                firstQueue,
                                type = Config.SONG_CLICK,
                            )
                        },
                        onAddToQueue = {
                            sharedViewModel.addListToQueue(
                                arrayListOf(song),
                            )
                        },
                    )
                }
            }
        }

        // Singles
        AnimatedVisibility(
            state.data.singles != null &&
                state.data.singles!!
                    .results
                    .isNotEmpty(),
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 20.dp),
                ) {
                    Text(
                        text = stringResource(Res.string.singles),
                        style = typo().labelMedium,
                        color = Color.White,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(
                        onClick = {
                            if (state.data.channelId != null) {
                                val id = "MPAD${state.data.channelId}"
                                navController.navigate(
                                    MoreAlbumsDestination(
                                        id = id,
                                        type = MoreAlbumsDestination.SINGLE_TYPE,
                                    ),
                                )
                            } else {
                                viewModel.makeToast(getStringBlocking(Res.string.error))
                            }
                        },
                        colors =
                            ButtonDefaults
                                .textButtonColors()
                                .copy(
                                    contentColor = Color.White,
                                ),
                    ) {
                        Text(stringResource(Res.string.more), style = typo().bodySmall)
                    }
                }
                LazyRow(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    item {
                        Spacer(Modifier.size(10.dp))
                    }
                    items(state.data.singles?.results ?: emptyList()) { single ->
                        HomeItemContentPlaylist(
                            onClick = {
                                navController.navigate(
                                    AlbumDestination(
                                        single.browseId,
                                    ),
                                )
                            },
                            data = single,
                            thumbSize = 180.dp,
                        )
                    }
                    item {
                        Spacer(Modifier.size(10.dp))
                    }
                }
            }
        }

        // Albums
        AnimatedVisibility(
            state.data.albums != null &&
                state.data.albums!!
                    .results
                    .isNotEmpty(),
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 20.dp),
                ) {
                    Text(
                        text = stringResource(Res.string.albums),
                        style = typo().labelMedium,
                        color = Color.White,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(
                        onClick = {
                            if (state.data.channelId != null) {
                                val id = "MPAD${state.data.channelId}"
                                navController.navigate(
                                    MoreAlbumsDestination(
                                        id = id,
                                        type = MoreAlbumsDestination.ALBUM_TYPE,
                                    ),
                                )
                            } else {
                                viewModel.makeToast(getStringBlocking(Res.string.error))
                            }
                        },
                        colors =
                            ButtonDefaults
                                .textButtonColors()
                                .copy(
                                    contentColor = Color.White,
                                ),
                    ) {
                        Text(stringResource(Res.string.more), style = typo().bodySmall)
                    }
                }
                LazyRow(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    item {
                        Spacer(Modifier.size(10.dp))
                    }
                    items(state.data.albums?.results ?: emptyList()) { album ->
                        HomeItemContentPlaylist(
                            onClick = {
                                navController.navigate(
                                    AlbumDestination(
                                        browseId = album.browseId,
                                    ),
                                )
                            },
                            data = album,
                            thumbSize = 180.dp,
                        )
                    }
                    item {
                        Spacer(Modifier.size(10.dp))
                    }
                }
            }
        }

        // Videos
        AnimatedVisibility(
            state.data.video != null &&
                state.data.video!!
                    .video
                    .isNotEmpty(),
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 20.dp),
                ) {
                    Text(
                        text = stringResource(Res.string.videos),
                        style = typo().labelMedium,
                        color = Color.White,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(
                        onClick = {
                            val videoListParam = state.data.video?.videoListParam
                            if (videoListParam != null) {
                                navController.navigate(
                                    PlaylistDestination(
                                        videoListParam,
                                    ),
                                )
                            } else {
                                viewModel.makeToast(getStringBlocking(Res.string.error))
                            }
                        },
                        colors =
                            ButtonDefaults
                                .textButtonColors()
                                .copy(
                                    contentColor = Color.White,
                                ),
                    ) {
                        Text(stringResource(Res.string.more), style = typo().bodySmall)
                    }
                }
                LazyRow(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    item {
                        Spacer(Modifier.size(10.dp))
                    }
                    items(state.data.video?.video ?: emptyList()) { video ->
                        HomeItemVideo(
                            onClick = {
                                val firstQueue: Track = video
                                viewModel.setQueueData(
                                    QueueData.Data(
                                        listTracks = arrayListOf(firstQueue),
                                        firstPlayedTrack = firstQueue,
                                        playlistId = "RDAMVM${video.videoId}",
                                        playlistName = (state.data.title ?: "") + getStringBlocking(Res.string.videos),
                                        playlistType = PlaylistType.RADIO,
                                        continuation = null,
                                    ),
                                )
                                viewModel.loadMediaItem(
                                    firstQueue,
                                    type = Config.VIDEO_CLICK,
                                )
                            },
                            onLongClick = {
                                onTrackMore(video)
                            },
                            data =
                                Content(
                                    album = null,
                                    artists = video.artists,
                                    description = null,
                                    isExplicit = video.isExplicit,
                                    playlistId = null,
                                    browseId = null,
                                    thumbnails = video.thumbnails ?: emptyList(),
                                    title = video.title,
                                    videoId = video.videoId,
                                    views = video.videoType,
                                ),
                        )
                    }
                    item {
                        Spacer(Modifier.size(10.dp))
                    }
                }
            }
        }

        // Feature on
        AnimatedVisibility(state.data.featuredOn.isNotEmpty()) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 20.dp),
                ) {
                    Text(
                        text = stringResource(Res.string.featured_inArtist),
                        style = typo().labelMedium,
                        color = Color.White,
                        modifier =
                            Modifier
                                .weight(1f)
                                .padding(vertical = 10.dp),
                    )
                }
                LazyRow(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    item {
                        Spacer(Modifier.size(10.dp))
                    }
                    items(state.data.featuredOn) { feature ->
                        HomeItemContentPlaylist(
                            onClick = {
                                navController.navigate(
                                    PlaylistDestination(
                                        feature.id,
                                    ),
                                )
                            },
                            data = feature,
                            thumbSize = 180.dp,
                        )
                    }
                    item {
                        Spacer(Modifier.size(10.dp))
                    }
                }
            }
        }

        // Related
        AnimatedVisibility(
            state.data.related != null &&
                state.data.related!!
                    .results
                    .isNotEmpty(),
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 20.dp),
                ) {
                    Text(
                        text = stringResource(Res.string.related_artists),
                        style = typo().labelMedium,
                        color = Color.White,
                        modifier =
                            Modifier
                                .weight(1f)
                                .padding(vertical = 10.dp),
                    )
                }
                LazyRow(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    item {
                        Spacer(Modifier.size(10.dp))
                    }
                    items(state.data.related?.results ?: emptyList()) { related ->
                        HomeItemArtist(
                            onClick = {
                                navController.navigate(
                                    ArtistDestination(
                                        channelId = related.browseId,
                                    ),
                                )
                            },
                            data =
                                Content(
                                    album = null,
                                    artists =
                                        listOf(
                                            Artist(
                                                id = related.browseId,
                                                name = related.title,
                                            ),
                                        ),
                                    description = related.subscribers,
                                    isExplicit = null,
                                    playlistId = null,
                                    browseId = related.browseId,
                                    thumbnails = related.thumbnails,
                                    title = related.title,
                                    videoId = null,
                                    views = null,
                                    durationSeconds = null,
                                    radio = null,
                                ),
                        )
                    }
                    item {
                        Spacer(Modifier.size(10.dp))
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 20.dp),
        ) {
            Text(
                text = stringResource(Res.string.description),
                style = typo().labelMedium,
                color = Color.White,
                modifier =
                    Modifier
                        .weight(1f)
                        .padding(vertical = 12.dp),
            )
        }
        val urlHandler = LocalUriHandler.current
        ElevatedCard(
            modifier = Modifier.padding(horizontal = 20.dp),
            shape = RoundedCornerShape(8.dp),
            colors =
                CardDefaults.elevatedCardColors().copy(
                    containerColor = descriptionTint.rgbFactor(0.5f),
                ),
        ) {
            DescriptionView(
                modifier = Modifier.padding(16.dp),
                text = state.data.description ?: stringResource(Res.string.no_description),
                limitLine = 5,
                onTimeClicked = {},
                onURLClicked = { url ->
                    urlHandler.openUri(url)
                },
            )
        }
        EndOfPage()
    }
}