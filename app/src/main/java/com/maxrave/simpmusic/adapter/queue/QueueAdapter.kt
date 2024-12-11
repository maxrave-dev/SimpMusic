package com.maxrave.simpmusic.adapter.queue

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.maxrave.simpmusic.data.model.browse.album.Track
import com.maxrave.simpmusic.databinding.ItemQueueTrackBinding
import com.maxrave.simpmusic.extension.connectArtists
import com.maxrave.simpmusic.extension.toListName
import java.util.Collections

class QueueAdapter(
    private val listTrack: ArrayList<Track>,
    val context: Context,
    private var currentPlaying: Int,
) : RecyclerView.Adapter<QueueAdapter.QueueViewHolder>(),
    RecyclerRowMoveCallback.RecyclerViewRowTouchHelperContract {
    private lateinit var mListener: OnItemClickListener
    private lateinit var mOptionListener: OnOptionClickListener
    private lateinit var mSwapListener: OnSwapListener

    interface OnItemClickListener {
        fun onItemClick(position: Int)
    }

    fun setOnClickListener(listener: OnItemClickListener) {
        mListener = listener
    }

    fun setOnSwapListener(listener: OnSwapListener) {
        mSwapListener = listener
    }

    fun getIndexOf(videoId: String): Int? {
        for (i in listTrack.indices) {
            if (listTrack.getOrNull(i)?.videoId == videoId) {
                return i
            }
        }
        return null
    }

    interface OnOptionClickListener {
        fun onOptionClick(position: Int)
    }

    interface OnSwapListener {
        fun onSwap(
            from: Int,
            to: Int,
        )
    }

    fun setOnOptionClickListener(listener: OnOptionClickListener) {
        mOptionListener = listener
    }

    fun setCurrentPlaying(position: Int) {
        currentPlaying = position
        notifyDataSetChanged()
    }

    inner class QueueViewHolder(
        val binding: ItemQueueTrackBinding,
        listener: OnItemClickListener,
        optionClickListener: OnOptionClickListener,
    ) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                listener.onItemClick(bindingAdapterPosition)
            }
            binding.btMore.setOnClickListener {
                optionClickListener.onOptionClick(bindingAdapterPosition)
            }
        }
    }

    fun updateList(tracks: ArrayList<Track>) {
        listTrack.clear()
        listTrack.addAll(tracks)
        notifyDataSetChanged()
    }

    fun addToList(track: Track) {
        listTrack.add(track)
        notifyItemInserted(listTrack.size - 1)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): QueueViewHolder =
        QueueViewHolder(ItemQueueTrackBinding.inflate(LayoutInflater.from(parent.context), parent, false), mListener, mOptionListener)

    override fun getItemCount(): Int = listTrack.size

    override fun onBindViewHolder(
        holder: QueueViewHolder,
        position: Int,
    ) {
        val track = listTrack[position]
        with(holder) {
            if (position == currentPlaying) {
                binding.tvPosition.visibility = View.VISIBLE
                binding.ivPlaying.visibility = View.VISIBLE
                binding.tvPosition.text = null
                binding.tvSongName.text = track.title
                binding.tvSongName.isSelected = true
                binding.tvArtistName.text = track.artists.toListName().connectArtists()
                binding.tvArtistName.isSelected = true
            } else {
                binding.tvPosition.visibility = View.VISIBLE
                binding.ivPlaying.visibility = View.GONE
                binding.tvPosition.text = (position + 1).toString()
                binding.tvSongName.text = track.title
                binding.tvSongName.isSelected = true
                binding.tvArtistName.text = track.artists.toListName().connectArtists()
                binding.tvArtistName.isSelected = true
            }
        }
    }

    override fun onRowMoved(
        from: Int,
        to: Int,
    ) {
        if (from < to) {
            for (i in from until to) {
                Collections.swap(listTrack, i, i + 1)
            }
        } else {
            for (i in from downTo to + 1) {
                Collections.swap(listTrack, i, i - 1)
            }
        }
        mSwapListener.onSwap(from, to)
        notifyItemMoved(from, to)
    }

    override fun onRowMove(
        from: Int,
        to: Int,
    ) {
        if (from < to) {
            for (i in from until to) {
                Collections.swap(listTrack, i, i + 1)
            }
        } else {
            for (i in from downTo to + 1) {
                Collections.swap(listTrack, i, i - 1)
            }
        }
        notifyItemMoved(from, to)
    }

    override fun onRowSelected(myViewHolder: QueueViewHolder?) {
    }

    override fun onRowClear(myViewHolder: QueueViewHolder?) {
    }

//    override fun onViewAttachedToWindow(holder: QueueViewHolder) {
//        super.onViewAttachedToWindow(holder)
//        val track = listTrack[holder.layoutPosition]
//        with(holder){
//            binding.tvPosition.visibility = View.GONE
//            binding.ivPlaying.visibility = View.VISIBLE
//
//            binding.tvSongName.text = track.mediaMetadata.title
//            binding.tvSongName.isSelected = true
// //            var artist = ""
// //            for (i in track.artists!!.indices){
// //                artist += if (i == track.artists.size - 1) track.artists[i].name else "${track.artists[i].name}, "
// //            }
//            binding.tvArtistName.text = track.mediaMetadata.artist
//        }
//    }
}