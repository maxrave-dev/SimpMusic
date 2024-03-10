package com.maxrave.simpmusic.ui.fragment.home

import android.app.usage.StorageStatsManager
import android.content.Intent
import android.media.audiofx.AudioEffect
import android.net.Uri
import android.os.Bundle
import android.os.storage.StorageManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.os.LocaleListCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import coil.annotation.ExperimentalCoilApi
import com.google.android.flexbox.FlexboxLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.slider.Slider
import com.maxrave.simpmusic.R
import com.maxrave.simpmusic.adapter.account.AccountAdapter
import com.maxrave.simpmusic.common.LIMIT_CACHE_SIZE
import com.maxrave.simpmusic.common.LYRICS_PROVIDER
import com.maxrave.simpmusic.common.QUALITY
import com.maxrave.simpmusic.common.SPONSOR_BLOCK
import com.maxrave.simpmusic.common.SUPPORTED_LANGUAGE
import com.maxrave.simpmusic.common.SUPPORTED_LOCATION
import com.maxrave.simpmusic.common.VIDEO_QUALITY
import com.maxrave.simpmusic.data.dataStore.DataStoreManager
import com.maxrave.simpmusic.databinding.FragmentSettingsBinding
import com.maxrave.simpmusic.databinding.YoutubeAccountDialogBinding
import com.maxrave.simpmusic.extension.navigateSafe
import com.maxrave.simpmusic.extension.setEnabledAll
import com.maxrave.simpmusic.viewModel.SettingsViewModel
import com.maxrave.simpmusic.viewModel.SharedViewModel
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.LibsBuilder
import dagger.hilt.android.AndroidEntryPoint
import dev.chrisbanes.insetter.applyInsetter
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.Scanner


@UnstableApi
@AndroidEntryPoint
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private val viewModel by activityViewModels<SettingsViewModel>()
    private val sharedViewModel by activityViewModels<SharedViewModel>()

    private val backupLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        if (uri != null) {
            viewModel.backup(requireContext(), uri)
        }
    }
    private val restoreLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            viewModel.restore(requireContext(), uri)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        binding.topAppBarLayout.applyInsetter {
            type(statusBars = true){
                margin()
            }
        }
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        viewModel.getLoggedIn()
    }

    private var resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {}

    @OptIn(ExperimentalCoilApi::class)
    @UnstableApi
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.getLocation()
        viewModel.getLanguage()
        viewModel.getQuality()
        viewModel.getPlayerCacheSize()
        viewModel.getDownloadedCacheSize()
        viewModel.getPlayerCacheLimit()
        viewModel.getLoggedIn()
        viewModel.getNormalizeVolume()
        viewModel.getSkipSilent()
        viewModel.getSavedPlaybackState()
        viewModel.getSendBackToGoogle()
        viewModel.getSaveRecentSongAndQueue()
        viewModel.getLastCheckForUpdate()
        viewModel.getSponsorBlockEnabled()
        viewModel.getSponsorBlockCategories()
        viewModel.getTranslationLanguage() //
        viewModel.getLyricsProvider() //
        viewModel.getUseTranslation() //
        viewModel.getMusixmatchLoggedIn() //
        viewModel.getHomeLimit()
        viewModel.getPlayVideoInsteadOfAudio()
        viewModel.getVideoQuality()
        viewModel.getThumbCacheSize()
        viewModel.getSpotifyLogIn()
        viewModel.getSpotifyLyrics()
        viewModel.getSpotifyCanvas()

        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.CREATED) {
                val job1 = launch {
                    viewModel.musixmatchLoggedIn.collect {
                        if (it != null) {
                            if (it == DataStoreManager.TRUE) {
                                binding.tvMusixmatchLoginTitle.text =
                                    getString(R.string.log_out_from_musixmatch)
                                binding.tvMusixmatchLogin.text = getString(R.string.logged_in)
                                setEnabledAll(binding.swUseMusixmatchTranslation, true)
                                setEnabledAll(binding.btTranslationLanguage, true)
                            } else if (it == DataStoreManager.FALSE) {
                                binding.tvMusixmatchLoginTitle.text =
                                    getString(R.string.log_in_to_Musixmatch)
                                binding.tvMusixmatchLogin.text =
                                    getString(R.string.only_support_email_and_password_type)
                                setEnabledAll(binding.swUseMusixmatchTranslation, false)
                                setEnabledAll(binding.btTranslationLanguage, false)
                            }
                        }
                    }
                }
                val job2 = launch {
                    viewModel.playVideoInsteadOfAudio.collect {
                        if (it == DataStoreManager.TRUE) {
                            binding.swEnableVideo.isChecked = true
                            setEnabledAll(binding.btVideoQuality, true)
                        } else if (it == DataStoreManager.FALSE) {
                            binding.swEnableVideo.isChecked = false
                            setEnabledAll(binding.btVideoQuality, false)
                        }
                    }
                }

                val job3 = launch {
                    viewModel.videoQuality.collect {
                        binding.tvVideoQuality.text = it
                    }
                }
                val job4 = launch {
                    viewModel.mainLyricsProvider.collect {
                        if (it == DataStoreManager.YOUTUBE) {
                            binding.tvMainLyricsProvider.text = LYRICS_PROVIDER.items.get(1)
                        } else if (it == DataStoreManager.MUSIXMATCH) {
                            binding.tvMainLyricsProvider.text = LYRICS_PROVIDER.items.get(0)
                        }
                    }
                }
                val job5 = launch {
                    viewModel.translationLanguage.collect {
                        binding.tvTranslationLanguage.text = it
                    }
                }
                val job6 = launch {
                    viewModel.useTranslation.collect {
                        binding.swUseMusixmatchTranslation.isChecked = it == DataStoreManager.TRUE
                    }
                }
                val job7 = launch {
                    viewModel.sendBackToGoogle.collect {
                        binding.swSaveHistory.isChecked = it == DataStoreManager.TRUE
                    }
                }
                val job8 = launch {
                    viewModel.location.collect {
                        binding.tvContentCountry.text = it
                    }
                }
                val job9 = launch {
                    viewModel.language.collect {
                        Log.w("Language", it.toString())
                        if (it != null) {
                            if (it.contains("id") || it.contains("in")) {
                                binding.tvLanguage.text = "Bahasa Indonesia"
                            } else {
                                val temp =
                                    SUPPORTED_LANGUAGE.items.getOrNull(
                                        SUPPORTED_LANGUAGE.codes.indexOf(
                                            it
                                        )
                                    )
                                binding.tvLanguage.text = temp
                            }
                        } else {
                            binding.tvLanguage.text = "Automatic"
                        }
                    }
                }
                val job10 = launch {
                    viewModel.quality.collect {
                        binding.tvQuality.text = it
                    }
                }
                val job11 = launch {
                    viewModel.cacheSize.collect {
                        if (it != null) {
                            drawDataStat()
                            binding.tvPlayerCache.text =
                                getString(R.string.cache_size, bytesToMB(it).toString())
                        }
                    }
                }
                val job12 = launch {
                    viewModel.downloadedCacheSize.collect {
                        if (it != null) {
                            drawDataStat()
                            binding.tvDownloadedCache.text =
                                getString(R.string.cache_size, bytesToMB(it).toString())
                        }
                    }
                }
                val job13 = launch {
                    viewModel.normalizeVolume.collect {
                        binding.swNormalizeVolume.isChecked = it == DataStoreManager.TRUE
                    }
                }
                val job14 = launch {
                    viewModel.skipSilent.collect {
                        binding.swSkipSilent.isChecked = it == DataStoreManager.TRUE
                    }
                }
                val job15 = launch {
                    viewModel.savedPlaybackState.collect {
                        binding.swSavePlaybackState.isChecked = it == DataStoreManager.TRUE
                    }
                }
                val job16 = launch {
                    viewModel.saveRecentSongAndQueue.collect {
                        binding.swSaveLastPlayed.isChecked = it == DataStoreManager.TRUE
                    }
                }
                val job17 = launch {
                    viewModel.saveRecentSongAndQueue.collect {
                        binding.swSaveLastPlayed.isChecked = it == DataStoreManager.TRUE
                    }
                }
                val job18 = launch {
                    viewModel.sponsorBlockEnabled.collect {
                        binding.swEnableSponsorBlock.isChecked = it == DataStoreManager.TRUE
                    }
                }
                val job19 = launch {
                    viewModel.lastCheckForUpdate.collect {
                        if (it != null) {
                            binding.tvCheckForUpdate.text = getString(
                                R.string.last_checked_at,
                                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                                    .withZone(ZoneId.systemDefault())
                                    .format(Instant.ofEpochMilli(it.toLong()))
                            )
                        }
                    }
                }
                val job20 = launch {
                    viewModel.playerCacheLimit.collect {
                        binding.tvLimitPlayerCache.text =
                            if (it != -1) "$it MB" else getString(R.string.unlimited)
                    }
                }
                val job21 = launch {
                    viewModel.githubResponse.collect { response ->
                        if (response != null) {
                            if (response.tagName != getString(R.string.version_name)) {
                                binding.tvCheckForUpdate.text = getString(
                                    R.string.last_checked_at,
                                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                                        .withZone(ZoneId.systemDefault())
                                        .format(Instant.ofEpochMilli(System.currentTimeMillis()))
                                )
                                val inputFormat =
                                    SimpleDateFormat(
                                        "yyyy-MM-dd'T'HH:mm:ss'Z'",
                                        Locale.getDefault()
                                    )
                                val outputFormat =
                                    SimpleDateFormat("dd MMM yyyy HH:mm:ss", Locale.getDefault())
                                val formatted = response.publishedAt?.let { input ->
                                    inputFormat.parse(input)
                                        ?.let { outputFormat.format(it) }
                                }
                                MaterialAlertDialogBuilder(requireContext())
                                    .setTitle(getString(R.string.update_available))
                                    .setMessage(
                                        getString(
                                            R.string.update_message,
                                            response.tagName,
                                            formatted,
                                            response.body
                                        )
                                    )
                                    .setPositiveButton(getString(R.string.download)) { _, _ ->
                                        val browserIntent = Intent(
                                            Intent.ACTION_VIEW,
                                            Uri.parse(response.assets?.firstOrNull()?.browserDownloadUrl)
                                        )
                                        startActivity(browserIntent)
                                    }
                                    .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                                        dialog.dismiss()
                                    }
                                    .show()
                            } else {
                                Toast.makeText(
                                    requireContext(),
                                    getString(R.string.no_update),
                                    Toast.LENGTH_SHORT
                                ).show()
                                viewModel.getLastCheckForUpdate()
                                viewModel.lastCheckForUpdate.collect {
                                    if (it != null) {
                                        binding.tvCheckForUpdate.text = getString(
                                            R.string.last_checked_at,
                                            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                                                .withZone(ZoneId.systemDefault())
                                                .format(Instant.ofEpochMilli(it.toLong()))
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                val job22 = launch {
                    viewModel.thumbCacheSize.collect {
                        binding.tvThumbnailCache.text = getString(
                            R.string.cache_size, if (it != null) {
                                bytesToMB(it)
                            } else {
                                0
                            }.toString()
                        )
                    }
                }

                val job23 = launch {
                    viewModel.spotifyLogIn.collect {
                        if (it) {
                            binding.tvSpotifyLogin.text = getString(R.string.logged_in)
                            setEnabledAll(binding.btEnableCanvas, true)
                            setEnabledAll(binding.btEnableSpotifyLyrics, true)
                        } else {
                            binding.tvSpotifyLogin.text = getString(R.string.intro_login_to_spotify)
                            setEnabledAll(binding.btEnableCanvas, false)
                            setEnabledAll(binding.btEnableSpotifyLyrics, false)
                        }
                    }
                }

                val job24 = launch {
                    viewModel.spotifyLyrics.collect {
                        if (it) {
                            binding.swEnableSpotifyLyrics.isChecked = true
                        } else {
                            binding.swEnableSpotifyLyrics.isChecked = false
                        }
                    }
                }

                val job25 = launch {
                    viewModel.spotifyCanvas.collect {
                        if (it) {
                            binding.swEnableCanvas.isChecked = true
                        } else {
                            binding.swEnableCanvas.isChecked = false
                        }
                    }
                }
                val job26 = launch {
                    viewModel.homeLimit.collect {
                        binding.tvHomeLimit.text = it.toString()
                        if (it != null) {
                            binding.sliderHomeLimit.value = it.toFloat()
                        }
                    }
                }
                job1.join()
                job2.join()
                job3.join()
                job4.join()
                job5.join()
                job6.join()
                job7.join()
                job8.join()
                job9.join()
                job10.join()
                job11.join()
                job12.join()
                job13.join()
                job14.join()
                job15.join()
                job16.join()
                job17.join()
                job18.join()
                job19.join()
                job20.join()
                job21.join()
                job22.join()
                job23.join()
                job24.join()
                job25.join()
                job26.join()
            }
        }
        binding.sliderHomeLimit.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) {
                // Responds to when slider's touch event is being started
            }

            override fun onStopTrackingTouch(slider: Slider) {
                viewModel.setHomeLimit(slider.value.toInt())
            }
        })
        binding.btLimitPlayerCache.setOnClickListener {
            var checkedIndex = -1
            val dialog = MaterialAlertDialogBuilder(requireContext())
                .setSingleChoiceItems(LIMIT_CACHE_SIZE.items, -1) { _, which ->
                    checkedIndex = which
                }
                .setTitle(getString(R.string.limit_player_cache))
                .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                    dialog.dismiss()
                }
                .setPositiveButton(getString(R.string.change)) { dialog, _ ->
                    if (checkedIndex != -1) {
                        viewModel.setPlayerCacheLimit(LIMIT_CACHE_SIZE.data[checkedIndex])
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.restart_app),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    dialog.dismiss()
                }
            dialog.show()
        }
        binding.btYouTubeAccount.setOnClickListener {
            val dialog = MaterialAlertDialogBuilder(requireContext())
            val accountBinding =
                YoutubeAccountDialogBinding.inflate(LayoutInflater.from(requireContext()))
            val accountAdapter = AccountAdapter(arrayListOf())
            dialog.setView(accountBinding.root)
            val alertDialog = dialog.create()
            accountBinding.rvAccount.apply {
                adapter = accountAdapter
                layoutManager = LinearLayoutManager(requireContext())
            }
            accountAdapter.setOnAccountClickListener(object :
                AccountAdapter.OnAccountClickListener {
                override fun onAccountClick(pos: Int) {
                    Log.w("Account", accountAdapter.getAccountList().getOrNull(pos).toString())
                    if (accountAdapter.getAccountList().getOrNull(pos) != null) {
                        viewModel.setUsedAccount(accountAdapter.getAccountList().get(pos))
                    }
                }
            })
            accountBinding.btAddAccount.setOnClickListener {
                findNavController().navigateSafe(R.id.action_global_logInFragment)
                alertDialog.dismiss()
            }
            accountBinding.btClose.setOnClickListener {
                alertDialog.dismiss()
            }
            accountBinding.btGuest.setOnClickListener {
                viewModel.setUsedAccount(null)
                alertDialog.dismiss()
            }
            accountBinding.btLogOut.setOnClickListener {
                val subAlertDialogBuilder = MaterialAlertDialogBuilder(requireContext())
                    .setTitle(getString(R.string.warning))
                    .setMessage(getString(R.string.log_out_warning))
                    .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                        dialog.dismiss()
                    }
                    .setPositiveButton(getString(R.string.log_out)) { dialog, _ ->
                        viewModel.logOutAllYouTube()
                    }
                subAlertDialogBuilder.show()
            }
            viewModel.getAllGoogleAccount()
            accountBinding.loadingLayout.visibility = View.VISIBLE
            lifecycleScope.launch {
                val job2 = launch {
                    viewModel.loading.collectLatest {
                        if (it) {
                            accountBinding.loadingLayout.visibility = View.VISIBLE
                            accountBinding.apply {
                                setEnabledAll(btAddAccount, false)
                                setEnabledAll(btLogOut, false)
                                setEnabledAll(btGuest, false)
                            }
                        } else {
                            accountBinding.loadingLayout.visibility = View.GONE
                            accountBinding.apply {
                                setEnabledAll(btAddAccount, true)
                                setEnabledAll(btLogOut, true)
                                setEnabledAll(btGuest, true)
                            }
                        }
                    }
                }
                val job1 = launch {
                    viewModel.googleAccounts.collect {
                        if (it != null) {
                            accountBinding.tvNoAccount.visibility = View.GONE
                            accountBinding.rvAccount.visibility = View.VISIBLE
                            accountAdapter.updateAccountList(it)
                        } else {
                            accountAdapter.updateAccountList(arrayListOf())
                            accountBinding.tvNoAccount.visibility = View.VISIBLE
                            accountBinding.rvAccount.visibility = View.GONE
                        }
                    }
                }
                job1.join()
                job2.join()
            }
            alertDialog.show()
        }
        binding.btCheckForUpdate.setOnClickListener {
            binding.tvCheckForUpdate.text = getString(R.string.checking)
            viewModel.checkForUpdate()
        }

        binding.btVersion.setOnClickListener {
            findNavController().navigateSafe(R.id.action_global_creditFragment)
        }
        
        binding.btMusixmatchLogin.setOnClickListener {
            if (viewModel.musixmatchLoggedIn.value == DataStoreManager.TRUE) {
                viewModel.clearMusixmatchCookie()
                Toast.makeText(requireContext(), getString(R.string.logged_out), Toast.LENGTH_SHORT).show()
            }
            else if (viewModel.musixmatchLoggedIn.value == DataStoreManager.FALSE) {
                findNavController().navigateSafe(R.id.action_global_musixmatchFragment)
            }
        }

        binding.btEqualizer.setOnClickListener {
            val eqIntent = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL)
            eqIntent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, requireContext().packageName)
            eqIntent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, sharedViewModel.simpleMediaServiceHandler?.player?.audioSessionId)
            eqIntent.putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
            val packageManager = requireContext().packageManager
            val resolveInfo: List<*> = packageManager.queryIntentActivities(eqIntent, 0)
            Log.d("EQ", resolveInfo.toString())
            if (resolveInfo.isEmpty()) {
                Toast.makeText(requireContext(), getString(R.string.no_equalizer), Toast.LENGTH_SHORT).show()
            }
            else{
                resultLauncher.launch(eqIntent)
            }
        }
        binding.btGithub.setOnClickListener {
            val urlIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://github.com/maxrave-dev/")
            )
            startActivity(urlIntent)
        }
        binding.btDonate.setOnClickListener {
            val urlIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://paypal.me/maxraveofficial")
            )
            startActivity(urlIntent)
        }
        binding.btStoragePlayerCache.setOnClickListener {
            val dialog = MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.clear_player_cache))
                .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                    dialog.dismiss()
                }
                .setPositiveButton(getString(R.string.clear)) { dialog, _ ->
                    viewModel.clearPlayerCache()
                    dialog.dismiss()
                }
            dialog.show()
        }
        binding.btContentCountry.setOnClickListener {
            var checkedIndex = -1
            val dialog = MaterialAlertDialogBuilder(requireContext())
                .setSingleChoiceItems(SUPPORTED_LOCATION.items, -1) { _, which ->
                    checkedIndex = which
                }
                .setTitle(getString(R.string.content_country))
                .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                    dialog.dismiss()
                }
                .setPositiveButton(getString(R.string.change)) { dialog, _ ->
                    if (checkedIndex != -1) {
                        viewModel.changeLocation(SUPPORTED_LOCATION.items[checkedIndex].toString())
                    }
                    dialog.dismiss()
                }
            dialog.show()
        }
        binding.btLanguage.setOnClickListener {
            var checkedIndex = -1
            val dialog = MaterialAlertDialogBuilder(requireContext())
                .setSingleChoiceItems(SUPPORTED_LANGUAGE.items, -1) { _, which ->
                    checkedIndex = which
                }
                .setTitle(getString(R.string.language))
                .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                    dialog.dismiss()
                }
                .setPositiveButton(getString(R.string.change)) { dialog, _ ->
                    if (checkedIndex != -1) {
                        val alertDialog = MaterialAlertDialogBuilder(requireContext())
                            .setTitle(R.string.warning)
                            .setMessage(R.string.change_language_warning)
                            .setNegativeButton(getString(R.string.cancel)) { d, _ ->
                                d.dismiss()
                                dialog.dismiss()
                            }
                            .setPositiveButton(getString(R.string.change)) { d, _ ->
                                viewModel.changeLanguage(SUPPORTED_LANGUAGE.codes[checkedIndex])
                                if (SUPPORTED_LANGUAGE.codes.getOrNull(checkedIndex) != null) {
                                    runCatching {
                                        SUPPORTED_LANGUAGE.items[SUPPORTED_LANGUAGE.codes.indexOf(
                                            SUPPORTED_LANGUAGE.codes[checkedIndex]
                                        )]
                                    }.onSuccess { temp ->
                                        binding.tvLanguage.text = temp
                                        val localeList = LocaleListCompat.forLanguageTags(
                                            SUPPORTED_LANGUAGE.codes.getOrNull(checkedIndex)
                                        )
                                        sharedViewModel.activityRecreate()
                                        AppCompatDelegate.setApplicationLocales(localeList)
                                    }
                                        .onFailure {
                                            Toast.makeText(
                                                requireContext(),
                                                getString(R.string.invalid_language_code),
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                }
                                d.dismiss()
                                dialog.dismiss()
                            }
                        alertDialog.show()
                    }
                    dialog.dismiss()
                }
            dialog.show()
        }
        binding.btQuality.setOnClickListener {
            var checkedIndex = -1
            val dialog = MaterialAlertDialogBuilder(requireContext())
                .setSingleChoiceItems(QUALITY.items, -1) { _, which ->
                    checkedIndex = which
                }
                .setTitle(getString(R.string.quality))
                .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                    dialog.dismiss()
                }
                .setPositiveButton(getString(R.string.change)) { dialog, _ ->
                    if (checkedIndex != -1) {
                        viewModel.changeQuality(checkedIndex)
                    }
                    dialog.dismiss()
                }
            dialog.show()

        }
        binding.btVideoQuality.setOnClickListener {
            var checkedIndex = -1
            val dialog = MaterialAlertDialogBuilder(requireContext())
                .setSingleChoiceItems(VIDEO_QUALITY.items, -1) { _, which ->
                    checkedIndex = which
                }
                .setTitle(getString(R.string.quality_video))
                .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                    dialog.dismiss()
                }
                .setPositiveButton(getString(R.string.change)) { dialog, _ ->
                    if (checkedIndex != -1) {
                        viewModel.changeVideoQuality(checkedIndex)
                    }
                    dialog.dismiss()
                }
            dialog.show()
        }
        binding.btMainLyricsProvider.setOnClickListener {
            var checkedIndex = -1
            val dialog = MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.main_lyrics_provider))
                .setSingleChoiceItems(LYRICS_PROVIDER.items, -1) { _, which ->
                    checkedIndex = which
                }
                .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                    dialog.dismiss()
                }
                .setPositiveButton(getString(R.string.change)) { dialog, _ ->
                    if (checkedIndex != -1) {
                        if (checkedIndex == 0) {
                            viewModel.setLyricsProvider(DataStoreManager.MUSIXMATCH)
                            binding.tvMainLyricsProvider.text = DataStoreManager.MUSIXMATCH
                        } else if (checkedIndex == 1){
                            viewModel.setLyricsProvider(DataStoreManager.YOUTUBE)
                            binding.tvMainLyricsProvider.text = DataStoreManager.YOUTUBE
                        }
                    }
                    viewModel.getLyricsProvider()
                    dialog.dismiss()
                }
            dialog.show()
        }

        binding.btTranslationLanguage.setOnClickListener{
            val materialAlertDialogBuilder = MaterialAlertDialogBuilder(requireContext())
            materialAlertDialogBuilder.setTitle(getString(R.string.translation_language))
            materialAlertDialogBuilder.setMessage(getString(R.string.translation_language_message))
            val editText = EditText(requireContext())
            materialAlertDialogBuilder.setView(editText)
            materialAlertDialogBuilder.setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            materialAlertDialogBuilder.setPositiveButton(getString(R.string.change)) { dialog, _ ->
                if (editText.text.toString().isNotEmpty()) {
                    if (editText.text.toString().length == 2) {
                        viewModel.setTranslationLanguage(editText.text.toString())
                    }
                    else {
                        Toast.makeText(requireContext(), getString(R.string.invalid_language_code), Toast.LENGTH_SHORT).show()
                    }
                }
                else {
                    if (viewModel.language.value != null && viewModel.language.value!!.length >= 2) {
                        viewModel.language.value?.slice(0..1)
                            ?.let { it1 -> viewModel.setTranslationLanguage(it1) }
                    }
                }
                dialog.dismiss()
            }
            materialAlertDialogBuilder.show()
        }

        binding.btStorageDownloadedCache.setOnClickListener {
            val dialog = MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.clear_downloaded_cache))
                .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                    dialog.dismiss()
                }
                .setPositiveButton(getString(R.string.clear)) { dialog, _ ->
                    viewModel.clearDownloadedCache()
                    dialog.dismiss()
                }
            dialog.show()
        }

        binding.btStorageThumbnailCache.setOnClickListener {
            val dialog = MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.clear_thumbnail_cache))
                .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                    dialog.dismiss()
                }
                .setPositiveButton(getString(R.string.clear)) { dialog, _ ->
                    viewModel.clearThumbnailCache()
                    dialog.dismiss()
                }
            dialog.show()
        }
        binding.btCategoriesSponsorBlock.setOnClickListener {
            Log.d("Check category", viewModel.sponsorBlockCategories.value.toString())
            val selectedItem: ArrayList<String> = arrayListOf()
            val item: Array<CharSequence> = Array(9) {i ->
                getString(SPONSOR_BLOCK.listName[i])
            }

            val checked = BooleanArray(9) { i ->
                if (!viewModel.sponsorBlockCategories.value.isNullOrEmpty()) {
                    viewModel.sponsorBlockCategories.value!!.contains(SPONSOR_BLOCK.list[i].toString())
                }
                else {
                    false
                }
            }

            val dialog = MaterialAlertDialogBuilder(requireContext())
                .setTitle("Category")
                .setMultiChoiceItems(item, checked) { _, i, b ->
                    if (b) {
                        if (!selectedItem.contains(SPONSOR_BLOCK.list[i].toString())) {
                            selectedItem.add(SPONSOR_BLOCK.list[i].toString())
                        }
                    } else {
                        if (selectedItem.contains(SPONSOR_BLOCK.list[i].toString())) {
                            selectedItem.remove(SPONSOR_BLOCK.list[i].toString())
                        }
                    }
                }
                .setPositiveButton(getString(R.string.save)) { dialog, _ ->
                    viewModel.setSponsorBlockCategories(selectedItem)
                    Log.d("Check category", selectedItem.toString())
                    viewModel.getSponsorBlockCategories()
                }
                .setNegativeButton(R.string.cancel) { dialog, _ ->
                    dialog.dismiss()
                }
            dialog.show()
        }


        binding.topAppBar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        binding.btBackup.setOnClickListener {
            val formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
            backupLauncher.launch("${getString(R.string.app_name)}_${LocalDateTime.now().format(formatter)}.backup")
        }

        binding.btRestore.setOnClickListener {
            restoreLauncher.launch(arrayOf("application/octet-stream"))
        }
        binding.swNormalizeVolume.setOnCheckedChangeListener { _, checked ->
            if (checked) {
                viewModel.setNormalizeVolume(true)
            } else {
                viewModel.setNormalizeVolume(false)
            }
        }
        binding.swEnableVideo.setOnCheckedChangeListener { _, checked ->
            val test = viewModel.playVideoInsteadOfAudio.value
            val checkReal = (test == DataStoreManager.TRUE) != checked
            if (checkReal) {
                val dialog = MaterialAlertDialogBuilder(requireContext())
                    .setTitle(getString(R.string.warning))
                    .setMessage(getString(R.string.play_video_instead_of_audio_warning))
                    .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                        binding.swEnableVideo.isChecked = false
                        dialog.dismiss()
                    }
                    .setPositiveButton(getString(R.string.change)) { dialog, _ ->
                        viewModel.clearPlayerCache()
                        if (checked) {
                            viewModel.setPlayVideoInsteadOfAudio(true)
                        } else {
                            viewModel.setPlayVideoInsteadOfAudio(false)
                        }
                        dialog.dismiss()
                    }
                dialog.show()
            }
        }
        binding.swSkipSilent.setOnCheckedChangeListener { _, checked ->
            if (checked) {
                viewModel.setSkipSilent(true)
            } else {
                viewModel.setSkipSilent(false)
            }
        }
        binding.swSavePlaybackState.setOnCheckedChangeListener { _, checked ->
            if (checked) {
                viewModel.setSavedPlaybackState(true)
            } else {
                viewModel.setSavedPlaybackState(false)
            }
        }
        binding.swSaveLastPlayed.setOnCheckedChangeListener { _, checked ->
            if (checked) {
                viewModel.setSaveLastPlayed(true)
            } else {
                viewModel.setSaveLastPlayed(false)
            }
        }
        binding.swEnableSponsorBlock.setOnCheckedChangeListener { _, checked ->
            if (checked) {
                viewModel.setSponsorBlockEnabled(true)
            } else {
                viewModel.setSponsorBlockEnabled(false)
            }
        }
        binding.swSaveHistory.setOnCheckedChangeListener { _, checked ->
            if (checked) {
                viewModel.setSendBackToGoogle(true)
            } else {
                viewModel.setSendBackToGoogle(false)
            }
        }
        binding.swUseMusixmatchTranslation.setOnCheckedChangeListener { _, checked ->
            if (checked) {
                viewModel.setUseTranslation(true)
            } else {
                viewModel.setUseTranslation(false)
            }
        }
        binding.btSpotifyLogin.setOnClickListener {
            if (runBlocking { viewModel.spotifyLogIn.value }) {
                val subAlertDialogBuilder = MaterialAlertDialogBuilder(requireContext())
                    .setTitle(getString(R.string.warning))
                    .setMessage(getString(R.string.log_out_warning))
                    .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                        dialog.dismiss()
                    }
                    .setPositiveButton(getString(R.string.log_out_from_spotify)) { dialog, _ ->
                        viewModel.setSpotifyLogIn(false)
                    }
                subAlertDialogBuilder.show()
            } else {
                findNavController().navigateSafe(R.id.action_global_spotifyLogInFragment)
            }
        }
        binding.swEnableSpotifyLyrics.setOnCheckedChangeListener { _, checked ->
            if (checked) {
                viewModel.setSpotifyLyrics(true)
            } else {
                viewModel.setSpotifyLyrics(false)
            }
        }
        binding.swEnableCanvas.setOnCheckedChangeListener { _, checked ->
            if (checked) {
                viewModel.setSpotifyCanvas(true)
            } else {
                viewModel.setSpotifyCanvas(false)
            }
        }
        binding.bt3rdPartyLibraries.setOnClickListener {

            val inputStream = requireContext().resources.openRawResource(R.raw.aboutlibraries)
            val scanner = Scanner(inputStream).useDelimiter("\\A")
            val stringBuilder = StringBuilder()
            while (scanner.hasNextLine()) {
                stringBuilder.append(scanner.nextLine())
            }
            Log.w("AboutLibraries", stringBuilder.toString())
            val localLib = Libs.Builder().withJson(stringBuilder.toString()).build()
            val intent = LibsBuilder()
                .withLicenseShown(true)
                .withVersionShown(true)
                .withActivityTitle(getString(R.string.third_party_libraries))
                .withSearchEnabled(true)
                .withEdgeToEdge(true)
                .withLibs(
                    localLib
                )
                .intent(requireContext())
            startActivity(intent)
        }
    }
    private fun browseFiles(dir: File): Long {
        var dirSize: Long = 0
        if (!dir.listFiles().isNullOrEmpty()) {
            for (f in dir.listFiles()!!) {
                dirSize += f.length()
                if (f.isDirectory) {
                    dirSize += browseFiles(f)
                }
            }
        }
        return dirSize
    }
    private fun drawDataStat() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                launch {
                    val mStorageStatsManager =
                        getSystemService(requireContext(), StorageStatsManager::class.java)
                    if (mStorageStatsManager != null) {

                        val totalByte =
                            mStorageStatsManager.getTotalBytes(StorageManager.UUID_DEFAULT)
                        val freeSpace =
                            mStorageStatsManager.getFreeBytes(StorageManager.UUID_DEFAULT)
                        val usedSpace = totalByte - freeSpace
                        val simpMusicSize = browseFiles(requireContext().filesDir)
                        val otherApp = simpMusicSize.let { usedSpace.minus(it) }
                        val databaseSize =
                            simpMusicSize - viewModel.playerCache.cacheSpace - viewModel.downloadCache.cacheSpace
                        if (totalByte == freeSpace + otherApp + databaseSize + viewModel.playerCache.cacheSpace + viewModel.downloadCache.cacheSpace) {
                            (binding.flexBox.getChildAt(0).layoutParams as FlexboxLayout.LayoutParams).flexBasisPercent =
                                otherApp.toFloat().div(totalByte.toFloat())
                            (binding.flexBox.getChildAt(1).layoutParams as FlexboxLayout.LayoutParams).flexBasisPercent =
                                viewModel.downloadCache.cacheSpace.toFloat()
                                    .div(totalByte.toFloat())
                            (binding.flexBox.getChildAt(2).layoutParams as FlexboxLayout.LayoutParams).flexBasisPercent =
                                viewModel.playerCache.cacheSpace.toFloat().div(totalByte.toFloat())
                            (binding.flexBox.getChildAt(3).layoutParams as FlexboxLayout.LayoutParams).flexBasisPercent =
                                databaseSize.toFloat().div(totalByte.toFloat())
                            (binding.flexBox.getChildAt(4).layoutParams as FlexboxLayout.LayoutParams).flexBasisPercent =
                                freeSpace.toFloat().div(totalByte.toFloat())
                        }
                    }
                }
            }
        }
    }

    private fun bytesToMB(bytes: Long): Long {
        val mbInBytes = 1024 * 1024
        return bytes / mbInBytes
    }
}