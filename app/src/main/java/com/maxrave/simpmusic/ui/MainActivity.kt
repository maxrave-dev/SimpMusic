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
            navController.navigate(R.id.action_global_nowPlayingFragment)
        }
        binding.btPlayPause.setOnClickListener {
            viewModel.onUIEvent(UIEvent.PlayPause)
        }
        lifecycleScope.launch {
            val job1 = launch {
                viewModel.metadata.observe(this@MainActivity){
                    if (it is Resource.Success){
                        if (viewModel.isServiceRunning.value == false){
                            startService()
                            viewModel.isServiceRunning.postValue(true)
                        }
                    }
                }
            }
            val job2 = launch {
                viewModel.progress.collect{
                    binding.progressBar.progress = (it * 100).toInt()
                }
            }
            job1.join()
            job2.join()
        }
        binding.card.animation = AnimationUtils.loadAnimation(this, R.anim.bottom_to_top)

    }
    override fun onDestroy() {
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
                    var artistName = ""
                    if (it.data?.artists != null) {
                        for (artist in it.data.artists) {
                            artistName += artist.name + ", "
                        }
                    }
                    artistName = removeTrailingComma(artistName)
                    artistName = removeComma(artistName)
                    binding.songArtist.text = artistName
                    binding.songArtist.isSelected = true
                    viewModel.lyricsBackground.value?.let { it1 ->
                        binding.card.setCardBackgroundColor(
                            it1
                        )
                    }
                    binding.ivArt.load(it.data?.thumbnails?.get(0)?.url)
                }
                is Resource.Loading -> {

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