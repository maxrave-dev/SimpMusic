package com.maxrave.simpmusic.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
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
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import simpmusic.composeapp.generated.resources.*
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

    val selectedIndex = remember(currentBackStackEntry) {
        val currentDestination = currentBackStackEntry?.destination
        when {
            currentDestination?.hierarchy?.any { it.hasRoute(HomeDestination::class) } == true -> BottomNavScreen.Home.ordinal
            currentDestination?.hierarchy?.any { it.hasRoute(SearchDestination::class) } == true -> BottomNavScreen.Search.ordinal
            currentDestination?.hierarchy?.any { it.hasRoute(LibraryDestination::class) } == true -> BottomNavScreen.Library.ordinal
            else -> {
                when (startDestination) {
                    is HomeDestination -> BottomNavScreen.Home.ordinal
                    is SearchDestination -> BottomNavScreen.Search.ordinal
                    is LibraryDestination -> BottomNavScreen.Library.ordinal
                    else -> BottomNavScreen.Home.ordinal
                }
            }
        }
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
                                    MaterialTheme.colorScheme.background.copy(alpha = 0.5f),
                                    MaterialTheme.colorScheme.background.copy(alpha = 0.8f),
                                    MaterialTheme.colorScheme.background,
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
                    MaterialTheme.colorScheme.background
                },
        ) {
            bottomNavScreens.forEach { screen ->
                val isSelected = selectedIndex == screen.ordinal
                NavigationBarItem(
                    selected = isSelected,
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onBackground,
                        selectedTextColor = MaterialTheme.colorScheme.onBackground,
                        unselectedIconColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        unselectedTextColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        indicatorColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    onClick = {
                        if (isSelected) {
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
                                if (isSelected) {
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
    reloadDestinationIfNeeded: (KClass<*>) -> Unit = { _ -> },
) {
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val bottomNavScreens =
        listOf(
            BottomNavScreen.Home,
            BottomNavScreen.Search,
            BottomNavScreen.Library,
        )

    val selectedIndex = remember(currentBackStackEntry) {
        val currentDestination = currentBackStackEntry?.destination
        when {
            currentDestination?.hierarchy?.any { it.hasRoute(HomeDestination::class) } == true -> BottomNavScreen.Home.ordinal
            currentDestination?.hierarchy?.any { it.hasRoute(SearchDestination::class) } == true -> BottomNavScreen.Search.ordinal
            currentDestination?.hierarchy?.any { it.hasRoute(LibraryDestination::class) } == true -> BottomNavScreen.Library.ordinal
            else -> {
                when (startDestination) {
                    is HomeDestination -> BottomNavScreen.Home.ordinal
                    is SearchDestination -> BottomNavScreen.Search.ordinal
                    is LibraryDestination -> BottomNavScreen.Library.ordinal
                    else -> BottomNavScreen.Home.ordinal
                }
            }
        }
    }

    val bgColor = MaterialTheme.colorScheme.background
    val isLightMode = bgColor.luminance() > 0.5f

    Row(modifier = Modifier.fillMaxHeight()) {
        NavigationRail(
            containerColor = MaterialTheme.colorScheme.background
        ) {
            Spacer(Modifier.height(16.dp))
            Box(Modifier.padding(horizontal = 16.dp)) {
                Box(
                    Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
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
                val isSelected = selectedIndex == index
                NavigationRailItem(
                    icon = screen.icon,
                    label = {
                        Text(
                            stringResource(screen.title),
                            style =
                                if (isSelected) {
                                    typo().bodySmall
                                } else {
                                    typo().bodySmall.greyScale()
                                },
                        )
                    },
                    colors = NavigationRailItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onBackground,
                        selectedTextColor = MaterialTheme.colorScheme.onBackground,
                        unselectedIconColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        unselectedTextColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        indicatorColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    selected = isSelected,
                    onClick = {
                        if (isSelected) {
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
                            navController.navigate(screen.destination) {
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                )
            }
            Spacer(Modifier.height(32.dp))
        }
        
        // La línea divisoria a la derecha del menú
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(1.dp)
                .background(if (isLightMode) Color.Black else Color.DarkGray)
        )
    }
}