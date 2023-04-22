package com.maxrave.simpmusic.adapter.home

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.NavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.maxrave.simpmusic.R
import com.maxrave.simpmusic.data.model.home.Content
import com.maxrave.simpmusic.data.model.home.homeItem
import com.maxrave.simpmusic.databinding.ItemHomeBinding

class HomeItemAdapter(private var homeItemList: ArrayList<homeItem>, var context: Context, val navController: NavController): RecyclerView.Adapter<HomeItemAdapter.ViewHolder>() {
    inner class ViewHolder(var binding: ItemHomeBinding) : RecyclerView.ViewHolder(binding.root) {

    }
    fun updateData(newData: ArrayList<homeItem>){
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
        val layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        var tempContentList = homeItem.contents.toCollection(ArrayList<Content?>())
        tempContentList.removeIf { it == null }
        val itemAdapter = HomeItemContentAdapter(tempContentList as ArrayList<Content>)
        holder.binding.childRecyclerview.apply {
            this.adapter = itemAdapter
            this.layoutManager = layoutManager
        }
        itemAdapter.setOnSongClickListener(object : HomeItemContentAdapter.onSongItemClickListener{
            override fun onSongItemClick(position: Int) {
                Toast.makeText(context, homeItemList[holder.bindingAdapterPosition].contents[position].toString(), Toast.LENGTH_SHORT).show()
                val args = Bundle()
                args.putString("videoId", homeItemList[holder.bindingAdapterPosition].contents[position]?.videoId)
                args.putString("from", homeItem.title)
                navController.navigate(R.id.action_global_nowPlayingFragment, args)
            }
        })
        itemAdapter.setOnPlaylistClickListener(object : HomeItemContentAdapter.onPlaylistItemClickListener{
            override fun onPlaylistItemClick(position: Int) {
                Toast.makeText(context, homeItemList[position].toString(), Toast.LENGTH_SHORT).show()
                val args = Bundle()
                args.putString("id", homeItemList[holder.bindingAdapterPosition].contents[position]?.playlistId)
                navController.navigate(R.id.action_global_playlistFragment, args)
            }
        })
    }
}