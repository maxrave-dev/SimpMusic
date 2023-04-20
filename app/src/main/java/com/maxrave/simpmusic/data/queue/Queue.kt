package com.maxrave.simpmusic.data.queue

import com.maxrave.simpmusic.data.model.mediaService.Song
import com.maxrave.simpmusic.data.model.metadata.MetadataSong
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.util.LinkedList
import java.util.Queue

object Queue {
    private var queue: Queue<MetadataSong> = LinkedList()
    fun add(song: MetadataSong) {
        queue.add(song)
    }
    fun addAll(songs: List<MetadataSong>) {
        queue.addAll(songs)
    }
    fun remove(song: MetadataSong) {
        queue.remove(song)
    }
    fun peek(): MetadataSong? {
        return queue.remove()
    }
    fun getQueue(): Queue<MetadataSong> {
        return queue
    }
}