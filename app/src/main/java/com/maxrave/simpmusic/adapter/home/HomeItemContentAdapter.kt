package com.maxrave.simpmusic.adapter.home

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.maxrave.simpmusic.data.model.home.Content
import com.maxrave.simpmusic.databinding.ItemHomeContentPlaylistBinding
import com.maxrave.simpmusic.databinding.ItemHomeContentSongBinding
import com.maxrave.simpmusic.extension.connectArtists
import com.maxrave.simpmusic.extension.toListName

class HomeItemContentAdapter(private var listContent: ArrayList<Content>): RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private lateinit var mPlaylistListener: onPlaylistItemClickListener
    private lateinit var mAlbumListener: onAlbumItemClickListener
    private lateinit var mSongListener: onSongItemClickListener
    interface onSongItemClickListener{
        fun onSongItemClick(position: Int)
    }
    interface onPlaylistItemClickListener{
        fun onPlaylistItemClick(position: Int)
    }
    interface onAlbumItemClickListener{
        fun onAlbumItemClick(position: Int)
    }
    fun setOnSongClickListener(listener: onSongItemClickListener){
        mSongListener = listener
    }
    fun setOnPlaylistClickListener(listener: onPlaylistItemClickListener){
        mPlaylistListener = listener
    }
    fun setOnAlbumClickListener(listener: onAlbumItemClickListener){
        mAlbumListener = listener
    }
    inner class SongViewHolder(var binding: ItemHomeContentSongBinding, var listener: onSongItemClickListener): RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {listener.onSongItemClick(bindingAdapterPosition)}
        }
        fun bind(content: Content){
            with(binding){
                if (content.thumbnails.size > 1) {
                    ivArt.load(content.thumbnails[1].url)
                }
                else{
                    ivArt.load(content.thumbnails[0].url)
                }
                tvSongName.text = content.title
                tvSongName.isSelected = true
                tvArtistName.text = content.artists.toListName().connectArtists()
                tvArtistName.isSelected = true
                tvAlbumName.text = content.album?.name
                tvAlbumName.isSelected = true
            }
        }
    }
    inner class PlaylistViewHolder(var binding: ItemHomeContentPlaylistBinding, var listener: onPlaylistItemClickListener): RecyclerView.ViewHolder(binding.root){
        init {
            binding.root.setOnClickListener {listener.onPlaylistItemClick(bindingAdapterPosition)}
        }
        fun bind(content: Content){
            with(binding){
                if (content.thumbnails.size > 1) {
                    ivArt.load(content.thumbnails[1].url)
                }
                else{
                    ivArt.load(content.thumbnails[0].url)
                }
                tvTitle.text = content.title
                tvTitle.isSelected = true
                tvDescription.text = content.description
                tvDescription.isSelected = true
            }
        }
    }
    inner class AlbumViewHolder(var binding: ItemHomeContentPlaylistBinding, var listener: onAlbumItemClickListener): RecyclerView.ViewHolder(binding.root){
        init {
            binding.root.setOnClickListener {listener.onAlbumItemClick(bindingAdapterPosition)}
        }
        fun bind(content: Content){
            with(binding){
                if (content.thumbnails.size > 1) {
                    ivArt.load(content.thumbnails[1].url)
                }
                else{
                    ivArt.load(content.thumbnails[0].url)
                }
                tvTitle.text = content.title
                tvTitle.isSelected = true
                tvDescription.visibility = View.GONE
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflate = LayoutInflater.from(parent.context)
        return when (viewType) {
            SONG -> SongViewHolder(ItemHomeContentSongBinding.inflate(inflate, parent, false), mSongListener)
            PLAYLIST -> PlaylistViewHolder(ItemHomeContentPlaylistBinding.inflate(inflate, parent, false), mPlaylistListener)
            ALBUM -> AlbumViewHolder(ItemHomeContentPlaylistBinding.inflate(inflate, parent, false), mAlbumListener)
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder){
            is SongViewHolder -> holder.bind(listContent[position])
            is PlaylistViewHolder -> holder.bind(listContent[position])
            is AlbumViewHolder -> holder.bind(listContent[position])
        }
    }

    override fun getItemViewType(position: Int): Int {
        Log.d("TAG", "getItemViewType: ${listContent[position].playlistId}")
        val temp = listContent[position]
        return if (temp.playlistId != null && temp.videoId == null) {
            PLAYLIST
        } else if (temp.browseId != null && temp.videoId == null) {
            ALBUM
        }
        else{
            SONG
        }
    }

    override fun getItemCount(): Int {
        return listContent.size
    }

    companion object {
        private const val SONG = 1
        private const val PLAYLIST = 2
        private const val ALBUM = 3
    }
}