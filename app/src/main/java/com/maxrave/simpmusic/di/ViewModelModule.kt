package com.maxrave.simpmusic.di

import androidx.media3.common.util.UnstableApi
import com.maxrave.simpmusic.viewModel.AlbumViewModel
import com.maxrave.simpmusic.viewModel.ArtistViewModel
import com.maxrave.simpmusic.viewModel.HomeViewModel
import com.maxrave.simpmusic.viewModel.LibraryDynamicPlaylistViewModel
import com.maxrave.simpmusic.viewModel.LibraryViewModel
import com.maxrave.simpmusic.viewModel.LocalPlaylistViewModel
import com.maxrave.simpmusic.viewModel.LogInViewModel
import com.maxrave.simpmusic.viewModel.MoodViewModel
import com.maxrave.simpmusic.viewModel.MoreAlbumsViewModel
import com.maxrave.simpmusic.viewModel.MusixmatchViewModel
import com.maxrave.simpmusic.viewModel.NotificationViewModel
import com.maxrave.simpmusic.viewModel.NowPlayingBottomSheetViewModel
import com.maxrave.simpmusic.viewModel.PlaylistViewModel
import com.maxrave.simpmusic.viewModel.PodcastViewModel
import com.maxrave.simpmusic.viewModel.RecentlySongsViewModel
import com.maxrave.simpmusic.viewModel.SearchViewModel
import com.maxrave.simpmusic.viewModel.SettingsViewModel
import com.maxrave.simpmusic.viewModel.SharedViewModel
import org.koin.android.ext.koin.androidApplication
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

@UnstableApi
val viewModelModule =
    module {
        single {
            SharedViewModel(
                androidApplication(),
            )
        }
        single {
            SearchViewModel(
                application = androidApplication(),
            )
        }
        viewModel {
            NowPlayingBottomSheetViewModel(
                application = androidApplication(),
            )
        }
        viewModel {
            LibraryViewModel(
                application = androidApplication(),
            )
        }
        viewModel {
            LibraryDynamicPlaylistViewModel(
                application = androidApplication(),
            )
        }
        viewModel {
            AlbumViewModel(
                application = androidApplication(),
            )
        }
        viewModel {
            HomeViewModel(
                application = androidApplication(),
            )
        }
        viewModel {
            SettingsViewModel(
                application = androidApplication(),
            )
        }
        viewModel {
            ArtistViewModel(
                application = androidApplication(),
            )
        }
        viewModel {
            PlaylistViewModel(
                application = androidApplication(),
            )
        }
        viewModel {
            LogInViewModel(
                application = androidApplication(),
            )
        }
        viewModel {
            MusixmatchViewModel(
                application = androidApplication(),
            )
        }
        viewModel {
            PodcastViewModel(
                application = androidApplication(),
            )
        }
        viewModel {
            MoreAlbumsViewModel(
                application = androidApplication(),
            )
        }
        viewModel {
            RecentlySongsViewModel(
                application = androidApplication(),
            )
        }
        viewModel {
            LocalPlaylistViewModel(
                androidApplication(),
            )
        }
        viewModel {
            NotificationViewModel(
                androidApplication(),
            )
        }
        viewModel {
            MoodViewModel(
                androidApplication(),
            )
        }
    }