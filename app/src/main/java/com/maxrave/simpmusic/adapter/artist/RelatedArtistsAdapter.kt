package com.maxrave.simpmusic.adapter.artist

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.maxrave.simpmusic.data.model.browse.artist.ResultRelated
import com.maxrave.simpmusic.databinding.ItemRelatedArtistBinding

class RelatedArtistsAdapter(private var relatedArtistsList: ArrayList<ResultRelated>, val context: Context): RecyclerView.Adapter<RelatedArtistsAdapter.ViewHolder>() {
    private lateinit var mListener: OnItemClickListener
    interface OnItemClickListener{
        fun onItemClick(position: Int)
    }
    fun setOnClickListener(listener: OnItemClickListener){
        mListener = listener
    }

    inner class ViewHolder(val binding: ItemRelatedArtistBinding, listener: OnItemClickListener): RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                listener.onItemClick(bindingAdapterPosition)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemRelatedArtistBinding.inflate(LayoutInflater.from(parent.context), parent, false), mListener)
    }

    override fun getItemCount(): Int {
        return relatedArtistsList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val relatedArtist = relatedArtistsList[position]
        with(holder){
            binding.tvArtistName.text = relatedArtist.title
            binding.tvArtistSubscribers.text = relatedArtist.subscribers
            binding.ivArtistArt.load(if (relatedArtist.thumbnails.size > 1) relatedArtist.thumbnails[1].url else relatedArtist.thumbnails[0].url)
        }
    }

    fun updateList(resultRelateds: ArrayList<ResultRelated>) {
        relatedArtistsList.clear()
        relatedArtistsList.addAll(resultRelateds)
        notifyDataSetChanged()
    }

    fun getItem(position: Int): ResultRelated {
        return relatedArtistsList[position]
    }
}