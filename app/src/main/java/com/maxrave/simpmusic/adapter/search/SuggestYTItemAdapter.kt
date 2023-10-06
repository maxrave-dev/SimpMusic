package com.maxrave.simpmusic.adapter.search

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.maxrave.kotlinytmusicscraper.models.AlbumItem
import com.maxrave.kotlinytmusicscraper.models.ArtistItem
import com.maxrave.kotlinytmusicscraper.models.PlaylistItem
import com.maxrave.kotlinytmusicscraper.models.SongItem
import com.maxrave.kotlinytmusicscraper.models.VideoItem
import com.maxrave.kotlinytmusicscraper.models.YTItem
import com.maxrave.simpmusic.R
import com.maxrave.simpmusic.common.Config.ALBUM_CLICK
import com.maxrave.simpmusic.common.Config.PLAYLIST_CLICK
import com.maxrave.simpmusic.common.Config.SONG_CLICK
import com.maxrave.simpmusic.common.Config.VIDEO_CLICK
import com.maxrave.simpmusic.databinding.ItemAlbumSearchResultBinding
import com.maxrave.simpmusic.databinding.ItemArtistSearchResultBinding
import com.maxrave.simpmusic.databinding.ItemPlaylistSearchResultBinding
import com.maxrave.simpmusic.databinding.ItemSongsSearchResultBinding
import com.maxrave.simpmusic.databinding.ItemsVideosSearchResultBinding
import com.maxrave.simpmusic.extension.connectArtists
import com.maxrave.simpmusic.extension.toListName
import com.maxrave.simpmusic.extension.toTrack

class SuggestYTItemAdapter(private val listYtItems: ArrayList<YTItem>, private val context: Context): RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private lateinit var mListener: onItemClickListener

    interface onItemClickListener{
        fun onItemClick(position: Int, type: String)
    }
    fun setOnClickListener(listener: onItemClickListener){
        mListener = listener
    }
    fun getCurrentList(): ArrayList<YTItem> = listYtItems



    inner class SongViewHolder(val binding: ItemSongsSearchResultBinding, listener: onItemClickListener): RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                listener.onItemClick(bindingAdapterPosition, SONG_CLICK)
            }
        }
        fun bind(data: SongItem){
            val song = data.toTrack()
            with(binding){
                if (!song.thumbnails.isNullOrEmpty()) {
                    if (song.thumbnails.size > 1){
                        ivThumbnail.load(song.thumbnails[1].url)}
                    else{
                        ivThumbnail.load(song.thumbnails[0].url)}
                }
                else {
                    ivThumbnail.load(data.thumbnail)
                }
                tvSongTitle.text = song.title
                val artistName = song.artists.toListName().connectArtists()
                tvSongArtist.text = context.getString(R.string.Song_and_artist_name, artistName)
                tvSongAlbum.text = song.album?.name
                tvSongTitle.isSelected = true
                tvSongArtist.isSelected = true
                tvSongAlbum.isSelected = true
                btOptions.visibility = View.GONE
            }
        }
    }
    inner class VideoViewHolder(val binding: ItemsVideosSearchResultBinding, listener: onItemClickListener): RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                listener.onItemClick(bindingAdapterPosition, VIDEO_CLICK)
            }
        }
        fun bind(video: VideoItem){
            val data = video.toTrack()
            with (binding) {
                btOptions.visibility = View.GONE
                ivThumbnail.load(video.thumbnails?.thumbnails?.lastOrNull()?.url)
                tvVideoTitle.text = data.title
                tvAuthor.text = data.artists.toListName().connectArtists()
                tvView.text = video.view
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
        fun bind(artist: ArtistItem){
            with(binding){
                ivThumbnail.load(artist.thumbnail)
                tvArtistName.text = artist.title
            }
        }
    }
    inner class PlaylistViewHolder(val binding: ItemPlaylistSearchResultBinding, listener: onItemClickListener): RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                listener.onItemClick(bindingAdapterPosition, PLAYLIST_CLICK)
            }
        }
        fun bind(playlist: PlaylistItem) {
            with(binding) {
                ivThumbnail.load(playlist.thumbnail)
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
        fun bind(album: AlbumItem){
            with(binding){
                ivThumbnail.load(album.thumbnail)
                tvAlbumName.text = album.title
                val artistName = album.artists?.firstOrNull()?.name
//                artistName = removeTrailingComma(artistName)
//                artistName = removeComma(artistName)
                tvAlbumArtist.text = context.getString(R.string.album_and_artist_name, artistName)
                tvAlbumYear.text = (album.year ?: "").toString()
                tvAlbumName.isSelected = true
                tvAlbumArtist.isSelected = true
                tvAlbumYear.isSelected = true
            }
        }
    }
    fun updateList(newList: ArrayList<YTItem>){
        listYtItems.clear()
        listYtItems.addAll(newList)
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return when (listYtItems[position]) {
            is SongItem -> VIEW_TYPE_SONG
            is ArtistItem -> VIEW_TYPE_ARTIST
            is PlaylistItem -> VIEW_TYPE_PLAYLIST
            is AlbumItem -> VIEW_TYPE_ALBUM
            is VideoItem -> VIEW_TYPE_VIDEO
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
            else -> throw IllegalArgumentException("Unknown view type")
        }
    }

    override fun getItemCount(): Int {
        return listYtItems.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is SongViewHolder -> {
                holder.bind(listYtItems[position] as SongItem)
            }
            is VideoViewHolder -> {
                holder.bind(listYtItems[position] as VideoItem)
            }
            is ArtistViewHolder -> {
                holder.bind(listYtItems[position] as ArtistItem)
            }
            is PlaylistViewHolder -> {
                holder.bind(listYtItems[position] as PlaylistItem)
            }
            is AlbumViewHolder -> {
                holder.bind(listYtItems[position] as AlbumItem)
            }
        }
    }
    companion object {
        private const val VIEW_TYPE_SONG = 0
        private const val VIEW_TYPE_ARTIST = 1
        private const val VIEW_TYPE_PLAYLIST = 2
        private const val VIEW_TYPE_ALBUM = 3
        private const val VIEW_TYPE_VIDEO = 4
    }
}