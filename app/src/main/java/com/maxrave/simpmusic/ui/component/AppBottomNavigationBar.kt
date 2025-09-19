package com.maxrave.simpmusic.ui.component

import android.annotation.SuppressLint
import androidx.annotation.StringRes
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.currentBackStackEntryAsState
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.dispersion
import com.kyant.backdrop.effects.refraction
import com.kyant.backdrop.effects.saturation
import com.maxrave.common.R
import com.maxrave.simpmusic.extension.animateAlignmentAsState
import com.maxrave.simpmusic.ui.navigation.destination.home.HomeDestination
import com.maxrave.simpmusic.ui.navigation.destination.library.LibraryDestination
import com.maxrave.simpmusic.ui.navigation.destination.search.SearchDestination
import com.maxrave.simpmusic.ui.theme.seed
import com.maxrave.simpmusic.ui.theme.transparent
import kotlin.reflect.KClass

@SuppressLint("RestrictedApi")
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppBottomNavigationBar(
    startDestination: Any = HomeDestination,
    navController: NavController,
    isTranslucentBackground: Boolean = false,
    backdrop: Backdrop,
    reloadDestinationIfNeeded: (KClass<*>) -> Unit = { _ -> },
) {
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val bottomNavScreens =
        listOf(
            BottomNavScreen.Home,
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
    var isExpanded by rememberSaveable {
        mutableStateOf(false)
    }

    val alignment by animateAlignmentAsState(
        if (isExpanded) Alignment.Center else Alignment.CenterStart,
    )

    Box(
        modifier =
            Modifier
                .fillMaxWidth(),
//                .then(
//                    if (isTranslucentBackground) {
//                        Modifier.background(
//                            Brush.verticalGradient(
//                                listOf(
//                                    Color.Transparent,
//                                    Color.Black.copy(alpha = 0.5f),
//                                    Color.Black.copy(alpha = 0.8f),
//                                    Color.Black,
//                                ),
//                            ),
//                        )
//                    } else {
//                        Modifier
//                    },
//                ),
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
                    ).padding(vertical = 16.dp, horizontal = 16.dp)
                    .wrapContentSize(),
        ) {
            HorizontalFloatingToolbar(
                modifier =
                    Modifier.drawBackdrop(
                        backdrop = backdrop,
                        shapeProvider = { CircleShape },
                        onDrawSurface = { drawRect(Color.White.copy(alpha = 0.05f)) },
                    ) {
                        // saturation boost
                        saturation()
                        // blur
                        // lens
                        refraction(
                            height = 24f.dp.toPx(),
                            amount = 64f.dp.toPx(),
                            hasDepthEffect = true,
                        )
                        dispersion(
                            height = 24.dp.toPx(),
                            amount = 64.dp.toPx(),
                        )
                    },
                colors =
                    FloatingToolbarDefaults
                        .standardFloatingToolbarColors()
                        .copy(
                            toolbarContainerColor = transparent,
                        ),
                expanded = isExpanded,
                trailingContent = {
                    bottomNavScreens.forEach { screen ->
                        IconButton(
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
                            colors =
                                IconButtonDefaults.iconButtonColors().let {
                                    if (selectedIndex == screen.ordinal) {
                                        it.copy(
                                            contentColor = seed,
                                        )
                                    } else {
                                        it
                                    }
                                },
                        ) {
                            Icon(
                                when (screen) {
                                    BottomNavScreen.Home -> Icons.Outlined.Home
                                    BottomNavScreen.Search -> Icons.Outlined.Search
                                    BottomNavScreen.Library -> Icons.Filled.LibraryMusic
                                },
                                "",
                            )
                        }
                    }
                },
            ) {
                if (!isExpanded) {
                    val screen = bottomNavScreens.find { screen -> screen.ordinal == selectedIndex } ?: return@HorizontalFloatingToolbar
                    IconButton(
                        shape = CircleShape,
                        onClick = {
                            isExpanded = !isExpanded
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
                        colors =
                            IconButtonDefaults.iconButtonColors().copy(
                                contentColor = seed,
                            ),
                    ) {
                        Icon(
                            when (selectedIndex) {
                                BottomNavScreen.Home.ordinal -> Icons.Outlined.Home
                                BottomNavScreen.Search.ordinal -> Icons.Outlined.Search
                                BottomNavScreen.Library.ordinal -> Icons.Filled.LibraryMusic
                                else -> Icons.Outlined.Home
                            },
                            "",
                        )
                    }
                }
            }
            Spacer(Modifier.size(12.dp))
            Crossfade(targetState = selectedIndex == BottomNavScreen.Search.ordinal) { isSearchSelected ->
                if (isSearchSelected) {
                } else {
                    FloatingActionButton(
                        onClick = {
                            isExpanded = !isExpanded
                        },
                        shape = CircleShape,
                    ) {
                        Icon(
                            Icons.Outlined.Search,
                            "",
                        )
                    }
                }
            }
        }
        /*
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

         */
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