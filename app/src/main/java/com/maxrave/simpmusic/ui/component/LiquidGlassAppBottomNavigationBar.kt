package com.maxrave.simpmusic.ui.component

import android.annotation.SuppressLint
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.currentBackStackEntryAsState
import com.kyant.backdrop.Backdrop
import com.maxrave.common.R
import com.maxrave.logger.Logger
import com.maxrave.simpmusic.extension.animateAlignmentAsState
import com.maxrave.simpmusic.extension.drawBackdropCustomShape
import com.maxrave.simpmusic.extension.greyScale
import com.maxrave.simpmusic.ui.navigation.destination.home.HomeDestination
import com.maxrave.simpmusic.ui.navigation.destination.library.LibraryDestination
import com.maxrave.simpmusic.ui.navigation.destination.search.SearchDestination
import com.maxrave.simpmusic.ui.screen.MiniPlayer
import com.maxrave.simpmusic.ui.theme.seed
import com.maxrave.simpmusic.ui.theme.transparent
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.SharedViewModel
import kotlin.reflect.KClass

private const val TAG = "LiquidGlassAppBottomNavigationBar"

@SuppressLint("RestrictedApi")
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LiquidGlassAppBottomNavigationBar(
    startDestination: Any = HomeDestination,
    navController: NavController,
    backdrop: Backdrop,
    shouldShowMiniPlayer: Boolean,
    viewModel: SharedViewModel,
    onOpenNowPlaying: () -> Unit = {},
    reloadDestinationIfNeeded: (KClass<*>) -> Unit = { _ -> },
) {
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val bottomNavScreens =
        listOf(
            BottomNavScreen.Home,
            BottomNavScreen.Search,
            BottomNavScreen.Library,
        )
    var selectedIndex by rememberSaveable {
        mutableIntStateOf(
            when (startDestination) {
                is HomeDestination -> BottomNavScreen.Home.ordinal
                is SearchDestination -> BottomNavScreen.Search.ordinal
                is LibraryDestination -> BottomNavScreen.Library.ordinal
                else -> BottomNavScreen.Home.ordinal // Default to Home if not recognized
            },
        )
    }
    var previousSelectedIndex by rememberSaveable {
        mutableIntStateOf(selectedIndex)
    }
    var isExpanded by rememberSaveable {
        mutableStateOf(true)
    }

    val alignment by animateAlignmentAsState(
        if (isExpanded) Alignment.Center else Alignment.CenterStart,
    )

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

    Box(
        modifier =
            Modifier
                .fillMaxWidth(),
        contentAlignment = alignment,
    ) {
        /**
         * LTR: HOME -> MIX FOR YOU -> LIBRARY | SEARCH
         */

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                Modifier
                    .padding(
                        WindowInsets.navigationBars.asPaddingValues(),
                    ).padding(vertical = 8.dp, horizontal = 16.dp)
                    .imePadding()
                    .wrapContentSize(),
        ) {
            HorizontalFloatingToolbar(
                modifier =
                    Modifier.circleDrawBackdrop(backdrop),
                contentPadding = PaddingValues(0.dp),
                colors =
                    FloatingToolbarDefaults
                        .standardFloatingToolbarColors()
                        .copy(
                            toolbarContainerColor = transparent,
                        ),
                expanded = isExpanded,
                trailingContent = {
                    bottomNavScreens.filter { it != BottomNavScreen.Search }.forEach { screen ->
                        Button(
                            onClick = {
                                if (selectedIndex == screen.ordinal) {
                                    if (currentBackStackEntry?.destination?.hierarchy?.any {
                                            it.hasRoute(screen.destination::class)
                                        } == true
                                    ) {
                                        reloadDestinationIfNeeded(
                                            screen.destination::class,
                                        )
                                    } else {
                                        navController.navigate(screen.destination)
                                    }
                                } else {
                                    previousSelectedIndex = selectedIndex
                                    selectedIndex = screen.ordinal
                                    navController.navigate(screen.destination) {
                                        popUpTo(navController.graph.startDestinationId) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            shape = CircleShape,
                            colors =
                                ButtonDefaults
                                    .buttonColors()
                                    .copy(
                                        disabledContainerColor = transparent,
                                        containerColor = transparent,
                                        contentColor =
                                            if (selectedIndex == screen.ordinal) {
                                                seed
                                            } else {
                                                Color.Gray
                                            },
                                    ),
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Icon(
                                    when (screen) {
                                        BottomNavScreen.Home -> Icons.Filled.Home
                                        BottomNavScreen.Search -> Icons.Filled.Search
                                        BottomNavScreen.Library -> Icons.Filled.LibraryMusic
                                    },
                                    "",
                                )
                                Text(
                                    stringResource(screen.title),
                                    style =
                                        if (selectedIndex == screen.ordinal) {
                                            typo.bodySmall
                                        } else {
                                            typo.bodySmall.greyScale()
                                        },
                                )
                            }
                        }
                    }
                },
            ) {
                if (!isExpanded) {
                    val screen = bottomNavScreens.find { screen -> screen.ordinal == selectedIndex } ?: return@HorizontalFloatingToolbar
                    val previousScreen =
                        bottomNavScreens.find { screen -> screen.ordinal == previousSelectedIndex } ?: return@HorizontalFloatingToolbar
                    IconButton(
                        modifier =
                            Modifier.size(
                                FloatingToolbarDefaults.ContainerSize.value.dp,
                            ),
                        shape = CircleShape,
                        onClick = {
                            if (screen == BottomNavScreen.Search) {
                                val destination =
                                    when (previousScreen) {
                                        BottomNavScreen.Home -> HomeDestination
                                        BottomNavScreen.Library -> LibraryDestination
                                        else -> HomeDestination
                                    }
                                selectedIndex = previousSelectedIndex
                                previousSelectedIndex = BottomNavScreen.Search.ordinal
                                navController.navigate(destination) {
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            } else {
                                if (selectedIndex == screen.ordinal) {
                                    if (currentBackStackEntry?.destination?.hierarchy?.any {
                                            it.hasRoute(screen.destination::class)
                                        } == true
                                    ) {
                                        reloadDestinationIfNeeded(
                                            screen.destination::class,
                                        )
                                    } else {
                                        navController.navigate(screen.destination)
                                    }
                                } else {
                                    previousSelectedIndex = selectedIndex
                                    selectedIndex = screen.ordinal
                                    navController.navigate(screen.destination) {
                                        popUpTo(navController.graph.startDestinationId) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }
                        },
                        colors =
                            IconButtonDefaults.iconButtonColors().copy(
                                contentColor = seed,
                            ),
                    ) {
                        Icon(
                            when (selectedIndex) {
                                BottomNavScreen.Home.ordinal -> Icons.Outlined.Home
                                BottomNavScreen.Search.ordinal ->
                                    when (previousScreen) {
                                        BottomNavScreen.Home -> Icons.Outlined.Home
                                        BottomNavScreen.Library -> Icons.Filled.LibraryMusic
                                        else -> Icons.Filled.Search
                                    }
                                BottomNavScreen.Library.ordinal -> Icons.Filled.LibraryMusic
                                else -> Icons.Outlined.Home
                            },
                            "",
                        )
                    }
                }
            }

            if (isExpanded) {
                Spacer(Modifier.size(12.dp))
            }

            Crossfade(targetState = selectedIndex == BottomNavScreen.Search.ordinal) { isSearchSelected ->
                if (!isSearchSelected) {
                    FloatingActionButton(
                        modifier = Modifier.circleDrawBackdrop(backdrop),
                        onClick = {
                            previousSelectedIndex = selectedIndex
                            selectedIndex = BottomNavScreen.Search.ordinal
                            navController.navigate(BottomNavScreen.Search.destination) {
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        shape = CircleShape,
                        containerColor = transparent,
                    ) {
                        Icon(
                            Icons.Outlined.Search,
                            "",
                        )
                    }
                } else {
                    AnimatedVisibility(
                        visible = shouldShowMiniPlayer && isInSearchDestination,
                        enter = fadeIn() + slideInHorizontally(),
                        exit = fadeOut(),
                    ) {
                        MiniPlayer(
                            Modifier
                                .height(56.dp)
                                .fillMaxWidth()
                                .padding(
                                    horizontal = 12.dp,
                                ).padding(
                                    bottom = 4.dp,
                                ),
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
            }
        }
    }
}

private fun Modifier.circleDrawBackdrop(backdrop: Backdrop): Modifier = drawBackdropCustomShape(backdrop, CircleShape)

sealed class BottomNavScreen(
    val ordinal: Int,
    val destination: Any,
    @param:StringRes val title: Int,
    val icon: @Composable () -> Unit,
) {
    data object Home : BottomNavScreen(
        ordinal = 0,
        destination = HomeDestination,
        title = R.string.home,
        icon = {
            Icon(
                Icons.Rounded.Home,
                contentDescription = null,
            )
        },
    )

    data object Search : BottomNavScreen(
        ordinal = 1,
        destination = SearchDestination,
        title = R.string.search,
        icon = {
            Icon(
                Icons.Rounded.Search,
                contentDescription = null,
            )
        },
    )

    data object Library : BottomNavScreen(
        ordinal = 2,
        destination = LibraryDestination,
        title = R.string.library,
        icon = {
            Icon(
                imageVector = Icons.Filled.LibraryMusic,
                contentDescription = null,
            )
        },
    )
}