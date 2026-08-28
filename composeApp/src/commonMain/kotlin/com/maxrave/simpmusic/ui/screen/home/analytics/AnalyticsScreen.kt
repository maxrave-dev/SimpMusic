package com.maxrave.simpmusic.ui.screen.home.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import com.maxrave.simpmusic.ui.icon.ArrowForwardIos
import simpmusic.composeapp.generated.resources.analytics_avg_per_day
import simpmusic.composeapp.generated.resources.analytics_busiest_day
import simpmusic.composeapp.generated.resources.analytics_by_decade
import simpmusic.composeapp.generated.resources.analytics_fingerprint
import simpmusic.composeapp.generated.resources.analytics_listening_clock
import simpmusic.composeapp.generated.resources.analytics_music_ratio
import simpmusic.composeapp.generated.resources.analytics_quick_facts
import simpmusic.composeapp.generated.resources.analytics_vs_previous
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.kmpalette.rememberPaletteState
import com.kyant.backdrop.highlight.Highlight
import com.maxrave.common.Config
import com.maxrave.domain.data.entities.AlbumEntity
import com.maxrave.domain.data.entities.ArtistEntity
import com.maxrave.domain.data.entities.SongEntity
import com.maxrave.domain.data.entities.analytics.PlaybackEventEntity
import com.maxrave.domain.data.entities.analytics.query.TopPlayedAlbum
import com.maxrave.domain.data.entities.analytics.query.TopPlayedArtist
import com.maxrave.domain.data.entities.analytics.query.TopPlayedTracks
import com.maxrave.domain.mediaservice.handler.PlaylistType
import com.maxrave.domain.mediaservice.handler.QueueData
import com.maxrave.domain.utils.LocalResource
import com.maxrave.domain.utils.connectArtists
import com.maxrave.domain.utils.toArrayListTrack
import com.maxrave.domain.utils.toTrack
import com.maxrave.simpmusic.expect.ui.PlatformBackdrop
import com.maxrave.simpmusic.expect.ui.layerBackdrop
import com.maxrave.simpmusic.expect.ui.rememberBackdrop
import com.maxrave.simpmusic.expect.ui.toImageBitmap
import com.maxrave.simpmusic.extension.getScreenSizeInfo
import com.maxrave.simpmusic.extension.getStringBlocking
import com.maxrave.simpmusic.extension.smoothScrimBrush
import com.maxrave.simpmusic.extension.toImmersiveBackground
import com.maxrave.simpmusic.ui.component.AddToPlaylistModalBottomSheet
import com.maxrave.simpmusic.ui.component.CenterLoadingBox
import com.maxrave.simpmusic.ui.component.EndOfPage
import com.maxrave.simpmusic.ui.component.FiveImagesComponent
import com.maxrave.simpmusic.ui.component.ImageData
import com.maxrave.simpmusic.ui.component.LiquidGlassIconButton
import com.maxrave.simpmusic.ui.component.NowPlayingBottomSheet
import com.maxrave.simpmusic.ui.component.SongFullWidthItems
import com.maxrave.simpmusic.ui.component.WrappedEntryCard
import com.maxrave.simpmusic.ui.component.liquidGlass
import com.maxrave.simpmusic.ui.component.selection.SelectedSongsBottomSheet
import com.maxrave.simpmusic.ui.component.selection.SongSelectionTopAppBar
import com.maxrave.simpmusic.ui.component.selection.rememberSongSelectionState
import com.maxrave.simpmusic.ui.icon.ArrowBackIosNew
import com.maxrave.simpmusic.ui.icon.KeyboardArrowDown
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.navigation.destination.home.RecentlySongsDestination
import com.maxrave.simpmusic.ui.navigation.destination.home.WrappedDestination
import com.maxrave.simpmusic.ui.navigation.destination.library.LibraryDynamicPlaylistDestination
import com.maxrave.simpmusic.ui.navigation.destination.list.AlbumDestination
import com.maxrave.simpmusic.ui.navigation.destination.list.ArtistDestination
import com.maxrave.simpmusic.ui.screen.library.LibraryDynamicPlaylistType
import com.maxrave.simpmusic.ui.theme.seed
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.AnalyticsUiState
import com.maxrave.simpmusic.viewModel.AnalyticsViewModel
import com.maxrave.simpmusic.viewModel.SharedViewModel
import com.maxrave.simpmusic.viewModel.SongSelectionViewModel
import com.maxrave.simpmusic.viewModel.WrappedUiState
import com.maxrave.simpmusic.viewModel.WrappedViewModel
import kotlinx.coroutines.flow.map
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.artists
import simpmusic.composeapp.generated.resources.date_range
import simpmusic.composeapp.generated.resources.last_30_days
import simpmusic.composeapp.generated.resources.last_7_days
import simpmusic.composeapp.generated.resources.last_90_days
import simpmusic.composeapp.generated.resources.listened_time
import simpmusic.composeapp.generated.resources.lower_plays
import simpmusic.composeapp.generated.resources.more
import simpmusic.composeapp.generated.resources.analytics_no_data_period
import simpmusic.composeapp.generated.resources.no_data_analytics
import simpmusic.composeapp.generated.resources.songs_played
import simpmusic.composeapp.generated.resources.this_year
import simpmusic.composeapp.generated.resources.top_song
import simpmusic.composeapp.generated.resources.total_listened_time
import simpmusic.composeapp.generated.resources.your_recently_played
import simpmusic.composeapp.generated.resources.your_top_albums
import simpmusic.composeapp.generated.resources.your_top_artists
import simpmusic.composeapp.generated.resources.your_top_tracks

/**
 * How far a section's own content is inset from the block it sits in.
 *
 * It is 24dp because that is where a song row puts its artwork: [SongFullWidthItems] pads itself
 * `horizontal = 15.dp` and then spaces 8dp before the 48dp thumbnail. Section titles, the chart and
 * the mosaics all take the same inset so every left edge on the screen lines up with that artwork —
 * while the row's own highlight still bleeds the full width, which is the item's default and stays
 * untouched. Adding padding around the rows instead is what pushed them 23dp right of their titles.
 */
private val CONTENT_INSET = 24.dp

/** The wider window follows the Apple Music headers, which sit at 32dp. */
private val LANDSCAPE_GUTTER = 32.dp

/** Status-bar inset + `top = 16.dp` + the 48.dp glass buttons + 16.dp of air below them. */
private val TOP_STRIP = 80.dp

/** Between two sections. Tighter spacing inside one is 16dp; nothing else is used. */
private val SECTION_GAP = 32.dp

/** Square artwork in the landscape header, matching Album, Playlist and LocalPlaylist. */
private val LANDSCAPE_ARTWORK = 280.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    innerPadding: PaddingValues,
    navController: NavController,
    analyticsViewModel: AnalyticsViewModel = koinViewModel(),
    wrappedViewModel: WrappedViewModel = koinViewModel(),
    sharedViewModel: SharedViewModel = koinInject(),
) {
    val screenSizeInfo = getScreenSizeInfo()
    val uiState by analyticsViewModel.analyticsUIState.collectAsStateWithLifecycle()
    // Held only to decide whether the Wrapped banner has anything to point at. The entry needs
    // this year's own figures to say what is waiting, so there is no cheaper question to ask —
    // and the banner must be absent, not empty, when the year is too thin to fill a reel.
    val wrappedState by wrappedViewModel.uiState.collectAsStateWithLifecycle()
    val playingTrack by sharedViewModel.nowPlayingState.map { it?.track?.videoId }.collectAsState(null)

    // Which header is used depends on the window's aspect ratio alone, exactly as on Album and
    // Playlist: a portrait window gets the edge-to-edge artwork header, a landscape one gets the
    // side-by-side header and a two-column body.
    val isPortrait = screenSizeInfo.wDP < screenSizeInfo.hDP

    var currentItem by remember { mutableStateOf<SongEntity?>(null) }
    var itemBottomSheetShow by remember { mutableStateOf(false) }

    val selectionState = rememberSongSelectionState()
    val selectionViewModel: SongSelectionViewModel = koinViewModel()
    var showSelectionSheet by rememberSaveable { mutableStateOf(false) }
    var showSelectionAddToPlaylist by rememberSaveable { mutableStateOf(false) }

    val onItemMoreClick: (song: SongEntity) -> Unit = {
        currentItem = it
        itemBottomSheetShow = true
    }
    val onArtistClick: (channelId: String) -> Unit = {
        navController.navigate(ArtistDestination(channelId = it))
    }
    val onAlbumClick: (browseId: String) -> Unit = {
        navController.navigate(AlbumDestination(browseId = it))
    }

    // The page takes the top track's dominant tone, the Apple Music treatment the other immersive
    // screens use. Regenerating is keyed on the URL so recycling the header item does not flash.
    val paletteState = rememberPaletteState()
    var bitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var paletteGeneratedFor by remember { mutableStateOf<String?>(null) }
    val topTrackArtwork = uiState.topTracks.data?.firstOrNull()?.second?.thumbnails
    // Keyed on the bitmap ALONE, deliberately — AlbumScreen does the same. Adding the URL as a key
    // looks harmless but it goes null every time the range reloads and then back to the SAME value,
    // which cancels this effect mid-generate. kmpalette holds PaletteResult.Loading while it works
    // and `palette` reads null unless the result is Success, so a cancelled generate leaves it
    // Loading for good; and since paletteGeneratedFor was never reassigned it still matches the
    // unchanged URL, so the next pass skips. That is the "same song, sometimes black" bug.
    LaunchedEffect(bitmap) {
        val bm = bitmap
        if (bm != null && topTrackArtwork != null && paletteGeneratedFor != topTrackArtwork) {
            paletteState.generate(bm)
            paletteGeneratedFor = topTrackArtwork
        }
    }
    // The last colour that actually resolved. Reading paletteState.palette straight would paint the
    // page black for the whole duration of every generate(), because null is what it reads until the
    // result is Success — and Color.Black is what a null palette resolves to.
    var pageBackground by remember { mutableStateOf(Color.Black) }
    LaunchedEffect(paletteState.palette) {
        paletteState.palette?.let { pageBackground = it.toImmersiveBackground() }
    }

    if (showSelectionSheet) {
        val selectedIds = selectionState.selected.toList()
        SelectedSongsBottomSheet(
            count = selectedIds.size,
            onDismiss = { showSelectionSheet = false },
            onPlayNext = {
                selectionViewModel.playNext(selectedIds)
                selectionState.exit()
            },
            onAddToQueue = {
                selectionViewModel.addToQueue(selectedIds)
                selectionState.exit()
            },
            onAddToPlaylist = { showSelectionAddToPlaylist = true },
            onDownload = {
                selectionViewModel.download(selectedIds)
                selectionState.exit()
            },
            onAddToFavorite = {
                selectionViewModel.addToFavorite(selectedIds)
                selectionState.exit()
            },
        )
    }
    if (showSelectionAddToPlaylist) {
        val selectedIds = selectionState.selected.toList()
        val localPlaylists by selectionViewModel.listLocalPlaylist.collectAsStateWithLifecycle()
        AddToPlaylistModalBottomSheet(
            isBottomSheetVisible = true,
            listLocalPlaylist = localPlaylists,
            listYouTubePlaylist = emptyList(),
            onDismiss = { showSelectionAddToPlaylist = false },
            onClick = { playlist ->
                selectionViewModel.addToPlaylist(playlist.id, selectedIds)
                selectionState.exit()
            },
            onYTPlaylistClick = {},
        )
    }
    if (itemBottomSheetShow && currentItem != null) {
        val track = currentItem ?: return
        NowPlayingBottomSheet(
            onDismiss = {
                itemBottomSheetShow = false
                currentItem = null
            },
            navController = navController,
            song = track,
        )
    }

    // The glass buttons refract whatever the list has drawn, so the LIST is the backdrop source
    // and the buttons are its SIBLINGS. Nesting them inside the source is the render-feedback
    // loop that kills the RuntimeShader — see the note in AlbumScreen.
    val headerBackdrop = rememberBackdrop(Color.Black)

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(pageBackground),
    ) {
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .layerBackdrop(headerBackdrop),
            // A LazyColumn puts NOTHING between its items. The old screen hid that by giving every
            // section header its own `.padding(top = 12.dp)`; the shared SectionHeader dropped it,
            // and every section went flush against the one above.
            verticalArrangement = Arrangement.spacedBy(SECTION_GAP),
        ) {
            item {
                // Portrait folds the period navigator + headline count into the SAME item as the
                // header: as separate items the list's 32dp SECTION_GAP (on top of the header
                // text's own 24dp bottom inset) left a 56dp dead band over the navigator — the
                // owner flagged it. 8dp here keeps the trio reading as one unit.
                Column {
                    AnalyticsHeader(
                        uiState = uiState,
                        isPortrait = isPortrait,
                        onStep = analyticsViewModel::stepPeriod,
                        headerHeight = (screenSizeInfo.hDP / 2.5).dp,
                        scrimStartY = (screenSizeInfo.hPX / 2.5f) / 2,
                        scrimColor = pageBackground,
                        onBitmap = { bitmap = it },
                    )
                    if (isPortrait) {
                        Spacer(Modifier.height(8.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                            PeriodNavigator(uiState, analyticsViewModel::stepPeriod, CONTENT_INSET)
                            HeadlineCount(uiState, CONTENT_INSET)
                        }
                    }
                }
            }

            // Declared outside `item` on purpose: an item that renders nothing is still an item,
            // and the list's 32dp SECTION_GAP would leave a hole where the banner is not shown.
            (wrappedState as? WrappedUiState.Ready)?.let { ready ->
                item {
                    WrappedEntryCard(
                        wrapped = ready.wrapped,
                        onClick = { navController.navigate(WrappedDestination) },
                        modifier =
                            Modifier.padding(
                                horizontal = if (isPortrait) CONTENT_INSET else LANDSCAPE_GUTTER + CONTENT_INSET,
                            ),
                    )
                }
            }

            if (isPortrait) {
                item { QuickFactsSection(uiState, CONTENT_INSET, 2) }
                item {
                    RecentlyPlayedSection(
                        uiState, navController, sharedViewModel, selectionState,
                        playingTrack, onItemMoreClick, CONTENT_INSET,
                    )
                }
                item { TopArtistsSection(uiState, navController, onArtistClick, CONTENT_INSET, false) }
                item { TopAlbumsSection(uiState, navController, onAlbumClick, CONTENT_INSET, false) }
                item {
                    TopTracksSection(
                        uiState, navController, sharedViewModel, selectionState,
                        playingTrack, onItemMoreClick, CONTENT_INSET,
                    )
                }
                item { MusicRatioSection(uiState, CONTENT_INSET) }
                item { FingerprintSection(uiState, CONTENT_INSET) }
                item { ListeningClockSection(uiState, CONTENT_INSET) }
                item { DecadeSection(uiState, CONTENT_INSET) }
                item { DateRangeSection(uiState, CONTENT_INSET) }
            } else {
                // Full width, above the split: it is a reading about the whole period, and a
                // strip across the top says that in a way a block inside one column cannot.
                item { QuickFactsSection(uiState, LANDSCAPE_GUTTER + CONTENT_INSET, 4) }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = LANDSCAPE_GUTTER),
                        horizontalArrangement = Arrangement.spacedBy(SECTION_GAP),
                    ) {
                        // One column is the library, the other is what the numbers say about it.
                        // Splitting by block *kind* rather than by height is what keeps the two
                        // song lists near the top, where the old single-column screen had them —
                        // parking them under five charts buried the part people came to read.
                        Column(
                            // An even half each. A fixed 600dp for this column looked right on a
                            // 1400dp window and starved the charts everywhere else: at 986dp the
                            // remainder was 202dp, narrow enough to wrap the ratio legend one
                            // letter per line.
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(SECTION_GAP),
                        ) {
                            TopArtistsSection(uiState, navController, onArtistClick, CONTENT_INSET, true)
                            TopAlbumsSection(uiState, navController, onAlbumClick, CONTENT_INSET, true)
                            TopTracksSection(
                                uiState, navController, sharedViewModel, selectionState,
                                playingTrack, onItemMoreClick, CONTENT_INSET,
                            )
                            RecentlyPlayedSection(
                                uiState, navController, sharedViewModel, selectionState,
                                playingTrack, onItemMoreClick, CONTENT_INSET,
                            )
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(SECTION_GAP),
                        ) {
                            MusicRatioSection(uiState, CONTENT_INSET)
                            FingerprintSection(uiState, CONTENT_INSET)
                            ListeningClockSection(uiState, CONTENT_INSET)
                            DecadeSection(uiState, CONTENT_INSET)
                            DateRangeSection(uiState, CONTENT_INSET)
                        }
                    }
                }
            }

            item { EndOfPage() }
        }

        if (selectionState.isActive) {
            SongSelectionTopAppBar(
                state = selectionState,
                onSelectAll = {
                    // Two lists are on screen at once here, so both feed select-all; the cap
                    // trims whatever does not fit.
                    selectionState.toggleSelectAll(
                        (
                            (uiState.recentlyRecord.data ?: emptyList()).map { it.second.videoId } +
                                (uiState.topTracks.data ?: emptyList()).map { it.second.videoId }
                        ).distinct(),
                    )
                },
                onOpenActions = { showSelectionSheet = true },
                modifier = Modifier.align(Alignment.TopCenter),
                containerColor = Color.Black,
            )
        }

        // Back, and the day-range picker, on their own row over the strip the header reserves.
        // AlbumScreen nests the button 12dp inside a Box already inset 12dp, so 24dp from the
        // window edge; its like/more pill sits at the header's own 32dp gutter.
        val backGutter = if (isPortrait) 12.dp else 24.dp
        val pillGutter = if (isPortrait) 16.dp else LANDSCAPE_GUTTER
        LiquidGlassIconButton(
            backdrop = headerBackdrop,
            imageVector = SimpIcons.ArrowBackIosNew,
            shape = RoundedCornerShape(24.dp),
            // A 48dp circle catches only a short arc of the default directional sweep and reads
            // as rimless; 1.dp is the smallest step that stays visible without looking a border.
            highlight = Highlight(width = 1.dp),
            modifier =
                Modifier
                    .align(Alignment.TopStart)
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(start = backGutter, top = 16.dp)
                    .size(48.dp),
        ) {
            navController.navigateUp()
        }

        DayRangePill(
            uiState = uiState,
            backdrop = headerBackdrop,
            onPick = { analyticsViewModel.setDayRange(it) },
            modifier =
                Modifier
                    .align(Alignment.TopEnd)
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(end = pillGutter, top = 16.dp),
        )
    }
}

/**
 * The day-range picker: still a [DropdownMenu], only its trigger is now a glass pill that says
 * which range is showing rather than a calendar icon with "7d" printed inside it at 8sp.
 */
@Composable
private fun DayRangePill(
    uiState: AnalyticsUiState,
    backdrop: PlatformBackdrop,
    onPick: (AnalyticsUiState.DayRange) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val label =
        stringResource(
            when (uiState.dayRange) {
                AnalyticsUiState.DayRange.LAST_7_DAYS -> Res.string.last_7_days
                AnalyticsUiState.DayRange.LAST_30_DAYS -> Res.string.last_30_days
                AnalyticsUiState.DayRange.LAST_90_DAYS -> Res.string.last_90_days
                AnalyticsUiState.DayRange.THIS_YEAR -> Res.string.this_year
            },
        )
    Box(modifier) {
        Row(
            modifier =
                Modifier
                    .height(48.dp)
                    .liquidGlass(backdrop, RoundedCornerShape(24.dp), highlight = Highlight(width = 1.dp))
                    .clip(RoundedCornerShape(24.dp))
                    .clickable { expanded = true }
                    .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(text = label, style = typo().labelSmall, color = Color.White, maxLines = 1)
            Icon(
                imageVector = SimpIcons.KeyboardArrowDown,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp),
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            AnalyticsUiState.DayRange.entries.forEach { range ->
                DropdownMenuItem(
                    text = {
                        Text(
                            stringResource(
                                when (range) {
                                    AnalyticsUiState.DayRange.LAST_7_DAYS -> Res.string.last_7_days
                                    AnalyticsUiState.DayRange.LAST_30_DAYS -> Res.string.last_30_days
                                    AnalyticsUiState.DayRange.LAST_90_DAYS -> Res.string.last_90_days
                                    AnalyticsUiState.DayRange.THIS_YEAR -> Res.string.this_year
                                },
                            ),
                            style = typo().labelSmall,
                        )
                    },
                    onClick = {
                        onPick(range)
                        expanded = false
                    },
                )
            }
        }
    }
}

// ========== Header ==========

@Composable
private fun AnalyticsHeader(
    uiState: AnalyticsUiState,
    isPortrait: Boolean,
    onStep: (Int) -> Unit,
    headerHeight: androidx.compose.ui.unit.Dp,
    scrimStartY: Float,
    // The colour the page itself is painted with. The scrim has to dissolve into THIS rather than
    // into colorScheme.background: the page carries the artwork's tone, so ramping to the theme's
    // own background leaves a visible seam exactly where the header ends.
    scrimColor: Color,
    onBitmap: (ImageBitmap) -> Unit,
) {
    val topTracks = uiState.topTracks
    val topTrack = topTracks.data?.firstOrNull()

    when {
        topTracks is LocalResource.Loading ->
            Box(Modifier.fillMaxWidth().height(headerHeight)) {
                CenterLoadingBox(modifier = Modifier.fillMaxSize())
            }

        topTrack == null ->
            EmptyPeriodHeader(uiState, isPortrait, onStep)

        isPortrait ->
            PortraitHeader(topTrack, headerHeight, scrimStartY, scrimColor, onBitmap)

        else ->
            LandscapeHeader(uiState, topTrack, onStep, onBitmap)
    }
}

/**
 * The header for a period that has nothing in it.
 *
 * It carries the period navigator in landscape because that is where the navigator LIVES: portrait
 * renders it as its own item further down the list, but landscape keeps it inside [LandscapeHeader].
 * Replacing the whole header with a bare message therefore took the arrows away with it and left
 * the user parked on an empty period with no way to step off it.
 */
@Composable
private fun EmptyPeriodHeader(
    uiState: AnalyticsUiState,
    isPortrait: Boolean,
    onStep: (Int) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(
                    // Lines up with the body, NOT with the header: landscape sections sit at the
                    // Row's gutter PLUS their own content inset, and text that misses that by 24dp
                    // reads as broken next to "Your recently played".
                    start = if (isPortrait) CONTENT_INSET else LANDSCAPE_GUTTER + CONTENT_INSET,
                    end = if (isPortrait) CONTENT_INSET else LANDSCAPE_GUTTER + CONTENT_INSET,
                    // Reserves the strip the back button and the day-range pill are drawn over.
                    // They are siblings of this list, positioned at the same status-bar inset plus
                    // top = 16.dp with a 48.dp height, so anything starting above 64.dp lands
                    // underneath them. LandscapeHeader reserves the same 16 + 48 + 16 = 80.dp; the
                    // artwork branch never showed the collision because a header image is what the
                    // glass buttons are meant to float over — a line of text is not.
                    top = TOP_STRIP,
                    // Deliberately NOT headerHeight. There is no artwork to fill it, so reserving
                    // 40% of the window for a one-line message left a dead band under the arrows.
                    bottom = 32.dp,
                ),
        horizontalAlignment = if (isPortrait) Alignment.CenterHorizontally else Alignment.Start,
    ) {
        Text(
            // "Play some music" is only the right sentence for someone who has never played
            // anything. Once they have stepped to another period it is simply false — what they
            // need to know is that THIS period is empty and the arrows will take them elsewhere.
            text =
                stringResource(
                    if (uiState.periodOffset == 0) {
                        Res.string.no_data_analytics
                    } else {
                        Res.string.analytics_no_data_period
                    },
                ),
            style = typo().bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        if (!isPortrait) {
            Spacer(Modifier.height(20.dp))
            PeriodNavigator(uiState, onStep, 0.dp)
        }
    }
}

@Composable
private fun PortraitHeader(
    topTrack: Pair<TopPlayedTracks, SongEntity>,
    headerHeight: androidx.compose.ui.unit.Dp,
    scrimStartY: Float,
    // The colour the page itself is painted with. The scrim has to dissolve into THIS rather than
    // into colorScheme.background: the page carries the artwork's tone, so ramping to the theme's
    // own background leaves a visible seam exactly where the header ends.
    scrimColor: Color,
    onBitmap: (ImageBitmap) -> Unit,
) {
    Box(Modifier.fillMaxWidth().height(headerHeight)) {
        ArtworkImage(
            url = topTrack.second.thumbnails,
            onBitmap = onBitmap,
            modifier = Modifier.align(Alignment.Center).fillMaxSize(),
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    brush =
                        smoothScrimBrush(
                            from = scrimColor.copy(alpha = 0f),
                            to = scrimColor,
                            startY = scrimStartY,
                        ),
                ),
        )
        Column(
            modifier =
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(CONTENT_INSET),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(Res.string.top_song),
                style = typo().titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(
                    // Takes the leftover width and stops there, so a long name cannot grow
                    // across the listened-time column.
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        topTrack.second.title,
                        style = typo().labelMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        topTrack.second.artistName?.connectArtists() ?: "",
                        style = typo().bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        // Was a hardcoded English literal, so no translation could reach it.
                        text = stringResource(Res.string.listened_time),
                        style = typo().bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                    )
                    Text(
                        text = formatListeningTime(topTrack.first.totalListeningTime),
                        style = typo().bodyLarge,
                        color = seed,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun LandscapeHeader(
    uiState: AnalyticsUiState,
    topTrack: Pair<TopPlayedTracks, SongEntity>,
    onStep: (Int) -> Unit,
    onBitmap: (ImageBitmap) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = LANDSCAPE_GUTTER, vertical = 16.dp),
    ) {
        // Reserves the strip the sibling glass buttons are drawn over — back and the day-range
        // pill get their own top row, then the artwork sits beside the text column.
        Spacer(Modifier.height(48.dp))
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.Top,
        ) {
            ArtworkImage(
                url = topTrack.second.thumbnails,
                onBitmap = onBitmap,
                modifier = Modifier.size(LANDSCAPE_ARTWORK).clip(RoundedCornerShape(8.dp)),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(Res.string.top_song),
                    style = typo().titleLarge,
                    color = Color.White,
                    maxLines = 1,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = topTrack.second.title,
                    style = typo().labelMedium,
                    // The app accent, standing in for the brand red Apple uses on this line.
                    color = seed,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text =
                        "${topTrack.second.artistName?.connectArtists() ?: ""} · " +
                            "${stringResource(Res.string.listened_time)} " +
                            formatListeningTime(topTrack.first.totalListeningTime),
                    style = typo().bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(22.dp))
                PeriodNavigator(uiState, onStep, 0.dp)
                Spacer(Modifier.height(20.dp))
                HeadlineCount(uiState, 0.dp)
            }
        }
    }
}

@Composable
private fun ArtworkImage(
    url: String?,
    onBitmap: (ImageBitmap) -> Unit,
    modifier: Modifier,
) {
    AsyncImage(
        model =
            ImageRequest
                .Builder(LocalPlatformContext.current)
                .data(url)
                .diskCachePolicy(CachePolicy.ENABLED)
                .diskCacheKey(url ?: "")
                .crossfade(550)
                .build(),
        contentDescription = "",
        contentScale = ContentScale.Crop,
        onSuccess = { onBitmap(it.result.image.toImageBitmap()) },
        modifier = modifier,
    )
}

// ========== Sections ==========

/**
 * Step the window back and forward through periods.
 *
 * The forward arrow dims at the present rather than disappearing, so the control keeps its shape
 * and the label underneath never shifts sideways as the user walks back through weeks.
 */
@Composable
private fun PeriodNavigator(
    uiState: AnalyticsUiState,
    onStep: (Int) -> Unit,
    gutter: Dp,
) {
    val start = uiState.periodStart
    val end = uiState.periodEnd
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = gutter),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (gutter == 0.dp) Arrangement.spacedBy(14.dp) else Arrangement.SpaceBetween,
    ) {
        StepArrow(SimpIcons.ArrowBackIosNew, enabled = true) { onStep(-1) }
        Text(
            text = if (start != null && end != null) formatPeriodSpan(start, end) else "",
            style = typo().bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
        )
        StepArrow(SimpIcons.ArrowForwardIos, enabled = uiState.canStepForward) { onStep(1) }
    }
}

@Composable
private fun StepArrow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (enabled) 0.12f else 0.05f))
                .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
            modifier = Modifier.size(16.dp),
        )
    }
}

/**
 * The period's play count, and how it moved.
 *
 * The change is the point: a bare "514" tells a listener nothing they can act on, while "514, a
 * quarter more than last week" does. It is simply absent when the previous period held nothing,
 * because every figure would otherwise read as an infinite increase in a new user's first week.
 */
@Composable
private fun HeadlineCount(
    uiState: AnalyticsUiState,
    gutter: Dp,
) {
    val stats = uiState.stats.data ?: return
    val delta = percentDelta(stats.plays, uiState.previousStats?.plays)
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = gutter),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("${stats.plays}", style = typo().titleLarge, color = Color.White, maxLines = 1)
            Text(stringResource(Res.string.songs_played), style = typo().bodyMedium, maxLines = 1)
        }
        if (delta != null) {
            Column(
                modifier = Modifier.padding(bottom = 4.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = if (delta >= 0) "+$delta%" else "$delta%",
                    style = typo().labelSmall,
                    color = seed,
                    maxLines = 1,
                )
                Text(stringResource(Res.string.analytics_vs_previous), style = typo().bodySmall, maxLines = 1)
            }
        }
    }
}

/**
 * Four readings about the period, each against the same span one period earlier.
 *
 * No container: these are facts about the period named above them, not separable objects, and the
 * page behind them is a flat palette tone with nothing to separate from.
 */
@Composable
private fun QuickFactsSection(
    uiState: AnalyticsUiState,
    gutter: Dp,
    columns: Int,
) {
    val stats = uiState.stats.data ?: return
    if (stats.isEmpty) return
    val prev = uiState.previousStats
    // Both of these are counts of PLAYS, which the label alone does not say: "Average per day: 22"
    // and "Most active day: 101 · 10 Mar" read as bare magnitudes. The same word already labels the
    // bars in DateRangeSection, so the unit is spelled the same way in both places.
    val plays = stringResource(Res.string.lower_plays)
    val facts =
        listOf(
            Triple(
                stringResource(Res.string.total_listened_time),
                formatListeningTime(stats.listenedSeconds),
                percentDelta(stats.listenedSeconds, prev?.listenedSeconds),
            ),
            Triple(
                stringResource(Res.string.analytics_avg_per_day),
                "${stats.playsPerActiveDay} $plays",
                percentDelta(stats.playsPerActiveDay.toLong(), prev?.playsPerActiveDay?.toLong()),
            ),
            Triple(
                stringResource(Res.string.analytics_busiest_day),
                stats.busiestDay?.let { "${stats.busiestDayPlays} $plays · ${formatChartDayShort(it)}" } ?: "—",
                percentDelta(stats.busiestDayPlays.toLong(), prev?.busiestDayPlays?.toLong()),
            ),
            Triple(
                stringResource(Res.string.artists),
                "${stats.distinctArtists}",
                percentDelta(stats.distinctArtists.toLong(), prev?.distinctArtists?.toLong()),
            ),
        )
    Column {
        SectionHeader(stringResource(Res.string.analytics_quick_facts), gutter)
        Spacer(Modifier.height(16.dp))
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = gutter),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            facts.chunked(columns).forEach { rowFacts ->
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    rowFacts.forEach { (label, value, delta) ->
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(label, style = typo().bodySmall, maxLines = 2)
                            Row(
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(value, style = typo().labelMedium, color = Color.White, maxLines = 2)
                                if (delta != null) {
                                    Text(
                                        text = if (delta >= 0) "+$delta%" else "$delta%",
                                        style = typo().bodySmall,
                                        color = seed,
                                        maxLines = 1,
                                    )
                                }
                            }
                        }
                    }
                    // Keeps a short final row aligned with the one above it.
                    repeat(columns - rowFacts.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun MusicRatioSection(
    uiState: AnalyticsUiState,
    gutter: Dp,
) {
    val stats = uiState.stats.data ?: return
    if (stats.isEmpty) return
    Column {
        SectionHeader(stringResource(Res.string.analytics_music_ratio), gutter)
        Spacer(Modifier.height(16.dp))
        MusicRatioChart(
            stats = stats,
            previous = uiState.previousStats,
            modifier = Modifier.fillMaxWidth().padding(horizontal = gutter),
        )
    }
}

@Composable
private fun FingerprintSection(
    uiState: AnalyticsUiState,
    gutter: Dp,
) {
    val stats = uiState.stats.data ?: return
    if (stats.isEmpty) return
    Column {
        SectionHeader(stringResource(Res.string.analytics_fingerprint), gutter)
        Spacer(Modifier.height(16.dp))
        FingerprintChart(
            current = stats.fingerprint,
            previous = uiState.previousStats?.fingerprint,
            modifier = Modifier.fillMaxWidth().padding(horizontal = gutter),
        )
    }
}

@Composable
private fun ListeningClockSection(
    uiState: AnalyticsUiState,
    gutter: Dp,
) {
    val stats = uiState.stats.data ?: return
    if (stats.isEmpty) return
    Column {
        SectionHeader(stringResource(Res.string.analytics_listening_clock), gutter)
        Spacer(Modifier.height(16.dp))
        ListeningClockChart(
            playsByHour = stats.playsByHour,
            modifier = Modifier.fillMaxWidth().padding(horizontal = gutter),
        )
    }
}

@Composable
private fun DecadeSection(
    uiState: AnalyticsUiState,
    gutter: Dp,
) {
    val stats = uiState.stats.data ?: return
    // Nothing to draw when no play in the period could be dated at all — which is the normal case
    // for someone who only listens to radio.
    if (stats.decades.isEmpty()) return
    Column {
        SectionHeader(stringResource(Res.string.analytics_by_decade), gutter)
        Spacer(Modifier.height(16.dp))
        DecadeChart(stats = stats, modifier = Modifier.fillMaxWidth().padding(horizontal = gutter))
    }
}

@Composable
private fun SectionHeader(
    title: String,
    gutter: androidx.compose.ui.unit.Dp,
    onMore: (() -> Unit)? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(horizontal = gutter),
    ) {
        Text(
            text = title,
            style = typo().labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        if (onMore != null) {
            TextButton(
                onClick = onMore,
                colors =
                    ButtonDefaults
                        .textButtonColors()
                        .copy(contentColor = MaterialTheme.colorScheme.onSurface),
            ) {
                Text(stringResource(Res.string.more), style = typo().bodySmall)
            }
        }
    }
}

@Composable
private fun RecentlyPlayedSection(
    uiState: AnalyticsUiState,
    navController: NavController,
    sharedViewModel: SharedViewModel,
    selectionState: com.maxrave.simpmusic.ui.component.selection.SongSelectionState,
    playingTrack: String?,
    onItemMoreClick: (SongEntity) -> Unit,
    gutter: androidx.compose.ui.unit.Dp,
) {
    val records = uiState.recentlyRecord.data ?: return
    if (records.isEmpty()) return
    Column {
        SectionHeader(stringResource(Res.string.your_recently_played), gutter) {
            navController.navigate(RecentlySongsDestination)
        }
        records.forEach { pair ->
            SongRow(
                pair = pair.second,
                all = records.map { it.second },
                playlistName = getStringBlocking(Res.string.your_recently_played),
                playingTrack = playingTrack,
                sharedViewModel = sharedViewModel,
                selectionState = selectionState,
                onItemMoreClick = onItemMoreClick,
                rightView = { PlayedAtLabel(pair.first) },
            )
        }
    }
}

@Composable
private fun PlayedAtLabel(event: PlaybackEventEntity) {
    Text(
        // Was formatted with MonthNames.ENGLISH_FULL, which is a constant, not a locale lookup.
        text = formatPlayedAt(event.timestamp),
        style = typo().bodySmall,
    )
}

@Composable
private fun TopArtistsSection(
    uiState: AnalyticsUiState,
    navController: NavController,
    onArtistClick: (String) -> Unit,
    gutter: androidx.compose.ui.unit.Dp,
    landscape: Boolean,
) {
    val artists = uiState.topArtists.data?.take(5) ?: return
    if (artists.isEmpty()) return
    Column {
        SectionHeader(stringResource(Res.string.your_top_artists), gutter) {
            navController.navigate(
                LibraryDynamicPlaylistDestination(
                    type = LibraryDynamicPlaylistType.TopArtists.toStringParams(),
                ),
            )
        }
        Spacer(Modifier.height(16.dp))
        FiveImagesComponent(
            modifier = Modifier.fillMaxWidth().padding(horizontal = gutter),
            landscape = landscape,
            images =
                artists.map { (played: TopPlayedArtist, artist: ArtistEntity) ->
                    ImageData(
                        imageUrl = artist.thumbnails ?: "",
                        title = artist.name,
                        subtitle = "${played.playCount} ${stringResource(Res.string.lower_plays)}",
                        onClick = { onArtistClick(artist.channelId) },
                    )
                },
        )
    }
}

@Composable
private fun TopAlbumsSection(
    uiState: AnalyticsUiState,
    navController: NavController,
    onAlbumClick: (String) -> Unit,
    gutter: androidx.compose.ui.unit.Dp,
    landscape: Boolean,
) {
    val albums = uiState.topAlbums.data?.take(5) ?: return
    if (albums.isEmpty()) return
    Column {
        SectionHeader(stringResource(Res.string.your_top_albums), gutter) {
            navController.navigate(
                LibraryDynamicPlaylistDestination(
                    type = LibraryDynamicPlaylistType.TopAlbums.toStringParams(),
                ),
            )
        }
        Spacer(Modifier.height(16.dp))
        FiveImagesComponent(
            modifier = Modifier.fillMaxWidth().padding(horizontal = gutter),
            landscape = landscape,
            images =
                albums.map { (played: TopPlayedAlbum, album: AlbumEntity) ->
                    ImageData(
                        imageUrl = album.thumbnails ?: "",
                        title = album.title,
                        subtitle = album.artistName?.connectArtists() ?: "",
                        thirdTitle = "${played.playCount} ${stringResource(Res.string.lower_plays)}",
                        onClick = { onAlbumClick(album.browseId) },
                    )
                },
        )
    }
}

@Composable
private fun TopTracksSection(
    uiState: AnalyticsUiState,
    navController: NavController,
    sharedViewModel: SharedViewModel,
    selectionState: com.maxrave.simpmusic.ui.component.selection.SongSelectionState,
    playingTrack: String?,
    onItemMoreClick: (SongEntity) -> Unit,
    gutter: androidx.compose.ui.unit.Dp,
) {
    val tracks = uiState.topTracks.data?.take(5) ?: return
    if (tracks.isEmpty()) return
    Column {
        SectionHeader(stringResource(Res.string.your_top_tracks), gutter) {
            navController.navigate(
                LibraryDynamicPlaylistDestination(
                    type = LibraryDynamicPlaylistType.TopTracks.toStringParams(),
                ),
            )
        }
        tracks.forEach { pair ->
            SongRow(
                pair = pair.second,
                all = tracks.map { it.second },
                playlistName = getStringBlocking(Res.string.your_top_tracks),
                playingTrack = playingTrack,
                sharedViewModel = sharedViewModel,
                selectionState = selectionState,
                onItemMoreClick = onItemMoreClick,
                rightView = {
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = "${pair.first.playCount} ${stringResource(Res.string.lower_plays)}",
                            style = typo().bodySmall,
                            color = Color.White,
                            maxLines = 1,
                        )
                        Text(
                            // Was "$seconds seconds" here too.
                            text = formatListeningTime(pair.first.totalListeningTime),
                            style = typo().bodySmall,
                            maxLines = 1,
                        )
                    }
                },
            )
        }
    }
}

/**
 * The play counts over time. Still the horizontal bar list the screen has always drawn — the
 * chart buckets by day for short ranges and by month for long ones, so it never grows past a
 * dozen rows and a line would not read any better.
 */
@Composable
private fun DateRangeSection(
    uiState: AnalyticsUiState,
    gutter: androidx.compose.ui.unit.Dp,
) {
    val data = uiState.scrobblesLineChart.data ?: return
    if (data.isEmpty()) return
    val maxPlays = data.maxOf { it.second }.coerceAtLeast(1)
    Column(modifier = Modifier.padding(horizontal = gutter)) {
        Text(
            text = stringResource(Res.string.date_range),
            style = typo().labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            data.forEach { (bucket, playCount) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text =
                            when (bucket) {
                                is AnalyticsUiState.ChartType.Day -> formatChartDay(bucket.day)
                                is AnalyticsUiState.ChartType.Week -> formatChartWeek(bucket.start, bucket.end)
                                is AnalyticsUiState.ChartType.Month -> formatChartMonth(bucket.month, bucket.year)
                            },
                        style = typo().bodySmall,
                        maxLines = 1,
                        modifier = Modifier.width(108.dp),
                    )
                    Spacer(Modifier.width(12.dp))
                    Box(
                        Modifier
                            .weight(1f)
                            .height(24.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f)),
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth((playCount.toFloat() / maxPlays).coerceIn(0.001f, 1f))
                                .height(24.dp)
                                .clip(CircleShape)
                                .background(seed),
                        )
                        Text(
                            text = "$playCount ${stringResource(Res.string.lower_plays)}",
                            style = typo().bodySmall,
                            color = MaterialTheme.colorScheme.surface,
                            maxLines = 1,
                            modifier =
                                Modifier
                                    .align(Alignment.CenterStart)
                                    .padding(horizontal = 10.dp),
                        )
                    }
                }
            }
        }
    }
}

/**
 * One song row, wired the way both lists on this screen already wired theirs: the shared
 * [SongFullWidthItems], the same queue handoff, and the same selection hooks.
 */
@Composable
private fun SongRow(
    pair: SongEntity,
    all: List<SongEntity>,
    playlistName: String,
    playingTrack: String?,
    sharedViewModel: SharedViewModel,
    selectionState: com.maxrave.simpmusic.ui.component.selection.SongSelectionState,
    onItemMoreClick: (SongEntity) -> Unit,
    rightView: @Composable () -> Unit,
) {
    val song = pair.toTrack()
    SongFullWidthItems(
        track = song,
        isPlaying = song.videoId == playingTrack,
        modifier = Modifier.fillMaxWidth(),
        onMoreClickListener = { onItemMoreClick(pair) },
        onClickListener = {
            with(sharedViewModel) {
                setQueueData(
                    QueueData.Data(
                        listTracks = all.toArrayListTrack(),
                        firstPlayedTrack = pair.toTrack(),
                        playlistId = null,
                        playlistName = playlistName,
                        playlistType = PlaylistType.RADIO,
                        continuation = null,
                    ),
                )
                loadMediaItem(
                    pair.toTrack(),
                    Config.PLAYLIST_CLICK,
                    all.indexOf(pair).coerceAtLeast(0),
                )
            }
        },
        onAddToQueue = { sharedViewModel.addListToQueue(arrayListOf(song)) },
        selectionMode = selectionState.isActive,
        isSelected = selectionState.isSelected(song.videoId),
        onLongClick = { selectionState.start(it) },
        onSelectToggle = { selectionState.toggle(it) },
        rightView = {
            Box(Modifier.wrapContentHeight()) { rightView() }
        },
    )
}
