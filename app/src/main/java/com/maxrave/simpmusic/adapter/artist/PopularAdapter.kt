package com.maxrave.simpmusic.adapter.artist

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.maxrave.simpmusic.data.model.browse.artist.ResultSong
import com.maxrave.simpmusic.databinding.ItemPopularSongBinding

class PopularAdapter(private var popularList: ArrayList<ResultSong>): RecyclerView.Adapter<PopularAdapter.ViewHolder>() {
    private lateinit var mListener: OnItemClickListener
    interface OnItemClickListener{
        fun onItemClick(position: Int, type: String = "song")
    }

    fun setOnClickListener(listener: OnItemClickListener){
        mListener = listener
    }
    fun getCurrentList(): ArrayList<ResultSong> {
        return popularList
    }

    inner class ViewHolder(val binding: ItemPopularSongBinding, listener: OnItemClickListener): RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                listener.onItemClick(bindingAdapterPosition)
            }
        }
    }
    fun updateList(newList: ArrayList<ResultSong>){
        popularList.clear()
        popularList.addAll(newList)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemPopularSongBinding.inflate(LayoutInflater.from(parent.context), parent, false), mListener)
    }

    override fun getItemCount(): Int {
        return popularList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val song = popularList[position]
        with(holder.binding){
            tvSongTitle.text = song.title
            var artistName = ""
            if (song.artists != null) {
                for (artist in song.artists) {
                    artistName += artist.name + ", "
                }
            }
            artistName = removeTrailingComma(artistName)
            artistName = removeComma(artistName)
            tvSongArtist.text = artistName
            if (song.thumbnails.size > 1){
                ivThumbnail.load(song.thumbnails[1].url)}
            else{
                ivThumbnail.load(song.thumbnails[0].url)}
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

    fun getItem(position: Int): ResultSong {
        return popularList[position]
    }
}