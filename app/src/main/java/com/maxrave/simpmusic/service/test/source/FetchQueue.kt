package com.maxrave.simpmusic.service.test.source

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.media3.common.util.UnstableApi
import com.maxrave.simpmusic.data.queue.Queue
import com.maxrave.simpmusic.service.SimpleMediaServiceHandler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class FetchQueue: Service() {
    @Inject
    lateinit var musicSource: MusicSource

    @Inject
    lateinit var simpleMediaServiceHandler: SimpleMediaServiceHandler

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Main + job)


    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    @UnstableApi
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val index = intent?.extras?.getInt("index")
        val downloaded = intent?.extras?.getInt("downloaded")
        Log.d("Check Index inside Service", "$index")
        Log.d("Check Queue", "getRelated: ${Queue.getQueue().toString()}")
        scope.launch {
            if (downloaded == 1){
                musicSource.load(1)
            }
            else {
                musicSource.load()
            }
            musicSource.stateFlow.collect{ state ->
                if (state == StateSource.STATE_INITIALIZED) {
                    when (index) {
                        null -> {
                            musicSource.addFirstMediaItem(simpleMediaServiceHandler.getMediaItemWithIndex(0))
                        }
                        -1 -> {

                        }
                        else -> {
                            musicSource.addFirstMediaItemToIndex(simpleMediaServiceHandler.getMediaItemWithIndex(0), index)
                            Queue.getNowPlaying().let { song ->
                                if (song != null) {
                                    musicSource.catalogMetadata.removeAt(0)
                                    musicSource.catalogMetadata.add(index, song)
                                    if (downloaded != 1){
                                        val tempUrl = musicSource.downloadUrl[0]
                                        musicSource.downloadUrl.removeAt(0)
                                        musicSource.downloadUrl.add(index, tempUrl)
                                    }
                                }
                            }
                        }
                    }
                    stopSelf()
                }
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
        scope.cancel()
        Log.d("FetchQueue", "onDestroy: ")
    }

}