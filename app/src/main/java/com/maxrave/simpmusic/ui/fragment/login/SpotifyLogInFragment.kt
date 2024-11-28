package com.maxrave.simpmusic.ui.fragment.login

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.media3.common.util.UnstableApi
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.maxrave.simpmusic.R
import com.maxrave.simpmusic.common.Config
import com.maxrave.simpmusic.databinding.FragmentSpotifyLogInBinding
import com.maxrave.simpmusic.extension.isMyServiceRunning
import com.maxrave.simpmusic.service.SimpleMediaService
import com.maxrave.simpmusic.viewModel.LogInViewModel
import com.maxrave.simpmusic.viewModel.SettingsViewModel
import com.maxrave.simpmusic.viewModel.SharedViewModel
import dev.chrisbanes.insetter.applyInsetter
import kotlinx.coroutines.runBlocking

class SpotifyLogInFragment : Fragment() {

    private var _binding: FragmentSpotifyLogInBinding? = null
    val binding get() = _binding!!

    private val viewModel by viewModels<LogInViewModel>()
    private val settingsViewModel by activityViewModels<SettingsViewModel>()
    private val sharedViewModel by activityViewModels<SharedViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentSpotifyLogInBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.topAppBarLayout.applyInsetter {
            type(statusBars = true) {
                margin()
            }
        }
        val activity = requireActivity()
        val bottom = activity.findViewById<BottomNavigationView>(R.id.bottom_navigation_view)
        val miniplayer = activity.findViewById<ComposeView>(R.id.miniplayer)
        bottom.visibility = View.GONE
        miniplayer.visibility = View.GONE
        binding.webView.apply {
            webViewClient = object : WebViewClient() {
                @SuppressLint("FragmentLiveDataObserve")
                override fun onPageFinished(view: WebView?, url: String?) {
                    if (url == Config.SPOTIFY_ACCOUNT_URL) {
                        CookieManager.getInstance().getCookie(url)?.let {
                            viewModel.saveSpotifySpdc(it)
                        }
                        WebStorage.getInstance().deleteAllData()

                        // Clear all the cookies
                        CookieManager.getInstance().removeAllCookies(null)
                        CookieManager.getInstance().flush()

                        binding.webView.clearCache(true)
                        binding.webView.clearFormData()
                        binding.webView.clearHistory()
                        binding.webView.clearSslPreferences()
                        viewModel.spotifyStatus.observe(this@SpotifyLogInFragment) {
                            if (it) {
                                settingsViewModel.setSpotifyLogIn(true)
                                Toast.makeText(
                                    requireContext(),
                                    R.string.login_success,
                                    Toast.LENGTH_SHORT
                                ).show()
                                findNavController().popBackStack()
                            }
                        }
                    }
                }
            }
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            loadUrl(Config.SPOTIFY_LOG_IN_URL)
        }
        binding.topAppBar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
    }

    @UnstableApi
    override fun onDestroyView() {
        super.onDestroyView()
        val activity = requireActivity()
        val bottom = activity.findViewById<BottomNavigationView>(R.id.bottom_navigation_view)
        bottom.animation = AnimationUtils.loadAnimation(requireContext(), R.anim.bottom_to_top)
        bottom.visibility = View.VISIBLE
        val miniplayer = activity.findViewById<ComposeView>(R.id.miniplayer)
        if (requireActivity().isMyServiceRunning(SimpleMediaService::class.java)) {
            miniplayer.animation =
                AnimationUtils.loadAnimation(requireContext(), R.anim.bottom_to_top)
            if (runBlocking { sharedViewModel.nowPlayingState.value?.mediaItem != null }) {
                miniplayer.visibility = View.VISIBLE
            }
        }
    }
}