package com.maxrave.simpmusic.adapter.search

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.maxrave.simpmusic.databinding.ItemSuggestQueryBinding

class SuggestQueryAdapter(
    private var suggestQuery: ArrayList<String>,
) : RecyclerView.Adapter<SuggestQueryAdapter.ViewHolder>() {
    private lateinit var mListener: onItemClickListener
    private lateinit var mCopyListener: OnCopyClickListener

    interface onItemClickListener {
        fun onItemClick(position: Int)
    }

    interface OnCopyClickListener {
        fun onCopyClick(position: Int)
    }

    fun setOnClickListener(listener: onItemClickListener) {
        mListener = listener
    }

    fun setOnCopyClickListener(listener: OnCopyClickListener) {
        mCopyListener = listener
    }

    fun getCurrentList(): ArrayList<String> = suggestQuery

    inner class ViewHolder(
        val binding: ItemSuggestQueryBinding,
        listener: onItemClickListener,
        mCopyClickListener: OnCopyClickListener,
    ) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                listener.onItemClick(bindingAdapterPosition)
            }
            binding.btCopySuggestQuery.setOnClickListener {
                mCopyClickListener.onCopyClick(bindingAdapterPosition)
            }
        }
    }

    fun updateData(newData: ArrayList<String>) {
        suggestQuery.clear()
        suggestQuery.addAll(newData)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): ViewHolder {
        val binding = ItemSuggestQueryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding, mListener, mCopyListener)
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int,
    ) {
        val suggestQueryItem = suggestQuery[position]
        with(holder.binding) {
            tvSuggestQuery.text = suggestQueryItem
        }
    }

    override fun getItemCount(): Int = suggestQuery.size
}