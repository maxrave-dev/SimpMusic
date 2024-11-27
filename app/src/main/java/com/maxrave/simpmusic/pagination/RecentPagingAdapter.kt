package com.maxrave.simpmusic.pagination

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import coil3.load
import coil3.request.crossfade
import coil3.request.placeholder
import com.maxrave.simpmusic.R
import com.maxrave.simpmusic.common.Config
import com.maxrave.simpmusic.data.db.entities.AlbumEntity
import com.maxrave.simpmusic.data.db.entities.ArtistEntity
import com.maxrave.simpmusic.data.db.entities.PlaylistEntity
import com.maxrave.simpmusic.data.db.entities.SongEntity
import com.maxrave.simpmusic.databinding.ItemAlbumSearchResultBinding
import com.maxrave.simpmusic.databinding.ItemArtistSearchResultBinding
import com.maxrave.simpmusic.databinding.ItemPlaylistSearchResultBinding
import com.maxrave.simpmusic.databinding.ItemSongsSearchResultBinding
import com.maxrave.simpmusic.extension.connectArtists

class RecentPagingAdapter(private val context: Context): PagingDataAdapter<Any, RecyclerView.ViewHolder>(DIFF_CALLBACK) {
    companion object DIFF_CALLBACK : DiffUtil.ItemCallback<Any>() {
        override fun areItemsTheSame(oldItem: Any, newItem: Any): Boolean {
            return when {
                oldItem is SongEntity && newItem is SongEntity -> oldItem.videoId == newItem.videoId
                oldItem is ArtistEntity && newItem is ArtistEntity -> oldItem.channelId == newItem.channelId
                oldItem is PlaylistEntity && newItem is PlaylistEntity -> oldItem.id == newItem.id
                oldItem is AlbumEntity && newItem is AlbumEntity -> oldItem.browseId == newItem.browseId
                else -> false
            }
        }

        @SuppressLint("DiffUtilEquals")
        override fun areContentsTheSame(oldItem: Any, newItem: Any): Boolean {
            return when {
                oldItem is SongEntity && newItem is SongEntity -> oldItem == newItem
                oldItem is ArtistEntity && newItem is ArtistEntity -> oldItem == newItem
                oldItem is PlaylistEntity && newItem is PlaylistEntity -> oldItem == newItem
                oldItem is AlbumEntity && newItem is AlbumEntity -> oldItem == newItem
                else -> false
            }
        }
    }
    private lateinit var mListener: onItemClickListener

    interface onItemClickListener{
        fun onItemClick(position: Int, type: String)
    }
    fun setOnClickListener(listener: onItemClickListener){
        mListener = listener

    }
    fun getItemByIndex(position: Int): Any?{
        return getItem(position)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is SongEntityViewHolder -> {
                holder.bind(getItem(position) as SongEntity)
            }
            is ArtistEntityViewHolder -> {
                holder.bind(getItem(position) as ArtistEntity)
            }
            is PlaylistEntityViewHolder -> {
                holder.bind(getItem(position) as PlaylistEntity)
            }
            is AlbumEntityViewHolder -> {
                holder.bind(getItem(position) as AlbumEntity)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE.VIEW_TYPE_SONG_ENTITY -> {
                val binding = ItemSongsSearchResultBinding.inflate(inflater, parent, false)
                SongEntityViewHolder(binding, mListener)
            }
            VIEW_TYPE.VIEW_TYPE_ARTIST_ENTITY -> {
                val binding = ItemArtistSearchResultBinding.inflate(inflater, parent, false)
                ArtistEntityViewHolder(binding, mListener)
            }
            VIEW_TYPE.VIEW_TYPE_PLAYLIST_ENTITY -> {
                val binding = ItemPlaylistSearchResultBinding.inflate(inflater, parent, false)
                PlaylistEntityViewHolder(binding, mListener)
            }
            VIEW_TYPE.VIEW_TYPE_ALBUM_ENTITY -> {
                val binding = ItemAlbumSearchResultBinding.inflate(inflater, parent, false)
                AlbumEntityViewHolder(binding, mListener)
            }
            else -> throw IllegalArgumentException("Unknown view type")
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is SongEntity -> VIEW_TYPE.VIEW_TYPE_SONG_ENTITY
            is ArtistEntity -> VIEW_TYPE.VIEW_TYPE_ARTIST_ENTITY
            is PlaylistEntity -> VIEW_TYPE.VIEW_TYPE_PLAYLIST_ENTITY
            is AlbumEntity -> VIEW_TYPE.VIEW_TYPE_ALBUM_ENTITY
            else -> throw IllegalArgumentException("Unknown view type")
        }
    }
    
    inner class AlbumEntityViewHolder(val binding: ItemAlbumSearchResultBinding, listener: onItemClickListener): RecyclerView.ViewHolder(binding.root){
        init {
            binding.root.setOnClickListener {
                listener.onItemClick(bindingAdapterPosition, Config.ALBUM_CLICK)
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
    inner class PlaylistEntityViewHolder(val binding: ItemPlaylistSearchResultBinding, listener: onItemClickListener): RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                listener.onItemClick(bindingAdapterPosition, Config.PLAYLIST_CLICK)
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
    inner class SongEntityViewHolder(val binding: ItemSongsSearchResultBinding, listener: onItemClickListener): RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                listener.onItemClick(bindingAdapterPosition, Config.SONG_CLICK)
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
                btOptions.visibility = View.GONE
            }
        }
    }

    object VIEW_TYPE {
        const val VIEW_TYPE_SONG_ENTITY = 5
        const val VIEW_TYPE_ARTIST_ENTITY = 6
        const val VIEW_TYPE_PLAYLIST_ENTITY = 7
        const val VIEW_TYPE_ALBUM_ENTITY = 8
    }
}