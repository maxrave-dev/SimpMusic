package com.maxrave.simpmusic.ui.screen.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.SnapLayoutInfoProvider
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.kmpalette.loader.rememberNetworkLoader
import com.kmpalette.rememberDominantColorState
import com.maxrave.common.CHART_SUPPORTED_COUNTRY
import com.maxrave.common.Config
import com.maxrave.domain.data.model.browse.album.Track
import com.maxrave.domain.data.model.home.HomeItem
import com.maxrave.domain.data.model.home.chart.Chart
import com.maxrave.domain.data.model.mood.Mood
import com.maxrave.domain.extension.now
import com.maxrave.domain.mediaservice.handler.PlaylistType
import com.maxrave.domain.mediaservice.handler.QueueData
import com.maxrave.domain.utils.toTrack
import com.maxrave.logger.Logger
import com.maxrave.simpmusic.extension.angledGradientBackground
import com.maxrave.simpmusic.extension.isScrollingUp
import com.maxrave.simpmusic.extension.rgbFactor
import com.maxrave.simpmusic.ui.component.CenterLoadingBox
import com.maxrave.simpmusic.ui.component.Chip
import com.maxrave.simpmusic.ui.component.CoreIntegrityValidator
import com.maxrave.simpmusic.ui.component.DropdownButton
import com.maxrave.simpmusic.ui.component.EndOfPage
import com.maxrave.simpmusic.ui.component.HomeItem
import com.maxrave.simpmusic.ui.component.HomeItemContentPlaylist
import com.maxrave.simpmusic.ui.component.HomeShimmer
import com.maxrave.simpmusic.ui.component.ItemArtistChart
import com.maxrave.simpmusic.ui.component.MoodMomentAndGenreHomeItem
import com.maxrave.simpmusic.ui.component.OfflineErrorState
import com.maxrave.simpmusic.ui.component.QuickPicksItem
import com.maxrave.simpmusic.ui.component.RippleIconButton
import com.maxrave.simpmusic.ui.component.ShareSavedLyricsDialog
import com.maxrave.simpmusic.ui.navigation.destination.home.HomeDestination
import com.maxrave.simpmusic.ui.navigation.destination.home.MoodDestination
import com.maxrave.simpmusic.ui.navigation.destination.home.NotificationDestination
import com.maxrave.simpmusic.ui.navigation.destination.home.RecentlySongsDestination
import com.maxrave.simpmusic.ui.navigation.destination.home.SettingsDestination
import com.maxrave.simpmusic.ui.navigation.destination.library.LibraryDynamicPlaylistDestination
import com.maxrave.simpmusic.ui.navigation.destination.list.ArtistDestination
import com.maxrave.simpmusic.ui.screen.library.LibraryDynamicPlaylistType
import com.maxrave.simpmusic.ui.navigation.destination.list.PlaylistDestination
import com.maxrave.simpmusic.ui.navigation.destination.login.LoginDestination
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.HomeViewModel
import com.maxrave.simpmusic.viewModel.HomeViewModel.Companion.HOME_PARAMS_COMMUTE
import com.maxrave.simpmusic.viewModel.HomeViewModel.Companion.HOME_PARAMS_ENERGIZE
import com.maxrave.simpmusic.viewModel.HomeViewModel.Companion.HOME_PARAMS_FEEL_GOOD
import com.maxrave.simpmusic.viewModel.HomeViewModel.Companion.HOME_PARAMS_FOCUS
import com.maxrave.simpmusic.viewModel.HomeViewModel.Companion.HOME_PARAMS_PARTY
import com.maxrave.simpmusic.viewModel.HomeViewModel.Companion.HOME_PARAMS_RELAX
import com.maxrave.simpmusic.viewModel.HomeViewModel.Companion.HOME_PARAMS_ROMANCE
import com.maxrave.simpmusic.viewModel.HomeViewModel.Companion.HOME_PARAMS_SAD
import com.maxrave.simpmusic.viewModel.HomeViewModel.Companion.HOME_PARAMS_SLEEP
import com.maxrave.simpmusic.viewModel.HomeViewModel.Companion.HOME_PARAMS_WORKOUT
import com.maxrave.simpmusic.viewModel.ListState
import com.maxrave.simpmusic.viewModel.SharedViewModel
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.http.Url
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.all
import simpmusic.composeapp.generated.resources.baseline_history_24
import simpmusic.composeapp.generated.resources.baseline_settings_24
import simpmusic.composeapp.generated.resources.cancel
import simpmusic.composeapp.generated.resources.chart
import simpmusic.composeapp.generated.resources.mono
import simpmusic.composeapp.generated.resources.commute
import simpmusic.composeapp.generated.resources.do_not_show_again
import simpmusic.composeapp.generated.resources.energize
import simpmusic.composeapp.generated.resources.feel_good
import simpmusic.composeapp.generated.resources.focus
import simpmusic.composeapp.generated.resources.genre
import simpmusic.composeapp.generated.resources.go_to_log_in_page
import simpmusic.composeapp.generated.resources.good_afternoon
import simpmusic.composeapp.generated.resources.good_evening
import simpmusic.composeapp.generated.resources.good_morning
import simpmusic.composeapp.generated.resources.good_night
import simpmusic.composeapp.generated.resources.let_s_pick_a_playlist_for_you
import simpmusic.composeapp.generated.resources.let_s_start_with_a_radio
import simpmusic.composeapp.generated.resources.log_in_warning
import simpmusic.composeapp.generated.resources.moods_amp_moment
import simpmusic.composeapp.generated.resources.outline_notifications_24
import simpmusic.composeapp.generated.resources.party
import simpmusic.composeapp.generated.resources.quick_picks
import simpmusic.composeapp.generated.resources.relax
import simpmusic.composeapp.generated.resources.romance
import simpmusic.composeapp.generated.resources.sad
import simpmusic.composeapp.generated.resources.sleep
import simpmusic.composeapp.generated.resources.top_artists
import simpmusic.composeapp.generated.resources.warning
import simpmusic.composeapp.generated.resources.welcome_back
import simpmusic.composeapp.generated.resources.what_is_best_choice_today
import simpmusic.composeapp.generated.resources.workout

private val listOfHomeChip =
    listOf(
        Res.string.all,
        Res.string.relax,
        Res.string.sleep,
        Res.string.energize,
        Res.string.sad,
        Res.string.romance,
        Res.string.feel_good,
        Res.string.workout,
        Res.string.party,
        Res.string.commute,
        Res.string.focus,
    )

@OptIn(ExperimentalMaterial3Api::class)
@ExperimentalFoundationApi
@Composable
fun HomeScreen(
    onScrolling: (onTop: Boolean) -> Unit = {},
    viewModel: HomeViewModel = koinViewModel(),
    sharedViewModel: SharedViewModel = koinInject(),
    navController: NavController,
) {
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberLazyListState()
    val isScrollingUp by scrollState.isScrollingUp()
    val accountInfo by viewModel.accountInfo.collectAsStateWithLifecycle()
    val homeData by viewModel.homeItemList.collectAsStateWithLifecycle()
    val newRelease by viewModel.newRelease.collectAsStateWithLifecycle()
    val chart by viewModel.chart.collectAsStateWithLifecycle()
    val moodMomentAndGenre by viewModel.exploreMoodItem.collectAsStateWithLifecycle()
    val chartLoading by viewModel.loadingChart.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    var accountShow by rememberSaveable { mutableStateOf(false) }
    val regionChart by viewModel.regionCodeChart.collectAsStateWithLifecycle()
    val reloadDestination by sharedViewModel.reloadDestination.collectAsStateWithLifecycle()
    var isRefreshing by remember { mutableStateOf(false) }
    val chipRowState = rememberScrollState()
    val params by viewModel.params.collectAsStateWithLifecycle()
    val homeListState by viewModel.homeListState.collectAsStateWithLifecycle()
    val continuation by viewModel.continuation.collectAsStateWithLifecycle()
    val shouldShowLogInAlert by viewModel.showLogInAlert.collectAsStateWithLifecycle()
    val openAppTime by sharedViewModel.openAppTime.collectAsStateWithLifecycle()
    val shareLyricsPermissions by sharedViewModel.shareSavedLyrics.collectAsStateWithLifecycle()
    val bgColor = MaterialTheme.colorScheme.background
    
    val isLightMode = bgColor.luminance() > 0.5f

    var topHeaderColor by remember { mutableStateOf(bgColor) }
    val animatedColor by animateColorAsState(topHeaderColor, tween(500))
    val mainHomeThumbnail by viewModel.mainHomeThumbnail.collectAsStateWithLifecycle()
    val networkLoader = rememberNetworkLoader(HttpClient(CIO))
    val dominantColorState = rememberDominantColorState(
        defaultColor = bgColor,
        defaultOnColor = bgColor,
        loader = networkLoader,
    )

    var hasSystemUpdate by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(mainHomeThumbnail) {
        mainHomeThumbnail?.let {
            dominantColorState.updateFrom(Url(it))
        }
    }

    LaunchedEffect(dominantColorState) {
        snapshotFlow { dominantColorState.color }.collect {
            topHeaderColor = it.rgbFactor(0.3f)
        }
    }

    var showRequestShareLyricsPermissions by rememberSaveable { mutableStateOf(false) }
    var topAppBarHeightPx by rememberSaveable { mutableIntStateOf(0) }

    LaunchedEffect(scrollState) {
        snapshotFlow { scrollState.firstVisibleItemIndex }
            .collect {
                if (it <= 1) {
                    onScrolling.invoke(true)
                } else {
                    onScrolling.invoke(isScrollingUp)
                }
            }
    }

    val onRefresh: () -> Unit = {
        if (!loading && !isRefreshing) {
            isRefreshing = true
            viewModel.getHomeItemList(params)
        }
    }

    // AQUI ESTÁ LA CORRECCIÓN: onPostScroll escucha la inercia SOBRANTE del scroll
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                // Si la lista ya consumió todo y sobra energía de scroll hacia abajo (available.y > 25f)
                // Y además estamos garantizadamente en el tope (index 0, offset 0)
                if (available.y > 25f && scrollState.firstVisibleItemIndex == 0 && scrollState.firstVisibleItemScrollOffset == 0) {
                    onRefresh()
                }
                return Offset.Zero
            }
        }
    }

    LaunchedEffect(key1 = reloadDestination) {
        if (reloadDestination == HomeDestination::class) {
            if (scrollState.firstVisibleItemIndex > 1) {
                scrollState.animateScrollToItem(0)
                sharedViewModel.reloadDestinationDone()
            } else {
                onRefresh.invoke()
            }
        }
    }

    LaunchedEffect(key1 = loading) {
        if (!loading) {
            isRefreshing = false
            sharedViewModel.reloadDestinationDone()
        }
    }

    LaunchedEffect(key1 = homeData) {
        accountShow = homeData.find { it.subtitle == accountInfo?.first } == null
    }

    LaunchedEffect(openAppTime, shareLyricsPermissions) {
        if ((openAppTime == 1 || openAppTime % 15 == 0) && openAppTime <= 60 && !shareLyricsPermissions) {
            showRequestShareLyricsPermissions = true
        } else {
            showRequestShareLyricsPermissions = false
        }
    }

    val shouldStartPaginate = remember {
        derivedStateOf {
            homeListState != ListState.PAGINATION_EXHAUST &&
                (scrollState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -9) >= (scrollState.layoutInfo.totalItemsCount - 1)
        }
    }

    LaunchedEffect(key1 = shouldStartPaginate.value) {
        if (shouldStartPaginate.value && homeListState == ListState.IDLE) {
            viewModel.getContinueHomeItem(continuation)
        }
    }

    if (showRequestShareLyricsPermissions) {
        ShareSavedLyricsDialog(
            onDismissRequest = {
                showRequestShareLyricsPermissions = false
                sharedViewModel.onDoneReview(isDismissOnly = true)
            },
            onConfirm = { contributor ->
                sharedViewModel.onDoneRequestingShareLyrics(contributor)
            },
        )
    }

    if (shouldShowLogInAlert) {
        var doNotShowAgain by rememberSaveable { mutableStateOf(false) }
        AlertDialog(
            title = { Text(stringResource(Res.string.warning), color = MaterialTheme.colorScheme.onBackground) },
            text = {
                Column {
                    Text(text = stringResource(Res.string.log_in_warning), color = MaterialTheme.colorScheme.onBackground)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { doNotShowAgain = !doNotShowAgain }.fillMaxWidth(),
                    ) {
                        Checkbox(checked = doNotShowAgain, onCheckedChange = { doNotShowAgain = it })
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(stringResource(Res.string.do_not_show_again), color = MaterialTheme.colorScheme.onBackground)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.doneShowLogInAlert(doNotShowAgain)
                    navController.navigate(LoginDestination)
                }) { Text(stringResource(Res.string.go_to_log_in_page)) }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.doneShowLogInAlert(doNotShowAgain) }) { Text(stringResource(Res.string.cancel)) }
            },
            onDismissRequest = { viewModel.doneShowLogInAlert() },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    CoreIntegrityValidator(currentCode = 50) {
        hasSystemUpdate = true
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Box(modifier = Modifier.fillMaxSize()) {
            Crossfade(targetState = loading, label = "Home Shimmer") { loading ->
                if (!loading) {
                    if (homeData.isEmpty()) {
                        OfflineErrorState(
                            onRetry = onRefresh,
                            onOpenDownloaded = {
                                navController.navigate(
                                    LibraryDynamicPlaylistDestination(
                                        type = LibraryDynamicPlaylistType.Downloaded.toStringParams(),
                                    ),
                                )
                            },
                        )
                        return@Crossfade
                    }
                    LazyColumn(
                        state = scrollState,
                        verticalArrangement = Arrangement.spacedBy(28.dp),
                        modifier = Modifier.nestedScroll(nestedScrollConnection)
                    ) {
                        itemsIndexed(homeData, key = { _, item ->
                            item.hashCode().toString() + (mainHomeThumbnail ?: "nothumb")
                        }) { index, item ->
                            Box {
                                if (index == 0 && !isLightMode) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(300.dp)
                                            .angledGradientBackground(listOf(animatedColor, MaterialTheme.colorScheme.background), 25f),
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(180.dp)
                                                .align(Alignment.BottomCenter)
                                                .background(
                                                    brush = Brush.verticalGradient(
                                                        listOf(
                                                            Color.Transparent,
                                                            MaterialTheme.colorScheme.background.copy(alpha = 0.5f),
                                                            MaterialTheme.colorScheme.background.copy(alpha = 0.8f),
                                                            MaterialTheme.colorScheme.background,
                                                        ),
                                                    ),
                                                ),
                                        )
                                    }
                                }
                                Column(
                                    modifier = Modifier.padding(horizontal = 15.dp),
                                ) {
                                    if (index == 0) {
                                        Spacer(Modifier.height(with(LocalDensity.current) { topAppBarHeightPx.toDp() }))
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    if (index == 0 && accountInfo != null && accountShow) {
                                        AccountLayout(
                                            accountName = accountInfo?.first ?: "",
                                            url = accountInfo?.second ?: "",
                                        )
                                        Spacer(Modifier.height(8.dp))
                                    }
                                    if (item.title == stringResource(Res.string.quick_picks)) {
                                        AnimatedVisibility(
                                            visible = homeData.find { it.title == stringResource(Res.string.quick_picks) } != null,
                                        ) {
                                            QuickPicks(
                                                homeItem = (homeData.find { it.title == stringResource(Res.string.quick_picks) } ?: return@AnimatedVisibility).let { content ->
                                                    content.copy(
                                                        contents = content.contents.mapNotNull { ct ->
                                                            ct?.copy(artists = ct.artists?.let { art -> if (art.size > 1) art.dropLast(1) else art })
                                                        },
                                                    )
                                                },
                                                viewModel = viewModel,
                                            )
                                        }
                                    } else {
                                        HomeItem(navController = navController, data = item)
                                    }
                                }
                            }
                        }

                        items(newRelease, key = { "nr_" + it.hashCode() }) {
                            Box(modifier = Modifier.padding(horizontal = 15.dp)) {
                                HomeItem(navController = navController, data = it)
                            }
                        }

                        item(key = "moods_moments") {
                            AnimatedVisibility(visible = moodMomentAndGenre != null) {
                                Box(modifier = Modifier.padding(horizontal = 15.dp)) {
                                    moodMomentAndGenre?.let { MoodMomentAndGenre(mood = it, navController = navController) }
                                }
                            }
                        }

                        item(key = "charts") {
                            Column(
                                Modifier.padding(vertical = 10.dp).padding(horizontal = 15.dp),
                                verticalArrangement = Arrangement.SpaceBetween,
                            ) {
                                ChartTitle()
                                Spacer(modifier = Modifier.height(5.dp))
                                Crossfade(targetState = regionChart) {
                                    if (it != null) {
                                        DropdownButton(
                                            items = CHART_SUPPORTED_COUNTRY.itemsData.toList(),
                                            defaultSelected = CHART_SUPPORTED_COUNTRY.itemsData.getOrNull(CHART_SUPPORTED_COUNTRY.items.indexOf(it)) ?: CHART_SUPPORTED_COUNTRY.itemsData[1],
                                        ) { viewModel.exploreChart(CHART_SUPPORTED_COUNTRY.items[CHART_SUPPORTED_COUNTRY.itemsData.indexOf(it)]) }
                                    }
                                }
                                Spacer(modifier = Modifier.height(5.dp))
                                Crossfade(targetState = chartLoading, label = "Chart") { loading ->
                                    if (!loading) {
                                        chart?.let { ChartData(chart = it, navController = navController) }
                                    } else {
                                        CenterLoadingBox(modifier = Modifier.fillMaxWidth().height(400.dp))
                                    }
                                }
                            }
                        }

                        item {
                            AnimatedVisibility(
                                homeListState == ListState.PAGINATING,
                                enter = expandVertically() + expandVertically(),
                                exit = fadeOut() + shrinkVertically(),
                            ) {
                                CenterLoadingBox(modifier = Modifier.fillMaxWidth().height(200.dp))
                            }
                        }
                        item { EndOfPage() }
                    }
                } else {
                    Column {
                        Spacer(Modifier.height(with(LocalDensity.current) { topAppBarHeightPx.toDp() }))
                        HomeShimmer()
                    }
                }
            }
        }
        AnimatedContent(
            targetState = scrollState.firstVisibleItemIndex == 0 && scrollState.firstVisibleItemScrollOffset == 0,
            transitionSpec = { fadeIn(tween(300)).togetherWith(fadeOut(tween(300))) },
        ) { target ->
            Column(
                modifier = Modifier.align(Alignment.TopCenter).then(
                    if (target) Modifier.background(Color.Transparent)
                    else Modifier.background(MaterialTheme.colorScheme.background.copy(alpha = 0.95f))
                ).onGloballyPositioned { coordinates -> topAppBarHeightPx = coordinates.size.height },
            ) {
                AnimatedVisibility(visible = isScrollingUp, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                    HomeTopAppBar(navController, hasSystemUpdate)
                }
                AnimatedVisibility(visible = !isScrollingUp, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                    Spacer(modifier = Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars))
                }
                Row(
                    modifier = Modifier.horizontalScroll(chipRowState).padding(vertical = 8.dp, horizontal = 15.dp).background(Color.Transparent),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    listOfHomeChip.forEach { id ->
                        val isSelected = when (params) {
                            HOME_PARAMS_RELAX -> id == Res.string.relax
                            HOME_PARAMS_SLEEP -> id == Res.string.sleep
                            HOME_PARAMS_ENERGIZE -> id == Res.string.energize
                            HOME_PARAMS_SAD -> id == Res.string.sad
                            HOME_PARAMS_ROMANCE -> id == Res.string.romance
                            HOME_PARAMS_FEEL_GOOD -> id == Res.string.feel_good
                            HOME_PARAMS_WORKOUT -> id == Res.string.workout
                            HOME_PARAMS_PARTY -> id == Res.string.party
                            HOME_PARAMS_COMMUTE -> id == Res.string.commute
                            HOME_PARAMS_FOCUS -> id == Res.string.focus
                            else -> id == Res.string.all
                        }
                        Chip(isAnimated = loading, isSelected = isSelected, text = stringResource(id)) {
                            when (id) {
                                Res.string.all -> viewModel.setParams(null)
                                Res.string.relax -> viewModel.setParams(HOME_PARAMS_RELAX)
                                Res.string.sleep -> viewModel.setParams(HOME_PARAMS_SLEEP)
                                Res.string.energize -> viewModel.setParams(HOME_PARAMS_ENERGIZE)
                                Res.string.sad -> viewModel.setParams(HOME_PARAMS_SAD)
                                Res.string.romance -> viewModel.setParams(HOME_PARAMS_ROMANCE)
                                Res.string.feel_good -> viewModel.setParams(HOME_PARAMS_FEEL_GOOD)
                                Res.string.workout -> viewModel.setParams(HOME_PARAMS_WORKOUT)
                                Res.string.party -> viewModel.setParams(HOME_PARAMS_PARTY)
                                Res.string.commute -> viewModel.setParams(HOME_PARAMS_COMMUTE)
                                Res.string.focus -> viewModel.setParams(HOME_PARAMS_FOCUS)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTopAppBar(navController: NavController, hasUpdate: Boolean) {
    val hour = remember { val date = now().time; date.hour }
    TopAppBar(
        windowInsets = TopAppBarDefaults.windowInsets.exclude(TopAppBarDefaults.windowInsets.only(WindowInsetsSides.Start)),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(Res.drawable.mono),
                    contentDescription = "Logo",
                    modifier = Modifier.size(36.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Music",
                        style = typo().titleMedium.copy(
                            fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif
                        ),
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                    Text(
                        text = when (hour) {
                            in 6..12 -> stringResource(Res.string.good_morning)
                            in 13..17 -> stringResource(Res.string.good_afternoon)
                            in 18..23 -> stringResource(Res.string.good_evening)
                            else -> stringResource(Res.string.good_night)
                        },
                        style = typo().bodySmall,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
            }
        },
        actions = {
            Box {
                RippleIconButton(resId = Res.drawable.outline_notifications_24) { navController.navigate(NotificationDestination) }
                if (hasUpdate) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 10.dp, end = 10.dp)
                            .size(10.dp)
                            .background(Color.Red, CircleShape)
                    )
                }
            }
            RippleIconButton(resId = Res.drawable.baseline_history_24) { navController.navigate(RecentlySongsDestination) }
            RippleIconButton(resId = Res.drawable.baseline_settings_24) { navController.navigate(SettingsDestination) }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
    )
}

@Composable
fun AccountLayout(accountName: String, url: String) {
    Column {
        Text(
            text = stringResource(Res.string.welcome_back),
            style = typo().bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 3.dp),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 5.dp),
        ) {
            Text(text = accountName, style = typo().headlineMedium, color = MaterialTheme.colorScheme.onBackground)
        }
    }
}

@ExperimentalFoundationApi
@Composable
fun QuickPicks(homeItem: HomeItem, viewModel: HomeViewModel = koinViewModel()) {
    val lazyListState = rememberLazyGridState()
    val snapperFlingBehavior = rememberSnapFlingBehavior(SnapLayoutInfoProvider(lazyGridState = lazyListState, snapPosition = SnapPosition.Start))
    val density = LocalDensity.current
    var widthDp by remember { mutableStateOf(0.dp) }
    Column(
        Modifier.padding(vertical = 8.dp).onGloballyPositioned { coordinates ->
            with(density) { widthDp = (coordinates.size.width).toDp() }
        },
    ) {
        Text(text = stringResource(Res.string.let_s_start_with_a_radio), style = typo().bodySmall, color = MaterialTheme.colorScheme.onBackground)
        Text(
            text = stringResource(Res.string.quick_picks),
            style = typo().headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
        )
        LazyHorizontalGrid(
            rows = GridCells.Fixed(4),
            modifier = Modifier.height(256.dp),
            state = lazyListState,
            flingBehavior = snapperFlingBehavior,
        ) {
            items(homeItem.contents, key = { it.hashCode() }) {
                if (it != null) {
                    QuickPicksItem(
                        onClick = {
                            val firstQueue: Track = it.toTrack()
                            viewModel.setQueueData(
                                QueueData.Data(
                                    listTracks = arrayListOf(firstQueue),
                                    firstPlayedTrack = firstQueue,
                                    playlistId = "RDAMVM${it.videoId}",
                                    playlistName = "\"${it.title}\" Radio",
                                    playlistType = PlaylistType.RADIO,
                                    continuation = null,
                                ),
                            )
                            viewModel.loadMediaItem(firstQueue, type = Config.SONG_CLICK)
                        },
                        data = it,
                        widthDp = widthDp,
                    )
                }
            }
        }
    }
}

@Composable
fun MoodMomentAndGenre(mood: Mood, navController: NavController) {
    val lazyListState1 = rememberLazyGridState()
    val snapperFlingBehavior1 = rememberSnapFlingBehavior(SnapLayoutInfoProvider(lazyGridState = lazyListState1))
    val lazyListState2 = rememberLazyGridState()
    val snapperFlingBehavior2 = rememberSnapFlingBehavior(SnapLayoutInfoProvider(lazyGridState = lazyListState2))

    Column(Modifier.padding(vertical = 8.dp)) {
        Text(text = stringResource(Res.string.let_s_pick_a_playlist_for_you), style = typo().bodyMedium, color = MaterialTheme.colorScheme.onBackground)
        Text(
            text = stringResource(Res.string.moods_amp_moment),
            style = typo().headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
        )
        LazyHorizontalGrid(
            rows = GridCells.Fixed(3),
            modifier = Modifier.height(210.dp),
            state = lazyListState1,
            flingBehavior = snapperFlingBehavior1,
        ) {
            items(mood.moodsMoments, key = { it.title }) {
                MoodMomentAndGenreHomeItem(title = it.title) { navController.navigate(MoodDestination(it.params)) }
            }
        }
        Text(
            text = stringResource(Res.string.genre),
            style = typo().headlineMedium,
            maxLines = 1,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
        )
        LazyHorizontalGrid(
            rows = GridCells.Fixed(3),
            modifier = Modifier.height(210.dp),
            state = lazyListState2,
            flingBehavior = snapperFlingBehavior2,
        ) {
            items(mood.genres, key = { it.title }) {
                MoodMomentAndGenreHomeItem(title = it.title) { navController.navigate(MoodDestination(it.params)) }
            }
        }
    }
}

@Composable
fun ChartTitle() {
    Column {
        Text(text = stringResource(Res.string.what_is_best_choice_today), style = typo().bodyMedium, color = MaterialTheme.colorScheme.onBackground)
        Text(
            text = stringResource(Res.string.chart),
            style = typo().headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
        )
    }
}

@Composable
fun ChartData(chart: Chart, navController: NavController) {
    var gridWidthDp by remember { mutableStateOf(0.dp) }
    val density = LocalDensity.current
    val lazyListState2 = rememberLazyGridState()
    val snapperFlingBehavior2 = rememberSnapFlingBehavior(SnapLayoutInfoProvider(lazyGridState = lazyListState2))

    Column(Modifier.onGloballyPositioned { coordinates -> with(density) { gridWidthDp = (coordinates.size.width).toDp() } }) {
        chart.listChartItem.forEach { item ->
            Text(
                text = item.title,
                style = typo().headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
            )
            val lazyListState = rememberLazyListState()
            val snapperFlingBehavior = rememberSnapFlingBehavior(SnapLayoutInfoProvider(lazyListState = lazyListState))
            LazyRow(flingBehavior = snapperFlingBehavior) {
                items(item.playlists.size, key = { index -> val data = item.playlists[index]; data.id + data.title + index }) {
                    HomeItemContentPlaylist(
                        onClick = { navController.navigate(PlaylistDestination(playlistId = item.playlists[it].id, isYourYouTubePlaylist = false)) },
                        data = item.playlists[it],
                    )
                }
            }
        }
        Text(
            text = stringResource(Res.string.top_artists),
            style = typo().headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        )
        LazyHorizontalGrid(
            rows = GridCells.Fixed(3),
            modifier = Modifier.height(240.dp),
            state = lazyListState2,
            flingBehavior = snapperFlingBehavior2,
        ) {
            items(chart.artists.itemArtists.size, key = { index -> val item = chart.artists.itemArtists[index]; item.title + item.browseId + index }) {
                val data = chart.artists.itemArtists[it]
                ItemArtistChart(onClick = { navController.navigate(ArtistDestination(channelId = data.browseId)) }, data = data, widthDp = gridWidthDp)
            }
        }
    }
}