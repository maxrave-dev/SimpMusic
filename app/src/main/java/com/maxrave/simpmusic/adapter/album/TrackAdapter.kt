package com.maxrave.simpmusic.adapter.album

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.maxrave.simpmusic.data.model.browse.album.Track
import com.maxrave.simpmusic.databinding.ItemAlbumTrackBinding


class TrackAdapter(private var trackList: ArrayList<Track>): RecyclerView.Adapter<TrackAdapter.ViewHolder>() {
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
    fun updateList(newList: ArrayList<Track>){
        trackList.clear()
        trackList.addAll(newList)
        notifyDataSetChanged()
    }
    fun getList(): ArrayList<Track> {
        return trackList
    }
    inner class ViewHolder(val binding: ItemAlbumTrackBinding, rootListener: OnItemClickListener, mOptionListener: OnOptionClickListener): RecyclerView.ViewHolder(binding.root) {
        init {
            binding.rootLayout.setOnClickListener {
                rootListener.onItemClick(bindingAdapterPosition)
            }
            binding.btMore.setOnClickListener {
                mOptionListener.onOptionClick(bindingAdapterPosition)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder = ViewHolder(ItemAlbumTrackBinding.inflate(
        LayoutInflater.from(parent.context), parent, false), mListener, optionListener)

    override fun getItemCount(): Int = trackList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val track = trackList[position]
        with(holder){
            binding.tvSongName.text = track.title
            var artistName = ""
            if (track.artists != null) {
                for (artist in track.artists) {
                    artistName += artist.name + ", "
                }
            }
            artistName = removeTrailingComma(artistName)
            artistName = removeComma(artistName)
            binding.tvArtistName.text = artistName
            binding.tvPosition.text = (position + 1).toString()
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

    fun getItem(position: Int): Track {
        return trackList[position]
    }
}