package com.maxrave.simpmusic.service

import android.content.Context
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LyricsFetcher(private val context: Context) {

    init {
        // Initialize Python for the Android module
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(context))
        }
    }

    suspend fun fetchSyncedLyrics(query: String): String? = withContext(Dispatchers.IO) {
        try {
            val py = Python.getInstance()
            val module = py.getModule("lyrics_service") 
            val result = module.callAttr("search_synced_lyrics", query)
            result.toString()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
