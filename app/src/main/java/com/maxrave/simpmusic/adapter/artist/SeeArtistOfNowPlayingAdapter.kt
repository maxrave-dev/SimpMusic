package com.maxrave.simpmusic.adapter.artist

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.maxrave.simpmusic.data.model.searchResult.songs.Artist
import com.maxrave.simpmusic.databinding.ItemArtistInNowPlayingBinding

class SeeArtistOfNowPlayingAdapter(private val listArtist: List<Artist>): RecyclerView.Adapter<SeeArtistOfNowPlayingAdapter.ViewHolder>() {
    private lateinit var mListener: OnItemClickListener

    interface OnItemClickListener {
        fun onItemClick(position: Int)
    }

    fun setOnClickListener(listener: OnItemClickListener) {
        mListener = listener
    }
    inner class ViewHolder(val binding: ItemArtistInNowPlayingBinding, listener: OnItemClickListener): RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                if (bindingAdapterPosition != RecyclerView.NO_POSITION) {
                    listener.onItemClick(bindingAdapterPosition)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemArtistInNowPlayingBinding.inflate(LayoutInflater.from(parent.context), parent, false), mListener)
    }

    override fun getItemCount(): Int = listArtist.size

    override fun onBindViewHolder(holder: SeeArtistOfNowPlayingAdapter.ViewHolder, position: Int) {
        val artist = listArtist[position]
        with(holder.binding) {
            tvSeeArtists.text = artist.name
        }
    }
}