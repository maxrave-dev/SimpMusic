package com.maxrave.simpmusic.adapter.podcast

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.maxrave.simpmusic.data.model.podcast.PodcastBrowse
import com.maxrave.simpmusic.databinding.ItemPodcastBinding

class PodcastAdapter(private var podcastList: ArrayList<PodcastBrowse.EpisodeItem>) :
    RecyclerView.Adapter<PodcastAdapter.ViewHolder>() {
    interface OnItemClickListener {
        fun onItemClick(position: Int)
    }

    private lateinit var mListener: OnItemClickListener

    fun setOnItemClickListener(onItemClickListener: OnItemClickListener) {
        mListener = onItemClickListener
    }

    inner class ViewHolder(val binding: ItemPodcastBinding, listener: OnItemClickListener) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                listener.onItemClick(bindingAdapterPosition)
            }
        }

        fun bind(podcast: PodcastBrowse.EpisodeItem) {
            with(binding) {
                tvTitle.text = podcast.title
                tvDescription.text = podcast.description
                tvDateAndLength.text = "${podcast.createdDay} • ${podcast.durationString}"
                ivThumbnail.load(podcast.thumbnail.lastOrNull()?.url)
                btMore.visibility = View.GONE
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPodcastBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding, mListener)
    }

    override fun getItemCount(): Int {
        return podcastList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(podcastList[position])
    }

    fun updateData(newPodcastList: ArrayList<PodcastBrowse.EpisodeItem>) {
        podcastList = newPodcastList
        notifyDataSetChanged()
    }
}