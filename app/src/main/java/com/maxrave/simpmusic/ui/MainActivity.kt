package com.maxrave.simpmusic.ui

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.res.Configuration
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.text.util.Linkify
import android.util.Log
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import android.view.animation.AnimationUtils
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.res.ResourcesCompat
import androidx.core.net.toUri
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.maxrave.simpmusic.R
import com.maxrave.simpmusic.common.Config
import com.maxrave.simpmusic.common.FIRST_TIME_MIGRATION
import com.maxrave.simpmusic.common.SELECTED_LANGUAGE
import com.maxrave.simpmusic.common.STATUS_DONE
import com.maxrave.simpmusic.common.SUPPORTED_LANGUAGE
import com.maxrave.simpmusic.common.SUPPORTED_LOCATION
import com.maxrave.simpmusic.data.dataStore.DataStoreManager
import com.maxrave.simpmusic.databinding.ActivityMainBinding
import com.maxrave.simpmusic.extension.isMyServiceRunning
import com.maxrave.simpmusic.extension.markdownToHtml
import com.maxrave.simpmusic.extension.navigateSafe
import com.maxrave.simpmusic.service.SimpleMediaService
import com.maxrave.simpmusic.ui.screen.MiniPlayer
import com.maxrave.simpmusic.ui.theme.AppTheme
import com.maxrave.simpmusic.utils.VersionManager
import com.maxrave.simpmusic.viewModel.SharedViewModel
import dev.chrisbanes.insetter.applyInsetter
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import pub.devrel.easypermissions.EasyPermissions
import java.text.SimpleDateFormat
import java.util.Locale

@UnstableApi
@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    val viewModel by viewModels<SharedViewModel>()
    private var action: String? = null
    private var data: Uri? = null

    private val serviceConnection =
        object : ServiceConnection {
            override fun onServiceConnected(
                name: ComponentName?,
                service: IBinder?,
            ) {
                if (service is SimpleMediaService.MusicBinder) {
                    Log.w("MainActivity", "onServiceConnected: ")
                }
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                Log.w("MainActivity", "onServiceDisconnected: ")
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
        VersionManager.initialize(applicationContext)
        checkForUpdate()
        if (viewModel.recreateActivity.value == true) {
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
            } else {
                putString(SELECTED_LANGUAGE, "en-US")
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
        }
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//            WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge(
            navigationBarStyle =
                SystemBarStyle.auto(
                    lightScrim = Color.Transparent.toArgb(),
                    darkScrim = Color.Transparent.toArgb(),
                ),
        )
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
        viewModel.getLocation()
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
        if (viewModel.nowPlayingState.value?.mediaItem == MediaItem.EMPTY || viewModel.nowPlayingState.value?.mediaItem == null) {
            binding.miniplayer.visibility = View.GONE
        }
        binding.root.addOnLayoutChangeListener {
            _,
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
            if ((rect.width() != oldRect.width() || rect.height() != oldRect.height()) &&
                oldRect !=
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
                    binding.bottomNavigationView.menu
                        .findItem(
                            R.id.bottom_navigation_item_home,
                        )?.isChecked =
                        true
                }

                R.id.bottom_navigation_item_search -> {
                    binding.bottomNavigationView.menu
                        .findItem(
                            R.id.bottom_navigation_item_search,
                        )?.isChecked =
                        true
                }

                R.id.bottom_navigation_item_library,
                R.id.favoriteFragment, R.id.localPlaylistFragment,
                -> {
                    binding.bottomNavigationView.menu
                        .findItem(
                            R.id.bottom_navigation_item_library,
                        )?.isChecked =
                        true
                }

                R.id.playlistFragment, R.id.artistFragment, R.id.albumFragment -> {
                    val currentBackStack = nav.previousBackStackEntry?.destination?.id
                    when (currentBackStack) {
                        R.id.bottom_navigation_item_library,
                        R.id.favoriteFragment, R.id.localPlaylistFragment,
                        -> {
                            binding.bottomNavigationView.menu
                                .findItem(
                                    R.id.bottom_navigation_item_library,
                                )?.isChecked =
                                true
                        }

                        R.id.bottom_navigation_item_search -> {
                            binding.bottomNavigationView.menu
                                .findItem(
                                    R.id.bottom_navigation_item_search,
                                )?.isChecked =
                                true
                        }

                        R.id.bottom_navigation_item_home, R.id.settingsFragment, R.id.recentlySongsFragment, R.id.moodFragment -> {
                            binding.bottomNavigationView.menu
                                .findItem(
                                    R.id.bottom_navigation_item_home,
                                )?.isChecked =
                                true
                        }
                    }
                }
            }
            Log.w("MainActivity", "Destination: ${destination.label}")
            Log.w("MainActivity", "Show or Hide: ${viewModel.showOrHideMiniplayer}")
            if (
                (
                    listOf(
                        "NowPlayingFragment",
                        "FullscreenFragment",
                        "InfoFragment",
                        "QueueFragment",
                        "SpotifyLogInFragment",
                        "fragment_log_in",
                        "MusixmatchFragment",
                    )
                ).contains(destination.label)
            ) {
                lifecycleScope.launch { viewModel.showOrHideMiniplayer.emit(false) }
                Log.w("MainActivity", "onCreate: HIDE MINIPLAYER")
            } else {
                lifecycleScope.launch { viewModel.showOrHideMiniplayer.emit(true) }
                Log.w("MainActivity", "onCreate: SHOW MINIPLAYER")
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
                                            data!!
                                                .getQueryParameter("list")
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
                                                    Toast
                                                        .makeText(
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
                                                viewModel.loadSharedMediaItem(videoId)
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
                                }.show()
                        }
                    }
                }
            job1.join()
            job2.join()
        }
        lifecycleScope.launch {
            val miniplayerJob =
                launch {
                    repeatOnLifecycle(Lifecycle.State.CREATED) {
                        viewModel.nowPlayingScreenData.collect {
                            Log.w("MainActivity", "Current Destination: ${navController.currentDestination?.label}")
                            if (!(
                                    listOf(
                                        "NowPlayingFragment",
                                        "FullscreenFragment",
                                        "InfoFragment",
                                        "QueueFragment",
                                        "SpotifyLogInFragment",
                                        "fragment_log_in",
                                        "MusixmatchFragment",
                                    )
                                ).contains(navController.currentDestination?.label) &&
                                it.nowPlayingTitle.isNotEmpty() &&
                                binding.miniplayer.visibility != View.VISIBLE
                            ) {
                                Log.w("MainActivity", "Show Miniplayer")
                                binding.miniplayer.animation =
                                    AnimationUtils.loadAnimation(
                                        this@MainActivity,
                                        R.anim.slide_from_right,
                                    )
                                binding.miniplayer.visibility = View.VISIBLE
                            }
                        }
                    }
                }

            val showHideJob =
                launch {
                    repeatOnLifecycle(Lifecycle.State.RESUMED) {
                        viewModel.showOrHideMiniplayer.collectLatest {
                            if (it &&
                                binding.miniplayer.visibility != View.VISIBLE &&
                                binding.bottomNavigationView.visibility != View.VISIBLE &&
                                viewModel.nowPlayingState.value?.isNotEmpty() == true
                            ) {
                                Log.w("MainActivity", "Show Miniplayer")
                                lifecycleScope.launch {
                                    delay(500)
                                    binding.bottomNavigationView.animation = AnimationUtils.loadAnimation(this@MainActivity, R.anim.btt)
                                    binding.miniplayer.animation = AnimationUtils.loadAnimation(this@MainActivity, R.anim.btt)
                                    binding.miniplayer.visibility = View.VISIBLE
                                    binding.bottomNavigationView.visibility = View.VISIBLE
                                }
                            } else if (binding.bottomNavigationView.visibility != View.GONE &&
                                binding.miniplayer.visibility != View.GONE &&
                                !it
                            ) {
                                binding.bottomNavigationView.animation = AnimationUtils.loadAnimation(this@MainActivity, R.anim.ttb)
                                binding.miniplayer.animation = AnimationUtils.loadAnimation(this@MainActivity, R.anim.ttb)
                                binding.bottomNavigationView.visibility = View.GONE
                                binding.miniplayer.visibility = View.GONE
                            }
                        }
                    }
                }
            val bottomNavBarJob =
                launch {
                    repeatOnLifecycle(Lifecycle.State.CREATED) {
                        viewModel.getTranslucentBottomBar().distinctUntilChanged().collectLatest {
                            if (it == DataStoreManager.TRUE) {
                                binding.bottomNavigationView.background =
                                    ResourcesCompat.getDrawable(resources, R.drawable.transparent_rect, null)?.apply {
                                        this.setDither(true)
                                    }
                            } else if (it == DataStoreManager.FALSE) {
                                binding.bottomNavigationView.background =
                                    ColorDrawable(ResourcesCompat.getColor(resources, R.color.md_theme_dark_background, null))
                            }
                        }
                    }
                }

            miniplayerJob.join()
            showHideJob.join()
            bottomNavBarJob.join()
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

    private fun checkForUpdate() {
        if (viewModel.shouldCheckForUpdate()) {
            viewModel.checkForUpdate()
            viewModel.githubResponse.observe(this) { response ->
                if (response != null && !this.isInPictureInPictureMode && !viewModel.showedUpdateDialog) {
                    Log.w("MainActivity", "Check for update")
                    Log.w("MainActivity", "Current version: ${getString(R.string.version_format, VersionManager.getVersionName())}")
                    if (response.tagName != getString(R.string.version_format, VersionManager.getVersionName())) {
                        viewModel.showedUpdateDialog = true
                        val inputFormat =
                            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
                        val outputFormat = SimpleDateFormat("dd MMM yyyy HH:mm:ss", Locale.getDefault())
                        val formatted =
                            response.publishedAt?.let { input ->
                                inputFormat
                                    .parse(input)
                                    ?.let { outputFormat.format(it) }
                            }
                        val scrollView =
                            ScrollView(this)
                                .apply {
                                    layoutParams =
                                        LinearLayout.LayoutParams(
                                            LinearLayout.LayoutParams.MATCH_PARENT,
                                            LinearLayout.LayoutParams.WRAP_CONTENT,
                                        )
                                }
                        val layout =
                            LinearLayout(this).apply {
                                orientation = LinearLayout.VERTICAL
                                layoutParams =
                                    LinearLayout.LayoutParams(
                                        LinearLayout.LayoutParams.MATCH_PARENT,
                                        LinearLayout.LayoutParams.WRAP_CONTENT,
                                    )
                                setPadding(24, 24, 24, 12)
                            }
                        layout.addView(
                            TextView(this).apply {
                                text =
                                    getString(
                                        R.string.update_message,
                                        response.tagName,
                                        formatted,
                                        "",
                                    )
                                textSize = 13f
                                layoutParams =
                                    MarginLayoutParams(
                                        MarginLayoutParams.MATCH_PARENT,
                                        MarginLayoutParams.WRAP_CONTENT,
                                    ).apply {
                                        setMargins(42, 8, 42, 0)
                                    }
                            },
                        )
                        layout.addView(
                            TextView(this).apply {
                                text = markdownToHtml(response.body ?: "")
                                textSize = 13f
                                autoLinkMask = Linkify.ALL
                                setLineSpacing(0f, 1.2f)
                                layoutParams =
                                    MarginLayoutParams(
                                        MarginLayoutParams.MATCH_PARENT,
                                        MarginLayoutParams.WRAP_CONTENT,
                                    ).apply {
                                        setMargins(42, 0, 42, 24)
                                    }
                            },
                        )
                        scrollView.addView(layout)

                        MaterialAlertDialogBuilder(this)
                            .setTitle(getString(R.string.update_available))
                            .setView(scrollView)
                            .setPositiveButton(getString(R.string.download)) { _, _ ->
                                val browserIntent =
                                    Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse(response.assets?.firstOrNull()?.browserDownloadUrl),
                                    )
                                startActivity(browserIntent)
                            }.setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                                dialog.dismiss()
                            }.show()
                    }
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

    private fun getString(key: String): String? = viewModel.getString(key)

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