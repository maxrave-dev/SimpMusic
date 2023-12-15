package com.maxrave.simpmusic.adapter.home

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.maxrave.simpmusic.R
import com.maxrave.simpmusic.data.model.home.Content
import com.maxrave.simpmusic.databinding.ItemHomeContentArtistBinding
import com.maxrave.simpmusic.databinding.ItemHomeContentPlaylistBinding
import com.maxrave.simpmusic.databinding.ItemHomeContentSongBinding
import com.maxrave.simpmusic.databinding.ItemHomeContentVideoBinding
import com.maxrave.simpmusic.extension.connectArtists
import com.maxrave.simpmusic.extension.toListName

class HomeItemContentAdapter(private var listContent: ArrayList<Content>, private val context: Context): RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private lateinit var mPlaylistListener: onPlaylistItemClickListener
    private lateinit var mAlbumListener: onAlbumItemClickListener
    private lateinit var mSongListener: onSongItemClickListener
    private lateinit var mArtistListener: onArtistItemClickListener
    interface onSongItemClickListener{
        fun onSongItemClick(position: Int)
    }
    interface onPlaylistItemClickListener{
        fun onPlaylistItemClick(position: Int)
    }
    interface onAlbumItemClickListener{
        fun onAlbumItemClick(position: Int)
    }
    interface onArtistItemClickListener{
        fun onArtistItemClick(position: Int)
    }
    fun setOnSongClickListener(listener: onSongItemClickListener){
        mSongListener = listener
    }
    fun setOnPlaylistClickListener(listener: onPlaylistItemClickListener){
        mPlaylistListener = listener
    }
    fun setOnAlbumClickListener(listener: onAlbumItemClickListener){
        mAlbumListener = listener
    }
    fun setOnArtistClickListener(listener: onArtistItemClickListener){
        mArtistListener = listener
    }
    inner class SongViewHolder(var binding: ItemHomeContentSongBinding, var listener: onSongItemClickListener): RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {listener.onSongItemClick(bindingAdapterPosition)}
        }
        fun bind(content: Content){
            with(binding){
                if (content.thumbnails.size > 1) {
                    ivArt.load(content.thumbnails[1].url)
                }
                else{
                    ivArt.load(content.thumbnails[0].url)
                }
                tvSongName.text = content.title
                tvSongName.isSelected = true
                tvArtistName.text = content.artists.toListName().firstOrNull()
                tvArtistName.isSelected = true
                tvAlbumName.text = content.album?.name ?: context.getString(R.string.songs)
                tvAlbumName.isSelected = true
            }
        }
    }

    inner class VideoViewHolder(
        var binding: ItemHomeContentVideoBinding,
        var listener: onSongItemClickListener
    ) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener { listener.onSongItemClick(bindingAdapterPosition) }
        }

        fun bind(content: Content) {
            with(binding) {
                if (content.thumbnails.isNotEmpty()) {
                    ivArt.load(content.thumbnails.lastOrNull()?.url)
                }
                tvSongName.text = content.title
                tvSongName.isSelected = true
                tvArtistName.text = content.artists.toListName().firstOrNull()
                tvArtistName.isSelected = true
                tvAlbumName.text = content.album?.name ?: context.getString(R.string.videos)
                tvAlbumName.isSelected = true
            }
        }
    }

    inner class PlaylistViewHolder(
        var binding: ItemHomeContentPlaylistBinding,
        var listener: onPlaylistItemClickListener
    ) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener { listener.onPlaylistItemClick(bindingAdapterPosition) }
        }

        fun bind(content: Content) {
            with(binding) {
                if (content.thumbnails.size > 1) {
                    ivArt.load(content.thumbnails[1].url)
                } else {
                    ivArt.load(content.thumbnails[0].url)
                }
                tvTitle.text = content.title
                tvTitle.isSelected = true
                tvDescription.text = content.description ?: (if (!content.artists.isNullOrEmpty()) content.artists.toListName().connectArtists() else context.getString(R.string.playlist))
                tvDescription.isSelected = true
            }
        }
    }
    inner class AlbumViewHolder(var binding: ItemHomeContentPlaylistBinding, var listener: onAlbumItemClickListener): RecyclerView.ViewHolder(binding.root){
        init {
            binding.root.setOnClickListener {listener.onAlbumItemClick(bindingAdapterPosition)}
        }
        fun bind(content: Content){
            with(binding){
                if (content.thumbnails.size > 1) {
                    ivArt.load(content.thumbnails[1].url)
                }
                else{
                    ivArt.load(content.thumbnails[0].url)
                }
                tvTitle.text = content.title
                tvTitle.isSelected = true
                if (content.description != "" && content.description != null) {
                    tvDescription.text = content.description
                }
                else {
                    tvDescription.text = if (!content.artists.isNullOrEmpty()) content.artists.toListName().connectArtists() else context.getString(R.string.album)
                }
                tvDescription.isSelected = true
            }
        }
    }
    inner class ArtistViewHolder(var binding: ItemHomeContentArtistBinding, var listener: onArtistItemClickListener) : RecyclerView.ViewHolder(binding.root){
        init {
            binding.root.setOnClickListener {listener.onArtistItemClick(bindingAdapterPosition)}
        }
        fun bind(content: Content){
            with(binding){
                if (content.thumbnails.size > 1) {
                    ivArt.load(content.thumbnails[1].url)
                }
                else{
                    ivArt.load(content.thumbnails[0].url)
                }
                tvArtistName.text = content.title
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflate = LayoutInflater.from(parent.context)
        return when (viewType) {
            SONG -> SongViewHolder(ItemHomeContentSongBinding.inflate(inflate, parent, false), mSongListener)
            PLAYLIST -> PlaylistViewHolder(ItemHomeContentPlaylistBinding.inflate(inflate, parent, false), mPlaylistListener)
            ALBUM -> AlbumViewHolder(ItemHomeContentPlaylistBinding.inflate(inflate, parent, false), mAlbumListener)
            ARTIST -> ArtistViewHolder(
                ItemHomeContentArtistBinding.inflate(inflate, parent, false),
                mArtistListener
            )

            VIDEO -> VideoViewHolder(
                ItemHomeContentVideoBinding.inflate(inflate, parent, false),
                mSongListener
            )

            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder){
            is SongViewHolder -> holder.bind(listContent[position])
            is PlaylistViewHolder -> holder.bind(listContent[position])
            is AlbumViewHolder -> holder.bind(listContent[position])
            is ArtistViewHolder -> holder.bind(listContent[position])
            is VideoViewHolder -> holder.bind(listContent[position])
        }
    }

    override fun getItemViewType(position: Int): Int {
        val temp = listContent[position]
        return if ((temp.playlistId != null && temp.videoId == null) || (temp.playlistId != null && temp.videoId == "")){
            if (temp.playlistId.startsWith("UC"))
                ARTIST
            else
                PLAYLIST
        } else if ((temp.browseId != null && temp.videoId == null) || (temp.browseId != null && temp.videoId == "")) {
            if (temp.browseId.startsWith("UC"))
                ARTIST
            else
                ALBUM
        } else if (temp.thumbnails.firstOrNull()?.width != temp.thumbnails.firstOrNull()?.height) {
            VIDEO
        } else {
            SONG
        }
    }

    override fun getItemCount(): Int {
        return listContent.size
    }

    companion object {
        private const val SONG = 1
        private const val PLAYLIST = 2
        private const val ALBUM = 3
        private const val ARTIST = 4
        private const val VIDEO = 5
    }
}