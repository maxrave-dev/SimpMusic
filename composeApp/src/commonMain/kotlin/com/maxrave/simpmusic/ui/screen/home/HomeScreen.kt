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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.currentStateAsState
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.eygraber.uri.Uri
import com.kmpalette.loader.rememberNetworkLoader
import com.kmpalette.rememberDominantColorState
import com.maxrave.common.CHART_SUPPORTED_COUNTRY
import com.maxrave.common.Config
import com.maxrave.domain.data.model.browse.album.Track
import com.maxrave.domain.data.model.home.HomeItem
import com.maxrave.domain.data.model.home.chart.Chart
import com.maxrave.domain.data.model.intent.GenericIntent
import com.maxrave.domain.data.model.mood.Mood
import com.maxrave.domain.extension.now
import com.maxrave.domain.manager.DataStoreManager.Values.TRUE
import com.maxrave.domain.mediaservice.handler.PlaylistType
import com.maxrave.domain.mediaservice.handler.QueueData
import com.maxrave.domain.utils.toSongEntity
import com.maxrave.domain.utils.toTrack
import com.maxrave.logger.Logger
import com.maxrave.simpmusic.Platform
import com.maxrave.simpmusic.expect.openUrl
import com.maxrave.simpmusic.expect.ui.HorizontalScrollBar
import com.maxrave.simpmusic.expect.ui.layerBackdrop
import com.maxrave.simpmusic.expect.ui.rememberBackdrop
import com.maxrave.simpmusic.extension.angledGradientBackground
import com.maxrave.simpmusic.extension.artworkScrimBrush
import com.maxrave.simpmusic.extension.getScreenSizeInfo
import com.maxrave.simpmusic.extension.isScrollingUp
import com.maxrave.simpmusic.extension.rgbFactor
import com.maxrave.simpmusic.extension.ultraThinBarStyle
import com.maxrave.simpmusic.getPlatform
import com.maxrave.simpmusic.ui.component.BlogPromoDialog
import com.maxrave.simpmusic.ui.component.CenterLoadingBox
import com.maxrave.simpmusic.ui.component.Chip
import com.maxrave.simpmusic.ui.component.DropdownButton
import com.maxrave.simpmusic.ui.component.EndOfPage
import com.maxrave.simpmusic.ui.component.ExpandableChip
import com.maxrave.simpmusic.ui.component.FootgunsStarDialog
import com.maxrave.simpmusic.ui.component.HomeDiscoverSection
import com.maxrave.simpmusic.ui.component.HomeItem
import com.maxrave.simpmusic.ui.component.HomeItemContentPlaylist
import com.maxrave.simpmusic.ui.component.HomeShimmer
import com.maxrave.simpmusic.ui.component.ItemArtistChart
import com.maxrave.simpmusic.ui.component.ListenTogetherIconButton
import com.maxrave.simpmusic.ui.component.LocalAppDialogOpen
import com.maxrave.simpmusic.ui.component.LocalNowPlayingOpen
import com.maxrave.simpmusic.ui.component.LocalUserStarted
import com.maxrave.simpmusic.ui.component.MoodMomentAndGenreHomeItem
import com.maxrave.simpmusic.ui.component.NowPlayingBottomSheet
import com.maxrave.simpmusic.ui.component.OfflineErrorState
import com.maxrave.simpmusic.ui.component.PromoBanner
import com.maxrave.simpmusic.ui.component.PromoBannerData
import com.maxrave.simpmusic.ui.component.QuickPicksItem
import com.maxrave.simpmusic.ui.component.ReviewDialog
import com.maxrave.simpmusic.ui.component.RippleIconButton
import com.maxrave.simpmusic.ui.component.ShareSavedLyricsDialog
import com.maxrave.simpmusic.ui.component.homeCardAspectRatio
import com.maxrave.simpmusic.ui.component.rememberHolderPainter
import com.maxrave.simpmusic.ui.icon.Notifications
import com.maxrave.simpmusic.ui.icon.Person
import com.maxrave.simpmusic.ui.icon.Settings
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.navigation.destination.home.HomeDestination
import com.maxrave.simpmusic.ui.navigation.destination.home.ListenTogetherDestination
import com.maxrave.simpmusic.ui.navigation.destination.home.MoodDestination
import com.maxrave.simpmusic.ui.navigation.destination.home.NotificationDestination
import com.maxrave.simpmusic.ui.navigation.destination.home.SettingsDestination
import com.maxrave.simpmusic.ui.navigation.destination.library.LibraryDynamicPlaylistDestination
import com.maxrave.simpmusic.ui.navigation.destination.list.ArtistDestination
import com.maxrave.simpmusic.ui.navigation.destination.list.PodcastDestination
import com.maxrave.simpmusic.ui.navigation.destination.list.PlaylistDestination
import com.maxrave.simpmusic.ui.navigation.destination.login.LoginDestination
import com.maxrave.simpmusic.ui.screen.library.LibraryDynamicPlaylistType
import com.maxrave.simpmusic.ui.theme.LocalLiquidGlassEnabled
import com.maxrave.simpmusic.ui.theme.desktopPanelDark
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.utils.VersionManager
import com.maxrave.simpmusic.utils.eligiblePromos
import com.maxrave.simpmusic.viewModel.FOOTGUNS_STAR_KEY
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
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.blur.hazeBlur
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.http.Url
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.all
import simpmusic.composeapp.generated.resources.cancel
import simpmusic.composeapp.generated.resources.chart
import simpmusic.composeapp.generated.resources.commute
import simpmusic.composeapp.generated.resources.do_not_show_again
import simpmusic.composeapp.generated.resources.energize
import simpmusic.composeapp.generated.resources.feel_good
import simpmusic.composeapp.generated.resources.focus
import simpmusic.composeapp.generated.resources.go_to_log_in_page
import simpmusic.composeapp.generated.resources.good_afternoon
import simpmusic.composeapp.generated.resources.good_evening
import simpmusic.composeapp.generated.resources.good_morning
import simpmusic.composeapp.generated.resources.good_night
import simpmusic.composeapp.generated.resources.guest
import simpmusic.composeapp.generated.resources.let_s_pick_a_playlist_for_you
import simpmusic.composeapp.generated.resources.let_s_start_with_a_radio
import simpmusic.composeapp.generated.resources.log_in_to_YouTube
import simpmusic.composeapp.generated.resources.log_in_warning
import simpmusic.composeapp.generated.resources.mood
import simpmusic.composeapp.generated.resources.party
import simpmusic.composeapp.generated.resources.play_all
import simpmusic.composeapp.generated.resources.quick_picks
import simpmusic.composeapp.generated.resources.relax
import simpmusic.composeapp.generated.resources.romance
import simpmusic.composeapp.generated.resources.sad
import simpmusic.composeapp.generated.resources.sleep
import simpmusic.composeapp.generated.resources.top_artists
import simpmusic.composeapp.generated.resources.warning
import simpmusic.composeapp.generated.resources.what_is_best_choice_today
import simpmusic.composeapp.generated.resources.workout

// DataStore key for blog-promo one-shot dialog. Bump the suffix (v2, v3, …) to re-promote.
private const val BLOG_PROMO_KEY = "blog_promo_v1_seen"

// Launch banner timing: how long Home's own launch dialogs get to settle, and how long the screen must
// stay free of other dialogs before the banner draws.
private const val PROMO_SETTLE_MS = 1_000L
private const val PROMO_CLEAR_MS = 400L

// The height the first item lost with the old app bar (64dp + the 50dp chip row, now one 56dp row) and
// the Welcome back row (81dp), which Home's top glow still has to cover.
private val HomeGlowOverflow = 140.dp

// The moods behind the Mood chip, each with the Home params it loads.
private val homeMoods =
    listOf(
        Res.string.relax to HOME_PARAMS_RELAX,
        Res.string.sleep to HOME_PARAMS_SLEEP,
        Res.string.energize to HOME_PARAMS_ENERGIZE,
        Res.string.sad to HOME_PARAMS_SAD,
        Res.string.romance to HOME_PARAMS_ROMANCE,
        Res.string.feel_good to HOME_PARAMS_FEEL_GOOD,
        Res.string.workout to HOME_PARAMS_WORKOUT,
        Res.string.party to HOME_PARAMS_PARTY,
        Res.string.commute to HOME_PARAMS_COMMUTE,
        Res.string.focus to HOME_PARAMS_FOCUS,
    )

@OptIn(ExperimentalMaterial3Api::class)
@ExperimentalFoundationApi
@Composable
fun HomeScreen(
    onScrolling: (onTop: Boolean) -> Unit = {},
    viewModel: HomeViewModel =
        koinViewModel(),
    sharedViewModel: SharedViewModel =
        koinInject(),
    navController: NavController,
) {
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberLazyListState()
    val isScrollingUp by scrollState.isScrollingUp()
    val accountInfo by viewModel.accountInfo.collectAsStateWithLifecycle()
    val homeData by viewModel.homeItemList.collectAsStateWithLifecycle()
    // Any shelf YouTube heads with the listener's own avatar over its title (Listen again is one of
    // several). Home's header already shows the avatar, so those shelves drop it and keep their name
    // and title. Matched on the name rather than on any shelf title, the way the old Welcome back row did.
    val homeShelves =
        remember(homeData, accountInfo) {
            val accountName = accountInfo?.first
            if (accountName.isNullOrBlank()) {
                homeData
            } else {
                homeData.map {
                    if (it.subtitle.equals(accountName, ignoreCase = true)) it.copy(thumbnail = null) else it
                }
            }
        }
    val newRelease by viewModel.newRelease.collectAsStateWithLifecycle()
    val chart by viewModel.chart.collectAsStateWithLifecycle()
    val moodMomentAndGenre by viewModel.exploreMoodItem.collectAsStateWithLifecycle()
    val chartLoading by viewModel.loadingChart.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    val regionChart by viewModel.regionCodeChart.collectAsStateWithLifecycle()
    val reloadDestination by sharedViewModel.reloadDestination.collectAsStateWithLifecycle()
    val pullToRefreshState = rememberPullToRefreshState()
    var isRefreshing by remember { mutableStateOf(false) }
    val chipRowState = rememberScrollState()
    val params by viewModel.params.collectAsStateWithLifecycle()
    val homeListState by viewModel.homeListState.collectAsStateWithLifecycle()
    val continuation by viewModel.continuation.collectAsStateWithLifecycle()

    val shouldShowLogInAlert by viewModel.showLogInAlert.collectAsStateWithLifecycle()

    val openAppTime by sharedViewModel.openAppTime.collectAsStateWithLifecycle()
    val shareLyricsPermissions by sharedViewModel.shareSavedLyrics.collectAsStateWithLifecycle()

    // Launch banner inputs. App.kt's own dialogs, its Now Playing sheet and whether the user has
    // started using the app come down from App.kt; Home's dialogs are read where the banner is drawn.
    val appDialogOpen = LocalAppDialogOpen.current
    val nowPlayingOpen = LocalNowPlayingOpen.current
    val userStarted by rememberUpdatedState(LocalUserStarted.current)
    // Home's own lifecycle: RESUMED only while it is the screen in front (not mid-transition to another
    // tab, not behind a system prompt). On Desktop STARTED means its window is showing.
    val homeLifecycle by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    // The language part of the BCP 47 tag: Android's Locale.language still reports the legacy "in"
    // for Indonesian and "iw" for Hebrew, while the config is keyed "id" and "he".
    val language = Locale.current.toLanguageTag().substringBefore('-')

    val backgroundColor = MaterialTheme.colorScheme.background
    val isLightTheme = backgroundColor.luminance() > 0.5f
    // What is ACTUALLY painted behind this screen. The desktop shell wraps content in a rounded
    // panel (App.kt: surfaceContainer on light, desktopPanelDark on dark) — deliberately not
    // colorScheme.background — so a gradient tail aimed at colorScheme.background ends on the
    // wrong colour and draws a seam where the first item stops. Same light check as App.kt's
    // isLightScheme.
    val pageBackground =
        if (getPlatform() == Platform.Desktop) {
            if (isLightTheme) MaterialTheme.colorScheme.surfaceContainer else desktopPanelDark
        } else {
            backgroundColor
        }
    var topHeaderColor by remember {
        mutableStateOf(backgroundColor)
    }
    val animatedColor by animateColorAsState(topHeaderColor, tween(500))
    val mainHomeThumbnail by viewModel.mainHomeThumbnail.collectAsStateWithLifecycle()
    val networkLoader = rememberNetworkLoader(HttpClient(CIO))
    val dominantColorState =
        rememberDominantColorState(
            defaultColor = backgroundColor,
            defaultOnColor = backgroundColor,
            loader = networkLoader,
        )

    LaunchedEffect(mainHomeThumbnail) {
        mainHomeThumbnail?.let {
            dominantColorState.updateFrom(Url(it))
        }
    }

    LaunchedEffect(dominantColorState, isLightTheme) {
        snapshotFlow { dominantColorState.color }.collect {
            // Light theme: pull the artwork color toward white for a soft pastel header;
            // dark theme keeps the original darkened tone.
            topHeaderColor = if (isLightTheme) lerp(it, Color.White, 0.85f) else it.rgbFactor(0.3f)
        }
    }

    var showReviewDialog by rememberSaveable {
        mutableStateOf(false)
    }
    var showRequestShareLyricsPermissions by rememberSaveable {
        mutableStateOf(false)
    }
    var showBlogPromoDialog by rememberSaveable {
        mutableStateOf(false)
    }
    var showFootgunsDialog by rememberSaveable {
        mutableStateOf(false)
    }
    // Decided once per launch (saveable, so a rotation does not count as a new launch).
    var promoLaunchDecided by rememberSaveable {
        mutableStateOf(false)
    }
    var promoBanner by remember {
        mutableStateOf<PromoBannerData?>(null)
    }
    // The banner that has gone a moment with nothing keeping it off screen; see where it is drawn.
    var promoClearFor by remember {
        mutableStateOf<PromoBannerData?>(null)
    }

    var topAppBarHeightPx by rememberSaveable {
        mutableIntStateOf(0)
    }

    var moodExpanded by rememberSaveable {
        mutableStateOf(false)
    }
    // Whether the chips have scrolled over the avatar / reached the end clear of Listen Together.
    // Derived, so the header recomposes only when one of them flips, not on every scrolled pixel.
    val chipsAtStart by remember { derivedStateOf { chipRowState.value == 0 } }
    val chipsAtEnd by remember { derivedStateOf { chipRowState.value >= chipRowState.maxValue } }
    // Derived, so scrolling recomposes only when the page reaches or leaves its very top.
    val atTop by remember {
        derivedStateOf { scrollState.firstVisibleItemIndex == 0 && scrollState.firstVisibleItemScrollOffset == 0 }
    }
    val hazeState =
        rememberHazeState()
    // The floating chip row refracts the page scrolling under it, so the page is the glass source and
    // the row its sibling (nesting the row inside the source is the RuntimeShader feedback loop).
    val chipBackdrop = rememberBackdrop(MaterialTheme.colorScheme.background)

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
        isRefreshing = true
        viewModel.getHomeItemList(params)
        Logger.w("HomeScreen", "onRefresh")
    }
    LaunchedEffect(key1 = reloadDestination) {
        if (reloadDestination == HomeDestination::class) {
            if (scrollState.firstVisibleItemIndex > 1) {
                Logger.w("HomeScreen", "scrollState.firstVisibleItemIndex: ${scrollState.firstVisibleItemIndex}")
                scrollState.animateScrollToItem(0)
                sharedViewModel.reloadDestinationDone()
            } else {
                Logger.w("HomeScreen", "scrollState.firstVisibleItemIndex: ${scrollState.firstVisibleItemIndex}")
                onRefresh.invoke()
            }
        }
    }
    LaunchedEffect(key1 = loading) {
        if (!loading) {
            isRefreshing = false
            sharedViewModel.reloadDestinationDone()
            coroutineScope.launch {
                pullToRefreshState.animateToHidden()
            }
        }
    }
    LaunchedEffect(openAppTime, shareLyricsPermissions) {
        Logger.w("HomeScreen", "openAppTime: $openAppTime, shareLyricsPermissions: $shareLyricsPermissions")
        if (openAppTime >= 10 && openAppTime % 10 == 0 && openAppTime <= 50) {
            showReviewDialog = true
        } else if ((openAppTime == 1 || openAppTime % 15 == 0) && openAppTime <= 60 && !shareLyricsPermissions) {
            showRequestShareLyricsPermissions = true
        } else if (openAppTime == 5) {
            // Blog promo: one-shot after 5 app opens, bump key suffix to re-promote later
            if (sharedViewModel.getString(BLOG_PROMO_KEY) != "true") {
                showBlogPromoDialog = true
            }
        } else if (openAppTime % 10 == 6 &&
            openAppTime <= 46 &&
            sharedViewModel.getString(FOOTGUNS_STAR_KEY) != "true"
        ) {
            // kotlin-footguns star prompt: 6, 16, 26, 36, 46 - one open after each review milestone,
            // and clear of the share-lyrics (15, 45) and blog-promo (5) milestones
            showFootgunsDialog = true
        } else {
            showReviewDialog = false
            showFootgunsDialog = false
            showRequestShareLyricsPermissions = false
        }
    }

    val shouldStartPaginate =
        remember {
            derivedStateOf {
                homeListState != ListState.PAGINATION_EXHAUST &&
                    (
                        scrollState.layoutInfo.visibleItemsInfo
                            .lastOrNull()
                            ?.index ?: -9
                    ) >= (scrollState.layoutInfo.totalItemsCount - 1)
            }
        }

    LaunchedEffect(key1 = shouldStartPaginate.value) {
        Logger.d("HomeScreen", "shouldStartPaginate: ${shouldStartPaginate.value}")
        Logger.d("HomeScreen", "homeListState: $homeListState")
        Logger.d("HomeScreen", "Continuation: $continuation")
        if (shouldStartPaginate.value && homeListState == ListState.IDLE) {
            viewModel.getContinueHomeItem(
                continuation,
            )
        }
    }

//    if (shouldShowGetDataSyncIdBottomSheet) {
//        GetDataSyncIdBottomSheet(
//            cookie = youTubeCookie,
//            onDismissRequest = {
//                shouldShowGetDataSyncIdBottomSheet = false
//            },
//        )
//    }

    if (showReviewDialog) {
        ReviewDialog(
            onDismissRequest = {
                sharedViewModel.onDoneReview(
                    isDismissOnly = true,
                )
                showReviewDialog = false
            },
            onDoneReview = {
                sharedViewModel.onDoneReview(
                    isDismissOnly = false,
                )
                showReviewDialog = false
            },
        )
    }

    if (showFootgunsDialog) {
        FootgunsStarDialog(
            onDismissRequest = {
                // "Later" advances OPEN_APP_TIME, the same way the review and share-lyrics dialogs do.
                // Home's launch effect runs again every time Home re-enters composition, reading the
                // stored count; leaving it untouched kept the milestone condition true, so the prompt
                // came back on every return to Home until the app was restarted.
                showFootgunsDialog = false
                sharedViewModel.onDoneReview(isDismissOnly = true)
            },
            onDoneStar = {
                sharedViewModel.putString(FOOTGUNS_STAR_KEY, "true")
                showFootgunsDialog = false
            },
        )
    }

    if (showBlogPromoDialog) {
        BlogPromoDialog(
            onDismissRequest = {
                sharedViewModel.putString(BLOG_PROMO_KEY, "true")
                showBlogPromoDialog = false
            },
            onVisitBlog = {
                sharedViewModel.putString(BLOG_PROMO_KEY, "true")
                showBlogPromoDialog = false
            },
        )
    }

    if (showRequestShareLyricsPermissions) {
        ShareSavedLyricsDialog(
            onDismissRequest = {
                showRequestShareLyricsPermissions = false
                sharedViewModel.onDoneReview(
                    isDismissOnly = true,
                )
            },
            onConfirm = { contributor ->
                sharedViewModel.onDoneRequestingShareLyrics(
                    contributor,
                )
            },
        )
    }

    val otherDialogOpen =
        appDialogOpen || showReviewDialog || showRequestShareLyricsPermissions || showBlogPromoDialog ||
            showFootgunsDialog || shouldShowLogInAlert
    // The launch banner, decided once per launch. It waits for Home to settle on its own dialogs (the
    // open count and the lyrics-sharing flag load just after the first frame) and for the update check
    // started at launch, however long that takes, so an update dialog comes first. Then it takes the
    // first banner this version has not shown yet.
    LaunchedEffect(Unit) {
        if (promoLaunchDecided) return@LaunchedEffect
        // Marked at the start, not the end: leaving Home cancels this for good (by then the user is
        // busy elsewhere), and a value written while Home is being left would not be saved.
        promoLaunchDecided = true
        snapshotFlow { openAppTime }.first { it > 0 }
        delay(PROMO_SETTLE_MS)
        sharedViewModel.isCheckingUpdate.first { !it }
        if (userStarted) {
            Logger.i("Promo", "Skipped: the user started before the banner's turn")
            return@LaunchedEffect
        }
        val localTracking = sharedViewModel.getLocalTrackingEnabled().first() == TRUE
        val youTubeLoggedIn = sharedViewModel.getYouTubeLoggedIn().first() == TRUE
        val shown = sharedViewModel.shownPromos()
        val promos = sharedViewModel.cachedPromos().first()
        val eligible = eligiblePromos(promos, VersionManager.getVersionName(), language, localTracking, youTubeLoggedIn)
        val banner =
            eligible
                .firstOrNull { (promo, _) -> promo.id.orEmpty() !in shown }
                ?.let { (promo, url) -> PromoBannerData(id = promo.id.orEmpty(), imageUrl = url, link = promo.link.orEmpty()) }
        when {
            banner == null ->
                Logger.i(
                    "Promo",
                    "Nothing to show: ${eligible.size} of ${promos.size} banners usable here " +
                        "(local tracking ${if (localTracking) "on" else "off"}, " +
                        "YouTube ${if (youTubeLoggedIn) "signed in" else "signed out"}), " +
                        "already shown in ${VersionManager.getVersionName()}: $shown",
                )

            userStarted -> Logger.i("Promo", "Skipped ${banner.id}: the user started before its turn")

            else -> {
                Logger.i("Promo", "Picked ${banner.id}, waiting for its turn")
                promoBanner = banner
            }
        }
    }
    // Everything that keeps the banner off screen: any other dialog (it waits its turn behind them),
    // Now Playing, and Home not being in front. On Desktop that only asks for a showing window: one
    // opened from a terminal or an IDE may start without focus, and the click that focuses it would
    // count as the user starting, so the banner would never get its turn.
    val homeInFront =
        homeLifecycle.isAtLeast(
            if (getPlatform() == Platform.Desktop) Lifecycle.State.STARTED else Lifecycle.State.RESUMED,
        )
    val promoBlocked = otherDialogOpen || nowPlayingOpen || !homeInFront
    // It draws only once the banner has gone a moment unblocked since it was decided. The wait belongs
    // to that banner, so nothing left over from before it arrived can let it through early: it never
    // flashes in the gap between two dialogs, and the update dialog, raised a frame after its check
    // ends, still gets there first.
    LaunchedEffect(promoBlocked, promoBanner) {
        val banner = promoBanner
        promoClearFor = null
        if (!promoBlocked && banner != null) {
            delay(PROMO_CLEAR_MS)
            promoClearFor = banner
        }
    }
    // Whatever the user starts before the banner's turn puts it off to a later launch, uncounted.
    LaunchedEffect(userStarted) {
        val pending = promoBanner
        if (userStarted && pending != null) {
            Logger.i("Promo", "Put off ${pending.id}: the user started before its turn")
            promoBanner = null
        }
    }
    promoBanner?.takeIf { it == promoClearFor && !promoBlocked }?.let { banner ->
        LaunchedEffect(banner.id) { Logger.i("Promo", "On screen: ${banner.id}") }
        PromoBanner(
            banner = banner,
            onOpen = {
                Logger.i("Promo", "Opened ${banner.id}: ${banner.link}")
                promoBanner = null
                sharedViewModel.markPromoShown(banner.id)
                val link = banner.link
                when {
                    // The same router as links from the web and the widgets.
                    link.startsWith("simpmusic://") -> {
                        runCatching { Uri.parse(link) }.getOrNull()?.let {
                            sharedViewModel.setIntent(GenericIntent(data = it))
                        }
                    }

                    // openUrl rather than LocalUriHandler, which throws where no browser answers.
                    link.startsWith("https://", ignoreCase = true) -> {
                        runCatching { openUrl(link) }
                    }
                    // Anything else is a mistake in the config: ignoring it beats crashing on a tap.
                }
            },
            onClose = {
                Logger.i("Promo", "Closed ${banner.id}")
                promoBanner = null
                sharedViewModel.markPromoShown(banner.id)
            },
            // No picture, no banner: closed without counting, so a later launch tries again.
            onFailed = {
                Logger.w("Promo", "Picture failed to load for ${banner.id}; closed without counting it")
                promoBanner = null
            },
        )
    }

    if (shouldShowLogInAlert) {
        var doNotShowAgain by rememberSaveable {
            mutableStateOf(false)
        }
        AlertDialog(
            title = {
                Text(stringResource(Res.string.warning))
            },
            text = {
                Column {
                    Text(text = stringResource(Res.string.log_in_warning))
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier =
                            Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    doNotShowAgain = !doNotShowAgain
                                }.fillMaxWidth(),
                    ) {
                        Checkbox(
                            checked = doNotShowAgain,
                            onCheckedChange = {
                                doNotShowAgain = it
                            },
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(stringResource(Res.string.do_not_show_again))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.doneShowLogInAlert(doNotShowAgain)
                    navController.navigate(LoginDestination)
                }) {
                    Text(stringResource(Res.string.go_to_log_in_page))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.doneShowLogInAlert(doNotShowAgain)
                }) {
                    Text(stringResource(Res.string.cancel))
                }
            },
            onDismissRequest = {
                viewModel.doneShowLogInAlert()
            },
        )
    }

    Box {
        PullToRefreshBox(
            modifier =
                Modifier
                    .then(if (LocalLiquidGlassEnabled.current) Modifier.layerBackdrop(chipBackdrop) else Modifier)
                    .hazeSource(hazeState),
            state = pullToRefreshState,
            onRefresh = onRefresh,
            isRefreshing = isRefreshing,
            indicator = {
                PullToRefreshDefaults.Indicator(
                    state = pullToRefreshState,
                    isRefreshing = isRefreshing,
                    modifier =
                        Modifier
                            .align(Alignment.TopCenter)
                            .padding(
                                top =
                                    with(LocalDensity.current) {
                                        topAppBarHeightPx.toDp()
                                    },
                            ),
                    containerColor = PullToRefreshDefaults.indicatorContainerColor,
                    color = PullToRefreshDefaults.indicatorColor,
                    maxDistance = PullToRefreshDefaults.PositionalThreshold,
                )
            },
        ) {
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
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        itemsIndexed(homeShelves, key = { _, item ->
                            item.hashCode().toString() + (mainHomeThumbnail ?: "nothumb")
                        }) { index, item ->
                            Box {
                                if (index == 0) {
                                    Box(
                                        modifier =
                                            Modifier
                                                // matchParentSize, not height(300.dp): 300 is the
                                                // height of this shelf ON A PHONE. On a desktop
                                                // window the first item is taller, the gradient
                                                // stopped mid-item and everything below it fell
                                                // back to the flat background — a hard colour seam
                                                // straight across Home. Sized by the item, the
                                                // bottom scrim always lands on the item's edge.
                                                .matchParentSize()
                                                // The glow used to reach down past the old app bar and
                                                // the Welcome back row, both gone with the Spotify
                                                // header. It keeps that reach by running on below the
                                                // first item instead of making the item taller; what
                                                // runs over is the scrim's tail, already faded out.
                                                .layout { measurable, constraints ->
                                                    val height = constraints.maxHeight + HomeGlowOverflow.roundToPx()
                                                    val placeable =
                                                        measurable.measure(constraints.copy(minHeight = height, maxHeight = height))
                                                    layout(constraints.maxWidth, constraints.maxHeight) { placeable.place(0, 0) }
                                                }.angledGradientBackground(listOf(animatedColor, pageBackground), 25f),
                                    ) {
                                        Box(
                                            modifier =
                                                Modifier
                                                    .fillMaxWidth()
                                                    // A share of the glow, not a fixed 180dp: on a glow
                                                    // ~560dp tall, 180dp made a long flat field that
                                                    // dropped off in one short step.
                                                    .fillMaxHeight(0.6f)
                                                    .align(Alignment.BottomCenter)
                                                    .background(artworkScrimBrush(pageBackground)),
                                        )
                                    }
                                }
                                Column(
                                    modifier =
                                        Modifier
                                            .padding(horizontal = 15.dp),
                                ) {
                                    if (index == 0) {
                                        Spacer(
                                            Modifier.height(
                                                with(LocalDensity.current) { topAppBarHeightPx.toDp() },
                                            ),
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    if (item.title == stringResource(Res.string.quick_picks)) {
                                        AnimatedVisibility(
                                            visible =
                                                homeData.find {
                                                    it.title ==
                                                        stringResource(
                                                            Res.string.quick_picks,
                                                        )
                                                } != null,
                                        ) {
                                            QuickPicks(
                                                homeItem =
                                                    (
                                                        homeData.find {
                                                            it.title ==
                                                                stringResource(
                                                                    Res.string.quick_picks,
                                                                )
                                                        } ?: return@AnimatedVisibility
                                                    ).let { content ->
                                                        content.copy(
                                                            contents =
                                                                content.contents.mapNotNull { ct ->
                                                                    ct?.copy(
                                                                        artists =
                                                                            ct.artists?.let { art ->
                                                                                if (art.size > 1) {
                                                                                    art.dropLast(1)
                                                                                } else {
                                                                                    art
                                                                                }
                                                                            },
                                                                    )
                                                                },
                                                        )
                                                    },
                                                navController = navController,
                                                viewModel = viewModel,
                                            )
                                        }
                                    } else if (item.isSongCardShelf()) {
                                        HomeDiscoverSection(
                                            data = item,
                                            navController = navController,
                                            homeViewModel = viewModel,
                                        )
                                    } else {
                                        HomeItem(
                                            navController = navController,
                                            data = item,
                                        )
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
                                CenterLoadingBox(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .height(200.dp),
                                )
                            }
                        }
                        if (homeListState == ListState.PAGINATION_EXHAUST) {
                            items(newRelease, key = { it.hashCode() }) {
                                AnimatedVisibility(
                                    visible = newRelease.isNotEmpty(),
                                ) {
                                    Box(
                                        modifier =
                                            Modifier
                                                .padding(horizontal = 15.dp),
                                    ) {
                                        HomeItem(
                                            navController = navController,
                                            data = it,
                                        )
                                    }
                                }
                            }
                            item {
                                AnimatedVisibility(
                                    visible = moodMomentAndGenre != null,
                                ) {
                                    Box(
                                        modifier =
                                            Modifier
                                                .padding(horizontal = 15.dp),
                                    ) {
                                        moodMomentAndGenre?.let {
                                            MoodMomentAndGenre(
                                                mood = it,
                                                navController = navController,
                                            )
                                        }
                                    }
                                }
                            }
                            item {
                                Column(
                                    Modifier
                                        .padding(vertical = 10.dp)
                                        .padding(horizontal = 15.dp),
                                    verticalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    ChartTitle()
                                    Spacer(modifier = Modifier.height(5.dp))
                                    Crossfade(targetState = regionChart) {
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
                        }
                        item {
                            EndOfPage()
                        }
                    }
                } else {
                    Column {
                        Spacer(
                            Modifier.height(
                                with(LocalDensity.current) {
                                    topAppBarHeightPx.toDp()
                                },
                            ),
                        )
                        HomeShimmer()
                    }
                }
            }
        }
        // Spotify's header: the avatar, the chips and Listen Together on one row, with no app bar. At the
        // top of the page it sits straight on the artwork glow; once the page scrolls under it, the blurred
        // bar fades in behind it.
        AnimatedContent(
            targetState = atTop,
            transitionSpec = {
                fadeIn(tween(300)).togetherWith(fadeOut(tween(300)))
            },
        ) { target ->
            Column(
                modifier =
                    Modifier
                        .align(Alignment.TopCenter)
                        .then(
                            if (target) {
                                Modifier.background(Color.Transparent)
                            } else {
                                Modifier
                                    .hazeBlur(HazeInput.Sources(hazeState), ultraThinBarStyle())
                            },
                        ).onGloballyPositioned { coordinates ->
                            topAppBarHeightPx = coordinates.size.height
                        },
            ) {
                Spacer(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(
                                WindowInsets.statusBars,
                            ),
                )
                // The chips scroll across the whole header and pass OVER the avatar and the end buttons,
                // as Spotify's do, instead of being cut off between them. Padding keeps them clear of
                // both at rest. A button sits above the chips (for drawing and for taps) only while the
                // chips have not scrolled over it, so whichever is on top is also what a tap reaches.
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                ) {
                    Box(
                        modifier =
                            Modifier
                                .align(Alignment.CenterStart)
                                .padding(start = 15.dp)
                                .zIndex(if (chipsAtStart) 2f else 0f),
                    ) {
                        HomeAccountButton(
                            accountName = accountInfo?.first,
                            thumbnailUrl = accountInfo?.second,
                            navController = navController,
                        )
                    }
                    Row(
                        modifier =
                            Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = 4.dp)
                                .zIndex(if (chipsAtEnd) 2f else 0f),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RippleIconButton(imageVector = SimpIcons.Notifications, tint = MaterialTheme.colorScheme.onBackground) {
                            navController.navigate(NotificationDestination)
                        }
                        ListenTogetherIconButton { navController.navigate(ListenTogetherDestination) }
                        RippleIconButton(imageVector = SimpIcons.Settings, tint = MaterialTheme.colorScheme.onBackground) {
                            navController.navigate(SettingsDestination)
                        }
                    }
                    Row(
                        modifier =
                            Modifier
                                .align(Alignment.CenterStart)
                                .fillMaxWidth()
                                .zIndex(1f)
                                .horizontalScroll(chipRowState)
                                // Avatar (15 + 32dp) and an 8dp gap; the three 48dp end buttons and their 4dp inset.
                                .padding(start = 55.dp, end = 148.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Chip(
                            isAnimated = loading,
                            isSelected = params == null && !moodExpanded,
                            text = stringResource(Res.string.all),
                            backdrop = chipBackdrop,
                        ) {
                            moodExpanded = false
                            viewModel.setParams(null)
                        }
                        ExpandableChip(
                            text = stringResource(Res.string.mood),
                            options = homeMoods.map { stringResource(it.first) },
                            selectedIndex = homeMoods.indexOfFirst { it.second == params }.takeIf { it >= 0 },
                            expanded = moodExpanded,
                            onExpandedChange = { moodExpanded = it },
                            onSelect = { index ->
                                moodExpanded = false
                                viewModel.setParams(homeMoods[index].second)
                            },
                            isAnimated = loading,
                            backdrop = chipBackdrop,
                        )
                    }
                }
            }
        }
    }
}

/**
 * The avatar at the start of Home's header. It opens a menu showing who is signed in and the
 * greeting the old app bar used to show. Signed out, the avatar is a person glyph and the menu
 * offers the log-in.
 */
@Composable
private fun HomeAccountButton(
    accountName: String?,
    thumbnailUrl: String?,
    navController: NavController,
) {
    var menuOpen by remember { mutableStateOf(false) }
    // Sign-out blanks the stored account name, so a blank name is the signed-out state.
    val signedIn = !accountName.isNullOrBlank()
    val displayName = if (signedIn) accountName.orEmpty() else stringResource(Res.string.guest)
    val hour = remember { now().time.hour }
    val greeting =
        stringResource(
            when (hour) {
                in 6..12 -> Res.string.good_morning
                in 13..17 -> Res.string.good_afternoon
                in 18..23 -> Res.string.good_evening
                else -> Res.string.good_night
            },
        )
    Box {
        AccountAvatar(
            signedIn = signedIn,
            thumbnailUrl = thumbnailUrl,
            size = 32.dp,
            contentDescription = displayName,
            modifier = Modifier.clickable { menuOpen = true },
        )
        DropdownMenu(
            expanded = menuOpen,
            onDismissRequest = { menuOpen = false },
            // The draft's 280dp (also Material's widest menu item) instead of wrapping its content,
            // which left the menu as narrow as its longest row. Capped, not fixed: a window narrower
            // than that gets a menu as wide as the window instead of one that runs off it.
            modifier =
                Modifier
                    .widthIn(max = 280.dp)
                    .fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AccountAvatar(
                    signedIn = signedIn,
                    thumbnailUrl = thumbnailUrl,
                    size = 40.dp,
                    contentDescription = null,
                )
                Column {
                    Text(text = displayName, style = typo().titleSmall, maxLines = 1)
                    Text(text = greeting, style = typo().bodySmall)
                }
            }
            if (!signedIn) {
                Button(
                    onClick = {
                        menuOpen = false
                        navController.navigate(LoginDestination)
                    },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                ) {
                    Text(stringResource(Res.string.log_in_to_YouTube), color = LocalContentColor.current)
                }
            }
        }
    }
}

@Composable
private fun AccountAvatar(
    signedIn: Boolean,
    thumbnailUrl: String?,
    size: Dp,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    val avatarModifier =
        Modifier
            .size(size)
            .clip(CircleShape)
            .then(modifier)
    if (signedIn && !thumbnailUrl.isNullOrBlank()) {
        AsyncImage(
            model =
                ImageRequest
                    .Builder(LocalPlatformContext.current)
                    .data(thumbnailUrl)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .diskCacheKey(thumbnailUrl)
                    .crossfade(true)
                    .build(),
            placeholder = rememberHolderPainter(),
            error = rememberHolderPainter(),
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = avatarModifier,
        )
    } else {
        Box(
            modifier = avatarModifier.background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = SimpIcons.Person,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(size * 0.6f),
            )
        }
    }
}

// Portrait fills the width with one column (the next one peeking in). On a landscape window that one
// column stretched across the whole screen, so every row there is capped instead.
private val LandscapeGridItemMaxWidth = 400.dp

// A card shelf of songs only that leads to no page of its own ("Your daily discover"). List shelves
// (Quick picks, Heard in Shorts) carry itemsPerColumn and keep their own look; a shelf with a More
// page (Listen again can be all songs) keeps its cards; videos draw as wide cards, so never match.
private fun HomeItem.isSongCardShelf(): Boolean =
    itemsPerColumn == null &&
        moreEndpoint == null &&
        contents.isNotEmpty() &&
        contents.all { it != null && !it.videoId.isNullOrEmpty() && it.homeCardAspectRatio() == 1f }

@ExperimentalFoundationApi
@Composable
fun QuickPicks(
    homeItem: HomeItem,
    navController: NavController,
    viewModel: HomeViewModel = koinViewModel(),
) {
    val lazyListState = rememberLazyGridState()
    val snapperFlingBehavior = rememberSnapFlingBehavior(SnapLayoutInfoProvider(lazyGridState = lazyListState, snapPosition = SnapPosition.Start))
    val isPortrait = getScreenSizeInfo().let { it.wDP < it.hDP }
    val density = LocalDensity.current
    var widthDp by remember {
        mutableStateOf(0.dp)
    }
    var bottomSheetShow by remember { mutableStateOf(false) }
    var track by remember { mutableStateOf<Track?>(null) }

    if (bottomSheetShow) {
        NowPlayingBottomSheet(
            onDismiss = { bottomSheetShow = false },
            song = track?.toSongEntity(),
            navController = navController,
        )
    }

    Column(
        Modifier
            .padding(vertical = 8.dp)
            .onGloballyPositioned { coordinates ->
                with(density) {
                    widthDp = (coordinates.size.width).toDp()
                }
            },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(Res.string.let_s_start_with_a_radio),
                    style = typo().bodySmall,
                )
                Text(
                    text = stringResource(Res.string.quick_picks),
                    style = typo().headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp),
                )
            }
            if (homeItem.playAllEndpoint != null) {
                TextButton(
                    onClick = { viewModel.playAll(homeItem) },
                    colors =
                        ButtonDefaults
                            .textButtonColors()
                            .copy(contentColor = MaterialTheme.colorScheme.onSurface),
                ) {
                    Text(stringResource(Res.string.play_all), style = typo().bodySmall)
                }
            }
        }
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
                            viewModel.loadMediaItem(
                                firstQueue,
                                type = Config.SONG_CLICK,
                            )
                        },
                        onLongClick = {
                            track = it.toTrack()
                            bottomSheetShow = true
                        },
                        data = it,
                        widthDp = if (isPortrait) widthDp else minOf(widthDp, LandscapeGridItemMaxWidth + 30.dp),
                    )
                }
            }
        }
        HorizontalScrollBar(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            scrollState = lazyListState,
            flingBehavior = snapperFlingBehavior,
        )
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
            text = stringResource(Res.string.let_s_pick_a_playlist_for_you),
            style = typo().bodyMedium,
        )
        // One block per section YouTube returned, headed by ITS OWN title. Hard-coding
        // "Moods & moment" / "Genre" here (and reading mood.moodsMoments / mood.genres by
        // index) mislabelled every row as soon as a signed-in account got an extra
        // "For you" section, and hid the real Genres section altogether.
        mood.sections.forEach { section ->
            val gridState = rememberLazyGridState()
            val flingBehavior = rememberSnapFlingBehavior(SnapLayoutInfoProvider(lazyGridState = gridState))
            Text(
                text = section.title,
                style = typo().headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp),
            )
            LazyHorizontalGrid(
                rows = GridCells.Fixed(3),
                modifier = Modifier.height(210.dp),
                state = gridState,
                flingBehavior = flingBehavior,
            ) {
                items(section.items, key = { it.params }) { item ->
                    MoodMomentAndGenreHomeItem(
                        title = item.title,
                        stripeColor = item.stripeColor,
                    ) {
                        navController.navigate(
                            MoodDestination(
                                item.params,
                            ),
                        )
                    }
                }
            }
            HorizontalScrollBar(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                scrollState = gridState,
                flingBehavior = flingBehavior,
            )
        }
    }
}

@Composable
fun ChartTitle() {
    Column {
        Text(
            text = stringResource(Res.string.what_is_best_choice_today),
            style = typo().bodyMedium,
        )
        Text(
            text = stringResource(Res.string.chart),
            style = typo().headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
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
) {
    var gridWidthDp by remember {
        mutableStateOf(0.dp)
    }
    val density = LocalDensity.current

    val lazyListState2 = rememberLazyGridState()
    val snapperFlingBehavior2 = rememberSnapFlingBehavior(SnapLayoutInfoProvider(lazyGridState = lazyListState2))
    val isPortrait = getScreenSizeInfo().let { it.wDP < it.hDP }

    Column(
        Modifier.onGloballyPositioned { coordinates ->
            with(density) {
                gridWidthDp = (coordinates.size.width).toDp()
            }
        },
    ) {
        chart.listChartItem.forEach { item ->
            Text(
                text = item.title,
                style = typo().headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
            )
            val lazyListState = rememberLazyListState()
            val snapperFlingBehavior = rememberSnapFlingBehavior(SnapLayoutInfoProvider(lazyListState = lazyListState))
            LazyRow(flingBehavior = snapperFlingBehavior) {
                items(item.playlists.size, key = { index ->
                    val data = item.playlists[index]
                    data.id + data.title + index
                }) {
                    HomeItemContentPlaylist(
                        onClick = {
                            navController.navigate(
                                PlaylistDestination(
                                    playlistId = item.playlists[it].id,
                                    isYourYouTubePlaylist = false,
                                ),
                            )
                        },
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
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
        )
        LazyHorizontalGrid(
            rows = GridCells.Fixed(3),
            modifier = Modifier.height(240.dp),
            state = lazyListState2,
            flingBehavior = snapperFlingBehavior2,
        ) {
            items(chart.artists.itemArtists.size, key = { index ->
                val item = chart.artists.itemArtists[index]
                item.title + item.browseId + index
            }) {
                val data = chart.artists.itemArtists[it]
                ItemArtistChart(
                    onClick = {
                        navController.navigate(
                            ArtistDestination(
                                channelId = data.browseId,
                            ),
                        )
                    },
                    data = data,
                    widthDp = if (isPortrait) gridWidthDp else minOf(gridWidthDp, LandscapeGridItemMaxWidth),
                )
            }
        }
        HorizontalScrollBar(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            scrollState = lazyListState2,
            flingBehavior = snapperFlingBehavior2,
        )
        // Ranked podcast shows, laid out exactly like the artist chart above.
        chart.podcasts?.let { podcasts ->
            Text(
                text = podcasts.title,
                style = typo().headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
            )
            val podcastGridState = rememberLazyGridState()
            val podcastFlingBehavior = rememberSnapFlingBehavior(SnapLayoutInfoProvider(lazyGridState = podcastGridState))
            LazyHorizontalGrid(
                rows = GridCells.Fixed(3),
                modifier = Modifier.height(240.dp),
                state = podcastGridState,
                flingBehavior = podcastFlingBehavior,
            ) {
                items(podcasts.shows.size, key = { index -> podcasts.shows[index].browseId + index }) {
                    val data = podcasts.shows[it]
                    ItemArtistChart(
                        onClick = { navController.navigate(PodcastDestination(podcastId = data.browseId)) },
                        data = data,
                        widthDp = if (isPortrait) gridWidthDp else minOf(gridWidthDp, LandscapeGridItemMaxWidth),
                        thumbnailShape = RoundedCornerShape(8.dp),
                        subtitle = data.subscribers,
                    )
                }
            }
            HorizontalScrollBar(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                scrollState = podcastGridState,
                flingBehavior = podcastFlingBehavior,
            )
        }
    }
}