package com.maxrave.simpmusic.ui.component.selection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.maxrave.simpmusic.ui.component.ActionButton
import com.maxrave.simpmusic.ui.component.EndOfModalBottomSheet
import com.maxrave.simpmusic.ui.component.rememberSurfaceDarkColors
import com.maxrave.simpmusic.ui.icon.Download
import com.maxrave.simpmusic.ui.icon.Favorite
import com.maxrave.simpmusic.ui.icon.PlaylistAdd
import com.maxrave.simpmusic.ui.icon.QueueMusic
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.icon.SkipNext
import com.maxrave.simpmusic.ui.theme.typo
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.add_to_a_playlist
import simpmusic.composeapp.generated.resources.add_to_queue
import simpmusic.composeapp.generated.resources.download
import simpmusic.composeapp.generated.resources.favorite
import simpmusic.composeapp.generated.resources.n_songs_selected
import simpmusic.composeapp.generated.resources.play_next

/**
 * One row in [SelectedSongsBottomSheet] that only some screens have — "remove from playlist"
 * means nothing on an album or in search results, so it is passed in rather than hardcoded here.
 */
data class SongSelectionAction(
    val icon: ImageVector,
    val label: String,
    val tint: Color? = null,
    val onClick: () -> Unit,
)

/**
 * Actions for the current selection, opened from the overflow button in [SongSelectionTopAppBar].
 * A null callback hides its row, so a screen only offers what it can actually do.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectedSongsBottomSheet(
    count: Int,
    onDismiss: () -> Unit,
    onPlayNext: (() -> Unit)? = null,
    onAddToQueue: (() -> Unit)? = null,
    onAddToPlaylist: (() -> Unit)? = null,
    onDownload: (() -> Unit)? = null,
    onAddToFavorite: (() -> Unit)? = null,
    extraActions: List<SongSelectionAction> = emptyList(),
) {
    val coroutineScope = rememberCoroutineScope()
    val colors = rememberSurfaceDarkColors()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val hideThen: (() -> Unit) -> Unit = { action ->
        coroutineScope.launch {
            sheetState.hide()
            onDismiss()
            action()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.Transparent,
        contentColor = Color.Transparent,
        dragHandle = null,
        scrimColor = Color.Black.copy(alpha = .5f),
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
    ) {
        Card(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
            shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
            colors = CardDefaults.cardColors().copy(containerColor = colors.container),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(modifier = Modifier.height(5.dp))
                Card(
                    modifier =
                        Modifier
                            .width(60.dp)
                            .height(4.dp),
                    colors = CardDefaults.cardColors().copy(containerColor = colors.handle),
                    shape = RoundedCornerShape(50),
                ) {}
                Spacer(modifier = Modifier.height(5.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.Top,
                ) {
                    Text(
                        text = stringResource(Res.string.n_songs_selected, count),
                        style = typo().bodySmall,
                        color = colors.subtitle,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    )
                    if (onPlayNext != null) {
                        ActionButton(
                            icon = SimpIcons.SkipNext,
                            text = Res.string.play_next,
                        ) { hideThen(onPlayNext) }
                    }
                    if (onAddToQueue != null) {
                        ActionButton(
                            icon = SimpIcons.QueueMusic,
                            text = Res.string.add_to_queue,
                        ) { hideThen(onAddToQueue) }
                    }
                    if (onAddToPlaylist != null) {
                        ActionButton(
                            icon = SimpIcons.PlaylistAdd,
                            text = Res.string.add_to_a_playlist,
                        ) { hideThen(onAddToPlaylist) }
                    }
                    if (onDownload != null) {
                        ActionButton(
                            icon = SimpIcons.Download,
                            text = Res.string.download,
                        ) { hideThen(onDownload) }
                    }
                    if (onAddToFavorite != null) {
                        ActionButton(
                            icon = SimpIcons.Favorite,
                            text = Res.string.favorite,
                        ) { hideThen(onAddToFavorite) }
                    }
                    extraActions.forEach { action ->
                        ActionButton(
                            icon = action.icon,
                            text = null,
                            textString = action.label,
                            textColor = action.tint,
                            iconColor = action.tint ?: Color.Unspecified,
                        ) { hideThen(action.onClick) }
                    }
                    EndOfModalBottomSheet()
                }
            }
        }
    }
}
