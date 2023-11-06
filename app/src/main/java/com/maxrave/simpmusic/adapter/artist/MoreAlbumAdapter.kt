package com.maxrave.simpmusic.adapter.artist

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.maxrave.kotlinytmusicscraper.models.AlbumItem
import com.maxrave.kotlinytmusicscraper.models.YTItem
import com.maxrave.simpmusic.databinding.ItemSinglesBinding

class MoreAlbumAdapter(private var albumList: ArrayList<YTItem>): RecyclerView.Adapter<MoreAlbumAdapter.ViewHolder>() {
    interface OnItemClickListener {
        fun onItemClick(position: Int, type: String = "album")
    }
    private lateinit var mListener: OnItemClickListener
    fun setOnClickListener(listener: OnItemClickListener) {
        mListener = listener
    }

    inner class ViewHolder(private val binding: ItemSinglesBinding, listener: OnItemClickListener): RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                listener.onItemClick(bindingAdapterPosition)
            }
        }
        fun bind(item: YTItem) {
            with(binding) {
                if (item is AlbumItem) {
                    tvAlbumName.text = item.title
                    tvAlbumYear.text = item.year.toString()
                    ivAlbumArt.load(item.thumbnail)
                }
            }
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemSinglesBinding.inflate(LayoutInflater.from(parent.context), parent, false), mListener)
    }

    override fun getItemCount(): Int {
        return albumList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val album = albumList[position]
        holder.bind(album)
    }

    fun updateList(listAlbum: ArrayList<YTItem>) {
        albumList = listAlbum
        Log.w("MoreAlbumAdapter", "updateList: $albumList")
        notifyDataSetChanged()
    }
}