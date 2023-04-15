package com.maxrave.simpmusic.adapter.search

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.maxrave.simpmusic.databinding.ItemSuggestQueryBinding

class SuggestQueryAdapter(private var suggestQuery: ArrayList<String>): RecyclerView.Adapter<SuggestQueryAdapter.ViewHolder>() {
    private lateinit var mListener: onItemClickListener
    interface onItemClickListener{
        fun onItemClick(position: Int)
    }
    fun setOnClickListener(listener: onItemClickListener){
        mListener = listener
    }
    inner class ViewHolder(val binding: ItemSuggestQueryBinding, listener: onItemClickListener) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                listener.onItemClick(bindingAdapterPosition)
            }
        }
    }
    fun updateData(newData: ArrayList<String>){
        suggestQuery.clear()
        suggestQuery.addAll(newData)
        notifyDataSetChanged()
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSuggestQueryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding, mListener)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val suggestQueryItem = suggestQuery[position]
        with(holder.binding){
            tvSuggestQuery.text = suggestQueryItem
        }
    }

    override fun getItemCount(): Int {
        return suggestQuery.size
    }
}
