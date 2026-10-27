package com.maxrave.simpmusic.ui.component

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.currentBackStackEntryAsState
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import com.kyant.backdrop.highlight.Highlight
import com.maxrave.domain.data.model.library.LibraryCollectionPreview
import com.maxrave.simpmusic.expect.ui.PlatformBackdrop
import com.maxrave.simpmusic.expect.ui.layerBackdrop
import com.maxrave.simpmusic.expect.ui.rememberBackdrop
import com.maxrave.simpmusic.extension.barBlurStyle
import com.maxrave.simpmusic.extension.mouseDragScroll
import com.maxrave.simpmusic.ui.icon.Close
import com.maxrave.simpmusic.ui.icon.FormatListBulleted
import com.maxrave.simpmusic.ui.icon.LeftPanelClose
import com.maxrave.simpmusic.ui.icon.LeftPanelOpen
import com.maxrave.simpmusic.ui.icon.LibraryMusic
import com.maxrave.simpmusic.ui.icon.PushPin
import com.maxrave.simpmusic.ui.icon.Search
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.icon.TrendingUp
import com.maxrave.simpmusic.ui.icon.VolumeUp
import com.maxrave.simpmusic.ui.navigation.destination.library.LibraryDynamicPlaylistDestination
import com.maxrave.simpmusic.ui.screen.home.chipStripEdgeFades
import com.maxrave.simpmusic.ui.screen.library.LibraryDynamicPlaylistType
import com.maxrave.simpmusic.ui.theme.LocalLiquidGlassEnabled
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.LibrarySidebarItem
import com.maxrave.simpmusic.viewModel.LibrarySidebarKind
import com.maxrave.simpmusic.viewModel.LibrarySidebarViewModel
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.blur.hazeBlur
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.album
import simpmusic.composeapp.generated.resources.albums
import simpmusic.composeapp.generated.resources.alphabetical
import simpmusic.composeapp.generated.resources.artist
import simpmusic.composeapp.generated.resources.artists
import simpmusic.composeapp.generated.resources.baseline_downloaded
import simpmusic.composeapp.generated.resources.baseline_favorite_24
import simpmusic.composeapp.generated.resources.collapse_your_library
import simpmusic.composeapp.generated.resources.downloaded
import simpmusic.composeapp.generated.resources.expand_your_library
import simpmusic.composeapp.generated.resources.favorite
import simpmusic.composeapp.generated.resources.liked_songs_count
import simpmusic.composeapp.generated.resources.most_played
import simpmusic.composeapp.generated.resources.playlist
import simpmusic.composeapp.generated.resources.playlists
import simpmusic.composeapp.generated.resources.podcast
import simpmusic.composeapp.generated.resources.podcasts
import simpmusic.composeapp.generated.resources.recents
import simpmusic.composeapp.generated.resources.search_in_your_library
import simpmusic.composeapp.generated.resources.your_library
import kotlin.reflect.KClass

private val SidebarExpandedWidth = 280.dp
private val SidebarCollapsedWidth = 72.dp

/**
 * The Desktop sidebar, Spotify style: a second rounded panel beside the content panel. Navigation
 * sits on top, then "Your library", whose header opens the Library tab (so Library leaves the
 * navigation), with the pinned collections and everything else the user keeps below it. It folds to
 * a 72dp strip of icons and artwork and remembers which way it was left.
 */
@Composable
fun DesktopSidebar(
    navController: NavController,
    containerColor: Color,
    showAnalyticsTab: Boolean,
    showMixForYouTab: Boolean,
    modifier: Modifier = Modifier,
    viewModel: LibrarySidebarViewModel = koinViewModel(),
    reloadDestinationIfNeeded: (KClass<*>) -> Unit = { _ -> },
) {
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    // See the note in AppBottomNavigationBar: `ordinal` is the tab's identity, not its position.
    val navScreens =
        listOfNotNull(
            BottomNavScreen.Home,
            BottomNavScreen.MixForYou.takeIf { showMixForYouTab },
            BottomNavScreen.Analytics.takeIf { showAnalyticsTab },
            BottomNavScreen.Search,
        )
    var selectedIndex by rememberSaveable { mutableIntStateOf(BottomNavScreen.Home.ordinal) }
    // A tab can disappear under the user (tracking turned off, YouTube session ended); fall back to
    // Home so nothing is left highlighted.
    LaunchedEffect(showAnalyticsTab, showMixForYouTab) {
        if ((!showAnalyticsTab && selectedIndex == BottomNavScreen.Analytics.ordinal) ||
            (!showMixForYouTab && selectedIndex == BottomNavScreen.MixForYou.ordinal)
        ) {
            selectedIndex = BottomNavScreen.Home.ordinal
        }
    }
    val selectTab: (BottomNavScreen) -> Unit = { screen ->
        if (selectedIndex == screen.ordinal) {
            if (currentBackStackEntry?.destination?.hierarchy?.any { it.hasRoute(screen.destination::class) } == true) {
                reloadDestinationIfNeeded(screen.destination::class)
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
    }

    // The library sources are one-shot reads, not observed queries, so the list is read again whenever
    // the user moves between screens, which is where a like, follow or new playlist happens.
    LaunchedEffect(currentBackStackEntry?.id) { viewModel.refresh() }
    val collapsed by viewModel.collapsed.collectAsStateWithLifecycle()
    val overview by viewModel.overview.collectAsStateWithLifecycle()
    val libraryItems by viewModel.items.collectAsStateWithLifecycle()
    val playingQueueId by viewModel.playingQueueId.collectAsStateWithLifecycle()

    var filter by remember { mutableStateOf<LibrarySidebarKind?>(null) }
    var alphabetical by remember { mutableStateOf(false) }
    var searching by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    // The search field is hidden while folded, so its text must not keep filtering the strip.
    val activeQuery = if (collapsed) "" else query.trim()

    val kinds = remember(libraryItems) { LibrarySidebarKind.entries.filter { kind -> libraryItems.any { it.kind == kind } } }
    // A filter whose last item went away would leave an empty list behind a chip that no longer shows.
    LaunchedEffect(kinds) { if (filter != null && filter !in kinds) filter = null }
    val shown =
        remember(libraryItems, filter, alphabetical, activeQuery) {
            libraryItems
                .filter { (filter == null || it.kind == filter) && it.title.contains(activeQuery, ignoreCase = true) }
                .let { list -> if (alphabetical) list.sortedBy { it.title.lowercase() } else list.sortedByDescending { it.addedAt } }
        }
    val pinned =
        listOf(
            PinnedCollection(Res.string.favorite, LibraryDynamicPlaylistType.Favorite, overview?.favorite) {
                Image(painterResource(Res.drawable.baseline_favorite_24), contentDescription = null, modifier = Modifier.size(24.dp))
            },
            PinnedCollection(Res.string.downloaded, LibraryDynamicPlaylistType.Downloaded, overview?.downloaded) {
                Image(painterResource(Res.drawable.baseline_downloaded), contentDescription = null, modifier = Modifier.size(24.dp))
            },
            PinnedCollection(Res.string.most_played, LibraryDynamicPlaylistType.MostPlayed, overview?.mostPlayed) {
                Icon(SimpIcons.TrendingUp, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            },
        ).filter { filter == null || filter == LibrarySidebarKind.PLAYLIST }
    val pinnedTitles = pinned.map { stringResource(it.title) }

    val width by animateDpAsState(if (collapsed) SidebarCollapsedWidth else SidebarExpandedWidth, label = "sidebarWidth")
    // What the glass controls refract: the panel's own colour, recorded by a box that is their sibling,
    // never their parent (nesting them inside the source is the RuntimeShader feedback loop).
    val backdrop = rememberBackdrop(containerColor)
    val listState = rememberLazyListState()
    val hazeState = rememberHazeState()
    // The list runs the panel's full height, under the navigation and the library header that float
    // over it, so its top padding is their measured height.
    var headerHeightPx by remember { mutableIntStateOf(0) }
    // Derived, so scrolling recomposes only when the list reaches or leaves its very top.
    val atTop by remember {
        derivedStateOf { listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0 }
    }
    // Home's header: nothing behind it at the top of the list, the list blurred once it scrolls under.
    val blurAlpha by animateFloatAsState(if (atTop) 0f else 1f, tween(300), label = "sidebarHeaderBlur")
    Box(
        modifier
            .width(width)
            .fillMaxHeight()
            .clip(RoundedCornerShape(12.dp))
            .background(containerColor),
    ) {
        // Off, the controls draw flat and nothing reads the recording.
        if (LocalLiquidGlassEnabled.current) Box(Modifier.matchParentSize().layerBackdrop(backdrop))
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().hazeSource(hazeState),
            // The capsule player floats over the panel's bottom (60dp tall, 20dp above the window's
            // edge, i.e. 72dp above this panel's), so the last row can scroll up clear of it.
            contentPadding = PaddingValues(top = with(LocalDensity.current) { headerHeightPx.toDp() }, bottom = 80.dp),
        ) {
            pinned.forEachIndexed { index, collection ->
                if (pinnedTitles[index].contains(activeQuery, ignoreCase = true)) {
                    item(key = "pinned_${collection.type.toStringParams()}") {
                        SidebarLibraryRow(
                            title = pinnedTitles[index],
                            subtitle =
                                "${stringResource(Res.string.playlist)} · " +
                                    pluralStringResource(Res.plurals.liked_songs_count, collection.preview?.count ?: 0, collection.preview?.count ?: 0),
                            pinned = true,
                            playing = false,
                            collapsed = collapsed,
                            onClick = { navController.navigate(LibraryDynamicPlaylistDestination(type = collection.type.toStringParams())) },
                        ) {
                            Box(
                                Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                                contentAlignment = Alignment.Center,
                            ) { collection.icon() }
                        }
                    }
                }
            }
            items(shown, key = { it.key }) { item ->
                SidebarLibraryRow(
                    title = item.title,
                    subtitle = item.subtitleText(),
                    pinned = false,
                    playing = item.queueId != null && item.queueId.removePrefix("VL") == playingQueueId,
                    collapsed = collapsed,
                    onClick = { navController.navigate(item.destination) },
                ) {
                    SidebarCover(item)
                }
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
        Box(
            Modifier
                .fillMaxWidth()
                .onSizeChanged { headerHeightPx = it.height }
                // A wheel turned over the header scrolls the list under it, as anywhere else in the panel.
                // Reversed once, like the list's own: the wheel moves the content, not the viewport.
                .scrollable(listState, Orientation.Vertical, reverseDirection = true),
        ) {
            if (blurAlpha > 0f) {
                Box(
                    Modifier
                        .matchParentSize()
                        .graphicsLayer { alpha = blurAlpha }
                        .hazeBlur(HazeInput.Sources(hazeState), barBlurStyle(containerColor, 0.3f)),
                )
            }
            Column(
                Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 2.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                navScreens.forEach { screen ->
                    SidebarNavRow(
                        backdrop = backdrop,
                        label = stringResource(screen.title),
                        selected = selectedIndex == screen.ordinal,
                        collapsed = collapsed,
                        onClick = { selectTab(screen) },
                        icon = screen.icon,
                    )
                }
                HorizontalDivider(
                    Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                )
                LibraryHeader(
                    backdrop = backdrop,
                    selected = selectedIndex == BottomNavScreen.Library.ordinal,
                    collapsed = collapsed,
                    searching = searching,
                    query = query,
                    onOpenLibrary = { selectTab(BottomNavScreen.Library) },
                    onToggle = {
                        searching = false
                        query = ""
                        viewModel.setCollapsed(!collapsed)
                    },
                    onStartSearch = { searching = true },
                    onQueryChange = { query = it },
                    onCloseSearch = {
                        searching = false
                        query = ""
                    },
                )
                if (!collapsed) {
                    LibraryFilterRow(
                        backdrop = backdrop,
                        kinds = kinds,
                        filter = filter,
                        alphabetical = alphabetical,
                        onFilterChange = { filter = it },
                        onSortChange = { alphabetical = it },
                    )
                }
            }
        }
    }
}

private class PinnedCollection(
    val title: StringResource,
    val type: LibraryDynamicPlaylistType,
    val preview: LibraryCollectionPreview?,
    val icon: @Composable () -> Unit,
)

private fun LibrarySidebarKind.chipLabel(): StringResource =
    when (this) {
        LibrarySidebarKind.PLAYLIST -> Res.string.playlists
        LibrarySidebarKind.ARTIST -> Res.string.artists
        LibrarySidebarKind.ALBUM -> Res.string.albums
        LibrarySidebarKind.PODCAST -> Res.string.podcasts
    }

@Composable
private fun LibrarySidebarItem.subtitleText(): String {
    val kindLabel =
        stringResource(
            when (kind) {
                LibrarySidebarKind.PLAYLIST -> Res.string.playlist
                LibrarySidebarKind.ARTIST -> Res.string.artist
                LibrarySidebarKind.ALBUM -> Res.string.album
                LibrarySidebarKind.PODCAST -> Res.string.podcast
            },
        )
    val detail =
        when {
            trackCount != null -> pluralStringResource(Res.plurals.liked_songs_count, trackCount, trackCount)
            else -> subtitle?.takeIf { it.isNotBlank() }
        }
    return listOfNotNull(kindLabel, detail).joinToString(" · ")
}

@Composable
private fun SidebarNavRow(
    backdrop: PlatformBackdrop,
    label: String,
    selected: Boolean,
    collapsed: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
) {
    val color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        Modifier
            .padding(horizontal = if (collapsed) 12.dp else 8.dp)
            .then(if (collapsed) Modifier.size(48.dp) else Modifier.fillMaxWidth().height(48.dp))
            .then(if (selected) Modifier.liquidGlass(backdrop, RoundedCornerShape(24.dp), highlight = SidebarGlassRim) else Modifier)
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = if (collapsed) 0.dp else 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (collapsed) Arrangement.Center else Arrangement.spacedBy(16.dp),
    ) {
        CompositionLocalProvider(LocalContentColor provides color) { icon() }
        if (!collapsed) {
            Text(label, style = typo().bodyMedium, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun LibraryHeader(
    backdrop: PlatformBackdrop,
    selected: Boolean,
    collapsed: Boolean,
    searching: Boolean,
    query: String,
    onOpenLibrary: () -> Unit,
    onToggle: () -> Unit,
    onStartSearch: () -> Unit,
    onQueryChange: (String) -> Unit,
    onCloseSearch: () -> Unit,
) {
    if (collapsed) {
        Box(Modifier.fillMaxWidth().height(48.dp), contentAlignment = Alignment.Center) {
            SidebarGlassButton(backdrop, SimpIcons.LeftPanelOpen, stringResource(Res.string.expand_your_library), onToggle)
        }
        return
    }
    val color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
    // The field grows out of the search button (its width) over the title, and folds back into it.
    val searchButtonPx = with(LocalDensity.current) { 36.dp.roundToPx() }
    Row(
        Modifier
            .padding(horizontal = 8.dp)
            .fillMaxWidth()
            .height(48.dp)
            .then(
                if (selected && !searching) {
                    Modifier.liquidGlass(backdrop, RoundedCornerShape(24.dp), highlight = SidebarGlassRim)
                } else {
                    Modifier
                },
            ).clip(RoundedCornerShape(24.dp))
            .clickable(enabled = !searching, onClick = onOpenLibrary)
            .padding(start = 4.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
            androidx.compose.animation.AnimatedVisibility(
                visible = !searching,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(start = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        SimpIcons.LibraryMusic,
                        contentDescription = null,
                        tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        stringResource(Res.string.your_library),
                        style = typo().labelSmall,
                        color = color,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    SidebarGlassButton(backdrop, SimpIcons.Search, stringResource(Res.string.search_in_your_library), onStartSearch)
                }
            }
            androidx.compose.animation.AnimatedVisibility(
                visible = searching,
                enter = expandHorizontally(expandFrom = Alignment.End, initialWidth = { searchButtonPx }) + fadeIn(),
                exit = shrinkHorizontally(shrinkTowards = Alignment.End, targetWidth = { searchButtonPx }) + fadeOut(),
            ) {
                LibrarySearchField(backdrop, query, onQueryChange, onCloseSearch, Modifier.fillMaxWidth())
            }
        }
        SidebarGlassButton(backdrop, SimpIcons.LeftPanelClose, stringResource(Res.string.collapse_your_library), onToggle)
    }
}

@Composable
private fun SidebarIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Box(
        Modifier
            .size(32.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
    }
}

// The 1dp uniform rim every small glass control in the app carries; the default rim lights one side
// only and does not show on anything this small.
private val SidebarGlassRim = Highlight(width = 1.dp)

@Composable
private fun SidebarGlassButton(
    backdrop: PlatformBackdrop,
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    LiquidGlassIconButton(
        backdrop = backdrop,
        imageVector = icon,
        modifier = Modifier.size(36.dp).semantics { this.contentDescription = contentDescription },
        tint = MaterialTheme.colorScheme.onSurface,
        highlight = SidebarGlassRim,
        onClick = onClick,
    )
}

@Composable
private fun LibrarySearchField(
    backdrop: PlatformBackdrop,
    query: String,
    onQueryChange: (String) -> Unit,
    onCloseSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    Row(
        modifier
            .height(36.dp)
            .liquidGlass(backdrop, RoundedCornerShape(18.dp), interactive = false, highlight = SidebarGlassRim)
            .padding(start = 12.dp, end = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(SimpIcons.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
        Box(Modifier.weight(1f).padding(horizontal = 8.dp)) {
            if (query.isEmpty()) {
                Text(
                    stringResource(Res.string.search_in_your_library),
                    style = typo().bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = typo().bodySmall.copy(color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
            )
        }
        SidebarIconButton(SimpIcons.Close, stringResource(Res.string.search_in_your_library), onCloseSearch)
    }
}

/** The kind chips, scrolling between faded ends, with the sort button anchored at the row's end. */
@Composable
private fun LibraryFilterRow(
    backdrop: PlatformBackdrop,
    kinds: List<LibrarySidebarKind>,
    filter: LibrarySidebarKind?,
    alphabetical: Boolean,
    onFilterChange: (LibrarySidebarKind?) -> Unit,
    onSortChange: (Boolean) -> Unit,
) {
    val chipScroll = rememberScrollState()
    val headFade by animateFloatAsState(if (chipScroll.canScrollBackward) 1f else 0f, label = "sidebarChipHeadFade")
    val tailFade by animateFloatAsState(if (chipScroll.canScrollForward) 1f else 0f, label = "sidebarChipTailFade")
    var sortMenu by remember { mutableStateOf(false) }
    Row(
        // 14dp at the end lines the sort button up with the collapse button above it.
        Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 8.dp, end = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            Modifier
                .weight(1f)
                .chipStripEdgeFades(24.dp, start = { headFade }, end = { tailFade })
                .mouseDragScroll(chipScroll)
                .horizontalScroll(chipScroll)
                .padding(start = 16.dp, end = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            kinds.forEach { kind ->
                Chip(
                    isSelected = filter == kind,
                    text = stringResource(kind.chipLabel()),
                    backdrop = backdrop,
                    compact = true,
                    onClick = { onFilterChange(if (filter == kind) null else kind) },
                )
            }
        }
        Box {
            SidebarGlassButton(
                backdrop,
                SimpIcons.FormatListBulleted,
                stringResource(if (alphabetical) Res.string.alphabetical else Res.string.recents),
            ) { sortMenu = true }
            DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(Res.string.recents), style = typo().bodyMedium) },
                    onClick = {
                        onSortChange(false)
                        sortMenu = false
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(Res.string.alphabetical), style = typo().bodyMedium) },
                    onClick = {
                        onSortChange(true)
                        sortMenu = false
                    },
                )
            }
        }
    }
}

@Composable
private fun SidebarLibraryRow(
    title: String,
    subtitle: String,
    pinned: Boolean,
    playing: Boolean,
    collapsed: Boolean,
    onClick: () -> Unit,
    cover: @Composable () -> Unit,
) {
    Row(
        Modifier
            .padding(horizontal = if (collapsed) 12.dp else 8.dp)
            .then(if (collapsed) Modifier.size(width = 48.dp, height = 56.dp) else Modifier.fillMaxWidth().height(56.dp))
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = if (collapsed) 0.dp else 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        cover()
        if (!collapsed) {
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    style = typo().bodyMedium,
                    color = if (playing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (pinned) {
                        // Tilted like the pin on the Library's Recently Added rows (FullWidthItems).
                        Icon(SimpIcons.PushPin, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.rotate(30f).size(14.dp))
                    }
                    Text(
                        subtitle,
                        style = typo().bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (playing) {
                Icon(SimpIcons.VolumeUp, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun SidebarCover(item: LibrarySidebarItem) {
    val shape = if (item.kind == LibrarySidebarKind.ARTIST) CircleShape else RoundedCornerShape(8.dp)
    if (item.thumbnail.isNullOrEmpty()) {
        // A local playlist with no artwork: the gradient its title tile has in the Library grid,
        // without the title and badge, which that tile sizes for 150dp and which cannot be read at 48.
        Box(
            Modifier
                .size(48.dp)
                .clip(shape)
                .background(Brush.linearGradient(generateGradientFromTitle(item.title))),
        )
        return
    }
    val fallback = rememberHolderPainter()
    AsyncImage(
        // A row the list rebuilds (filter, sort, scroll back) must not flash the holder and fade the
        // cover in again: one size-free memory key, used as its own placeholder, puts the cover that
        // is already decoded on the very first frame, and there is no fade to replay.
        model =
            ImageRequest
                .Builder(LocalPlatformContext.current)
                .data(item.thumbnail)
                .diskCachePolicy(CachePolicy.ENABLED)
                .diskCacheKey(item.thumbnail)
                .memoryCacheKey(item.thumbnail)
                .placeholderMemoryCacheKey(item.thumbnail)
                .build(),
        placeholder = fallback,
        error = fallback,
        fallback = fallback,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier.size(48.dp).clip(shape),
    )
}
