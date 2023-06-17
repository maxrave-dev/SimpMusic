package com.maxrave.simpmusic.ui

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.view.animation.AnimationUtils
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import coil.load
import com.maxrave.simpmusic.R
import com.maxrave.simpmusic.common.Config
import com.maxrave.simpmusic.data.queue.Queue
import com.maxrave.simpmusic.databinding.ActivityMainBinding
import com.maxrave.simpmusic.service.SimpleMediaService
import com.maxrave.simpmusic.ui.fragment.NowPlayingFragment
import com.maxrave.simpmusic.utils.Resource
import com.maxrave.simpmusic.viewModel.SharedViewModel
import com.maxrave.simpmusic.viewModel.UIEvent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@UnstableApi
@AndroidEntryPoint
class MainActivity : AppCompatActivity(), NowPlayingFragment.OnNowPlayingSongChangeListener {
    private lateinit var binding: ActivityMainBinding
    private val viewModel by viewModels<SharedViewModel>()

    @UnstableApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
        } else {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
        }



        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
//        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v , windowInsets ->
//            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
//            // Apply the insets as a margin to the view. Here the system is setting
//            // only the bottom, left, and right dimensions, but apply whichever insets are
//            // appropriate to your layout. You can also update the view padding
//            // if that's more appropriate.
//            v.updateLayoutParams<MarginLayoutParams>() {
//                bottomMargin = insets.bottom
//            }
//
//            // Return CONSUMED if you don't want want the window insets to keep being
//            // passed down to descendant views.
//            WindowInsetsCompat.CONSUMED
//        }
//        window.statusBarColor = getColor(R.color.colorPrimaryDark)
//        window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        binding.card.visibility = View.GONE
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.fragment_container_view)
        val navController = navHostFragment?.findNavController()
        binding.bottomNavigationView.setupWithNavController(navController!!)

        binding.card.setOnClickListener {
            val bundle = Bundle()
            bundle.putString("type", Config.MINIPLAYER_CLICK)
            navController.navigate(R.id.action_global_nowPlayingFragment, bundle)
        }
        binding.btPlayPause.setOnClickListener {
            viewModel.onUIEvent(UIEvent.PlayPause)
        }
        lifecycleScope.launch {
//            val job3 = launch {
//                viewModel.videoId.observe(this@MainActivity){
//                    if (it != null){
//                        if (viewModel.songTransitions.value){
//                            Log.i("Now Playing Fragment", "Ở Activity")
//                            Log.d("Song Transition", "Song Transition")
//                            viewModel.getMetadata(it)
//                            viewModel.changeSongTransitionToFalse()
//                        }
//                    }
//                }
//            }
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
                        binding.ivArt.load(it.mediaMetadata.artworkUri)
                    }
                }
            }
//            val job1 = launch {
//                viewModel.metadata.observe(this@MainActivity){
//                    if (it is Resource.Success){
//                        if (viewModel.isServiceRunning.value == false){
//                            startService()
//                            viewModel.isServiceRunning.postValue(true)
//                        }
//                    }
//                }
//            }
            val job2 = launch {
                viewModel.progress.collect{
                    binding.progressBar.progress = (it * 100).toInt()
                }
            }
            val job4 = launch {
                viewModel.lyricsBackground.observe(this@MainActivity){
                    if (it != null){
                        binding.card.setCardBackgroundColor(it)
                    }
                }
            }
            //job1.join()
            job2.join()
            //job3.join()
            job4.join()
            job5.join()
        }
        binding.card.animation = AnimationUtils.loadAnimation(this, R.anim.bottom_to_top)

    }
    override fun onDestroy() {
        Queue.clear()
        stopService(Intent(this, SimpleMediaService::class.java))
        viewModel.isServiceRunning.postValue(false)
        super.onDestroy()
        Log.d("Service", "Service destroyed")
    }
    private fun startService() {
        if (viewModel.isServiceRunning.value == false) {
            val intent = Intent(this, SimpleMediaService::class.java)
            startForegroundService(intent)
            viewModel.isServiceRunning.postValue(true)
            Log.d("Service", "Service started")
        }
    }
    private fun stopService(){
        if (viewModel.isServiceRunning.value == true){
            val intent = Intent(this, SimpleMediaService::class.java)
            stopService(intent)
            viewModel.isServiceRunning.postValue(false)
            Log.d("Service", "Service stopped")
        }
    }

    override fun onNowPlayingSongChange() {
        viewModel.metadata.observe(this, Observer {
            when(it){
                is Resource.Success -> {
                    binding.songTitle.text = it.data?.title
                    binding.songTitle.isSelected = true
                    if (it.data?.artists != null){
                        var tempArtist = mutableListOf<String>()
                        for (artist in it.data.artists) {
                            tempArtist.add(artist.name)
                        }
                        val artistName = connectArtists(tempArtist)
                        binding.songArtist.text = artistName
                    }
                    binding.songArtist.isSelected = true
                    viewModel.lyricsBackground.value?.let { it1 ->
                        binding.card.setCardBackgroundColor(
                            it1
                        )
                    }
                    binding.ivArt.load(it.data?.thumbnails?.get(0)?.url)
                }
                is Resource.Error -> {

                }
            }
        })
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
    fun connectArtists(artists: List<String>): String {
        val stringBuilder = StringBuilder()

        for ((index, artist) in artists.withIndex()) {
            stringBuilder.append(artist)

            if (index < artists.size - 1) {
                stringBuilder.append(", ")
            }
        }

        return stringBuilder.toString()
    }

    private fun removeTrailingComma(sentence: String): String {
        val trimmed = sentence.trimEnd()
        return if (trimmed.endsWith(", ")) {
            trimmed.dropLast(2)
        } else {
            trimmed
        }
    }


    private fun removeComma(string: String): String {
        return if (string.endsWith(',')) {
            string.substring(0, string.length - 1)
        } else {
            string
        }
    }
    fun hideBottomNav(){
        binding.bottomNavigationView.visibility = View.GONE
        binding.miniPlayerContainer.visibility = View.GONE
    }
    fun showBottomNav(){
        binding.bottomNavigationView.visibility = View.VISIBLE
        binding.miniPlayerContainer.visibility = View.VISIBLE
    }

}