package com.maxrave.simpmusic.adapter.artist

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.maxrave.simpmusic.data.model.browse.artist.ResultSingle
import com.maxrave.simpmusic.databinding.ItemSinglesBinding

class SinglesAdapter(private var singleList: ArrayList<ResultSingle>): RecyclerView.Adapter<SinglesAdapter.ViewHolder>() {
    private lateinit var mListener: OnItemClickListener

    interface OnItemClickListener{
        fun onItemClick(position: Int, type: String = "single")
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

    fun updateList(newList: ArrayList<ResultSingle>){
        singleList.clear()
        singleList.addAll(newList)
        notifyDataSetChanged()
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemSinglesBinding.inflate(LayoutInflater.from(parent.context), parent, false), mListener)
    }

    override fun getItemCount(): Int {
        return singleList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val single = singleList[position]
        with(holder.binding){
            tvAlbumName.text = single.title
            tvAlbumYear.text = single.year.toString()
            ivAlbumArt.load(if (single.thumbnails.size > 1) single.thumbnails[1].url else single.thumbnails[0].url)
        }
    }

    fun getItem(position: Int): ResultSingle {
        return singleList[position]
    }
}