package com.maxrave.simpmusic.di

import com.maxrave.simpmusic.viewModel.AlbumViewModel
import com.maxrave.simpmusic.viewModel.ArtistViewModel
import com.maxrave.simpmusic.viewModel.HomeViewModel
import com.maxrave.simpmusic.viewModel.LibraryDynamicPlaylistViewModel
import com.maxrave.simpmusic.viewModel.LibraryViewModel
import com.maxrave.simpmusic.viewModel.LocalPlaylistViewModel
import com.maxrave.simpmusic.viewModel.LogInViewModel
import com.maxrave.simpmusic.viewModel.MoodViewModel
import com.maxrave.simpmusic.viewModel.MoreAlbumsViewModel
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

val viewModelModule =
    module {
        single {
            SharedViewModel(
                androidApplication(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
            )
        }
        single {
            SearchViewModel(
                application = androidApplication(),
                get(),
                get(),
            )
        }
        viewModel {
            NowPlayingBottomSheetViewModel(
                application = androidApplication(),
                get(),
                get(),
                get(),
            )
        }
        viewModel {
            LibraryViewModel(
                application = androidApplication(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
            )
        }
        viewModel {
            LibraryDynamicPlaylistViewModel(
                application = androidApplication(),
                get(),
                get(),
            )
        }
        viewModel {
            AlbumViewModel(
                application = androidApplication(),
                get(),
                get(),
            )
        }
        viewModel {
            HomeViewModel(
                application = androidApplication(),
                get(),
                get(),
            )
        }
        viewModel {
            SettingsViewModel(
                application = androidApplication(),
                get(),
                get(),
                get(),
                get(),
                get(),
            )
        }
        viewModel {
            ArtistViewModel(
                application = androidApplication(),
                get(),
                get(),
            )
        }
        viewModel {
            PlaylistViewModel(
                application = androidApplication(),
                get(),
                get(),
                get(),
            )
        }
        viewModel {
            LogInViewModel(
                application = androidApplication(),
                get(),
            )
        }
        viewModel {
            PodcastViewModel(
                application = androidApplication(),
                get(),
            )
        }
        viewModel {
            MoreAlbumsViewModel(
                application = androidApplication(),
                get(),
            )
        }
        viewModel {
            RecentlySongsViewModel(
                application = androidApplication(),
                get(),
            )
        }
        viewModel {
            LocalPlaylistViewModel(
                androidApplication(),
                get(),
                get(),
                get(),
            )
        }
        viewModel {
            NotificationViewModel(
                androidApplication(),
                get(),
            )
        }
        viewModel {
            MoodViewModel(
                androidApplication(),
                get(),
                get(),
            )
        }
    }