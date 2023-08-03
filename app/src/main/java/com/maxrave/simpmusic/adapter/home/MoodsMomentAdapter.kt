package com.maxrave.simpmusic.adapter.home

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.maxrave.simpmusic.data.model.explore.mood.MoodsMoment
import com.maxrave.simpmusic.databinding.ItemMoodsMomentGenreBinding
import kotlin.random.Random

class MoodsMomentAdapter(val moodsMomentList: ArrayList<MoodsMoment>): RecyclerView.Adapter<MoodsMomentAdapter.ViewHolder>() {
    private lateinit var mMoodsMomentListener: OnMoodsMomentItemClickListener
    interface OnMoodsMomentItemClickListener{
        fun onMoodsMomentItemClick(position: Int)
    }
    fun setOnMoodsMomentClickListener(listener: OnMoodsMomentItemClickListener){
        mMoodsMomentListener = listener
    }
    inner class ViewHolder(val binding: ItemMoodsMomentGenreBinding, var listener: OnMoodsMomentItemClickListener): RecyclerView.ViewHolder(binding.root){
        init {
            binding.root.setOnClickListener {listener.onMoodsMomentItemClick(bindingAdapterPosition)}
        }
        fun bind(moodsMoment: MoodsMoment){
            with(binding){
                tvTitle.text = moodsMoment.title
                colorBackground.setBackgroundColor(generateRandomColor())
            }
        }
    }
    fun updateData(newData: List<MoodsMoment>){
        moodsMomentList.clear()
        moodsMomentList.addAll(newData)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMoodsMomentGenreBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding, mMoodsMomentListener)
    }

    override fun getItemCount(): Int {
        return moodsMomentList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        return holder.bind(moodsMomentList[position])
    }
    fun generateRandomColor(): Int {
        val red = Random.nextInt(256)
        val green = Random.nextInt(256)
        val blue = Random.nextInt(256)
        return Color.rgb(red, green, blue)
    }
}