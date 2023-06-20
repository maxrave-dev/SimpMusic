package com.maxrave.simpmusic.adapter.home.chart

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.maxrave.simpmusic.R
import com.maxrave.simpmusic.data.model.browse.album.Track
import com.maxrave.simpmusic.data.model.home.chart.ItemVideo
import com.maxrave.simpmusic.databinding.ItemTrackChartBinding

class TrackChartAdapter(private var trackList: ArrayList<ItemVideo>, val context: Context): RecyclerView.Adapter<TrackChartAdapter.ViewHolder>() {
    inner class ViewHolder(val binding: ItemTrackChartBinding, listener: setOnItemClickListener): RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {listener.onItemClick(bindingAdapterPosition)}
        }
    }
    fun updateData(newData: ArrayList<ItemVideo>){
        trackList.clear()
        trackList.addAll(newData)
        notifyDataSetChanged()
    }
    interface setOnItemClickListener {
        fun onItemClick(position: Int)
    }
    private lateinit var mTrackListener: setOnItemClickListener
    fun setOnItemClickListener(listener: setOnItemClickListener){
        mTrackListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder = ViewHolder(ItemTrackChartBinding.inflate(
        LayoutInflater.from(parent.context), parent, false), mTrackListener)

    override fun getItemCount(): Int = trackList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val track = trackList[position]
        with(holder){
            binding.tvView.text = context.getString(R.string.view_count, track.views)
            binding.tvRank.text = (position + 1).toString()
            binding.tvTitle.text = track.title
            var artistName = ""
            if (track.artists != null) {
                for (artist in track.artists) {
                    artistName += artist.name + ", "
                }
            }
            artistName = removeTrailingComma(artistName)
            artistName = removeComma(artistName)
            binding.tvArtistName.text = artistName
            binding.ivArt.load(track.thumbnails.last().url)
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

    fun getItem(position: Int): ItemVideo {
        return trackList[position]
    }
}