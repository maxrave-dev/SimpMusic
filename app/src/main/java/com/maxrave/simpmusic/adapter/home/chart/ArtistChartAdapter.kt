package com.maxrave.simpmusic.adapter.home.chart

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.maxrave.simpmusic.R
import com.maxrave.simpmusic.data.model.home.chart.ItemArtist
import com.maxrave.simpmusic.databinding.ItemArtistChartBinding

class ArtistChartAdapter(var listArtist: ArrayList<ItemArtist>, val context: Context) : RecyclerView.Adapter<ArtistChartAdapter.ViewHolder>() {
    interface OnArtistItemClickListener {
        fun onArtistItemClick(position: Int)
    }

    private lateinit var mArtistListener: OnArtistItemClickListener
    fun setOnArtistClickListener(listener: OnArtistItemClickListener) {
        mArtistListener = listener
    }

    inner class ViewHolder(val binding: ItemArtistChartBinding, listener: OnArtistItemClickListener) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener { listener.onArtistItemClick(bindingAdapterPosition) }
        }
    }

    fun updateData(newData: List<ItemArtist>) {
        listArtist.clear()
        listArtist.addAll(newData)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder = ViewHolder(ItemArtistChartBinding.inflate(LayoutInflater.from(parent.context), parent, false), mArtistListener)

    override fun getItemCount(): Int = listArtist.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val artist = listArtist[position]
        with(holder) {
            binding.tvRank.text = artist.rank.toString()
            binding.tvArtistName.text = artist.title
            binding.tvSubscribers.text = if (artist.subscribers.contains(context.getString(R.string.subscribers))) artist.subscribers else context.getString(R.string.subscribers, artist.subscribers)
            binding.ivArt.load(artist.thumbnails.last().url)
        }
    }
}