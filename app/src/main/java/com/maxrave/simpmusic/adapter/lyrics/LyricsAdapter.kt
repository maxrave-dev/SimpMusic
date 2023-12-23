package com.maxrave.simpmusic.adapter.lyrics

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.maxrave.simpmusic.data.model.metadata.Line
import com.maxrave.simpmusic.data.model.metadata.Lyrics
import com.maxrave.simpmusic.databinding.ItemLyricsActiveBinding
import com.maxrave.simpmusic.databinding.ItemLyricsNormalBinding

class LyricsAdapter(private var originalLyrics: Lyrics?, var translated: Lyrics? = null): RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var activeLyrics: Line? = null

    interface OnItemClickListener {
        fun onItemClick(line: Line?)
    }

    lateinit var mListener: OnItemClickListener
    fun setOnItemClickListener(listener: OnItemClickListener) {
        mListener = listener
    }

    fun updateOriginalLyrics(lyrics: Lyrics) {
        if (lyrics != originalLyrics) {
            originalLyrics = lyrics
            translated = null
            notifyDataSetChanged()
        }
    }

    fun updateTranslatedLyrics(lyrics: Lyrics?) {
        if (lyrics != null) {
            translated = lyrics
            notifyDataSetChanged()
        }
    }

    fun setActiveLyrics(index: Int) {
        if (index == -1) {
            if (activeLyrics != null) {
                activeLyrics = null
                notifyDataSetChanged()
            }
        }
        else {
            if (originalLyrics?.lines?.get(index) != activeLyrics) {
                activeLyrics = originalLyrics?.lines?.get(index)
                notifyItemChanged(index)
                if (index > 0) {
                    notifyItemChanged(index - 1)
                }
            }
        }
    }

    inner class ActiveViewHolder(
        val binding: ItemLyricsActiveBinding,
        val listener: OnItemClickListener
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(line: Line) {
            binding.root.setOnClickListener {
                listener.onItemClick(line)
            }
            binding.tvNowLyrics.text = line.words
            if (translated != null && translated?.lines?.find { it.startTimeMs == line.startTimeMs }?.words != null) {
                translated?.lines?.find { it.startTimeMs == line.startTimeMs }?.words?.let {
                    binding.tvTranslatedLyrics.visibility = View.VISIBLE
                    binding.tvTranslatedLyrics.text = it
                }
            } else {
                binding.tvTranslatedLyrics.visibility = View.GONE
            }
        }
    }

    inner class NormalViewHolder(
        val binding: ItemLyricsNormalBinding,
        val listener: OnItemClickListener
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(line: Line) {
            binding.root.setOnClickListener {
                listener.onItemClick(line)
            }
            binding.tvLyrics.text = line.words
            if (translated != null && translated?.lines?.find { it.startTimeMs == line.startTimeMs }?.words != null) {
                translated?.lines?.find { it.startTimeMs == line.startTimeMs }?.words?.let {
                    binding.tvTranslatedLyrics.visibility = View.VISIBLE
                    binding.tvTranslatedLyrics.text = it
                }
            } else {
                binding.tvTranslatedLyrics.visibility = View.GONE
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_ACTIVE -> ActiveViewHolder(
                ItemLyricsActiveBinding.inflate(
                    LayoutInflater.from(
                        parent.context
                    ), parent, false
                ), mListener
            )

            else -> NormalViewHolder(
                ItemLyricsNormalBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                ), mListener
            )
        }
    }

    override fun getItemCount(): Int {
        return originalLyrics?.lines?.size ?: 0
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ActiveViewHolder -> {
                holder.bind(originalLyrics?.lines?.get(position) ?: return)
            }
            is NormalViewHolder -> {
                holder.bind(originalLyrics?.lines?.get(position) ?: return)
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (originalLyrics?.lines?.get(position) == activeLyrics) {
                TYPE_ACTIVE
            }
            else {
                TYPE_NORMAL
            }
        }

    companion object {
        const val TYPE_ACTIVE = 0
        const val TYPE_NORMAL = 1
    }
}