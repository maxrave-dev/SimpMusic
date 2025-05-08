package com.maxrave.simpmusic.data.parser

import android.util.Log
import com.maxrave.kotlinytmusicscraper.models.AlbumItem
import com.maxrave.kotlinytmusicscraper.models.ArtistItem
import com.maxrave.kotlinytmusicscraper.models.PlaylistItem
import com.maxrave.kotlinytmusicscraper.models.SongItem
import com.maxrave.kotlinytmusicscraper.models.VideoItem
import com.maxrave.kotlinytmusicscraper.pages.ArtistPage
import com.maxrave.simpmusic.data.model.browse.artist.Albums
import com.maxrave.simpmusic.data.model.browse.artist.ArtistBrowse
import com.maxrave.simpmusic.data.model.browse.artist.Related
import com.maxrave.simpmusic.data.model.browse.artist.ResultAlbum
import com.maxrave.simpmusic.data.model.browse.artist.ResultPlaylist
import com.maxrave.simpmusic.data.model.browse.artist.ResultRelated
import com.maxrave.simpmusic.data.model.browse.artist.ResultSingle
import com.maxrave.simpmusic.data.model.browse.artist.ResultSong
import com.maxrave.simpmusic.data.model.browse.artist.ResultVideo
import com.maxrave.simpmusic.data.model.browse.artist.Singles
import com.maxrave.simpmusic.data.model.browse.artist.Songs
import com.maxrave.simpmusic.data.model.searchResult.songs.Album
import com.maxrave.simpmusic.data.model.searchResult.songs.Artist
import com.maxrave.simpmusic.data.model.searchResult.songs.Thumbnail

fun parseArtistData(data: ArtistPage): ArtistBrowse {
    for (i in data.sections) {
        Log.d("data", "title: ${i.title}")
        Log.d("data", "items: ${i.items}")
    }
    val songSection = data.sections.find { it.items.firstOrNull() is SongItem }
    val albumSection =
        data.sections.find { artistSection ->
            artistSection.items.firstOrNull().let { it is AlbumItem && !it.isSingle }
        }
    val singleSection =
        data.sections.find { artistSection ->
            artistSection.items.firstOrNull().let { it is AlbumItem && it.isSingle }
        }
    val videoSection =
        data.sections.find { it.items.firstOrNull() is VideoItem }
    val featuredOnSection =
        data.sections.find { it.items.firstOrNull() is PlaylistItem }
    Log.w("ArtistParser", "videoSection: ${videoSection?.items}")
    Log.w("ArtistParser", "featuredOnSection: ${featuredOnSection?.items}")
    val relatedSection = data.sections.find { it.items.firstOrNull() is ArtistItem }
    val listSong: ArrayList<ResultSong> = arrayListOf()
    val listAlbum: ArrayList<ResultAlbum> = arrayListOf()
    val listSingle: ArrayList<ResultSingle> = arrayListOf()
    val listRelated: ArrayList<ResultRelated> = arrayListOf()
    val listVideo: ArrayList<ResultVideo> = arrayListOf()
    val listFeaturedOn: ArrayList<ResultPlaylist> = arrayListOf()
    albumSection?.items?.forEach { album ->
        listAlbum.add(
            ResultAlbum(
                browseId = (album as AlbumItem).browseId,
                isExplicit = false,
                thumbnails = listOf(Thumbnail(544, album.thumbnail, 544)),
                title = album.title,
                year = album.year.toString(),
            ),
        )
    }
    singleSection?.items?.forEach {
        val single = it as AlbumItem
        listSingle.add(
            ResultSingle(
                browseId = single.browseId,
                thumbnails = listOf(Thumbnail(544, single.thumbnail, 544)),
                title = single.title,
                year = single.year.toString(),
            ),
        )
    }
    songSection?.items?.forEach {
        val song = it as SongItem
        listSong.add(
            ResultSong(
                videoId = song.id,
                title = song.title,
                artists =
                    song.artists.map { artist ->
                        Artist(
                            id = artist.id ?: "",
                            name = artist.name,
                        )
                    },
                album = Album(id = song.album?.id ?: "", name = song.album?.name ?: ""),
                likeStatus = "INDIFFERENT",
                thumbnails = listOf(Thumbnail(544, song.thumbnail, 544)),
                isAvailable = true,
                isExplicit = false,
                videoType = "Song",
                durationSeconds = song.duration ?: 0,
            ),
        )
    }
    featuredOnSection?.items?.forEach {
        val playlist = it as PlaylistItem
        listFeaturedOn.add(
            ResultPlaylist(
                id = playlist.id,
                author = playlist.author?.name ?: "",
                thumbnails = listOf(Thumbnail(544, playlist.thumbnail, 544)),
                title = playlist.title,
            ),
        )
    }
    relatedSection?.items?.forEach {
        val artist = it as ArtistItem
        listRelated.add(
            ResultRelated(
                browseId = artist.id,
                subscribers = artist.subscribers ?: "",
                thumbnails = listOf(Thumbnail(544, artist.thumbnail, 544)),
                title = artist.title,
            ),
        )
    }
    videoSection?.items?.forEach {
        val video = it as VideoItem
        listVideo.add(
            ResultVideo(
                artists =
                    video.artists.map { artist ->
                        Artist(
                            id = artist.id ?: "",
                            name = artist.name,
                        )
                    },
                category = null,
                duration = null,
                durationSeconds = video.duration,
                resultType = null,
                thumbnails = video.thumbnails?.thumbnails?.toListThumbnail(),
                title = video.title,
                videoId = video.id,
                videoType = null,
                views = video.view,
                year = "",
            ),
        )
    }
    Log.d("ArtistParser", "listSong: ${listSong.size}")
    Log.d("ArtistParser", "listAlbum: ${listAlbum.size}")
    Log.d("ArtistParser", "listSingle: ${listSingle.size}")
    Log.d("ArtistParser", "listRelated: ${listRelated.size}")
    return ArtistBrowse(
        albums = Albums(browseId = "", results = listAlbum, params = ""),
        channelId = data.artist.id,
        description = data.description,
        name = data.artist.title,
        radioId = data.artist.radioEndpoint,
        related = Related(browseId = "", results = listRelated),
        shuffleId = data.artist.shuffleEndpoint,
        singles = Singles(browseId = "", params = "", results = listSingle),
        songs = Songs(browseId = songSection?.moreEndpoint?.browseId, results = listSong),
        subscribed = false,
        subscribers = data.subscribers,
        thumbnails = listOf(Thumbnail(617, data.artist.thumbnail.replace("w1483", "w617"), 617)),
        views = data.view,
        video = listVideo,
        videoList = videoSection?.moreEndpoint?.browseId,
        featuredOn = listFeaturedOn,
    )
}