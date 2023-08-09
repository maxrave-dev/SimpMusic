package com.maxrave.simpmusic.adapter.artist

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.maxrave.simpmusic.data.model.browse.artist.ResultSong
import com.maxrave.simpmusic.databinding.ItemPopularSongBinding
import com.maxrave.simpmusic.extension.connectArtists
import com.maxrave.simpmusic.extension.toListName

class PopularAdapter(private var popularList: ArrayList<ResultSong>): RecyclerView.Adapter<PopularAdapter.ViewHolder>() {
    private lateinit var mListener: OnItemClickListener
    private lateinit var mOptionsListener: OnOptionsClickListener
    interface OnItemClickListener{
        fun onItemClick(position: Int, type: String = "song")
    }
    interface OnOptionsClickListener{
        fun onOptionsClick(position: Int)
    }

    fun setOnClickListener(listener: OnItemClickListener){
        mListener = listener
    }
    fun setOnOptionsClickListener(listener: OnOptionsClickListener){
        mOptionsListener = listener
    }
    fun getCurrentList(): ArrayList<ResultSong> {
        return popularList
    }

    inner class ViewHolder(val binding: ItemPopularSongBinding, listener: OnItemClickListener, optionListener: OnOptionsClickListener): RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                listener.onItemClick(bindingAdapterPosition)
            }
            binding.btMore.setOnClickListener {
                optionListener.onOptionsClick(bindingAdapterPosition)
            }
        }
    }
    fun updateList(newList: ArrayList<ResultSong>){
        popularList.clear()
        popularList.addAll(newList)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemPopularSongBinding.inflate(LayoutInflater.from(parent.context), parent, false), mListener, mOptionsListener)
    }

    override fun getItemCount(): Int {
        return popularList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val song = popularList[position]
        with(holder.binding){
            tvSongTitle.text = song.title
            val artistName = song.artists.toListName().connectArtists()
            tvSongArtist.text = artistName
            tvSongTitle.isSelected = true
            tvSongArtist.isSelected = true
            if (song.thumbnails.size > 1){
                ivThumbnail.load(song.thumbnails[1].url)}
            else{
                ivThumbnail.load(song.thumbnails[0].url)}
        }
    }

    fun getItem(position: Int): ResultSong {
        return popularList[position]
    }
}