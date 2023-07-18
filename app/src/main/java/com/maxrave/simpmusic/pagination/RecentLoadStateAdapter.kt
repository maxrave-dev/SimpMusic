package com.maxrave.simpmusic.pagination

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.paging.LoadState
import androidx.paging.LoadStateAdapter
import androidx.recyclerview.widget.RecyclerView
import com.maxrave.simpmusic.databinding.LoadStateViewBinding

class RecentLoadStateAdapter: LoadStateAdapter<RecentLoadStateAdapter.ViewHolder>() {
    inner class ViewHolder(val binding: LoadStateViewBinding): RecyclerView.ViewHolder(binding.root)

    override fun onBindViewHolder(holder: ViewHolder, loadState: LoadState) {
        holder.binding.apply {
            progress.visibility = if (loadState is LoadState.Loading) {
                View.VISIBLE
            } else {
                View.GONE
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, loadState: LoadState): ViewHolder {
        return ViewHolder(LoadStateViewBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }
}