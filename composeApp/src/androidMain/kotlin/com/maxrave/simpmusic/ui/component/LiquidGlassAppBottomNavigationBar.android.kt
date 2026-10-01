package com.maxrave.simpmusic.ui.component

import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.unit.dp
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
import kotlinx.coroutines.delay
import kotlin.reflect.KClass

private const val TAG = "LiquidGlassAppBottomNavigationBar"

// How long the bar takes to morph between the tab bar and the collapsed pill.
private const val BAR_MORPH_MS = 300

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
    reloadDestinationIfNeeded: (KClass<*>) -> Unit,
) {
    val layer = rememberGraphicsLayer()
    val toolbarInteraction = rememberGlassInteraction()
    val searchFabInteraction = rememberGlassInteraction()
    val luminance = rememberGlassLuminance(layer)

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
    // Collapsing swaps the tabs for the pill at once and lets the layout shrink around it. Expanding
    // has to be that motion played backwards: the pill rides the layout out to its expanded place
    // and only then do the tabs replace it. Swapping first lays the full bar out inside the
    // collapsed constraints, so it overflows the screen and the mini player snaps to the far edge
    // before anything animates.
    var tabsSettled by remember { mutableStateOf(isExpanded) }
    LaunchedEffect(isExpanded) {
        if (isExpanded) delay(BAR_MORPH_MS.toLong())
        tabsSettled = isExpanded
    }
    val showTabs = isExpanded && tabsSettled

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

    val constraintSet =
        remember(isShowMiniPlayer, isExpanded) {
            decoupledConstraints(isShowMiniPlayer, isExpanded)
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

    ConstraintLayout(
        constraintSet = constraintSet,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    WindowInsets.navigationBars.asPaddingValues(),
                ).padding(
                    bottom = 8.dp,
                ).imePadding(),
        animateChangesSpec = tween(BAR_MORPH_MS),
    ) {
        /**
         * LTR: HOME -> MIX FOR YOU -> LIBRARY | SEARCH
         */
        Row(
            verticalAlignment = Alignment.CenterVertically,
            // Center the WHOLE cluster — capsule, gap, FAB — as one unit. With four tabs the
            // capsule fills every dp it is offered and this is a no-op; with two it is the
            // difference between one centred cluster and a capsule floating mid-screen while the
            // search FAB clings to the right edge on its own.
            horizontalArrangement = Arrangement.Center,
            modifier =
                Modifier
                    .then(
                        // Expanded: the row spans the screen so the capsule can be told how much
                        // room is left once the FAB has taken its 56dp. Collapsed it is just a
                        // single pill sitting next to the mini player, so it stays wrap-content.
                        if (showTabs) {
                            Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                        } else {
                            Modifier.padding(start = 16.dp).wrapContentSize()
                        },
                    ).layoutId("toolbar"),
        ) {
            if (showTabs) {
                // The FAB keeps its own slot beside the capsule — overlapping it reads fine on a
                // bar whose last item is decorative, but here the last item is the Library tab and
                // the FAB covered it. weight(1f) hands the capsule exactly what is left after the
                // gap and the FAB, and BoxWithConstraints reports that as its budget.
                //
                // fill = false is what keeps the FAB NEXT TO the capsule instead of pinned to the
                // right edge: tab width is capped at TabWidth, so with two tabs (Mix and Analytics
                // both gated off) the capsule measures far narrower than its budget — a filled
                // slot would still swallow the leftover and hold the FAB at the corner, while a
                // wrapped one lets the Row's Arrangement.Center treat capsule + gap + FAB as one
                // cluster.
                BoxWithConstraints(Modifier.weight(1f, fill = false)) {
                    LiquidGlassTabBar(
                        tabs = barTabs,
                        selectedTab = barTabs.indexOfFirst { it.ordinal == selectedIndex },
                        backdrop = backdrop,
                        layer = layer,
                        luminance = luminance,
                        availableWidth = maxWidth,
                        onTabSelected = { position -> selectTab(barTabs[position].ordinal) },
                    )
                }
                Spacer(Modifier.size(12.dp))
                // Search lives in its own circular glass FAB (Apple Music style).
                Box(
                    modifier =
                        Modifier
                            .size(56.dp)
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
                val selectedScreen =
                    bottomNavScreens.find { it.ordinal == selectedIndex } ?: BottomNavScreen.Home
                Box(
                    modifier =
                        Modifier
                            .size(48.dp)
                            .drawInteractiveGlass(
                                LocalIsDarkTheme.current,
                                backdrop,
                                layer,
                                { luminance.value },
                                CircleShape,
                                toolbarInteraction,
                            ).clickable { isExpanded = true },
                    contentAlignment = Alignment.Center,
                ) {
                    selectedScreen.icon()
                }
            }
        }
        MiniPlayer(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
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

private fun decoupledConstraints(
    isMiniplayerShow: Boolean = true,
    isExpanded: Boolean,
): ConstraintSet =
    ConstraintSet {
        val toolbar = createRefFor("toolbar")
        constrain(toolbar) {
            bottom.linkTo(parent.bottom)
            if (!isExpanded) {
                height = Dimension.wrapContent
                width = Dimension.wrapContent
                start.linkTo(parent.start)
            } else {
                // Pinned to the tab bar's height instead of wrapped: the pill is 16dp shorter, and
                // while it is carried through a morph the mini player would otherwise drop by that
                // much the moment the content changes, then hop back when the tabs arrive.
                height = Dimension.value(BarHeight)
                // fillToConstraints, not wrapContent: wrap let the row size itself to its content
                // and simply overflow the screen when a tab was added, taking the FAB with it.
                width = Dimension.fillToConstraints
                start.linkTo(parent.start)
                end.linkTo(parent.end)
            }
        }
        val miniPlayer = createRefFor("miniPlayer")
        constrain(miniPlayer) {
            if (!isExpanded) {
                start.linkTo(toolbar.end)
                end.linkTo(parent.end)
                top.linkTo(toolbar.top)
                bottom.linkTo(toolbar.bottom)
                width = if (isMiniplayerShow) Dimension.fillToConstraints else Dimension.wrapContent
            } else {
                start.linkTo(parent.start)
                end.linkTo(parent.end)
                bottom.linkTo(toolbar.top, margin = 12.dp)
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
