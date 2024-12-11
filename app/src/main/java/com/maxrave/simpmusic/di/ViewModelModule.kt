package com.maxrave.simpmusic.di

import androidx.media3.common.util.UnstableApi
import com.maxrave.simpmusic.viewModel.AlbumViewModel
import com.maxrave.simpmusic.viewModel.LibraryDynamicPlaylistViewModel
import com.maxrave.simpmusic.viewModel.LibraryViewModel
import com.maxrave.simpmusic.viewModel.NowPlayingBottomSheetViewModel
import com.maxrave.simpmusic.viewModel.SettingsViewModel
import org.koin.android.ext.koin.androidApplication
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

@UnstableApi
val viewModelModule =
    module {
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
            SettingsViewModel(
                application = androidApplication(),
            )
        }
    }