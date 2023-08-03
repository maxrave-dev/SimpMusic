package com.maxrave.simpmusic.adapter.home

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.maxrave.simpmusic.data.model.explore.mood.Genre
import com.maxrave.simpmusic.databinding.ItemMoodsMomentGenreBinding
import kotlin.random.Random

class GenreAdapter(var genreList: ArrayList<Genre>): RecyclerView.Adapter<GenreAdapter.ViewHolder>() {
    private lateinit var mGenreListener: OnGenreItemClickListener
    interface OnGenreItemClickListener{
        fun onGenreItemClick(position: Int)
    }
    fun setOnGenreClickListener(listener: OnGenreItemClickListener){
        mGenreListener = listener
    }
    inner class ViewHolder(val binding: ItemMoodsMomentGenreBinding, val listener: OnGenreItemClickListener): RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {listener.onGenreItemClick(bindingAdapterPosition)}
        }
        fun bind(genre: Genre){
            with(binding){
                tvTitle.text = genre.title
                colorBackground.setBackgroundColor(generateRandomColor())
            }
        }
    }
    fun updateData(newData: List<Genre>){
        genreList.clear()
        genreList.addAll(newData)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMoodsMomentGenreBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding, mGenreListener)
    }

    override fun getItemCount(): Int {
        return genreList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        return holder.bind(genreList[position])
    }
    fun generateRandomColor(): Int {
        val red = Random.nextInt(256)
        val green = Random.nextInt(256)
        val blue = Random.nextInt(256)
        return Color.rgb(red, green, blue)
    }
}