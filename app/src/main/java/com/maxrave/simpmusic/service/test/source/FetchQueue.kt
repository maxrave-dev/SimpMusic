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

    var IS_ACTIVITY_RUNNING = false

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    @UnstableApi
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        musicSource.reset()
        Log.d("Check Queue", "getRelated: ${Queue.getQueue().toString()}")
        scope.launch {
            musicSource.load()
            Log.d("CHECK SOURCE", "${musicSource.catalog.size}")
//            musicSource.whenReady { isInitilized ->
//                if (isInitilized) {
//                    simpleMediaServiceHandler.addMediaItemList(musicSource.catalog)
//                }
//            }
            musicSource.stateFlow.collect{ state ->
                if (state == StateSource.STATE_INITIALIZED) {
                    musicSource.addFirstMediaItem(simpleMediaServiceHandler.getCurrentMediaItem())
                    simpleMediaServiceHandler.addMediaItemList(musicSource.catalog)
                }
            }
        }
        return START_REDELIVER_INTENT
    }

    override fun onDestroy() {
        job.cancel()
        super.onDestroy()
    }

}