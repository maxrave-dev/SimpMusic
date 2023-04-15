package com.maxrave.simpmusic.adapter.artist

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.maxrave.simpmusic.data.model.browse.artist.ResultAlbum
import com.maxrave.simpmusic.databinding.ItemSinglesBinding

class AlbumsAdapter(private var albumsList: ArrayList<ResultAlbum>): RecyclerView.Adapter<AlbumsAdapter.ViewHolder>() {
    private lateinit var mListener: OnItemClickListener
    interface OnItemClickListener{
        fun onItemClick(position: Int, type: String = "album")
    }

    fun setOnClickListener(listener: OnItemClickListener){
        mListener = listener
    }


    inner class ViewHolder(val binding: ItemSinglesBinding, listener: OnItemClickListener): RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                listener.onItemClick(bindingAdapterPosition)
            }
        }
    }

    fun updateList(newList: ArrayList<ResultAlbum>){
        albumsList.clear()
        albumsList.addAll(newList)
        notifyDataSetChanged()
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemSinglesBinding.inflate(LayoutInflater.from(parent.context), parent, false), mListener)
    }

    override fun getItemCount(): Int {
        return albumsList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val album = albumsList[position]
        with(holder.binding){
            tvAlbumName.text = album.title
            tvAlbumYear.text = album.year.toString()
            ivAlbumArt.load(if (album.thumbnails.size > 1) album.thumbnails[1].url else album.thumbnails[0].url)
        }
    }
    fun getItem(position: Int): ResultAlbum {
        return albumsList[position]
    }
}