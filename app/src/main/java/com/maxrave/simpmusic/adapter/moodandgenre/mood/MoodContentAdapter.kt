package com.maxrave.simpmusic.adapter.moodandgenre.mood

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.maxrave.simpmusic.data.model.explore.mood.moodmoments.Content
import com.maxrave.simpmusic.databinding.ItemHomeContentPlaylistBinding

class MoodContentAdapter(private var contentList: ArrayList<Content>): RecyclerView.Adapter<MoodContentAdapter.ViewHolder>() {
    inner class ViewHolder(val binding: ItemHomeContentPlaylistBinding, var listener: OnClickListener): RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                listener.onClick(bindingAdapterPosition)
            }
        }
    }
    interface OnClickListener{
        fun onClick(position: Int)
    }
    private lateinit var mListener: OnClickListener
    fun setOnClickListener(listener: OnClickListener){
        mListener = listener
    }
    fun updateData(newList: ArrayList<Content>){
        contentList.clear()
        contentList.addAll(newList)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemHomeContentPlaylistBinding.inflate(LayoutInflater.from(parent.context), parent, false), mListener)
    }

    override fun getItemCount(): Int {
        return contentList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val content = contentList[position]
        with(holder){
            binding.tvTitle.text = content.title
            binding.tvDescription.text = content.subtitle
            binding.tvTitle.isSelected = true
            binding.tvDescription.isSelected = true
            if (content.thumbnails != null) {
                binding.ivArt.load(content.thumbnails.last().url)
            }
        }
    }
}