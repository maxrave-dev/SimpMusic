package com.maxrave.simpmusic.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.kyant.backdrop.highlight.Highlight
import com.maxrave.domain.repository.ListenTogetherRepository
import com.maxrave.simpmusic.expect.ui.PlatformBackdrop
import com.maxrave.simpmusic.ui.icon.Add
import com.maxrave.simpmusic.ui.icon.Groups
import com.maxrave.simpmusic.ui.icon.PlaylistAdd
import com.maxrave.simpmusic.ui.icon.Settings
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.navigation.destination.home.ListenTogetherDestination
import com.maxrave.simpmusic.ui.theme.LocalLiquidGlassEnabled
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.SharedViewModel
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.create
import simpmusic.composeapp.generated.resources.create_playlist_description
import simpmusic.composeapp.generated.resources.create_room_description
import simpmusic.composeapp.generated.resources.create_room_live
import simpmusic.composeapp.generated.resources.listen_together
import simpmusic.composeapp.generated.resources.local_playlist
import simpmusic.composeapp.generated.resources.playlist_name
import simpmusic.composeapp.generated.resources.playlist_name_cannot_be_empty
import simpmusic.composeapp.generated.resources.settings

/**
 * The top-bar "+" (Spotify's Create): a local playlist or a Listen Together room.
 *
 * With liquid glass on it is a glass pill, shared with Settings when [onSettings] is given, like the
 * like/search/more pill on the playlist screens; [backdrop] must be a sibling of this, never an
 * ancestor. With glass off the "+" gets a grey fill and a border so it reads as the special one, and
 * Settings stays a plain icon.
 */
@Composable
fun CreateActions(
    backdrop: PlatformBackdrop,
    navController: NavController,
    onSettings: (() -> Unit)? = null,
    sharedViewModel: SharedViewModel = koinInject(),
) {
    val room by koinInject<ListenTogetherRepository>().room.collectAsStateWithLifecycle()
    var showCreate by remember { mutableStateOf(false) }
    var showPlaylistName by remember { mutableStateOf(false) }
    val tint = MaterialTheme.colorScheme.onBackground

    val createButton: @Composable () -> Unit = {
        Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
            if (LocalLiquidGlassEnabled.current) {
                IconButton(onClick = { showCreate = true }) {
                    Icon(SimpIcons.Add, contentDescription = stringResource(Res.string.create), tint = tint)
                }
            } else {
                Box(
                    modifier =
                        Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                            .clickable { showCreate = true },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(SimpIcons.Add, contentDescription = stringResource(Res.string.create), tint = tint)
                }
            }
            // Listen Together moved into the sheet, so its live-room dot moved onto this button.
            if (room.inRoom) LiveRoomDot()
        }
    }
    val settingsButton: @Composable () -> Unit = {
        if (onSettings != null) {
            IconButton(onClick = onSettings, modifier = Modifier.size(44.dp)) {
                Icon(SimpIcons.Settings, contentDescription = stringResource(Res.string.settings), tint = tint)
            }
        }
    }

    if (LocalLiquidGlassEnabled.current) {
        Row(
            modifier =
                Modifier
                    // 44dp, the Home avatar's size: Apple sizes a bar's avatar and glass buttons alike.
                    .height(44.dp)
                    .liquidGlass(
                        backdrop,
                        RoundedCornerShape(24.dp),
                        // Alone, the "+" is a small circle, which the directional default rim misses.
                        highlight = if (onSettings == null) Highlight(width = 1.dp) else Highlight.Default,
                    ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            createButton()
            settingsButton()
        }
    } else {
        Row(verticalAlignment = Alignment.CenterVertically) {
            createButton()
            settingsButton()
        }
    }

    if (showCreate) {
        CreateSheet(
            liveRoom = room.inRoom,
            onDismiss = { showCreate = false },
            onPlaylist = {
                showCreate = false
                showPlaylistName = true
            },
            onListenTogether = {
                showCreate = false
                navController.navigate(ListenTogetherDestination)
            },
        )
    }
    if (showPlaylistName) {
        CreateLocalPlaylistSheet(
            onDismiss = { showPlaylistName = false },
            onCreate = sharedViewModel::createLocalPlaylist,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateSheet(
    liveRoom: Boolean,
    onDismiss: () -> Unit,
    onPlaylist: () -> Unit,
    onListenTogether: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
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
            colors = CardDefaults.cardColors().copy(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        ) {
            Column(Modifier.padding(bottom = 8.dp)) {
                SheetHandle()
                Text(
                    text = stringResource(Res.string.create),
                    style = typo().titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
                CreateRow(
                    icon = SimpIcons.PlaylistAdd,
                    title = stringResource(Res.string.local_playlist),
                    subtitle = stringResource(Res.string.create_playlist_description),
                    onClick = onPlaylist,
                )
                CreateRow(
                    icon = SimpIcons.Groups,
                    title = stringResource(Res.string.listen_together),
                    subtitle = stringResource(if (liveRoom) Res.string.create_room_live else Res.string.create_room_description),
                    onClick = onListenTogether,
                )
                EndOfModalBottomSheet()
            }
        }
    }
}

@Composable
private fun CreateRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
        }
        Column {
            Text(text = title, style = typo().titleSmall)
            Text(text = subtitle, style = typo().bodySmall)
        }
    }
}

@Composable
private fun SheetHandle() {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(5.dp))
            Card(
                modifier =
                    Modifier
                        .width(60.dp)
                        .height(4.dp),
                colors =
                    CardDefaults.cardColors().copy(
                        containerColor = MaterialTheme.colorScheme.outline,
                    ),
                shape = RoundedCornerShape(50),
            ) {}
            Spacer(modifier = Modifier.height(5.dp))
        }
    }
}

/** Name a new local playlist. Shared by Library's create tile and the Create sheet. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateLocalPlaylistSheet(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit,
    sharedViewModel: SharedViewModel = koinInject(),
) {
    val coroutineScope = rememberCoroutineScope()
    var newTitle by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
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
            colors = CardDefaults.cardColors().copy(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                // The sheet is handed no insets, so nothing else lifts the field above the keyboard.
                // Navigation bars are left out: EndOfModalBottomSheet already pads for them.
                modifier = Modifier.windowInsetsPadding(WindowInsets.ime.exclude(WindowInsets.navigationBars)),
            ) {
                SheetHandle()
                OutlinedTextField(
                    value = newTitle,
                    onValueChange = { s -> newTitle = s },
                    label = {
                        Text(text = stringResource(Res.string.playlist_name))
                    },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                )
                Spacer(modifier = Modifier.height(5.dp))
                TextButton(
                    onClick = {
                        if (newTitle.isBlank()) {
                            coroutineScope.launch { sharedViewModel.makeToast(getString(Res.string.playlist_name_cannot_be_empty)) }
                        } else {
                            onCreate(newTitle)
                            coroutineScope.launch {
                                sheetState.hide()
                                onDismiss()
                            }
                        }
                    },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .align(Alignment.CenterHorizontally),
                ) {
                    Text(text = stringResource(Res.string.create))
                }
                EndOfModalBottomSheet()
            }
        }
    }
}
