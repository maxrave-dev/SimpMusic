package com.maxrave.simpmusic.adapter.home

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.NavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.maxrave.simpmusic.R
import com.maxrave.simpmusic.common.Config
import com.maxrave.simpmusic.data.model.browse.album.Track
import com.maxrave.simpmusic.data.model.home.Content
import com.maxrave.simpmusic.data.model.home.HomeItem
import com.maxrave.simpmusic.data.queue.Queue
import com.maxrave.simpmusic.databinding.ItemHomeBinding
import com.maxrave.simpmusic.extension.navigateSafe
import com.maxrave.simpmusic.extension.toTrack

class HomeItemAdapter(private var homeItemList: ArrayList<HomeItem>, var context: Context, val navController: NavController): RecyclerView.Adapter<HomeItemAdapter.ViewHolder>() {
    inner class ViewHolder(var binding: ItemHomeBinding) : RecyclerView.ViewHolder(binding.root)

    fun updateData(newData: ArrayList<HomeItem>){
        homeItemList.clear()
        homeItemList.addAll(newData)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHomeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return homeItemList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val homeItem = homeItemList[position]
        holder.binding.tvTitle.text = homeItem.title
        if (homeItem.subtitle == null) {
            holder.binding.tvSubtitle.visibility = View.GONE
            holder.binding.ivArtist.visibility = View.GONE
        } else {
            holder.binding.tvSubtitle.text = homeItem.subtitle
            holder.binding.tvSubtitle.visibility = View.VISIBLE
        }
        if (!homeItem.thumbnail.isNullOrEmpty()) {
            holder.binding.ivArtist.visibility = View.VISIBLE
            holder.binding.ivArtist.load(homeItem.thumbnail.lastOrNull()?.url)
        } else {
            holder.binding.ivArtist.visibility = View.GONE
        }
        if (homeItem.channelId != null) {
            holder.binding.tvTitle.isClickable = true
            holder.binding.tvTitle.isFocusable = true
            val attr =
                context.obtainStyledAttributes(intArrayOf(android.R.attr.selectableItemBackgroundBorderless))
            holder.binding.tvTitle.foreground = attr.getDrawable(0)
            attr.recycle()
            holder.binding.tvTitle.setOnClickListener {
                val args = Bundle()
                args.putString("channelId", homeItem.channelId)
                navController.navigateSafe(R.id.action_global_artistFragment, args)
            }
        }
        val layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        val tempContentList = homeItem.contents.toCollection(ArrayList())
        tempContentList.removeIf { it == null }
        val itemAdapter = HomeItemContentAdapter(tempContentList as ArrayList<Content>, context)
        holder.binding.childRecyclerview.apply {
            this.adapter = itemAdapter
            this.layoutManager = layoutManager
        }
        itemAdapter.setOnSongClickListener(object : HomeItemContentAdapter.onSongItemClickListener {
            override fun onSongItemClick(position: Int) {
                val args = Bundle()
                args.putString("videoId", homeItemList[holder.bindingAdapterPosition].contents[position]?.videoId)
                args.putString("from", homeItem.title)
                Queue.clear()
                Log.d("HomeItemAdapter", "onSongItemClick: ${homeItemList[holder.bindingAdapterPosition].contents[position]}")
                val firstQueue: Track = homeItemList[holder.bindingAdapterPosition].contents[position]!!.toTrack()
                Queue.setNowPlaying(firstQueue)
                args.putString("type", Config.SONG_CLICK)
                navController.navigateSafe(R.id.action_global_nowPlayingFragment, args)
            }
        })
        itemAdapter.setOnPlaylistClickListener(object : HomeItemContentAdapter.onPlaylistItemClickListener{
            override fun onPlaylistItemClick(position: Int) {
                val args = Bundle()
                Log.d("HomeItemAdapter", "onPlaylistItemClick: ${homeItemList[holder.bindingAdapterPosition].contents[position]?.playlistId}")
                args.putString("id", homeItemList[holder.bindingAdapterPosition].contents[position]?.playlistId)
                navController.navigateSafe(R.id.action_global_playlistFragment, args)
            }
        })
        itemAdapter.setOnAlbumClickListener(object : HomeItemContentAdapter.onAlbumItemClickListener{
            override fun onAlbumItemClick(position: Int) {
                val args = Bundle()
                Log.d("HomeItemAdapter", "onAlbumItemClick: ${homeItemList[holder.bindingAdapterPosition].contents[position]?.browseId}")
                args.putString("browseId", homeItemList[holder.bindingAdapterPosition].contents[position]?.browseId)
                navController.navigateSafe(R.id.action_global_albumFragment, args)
            }
        })
        itemAdapter.setOnArtistClickListener(object : HomeItemContentAdapter.onArtistItemClickListener {
            override fun onArtistItemClick(position: Int) {
                val args = Bundle()
                val channelId = homeItemList[holder.bindingAdapterPosition].contents[position]?.browseId ?: homeItemList[holder.bindingAdapterPosition].contents[position]?.playlistId
                Log.d("HomeItemAdapter", "onArtistItemClick: $channelId")
                args.putString("channelId", channelId)
                navController.navigateSafe(R.id.action_global_artistFragment, args)
            }
        })
    }
}