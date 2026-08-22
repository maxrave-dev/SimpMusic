package com.maxrave.simpmusic.ui.component

import android.graphics.Bitmap
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Icon
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.icon.Mic
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.ConstraintSet
import androidx.constraintlayout.compose.Dimension
import androidx.constraintlayout.compose.Visibility
import androidx.core.graphics.scale
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.currentBackStackEntryAsState
import com.maxrave.domain.data.player.GenericMediaItem
import com.maxrave.logger.Logger
import com.maxrave.simpmusic.expect.ui.PlatformBackdrop
<<<<<<< HEAD
=======
import com.maxrave.simpmusic.ui.ext.hapticClickable
>>>>>>> 9c993a74 (feat: Implement voice search & haptics)
import com.maxrave.simpmusic.ui.navigation.destination.home.AnalyticsDestination
import com.maxrave.simpmusic.ui.navigation.destination.home.HomeDestination
import com.maxrave.simpmusic.ui.navigation.destination.library.LibraryDestination
import com.maxrave.simpmusic.ui.navigation.destination.library.MixForYouDestination
import com.maxrave.simpmusic.ui.navigation.destination.search.SearchDestination
import com.maxrave.simpmusic.ui.screen.MiniPlayer
import com.maxrave.simpmusic.ui.theme.LocalIsDarkTheme
import com.maxrave.simpmusic.viewModel.SharedViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.nio.IntBuffer
import kotlin.reflect.KClass
import kotlin.time.Duration.Companion.seconds

private const val TAG = "LiquidGlassAppBottomNavigationBar"

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
    val luminanceAnimation = remember { Animatable(0f) }

    LaunchedEffect(layer) {
        val buffer = IntBuffer.allocate(25)
        while (isActive) {
            try {
                withContext(Dispatchers.IO) {
                    val imageBitmap = layer.toImageBitmap()
                    val thumbnail =
                        imageBitmap
                            .asAndroidBitmap()
                            .scale(5, 5, false)
                            .copy(Bitmap.Config.ARGB_8888, false)
                    buffer.rewind()
                    thumbnail.copyPixelsToBuffer(buffer)
                }
            } catch (e: Exception) {
                Logger.e(TAG, "Error getting pixels from layer: ${e.localizedMessage}")
            }
            val averageLuminance =
                (0 until 25).sumOf { index ->
                    val color = buffer.get(index)
                    val r = (color shr 16 and 0xFF) / 255f
                    val g = (color shr 8 and 0xFF) / 255f
                    val b = (color and 0xFF) / 255f
                    0.2126 * r + 0.7152 * g + 0.0722 * b
                } / 25
            luminanceAnimation.animateTo(
                averageLuminance.coerceIn(0.3, 0.8).toFloat(),
                tween(500),
            )
            delay(1.seconds)
        }
    }

    val nowPlayingData by viewModel.nowPlayingState.collectAsStateWithLifecycle()
    // MiniPlayer visibility logic
    var isShowMiniPlayer by rememberSaveable {
        mutableStateOf(true)
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

    LaunchedEffect(nowPlayingData) {
        isShowMiniPlayer = !(nowPlayingData?.mediaItem == null || nowPlayingData?.mediaItem == GenericMediaItem.EMPTY)
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

    var updateConstraints by remember {
        mutableStateOf(true)
    }

    var constraintSet by remember {
        mutableStateOf(
            decoupledConstraints(isShowMiniPlayer, isExpanded),
        )
    }

    LaunchedEffect(isShowMiniPlayer, isExpanded) {
        constraintSet = decoupledConstraints(isShowMiniPlayer, isExpanded)
        updateConstraints = false
    }

    LaunchedEffect(updateConstraints) {
        if (updateConstraints) {
            constraintSet = decoupledConstraints(isShowMiniPlayer, isExpanded)
            updateConstraints = false
        }
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
        animateChangesSpec = tween(300),
    ) {
        /**
         * LTR: HOME -> MIX FOR YOU -> LIBRARY | SEARCH
         */
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                Modifier
                    .then(
                        // Expanded: the row spans the screen so the capsule can be told how much
                        // room is left once the FAB has taken its 56dp. Collapsed it is just a
                        // single pill sitting next to the mini player, so it stays wrap-content.
                        if (isExpanded) {
                            Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                        } else {
                            Modifier.padding(start = 16.dp).wrapContentSize()
                        },
                    ).layoutId("toolbar")
                    .onGloballyPositioned { updateConstraints = true },
        ) {
            if (isExpanded) {
                // The FAB keeps its own slot beside the capsule — overlapping it reads fine on a
                // bar whose last item is decorative, but here the last item is the Library tab and
                // the FAB covered it. weight(1f) hands the capsule exactly what is left after the
                // gap and the FAB, and BoxWithConstraints reports that as its budget.
                BoxWithConstraints(Modifier.weight(1f)) {
                    LiquidGlassTabBar(
                        tabs = barTabs,
                        selectedTab = barTabs.indexOfFirst { it.ordinal == selectedIndex },
                        backdrop = backdrop,
                        layer = layer,
                        luminance = luminanceAnimation.value,
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
                                layer,
                                luminanceAnimation.value,
                                CircleShape,
                                searchFabInteraction,
                            ).clickable { selectTab(BottomNavScreen.Search.ordinal) },
                    contentAlignment = Alignment.Center,
                ) {

                   

                    if (isInSearchDestination) {
                        androidx.compose.material3.Icon(com.maxrave.simpmusic.ui.icon.SimpIcons.Mic, contentDescription = "Voice Search")
                    } else {
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
                                luminanceAnimation.value,
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
            height = Dimension.wrapContent
            if (!isExpanded) {
                width = Dimension.wrapContent
                start.linkTo(parent.start)
            } else {
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
