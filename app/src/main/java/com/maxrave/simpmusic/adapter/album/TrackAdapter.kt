package com.maxrave.simpmusic.adapter.album

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.maxrave.simpmusic.data.db.entities.SongEntity
import com.maxrave.simpmusic.data.model.browse.album.Track
import com.maxrave.simpmusic.databinding.ItemAlbumTrackBinding
import com.maxrave.simpmusic.extension.connectArtists
import com.maxrave.simpmusic.extension.toListName
import com.maxrave.simpmusic.extension.toTrack
import com.maxrave.simpmusic.extension.toVideoIdList

class TrackAdapter(
    private var trackList: ArrayList<Any>,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private lateinit var mListener: OnItemClickListener
    private lateinit var mOptionListener: OnOptionClickListener

    interface OnItemClickListener {
        fun onItemClick(position: Int)
    }

    interface OnOptionClickListener {
        fun onOptionClick(position: Int)
    }

    fun setOnClickListener(listener: OnItemClickListener) {
        mListener = listener
    }

    fun setOnOptionClickListener(listener: OnOptionClickListener) {
        mOptionListener = listener
    }

    fun updateList(newList: ArrayList<Any>) {
        trackList.clear()
        trackList.addAll(newList)
        notifyDataSetChanged()
    }

    private var downloadedList = arrayListOf<SongEntity>()
    private var playingTrackVideoId: String? = null

    fun setDownloadedList(downloadedList: ArrayList<SongEntity>?) {
        val oldList = arrayListOf<SongEntity>()
        oldList.addAll(this.downloadedList)
        this.downloadedList = downloadedList ?: arrayListOf()
        trackList.mapIndexed { index, result ->
            if (result is Track || result is SongEntity) {
                val videoId =
                    when (result) {
                        is Track -> result.videoId
                        is SongEntity -> result.videoId
                        else -> null
                    }
                if (downloadedList != null) {
                    if (downloadedList.toVideoIdList().contains(videoId) &&
                        !oldList
                            .toVideoIdList()
                            .contains(videoId)
                    ) {
                        notifyItemChanged(index)
                    } else if (!downloadedList
                            .toVideoIdList()
                            .contains(videoId) &&
                        oldList.toVideoIdList().contains(videoId)
                    ) {
                        notifyItemChanged(index)
                    }
                }
            }
        }
    }

    fun setNowPlaying(it: String?) {
        val oldPlayingTrackVideoId = playingTrackVideoId
        playingTrackVideoId = it
        trackList.mapIndexed { index, result ->
            if (result is Track || result is SongEntity) {
                val videoId =
                    when (result) {
                        is Track -> result.videoId
                        is SongEntity -> result.videoId
                        else -> null
                    }
                if (videoId == playingTrackVideoId) {
                    notifyItemChanged(index)
                } else if (videoId == oldPlayingTrackVideoId) {
                    notifyItemChanged(index)
                }
            }
        }
    }

    inner class TrackViewHolder(
        val binding: ItemAlbumTrackBinding,
        rootListener: OnItemClickListener,
        optionListener: OnOptionClickListener,
    ) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.rootLayout.setOnClickListener {
                rootListener.onItemClick(bindingAdapterPosition)
            }
            binding.btMore.setOnClickListener {
                optionListener.onOptionClick(bindingAdapterPosition)
            }
        }

        fun bind(track: Track) {
            with(binding) {
                tvSongName.text = track.title
                tvArtistName.text = track.artists.toListName().connectArtists()
                tvPosition.text = (bindingAdapterPosition + 1).toString()
                if (downloadedList.toVideoIdList().contains(track.videoId)) {
                    ivDownloaded.visibility = View.VISIBLE
                } else {
                    ivDownloaded.visibility = View.GONE
                }
                if (track.videoId == playingTrackVideoId) {
                    ivPlaying.visibility = View.VISIBLE
                    tvPosition.visibility = View.GONE
                } else {
                    ivPlaying.visibility = View.GONE
                    tvPosition.visibility = View.VISIBLE
                }
            }
        }
    }

    inner class SongEntityViewHolder(
        val binding: ItemAlbumTrackBinding,
        rootListener: OnItemClickListener,
        optionListener: OnOptionClickListener,
    ) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.rootLayout.setOnClickListener {
                rootListener.onItemClick(bindingAdapterPosition)
            }
            binding.btMore.setOnClickListener {
                optionListener.onOptionClick(bindingAdapterPosition)
            }
        }

        fun bind(songEntity: SongEntity) {
            with(binding) {
                tvSongName.text = songEntity.title
                tvArtistName.text = songEntity.artistName?.connectArtists()
                tvPosition.text = (bindingAdapterPosition + 1).toString()
                if (downloadedList.toVideoIdList().contains(songEntity.videoId)) {
                    ivDownloaded.visibility = View.VISIBLE
                } else {
                    ivDownloaded.visibility = View.GONE
                }
                if (songEntity.videoId == playingTrackVideoId) {
                    ivPlaying.visibility = View.VISIBLE
                    tvPosition.visibility = View.GONE
                } else {
                    ivPlaying.visibility = View.GONE
                    tvPosition.visibility = View.VISIBLE
                }
            }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): RecyclerView.ViewHolder =
        when (viewType) {
            TYPE_TRACK -> {
                TrackViewHolder(ItemAlbumTrackBinding.inflate(LayoutInflater.from(parent.context), parent, false), mListener, mOptionListener)
            }
            TYPE_SONG_ENTITY -> {
                SongEntityViewHolder(ItemAlbumTrackBinding.inflate(LayoutInflater.from(parent.context), parent, false), mListener, mOptionListener)
            }
            else -> {
                throw IllegalArgumentException("Invalid type of data $viewType")
            }
        }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
    ) {
        when (holder) {
            is TrackViewHolder -> {
                holder.bind(trackList[position] as Track)
            }
            is SongEntityViewHolder -> {
                holder.bind(trackList[position] as SongEntity)
            }
            else -> {
                throw IllegalArgumentException("Invalid type of data $position")
            }
        }
    }

    override fun getItemCount(): Int = trackList.size

    override fun getItemViewType(position: Int): Int =
        when (trackList[position]) {
            is Track -> TYPE_TRACK
            is SongEntity -> TYPE_SONG_ENTITY
            else -> {
                throw IllegalArgumentException("Invalid type of data $position")
            }
        }

    fun getList(): ArrayList<Track> {
        val temp = arrayListOf<Track>()
        for (i in trackList) {
            if (i is Track) {
                temp.add(i)
            } else if (i is SongEntity) {
                temp.add(i.toTrack())
            }
        }
        return temp
    }

    companion object {
        private const val TYPE_TRACK = 0
        private const val TYPE_SONG_ENTITY = 1
    }
}