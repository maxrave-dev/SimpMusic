package com.maxrave.simpmusic.ui.screen.home

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import com.maxrave.simpmusic.R
import com.maxrave.simpmusic.common.CHART_SUPPORTED_COUNTRY
import com.maxrave.simpmusic.common.Config
import com.maxrave.simpmusic.data.model.browse.album.Track
import com.maxrave.simpmusic.data.model.explore.mood.Mood
import com.maxrave.simpmusic.data.model.home.HomeItem
import com.maxrave.simpmusic.data.model.home.chart.Chart
import com.maxrave.simpmusic.data.model.home.chart.toTrack
import com.maxrave.simpmusic.data.queue.Queue
import com.maxrave.simpmusic.extension.navigateSafe
import com.maxrave.simpmusic.extension.toTrack
import com.maxrave.simpmusic.ui.component.CenterLoadingBox
import com.maxrave.simpmusic.ui.component.DropdownButton
import com.maxrave.simpmusic.ui.component.EndOfPage
import com.maxrave.simpmusic.ui.component.HomeItem
import com.maxrave.simpmusic.ui.component.HomeShimmer
import com.maxrave.simpmusic.ui.component.ItemArtistChart
import com.maxrave.simpmusic.ui.component.ItemTrackChart
import com.maxrave.simpmusic.ui.component.ItemVideoChart
import com.maxrave.simpmusic.ui.component.MoodMomentAndGenreHomeItem
import com.maxrave.simpmusic.ui.component.QuickPicksItem
import com.maxrave.simpmusic.ui.component.RippleIconButton
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.HomeViewModel
import com.maxrave.simpmusic.viewModel.SharedViewModel
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.animation.crossfade.CrossfadePlugin
import com.skydoves.landscapist.coil.CoilImage
import com.skydoves.landscapist.components.rememberImageComponent
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@UnstableApi
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    sharedViewModel: SharedViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    navController: NavController,
) {
    val context = LocalContext.current
    val scrollState = rememberLazyListState()
    val accountInfo by viewModel.accountInfo.collectAsState()
    val homeData by viewModel.homeItemList.collectAsState()
    val newRelease by viewModel.newRelease.collectAsState()
    val chart by viewModel.chart.collectAsState()
    val moodMomentAndGenre by viewModel.exploreMoodItem.collectAsState()
    val chartLoading by viewModel.loadingChart.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val accountShow by rememberSaveable {
        mutableStateOf(homeData.find { it.subtitle == accountInfo?.first } != null)
    }
    val regionChart by viewModel.regionCodeChart.collectAsState()
    val homeRefresh by sharedViewModel.homeRefresh.collectAsState()
    val pullToRefreshState =
        rememberPullToRefreshState(
            20.dp,
        )
    val scaleFraction =
        if (pullToRefreshState.isRefreshing) {
            1f
        } else {
            LinearOutSlowInEasing.transform(pullToRefreshState.progress).coerceIn(0f, 1f)
        }
    if (pullToRefreshState.isRefreshing) {
        viewModel.getHomeItemList()
        if (!loading) {
            pullToRefreshState.endRefresh()
            sharedViewModel.homeRefreshDone()
        }
    }
    LaunchedEffect(key1 = homeRefresh) {
        Log.w("HomeScreen", "homeRefresh: $homeRefresh")
        if (homeRefresh) {
            Log.w(
                "HomeScreen",
                "scrollState.firstVisibleItemIndex: ${scrollState.firstVisibleItemIndex}",
            )
            if (scrollState.firstVisibleItemIndex == 1) {
                Log.w(
                    "HomeScreen",
                    "scrollState.canScrollBackward: ${scrollState.canScrollBackward}",
                )
                pullToRefreshState.startRefresh()
            } else {
                Log.w(
                    "HomeScreen",
                    "scrollState.canScrollBackward: ${scrollState.canScrollBackward}",
                )
                launch { scrollState.scrollToItem(0, 0) }
                sharedViewModel.homeRefreshDone()
            }
        }
    }
    Column {
        HomeTopAppBar(navController)
        Box(
            modifier =
                Modifier
                    .nestedScroll(pullToRefreshState.nestedScrollConnection)
                    .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            PullToRefreshContainer(
                modifier =
                    Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 15.dp)
                        .graphicsLayer(scaleX = scaleFraction, scaleY = scaleFraction),
                state = pullToRefreshState,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Crossfade(targetState = loading, label = "Home Shimmer") { loading ->
                if (!loading) {
                    LazyColumn(
                        modifier = Modifier.padding(horizontal = 15.dp),
                        state = scrollState,
                    ) {
                        item {
                            androidx.compose.animation.AnimatedVisibility(
                                visible = accountInfo != null && accountShow,
                            ) {
                                AccountLayout(
                                    accountName = accountInfo?.first ?: "",
                                    url = accountInfo?.second ?: "",
                                )
                            }
                        }
                        item {
                            androidx.compose.animation.AnimatedVisibility(
                                visible =
                                    homeData.find {
                                        it.title ==
                                            context.getString(
                                                R.string.quick_picks,
                                            )
                                    } != null,
                            ) {
                                QuickPicks(
                                    homeItem =
                                        homeData.find {
                                            it.title ==
                                                context.getString(
                                                    R.string.quick_picks,
                                                )
                                        } ?: return@AnimatedVisibility,
                                    navController = navController,
                                )
                            }
                        }
                        items(homeData) {
                            if (it.title != context.getString(R.string.quick_picks)) {
                                HomeItem(
                                    homeViewModel = viewModel,
                                    sharedViewModel = sharedViewModel,
                                    data = it,
                                    navController = navController,
                                )
                            }
                        }
                        items(newRelease) {
                            androidx.compose.animation.AnimatedVisibility(
                                visible = newRelease.isNotEmpty(),
                            ) {
                                HomeItem(
                                    homeViewModel = viewModel,
                                    sharedViewModel = sharedViewModel,
                                    data = it,
                                    navController = navController,
                                )
                            }
                        }
                        item {
                            androidx.compose.animation.AnimatedVisibility(
                                visible = moodMomentAndGenre != null,
                            ) {
                                moodMomentAndGenre?.let {
                                    MoodMomentAndGenre(
                                        mood = it,
                                        navController = navController,
                                    )
                                }
                            }
                        }
                        item {
                            Column(
                                Modifier
                                    .padding(vertical = 10.dp),
                                verticalArrangement = Arrangement.SpaceBetween,
                            ) {
                                ChartTitle()
                                Spacer(modifier = Modifier.height(5.dp))
                                Crossfade(targetState = regionChart) {
                                    Log.w("HomeScreen", "regionChart: $it")
                                    if (it != null) {
                                        DropdownButton(
                                            items = CHART_SUPPORTED_COUNTRY.itemsData.toList(),
                                            defaultSelected =
                                                CHART_SUPPORTED_COUNTRY.itemsData.getOrNull(
                                                    CHART_SUPPORTED_COUNTRY.items.indexOf(it),
                                                )
                                                    ?: CHART_SUPPORTED_COUNTRY.itemsData[1],
                                        ) {
                                            viewModel.exploreChart(
                                                CHART_SUPPORTED_COUNTRY.items[
                                                    CHART_SUPPORTED_COUNTRY.itemsData.indexOf(
                                                        it,
                                                    ),
                                                ],
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(5.dp))
                                Crossfade(
                                    targetState = chartLoading,
                                    label = "Chart",
                                ) { loading ->
                                    if (!loading) {
                                        chart?.let {
                                            ChartData(
                                                chart = it,
                                                navController = navController,
                                                context = context,
                                            )
                                        }
                                    } else {
                                        CenterLoadingBox(
                                            modifier =
                                                Modifier
                                                    .fillMaxWidth()
                                                    .height(400.dp),
                                        )
                                    }
                                }
                            }
                        }
                        item {
                            EndOfPage()
                        }
                    }
                } else {
                    HomeShimmer()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTopAppBar(navController: NavController) {
    val hour =
        remember {
            val date = Calendar.getInstance().time
            val formatter = SimpleDateFormat("HH")
            formatter.format(date).toInt()
        }
    TopAppBar(
        title = {
            Column {
                Text(
                    text = stringResource(id = R.string.app_name),
                    style = typo.titleMedium,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
                Text(
                    text =
                        when (hour) {
                            in 6..12 -> {
                                stringResource(R.string.good_morning)
                            }

                            in 13..17 -> {
                                stringResource(R.string.good_afternoon)
                            }

                            in 18..23 -> {
                                stringResource(R.string.good_evening)
                            }

                            else -> {
                                stringResource(R.string.good_night)
                            }
                        },
                    style = typo.bodySmall,
                )
            }
        },
        actions = {
            RippleIconButton(resId = R.drawable.outline_notifications_24) {
                navController.navigateSafe(R.id.action_global_notificationFragment)
            }
            RippleIconButton(resId = R.drawable.baseline_history_24) {
                navController.navigateSafe(
                    R.id.action_bottom_navigation_item_home_to_recentlySongsFragment,
                )
            }
            RippleIconButton(resId = R.drawable.baseline_settings_24) {
                navController.navigateSafe(
                    R.id.action_bottom_navigation_item_home_to_settingsFragment,
                )
            }
        },
    )
}

@Composable
fun AccountLayout(
    accountName: String,
    url: String,
) {
    Column {
        Text(
            text = stringResource(id = R.string.welcome_back),
            style = typo.bodyMedium,
            color = Color.White,
            modifier = Modifier.padding(bottom = 3.dp),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 5.dp),
        ) {
            CoilImage(
                imageModel = { url },
                imageOptions =
                    ImageOptions(
                        contentScale = ContentScale.Crop,
                        alignment = Alignment.Center,
                    ),
                previewPlaceholder = painterResource(id = R.drawable.holder),
                component =
                    rememberImageComponent {
                        CrossfadePlugin(
                            duration = 550,
                        )
                    },
                modifier =
                    Modifier
                        .size(40.dp)
                        .clip(
                            CircleShape,
                        ),
            )
            Text(
                text = accountName,
                style = typo.headlineMedium,
                color = Color.White,
                modifier =
                    Modifier
                        .padding(start = 8.dp),
            )
        }
    }
}

@Composable
fun QuickPicks(
    homeItem: HomeItem,
    navController: NavController,
) {
    Column(
        Modifier
            .padding(vertical = 8.dp),
    ) {
        Text(
            text = stringResource(id = R.string.let_s_start_with_a_radio),
            style = typo.bodyMedium,
        )
        Text(
            text = stringResource(id = R.string.quick_picks),
            style = typo.headlineMedium,
            maxLines = 1,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp),
        )
        LazyHorizontalGrid(rows = GridCells.Fixed(3), modifier = Modifier.height(210.dp)) {
            items(homeItem.contents) {
                if (it != null) {
                    QuickPicksItem(onClick = {
                        Queue.initPlaylist(
                            playlistId = "RDAMVM${it.videoId}",
                            playlistName = "\"${it.title}\" Radio",
                            playlistType = Queue.PlaylistType.RADIO,
                        )
                        val firstQueue: Track = it.toTrack()
                        Queue.setNowPlaying(firstQueue)
                        val args = Bundle()
                        args.putString("videoId", firstQueue.videoId)
                        args.putString("from", "\"${firstQueue.title}\" Radio")
                        args.putString("type", Config.SONG_CLICK)
                        navController.navigateSafe(R.id.action_global_nowPlayingFragment, args)
                    }, data = it)
                }
            }
        }
    }
}

@Composable
fun MoodMomentAndGenre(
    mood: Mood,
    navController: NavController,
) {
    Column(
        Modifier
            .padding(vertical = 8.dp),
    ) {
        Text(
            text = stringResource(id = R.string.let_s_pick_a_playlist_for_you),
            style = typo.bodyMedium,
        )
        Text(
            text = stringResource(id = R.string.moods_amp_moment),
            style = typo.headlineMedium,
            maxLines = 1,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp),
        )
        LazyHorizontalGrid(rows = GridCells.Fixed(3), modifier = Modifier.height(210.dp)) {
            items(mood.moodsMoments) {
                MoodMomentAndGenreHomeItem(title = it.title) {
                    navController.navigateSafe(
                        R.id.action_global_moodFragment,
                        Bundle().apply {
                            putString("params", it.params)
                        },
                    )
                }
            }
        }
        Text(
            text = stringResource(id = R.string.genre),
            style = typo.headlineMedium,
            maxLines = 1,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp),
        )
        LazyHorizontalGrid(rows = GridCells.Fixed(3), modifier = Modifier.height(210.dp)) {
            items(mood.genres) {
                MoodMomentAndGenreHomeItem(title = it.title) {
                    navController.navigateSafe(
                        R.id.action_global_moodFragment,
                        Bundle().apply {
                            putString("params", it.params)
                        },
                    )
                }
            }
        }
    }
}

@Composable
fun ChartTitle() {
    Column {
        Text(
            text = stringResource(id = R.string.what_is_best_choice_today),
            style = typo.bodyMedium,
        )
        Text(
            text = stringResource(id = R.string.chart),
            style = typo.headlineMedium,
            maxLines = 1,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp),
        )
    }
}

@Composable
fun ChartData(
    chart: Chart,
    navController: NavController,
    context: Context,
) {
    Column {
        AnimatedVisibility(
            visible = !chart.songs.isNullOrEmpty(),
            enter = fadeIn(animationSpec = tween(2000)),
            exit = fadeOut(animationSpec = tween(2000)),
        ) {
            Column {
                Text(
                    text = stringResource(id = R.string.top_tracks),
                    style = typo.headlineMedium,
                    maxLines = 1,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                )
                if (!chart.songs.isNullOrEmpty()) {
                    LazyHorizontalGrid(
                        rows = GridCells.Fixed(3),
                        modifier = Modifier.height(210.dp),
                    ) {
                        items(chart.songs.size) {
                            val data = chart.songs[it]
                            ItemTrackChart(onClick = {
//                                Queue.clear()
                                Queue.setNowPlaying(data)
                                val args = Bundle()
                                args.putString("videoId", data.videoId)
                                args.putString(
                                    "from",
                                    "\"${data.title}\" ${context.getString(R.string.in_charts)}",
                                )
                                args.putString("type", Config.SONG_CLICK)
                                navController.navigateSafe(
                                    R.id.action_global_nowPlayingFragment,
                                    args,
                                )
                            }, data = data, position = it + 1)
                        }
                    }
                }
            }
        }
        Text(
            text = stringResource(id = R.string.top_videos),
            style = typo.headlineMedium,
            maxLines = 1,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
        )
        LazyRow {
            items(chart.videos.items.size) {
                val data = chart.videos.items[it]
                ItemVideoChart(
                    onClick = {
//                        Queue.clear()
                        val firstQueue: Track = data.toTrack()
                        Queue.setNowPlaying(firstQueue)
                        val args = Bundle()
                        args.putString("videoId", data.videoId)
                        args.putString(
                            "from",
                            "\"${data.title}\" ${context.getString(R.string.in_charts)}",
                        )
                        args.putString("type", Config.VIDEO_CLICK)
                        navController.navigateSafe(R.id.action_global_nowPlayingFragment, args)
                    },
                    data = data,
                    position = it + 1,
                )
            }
        }
        Text(
            text = stringResource(id = R.string.top_artists),
            style = typo.headlineMedium,
            maxLines = 1,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
        )
        LazyHorizontalGrid(rows = GridCells.Fixed(3), modifier = Modifier.height(240.dp)) {
            items(chart.artists.itemArtists.size) {
                val data = chart.artists.itemArtists[it]
                ItemArtistChart(onClick = {
                    val args = Bundle()
                    args.putString("channelId", data.browseId)
                    navController.navigateSafe(R.id.action_global_artistFragment, args)
                }, data = data, context = context)
            }
        }
        AnimatedVisibility(visible = !chart.trending.isNullOrEmpty()) {
            Column {
                Text(
                    text = stringResource(id = R.string.trending),
                    style = typo.headlineMedium,
                    maxLines = 1,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                )
                if (!chart.trending.isNullOrEmpty()) {
                    LazyHorizontalGrid(
                        rows = GridCells.Fixed(3),
                        modifier = Modifier.height(210.dp),
                    ) {
                        items(chart.trending.size) {
                            val data = chart.trending[it]
                            ItemTrackChart(onClick = {
//                                Queue.clear()
                                Queue.setNowPlaying(data)
                                val args = Bundle()
                                args.putString("videoId", data.videoId)
                                args.putString(
                                    "from",
                                    "\"${data.title}\" ${context.getString(R.string.in_charts)}",
                                )
                                args.putString("type", Config.VIDEO_CLICK)
                                navController.navigateSafe(
                                    R.id.action_global_nowPlayingFragment,
                                    args,
                                )
                            }, data = data, position = null)
                        }
                    }
                }
            }
        }
    }
}