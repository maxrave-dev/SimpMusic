package com.maxrave.simpmusic.adapter.home

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.maxrave.simpmusic.data.model.home.Content
import com.maxrave.simpmusic.databinding.ItemHomeContentPlaylistBinding
import com.maxrave.simpmusic.databinding.ItemHomeContentSongBinding

class HomeItemContentAdapter(private var listContent: ArrayList<Content>): RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private lateinit var mPlaylistListener: onPlaylistItemClickListener
    private lateinit var mSongListener: onSongItemClickListener
    interface onSongItemClickListener{
        fun onSongItemClick(position: Int)
    }
    interface onPlaylistItemClickListener{
        fun onPlaylistItemClick(position: Int)
    }
    fun setOnSongClickListener(listener: onSongItemClickListener){
        mSongListener = listener
    }
    fun setOnPlaylistClickListener(listener: onPlaylistItemClickListener){
        mPlaylistListener = listener
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
                var artistName = ""
                if (content.artists != null) {
                    for (artist in content.artists) {
                        artistName += artist.name + ", "
                    }
                    artistName = removeTrailingComma(artistName)
                    artistName = removeComma(artistName)
                }
                tvArtistName.text = artistName
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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflate = LayoutInflater.from(parent.context)
        return when (viewType) {
            SONG -> SongViewHolder(ItemHomeContentSongBinding.inflate(inflate, parent, false), mSongListener)
            PLAYLIST -> PlaylistViewHolder(ItemHomeContentPlaylistBinding.inflate(inflate, parent, false), mPlaylistListener)
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder){
            is SongViewHolder -> holder.bind(listContent[position])
            is PlaylistViewHolder -> holder.bind(listContent[position])
        }
    }

    override fun getItemViewType(position: Int): Int {
        Log.d("TAG", "getItemViewType: ${listContent[position].playlistId}")
        val temp = listContent[position]
        return if (temp.playlistId != null && temp.videoId == null) {
            PLAYLIST
        } else {
            SONG
        }
    }

    override fun getItemCount(): Int {
        return listContent.size
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
        return if (string.endsWith(',')) {
            string.substring(0, string.length - 1)
        } else {
            string
        }
    }

    companion object {
        private const val SONG = 1
        private const val PLAYLIST = 2
    }
}