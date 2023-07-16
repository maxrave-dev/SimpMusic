package com.maxrave.simpmusic.adapter.queue

import android.animation.ValueAnimator
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.media3.common.MediaItem
import androidx.recyclerview.widget.RecyclerView
import com.maxrave.simpmusic.data.model.browse.album.Track
import com.maxrave.simpmusic.databinding.ItemQueueTrackBinding
import com.maxrave.simpmusic.extension.connectArtists
import com.maxrave.simpmusic.extension.toListId
import com.maxrave.simpmusic.extension.toListName


class QueueAdapter(private val listTrack: ArrayList<Track>, val context: Context, private var currentPlaying: Int): RecyclerView.Adapter<QueueAdapter.QueueViewHolder>() {
    private lateinit var mListener: OnItemClickListener
    interface OnItemClickListener{
        fun onItemClick(position: Int)
    }
    fun setOnClickListener(listener: OnItemClickListener){
        mListener = listener
    }
    fun setCurrentPlaying(position: Int) {
        currentPlaying = position
        notifyDataSetChanged()
    }

     inner class QueueViewHolder(val binding: ItemQueueTrackBinding, listener: OnItemClickListener): RecyclerView.ViewHolder(binding.root) {
         init {
             binding.root.setOnClickListener {
                 listener.onItemClick(bindingAdapterPosition)
             }
         }
    }

    fun updateList(tracks: ArrayList<Track>) {
        listTrack.clear()
        listTrack.addAll(tracks)
        notifyDataSetChanged()
    }
    fun addToList(track: Track){
        listTrack.add(track)
        notifyItemInserted(listTrack.size - 1)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QueueViewHolder {
        return QueueViewHolder(ItemQueueTrackBinding.inflate(LayoutInflater.from(parent.context), parent, false), mListener)
    }

    override fun getItemCount(): Int {
        return listTrack.size
    }

    override fun onBindViewHolder(holder: QueueViewHolder, position: Int) {
        val track = listTrack[position]
        with(holder){
            if (position == currentPlaying){
                binding.tvPosition.visibility = View.VISIBLE
                binding.ivPlaying.visibility = View.VISIBLE
//                val drawable = AnimatedVectorDrawableCompat.create(context, R.drawable.playing_anim)
//                drawable?.registerAnimationCallback(object : Animatable2Compat.AnimationCallback(){
//                    override fun onAnimationEnd(d: Drawable?) {
//                        binding.ivPlaying.post { drawable.start() }
//                    }
//                })
//                binding.ivPlaying.setImageDrawable(drawable)
//                drawable?.start()
                binding.tvPosition.text = null
                binding.tvSongName.text = track.title
                binding.tvSongName.isSelected = true
//            var artist = ""
//            for (i in track.artists!!.indices){
//                artist += if (i == track.artists.size - 1) track.artists[i].name else "${track.artists[i].name}, "
//            }
                binding.tvArtistName.text = track.artists.toListName().connectArtists()
                binding.tvArtistName.isSelected = true
            }
            else {
                binding.tvPosition.visibility = View.VISIBLE
                binding.ivPlaying.visibility = View.GONE
                binding.tvPosition.text = (position + 1).toString()
                binding.tvSongName.text = track.title
                binding.tvSongName.isSelected = true
//            var artist = ""
//            for (i in track.artists!!.indices){
//                artist += if (i == track.artists.size - 1) track.artists[i].name else "${track.artists[i].name}, "
//            }
                binding.tvArtistName.text = track.artists.toListName().connectArtists()
                binding.tvArtistName.isSelected = true
            }
        }
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
////            var artist = ""
////            for (i in track.artists!!.indices){
////                artist += if (i == track.artists.size - 1) track.artists[i].name else "${track.artists[i].name}, "
////            }
//            binding.tvArtistName.text = track.mediaMetadata.artist
//        }
//    }
}