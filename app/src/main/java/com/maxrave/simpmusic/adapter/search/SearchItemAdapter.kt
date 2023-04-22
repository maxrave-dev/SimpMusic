package com.maxrave.simpmusic.adapter.search

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.maxrave.simpmusic.data.model.searchResult.songs.SongsResult
import com.maxrave.simpmusic.databinding.ItemSongsSearchResultBinding
import coil.load
import com.maxrave.simpmusic.R
import com.maxrave.simpmusic.data.model.searchResult.albums.AlbumsResult
import com.maxrave.simpmusic.data.model.searchResult.artists.ArtistsResult
import com.maxrave.simpmusic.data.model.searchResult.playlists.PlaylistsResult
import com.maxrave.simpmusic.data.model.searchResult.videos.VideosResult
import com.maxrave.simpmusic.databinding.ItemAlbumSearchResultBinding
import com.maxrave.simpmusic.databinding.ItemArtistSearchResultBinding
import com.maxrave.simpmusic.databinding.ItemPlaylistSearchResultBinding

class SearchItemAdapter(private var searchResultList: ArrayList<Any>, var context: Context): RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private lateinit var mListener: onItemClickListener

    interface onItemClickListener{
        fun onItemClick(position: Int, type: String)
    }
    fun setOnClickListener(listener: onItemClickListener){
        mListener = listener

    }
    fun getCurrentList(): ArrayList<Any> = searchResultList



    inner class SongViewHolder(val binding: ItemSongsSearchResultBinding, listener: onItemClickListener): RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                listener.onItemClick(bindingAdapterPosition, "song")
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
                var artistName = ""
                if (song.artists != null) {
                    for (artist in song.artists) {
                        artistName += artist.name + ", "
                    }
                }
                artistName = removeTrailingComma(artistName)
                artistName = removeComma(artistName)
                tvSongArtist.text = context.getString(R.string.Song_and_artist_name, artistName)
                tvSongAlbum.text = song.album?.name
                tvSongTitle.isSelected = true
                tvSongArtist.isSelected = true
                tvSongAlbum.isSelected = true
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
    inner class PlaylistViewHolder(val binding: ItemPlaylistSearchResultBinding, listener: onItemClickListener): RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                listener.onItemClick(bindingAdapterPosition, "playlist")
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
    inner class AlbumViewHolder(val binding: ItemAlbumSearchResultBinding, listener: onItemClickListener): RecyclerView.ViewHolder(binding.root){
        init {
            binding.root.setOnClickListener {
                listener.onItemClick(bindingAdapterPosition, "album")
            }
        }
        fun bind(album: AlbumsResult){
            with(binding){
                if (album.thumbnails.size > 1){
                    ivThumbnail.load(album.thumbnails[1].url)}
                else{
                    ivThumbnail.load(album.thumbnails[0].url)}
                tvAlbumName.text = album.title
                var artistName = ""
                for (artist in album.artists) {
                    artistName += artist.name + ", "
                }
                artistName = removeTrailingComma(artistName)
                artistName = removeComma(artistName)
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
//            VIEW_TYPE_VIDEO -> {
//                TODO()
//            }
            //Chưa kịp làm video, h mới biết là có video trong search kkk
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
        }
    }
    fun removeTrailingComma(sentence: String): String {
        val trimmed = sentence.trimEnd()
        return if (trimmed.endsWith(", ")) {
            trimmed.dropLast(2)
        } else {
            trimmed
        }
    }


    fun removeComma(string: String): String {
        if (string.endsWith(',')) {
            return string.substring(0, string.length - 1)
        } else {
            return string
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