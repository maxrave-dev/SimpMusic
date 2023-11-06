package com.maxrave.simpmusic.adapter.playlist

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.maxrave.simpmusic.data.db.entities.LocalPlaylistEntity
import com.maxrave.simpmusic.databinding.ItemLocalPlaylistBinding

class AddToAPlaylistAdapter(private val list: ArrayList<LocalPlaylistEntity>): RecyclerView.Adapter<AddToAPlaylistAdapter.ViewHolder>() {

    lateinit var mListener: OnItemClickListener

    interface OnItemClickListener {
        fun onItemClick(position: Int)
    }

    fun setOnItemClickListener(listener: OnItemClickListener) {
        mListener = listener
    }

    inner class ViewHolder(val binding: ItemLocalPlaylistBinding, val listener: OnItemClickListener): RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                listener.onItemClick(bindingAdapterPosition)
            }
        }

        fun bind(item: LocalPlaylistEntity) {
            binding.tvLocalPlaylistTitle.text = item.title
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemLocalPlaylistBinding.inflate(LayoutInflater.from(parent.context), parent, false), mListener)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        return holder.bind(list[position])
    }

    fun updateList(newList: List<LocalPlaylistEntity>) {
        list.clear()
        list.addAll(newList)
        notifyDataSetChanged()
    }
}