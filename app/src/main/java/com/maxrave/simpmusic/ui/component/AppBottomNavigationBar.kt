package com.maxrave.simpmusic.ui.component

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.currentBackStackEntryAsState
import com.maxrave.simpmusic.R
import com.maxrave.simpmusic.extension.greyScale
import com.maxrave.simpmusic.ui.navigation.destination.home.HomeDestination
import com.maxrave.simpmusic.ui.navigation.destination.library.LibraryDestination
import com.maxrave.simpmusic.ui.navigation.destination.search.SearchDestination
import com.maxrave.simpmusic.ui.theme.typo
import kotlin.reflect.KClass

@Composable
fun AppBottomNavigationBar(
    startDestination: Any = HomeDestination,
    navController: NavController,
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
                NavigationBarItem(
                    selected = selectedIndex == screen.ordinal,
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
                    label = {
                        Text(
                            stringResource(screen.title),
                            style =
                                if (selectedIndex == screen.ordinal) {
                                    typo.bodySmall
                                } else {
                                    typo.bodySmall.greyScale()
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