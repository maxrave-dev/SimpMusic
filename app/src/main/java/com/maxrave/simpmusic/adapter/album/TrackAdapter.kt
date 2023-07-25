package com.maxrave.simpmusic.adapter.album

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.maxrave.simpmusic.data.db.entities.SongEntity
import com.maxrave.simpmusic.data.model.browse.album.Track
import com.maxrave.simpmusic.databinding.ItemAlbumTrackBinding
import com.maxrave.simpmusic.extension.connectArtists
import com.maxrave.simpmusic.extension.toListName
import com.maxrave.simpmusic.extension.toTrack


class TrackAdapter(private var trackList: ArrayList<Any>): RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private lateinit var mListener: OnItemClickListener
    interface OnItemClickListener{
        fun onItemClick(position: Int)
    }
    fun setOnClickListener(listener: OnItemClickListener){
        mListener = listener
    }
    fun updateList(newList: ArrayList<Any>){
        trackList.clear()
        trackList.addAll(newList)
        notifyDataSetChanged()
    }
    inner class TrackViewHolder(val binding: ItemAlbumTrackBinding, rootListener: OnItemClickListener): RecyclerView.ViewHolder(binding.root) {
        init {
            binding.rootLayout.setOnClickListener {
                rootListener.onItemClick(bindingAdapterPosition)
            }
        }
        fun bind(track: Track){
            with(binding){
                tvSongName.text = track.title
                tvArtistName.text = track.artists.toListName().connectArtists()
                tvPosition.text = (bindingAdapterPosition + 1).toString()
            }
        }
    }
    inner class SongEntityViewHolder(val binding: ItemAlbumTrackBinding, rootListener: OnItemClickListener): RecyclerView.ViewHolder(binding.root) {
        init {
            binding.rootLayout.setOnClickListener {
                rootListener.onItemClick(bindingAdapterPosition)
            }
        }
        fun bind(songEntity: SongEntity){
            with(binding){
                tvSongName.text = songEntity.title
                tvArtistName.text = songEntity.artistName?.connectArtists()
                tvPosition.text = (bindingAdapterPosition + 1).toString()
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_TRACK -> {
                TrackViewHolder(ItemAlbumTrackBinding.inflate(LayoutInflater.from(parent.context), parent, false), mListener)
            }
            TYPE_SONG_ENTITY -> {
                SongEntityViewHolder(ItemAlbumTrackBinding.inflate(LayoutInflater.from(parent.context), parent, false), mListener)
            }
            else -> {
                throw IllegalArgumentException("Invalid type of data $viewType")
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is TrackViewHolder -> {
                holder.bind(trackList[position] as Track)
            }
            is SongEntityViewHolder -> {
                holder.bind(trackList[position] as SongEntity)
            }
            else -> {
                throw IllegalArgumentException("Invalid type of data $position")
            }
        }
    }

    override fun getItemCount(): Int = trackList.size

    override fun getItemViewType(position: Int): Int {
        return when (trackList[position]) {
            is Track -> TYPE_TRACK
            is SongEntity -> TYPE_SONG_ENTITY
            else -> {
                throw IllegalArgumentException("Invalid type of data $position")
            }
        }
    }

    fun getList(): ArrayList<Track> {
        val temp = arrayListOf<Track>()
        for (i in trackList) {
            if (i is Track) {
                temp.add(i)
            }
            else if (i is SongEntity) {
                temp.add(i.toTrack())
            }
        }
        return temp
    }

    companion object {
        private const val TYPE_TRACK = 0
        private const val TYPE_SONG_ENTITY = 1
    }
}