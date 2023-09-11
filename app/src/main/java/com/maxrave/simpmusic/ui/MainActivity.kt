package com.maxrave.simpmusic.ui

import android.Manifest
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.graphics.ColorUtils
import androidx.core.net.toUri
import androidx.core.os.LocaleListCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.DownloadService
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.load
import coil.request.ImageRequest
import coil.size.Size
import coil.transform.Transformation
import com.daimajia.swipe.SwipeLayout
import com.maxrave.kotlinytmusicscraper.YouTube
import com.maxrave.kotlinytmusicscraper.models.YouTubeLocale
import com.maxrave.simpmusic.R
import com.maxrave.simpmusic.common.Config
import com.maxrave.simpmusic.common.FIRST_TIME_MIGRATION
import com.maxrave.simpmusic.common.SELECTED_LANGUAGE
import com.maxrave.simpmusic.common.STATUS_DONE
import com.maxrave.simpmusic.common.SUPPORTED_LANGUAGE
import com.maxrave.simpmusic.common.SUPPORTED_LOCATION
import com.maxrave.simpmusic.data.queue.Queue
import com.maxrave.simpmusic.databinding.ActivityMainBinding
import com.maxrave.simpmusic.extension.connectArtists
import com.maxrave.simpmusic.extension.isMyServiceRunning
import com.maxrave.simpmusic.extension.toTrack
import com.maxrave.simpmusic.service.SimpleMediaService
import com.maxrave.simpmusic.service.test.source.FetchQueue
import com.maxrave.simpmusic.ui.fragment.player.NowPlayingFragment
import com.maxrave.simpmusic.utils.Resource
import com.maxrave.simpmusic.viewModel.SharedViewModel
import com.maxrave.simpmusic.viewModel.UIEvent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import pub.devrel.easypermissions.EasyPermissions
import java.util.Locale

@UnstableApi
@AndroidEntryPoint
class MainActivity : AppCompatActivity(), NowPlayingFragment.OnNowPlayingSongChangeListener {
    private lateinit var binding: ActivityMainBinding
    private val viewModel by viewModels<SharedViewModel>()
    private var action: String? = null
    private var data: Uri? = null

    override fun onResume() {
        super.onResume()
        viewModel.getCurrentMediaItem()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        action = intent?.action
        data = intent?.data ?: intent?.getStringExtra(Intent.EXTRA_TEXT)?.toUri()
        Log.d("MainActivity", "onNewIntent: $data")
        viewModel.intent.value = intent
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    @UnstableApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        action = intent.action
        viewModel.checkIsRestoring()

        // Check if the migration has already been done or not
        if (getString(FIRST_TIME_MIGRATION) != STATUS_DONE) {
            Log.d("Locale Key", "onCreate: ${Locale.getDefault().toLanguageTag()}")
            if (SUPPORTED_LANGUAGE.codes.contains(Locale.getDefault().toLanguageTag())) {
                Log.d("Contains", "onCreate: ${SUPPORTED_LANGUAGE.codes.contains(Locale.getDefault().toLanguageTag())}")
                putString(SELECTED_LANGUAGE, Locale.getDefault().toLanguageTag())
                if (SUPPORTED_LOCATION.items.contains(Locale.getDefault().country)) {
                    putString("location", Locale.getDefault().country)
                }
                else {
                    putString("location", "US")
                }
                YouTube.locale = YouTubeLocale(
                    gl = getString("location") ?: "US",
                    hl = Locale.getDefault().toLanguageTag()
                )
            } else {
                putString(SELECTED_LANGUAGE, "en-US")
                YouTube.locale = YouTubeLocale(
                    gl = getString("location") ?: "US",
                    hl = "en-US"
                )
            }
            // Fetch the selected language from wherever it was stored. In this case its SharedPref
            getString(SELECTED_LANGUAGE)?.let {
                Log.d("Locale Key", "getString: $it")
                // Set this locale using the AndroidX library that will handle the storage itself
                val localeList = LocaleListCompat.forLanguageTags(it)
                AppCompatDelegate.setApplicationLocales(localeList)
                // Set the migration flag to ensure that this is executed only once
                putString(FIRST_TIME_MIGRATION, STATUS_DONE)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
        } else {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
        }

        if (!EasyPermissions.hasPermissions(this, Manifest.permission.POST_NOTIFICATIONS)){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                EasyPermissions.requestPermissions(this,
                    getString(R.string.this_app_needs_to_access_your_notification), 1, Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        viewModel.getLocation()
        viewModel.checkAuth()
        viewModel.checkAllDownloadingSongs()
        runBlocking { delay(500) }

        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        binding.miniplayer.visibility = View.GONE
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.fragment_container_view)
        val navController = navHostFragment?.findNavController()
        binding.bottomNavigationView.setupWithNavController(navController!!)
        when (action) {
            "com.maxrave.simpmusic.action.HOME" -> {
                binding.bottomNavigationView.selectedItemId = R.id.bottom_navigation_item_home
            }
            "com.maxrave.simpmusic.action.SEARCH" -> {
                binding.bottomNavigationView.selectedItemId = R.id.bottom_navigation_item_search
            }
            "com.maxrave.simpmusic.action.LIBRARY" -> {
                binding.bottomNavigationView.selectedItemId = R.id.bottom_navigation_item_library
            }
            else -> {}
        }

        binding.miniplayer.showMode = SwipeLayout.ShowMode.PullOut
        binding.miniplayer.addDrag(SwipeLayout.DragEdge.Right, binding.llBottom)
        binding.miniplayer.addSwipeListener(object : SwipeLayout.SwipeListener {
            override fun onStartOpen(layout: SwipeLayout?) {
                binding.card.radius = 0f
            }

            override fun onOpen(layout: SwipeLayout?) {
                binding.card.radius = 0f
            }

            override fun onStartClose(layout: SwipeLayout?) {
            }

            override fun onClose(layout: SwipeLayout?) {
                binding.card.radius = 8f
            }

            override fun onUpdate(layout: SwipeLayout?, leftOffset: Int, topOffset: Int) {
            }

            override fun onHandRelease(layout: SwipeLayout?, xvel: Float, yvel: Float) {
            }

        })
        binding.btRemoveMiniPlayer.setOnClickListener {
            if (viewModel.isServiceRunning.value == true){
                viewModel.stopPlayer()
            }
            viewModel.videoId.postValue(null)
            binding.miniplayer.visibility = View.GONE
            binding.card.radius = 8f
        }

        binding.card.setOnClickListener {
            val bundle = Bundle()
            bundle.putString("type", Config.MINIPLAYER_CLICK)
            navController.navigate(R.id.action_global_nowPlayingFragment, bundle)
        }
        binding.btPlayPause.setOnClickListener {
            viewModel.onUIEvent(UIEvent.PlayPause)
        }
        lifecycleScope.launch {
            val job1 = launch {
                viewModel.intent.collectLatest { intent ->
                    if (intent != null){
                        data = intent.data ?: intent.getStringExtra(Intent.EXTRA_TEXT)?.toUri()
                        Log.d("MainActivity", "onCreate: $data")
                        if (data != null) {
                            when (val path = data!!.pathSegments.firstOrNull()) {
                                "playlist" -> data!!.getQueryParameter("list")?.let { playlistId ->
                                    if (playlistId.startsWith("OLAK5uy_")) {
                                        viewModel.intent.value = null
                                        navController.navigate(R.id.action_global_albumFragment, Bundle().apply {
                                            putString("browseId", playlistId)
                                        })
                                    }
                                    else if (playlistId.startsWith("VL")) {
                                        viewModel.intent.value = null
                                        navController.navigate(R.id.action_global_playlistFragment, Bundle().apply {
                                            putString("id", playlistId)
                                        })
                                    }
                                    else {
                                        viewModel.intent.value = null
                                        navController.navigate(R.id.action_global_playlistFragment, Bundle().apply {
                                            putString("id", "VL$playlistId")
                                        })
                                    }
                                }

                                "channel", "c" -> data!!.lastPathSegment?.let { artistId ->
                                    if (artistId.startsWith("UC")) {
                                        viewModel.intent.value = null
                                        navController.navigate(R.id.action_global_artistFragment, Bundle().apply {
                                            putString("channelId", artistId)
                                        })
                                    }
//                                    else {
//                                        viewModel.convertNameToId(artistId)
//                                        viewModel.artistId.observe(this@MainActivity) {channelId ->
//                                            when (channelId) {
//                                                is Resource.Success -> {
//                                                    viewModel.intent.value = null
//                                                    navController.navigate(R.id.action_global_artistFragment, Bundle().apply {
//                                                        putString("channelId", channelId.data?.id)
//                                                    })
//                                                }
//                                                is Resource.Error -> {
//                                                    viewModel.intent.value = null
//                                                    Toast.makeText(this@MainActivity, channelId.message, Toast.LENGTH_SHORT).show()
//                                                }
//                                            }
//                                        }
//                                    }
                                }

                                else -> when {
                                    path == "watch" -> data!!.getQueryParameter("v")
                                    data!!.host == "youtu.be" -> path
                                    else -> null
                                }?.let { videoId ->
                                    viewModel.getSongFull(videoId)
                                    viewModel.songFull.observe(this@MainActivity) {
                                        if (it != null){
                                            val track = it.toTrack(videoId)
                                            Queue.clear()
                                            Queue.setNowPlaying(track)
                                            val args = Bundle()
                                            args.putString("videoId", videoId)
                                            args.putString("from", getString(R.string.shared))
                                            args.putString("type", Config.SONG_CLICK)
                                            viewModel.intent.value = null
                                            navController.navigate(R.id.action_global_nowPlayingFragment, args)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            val job5 = launch {
                viewModel.nowPlayingMediaItem.observe(this@MainActivity){
                    if (it != null){
                        if (viewModel.isServiceRunning.value == false){
                            startService()
                            viewModel.isServiceRunning.postValue(true)
                        }
                        binding.songTitle.text = it.mediaMetadata.title
                        binding.songTitle.isSelected = true
                        binding.songArtist.text = it.mediaMetadata.artist
                        binding.songArtist.isSelected = true
                        val request = ImageRequest.Builder(this@MainActivity)
                            .data(it.mediaMetadata.artworkUri)
                            .target(
                                onSuccess = { result ->
                                    binding.ivArt.setImageDrawable(result)
                                },
                            )
                            .transformations(object : Transformation {
                                override val cacheKey: String
                                    get() = it.mediaMetadata.artworkUri.toString()

                                override suspend fun transform(input: Bitmap, size: Size): Bitmap {
                                    val p = Palette.from(input).generate()
                                    val defaultColor = 0x000000
                                    var startColor = p.getDarkVibrantColor(defaultColor)
                                    Log.d("Check Start Color", "transform: $startColor")
                                    if (startColor == defaultColor){
                                        startColor = p.getDarkMutedColor(defaultColor)
                                        if (startColor == defaultColor){
                                            startColor = p.getVibrantColor(defaultColor)
                                            if (startColor == defaultColor){
                                                startColor = p.getMutedColor(defaultColor)
                                                if (startColor == defaultColor){
                                                    startColor = p.getLightVibrantColor(defaultColor)
                                                    if (startColor == defaultColor){
                                                        startColor = p.getLightMutedColor(defaultColor)
                                                    }
                                                }
                                            }
                                        }
                                        Log.d("Check Start Color", "transform: $startColor")
                                    }
                                    val endColor = 0x1b1a1f
                                    val gd = GradientDrawable(
                                        GradientDrawable.Orientation.TOP_BOTTOM,
                                        intArrayOf(startColor, endColor)
                                    )
                                    gd.cornerRadius = 0f
                                    gd.gradientType = GradientDrawable.LINEAR_GRADIENT
                                    gd.gradientRadius = 0.5f
                                    gd.alpha = 150
                                    val bg = ColorUtils.setAlphaComponent(startColor, 230)
                                    binding.card.setCardBackgroundColor(bg)
                                    binding.cardBottom.setCardBackgroundColor(bg)
                                    return input
                                }

                            })
                            .build()
                        ImageLoader(this@MainActivity).enqueue(request)
                    }
                }
            }
            val job2 = launch {
                viewModel.progress.collect{
                    binding.progressBar.progress = (it * 100).toInt()
                }
            }

            val job6 = launch {
                viewModel.liked.collect{ liked ->
                    binding.cbFavorite.isChecked = liked
                }
            }
            job1.join()
            job2.join()
            //job3.join()
            job5.join()
            job6.join()
        }
        binding.card.animation = AnimationUtils.loadAnimation(this, R.anim.bottom_to_top)
        binding.cbFavorite.setOnCheckedChangeListener{ _, isChecked ->
            if (!isChecked){
                Log.d("cbFavorite", "onCheckedChanged: $isChecked")
                viewModel.nowPlayingMediaItem.value?.let { nowPlayingSong -> viewModel.updateLikeStatus(nowPlayingSong.mediaId, false) }
            }
            else {
                Log.d("cbFavorite", "onCheckedChanged: $isChecked")
                viewModel.nowPlayingMediaItem.value?.let { nowPlayingSong ->
                    viewModel.updateLikeStatus(nowPlayingSong.mediaId, true)
                    viewModel.updateLikeInNotification(true)
                }
            }
        }
        mayBeRestoreLastPlayedTrackAndQueue()
    }

    private fun mayBeRestoreLastPlayedTrackAndQueue() {
        if (getString(RESTORE_LAST_PLAYED_TRACK_AND_QUEUE_DONE) == DataStoreManager.FALSE) {
            Log.d("Restore", "mayBeRestoreLastPlayedTrackAndQueue: ")
            viewModel.getSaveLastPlayedSong()
            val queue = viewModel.saveLastPlayedSong.switchMap { saved: Boolean ->
                if (saved) {
                    viewModel.from.postValue(viewModel.from_backup)
                    musicSource.reset()
                    viewModel.getSavedSongAndQueue()
                    return@switchMap viewModel.savedQueue
                } else {
                    return@switchMap null
                }
            }
            val result: MediatorLiveData<Pair<List<Track>, Boolean>> = MediatorLiveData<Pair<List<Track>, Boolean>>().apply {
                addSource(queue) {
                    value = Pair(it ?: listOf(), viewModel.isServiceRunning.value ?: false)
                }
                addSource(viewModel.isServiceRunning) {
                    value = Pair(queue.value ?: listOf(), it ?: false)
                }
            }
            result.observe(this) {data ->
                val isMusicServiceRunning = data.second
                val queueData = data.first
                if (queueData.isNotEmpty()) {
                    if (isMusicServiceRunning) {
                        binding.miniplayer.visibility = View.VISIBLE
                        viewModel.restoreLastPLayedTrackDone()
                    }
                    Queue.clear()
                    Queue.addAll(queueData)
                    viewModel.removeSaveQueue()
                    if (!isMyServiceRunning(FetchQueue::class.java)) {
                        startService(
                            Intent(
                                this,
                                FetchQueue::class.java
                            )
                        )
                    } else {
                        stopService(
                            Intent(
                                this,
                                FetchQueue::class.java
                            )
                        )
                        startService(
                            Intent(
                                this,
                                FetchQueue::class.java
                            )
                        )
                    }
                    checkForUpdate()
                }
            }
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        Queue.clear()
        stopService()
        viewModel.isServiceRunning.postValue(false)

        Log.d("Service", "Service destroyed")
    }
    private fun startService() {
        if (viewModel.isServiceRunning.value == false) {
            if (!isMyServiceRunning(SimpleMediaService::class.java)) {
                val intent = Intent(this, SimpleMediaService::class.java)
                startForegroundService(intent)
                viewModel.isServiceRunning.postValue(true)
                Log.d("Service", "Service started")
            }
        }
    }
    private fun stopService(){
        if (viewModel.isServiceRunning.value == true){
            stopService(Intent(this, SimpleMediaService::class.java))
            Log.d("Service", "Service stopped")
            if (this.isMyServiceRunning(FetchQueue:: class.java)){
                stopService(Intent(this, FetchQueue::class.java))
                Log.d("Service", "FetchQueue stopped")
            }
            if (this.isMyServiceRunning(DownloadService:: class.java)){
                this.stopService(Intent(this, DownloadService::class.java))
                viewModel.changeAllDownloadingToError()
                Log.d("Service", "DownloadService stopped")
            }
            viewModel.isServiceRunning.postValue(false)
        }
    }

    override fun onNowPlayingSongChange() {
        viewModel.metadata.observe(this) {
            when(it){
                is Resource.Success -> {
                    binding.songTitle.text = it.data?.title
                    binding.songTitle.isSelected = true
                    if (it.data?.artists != null){
                        var tempArtist = mutableListOf<String>()
                        for (artist in it.data.artists) {
                            tempArtist.add(artist.name)
                        }
                        val artistName = tempArtist.connectArtists()
                        binding.songArtist.text = artistName
                    }
                    binding.songArtist.isSelected = true
                    binding.ivArt.load(it.data?.thumbnails?.get(0)?.url)

                }
                is Resource.Error -> {

                }
            }
        }
    }

    override fun onIsPlayingChange() {
        viewModel.isPlaying.observe(this){
            if (it){
                binding.btPlayPause.setImageResource(R.drawable.baseline_pause_24)
            }else{
                binding.btPlayPause.setImageResource(R.drawable.baseline_play_arrow_24)
            }
        }
    }

    override fun onUpdateProgressBar(progress: Float) {

    }

    fun hideBottomNav(){
        binding.bottomNavigationView.visibility = View.GONE
        binding.miniPlayerContainer.visibility = View.GONE
    }
    fun showBottomNav(){
        binding.bottomNavigationView.visibility = View.VISIBLE
        binding.miniPlayerContainer.visibility = View.VISIBLE
    }

    private fun putString(key: String, value: String) {
        viewModel.putString(key, value)
    }

    private fun getString(key: String): String? {
        return viewModel.getString(key)
    }

}