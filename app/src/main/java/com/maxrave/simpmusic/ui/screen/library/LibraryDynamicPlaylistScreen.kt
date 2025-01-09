package com.maxrave.simpmusic.ui.screen.library

import android.os.Bundle
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import com.maxrave.simpmusic.R
import com.maxrave.simpmusic.data.db.entities.SongEntity
import com.maxrave.simpmusic.extension.navigateSafe
import com.maxrave.simpmusic.ui.component.ArtistFullWidthItems
import com.maxrave.simpmusic.ui.component.EndOfPage
import com.maxrave.simpmusic.ui.component.NowPlayingBottomSheet
import com.maxrave.simpmusic.ui.component.RippleIconButton
import com.maxrave.simpmusic.ui.component.SongFullWidthItems
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.LibraryDynamicPlaylistViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
@UnstableApi
@ExperimentalMaterial3Api
fun LibraryDynamicPlaylistScreen(
    innerPadding: PaddingValues,
    navController: NavController,
    type: LibraryDynamicPlaylistType,
    viewModel: LibraryDynamicPlaylistViewModel = koinViewModel(),
) {
    val nowPlayingVideoId by viewModel.nowPlayingVideoId.collectAsState()

    var chosenSong: SongEntity? by remember { mutableStateOf(null) }
    var showBottomSheet by rememberSaveable { mutableStateOf(false) }

    val favorite by viewModel.listFavoriteSong.collectAsState()
    val followed by viewModel.listFollowedArtist.collectAsState()
    val mostPlayed by viewModel.listMostPlayedSong.collectAsState()
    val downloaded by viewModel.listDownloadedSong.collectAsState()

    LazyColumn(
        modifier = Modifier.padding(top = 64.dp),
        contentPadding = innerPadding,
    ) {
        if (type == LibraryDynamicPlaylistType.Followed) {
            items(followed, key = { it.channelId }) { artist ->
                ArtistFullWidthItems(
                    artist,
                    onClickListener = {
                        navController.navigateSafe(
                            R.id.action_global_artistFragment,
                            Bundle().apply {
                                putString("channelId", artist.channelId)
                            },
                        )
                    },
                )
            }
        } else {
            items(
                when (type) {
                    LibraryDynamicPlaylistType.Downloaded -> downloaded
                    LibraryDynamicPlaylistType.Favorite -> favorite
                    LibraryDynamicPlaylistType.MostPlayed -> mostPlayed
                    else -> emptyList()
                },
                key = { it.videoId },
            ) { song ->
                SongFullWidthItems(
                    songEntity = song,
                    isPlaying = song.videoId == nowPlayingVideoId,
                    modifier = Modifier.fillMaxWidth(),
                    onMoreClickListener = {
                        chosenSong = song
                        showBottomSheet = true
                    },
                    onClickListener = { videoId ->
                        viewModel.playSong(videoId, type = type)
                    },
                )
            }
        }
        item {
            EndOfPage()
        }
    }
    if (showBottomSheet) {
        NowPlayingBottomSheet(
            onDismiss = {
                showBottomSheet = false
                chosenSong = null
            },
            navController = navController,
            song = chosenSong ?: return,
        )
    }
    TopAppBar(
        title = {
            Text(
                text =
                    stringResource(
                        type.name(),
                    ),
                style = typo.titleMedium,
            )
        },
        navigationIcon = {
            Box(Modifier.padding(horizontal = 5.dp)) {
                RippleIconButton(
                    R.drawable.baseline_arrow_back_ios_new_24,
                    Modifier
                        .size(32.dp),
                    true,
                ) {
                    navController.navigateUp()
                }
            }
        },
    )
}

sealed class LibraryDynamicPlaylistType {
    data object Favorite : LibraryDynamicPlaylistType()

    data object Followed : LibraryDynamicPlaylistType()

    data object MostPlayed : LibraryDynamicPlaylistType()

    data object Downloaded : LibraryDynamicPlaylistType()

    @StringRes fun name(): Int =
        when (this) {
            Favorite -> R.string.favorite
            Followed -> R.string.followed
            MostPlayed -> R.string.most_played
            Downloaded -> R.string.downloaded
        }
}