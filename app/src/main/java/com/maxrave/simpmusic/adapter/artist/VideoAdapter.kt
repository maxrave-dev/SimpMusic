package com.maxrave.simpmusic.adapter.artist

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.maxrave.simpmusic.data.model.browse.artist.ResultVideo
import com.maxrave.simpmusic.databinding.ItemVideoBinding
import com.maxrave.simpmusic.extension.connectArtists
import com.maxrave.simpmusic.extension.toListName

class VideoAdapter(private val videos: ArrayList<ResultVideo>): RecyclerView.Adapter<VideoAdapter.ViewHolder>() {
    private lateinit var mListener: OnItemClickListener

    interface OnItemClickListener{
        fun onItemClick(position: Int, type: String = "single")
    }

    fun setOnClickListener(listener: OnItemClickListener){
        mListener = listener
    }
    fun updateList(newList: ArrayList<ResultVideo>){
        videos.clear()
        videos.addAll(newList)
        notifyDataSetChanged()
    }
    inner class ViewHolder(val binding: ItemVideoBinding, listener: OnItemClickListener): RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                listener.onItemClick(bindingAdapterPosition)
            }
        }
        fun bind(video: ResultVideo){
            with(binding){
                tvVideoName.text = video.title
                tvArtistName.text = video.artists.toListName().connectArtists()
                tvViews.text = video.views
                tvVideoName.isSelected = true
                tvArtistName.isSelected = true
                tvViews.isSelected = true
                ivArt.load(video.thumbnails?.maxBy { it.width }?.url ?: video.thumbnails?.get(0)?.url )
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemVideoBinding.inflate(LayoutInflater.from(parent.context), parent, false), mListener)
    }

    override fun getItemCount(): Int {
        return videos.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(videos[position])
    }
    fun getCurrentList(): ArrayList<ResultVideo>{
        return videos
    }
}