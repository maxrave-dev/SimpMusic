package com.maxrave.simpmusic.adapter.search

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.maxrave.simpmusic.R
import com.maxrave.simpmusic.common.Config.ALBUM_CLICK
import com.maxrave.simpmusic.common.Config.PLAYLIST_CLICK
import com.maxrave.simpmusic.common.Config.SONG_CLICK
import com.maxrave.simpmusic.common.Config.VIDEO_CLICK
import com.maxrave.simpmusic.data.db.entities.AlbumEntity
import com.maxrave.simpmusic.data.db.entities.ArtistEntity
import com.maxrave.simpmusic.data.db.entities.PlaylistEntity
import com.maxrave.simpmusic.data.db.entities.SongEntity
import com.maxrave.simpmusic.data.model.searchResult.albums.AlbumsResult
import com.maxrave.simpmusic.data.model.searchResult.artists.ArtistsResult
import com.maxrave.simpmusic.data.model.searchResult.playlists.PlaylistsResult
import com.maxrave.simpmusic.data.model.searchResult.songs.SongsResult
import com.maxrave.simpmusic.data.model.searchResult.videos.VideosResult
import com.maxrave.simpmusic.databinding.ItemAlbumSearchResultBinding
import com.maxrave.simpmusic.databinding.ItemArtistSearchResultBinding
import com.maxrave.simpmusic.databinding.ItemPlaylistSearchResultBinding
import com.maxrave.simpmusic.databinding.ItemSongsSearchResultBinding
import com.maxrave.simpmusic.databinding.ItemsVideosSearchResultBinding
import com.maxrave.simpmusic.extension.connectArtists
import com.maxrave.simpmusic.extension.toListName

class SearchItemAdapter(private var searchResultList: ArrayList<Any>, var context: Context): RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private lateinit var mListener: onItemClickListener

    interface onItemClickListener{
        fun onItemClick(position: Int, type: String)
        fun onOptionsClick(position: Int, type: String)
    }
    fun setOnClickListener(listener: onItemClickListener){
        mListener = listener
    }
    fun getCurrentList(): ArrayList<Any> = searchResultList



    inner class SongViewHolder(val binding: ItemSongsSearchResultBinding, listener: onItemClickListener): RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                listener.onItemClick(bindingAdapterPosition, SONG_CLICK)
            }
            binding.btOptions.setOnClickListener {
                listener.onOptionsClick(bindingAdapterPosition, SONG_CLICK)
            }
        }
        fun bind(song: SongsResult){
            with(binding){
                if (song.thumbnails != null) {
                    if (song.thumbnails.size > 1){
                        ivThumbnail.load(song.thumbnails[1].url)}
                    else{
                        ivThumbnail.load(song.thumbnails[0].url)}
                }
                tvSongTitle.text = song.title
                val artistName = song.artists.toListName().connectArtists()
                tvSongArtist.text = context.getString(R.string.Song_and_artist_name, artistName)
                tvSongAlbum.text = song.album?.name
                tvSongTitle.isSelected = true
                tvSongArtist.isSelected = true
                tvSongAlbum.isSelected = true
            }
        }
    }
    inner class SongEntityViewHolder(val binding: ItemSongsSearchResultBinding, listener: onItemClickListener): RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                listener.onItemClick(bindingAdapterPosition, SONG_CLICK)
            }
            binding.btOptions.setOnClickListener {
                listener.onOptionsClick(bindingAdapterPosition, SONG_CLICK)
            }
        }
        fun bind(song: SongEntity){
            with(binding){
                ivThumbnail.load(song.thumbnails)
                tvSongTitle.text = song.title
                tvSongArtist.text = context.getString(R.string.Song_and_artist_name, song.artistName?.connectArtists())
                tvSongAlbum.text = song.albumName
                tvSongTitle.isSelected = true
                tvSongArtist.isSelected = true
                tvSongAlbum.isSelected = true
            }
        }
    }
    inner class VideoViewHolder(val binding: ItemsVideosSearchResultBinding, listener: onItemClickListener): RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                listener.onItemClick(bindingAdapterPosition, VIDEO_CLICK)
            }
            binding.btOptions.setOnClickListener {
                listener.onOptionsClick(bindingAdapterPosition, VIDEO_CLICK)
            }
        }
        fun bind(video: VideosResult){
            with (binding) {
                ivThumbnail.load(video.thumbnails?.get(0)?.url)
                tvVideoTitle.text = video.title
                val tempArtist = mutableListOf<String>()
                if (video.artists != null){
                    tvAuthor.text = video.artists.toListName().connectArtists()
                }
                else{
                    tvAuthor.text = ""
                }
                tvView.text = video.views
                tvVideoTitle.isSelected = true
            }
        }
    }
    inner class ArtistViewHolder(val binding: ItemArtistSearchResultBinding, listener: onItemClickListener): RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                listener.onItemClick(bindingAdapterPosition, "artist")
            }
        }
        fun bind(artist: ArtistsResult){
            with(binding){
                ivThumbnail.load(artist.thumbnails[0].url)
                tvArtistName.text = artist.artist
            }
        }
    }
    inner class ArtistEntityViewHolder(val binding: ItemArtistSearchResultBinding, listener: onItemClickListener): RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                listener.onItemClick(bindingAdapterPosition, "artist")
            }
        }
        fun bind(artist: ArtistEntity){
            with(binding){
                ivThumbnail.load(artist.thumbnails)
                tvArtistName.text = artist.name
            }
        }
    }
    inner class PlaylistViewHolder(val binding: ItemPlaylistSearchResultBinding, listener: onItemClickListener): RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                listener.onItemClick(bindingAdapterPosition, PLAYLIST_CLICK)
            }
        }
        fun bind(playlist: PlaylistsResult) {
            with(binding) {
                if (playlist.thumbnails.size > 1){
                    ivThumbnail.load(playlist.thumbnails[1].url)}
                else{
                    ivThumbnail.load(playlist.thumbnails[0].url)}
                tvPlaylistName.text = playlist.title
                tvPlaylistAuthor.text = context.getString(R.string.playlist_and_author, playlist.author)
                tvPlaylistName.isSelected = true
                tvPlaylistAuthor.isSelected = true
            }
        }
    }
    inner class PlaylistEntityViewHolder(val binding: ItemPlaylistSearchResultBinding, listener: onItemClickListener): RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                listener.onItemClick(bindingAdapterPosition, PLAYLIST_CLICK)
            }
        }
        fun bind(playlist: PlaylistEntity) {
            with(binding) {
                ivThumbnail.load(playlist.thumbnails)
                tvPlaylistName.text = playlist.title
                tvPlaylistAuthor.text = context.getString(R.string.playlist_and_author, playlist.author)
                tvPlaylistName.isSelected = true
                tvPlaylistAuthor.isSelected = true
            }
        }
    }
    inner class AlbumViewHolder(val binding: ItemAlbumSearchResultBinding, listener: onItemClickListener): RecyclerView.ViewHolder(binding.root){
        init {
            binding.root.setOnClickListener {
                listener.onItemClick(bindingAdapterPosition, ALBUM_CLICK)
            }
        }
        fun bind(album: AlbumsResult){
            with(binding){
                if (album.thumbnails.size > 1){
                    ivThumbnail.load(album.thumbnails[1].url)}
                else{
                    ivThumbnail.load(album.thumbnails[0].url)}
                tvAlbumName.text = album.title
                val artistName = album.artists.toListName().connectArtists()
//                artistName = removeTrailingComma(artistName)
//                artistName = removeComma(artistName)
                tvAlbumArtist.text = context.getString(R.string.album_and_artist_name, artistName)
                tvAlbumYear.text = album.year
                tvAlbumName.isSelected = true
                tvAlbumArtist.isSelected = true
                tvAlbumYear.isSelected = true
            }
        }
    }
    inner class AlbumEntityViewHolder(val binding: ItemAlbumSearchResultBinding, listener: onItemClickListener): RecyclerView.ViewHolder(binding.root){
        init {
            binding.root.setOnClickListener {
                listener.onItemClick(bindingAdapterPosition, ALBUM_CLICK)
            }
        }
        fun bind(album: AlbumEntity){
            with(binding){
                ivThumbnail.load(album.thumbnails)
                tvAlbumName.text = album.title
                val artistName = album.artistName?.connectArtists()
                tvAlbumArtist.text = context.getString(R.string.album_and_artist_name, artistName)
                tvAlbumYear.text = album.year
                tvAlbumName.isSelected = true
                tvAlbumArtist.isSelected = true
                tvAlbumYear.isSelected = true
            }
        }
    }
    fun updateList(newList: ArrayList<Any>){
        searchResultList.clear()
        searchResultList.addAll(newList)
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return when (searchResultList[position]) {
            is SongsResult -> VIEW_TYPE_SONG
            is ArtistsResult -> VIEW_TYPE_ARTIST
            is PlaylistsResult -> VIEW_TYPE_PLAYLIST
            is AlbumsResult -> VIEW_TYPE_ALBUM
            is VideosResult -> VIEW_TYPE_VIDEO
            is SongEntity -> VIEW_TYPE_SONG_ENTITY
            is ArtistEntity -> VIEW_TYPE_ARTIST_ENTITY
            is PlaylistEntity -> VIEW_TYPE_PLAYLIST_ENTITY
            is AlbumEntity -> VIEW_TYPE_ALBUM_ENTITY
            else -> throw IllegalArgumentException("Unknown view type")
        }
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_SONG -> {
                val binding = ItemSongsSearchResultBinding.inflate(inflater, parent, false)
                SongViewHolder(binding, mListener)
            }
            VIEW_TYPE_ARTIST -> {
                val binding = ItemArtistSearchResultBinding.inflate(inflater, parent, false)
                ArtistViewHolder(binding, mListener)
            }
            VIEW_TYPE_PLAYLIST -> {
                val binding = ItemPlaylistSearchResultBinding.inflate(inflater, parent, false)
                PlaylistViewHolder(binding, mListener)
            }
            VIEW_TYPE_ALBUM -> {
                val binding = ItemAlbumSearchResultBinding.inflate(inflater, parent, false)
                AlbumViewHolder(binding, mListener)
            }
            VIEW_TYPE_VIDEO -> {
                val binding = ItemsVideosSearchResultBinding.inflate(inflater, parent, false)
                VideoViewHolder(binding, mListener)
            }
            VIEW_TYPE_SONG_ENTITY -> {
                val binding = ItemSongsSearchResultBinding.inflate(inflater, parent, false)
                SongEntityViewHolder(binding, mListener)
            }
            VIEW_TYPE_ARTIST_ENTITY -> {
                val binding = ItemArtistSearchResultBinding.inflate(inflater, parent, false)
                ArtistEntityViewHolder(binding, mListener)
            }
            VIEW_TYPE_PLAYLIST_ENTITY -> {
                val binding = ItemPlaylistSearchResultBinding.inflate(inflater, parent, false)
                PlaylistEntityViewHolder(binding, mListener)
            }
            VIEW_TYPE_ALBUM_ENTITY -> {
                val binding = ItemAlbumSearchResultBinding.inflate(inflater, parent, false)
                AlbumEntityViewHolder(binding, mListener)
            }
            else -> throw IllegalArgumentException("Unknown view type")
        }
    }

    override fun getItemCount(): Int {
        return searchResultList.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is SongViewHolder -> {
                holder.bind(searchResultList[position] as SongsResult)
            }
            is ArtistViewHolder -> {
                holder.bind(searchResultList[position] as ArtistsResult)
            }
            is PlaylistViewHolder -> {
                holder.bind(searchResultList[position] as PlaylistsResult)
            }
            is AlbumViewHolder -> {
                holder.bind(searchResultList[position] as AlbumsResult)
            }
            is VideoViewHolder -> {
                holder.bind(searchResultList[position] as VideosResult)
            }
            is SongEntityViewHolder -> {
                holder.bind(searchResultList[position] as SongEntity)
            }
            is ArtistEntityViewHolder -> {
                holder.bind(searchResultList[position] as ArtistEntity)
            }
            is PlaylistEntityViewHolder -> {
                holder.bind(searchResultList[position] as PlaylistEntity)
            }
            is AlbumEntityViewHolder -> {
                holder.bind(searchResultList[position] as AlbumEntity)
            }
        }
    }
    companion object {
        private const val VIEW_TYPE_SONG = 0
        private const val VIEW_TYPE_ARTIST = 1
        private const val VIEW_TYPE_PLAYLIST = 2
        private const val VIEW_TYPE_ALBUM = 3
        private const val VIEW_TYPE_VIDEO = 4
        private const val VIEW_TYPE_SONG_ENTITY = 5
        private const val VIEW_TYPE_ARTIST_ENTITY = 6
        private const val VIEW_TYPE_PLAYLIST_ENTITY = 7
        private const val VIEW_TYPE_ALBUM_ENTITY = 8
    }
}