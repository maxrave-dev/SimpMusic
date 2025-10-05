package com.maxrave.data.di

import com.maxrave.common.Config.SERVICE_SCOPE
import com.maxrave.data.repository.AccountRepositoryImpl
import com.maxrave.data.repository.AlbumRepositoryImpl
import com.maxrave.data.repository.ArtistRepositoryImpl
import com.maxrave.data.repository.CommonRepositoryImpl
import com.maxrave.data.repository.HomeRepositoryImpl
import com.maxrave.data.repository.LocalPlaylistRepositoryImpl
import com.maxrave.data.repository.LyricsCanvasRepositoryImpl
import com.maxrave.data.repository.PlaylistRepositoryImpl
import com.maxrave.data.repository.PodcastRepositoryImpl
import com.maxrave.data.repository.SearchRepositoryImpl
import com.maxrave.data.repository.SongRepositoryImpl
import com.maxrave.data.repository.StreamRepositoryImpl
import com.maxrave.data.repository.UpdateRepositoryImpl
import com.maxrave.domain.repository.AccountRepository
import com.maxrave.domain.repository.AlbumRepository
import com.maxrave.domain.repository.ArtistRepository
import com.maxrave.domain.repository.CommonRepository
import com.maxrave.domain.repository.HomeRepository
import com.maxrave.domain.repository.LocalPlaylistRepository
import com.maxrave.domain.repository.LyricsCanvasRepository
import com.maxrave.domain.repository.PlaylistRepository
import com.maxrave.domain.repository.PodcastRepository
import com.maxrave.domain.repository.SearchRepository
import com.maxrave.domain.repository.SongRepository
import com.maxrave.domain.repository.StreamRepository
import com.maxrave.domain.repository.UpdateRepository
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.module

val repositoryModule =
    module {
        single<AccountRepository> {
            AccountRepositoryImpl(get(), get())
        }

        single<AlbumRepository> {
            AlbumRepositoryImpl(get(), get())
        }

        single<ArtistRepository> {
            ArtistRepositoryImpl(get(), get())
        }

        single<CommonRepository>(createdAtStart = true) {
            CommonRepositoryImpl(androidContext(), get(named(SERVICE_SCOPE)), get(), get(), get(), get(), get()).apply {
                this.init(get())
            }
        }

        single<HomeRepository> {
            HomeRepositoryImpl(get(), get())
        }

        single<LocalPlaylistRepository> {
            LocalPlaylistRepositoryImpl(androidContext(), get(), get())
        }

        single<LyricsCanvasRepository> {
            LyricsCanvasRepositoryImpl(get(), get(), get(), get(), get())
        }

        single<PlaylistRepository> {
            PlaylistRepositoryImpl(androidContext(), get(), get())
        }

        single<PodcastRepository> {
            PodcastRepositoryImpl(get(), get())
        }

        single<SearchRepository> {
            SearchRepositoryImpl(get(), get())
        }

        single<SongRepository> {
            SongRepositoryImpl(get(), get())
        }

        single<StreamRepository> {
            StreamRepositoryImpl(get(), get())
        }

        single<UpdateRepository> {
            UpdateRepositoryImpl(get())
        }
    }