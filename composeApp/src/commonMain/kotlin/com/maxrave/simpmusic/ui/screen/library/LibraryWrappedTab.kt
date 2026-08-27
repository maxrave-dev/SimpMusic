package com.maxrave.simpmusic.ui.screen.library

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.maxrave.domain.data.type.MonthlyRecapItem
import com.maxrave.domain.utils.LocalResource
import com.maxrave.simpmusic.ui.component.GridLibraryPlaylist
import com.maxrave.simpmusic.ui.component.WrappedEntryCard
import com.maxrave.simpmusic.ui.navigation.destination.home.WrappedDestination
import com.maxrave.simpmusic.viewModel.WrappedUiState
import com.maxrave.simpmusic.viewModel.WrappedViewModel
import org.koin.compose.viewmodel.koinViewModel
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.wrapped_recap_empty

/** The page margin the card sits in, matching the gutter the playlist tiles below it carry. */
private val CARD_GUTTER = 10.dp

/**
 * The Wrapped filter's page: the way in to the reel, and a recap playlist per month beneath it.
 *
 * The recaps are drawn by [GridLibraryPlaylist], the same component every other playlist list in
 * Library uses — a recap is a playlist, so it gets a playlist tile, and pull-to-refresh, the empty
 * state and the top-bar hiding all come from there rather than from a second implementation here.
 *
 * The entry card rides in that component's `header` slot, as a real grid item spanning every
 * column. It was previously drawn in a `Box` over the grid with its height reserved in
 * `contentPadding` and its position translated by the scroll offset — the measured height included
 * the top inset that was then added to it again, which is what left a screen-tall hole above the
 * tiles. A grid item cannot be double-counted.
 *
 * A month with no plays is absent rather than shown empty, which is decided upstream in
 * `LibraryViewModel.getMonthlyRecaps` — "Recap March" opening onto an empty list is worse than no
 * tile.
 */
@Composable
fun LibraryWrappedTab(
    navController: NavController,
    contentPadding: PaddingValues,
    recaps: LocalResource<List<MonthlyRecapItem>>,
    onScrolling: (onTop: Boolean) -> Unit = {},
    wrappedViewModel: WrappedViewModel = koinViewModel(),
    onReload: () -> Unit,
) {
    val wrappedState by wrappedViewModel.uiState.collectAsStateWithLifecycle()
    val ready = wrappedState as? WrappedUiState.Ready

    GridLibraryPlaylist(
        navController = navController,
        contentPadding = contentPadding,
        data = recaps,
        emptyText = Res.string.wrapped_recap_empty,
        onScrolling = onScrolling,
        // Only the reel knows whether the year holds enough to open, and the card is built from
        // that answer — so there is nothing to draw until it arrives. Same gate the Analytics
        // screen uses for it.
        header =
            ready?.let {
                {
                    WrappedEntryCard(
                        wrapped = it.wrapped,
                        onClick = { navController.navigate(WrappedDestination) },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = CARD_GUTTER, vertical = 8.dp),
                    )
                }
            },
        onReload = onReload,
    )
}
