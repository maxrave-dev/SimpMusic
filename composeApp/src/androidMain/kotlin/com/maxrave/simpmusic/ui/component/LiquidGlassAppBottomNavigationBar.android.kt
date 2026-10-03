package com.maxrave.simpmusic.ui.component

import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.ConstraintSet
import androidx.constraintlayout.compose.Dimension
import androidx.constraintlayout.compose.Visibility
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.currentBackStackEntryAsState
import com.maxrave.domain.data.player.GenericMediaItem
import com.maxrave.logger.Logger
import com.maxrave.simpmusic.expect.ui.PlatformBackdrop
import com.maxrave.simpmusic.ui.navigation.destination.home.AnalyticsDestination
import com.maxrave.simpmusic.ui.navigation.destination.home.HomeDestination
import com.maxrave.simpmusic.ui.navigation.destination.library.LibraryDestination
import com.maxrave.simpmusic.ui.navigation.destination.library.MixForYouDestination
import com.maxrave.simpmusic.ui.navigation.destination.search.SearchDestination
import com.maxrave.simpmusic.ui.screen.MiniPlayer
import com.maxrave.simpmusic.ui.theme.LocalIsDarkTheme
import com.maxrave.simpmusic.viewModel.SharedViewModel
import kotlin.reflect.KClass

private const val TAG = "LiquidGlassAppBottomNavigationBar"

// How long the bar takes to morph between the tab bar and the collapsed pill.
private const val BAR_MORPH_MS = 300
private val BarEdgePadding = 16.dp
private val SearchGap = 12.dp
private val SearchSize = 56.dp

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
actual fun LiquidGlassAppBottomNavigationBar(
    startDestination: Any,
    navController: NavController,
    backdrop: PlatformBackdrop,
    viewModel: SharedViewModel,
    isScrolledToTop: Boolean,
    showAnalyticsTab: Boolean,
    showMixForYouTab: Boolean,
    onOpenNowPlaying: () -> Unit,
    liquidGlass: Boolean,
    reloadDestinationIfNeeded: (KClass<*>) -> Unit,
) {
    val layer = rememberGraphicsLayer()
    val searchFabInteraction = rememberGlassInteraction()
    // Only the glass reads what is behind it.
    val luminance = rememberGlassLuminance(layer, enabled = liquidGlass)

    val nowPlayingData by viewModel.nowPlayingState.collectAsStateWithLifecycle()
    // MiniPlayer visibility: derived, never stored.
    //
    // This is a second copy of the rule App.kt applies to the plain bottom bar, and it carried the
    // same two faults. rememberSaveable(true) makes the first composition assert "a track is
    // playing" before anything knows — nowPlayingState starts null and only fills in once the
    // service has connected and the queue has been restored — and the LaunchedEffect that
    // corrected it could only run AFTER that frame had already been drawn. So the bar laid itself
    // out with the mini player, dropped it, then brought it back, animating each step through
    // decoupledConstraints.
    //
    // Fixing the copy in App.kt did nothing here, because with liquid glass on it is THIS file
    // that draws the mini player.
    val isShowMiniPlayer by remember {
        derivedStateOf {
            val item = nowPlayingData?.mediaItem
            item != null && item != GenericMediaItem.EMPTY
        }
    }

    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val bottomNavScreens =
        listOfNotNull(
            BottomNavScreen.Home,
            BottomNavScreen.MixForYou.takeIf { showMixForYouTab },
            BottomNavScreen.Analytics.takeIf { showAnalyticsTab },
            BottomNavScreen.Library,
            BottomNavScreen.Search,
        )
    // Tabs shown in the sliding bar (Apple Music style); Search lives in its own FAB.
    val barTabs =
        listOfNotNull(
            BottomNavScreen.Home,
            BottomNavScreen.MixForYou.takeIf { showMixForYouTab },
            BottomNavScreen.Analytics.takeIf { showAnalyticsTab },
            BottomNavScreen.Library,
        )
    var selectedIndex by rememberSaveable {
        mutableIntStateOf(
            when (startDestination) {
                is HomeDestination -> BottomNavScreen.Home.ordinal
                is SearchDestination -> BottomNavScreen.Search.ordinal
                is LibraryDestination -> BottomNavScreen.Library.ordinal
                is AnalyticsDestination -> BottomNavScreen.Analytics.ordinal
                is MixForYouDestination -> BottomNavScreen.MixForYou.ordinal
                else -> BottomNavScreen.Home.ordinal // Default to Home if not recognized
            },
        )
    }
    // A tab can disappear from the bar under the user: tracking gets turned off while Analytics is
    // selected, or the YouTube session ends while Mix for you is. Fall back to Home in both cases so
    // nothing is left highlighted.
    LaunchedEffect(showAnalyticsTab, showMixForYouTab) {
        if ((!showAnalyticsTab && selectedIndex == BottomNavScreen.Analytics.ordinal) ||
            (!showMixForYouTab && selectedIndex == BottomNavScreen.MixForYou.ordinal)
        ) {
            selectedIndex = BottomNavScreen.Home.ordinal
        }
    }
    var isExpanded by rememberSaveable {
        mutableStateOf(true)
    }

    var isInSearchDestination by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(currentBackStackEntry) {
        currentBackStackEntry?.destination?.let { current ->
            Logger.d(TAG, "LiquidGlassAppBottomNavigationBar: current route: ${current.route}")
            isInSearchDestination = current.hasRoute(SearchDestination::class)
        }
    }

    LaunchedEffect(isInSearchDestination) {
        isExpanded = !isInSearchDestination
    }

    // Flat: the bar's own 16dp side edges, so the two line up, and its 8dp gaps.
    val miniPlayerPadding = if (liquidGlass) 12.dp else 16.dp
    val miniPlayerGap = if (liquidGlass) 12.dp else 8.dp
    val constraintSet =
        remember(isShowMiniPlayer, isExpanded, liquidGlass) {
            decoupledConstraints(isShowMiniPlayer, isExpanded, miniPlayerPadding, miniPlayerGap)
        }

    LaunchedEffect(isScrolledToTop) {
        Logger.d(TAG, "isScrolledToTop: $isScrolledToTop")
        if (!isInSearchDestination) {
            isExpanded = isScrolledToTop
        }
    }

    fun selectTab(index: Int) {
        val screen = bottomNavScreens.find { it.ordinal == index } ?: return
        if (selectedIndex == index) {
            if (currentBackStackEntry?.destination?.hierarchy?.any {
                    it.hasRoute(screen.destination::class)
                } == true
            ) {
                reloadDestinationIfNeeded(screen.destination::class)
            } else {
                navController.navigate(screen.destination)
            }
        } else {
            selectedIndex = index
            navController.navigate(screen.destination) {
                popUpTo(navController.graph.startDestinationId) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    // The folded bar is one circle holding the selected tab; tapping it opens the bar again.
    val collapsedCircle: @Composable () -> Unit = {
        val selectedScreen = bottomNavScreens.find { it.ordinal == selectedIndex } ?: BottomNavScreen.Home
        Box(
            modifier = Modifier.fillMaxSize().clickable(enabled = !isExpanded) { isExpanded = true },
            contentAlignment = Alignment.Center,
        ) {
            if (liquidGlass) {
                selectedScreen.icon()
            } else {
                // The flat bar marks its selected tab in primary, and this circle is that tab.
                CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.primary) {
                    selectedScreen.icon()
                }
            }
        }
    }

    BoxWithConstraints(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    WindowInsets.navigationBars.asPaddingValues(),
                ).padding(
                    bottom = 8.dp,
                )
                // The flat bar never rode above the keyboard, so it still does not.
                .then(if (liquidGlass) Modifier.imePadding() else Modifier),
    ) {
        // The capsule's budget: the row minus its edge padding, the gap and the search FAB. The
        // capsule caps its tabs at TabWidth, so with two or three tabs it ends up narrower and the
        // toolbar centres capsule + gap + FAB as one cluster (see decoupledConstraints).
        val capsuleBudget = maxWidth - BarEdgePadding * 2 - SearchGap - SearchSize
        val capsuleWidth =
            if (liquidGlass) {
                liquidGlassTabBarWidth(barTabs.size, capsuleBudget)
            } else {
                flatTabBarWidth(barTabs.size, capsuleBudget)
            }
        ConstraintLayout(
            constraintSet = constraintSet,
            modifier = Modifier.fillMaxWidth(),
            animateChangesSpec = tween(BAR_MORPH_MS),
        ) {
            /**
             * LTR: HOME -> MIX FOR YOU -> LIBRARY | SEARCH
             *
             * ConstraintLayout plays the toolbar's frame from the folded circle out to the full bar
             * and measures it at every width in between. The capsule is handed that growth, so the
             * whole bar opens from the start edge in step with the mini player — never a circle
             * drifting across and then swapped for the bar.
             */
            Layout(
                modifier = Modifier.layoutId("toolbar"),
                content = {
                    // Exactly two children, in this order, for the measure policy below.
                    if (liquidGlass) {
                        LiquidGlassTabBar(
                            tabs = barTabs,
                            selectedTab = barTabs.indexOfFirst { it.ordinal == selectedIndex },
                            backdrop = backdrop,
                            layer = layer,
                            luminance = luminance,
                            availableWidth = capsuleBudget,
                            onTabSelected = { position -> selectTab(barTabs[position].ordinal) },
                            collapsedContent = collapsedCircle,
                        )
                        // Search lives in its own circular glass FAB (Apple Music style).
                        Box(
                            modifier =
                                Modifier
                                    .size(SearchSize)
                                    .drawInteractiveGlass(
                                        LocalIsDarkTheme.current,
                                        backdrop,
                                        // Only the capsule records the shared sample; Search must not overwrite it.
                                        null,
                                        { luminance.value },
                                        CircleShape,
                                        searchFabInteraction,
                                    ).clickable { selectTab(BottomNavScreen.Search.ordinal) },
                            contentAlignment = Alignment.Center,
                        ) {
                            BottomNavScreen.Search.icon()
                        }
                    } else {
                        FlatTabBar(
                            tabs = barTabs,
                            selectedIndex = selectedIndex,
                            availableWidth = capsuleBudget,
                            onTabSelected = { screen -> selectTab(screen.ordinal) },
                            collapsedContent = collapsedCircle,
                        )
                        FlatSearchButton(
                            selected = selectedIndex == BottomNavScreen.Search.ordinal,
                            onClick = { selectTab(BottomNavScreen.Search.ordinal) },
                        )
                    }
                },
            ) { measurables, constraints ->
                val folded = CollapsedBarSize.roundToPx()
                val capsule = capsuleWidth.roundToPx()
                val gap = SearchGap.roundToPx()
                val search = SearchSize.roundToPx()
                val expanded = capsule + gap + search
                val width =
                    if (constraints.hasFixedWidth) {
                        constraints.maxWidth
                    } else {
                        expanded.coerceIn(constraints.minWidth, constraints.maxWidth)
                    }
                val height =
                    if (constraints.hasFixedHeight) {
                        constraints.maxHeight
                    } else {
                        BarHeight.roundToPx().coerceIn(constraints.minHeight, constraints.maxHeight)
                    }
                val progress = ((width - folded).toFloat() / (expanded - folded).coerceAtLeast(1)).coerceIn(0f, 1f)
                val bar = measurables[0].measure(Constraints.fixed(lerp(folded, capsule, progress), height))
                val fab = measurables[1].measure(Constraints.fixed(search, search))
                val fabAlpha = ((progress - 0.5f) / 0.5f).coerceIn(0f, 1f)
                layout(width, height) {
                    bar.placeRelative(0, 0)
                    // Unplaced while hidden, so the folded bar leaves no invisible button behind.
                    if (fabAlpha > 0f) {
                        fab.placeRelativeWithLayer(capsule + gap, (height - search) / 2) {
                            alpha = fabAlpha
                            scaleX = lerp(0.8f, 1f, fabAlpha)
                            scaleY = scaleX
                        }
                    }
                }
            }
            MiniPlayer(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = miniPlayerPadding)
                    .height(56.dp)
                    .layoutId("miniPlayer"),
                backdrop = backdrop,
                isVisible = isShowMiniPlayer,
                onClick = {
                    onOpenNowPlaying()
                },
                onClose = {
                    viewModel.stopPlayer()
                    viewModel.isServiceRunning = false
                },
            )
        }
    }
}

private fun decoupledConstraints(
    isMiniplayerShow: Boolean = true,
    isExpanded: Boolean,
    miniPlayerPadding: Dp,
    miniPlayerGap: Dp,
): ConstraintSet =
    ConstraintSet {
        val toolbar = createRefFor("toolbar")
        constrain(toolbar) {
            bottom.linkTo(parent.bottom)
            start.linkTo(parent.start, margin = BarEdgePadding)
            if (!isExpanded) {
                // A fixed frame, never the content's size: ConstraintLayout re-measures the content
                // under BOTH constraint sets whenever it recomposes, so a wrapped frame here would
                // jump mid-animation and drag the mini player along with it.
                width = Dimension.value(CollapsedBarSize)
                height = Dimension.value(CollapsedBarSize)
            } else {
                // Capsule + gap + search, centred between the edge paddings. The capsule is sized
                // from the measured budget, so the cluster can no longer outgrow the screen.
                end.linkTo(parent.end, margin = BarEdgePadding)
                width = Dimension.wrapContent
                height = Dimension.value(BarHeight)
            }
        }
        val miniPlayer = createRefFor("miniPlayer")
        constrain(miniPlayer) {
            if (!isExpanded) {
                // The card sits [miniPlayerGap] after the circle: its own side padding is pulled
                // back out of that gap, which a link to toolbar.end could not do.
                start.linkTo(toolbar.start, margin = CollapsedBarSize + miniPlayerGap - miniPlayerPadding)
                end.linkTo(parent.end)
                top.linkTo(toolbar.top)
                bottom.linkTo(toolbar.bottom)
                width = if (isMiniplayerShow) Dimension.fillToConstraints else Dimension.wrapContent
            } else {
                start.linkTo(parent.start)
                end.linkTo(parent.end)
                bottom.linkTo(toolbar.top, margin = miniPlayerGap)
                width = if (isMiniplayerShow) Dimension.matchParent else Dimension.wrapContent
            }
            visibility =
                if (isMiniplayerShow) {
                    Visibility.Visible
                } else {
                    Visibility.Gone
                }
        }
    }
