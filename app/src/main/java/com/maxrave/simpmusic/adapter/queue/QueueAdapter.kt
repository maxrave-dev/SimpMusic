package com.maxrave.simpmusic.adapter.queue

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.maxrave.simpmusic.data.model.browse.album.Track
import com.maxrave.simpmusic.databinding.ItemAlbumTrackBinding

class QueueAdapter(private val listTrack: ArrayList<Track>): RecyclerView.Adapter<QueueAdapter.QueueViewHolder>() {
    private lateinit var mListener: OnItemClickListener
    interface OnItemClickListener{
        fun onItemClick(position: Int)
    }
    fun setOnClickListener(listener: OnItemClickListener){
        mListener = listener
    }


     inner class QueueViewHolder(val binding: ItemAlbumTrackBinding, listener: OnItemClickListener): RecyclerView.ViewHolder(binding.root) {
         init {
             binding.root.setOnClickListener {
                 listener.onItemClick(bindingAdapterPosition)
             }
         }
    }

    fun updateList(tracks: ArrayList<Track>) {
        listTrack.clear()
        listTrack.addAll(tracks)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QueueViewHolder {
        return QueueViewHolder(ItemAlbumTrackBinding.inflate(LayoutInflater.from(parent.context), parent, false), mListener)
    }

    override fun getItemCount(): Int {
        return listTrack.size
    }

    override fun onBindViewHolder(holder: QueueViewHolder, position: Int) {
        val track = listTrack[position]
        with(holder){
            binding.tvPosition.text = (position + 1).toString()
            binding.tvSongName.text = track.title
            binding.tvSongName.isSelected = true
            var artist = ""
            for (i in track.artists!!.indices){
                artist += if (i == track.artists.size - 1) track.artists[i].name else "${track.artists[i].name}, "
            }
            binding.tvArtistName.text = artist
        }
    }
}