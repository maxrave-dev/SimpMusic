package com.maxrave.simpmusic.ui.component

import android.content.res.Configuration
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.maxrave.simpmusic.R
import com.maxrave.simpmusic.data.db.entities.SongEntity
import com.maxrave.simpmusic.data.model.searchResult.playlists.PlaylistsResult
import com.maxrave.simpmusic.data.type.LibraryType
import com.maxrave.simpmusic.data.type.PlaylistType
import com.maxrave.simpmusic.data.type.RecentlyType
import com.maxrave.simpmusic.ui.theme.typo

@Composable
fun LibraryItem(
    state: LibraryItemState
) {
    val title = when (state.type) {
        is LibraryItemType.YouTubePlaylist -> stringResource(R.string.your_youtube_playlists)
        is LibraryItemType.LocalPlaylist -> stringResource(R.string.your_playlists)
        is LibraryItemType.FavoritePlaylist -> stringResource(R.string.favorite_playlists)
        is LibraryItemType.DownloadedPlaylist -> stringResource(R.string.downloaded_playlists)
        is LibraryItemType.RecentlyAdded -> stringResource(R.string.recently_added)
    }
    val noPlaylistTitle = when (state.type) {
        is LibraryItemType.YouTubePlaylist -> stringResource(R.string.no_YouTube_playlists)
        is LibraryItemType.LocalPlaylist -> stringResource(R.string.no_playlists_added)
        LibraryItemType.DownloadedPlaylist -> stringResource(R.string.no_playlists_downloaded)
        LibraryItemType.FavoritePlaylist -> stringResource(R.string.no_favorite_playlists)
        is LibraryItemType.RecentlyAdded -> stringResource(R.string.recently_added)
    }
    Column {
        Box(
            modifier = Modifier.padding(top = 15.dp, start = 10.dp, end = 10.dp),
        ) {
            Text(
                text = title,
                style = typo.headlineMedium,
                maxLines = 1,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(35.dp)
                    .align(Alignment.CenterStart)
            )
            if (state.type is LibraryItemType.LocalPlaylist || state.type is LibraryItemType.YouTubePlaylist) {
                TextButton(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .defaultMinSize(minWidth = 1.dp, minHeight = 1.dp),
                    onClick = {
                        if (state.type is LibraryItemType.LocalPlaylist) {
                            state.type.onAddClick.invoke()
                        } else {
                            (state.type as LibraryItemType.YouTubePlaylist).onReload.invoke()
                        }
                    }
                ) {
                    if (state.type is LibraryItemType.LocalPlaylist) {
                        Text(stringResource(R.string.add))
                    } else {
                        Text(stringResource(R.string.reload))
                    }
                }
            }
        }
        Crossfade(targetState = state.type is LibraryItemType.YouTubePlaylist && !state.type.isLoggedIn) { notLoggedIn ->
            if (notLoggedIn) {
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.log_in_to_get_YouTube_playlist), style = typo.bodyMedium)
                }
            } else {
                Crossfade(targetState = state.isLoading, label = "Loading") { isLoading ->
                    if (!isLoading) {
                        if (state.type is LibraryItemType.RecentlyAdded) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                state.data.filterIsInstance<RecentlyType>().forEach { item ->
                                    when (item.objectType()) {
                                        RecentlyType.Type.SONG -> {
                                            SongFullWidthItems(
                                                songEntity = item as SongEntity,
                                                isPlaying = item.videoId == state.type.playingVideoId,
                                                modifier = Modifier
                                            )
                                        }
                                        RecentlyType.Type.ALBUM -> {}
                                        RecentlyType.Type.ARTIST -> {}
                                        RecentlyType.Type.PLAYLIST -> {}
                                    }
                                }
                            }
                        } else {
                            if (state.data.isNotEmpty()) {
                                LazyRow {
                                    items(items = state.data) { item ->
                                        HomeItemContentPlaylist(
                                            onClick = {

                                            },
                                            data = item as? PlaylistType ?: return@items,
                                            thumbSize = 100.dp
                                        )
                                    }
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(120.dp), contentAlignment = Alignment.Center
                                ) {
                                    Text(noPlaylistTitle, style = typo.bodyMedium)
                                }
                            }
                        }
                    } else {
                        Box(modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                }
            }
        }
    }
}

sealed class LibraryItemType {
    data class YouTubePlaylist(
        val isLoggedIn: Boolean,
        val onReload: () -> Unit = {}
    ) : LibraryItemType()

    data class LocalPlaylist(
        val onAddClick: () -> Unit
    ) : LibraryItemType()

    data object FavoritePlaylist: LibraryItemType()

    data object DownloadedPlaylist: LibraryItemType()

    data class RecentlyAdded(
        val playingVideoId: String
    ) : LibraryItemType()
}

data class LibraryItemState(
    val type: LibraryItemType,
    val data: List<LibraryType>,
    val isLoading: Boolean = true
)

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
fun LibraryItemPreview() {
    LibraryItem(
        state = LibraryItemState(
            type = LibraryItemType.YouTubePlaylist(isLoggedIn = true),
            data = listOf(
                PlaylistsResult(
                    author = "comprehensam",
                    browseId = "errem",
                    category = "praesent",
                    itemCount = "tempus",
                    resultType = "signiferumque",
                    thumbnails = listOf(),
                    title = "deserunt"
                )
            ),
            isLoading = true
        )
    )
}