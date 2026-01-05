package com.maxrave.simpmusic.ui.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.currentBackStackEntryAsState
import com.maxrave.simpmusic.extension.greyScale
import com.maxrave.simpmusic.ui.navigation.destination.home.HomeDestination
import com.maxrave.simpmusic.ui.navigation.destination.library.LibraryDestination
import com.maxrave.simpmusic.ui.navigation.destination.search.SearchDestination
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.SharedViewModel
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import simpmusic.composeapp.generated.resources.*
import kotlin.reflect.KClass

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppBottomNavigationBar(
    startDestination: Any = HomeDestination,
    navController: NavController,
    viewModel: SharedViewModel? = null,
    isTranslucentBackground: Boolean = false,
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
    Box(
        modifier =
            Modifier
                .wrapContentSize()
                .then(
                    if (isTranslucentBackground) {
                        Modifier.background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.5f),
                                    Color.Black.copy(alpha = 0.8f),
                                    Color.Black,
                                ),
                            ),
                        )
                    } else {
                        Modifier
                    },
                ),
    ) {
        NavigationBar(
            windowInsets = WindowInsets(0, 0, 0, 0),
            containerColor =
                if (isTranslucentBackground) {
                    Color.Transparent
                } else {
                    Color.Black
                },
        ) {
            bottomNavScreens.forEach { screen ->
                var lastClickTime by remember { mutableStateOf(0L) }
                val doubleClickThreshold = 300L                
                NavigationBarItem(
                    selected = selectedIndex == screen.ordinal,
                    onClick = {
                        val currentTime = System.currentTimeMillis()
                        val isDoubleClick = (currentTime - lastClickTime) < doubleClickThreshold
                        val isOnSearchScreen = screen == BottomNavScreen.Search && currentBackStackEntry?.destination?.hasRoute(SearchDestination::class) == true
                        if (isDoubleClick && isOnSearchScreen) {
                            viewModel?.triggerSearchFocus()
                        } else if (selectedIndex == screen.ordinal) {
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
                            selectedIndex = screen.ordinal
                            navController.navigate(screen.destination) {
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                        lastClickTime = currentTime
                    },
                    label = {
                        Text(
                            stringResource(screen.title),
                            style =
                                if (selectedIndex == screen.ordinal) {
                                    typo().bodySmall
                                } else {
                                    typo().bodySmall.greyScale()
                                },
                        )
                    },
                    icon = screen.icon,
                    modifier =
                        Modifier.windowInsetsPadding(
                            NavigationBarDefaults.windowInsets,
                        ),
                )
            }
        }
    }
}

@Composable
fun AppNavigationRail(
    startDestination: Any = HomeDestination,
    navController: NavController,
    viewModel: SharedViewModel? = null,
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
    NavigationRail {
        Spacer(Modifier.height(16.dp))
        Box(Modifier.padding(horizontal = 16.dp)) {
            Box(
                Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.DarkGray),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(Res.drawable.mono),
                    contentDescription = null,
                    modifier =
                        Modifier
                            .height(32.dp)
                            .clip(CircleShape),
                )
            }
        }
        Spacer(Modifier.weight(1f))
        bottomNavScreens.forEachIndexed { index, screen ->
            var lastClickTime by remember { mutableStateOf(0L) }
            val doubleClickThreshold = 300L
            NavigationRailItem(
                icon = screen.icon,
                label = {
                    Text(
                        stringResource(screen.title),
                        style =
                            if (selectedIndex == screen.ordinal) {
                                typo().bodySmall
                            } else {
                                typo().bodySmall.greyScale()
                            },
                    )
                },
                selected = selectedIndex == index,
                onClick = {
                    val currentTime = System.currentTimeMillis()
                    val isDoubleClick = (currentTime - lastClickTime) < doubleClickThreshold
                    if (screen == BottomNavScreen.Search && isDoubleClick) {
                        viewModel?.triggerSearchFocus()
                    } else if (selectedIndex == screen.ordinal) {
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
                        selectedIndex = screen.ordinal
                        navController.navigate(screen.destination) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                    lastClickTime = currentTime
                },
            )
        }
        Spacer(Modifier.height(32.dp))
    }
}