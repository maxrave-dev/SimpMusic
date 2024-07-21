package com.maxrave.simpmusic.ui

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Rect
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.compositionLocalOf
import androidx.core.net.toUri
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.switchMap
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.DownloadService
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.maxrave.kotlinytmusicscraper.YouTube
import com.maxrave.kotlinytmusicscraper.models.YouTubeLocale
import com.maxrave.simpmusic.R
import com.maxrave.simpmusic.common.Config
import com.maxrave.simpmusic.common.FIRST_TIME_MIGRATION
import com.maxrave.simpmusic.common.SELECTED_LANGUAGE
import com.maxrave.simpmusic.common.SPONSOR_BLOCK
import com.maxrave.simpmusic.common.STATUS_DONE
import com.maxrave.simpmusic.common.SUPPORTED_LANGUAGE
import com.maxrave.simpmusic.common.SUPPORTED_LOCATION
import com.maxrave.simpmusic.data.dataStore.DataStoreManager
import com.maxrave.simpmusic.data.dataStore.DataStoreManager.Settings.RESTORE_LAST_PLAYED_TRACK_AND_QUEUE_DONE
import com.maxrave.simpmusic.data.model.browse.album.Track
import com.maxrave.simpmusic.data.repository.MainRepository
import com.maxrave.simpmusic.databinding.ActivityMainBinding
import com.maxrave.simpmusic.extension.isMyServiceRunning
import com.maxrave.simpmusic.extension.navigateSafe
import com.maxrave.simpmusic.extension.observeOnce
import com.maxrave.simpmusic.service.SimpleMediaService
import com.maxrave.simpmusic.ui.screen.MiniPlayer
import com.maxrave.simpmusic.ui.theme.AppTheme
import com.maxrave.simpmusic.viewModel.SharedViewModel
import dagger.hilt.android.AndroidEntryPoint
import dev.chrisbanes.insetter.applyInsetter
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.CacheControl
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import pub.devrel.easypermissions.EasyPermissions
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

@UnstableApi
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    val viewModel by viewModels<SharedViewModel>()
    private var action: String? = null
    private var data: Uri? = null

    @Inject
    lateinit var dataStoreManager: DataStoreManager

    @Inject
    lateinit var mainRepository: MainRepository

    private val serviceConnection =
        object : ServiceConnection {
            override fun onServiceConnected(
                name: ComponentName?,
                service: IBinder?,
            ) {
                if (service is SimpleMediaService.MusicBinder) {
                    Log.w("MainActivity", "onServiceConnected: ")

                    viewModel.setHandler(service.service.simpleMediaServiceHandler)

                    Log.w("MainActivity", "Now PLaying: ${viewModel.simpleMediaServiceHandler?.player?.currentMediaItem?.mediaMetadata?.title}")
                    if (service.service.simpleMediaServiceHandler.queueData.value == null) {
                        mayBeRestoreLastPlayedTrackAndQueue()
                    }
                    Log.w("TEST", viewModel.simpleMediaServiceHandler?.player.toString())
                }
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                Log.w("MainActivity", "onServiceDisconnected: ")
                viewModel.simpleMediaServiceHandler = null
            }
        }


    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        action = intent.action
        data = intent.data ?: intent.getStringExtra(Intent.EXTRA_TEXT)?.toUri()
        Log.d("MainActivity", "onNewIntent: $data")
        viewModel.intent.value = intent
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    override fun onResume() {
        super.onResume()
        Log.d("MainActivity", "onResume: ")
    }

    @UnstableApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        if (viewModel.simpleMediaServiceHandler == null) {
//            startMusicService()
//        }
        if (viewModel.recreateActivity.value == true) {
            viewModel.simpleMediaServiceHandler?.coroutineScope = lifecycleScope
            viewModel.activityRecreateDone()
        } else {
            startMusicService()
        }
        Log.d("MainActivity", "onCreate: ")
        action = intent.action
        data = intent?.data ?: intent?.getStringExtra(Intent.EXTRA_TEXT)?.toUri()
        if (data != null) {
            viewModel.intent.value = intent
        }
        Log.d("Italy", "Key: ${Locale.ITALY.toLanguageTag()}")

        // Check if the migration has already been done or not
        if (getString(FIRST_TIME_MIGRATION) != STATUS_DONE) {
            Log.d("Locale Key", "onCreate: ${Locale.getDefault().toLanguageTag()}")
            if (SUPPORTED_LANGUAGE.codes.contains(Locale.getDefault().toLanguageTag())) {
                Log.d(
                    "Contains",
                    "onCreate: ${
                        SUPPORTED_LANGUAGE.codes.contains(
                            Locale.getDefault().toLanguageTag(),
                        )
                    }",
                )
                putString(SELECTED_LANGUAGE, Locale.getDefault().toLanguageTag())
                if (SUPPORTED_LOCATION.items.contains(Locale.getDefault().country)) {
                    putString("location", Locale.getDefault().country)
                } else {
                    putString("location", "US")
                }
                YouTube.locale =
                    YouTubeLocale(
                        gl = getString("location") ?: "US",
                        hl = Locale.getDefault().toLanguageTag().substring(0..1),
                    )
            } else {
                putString(SELECTED_LANGUAGE, "en-US")
                YouTube.locale =
                    YouTubeLocale(
                        gl = getString("location") ?: "US",
                        hl = "en-US".substring(0..1),
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
        if (AppCompatDelegate.getApplicationLocales().toLanguageTags() !=
            getString(
                SELECTED_LANGUAGE,
            )
        ) {
            Log.d(
                "Locale Key",
                "onCreate: ${AppCompatDelegate.getApplicationLocales().toLanguageTags()}",
            )
            putString(SELECTED_LANGUAGE, AppCompatDelegate.getApplicationLocales().toLanguageTags())
            YouTube.locale =
                YouTubeLocale(
                    gl = getString("location") ?: "US",
                    hl = AppCompatDelegate.getApplicationLocales().toLanguageTags().substring(0..1),
                )
        }
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//            WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge()
        viewModel.checkIsRestoring()
        viewModel.runWorker()
//        } else {
//            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
//        }

        if (!EasyPermissions.hasPermissions(this, Manifest.permission.POST_NOTIFICATIONS)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                EasyPermissions.requestPermissions(
                    this,
                    getString(R.string.this_app_needs_to_access_your_notification),
                    1,
                    Manifest.permission.POST_NOTIFICATIONS,
                )
            }
        }
        YouTube.cacheControlInterceptor =
            object : Interceptor {
                override fun intercept(chain: Interceptor.Chain): Response {
                    val originalResponse = chain.proceed(chain.request())
                    if (isNetworkAvailable(applicationContext)) {
                        val maxAge = 60 // read from cache for 1 minute
                        return originalResponse.newBuilder()
                            .header("Cache-Control", "public, max-age=$maxAge")
                            .build()
                    } else {
                        val maxStale = 60 * 60 * 24 * 28 // tolerate 4-weeks stale
                        return originalResponse.newBuilder()
                            .header("Cache-Control", "public, only-if-cached, max-stale=$maxStale")
                            .build()
                    }
                }
            }
        YouTube.forceCacheInterceptor =
            Interceptor { chain ->
                val builder: Request.Builder = chain.request().newBuilder()
                if (!isNetworkAvailable(applicationContext)) {
                    builder.cacheControl(CacheControl.FORCE_CACHE)
                }
                chain.proceed(builder.build())
            }
        YouTube.cachePath = File(application.cacheDir, "http-cache")
        viewModel.getLocation()
        viewModel.checkAuth()
        viewModel.checkAllDownloadingSongs()
        runBlocking { delay(500) }

        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.fragment_container_view)
        val navController = navHostFragment?.findNavController()
        binding.miniplayer.setContent {
            AppTheme {
                MiniPlayer(sharedViewModel = viewModel, onClose = { onCloseMiniplayer() }) {
                    val bundle = Bundle()
                    bundle.putString("type", Config.MINIPLAYER_CLICK)
                    navController?.navigateSafe(R.id.action_global_nowPlayingFragment, bundle)
                }
            }
        }
        binding.root.addOnLayoutChangeListener {
                v,
                left,
                top,
                right,
                bottom,
                oldLeft,
                oldTop,
                oldRight,
                oldBottom,
            ->
            val rect = Rect(left, top, right, bottom)
            val oldRect = Rect(oldLeft, oldTop, oldRight, oldBottom)
            if ((rect.width() != oldRect.width() || rect.height() != oldRect.height()) && oldRect !=
                Rect(
                    0,
                    0,
                    0,
                    0,
                )
            ) {
                viewModel.activityRecreate()
            }
        }
        binding.bottomNavigationView.applyInsetter {
            type(navigationBars = true) {
                padding()
            }
        }
        window.navigationBarColor = Color.parseColor("#E80B0A0A")
        if (!isMyServiceRunning(SimpleMediaService::class.java)) {
            binding.miniplayer.visibility = View.GONE
        }
        binding.bottomNavigationView.setupWithNavController(navController!!)
        binding.bottomNavigationView.setOnItemReselectedListener {
            val id = navController.currentDestination?.id
            if (id != R.id.bottom_navigation_item_home && id != R.id.bottom_navigation_item_search && id != R.id.bottom_navigation_item_library) {
                navController.popBackStack(it.itemId, inclusive = false)
            } else if (id == R.id.bottom_navigation_item_home) {
                viewModel.homeRefresh()
            }
        }
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

        navController.addOnDestinationChangedListener { nav, destination, _ ->
            Log.w("Destination", "onCreate: ${destination.id}")
            when (destination.id) {
                R.id.bottom_navigation_item_home, R.id.settingsFragment, R.id.recentlySongsFragment, R.id.moodFragment -> {
                    binding.bottomNavigationView.menu.findItem(
                        R.id.bottom_navigation_item_home,
                    )?.isChecked =
                        true
                }

                R.id.bottom_navigation_item_search -> {
                    binding.bottomNavigationView.menu.findItem(
                        R.id.bottom_navigation_item_search,
                    )?.isChecked =
                        true
                }

                R.id.bottom_navigation_item_library, R.id.downloadedFragment, R.id.mostPlayedFragment, R.id.followedFragment, R.id.favoriteFragment, R.id.localPlaylistFragment -> {
                    binding.bottomNavigationView.menu.findItem(
                        R.id.bottom_navigation_item_library,
                    )?.isChecked =
                        true
                }

                R.id.playlistFragment, R.id.artistFragment, R.id.albumFragment -> {
                    val currentBackStack = nav.previousBackStackEntry?.destination?.id
                    when (currentBackStack) {
                        R.id.bottom_navigation_item_library, R.id.downloadedFragment, R.id.mostPlayedFragment, R.id.followedFragment, R.id.favoriteFragment, R.id.localPlaylistFragment -> {
                            binding.bottomNavigationView.menu.findItem(
                                R.id.bottom_navigation_item_library,
                            )?.isChecked =
                                true
                        }

                        R.id.bottom_navigation_item_search -> {
                            binding.bottomNavigationView.menu.findItem(
                                R.id.bottom_navigation_item_search,
                            )?.isChecked =
                                true
                        }

                        R.id.bottom_navigation_item_home, R.id.settingsFragment, R.id.recentlySongsFragment, R.id.moodFragment -> {
                            binding.bottomNavigationView.menu.findItem(
                                R.id.bottom_navigation_item_home,
                            )?.isChecked =
                                true
                        }
                    }
                }
            }
            if ((destination.id == R.id.nowPlayingFragment || destination.id == R.id.fullscreenFragment ||
                destination.id == R.id.infoFragment || destination.id == R.id.queueFragment) &&
                binding.miniplayer.visibility != View.GONE &&
                binding.bottomNavigationView.visibility != View.GONE
                ) {
                binding.bottomNavigationView.animation = AnimationUtils.loadAnimation(this, R.anim.ttb)
                binding.miniplayer.animation = AnimationUtils.loadAnimation(this, R.anim.ttb)
                binding.bottomNavigationView.visibility = View.GONE
                binding.miniplayer.visibility = View.GONE
            }
            else if (binding.bottomNavigationView.visibility != View.VISIBLE &&
                binding.miniplayer.visibility != View.VISIBLE
                ) {
                lifecycleScope.launch {
                    delay(500)
                    binding.bottomNavigationView.animation = AnimationUtils.loadAnimation(this@MainActivity, R.anim.btt)
                    binding.miniplayer.animation = AnimationUtils.loadAnimation(this@MainActivity, R.anim.btt)
                    binding.miniplayer.visibility = View.VISIBLE
                    binding.bottomNavigationView.visibility = View.VISIBLE
                }
            }
        }

//        binding.miniplayer.showMode = SwipeLayout.ShowMode.PullOut
//        binding.miniplayer.addDrag(SwipeLayout.DragEdge.Right, binding.llBottom)
//        binding.miniplayer.addSwipeListener(
//            object : SwipeLayout.SwipeListener {
//                override fun onStartOpen(layout: SwipeLayout?) {
//                    binding.card.radius = 0f
//                }
//
//                override fun onOpen(layout: SwipeLayout?) {
//                    binding.card.radius = 0f
//                }
//
//                override fun onStartClose(layout: SwipeLayout?) {
//                    binding.card.radius = 12f
//                }
//
//                override fun onClose(layout: SwipeLayout?) {
//                    binding.card.radius = 12f
//                }
//
//                override fun onUpdate(
//                    layout: SwipeLayout?,
//                    leftOffset: Int,
//                    topOffset: Int,
//                ) {
//                    binding.card.radius = 12f
//                }
//
//                override fun onHandRelease(
//                    layout: SwipeLayout?,
//                    xvel: Float,
//                    yvel: Float,
//                ) {
//                }
//            },
//        )
//        binding.btRemoveMiniPlayer.setOnClickListener {
//            viewModel.stopPlayer()
//            viewModel.isServiceRunning.postValue(false)
//            viewModel.videoId.postValue(null)
//            binding.miniplayer.visibility = View.GONE
//            binding.card.radius = 12f
//        }
//        binding.btSkipNext.setOnClickListener {
//            viewModel.onUIEvent(UIEvent.Next)
//            binding.card.radius = 12f
//        }
//
//        binding.card.setOnClickListener {
//            val bundle = Bundle()
//            bundle.putString("type", Config.MINIPLAYER_CLICK)
//            navController.navigateSafe(R.id.action_global_nowPlayingFragment, bundle)
//        }
//        binding.btPlayPause.setOnClickListener {
//            viewModel.onUIEvent(UIEvent.PlayPause)
//        }
        lifecycleScope.launch {
            val job1 =
                launch {
                    viewModel.intent.collectLatest { intent ->
                        if (intent != null) {
                            data = intent.data ?: intent.getStringExtra(Intent.EXTRA_TEXT)?.toUri()
                            Log.d("MainActivity", "onCreate: $data")
                            if (data != null) {
                                if (data == Uri.parse("simpmusic://notification")) {
                                    viewModel.intent.value = null
                                    navController.navigateSafe(
                                        R.id.action_global_notificationFragment,
                                    )
                                } else {
                                    Log.d("MainActivity", "onCreate: $data")
                                    when (val path = data!!.pathSegments.firstOrNull()) {
                                        "playlist" ->
                                            data!!.getQueryParameter("list")
                                                ?.let { playlistId ->
                                                    if (playlistId.startsWith("OLAK5uy_")) {
                                                        viewModel.intent.value = null
                                                        navController.navigateSafe(
                                                            R.id.action_global_albumFragment,
                                                            Bundle().apply {
                                                                putString("browseId", playlistId)
                                                            },
                                                        )
                                                    } else if (playlistId.startsWith("VL")) {
                                                        viewModel.intent.value = null
                                                        navController.navigateSafe(
                                                            R.id.action_global_playlistFragment,
                                                            Bundle().apply {
                                                                putString("id", playlistId)
                                                            },
                                                        )
                                                    } else {
                                                        viewModel.intent.value = null
                                                        navController.navigateSafe(
                                                            R.id.action_global_playlistFragment,
                                                            Bundle().apply {
                                                                putString("id", "VL$playlistId")
                                                            },
                                                        )
                                                    }
                                                }

                                        "channel", "c" ->
                                            data!!.lastPathSegment?.let { artistId ->
                                                if (artistId.startsWith("UC")) {
                                                    viewModel.intent.value = null
                                                    navController.navigateSafe(
                                                        R.id.action_global_artistFragment,
                                                        Bundle().apply {
                                                            putString("channelId", artistId)
                                                        },
                                                    )
                                                } else {
                                                    Toast.makeText(
                                                        this@MainActivity,
                                                        getString(
                                                            R.string.this_link_is_not_supported,
                                                        ),
                                                        Toast.LENGTH_SHORT,
                                                    ).show()
                                                }
//                                    else {
//                                        viewModel.convertNameToId(artistId)
//                                        viewModel.artistId.observe(this@MainActivity) {channelId ->
//                                            when (channelId) {
//                                                is Resource.Success -> {
//                                                    viewModel.intent.value = null
//                                                    navController.navigateSafe(R.id.action_global_artistFragment, Bundle().apply {
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

                                        else ->
                                            when {
                                                path == "watch" -> data!!.getQueryParameter("v")
                                                data!!.host == "youtu.be" -> path
                                                else -> null
                                            }?.let { videoId ->
                                                val args = Bundle()
                                                args.putString("videoId", videoId)
                                                args.putString("from", getString(R.string.shared))
                                                args.putString("type", Config.SHARE)
                                                viewModel.videoId.value = videoId
                                                hideBottomNav()
                                                if (navController.currentDestination?.id == R.id.nowPlayingFragment) {
                                                    findNavController(
                                                        R.id.fragment_container_view,
                                                    ).popBackStack()
                                                    navController.navigateSafe(
                                                        R.id.action_global_nowPlayingFragment,
                                                        args,
                                                    )
                                                } else {
                                                    navController.navigateSafe(
                                                        R.id.action_global_nowPlayingFragment,
                                                        args,
                                                    )
                                                }
                                            }
                                    }
                                }
                            }
                        }
                    }
                }
            val job2 =
                launch {
                    viewModel.sleepTimerState.collect { state ->
                        if (state.isDone) {
                            Log.w("MainActivity", "Collect from main activity $state")
                            viewModel.stopSleepTimer()
                            MaterialAlertDialogBuilder(this@MainActivity)
                                .setTitle(getString(R.string.sleep_timer_off))
                                .setMessage(getString(R.string.good_night))
                                .setPositiveButton(getString(R.string.yes)) { d, _ ->
                                    d.dismiss()
                                }
                                .show()
                        }
                    }
                }
            job1.join()
            job2.join()
        }
        lifecycleScope.launch {
            val miniplayerJob = launch {
                repeatOnLifecycle(Lifecycle.State.CREATED) {
                    viewModel.nowPlayingScreenData.collectLatest {
                        Log.w("MainActivity", "Now Playing: $it")
                        if (navController.currentDestination?.id != R.id.nowPlayingFragment
                            && navController.currentDestination?.id != R.id.infoFragment
                            && navController.currentDestination?.id != R.id.queueFragment
                            && navController.currentDestination?.id != R.id.fullscreenFragment
                            && binding.miniplayer.visibility == View.GONE) {
                            binding.miniplayer.visibility = View.VISIBLE
                        }
                    }
                }
            }
            val job1 =
                launch {
                    viewModel.timeline.collect { timeline ->
                        val progress = (timeline.current / timeline.total).toFloat()
                        if (timeline.total > 0L && !timeline.loading){
                            val skipSegments = viewModel.skipSegments.first()
                            val enabled = viewModel.sponsorBlockEnabled()
                            val listCategory = viewModel.sponsorBlockCategories()
                            if (skipSegments != null && enabled == DataStoreManager.TRUE) {
                                for (skip in skipSegments) {
                                    if (listCategory.contains(skip.category)) {
                                        val firstPart = (skip.segment[0] / skip.videoDuration).toFloat()
                                        val secondPart =
                                            (skip.segment[1] / skip.videoDuration).toFloat()
                                        if (progress in firstPart..secondPart) {
                                            Log.w(
                                                "Seek to",
                                                (skip.segment[1] / skip.videoDuration).toFloat()
                                                    .toString(),
                                            )
                                            viewModel.skipSegment((skip.segment[1] * 1000).toLong())
                                            Toast.makeText(
                                                this@MainActivity,
                                                getString(
                                                    R.string.sponsorblock_skip_segment,
                                                    getString(
                                                        SPONSOR_BLOCK.listName.get(
                                                            SPONSOR_BLOCK.list.indexOf(skip.category),
                                                        ),
                                                    ).lowercase(),
                                                ),
                                                Toast.LENGTH_SHORT,
                                            ).show()
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

            job1.join()
            miniplayerJob.join()
        }
//        binding.card.animation = AnimationUtils.loadAnimation(this, R.anim.bottom_to_top)
//        binding.cbFavorite.setOnClickListener {
//            viewModel.nowPlayingMediaItem.value?.let { nowPlayingSong ->
//                viewModel.updateLikeStatus(
//                    nowPlayingSong.mediaId,
//                    !runBlocking { viewModel.liked.first() },
//                )
//            }
//        }
    }

    private fun isNetworkAvailable(context: Context?): Boolean {
        val connectivityManager = context?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        // Returns a Network object corresponding to
        // the currently active default data network.
        val network = connectivityManager.activeNetwork ?: return false

        // Representation of the capabilities of an active network.
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false

        return when {
            // Indicates this network uses a Wi-Fi transport,
            // or WiFi has network connectivity
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true

            // Indicates this network uses a Cellular transport. or
            // Cellular has network connectivity
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true

            // else return false
            else -> false
        }
    }

    private fun mayBeRestoreLastPlayedTrackAndQueue() {
        if (getString(RESTORE_LAST_PLAYED_TRACK_AND_QUEUE_DONE) == DataStoreManager.FALSE) {
            Log.d("Restore", "mayBeRestoreLastPlayedTrackAndQueue: ")
            viewModel.getSaveLastPlayedSong()
            val queue =
                viewModel.saveLastPlayedSong.switchMap { saved: Boolean ->
                    if (saved) {
                        viewModel.simpleMediaServiceHandler?.reset()
                        viewModel.getSavedSongAndQueue()
                        return@switchMap viewModel.savedQueue
                    } else {
                        return@switchMap null
                    }
                }
            val result: MediatorLiveData<List<Track>> =
                MediatorLiveData<List<Track>>().apply {
                    addSource(queue) {
                        value = it ?: listOf()
                    }
                }
            binding.miniplayer.visibility = View.GONE
            result.observeOnce(this) { data ->
                val queueDataList = data
                Log.w("Check queue saved", queueDataList.toString())
                binding.miniplayer.visibility = View.VISIBLE
                if (queueDataList.isNotEmpty()) {
                    Log.w("Check queue saved", queueDataList.toString())
                    with(viewModel.simpleMediaServiceHandler) {
                        runBlocking {
                            this@with?.queueData?.first()?.addTrackList(
                                queueDataList
                            )
                        }?.let {
                            this?.setQueueData(
                                it
                            )
                        }
                    }
                    viewModel.removeSaveQueue()
                    viewModel.addQueueToPlayer()
                    checkForUpdate()
                }
            }
        } else {
            binding.miniplayer.visibility = View.GONE
            checkForUpdate()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
//        stopService()
        Log.w("MainActivity", "onDestroy: ")
    }

    override fun onRestart() {
        super.onRestart()
        viewModel.activityRecreate()
    }

    private fun startMusicService() {
        println("go to StartMusicService")
        if (viewModel.recreateActivity.value != true) {
            val intent = Intent(this, SimpleMediaService::class.java)
            startService(intent)
            bindService(intent, serviceConnection, BIND_AUTO_CREATE)
            viewModel.isServiceRunning.value = true
            Log.d("Service", "Service started")
        }
    }

    private fun stopService() {
        if (viewModel.recreateActivity.value != true) {
            viewModel.isServiceRunning.value = false
            viewModel.simpleMediaServiceHandler?.mayBeSaveRecentSong()
            viewModel.simpleMediaServiceHandler?.mayBeSavePlaybackState()
            viewModel.simpleMediaServiceHandler?.release()
            viewModel.simpleMediaServiceHandler = null
            unbindService(serviceConnection)
            Log.d("Service", "Service stopped")
            if (this.isMyServiceRunning(DownloadService::class.java)) {
                stopService(Intent(this, DownloadService::class.java))
                viewModel.changeAllDownloadingToError()
                Log.d("Service", "DownloadService stopped")
            }
        }
    }

//    override fun onNowPlayingSongChange() {
//        viewModel.metadata.observe(this) {
//            when(it){
//                is Resource.Success -> {
//                    binding.songTitle.text = it.data?.title
//                    binding.songTitle.isSelected = true
//                    if (it.data?.artists != null){
//                        var tempArtist = mutableListOf<String>()
//                        for (artist in it.data.artists) {
//                            tempArtist.add(artist.name)
//                        }
//                        val artistName = tempArtist.connectArtists()
//                        binding.songArtist.text = artistName
//                    }
//                    binding.songArtist.isSelected = true
//                    binding.ivArt.load(it.data?.thumbnails?.get(0)?.url)
//
//                }
//                is Resource.Error -> {
//
//                }
//            }
//        }
//    }
//
//    override fun onIsPlayingChange() {
//        viewModel.isPlaying.observe(this){
//            if (it){
//                binding.btPlayPause.setImageResource(R.drawable.baseline_pause_24)
//            }else{
//                binding.btPlayPause.setImageResource(R.drawable.baseline_play_arrow_24)
//            }
//        }
//    }
//
//    override fun onUpdateProgressBar(progress: Float) {
//
//    }

    fun hideBottomNav() {
        binding.bottomNavigationView.visibility = View.GONE
        binding.miniplayer.visibility = View.GONE
    }

    fun showBottomNav() {
        binding.bottomNavigationView.visibility = View.VISIBLE
        binding.miniPlayerContainer.visibility = View.VISIBLE
    }

    private fun checkForUpdate() {
        viewModel.checkForUpdate()
        viewModel.githubResponse.observe(this) { response ->
            if (response != null) {
                if (response.tagName != getString(R.string.version_name)) {
                    val inputFormat =
                        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
                    val outputFormat = SimpleDateFormat("dd MMM yyyy HH:mm:ss", Locale.getDefault())
                    val formatted =
                        response.publishedAt?.let { input ->
                            inputFormat.parse(input)
                                ?.let { outputFormat.format(it) }
                        }

                    MaterialAlertDialogBuilder(this)
                        .setTitle(getString(R.string.update_available))
                        .setMessage(
                            getString(
                                R.string.update_message,
                                response.tagName,
                                formatted,
                                response.body,
                            ),
                        )
                        .setPositiveButton(getString(R.string.download)) { _, _ ->
                            val browserIntent =
                                Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse(response.assets?.firstOrNull()?.browserDownloadUrl),
                                )
                            startActivity(browserIntent)
                        }
                        .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                            dialog.dismiss()
                        }
                        .show()
                }
            }
        }
    }

    private fun putString(
        key: String,
        value: String,
    ) {
        viewModel.putString(key, value)
    }

    private fun getString(key: String): String? {
        return viewModel.getString(key)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        viewModel.activityRecreate()
    }

    private fun onCloseMiniplayer() {
        viewModel.stopPlayer()
        viewModel.isServiceRunning.postValue(false)
        viewModel.videoId.postValue(null)
        binding.miniplayer.visibility = View.GONE
    }
}

val LocalPlayerAwareWindowInsets =
    compositionLocalOf<WindowInsets> { error("No WindowInsets provided") }