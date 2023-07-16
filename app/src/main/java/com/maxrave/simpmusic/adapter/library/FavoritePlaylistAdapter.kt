package com.maxrave.simpmusic.adapter.library

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.maxrave.simpmusic.common.DownloadState
import com.maxrave.simpmusic.data.db.entities.AlbumEntity
import com.maxrave.simpmusic.data.db.entities.PlaylistEntity
import com.maxrave.simpmusic.databinding.ItemYourPlaylistBinding
import com.maxrave.simpmusic.extension.connectArtists

class FavoritePlaylistAdapter(private var listPlaylist: ArrayList<Any>) : RecyclerView.Adapter<RecyclerView.ViewHolder>(){
    private lateinit var mListener: OnItemClickListener
    interface OnItemClickListener{
        fun onItemClick(position: Int, type: String)
    }
    fun setOnClickListener(listener: OnItemClickListener){
        mListener = listener
    }

    inner class AlbumViewHolder(val binding: ItemYourPlaylistBinding, listener: OnItemClickListener): RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                listener.onItemClick(bindingAdapterPosition, "album")
            }
        }
        fun bind(album: AlbumEntity) {
            with(binding) {
                ivArt.load(album.thumbnails)
                tvPlaylistName.text = album.title
                tvArtistName.text = album.artistName?.connectArtists()
                tvStatus.text =
                    if (album.downloadState == DownloadState.STATE_NOT_DOWNLOADED) "Available online"
                    else if (album.downloadState == DownloadState.STATE_DOWNLOADING) "Downloading"
                    else if (album.downloadState == DownloadState.STATE_PREPARING) "Preparing"
                    else if (album.downloadState == DownloadState.STATE_DOWNLOADED) "Downloaded"
                    else "Unavailable"
            }
        }
    }
    inner class PlaylistViewHolder(val binding: ItemYourPlaylistBinding, listener: OnItemClickListener): RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                listener.onItemClick(bindingAdapterPosition, "playlist")
            }
        }
        fun bind(playlist: PlaylistEntity){
            with(binding){
                ivArt.load(playlist.thumbnails)
                tvPlaylistName.text = playlist.title
                tvArtistName.text = playlist.author
                tvStatus.text =
                    if (playlist.downloadState == DownloadState.STATE_NOT_DOWNLOADED) "Available online"
                    else if (playlist.downloadState == DownloadState.STATE_DOWNLOADING) "Downloading"
                    else if (playlist.downloadState == DownloadState.STATE_PREPARING) "Preparing"
                    else if (playlist.downloadState == DownloadState.STATE_DOWNLOADED) "Downloaded"
                    else "Unavailable"
            }
        }
    }
    fun updateList(list: List<Any>){
        listPlaylist.clear()
        listPlaylist.addAll(list)
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return when (listPlaylist[position]) {
            is AlbumEntity -> VIEW_TYPE_ALBUM
            is PlaylistEntity -> VIEW_TYPE_PLAYLIST
            else -> throw IllegalArgumentException("Invalid type of data $position")
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_ALBUM -> AlbumViewHolder(ItemYourPlaylistBinding.inflate(inflater, parent, false), mListener)
            VIEW_TYPE_PLAYLIST -> PlaylistViewHolder(ItemYourPlaylistBinding.inflate(inflater, parent, false), mListener)
            else -> throw IllegalArgumentException("Invalid type of data $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is AlbumViewHolder -> holder.bind(listPlaylist[position] as AlbumEntity)
            is PlaylistViewHolder -> holder.bind(listPlaylist[position] as PlaylistEntity)
        }
    }

    override fun getItemCount(): Int = listPlaylist.size

    companion object {
        private const val VIEW_TYPE_ALBUM = 0
        private const val VIEW_TYPE_PLAYLIST = 1
    }

}