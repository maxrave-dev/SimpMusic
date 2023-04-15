package com.maxrave.simpmusic.adapter.playlist

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.maxrave.simpmusic.data.model.browse.album.Track
import com.maxrave.simpmusic.data.model.browse.playlist.TrackPlaylist
import com.maxrave.simpmusic.databinding.ItemPopularSongBinding

class PlaylistItemAdapter(private var playlistItemList: ArrayList<TrackPlaylist>): RecyclerView.Adapter<PlaylistItemAdapter.ViewHolder>() {
    private lateinit var mListener: OnItemClickListener
    private lateinit var optionListener: OnOptionClickListener
    interface OnItemClickListener{
        fun onItemClick(position: Int)
    }
    interface OnOptionClickListener{
        fun onOptionClick(position: Int)
    }
    fun setOnClickListener(listener: OnItemClickListener){
        mListener = listener
    }
    fun setOnOptionClickListener(listener: OnOptionClickListener){
        optionListener = listener
    }
    inner class ViewHolder(val binding: ItemPopularSongBinding, rootListener: OnItemClickListener, mOptionListener: OnOptionClickListener): RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                rootListener.onItemClick(bindingAdapterPosition)
            }
            binding.btMore.setOnClickListener {
                mOptionListener.onOptionClick(bindingAdapterPosition)
            }
        }
    }
    fun updateList(newList: ArrayList<TrackPlaylist>){
        playlistItemList.clear()
        playlistItemList.addAll(newList)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder = ViewHolder(
        ItemPopularSongBinding.inflate(LayoutInflater.from(parent.context), parent, false), mListener, optionListener)

    override fun getItemCount(): Int = playlistItemList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val track = playlistItemList[position]
        with(holder){
            binding.tvSongTitle.text = track.title
            var artistName = ""
            if (track.artistPlaylists != null) {
                for (artist in track.artistPlaylists) {
                    artistName += artist.name + ", "
                }
            }
            artistName = removeTrailingComma(artistName)
            artistName = removeComma(artistName)
            binding.tvSongArtist.text = artistName
            binding.ivThumbnail.load(track.thumbnails.last().url)
        }
    }
    private fun removeTrailingComma(sentence: String): String {
        val trimmed = sentence.trimEnd()
        return if (trimmed.endsWith(", ")) {
            trimmed.dropLast(2)
        } else {
            trimmed
        }
    }


    private fun removeComma(string: String): String {
        return if (string.endsWith(',')) {
            string.substring(0, string.length - 1)
        } else {
            string
        }
    }

    fun getItem(position: Int): TrackPlaylist {
        return playlistItemList[position]
    }
}