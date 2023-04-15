package com.maxrave.simpmusic.adapter.search

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.maxrave.simpmusic.databinding.ItemRecentlyQueryBinding

class SearchHistoryItemAdapter(private var searchHistory: ArrayList<String>): RecyclerView.Adapter<SearchHistoryItemAdapter.ViewHolder>() {

    private lateinit var mListener: onItemClickListener
    private lateinit var mDeleteListener: onDeleteClickListener
    interface onItemClickListener{
        fun onItemClick(position: Int)
    }
    interface onDeleteClickListener{
        fun onDeleteClick(position: Int)
    }
    fun setOnDeleteClickListener(listener: onDeleteClickListener){
        mDeleteListener = listener
    }
    fun setOnClickListener(listener: onItemClickListener){
        mListener = listener
    }
    fun getCurrentList(): ArrayList<String> = searchHistory
    inner class ViewHolder(val binding: ItemRecentlyQueryBinding, listener: onItemClickListener, deleteListener: onDeleteClickListener): RecyclerView.ViewHolder(binding.root){
        init {
            binding.rlRecentlyQuery.setOnClickListener {
                listener.onItemClick(bindingAdapterPosition)
            }
            binding.btDeleteQuery.setOnClickListener {
                deleteListener.onDeleteClick(bindingAdapterPosition)
            }
        }
    }
    fun updateData(newData: ArrayList<String>){
        searchHistory.clear()
        searchHistory.addAll(newData)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRecentlyQueryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding, mListener, mDeleteListener)
    }

    override fun getItemCount(): Int {
        return searchHistory.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val searchHistoryQuery = searchHistory[position]
        with(holder.binding){
            tvRecentlyQuery.text = searchHistoryQuery
        }
    }

}