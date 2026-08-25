package com.maxrave.simpmusic.ui.component.selection

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.maxrave.simpmusic.ui.icon.Close
import com.maxrave.simpmusic.ui.icon.MoreVert
import com.maxrave.simpmusic.ui.icon.SelectAll
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.theme.typo
import org.jetbrains.compose.resources.stringResource
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.exit_selection
import simpmusic.composeapp.generated.resources.n_selected
import simpmusic.composeapp.generated.resources.select_all

/**
 * Replaces a screen's own top bar while [state] is active.
 *
 * [onSelectAll] hands over the ids the screen is currently showing, in display order —
 * [SongSelectionState.selectAll] keeps the first [MAX_SONG_SELECTION] of them.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongSelectionTopAppBar(
    state: SongSelectionState,
    onSelectAll: () -> Unit,
    onOpenActions: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = Color.Transparent,
    contentColor: Color = Color.White,
    // Defaults to the status-bar inset, like any TopAppBar. Pass WindowInsets(0) when this bar is
    // STACKED above something that already consumes that inset — a Material SearchBar does, and two
    // of them in one Column reserve the status bar twice, which reads as a slab of dead padding.
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets,
) {
    TopAppBar(
        modifier = modifier,
        windowInsets = windowInsets,
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = containerColor,
                titleContentColor = contentColor,
                navigationIconContentColor = contentColor,
                actionIconContentColor = contentColor,
            ),
        navigationIcon = {
            IconButton(onClick = { state.exit() }) {
                Icon(
                    imageVector = SimpIcons.Close,
                    contentDescription = stringResource(Res.string.exit_selection),
                )
            }
        },
        title = {
            Text(
                text = stringResource(Res.string.n_selected, state.count),
                style = typo().titleMedium,
                color = contentColor,
            )
        },
        actions = {
            IconButton(onClick = onSelectAll) {
                Icon(
                    imageVector = SimpIcons.SelectAll,
                    contentDescription = stringResource(Res.string.select_all),
                )
            }
            // Select-all doubles as deselect-all, so the count can be 0 while still selecting.
            // There is nothing to act on then, and an empty sheet is worse than no sheet.
            if (state.count > 0) {
                IconButton(onClick = onOpenActions) {
                    Icon(
                        imageVector = SimpIcons.MoreVert,
                        contentDescription = null,
                    )
                }
            }
        },
    )
}
