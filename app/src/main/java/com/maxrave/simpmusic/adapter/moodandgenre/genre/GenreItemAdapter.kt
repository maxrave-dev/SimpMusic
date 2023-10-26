package com.maxrave.simpmusic.adapter.moodandgenre.genre

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.navigation.NavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.maxrave.simpmusic.R
import com.maxrave.simpmusic.data.model.explore.mood.genre.Content
import com.maxrave.simpmusic.data.model.explore.mood.genre.ItemsPlaylist
import com.maxrave.simpmusic.databinding.ItemMoodMomentPlaylistBinding
import com.maxrave.simpmusic.extension.navigateSafe

class GenreItemAdapter(private var genreList: ArrayList<ItemsPlaylist>, val context: Context, val navController: NavController): RecyclerView.Adapter<GenreItemAdapter.ViewHolder>() {
    inner class ViewHolder(val binding: ItemMoodMomentPlaylistBinding): RecyclerView.ViewHolder(binding.root)

    fun updateData(newList: ArrayList<ItemsPlaylist>){
        genreList.clear()
        genreList.addAll(newList)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemMoodMomentPlaylistBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun getItemCount(): Int {
    return genreList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val genre = genreList[position]
        with(holder){
            binding.tvTitle.text = genre.header
            val playlistContent: ArrayList<Content> = genre.contents as ArrayList<Content>
            val playlistAdapter = GenreContentAdapter(playlistContent)
            val linearLayoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            binding.childRecyclerview.apply {
                adapter = playlistAdapter
                layoutManager = linearLayoutManager
            }
            playlistAdapter.setOnClickListener(object : GenreContentAdapter.OnClickListener{
                override fun onClick(position: Int) {
                    val args = Bundle()
                    args.putString("id", playlistContent[position].playlistBrowseId)
                    navController.navigateSafe(R.id.action_global_playlistFragment, args)
                }
            })
        }
    }
}