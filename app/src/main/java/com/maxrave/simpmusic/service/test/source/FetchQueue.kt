package com.maxrave.simpmusic.service.test.source

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import com.maxrave.simpmusic.data.queue.Queue
import com.maxrave.simpmusic.service.SimpleMediaServiceHandler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
        musicSource.reset()
        Log.d("Check Queue", "getRelated: ${Queue.getQueue().toString()}")
        scope.launch {
            musicSource.load()
            Log.d("CHECK SOURCE", "${musicSource.catalog.size}")
            musicSource.whenReady { isInitilized ->
                if (isInitilized) {
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